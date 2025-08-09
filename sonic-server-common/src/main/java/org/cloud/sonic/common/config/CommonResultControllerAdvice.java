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

import jakarta.annotation.Resource;
import org.cloud.sonic.common.http.RespModel;
import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Locale;

/**
 * @author JayWenStar, Eason
 * @date 2022/4/11 1:59 上午
 */
/**
 * 全局响应国际化处理切面
 *
 * <p>
 * 该切面拦截使用 Jackson 消息转换器输出的响应对象，对统一响应模型
 * {@link org.cloud.sonic.common.http.RespModel} 的 message
 * 字段进行国际化转换（i18n）。它会根据请求头中的 Accept-Language 解析出 {@link java.util.Locale} ，并基于
 * Spring 的 {@link org.springframework.context.MessageSource} 从资源文件读取对应语言的文案。
 * </p>
 *
 * <p>
 * 生效范围：仅对使用
 * {@link org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter}
 * 的返回数据生效；如果返回体不是 {@code RespModel}，则跳过处理。
 * </p>
 */
@ControllerAdvice
public class CommonResultControllerAdvice implements ResponseBodyAdvice<Object> {

    @Resource
    private MessageSource messageSource;

    /**
     * 判断当前响应是否需要进行处理
     *
     * @param returnType 控制器方法的返回类型
     * @param converterType 实际使用的消息转换器类型
     * @return 仅当消息转换器为 Jackson 系列时返回 true
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    /**
     * 在响应体写出之前进行处理
     *
     * <p>
     * 当返回体是 {@code RespModel} 时，依据请求头的语言设置将 message 由消息码转换为具体文案。</p>
     *
     * @param body 原始返回体
     * @param returnType 返回类型
     * @param selectedContentType 内容类型
     * @param selectedConverterType 消息转换器类型
     * @param request 请求
     * @param response 响应
     * @return 包装后的 {@link MappingJacksonValue}
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        MappingJacksonValue container = getOrCreateContainer(body);

        String l = request.getHeaders().getFirst("Accept-Language");
        Object returnBody = container.getValue();

        if (returnBody instanceof RespModel) {
            RespModel<?> baseResponse = (RespModel) returnBody;
            process(l, baseResponse, messageSource);
        }
        return container;
    }

    /**
     * 将统一响应模型的消息码根据语言环境翻译为最终文案
     *
     * @param l Accept-Language 原始值，例如 zh_CN / en_US
     * @param respModel 统一响应模型
     * @param messageSource 消息源
     * @return 替换 message 后的模型
     */
    public static RespModel process(String l, RespModel respModel, MessageSource messageSource) {
        String language = "en_US";
        if (l != null) {
            language = l;
        }
        String[] split = language.split("_");
        Locale locale;
        if (split.length >= 2) {
            locale = new Locale(split[0], split[1]);
        } else {
            locale = new Locale("en", "US");
        }
        respModel.setMessage(messageSource.getMessage(respModel.getMessage(), new Object[]{}, locale));
        return respModel;
    }

    /**
     * 保证返回值被 {@link MappingJacksonValue} 包装，便于后续处理
     */
    private MappingJacksonValue getOrCreateContainer(Object body) {
        return (body instanceof MappingJacksonValue ? (MappingJacksonValue) body : new MappingJacksonValue(body));
    }
}
