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
package org.cloud.sonic.controller.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置类
 *
 * <p>
 * 该配置类用于设置Sonic Controller的API文档生成配置。</p>
 *
 * <p>
 * 主要功能：</p>
 * <ul>
 * <li>配置OpenAPI文档的基本信息</li>
 * <li>设置API文档标题、版本、描述</li>
 * <li>配置联系人和许可证信息</li>
 * <li>提供Swagger UI界面</li>
 * </ul>
 *
 * <p>
 * 访问地址：</p>
 * <ul>
 * <li>Swagger UI: /swagger-ui.html</li>
 * <li>OpenAPI JSON: /v3/api-docs</li>
 * </ul>
 *
 * @author Sonic Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class SpringDocConfig {

    @Value("${spring.version}")
    private String version;

    @Bean
    public OpenAPI springDocOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Controller REST API")
                        .version(version)
                        .contact(new Contact().name("SonicCloudOrg")
                                .email("soniccloudorg@163.com"))
                        .description("Controller of Sonic Cloud Real Machine Platform")
                        .termsOfService("https://github.com/SonicCloudOrg/sonic-server/blob/main/LICENSE")
                        .license(new License().name("AGPL v3")
                                .url("https://github.com/SonicCloudOrg/sonic-server/blob/main/LICENSE")));
    }
}
