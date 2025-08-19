/*
 *   sonic-server  Sonic Cloud Real Machine Platform.
 *   Copyright (C) 2022 SonicCloudOrg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.cloud.sonic.controller.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.cloud.sonic.controller.mapper.AgentDeviceMapper;
import org.cloud.sonic.controller.models.domain.Agents;
import org.cloud.sonic.controller.models.domain.Devices;
import org.cloud.sonic.controller.services.AgentsService;
import org.cloud.sonic.controller.services.DevicesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Tag(name = "Agent端维持Ws")
@RestController
// @RequestMapping("/agentsKeepWs")
@Component
public class AgentKeepWsController {

    // 1) 原 WS（去掉 @）
    private static final String WS_URL_MAIN = "ws://{}:{}/websockets/{}/{}/{}/{}";
    // 2) 新增 WS 地址1（去掉 @）
    private static final String WS_URL_TERMINAL = "ws://{}:{}/websockets/{}/terminal/{}/{}/{}";
    // 3) 新增 WS 地址2（去掉 @）
    private static final String WS_URL_SCREEN = "ws://{}:{}/websockets/{}/screen/{}/{}/{}";

    private static final int SCHEDULER_THREADS = Math.max(8, Runtime.getRuntime().availableProcessors() * 2);

    // 共享 HttpClient + 调度线程池（心跳、重连）
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(SCHEDULER_THREADS, r -> {
                Thread t = new Thread(r, "ws-keeper");
                t.setDaemon(true);
                return t;
            });

    @Autowired
    private AgentsService agentsService;

    @Autowired
    private DevicesService devicesService;

    @Autowired
    private AgentDeviceMapper agentDeviceMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${atmp.server.base-url:}")
    private String atmpBaseUrl;

    @Value("${atmp.server.token:}")
    private String atmpToken;

    @Value("${sonic.websocket.retry.max-attempts:10}")
    private int maxRetryAttempts;

    @Value("${sonic.websocket.retry.base-delay:1}")
    private int baseDelaySeconds;

    @Value("${sonic.websocket.retry.max-delay:30}")
    private int maxDelaySeconds;

    // 新增：去重保存每条连接，避免定时器重复创建
    private final ConcurrentMap<String, WsConnection> connections = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 5000L)
    public void keepAliveTask() {
        List<Devices> devicesList = agentDeviceMapper.findAgentAndDevice();
        devicesList.stream().forEach(devices -> {
            String host = devices.getHost();
            Integer port = devices.getPort();
            String devicePlatform = devices.getDevicePlatform();
            String secretKey = devices.getSecretKey();
            String udId = devices.getUdId();
            String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsic29uaWMiLCIxZDRkMDlkZS02OTRlLTQwZDktODYwNy00ZmMyMmRlZDIwMTYiXSwiZXhwIjoxNzU1ODI5NDI0fQ.UMqdG4KchPaAEX4CbuBzWStCeqgc31z0JYsyR8zriVg";

            String wsMainUrl = StrUtil.format(WS_URL_MAIN, host, port, devicePlatform, secretKey, udId, token);
            String wsTerminalUrl = StrUtil.format(WS_URL_TERMINAL, host, port, devicePlatform, secretKey, udId, token);
            String wsScreenUrl = StrUtil.format(WS_URL_SCREEN, host, port, devicePlatform, secretKey, udId, token);

            WsConnection connMain = connections.computeIfAbsent(udId + ":main", k -> new WsConnection(wsMainUrl, udId + "-main", k));
            WsConnection connTerminal = connections.computeIfAbsent(udId + ":terminal", k -> new WsConnection(wsTerminalUrl, udId + "-terminal", k));
            WsConnection connScreen = connections.computeIfAbsent(udId + ":screen", k -> new WsConnection(wsScreenUrl, udId + "-screen", k));

            connMain.start();
            connTerminal.start();
            connScreen.start();
        });
    }

    /**
     * 单个 WebSocket 连接的管理器：
     * - 负责建立连接、心跳保活（定时发送 ping）与断线后的指数退避重连
     * - 实现 {@link java.net.http.WebSocket.Listener} 以处理服务端回调
     * - 线程安全要点：使用 AtomicBoolean/AtomicInteger 与 volatile 字段控制状态
     */
    private final class WsConnection implements WebSocket.Listener {
        /**
         * WebSocket 服务端地址（已格式化后的完整 URL）
         */
        private final String url;
        /**
         * 连接名称（仅用于日志标识，不参与协议）
         */
        private final String name;
        /**
         * connections 中的 key
         */
        private final String connKey;

        /**
         * 运行中的 WebSocket 句柄；断开后置为 null
         */
        private volatile WebSocket ws;
        /**
         * 心跳任务（定时发送 ping）的句柄，用于取消与替换
         */
        private volatile ScheduledFuture<?> pingTask;

        /**
         * 运行状态标记：start() 成功置为 true，用于允许重连与心跳
         */
        private final AtomicBoolean running = new AtomicBoolean(false);
        /**
         * 连接中标记：避免并发重复发起连接
         */
        private final AtomicBoolean connecting = new AtomicBoolean(false);
        /**
         * 重试计数（用于指数退避），连接成功后会清零
         */
        private final AtomicInteger retry = new AtomicInteger(0);

        /**
         * 构造函数
         *
         * @param url  目标 WebSocket URL，不能为空
         * @param name 连接名称，用于日志标识，不能为空
         */
        private WsConnection(String url, String name, String connKey) {
            this.url = Objects.requireNonNull(url);
            this.name = Objects.requireNonNull(name);
            this.connKey = Objects.requireNonNull(connKey);
        }

        /**
         * 启动连接（进入运行态）：
         * - 首次调用：置 running=true 并立即发起连接
         * - 重复调用：若已运行但 ws==null（曾断开），确保会再次尝试连接
         */
        void start() {
            if (running.compareAndSet(false, true)) {
                log.info("[WS:{}] start()", name);
                doConnect();
            } else {
                // 已经在运行，确保若断开时会自动重连
                if (ws == null) {
                    doConnect();
                }
            }
        }

        /**
         * 发起一次异步连接：
         * - 在 running=true 且未处于 connecting 状态下才会执行
         * - 成功：保存 socket、清零重试次数、启动心跳
         * - 失败：记录日志并调度重连
         */
        private void doConnect() {
            if (!running.get()) return;
            if (!connecting.compareAndSet(false, true)) {
                return; // 避免并发重复连接
            }
            log.info("[WS:{}] 正在连接 -> {}", name, url);
            client.newWebSocketBuilder()
                    .buildAsync(URI.create(url), this)
                    .whenComplete((socket, ex) -> {
                        connecting.set(false);
                        if (ex != null) {
                            log.error("[WS:{}] 连接失败: {}", name, url, ex);
                            scheduleReconnect();
                        } else {
                            this.ws = socket;
                            this.retry.set(0);
                            log.info("[WS:{}] 连接成功 -> {}", name, url);
                            schedulePing();
                        }
                    });
        }

        /**
         * 启动/重置心跳任务：
         * - 先取消旧任务，再以固定频率（30s）发送 ping
         * - 每次发送设置 10s 超时，仅记录异常日志不抛出
         */
        private void schedulePing() {
            cancelPing();
            // 每 30s 发送一次 ping，保持长连
            pingTask = SCHEDULER.scheduleAtFixedRate(() -> {
                try {
                    WebSocket s = this.ws;
                    if (s != null) {
                        s.sendPing(ByteBuffer.wrap(new byte[0]))
                                .orTimeout(10, TimeUnit.SECONDS)
                                .exceptionally(e -> {
                                    log.warn("[WS:{}] 发送 PING 失败: {}", name, e.toString());
                                    return null;
                                });
                    }
                } catch (Throwable t) {
                    log.warn("[WS:{}] 心跳异常: {}", name, t.toString());
                }
            }, 10, 10, TimeUnit.SECONDS);
        }

        /**
         * 取消当前心跳任务（若存在）
         */
        private void cancelPing() {
            ScheduledFuture<?> task = pingTask;
            if (task != null) {
                task.cancel(false);
                pingTask = null;
            }
        }

        /**
         * 按指数退避策略调度重连：
         * - 退避序列约为：1, 2, 4, 8, 16, 30, 30...（秒）
         * - 仅在 running=true 时才会调度
         * - 增加最大重试次数限制，超过限制后停止重连
         */
        private void scheduleReconnect() {
            if (!running.get()) return;
            
            int currentRetry = retry.get();
            if (currentRetry >= maxRetryAttempts) {
                log.warn("[WS:{}] 已达到最大重试次数 {}，停止重连", name, maxRetryAttempts);
                running.set(false);
                connections.remove(connKey);
                // 设备连接失败，清空deviceUrl
                String udId = this.name.split("-")[0];
                devicesService.update(new LambdaUpdateWrapper<Devices>()
                        .eq(Devices::getUdId, udId)
                        .set(Devices::getDeviceUrl, ""));
                return;
            }
            
            retry.getAndIncrement();
            long delay = Math.min(maxDelaySeconds, 
                    (long) (baseDelaySeconds * Math.pow(2, Math.min(currentRetry, 5))));
            log.info("[WS:{}] {} 秒后重连（第 {}/{} 次）", name, delay, currentRetry + 1, maxRetryAttempts);
            SCHEDULER.schedule(this::doConnect, delay, TimeUnit.SECONDS);
        }

        /**
         * 连接已建立的回调
         */
        @Override
        public void onOpen(WebSocket webSocket) {
            log.info("[WS:{}] onOpen", name);
            webSocket.request(1);
        }

        /**
         * 接收文本消息
         */
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            log.info("[WS:{}] 收到文本: {}", name, data);
            log.info("[WS:{}] url: {}", name, this.url);

            // 从ws链接中提取参数
            Map<String, String> paramsMap = parseWsParams(this.url);
            String platform = paramsMap.get("platform");
            String udId = paramsMap.get("udId");
            String host = paramsMap.get("host");

            JSONObject jsonObject = JSONUtil.parseObj(data);
            log.info("[WS:{}] WS返回的json消息: {}", name, jsonObject.toString());

            // iOS连接成功的消息, 更新wda
            if (platform.equals("ios")) {
                if ("openDriver".equals(jsonObject.getStr("msg"))) {
                    Integer port = (Integer) jsonObject.get("wda");
                    // 有可能转发的port是null
                    if (port != null) {
                        devicesService.update(new LambdaUpdateWrapper<Devices>()
                                .eq(Devices::getUdId, udId)
                                .set(Devices::getDeviceUrl, host + ":" + port));
                        Devices device = devicesService.findByUdId(udId);
                        Agents agent = agentsService.findById(device.getAgentId());
                        SCHEDULER.execute(() -> syncDevicePhone(udId, "ios", host, port, true, device.getModel(), device.getVersion(), device.getSize(), agent.getTideviceSocket()));
                    }

                }
            }

            // android连接成功的消息, 更新agent转发的地址
            if (platform.equals("android")) {
                if ("sas".equals(jsonObject.getStr("msg"))) {
                    Integer port = (Integer) jsonObject.get("port");
                    // 有可能转发的port是null
                    if (port != null) {
                        devicesService.update(new LambdaUpdateWrapper<Devices>()
                                .eq(Devices::getUdId, udId)
                                .set(Devices::getDeviceUrl, host + ":" + port));
                        Devices device = devicesService.findByUdId(udId);
                        Agents agent = agentsService.findById(device.getAgentId());
                        SCHEDULER.execute(() -> syncDevicePhone(udId, "android", host, port, true, device.getModel(), device.getVersion(), device.getSize(), agent.getTideviceSocket()));
                    }

                }
            }

            // 请求接收下一条消息。在Java WebSocket API中，这是流量控制机制，数字1表示允许接收1条消息。
            webSocket.request(1);
            // 返回一个已完成的CompletableFuture，值为null。这符合onText方法的返回类型CompletionStage<?>，表示消息处理已完成。
            return CompletableFuture.completedFuture(null);
        }

        /**
         * 接收二进制消息
         */
        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            // 日志太多, 不用打印
            // log.info("[WS:{}] 收到二进制，长度: {}", name, data.remaining());
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        /**
         * 收到 PING 帧
         */
        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            log.info("[WS:{}] 收到 PING", name);
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        /**
         * 收到 PONG 帧
         */
        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            log.info("[WS:{}] 收到 PONG", name);
            Map<String, String> paramsMap = parseWsParams(this.url);
            String udId = paramsMap.get("udId");
            String host = paramsMap.get("host");
            SCHEDULER.execute(() -> sendHeart(udId, host));
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        /**
         * 连接关闭回调：
         * - 停止心跳、清空 ws，并触发重连
         */
        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.info("[WS:{}] 连接关闭，status={}, reason={}", name, statusCode, reason);
            cancelPing();
            this.ws = null;
            boolean deviceOffline = (statusCode == 1001) || (reason != null && reason.contains("DEVICE_OFFLINE"));
            if (deviceOffline) {
                running.set(false);
                log.info("[WS:{}] 因设备离线停止重连", name);
                String udId = this.name.split("-")[0];
                // 断开时, 删除deviceUrl
                devicesService.update(new LambdaUpdateWrapper<Devices>()
                        .eq(Devices::getUdId, udId)
                        .set(Devices::getDeviceUrl, ""));
                connections.remove(connKey);
            } else {
                scheduleReconnect();
            }
            return CompletableFuture.completedFuture(null);
        }

        /**
         * 连接异常回调：
         * - 停止心跳、清空 ws，并触发重连
         */
        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("[WS:{}] 发生错误: {}", name, error.toString(), error);
            cancelPing();
            this.ws = null;
            scheduleReconnect();
        }

        private Map parseWsParams(String url) {
            String[] parts = url.replace("ws://", "").split("/");
            String[] hostPort = parts[0].split(":");

            Map<String, String> result = new HashMap<>();
            result.put("host", hostPort[0]);
            result.put("port", hostPort[1]);
            result.put("platform", parts[2]);
            result.put("secretKey", parts[3]);
            result.put("udId", parts[4]);
            result.put("token", parts[5]);

            return result;
        }

        private void syncDevicePhone(String udId, String platform, String host, Integer port, Boolean online, String model, String version, String size, String tideviceSocket) {
            try {
                if (StrUtil.isBlank(atmpBaseUrl)) {
                    log.warn("[WS:{}] 跳过同步：未配置 atmp.server.base-url", name);
                    return;
                }
                String base = atmpBaseUrl.endsWith("/") ? atmpBaseUrl.substring(0, atmpBaseUrl.length() - 1) : atmpBaseUrl;
                String url = base + "/device/devicePhone/add";

                Map<String, Object> body = new HashMap<>();
                body.put("id", udId);
                body.put("deviceType", platform);
                body.put("online", online);
                body.put("isBusy", false);
                body.put("usedCounts", 0);
                body.put("busyTimeout", 10800);
                String remoteUrl = platform.equals("android") ? host + ":" + port : "http://" + host + ":" + port;
                body.put("remoteUrl", remoteUrl);
                body.put("providerId", "sonic-" + host);
                body.put("providerIp", host);
                body.put("heartTime", System.currentTimeMillis());
                body.put("name", model);
                body.put("version", version);
                body.put("resolution", size);
                body.put("location", "sonic");
                body.put("isServer", false);
                body.put("deviceGroup", "group_all");
                body.put("tideviceSocket", tideviceSocket);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Access-Token", atmpToken);
                HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
                ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(url, req, String.class);
                log.info("[WS:{}] 同步设备udId: {}到ATMP成功, 返回: {}", name, udId, stringResponseEntity.getBody());
            } catch (Exception e) {
                log.warn("[WS:{}] 同步设备udId: {}到ATMP失败: {}", name, udId, e.toString());
            }
        }

        private void sendHeart(String udId, String host) {
            try {
                if (StrUtil.isBlank(atmpBaseUrl)) {
                    log.warn("[WS:{}] 跳过心跳：未配置 atmp.server.base-url", name);
                    return;
                }
                String base = atmpBaseUrl.endsWith("/") ? atmpBaseUrl.substring(0, atmpBaseUrl.length() - 1) : atmpBaseUrl;
                String url = base + "/device/devicePhone/sendheart";

                Map<String, Object> body = new HashMap<>();
                body.put("providerId", "sonic-" + host);
                body.put("ids", Collections.singletonList(udId));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Access-Token", atmpToken);
                HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
                ResponseEntity<String> stringResponseEntity = restTemplate.exchange(url, HttpMethod.PUT, req, String.class);
                log.info("[WS:{}] 上报设备udId: {}心跳到ATMP成功, 返回: {}", name, udId, stringResponseEntity.getBody());
            } catch (Exception e) {
                log.warn("[WS:{}] 上报设备udId: {}心跳失败: {}", name, udId, e.toString());
            }
        }
    }
}
