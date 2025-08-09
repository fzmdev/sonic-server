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

/**
 * 统一响应码与消息码枚举
 *
 * <p>
 * 用于服务端与客户端之间协议约定：每个枚举常量包含数值型 code 与消息码 message。 其中 message 将由国际化切面
 * {@link org.cloud.sonic.common.config.CommonResultControllerAdvice}
 * 根据请求语言转换为最终文案。
 * </p>
 */
public enum RespEnum {
    HANDLE_OK(2000, "ok.handle"),
    SEARCH_OK(2000, "ok.search"),
    UPDATE_OK(2000, "ok.update"),
    DELETE_OK(2000, "ok.delete"),
    UPLOAD_OK(2000, "ok.upload"),
    SEND_OK(2000, "ok.send"),
    COPY_OK(2000, "ok.copy"),
    SEARCH_FAIL(3005, "fail.search"),
    UPDATE_FAIL(3004, "fail.update"),
    DELETE_FAIL(3002, "fail.delete"),
    UPLOAD_FAIL(3003, "fail.upload"),
    UNAUTHORIZED(1001, "unauthorized"),
    SERVICE_NOT_FOUND(1002, "not.found.service"),
    PERMISSION_DENIED(1003, "permission.denied"),
    RESOURCE_NOT_FOUND(1004, "not.found.resource"),
    ID_NOT_FOUND(3001, "not.found.id"),
    DEVICE_NOT_FOUND(3002, "not.found.device"),
    AGENT_NOT_ONLINE(5001, "not.online.agent"),
    PARAMS_NOT_VALID(4004, "not.valid.params"),
    PARAMS_NOT_READABLE(4005, "not.readable.params"),
    PARAMS_MISSING_ERROR(4001, "error.params.missing"),
    PARAMS_VIOLATE_ERROR(4002, "error.params.violate"),
    UNKNOWN_ERROR(4003, "error.unknown");

    private int code;
    private String message;

    RespEnum(int code, String message) {
        this.code = code;
        this.message = message;
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

    public void setMessage(String message) {
        this.message = message;
    }
}
