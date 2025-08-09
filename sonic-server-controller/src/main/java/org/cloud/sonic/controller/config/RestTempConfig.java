package org.cloud.sonic.controller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置类
 *
 * <p>
 * 该配置类用于设置HTTP客户端的RestTemplate配置，用于Sonic平台的远程API调用。</p>
 *
 * <p>
 * 主要功能：</p>
 * <ul>
 * <li>配置HTTP客户端连接超时时间</li>
 * <li>配置HTTP客户端读取超时时间</li>
 * <li>提供统一的HTTP请求工厂</li>
 * <li>支持与Agent端的HTTP通信</li>
 * </ul>
 *
 * <p>
 * 超时配置：</p>
 * <ul>
 * <li>连接超时：5秒</li>
 * <li>读取超时：5秒</li>
 * </ul>
 *
 * <p>
 * 应用场景：</p>
 * <ul>
 * <li>与Agent端进行HTTP通信</li>
 * <li>调用外部API服务</li>
 * <li>机器人通知消息发送</li>
 * <li>文件上传下载操作</li>
 * </ul>
 *
 * @author ZhouYiXun
 * @version 1.0
 * @since 2021/8/14
 */
@Configuration
public class RestTempConfig {

    @Bean
    public RestTemplate restTemplate(ClientHttpRequestFactory factory) {
        return new RestTemplate(factory);
    }

    @Bean
    public ClientHttpRequestFactory simpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(5000);
        factory.setConnectTimeout(5000);
        return factory;
    }
}
