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
package org.cloud.sonic.common.http;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一响应模型
 *
 * <p>
 * 提供后端接口标准化的响应结构：code（数值码）、message（消息码或最终文案）、data（数据体）。 与
 * {@link RespEnum}、国际化切面
 * {@link org.cloud.sonic.common.config.CommonResultControllerAdvice}
 * 配合使用，可实现多语言的统一响应。
 * </p>
 */
@Schema(name = "请求响应模型")
public class RespModel<T> {

    @Schema(description = "状态码")
    private int code;
    @Schema(description = "状态描述")
    private String message;
    @Schema(description = "响应详情")
    private T data;

    /**
     * 无参构造，保留给序列化框架
     */
    public RespModel() {
    }

    /**
     * 仅携带状态码与消息码/文案
     */
    public RespModel(int code, String message) {
        this(code, message, null);
    }

    /**
     * 携带状态码、消息码/文案与数据体
     */
    public RespModel(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 使用枚举快速构建响应（无数据体）
     */
    public RespModel(RespEnum respEnum) {
        this.code = respEnum.getCode();
        this.message = respEnum.getMessage();
    }

    /**
     * 使用枚举快速构建响应（包含数据体）
     */
    public RespModel(RespEnum respEnum, T data) {
        this.code = respEnum.getCode();
        this.message = respEnum.getMessage();
        this.data = data;
    }

    /**
     * 工具方法：无数据体的快捷构建
     */
    public static RespModel result(RespEnum respEnum) {
        return new RespModel(respEnum);
    }

    /**
     * 工具方法：包含数据体的快捷构建
     */
    public static <T> RespModel<T> result(RespEnum respEnum, T data) {
        return new RespModel<T>(respEnum, data);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public RespModel<T> setMessage(String msg) {
        this.message = msg;
        return this;
    }

    public T getData() {
        return data;
    }

    public RespModel<T> setData(T data) {
        this.data = data;
        return this;
    }

    @Override
    public String toString() {
        return "RespModel{"
                + "code=" + code
                + ", message='" + message + '\''
                + ", data=" + data
                + '}';
    }
}
