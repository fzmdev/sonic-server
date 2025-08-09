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
package org.cloud.sonic.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;

/**
 * @author JayWenStar, Eason
 * @date 2022/4/11 1:32 上午
 */
/**
 * 国际化 MessageSource 配置
 *
 * <p>
 * 提供 Spring {@link org.springframework.context.MessageSource} Bean，
 * 以支持代码中基于消息码读取多语言文案（见 resources/i18n 目录）。
 * </p>
 *
 * <p>
 * 配置说明：
 * <ul>
 * <li>基础路径：classpath:i18n/sonic（会自动拼接区域后缀，如 sonic_zh_CN.properties）</li>
 * <li>编码：UTF-8</li>
 * <li>可配合 {@link CommonResultControllerAdvice} 做统一响应国际化</li>
 * </ul>
 * </p>
 */
@Component
@Slf4j
public class I18nConfig {

    /**
     * 注册 MessageSource Bean
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource
                = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/sonic");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }
}
