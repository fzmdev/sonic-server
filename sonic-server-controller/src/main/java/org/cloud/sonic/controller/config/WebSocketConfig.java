package org.cloud.sonic.controller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置类
 *
 * <p>
 * 该配置类用于设置Sonic平台的WebSocket通信功能。</p>
 *
 * <p>
 * 主要功能：</p>
 * <ul>
 * <li>配置WebSocket服务端点导出器</li>
 * <li>设置WebSocket端点配置器</li>
 * <li>支持实时通信和消息推送</li>
 * <li>提供测试执行状态实时更新</li>
 * </ul>
 *
 * <p>
 * 应用场景：</p>
 * <ul>
 * <li>测试执行进度实时推送</li>
 * <li>设备状态变更通知</li>
 * <li>Agent端状态监控</li>
 * <li>系统日志实时输出</li>
 * </ul>
 *
 * @author Sonic Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    @Bean
    public WsEndpointConfigure newMyEndpointConfigure() {
        return new WsEndpointConfigure();
    }
}
