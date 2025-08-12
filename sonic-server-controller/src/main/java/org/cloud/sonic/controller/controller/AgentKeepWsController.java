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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Tag(name = "Agent端维持Ws")
@RestController
@RequestMapping("/agentsKeepWs")
public class AgentKeepWsController {

    // 1) 原 WS（去掉 @）
    private static final String WS_URL_MAIN = "ws://{}:{}/websockets/{}/{}/{}/{}";
    // 2) 新增 WS 地址1（去掉 @）
    private static final String WS_URL_TERMINAL = "ws://{}:{}/websockets/{}/terminal/{}/{}/{}";
    // 3) 新增 WS 地址2（去掉 @）
    private static final String WS_URL_SCREEN = "ws://{}:{}/websockets/{}/screen/{}/{}/{}";

    // 共享 HttpClient + 调度线程池（心跳、重连）
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(3, r -> {
                Thread t = new Thread(r, "ws-keeper");
                t.setDaemon(true);
                return t;
            });

    // 三个 WS 连接管理器
    private final WsConnection connMain = new WsConnection(WS_URL_MAIN, "main");
    private final WsConnection connT1 = new WsConnection(WS_URL_TERMINAL, "terminal-1");
    private final WsConnection connT2 = new WsConnection(WS_URL_SCREEN, "terminal-2");


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

            WsConnection connMain = new WsConnection(wsMainUrl, "main");
            WsConnection connTerminal = new WsConnection(wsTerminalUrl, "terminal-1");
            WsConnection connScreen = new WsConnection(wsScreenUrl, "terminal-2");

            connMain.start();
            connTerminal.start();
            connScreen.start();
        });
        return new RespModel<>(RespEnum.SEARCH_OK, devicesList);
    }

    private final class WsConnection implements WebSocket.Listener {
        private final String url;
        private final String name;

        private volatile WebSocket ws;
        private volatile ScheduledFuture<?> pingTask;

        private final AtomicBoolean running = new AtomicBoolean(false);
        private final AtomicBoolean connecting = new AtomicBoolean(false);
        private final AtomicInteger retry = new AtomicInteger(0);

        private WsConnection(String url, String name) {
            this.url = Objects.requireNonNull(url);
            this.name = Objects.requireNonNull(name);
        }

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
            }, 30, 30, TimeUnit.SECONDS);
        }

        private void cancelPing() {
            ScheduledFuture<?> task = pingTask;
            if (task != null) {
                task.cancel(false);
                pingTask = null;
            }
        }

        private void scheduleReconnect() {
            if (!running.get()) return;
            int attempt = Math.min(retry.getAndIncrement(), 6); // cap
            long delay = (long) Math.min(30, Math.pow(2, attempt)); // 1,2,4,8,16,30,30...
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
            log.info("[WS:{}] 收到文本: {}", name, data);
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            log.info("[WS:{}] 收到二进制，长度: {}", name, data.remaining());
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
    }
}
