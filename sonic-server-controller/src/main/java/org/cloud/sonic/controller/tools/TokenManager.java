/*
 *   sonic-agent  Agent of Sonic Cloud Real Machine Platform.
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
package org.cloud.sonic.controller.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class TokenManager {

    @Value("${sonic.server.host:}")
    private String serverHost;

    @Value("${sonic.server.port:}")
    private String serverPort;

    @Value("${sonic.login.username:sonic}")
    private String loginUsername;

    @Value("${sonic.login.password:sonic}")
    private String loginPassword;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String cachedToken;
    private long tokenExpiryTime;

    /**
     * 获取认证Token，如果缓存的Token未过期则直接返回，否则重新登录获取
     */
    public String getToken() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedToken;
        }

        return loginAndGetToken();
    }

    /**
     * 登录并获取Token
     */
    private String loginAndGetToken() {
        if (serverHost == null || serverHost.trim().isEmpty() || 
            serverPort == null || serverPort.trim().isEmpty()) {
            log.error("Server配置为空，无法进行登录");
            return null;
        }

        try {
            String loginUrl = String.format("http://%s:%s/server/api/controller/users/login",
                serverHost, serverPort);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "*/*");
            headers.set("Accept-Language", "zh_CN");
            headers.set("Connection", "keep-alive");
            headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36");

            String requestBody = String.format("{\"userName\":\"%s\",\"password\":\"%s\"}", 
                loginUsername, loginPassword);

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            log.info("正在登录获取Token...");
            ResponseEntity<String> response = restTemplate.postForEntity(loginUrl, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                log.debug("登录响应: {}", responseBody);

                JsonNode jsonNode = objectMapper.readTree(responseBody);
                int code = jsonNode.get("code").asInt();

                if (code == 2000) {
                    String token = jsonNode.get("data").asText();
                    
                    // 缓存Token，设置过期时间为1小时后
                    cachedToken = token;
                    tokenExpiryTime = System.currentTimeMillis() + 3600000; // 1小时

                    log.info("Token获取成功");
                    return token;
                } else {
                    String message = jsonNode.has("message") ? jsonNode.get("message").asText() : "未知错误";
                    log.error("登录失败，错误码: {}, 错误信息: {}", code, message);
                    return null;
                }
            } else {
                log.error("登录请求失败，HTTP状态码: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            log.error("登录过程中发生异常", e);
            return null;
        }
    }

    /**
     * 清除缓存的Token，强制下次重新登录
     */
    public void clearToken() {
        cachedToken = null;
        tokenExpiryTime = 0;
        log.info("Token缓存已清除");
    }
}