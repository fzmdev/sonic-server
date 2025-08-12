package org.cloud.sonic.controller.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.cloud.sonic.common.config.WebAspect;
import org.cloud.sonic.common.http.RespEnum;
import org.cloud.sonic.common.http.RespModel;
import org.cloud.sonic.controller.mapper.AgentDeviceMapper;
import org.cloud.sonic.controller.models.domain.Devices;
import org.cloud.sonic.controller.services.AgentsService;
import org.cloud.sonic.controller.services.DevicesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Tag(name = "Agent端维持Ws")
@RestController
@RequestMapping("/agentsKeepWs")
public class AgentKeepWsController {

    // 1) 原 WS
    private static final String WS_URL_MAIN = "ws://{}:{}/websockets/{}/{}/{}/{}";
    // 2) 终端 WS
    private static final String WS_URL_TERMINAL = "ws://{}:{}/websockets/{}/terminal/{}/{}/{}";
    // 3) 屏幕 WS
    private static final String WS_URL_SCREEN = "ws://{}:{}/websockets/{}/screen/{}/{}/{}";

    // 心跳/重连配置
    private static final int PING_INTERVAL_SECONDS = 30;
    private static final int PING_TIMEOUT_SECONDS = 10;
    private static final int SCHEDULER_THREADS = Math.max(8, Runtime.getRuntime().availableProcessors() * 2);
    private static final int MAX_RETRY_DELAY_SECONDS = 30;

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

    // 连接表：key=udId#channel（channel: main/terminal/screen）
    private final ConcurrentMap<String, WsConnection> connections = new ConcurrentHashMap<>();

    @Autowired
    private AgentsService agentsService;

    @Autowired
    private DevicesService devicesService;

    @Autowired
    private AgentDeviceMapper agentDeviceMapper;

    @WebAspect
    @GetMapping("/test")
    public RespModel<?> test() {
        List<Devices> devicesList = agentDeviceMapper.findAgentAndDevice();
        devicesList.forEach(devices -> {
            String host = devices.getHost();
            Integer port = devices.getPort();
            String devicePlatform = devices.getDevicePlatform();
            String secretKey = devices.getSecretKey();
            String udId = devices.getUdId();
            String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsic29uaWMiLCIxZDRkMDlkZS02OTRlLTQwZDktODYwNy00ZmMyMmRlZDIwMTYiXSwiZXhwIjoxNzU1ODI5NDI0fQ.UMqdG4KchPaAEX4CbuBzWStCeqgc31z0JYsyR8zriVg";

            String wsMainUrl = StrUtil.format(WS_URL_MAIN, host, port, devicePlatform, secretKey, udId, token);
            String wsTerminalUrl = StrUtil.format(WS_URL_TERMINAL, host, port, devicePlatform, secretKey, udId, token);
            String wsScreenUrl = StrUtil.format(WS_URL_SCREEN, host, port, devicePlatform, secretKey, udId, token);

            ensureWs(udId + "#main", wsMainUrl, udId + "-main");
            ensureWs(udId + "#terminal", wsTerminalUrl, udId + "-terminal");
            ensureWs(udId + "#screen", wsScreenUrl, udId + "-screen");
        });
        return new RespModel<>(RespEnum.SEARCH_OK, devicesList);
    }

    private void ensureWs(String key, String url, String name) {
        connections.compute(key, (k, existing) -> {
            if (existing == null) {
                WsConnection c = new WsConnection(url, name);
                c.start();
                return c;
            }
            // URL 变更则替换连接
            if (!Objects.equals(existing.getUrl(), url)) {
                existing.stop();
                WsConnection c = new WsConnection(url, name);
                c.start();
                return c;
            }
            // 已存在则确保运行（幂等）
            existing.start();
            return existing;
        });
    }

    private void upsertDeviceUrl(String udId, String newUrl) {
        try {
            Devices one = devicesService.lambdaQuery()
                    .eq(Devices::getUdId, udId)
                    .select(Devices::getDeviceUrl)
                    .one();
            if (one == null || !Objects.equals(newUrl, one.getDeviceUrl())) {
                devicesService.update(new LambdaUpdateWrapper<Devices>()
                        .eq(Devices::getUdId, udId)
                        .set(Devices::getDeviceUrl, newUrl));
            }
        } catch (Exception e) {
            log.warn("更新 deviceUrl 失败, udId={}, url={}, err={}", udId, newUrl, e.toString());
        }
    }

    /**
     * 单个 WebSocket 连接的管理器：
     * - 负责建立连接、心跳保活（定时发送 ping）与断线后的指数退避重连（带抖动）
     * - 实现 WebSocket.Listener 以处理服务端回调
     * - 线程安全要点：使用 AtomicBoolean/AtomicInteger 与 volatile 字段控制状态
     */
    private final class WsConnection implements WebSocket.Listener {
        /** WebSocket 服务端地址（已格式化后的完整 URL） */
        private final String url;
        /** 连接名称（仅用于日志标识，不参与协议） */
        private final String name;

        /** 运行中的 WebSocket 句柄；断开后置为 null */
        private volatile WebSocket ws;
        /** 心跳任务（定时发送 ping）的句柄，用于取消与替换 */
        private volatile ScheduledFuture<?> pingTask;

        /** 运行状态标记：start() 成功置为 true，用于允许重连与心跳 */
        private final AtomicBoolean running = new AtomicBoolean(false);
        /** 连接中标记：避免并发重复发起连接 */
        private final AtomicBoolean connecting = new AtomicBoolean(false);
        /** 重试计数（用于指数退避），连接成功后会清零 */
        private final AtomicInteger retry = new AtomicInteger(0);

        // 解析缓存（构造时一次解析）
        private final String platform;
        private final String udId;
        private final String host;

        private WsConnection(String url, String name) {
            this.url = Objects.requireNonNull(url);
            this.name = Objects.requireNonNull(name);
            Map<String, String> p = parseWsParams(this.url);
            this.platform = p.getOrDefault("platform", "");
            this.udId = p.getOrDefault("udId", "");
            this.host = p.getOrDefault("host", "");
        }

        String getUrl() {
            return url;
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
                if (ws == null) {
                    doConnect();
                }
            }
        }

        /**
         * 停止连接：停止心跳、关闭 socket、禁止重连
         */
        void stop() {
            running.set(false);
            cancelPing();
            WebSocket s = this.ws;
            this.ws = null;
            if (s != null) {
                try {
                    s.sendClose(1000, "shutdown");
                } catch (Throwable ignored) {
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
         * - 先取消旧任务，再以固定延迟（30s）发送 ping（避免任务追赶抖动）
         * - 每次发送设置 10s 超时，仅记录异常日志不抛出
         */
        private void schedulePing() {
            cancelPing();
            pingTask = SCHEDULER.scheduleWithFixedDelay(() -> {
                try {
                    WebSocket s = this.ws;
                    if (s != null) {
                        s.sendPing(ByteBuffer.wrap(new byte[0]))
                                .orTimeout(PING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                .exceptionally(e -> {
                                    log.debug("[WS:{}] 发送 PING 失败: {}", name, e.toString());
                                    return null;
                                });
                    }
                } catch (Throwable t) {
                    log.debug("[WS:{}] 心跳异常: {}", name, t.toString());
                }
            }, PING_INTERVAL_SECONDS, PING_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }

        /** 取消当前心跳任务（若存在） */
        private void cancelPing() {
            ScheduledFuture<?> task = pingTask;
            if (task != null) {
                task.cancel(false);
                pingTask = null;
            }
        }

        /**
         * 按指数退避策略调度重连（带抖动）：
         * - 退避序列约为：1, 2, 4, 8, 16, 30, 30...（秒）
         * - 额外加入 0~(base/3) 的随机抖动，避免“重连惊群”
         * - 仅在 running=true 时才会调度
         */
        private void scheduleReconnect() {
            if (!running.get()) return;
            int attempt = Math.min(retry.getAndIncrement(), 6);
            long base = (long) Math.min(MAX_RETRY_DELAY_SECONDS, Math.pow(2, attempt));
            long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1, base / 3));
            long delay = base + jitter;
            log.info("[WS:{}] {} 秒后重连（第 {} 次）", name, delay, attempt + 1);
            SCHEDULER.schedule(this::doConnect, delay, TimeUnit.SECONDS);
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            log.info("[WS:{}] onOpen", name);
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            log.info("[WS:{}] 收到文本", name);
            try {
                JSONObject jsonObject = JSONUtil.parseObj(data);
                if ("ios".equals(platform) && "openDriver".equals(jsonObject.getStr("msg"))) {
                    Integer port = jsonObject.getInt("wda");
                    if (port != null) {
                        upsertDeviceUrl(udId, host + ":" + port);
                    }
                } else if ("android".equals(platform) && "sas".equals(jsonObject.getStr("msg"))) {
                    Integer port = jsonObject.getInt("port");
                    if (port != null) {
                        upsertDeviceUrl(udId, host + ":" + port);
                    }
                }
            } catch (Exception e) {
                log.warn("[WS:{}] 处理文本消息异常: {}", name, e.toString());
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            // 日志降噪
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            log.info("[WS:{}] 收到 PING", name);
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            log.info("[WS:{}] 收到 PONG", name);
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.info("[WS:{}] 连接关闭，status={}, reason={}", name, statusCode, reason);
            cancelPing();
            this.ws = null;
            scheduleReconnect();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("[WS:{}] 发生错误: {}", name, error.toString(), error);
            cancelPing();
            this.ws = null;
            scheduleReconnect();
        }

        /**
         * 解析 ws URL 的关键参数（兼容 main/terminal/screen）
         */
        private Map<String, String> parseWsParams(String url) {
            Map<String, String> result = new HashMap<>();
            try {
                String noSchema = url.replaceFirst("^ws://", "");
                String[] firstSplit = noSchema.split("/", 2);
                String hostPort = firstSplit[0];
                String path = firstSplit.length > 1 ? firstSplit[1] : "";
                String[] hostPair = hostPort.split(":");
                result.put("host", hostPair[0]);
                result.put("port", hostPair.length > 1 ? hostPair[1] : "");

                String[] seg = path.split("/");
                // 期望：websockets/{platform}/[terminal|screen]?/{secretKey}/{udId}/{token}
                // seg[0]=websockets, seg[1]=platform, seg[2]=optional channel
                String platform = seg.length > 1 ? seg[1] : "";
                int base = 2;
                if (seg.length > 2 && ("terminal".equals(seg[2]) || "screen".equals(seg[2]))) {
                    base = 3;
                }
                String secretKey = seg.length > base ? seg[base] : "";
                String udId = seg.length > base + 1 ? seg[base + 1] : "";
                String token = seg.length > base + 2 ? seg[base + 2] : "";

                result.put("platform", platform);
                result.put("secretKey", secretKey);
                result.put("udId", udId);
                result.put("token", token);
            } catch (Exception e) {
                log.warn("parseWsParams 解析失败: {}", e.toString());
            }
            return result;
        }
    }
}
