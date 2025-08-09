package org.cloud.sonic.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * 业务基础异常
 *
 * <p>
 * 所有自定义业务异常的基类，默认 HTTP 状态为 200（由上层全局异常处理转换为统一响应模型）。 支持附带结构化的错误数据
 * {@code errorData}，便于前端或调用方进行额外处理。
 * </p>
 */
public class SonicException extends RuntimeException {

    private Object errorData;

    /**
     * 使用错误消息构造异常
     */
    public SonicException(String message) {
        super(message);
    }

    /**
     * 使用错误消息与结构化数据构造异常
     */
    public SonicException(String message, Object errorData) {
        super(message);
        this.errorData = errorData;
    }

    /**
     * 使用错误消息与根因构造异常
     */
    public SonicException(String message, Throwable cause) {
        super(message, cause);
    }

    @NonNull
    /**
     * 默认返回 200，由全局异常处理决定最终响应码
     */
    public HttpStatus getStatus() {
        return HttpStatus.OK;
    }

    @Nullable
    /**
     * 获取附带的结构化错误数据
     */
    public Object getErrorData() {
        return errorData;
    }

    @NonNull
    /**
     * 设置错误数据，返回当前异常便于链式调用
     */
    public SonicException setErrorData(@Nullable Object errorData) {
        this.errorData = errorData;
        return this;
    }
}
