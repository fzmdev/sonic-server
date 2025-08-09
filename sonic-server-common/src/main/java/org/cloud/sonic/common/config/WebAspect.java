package org.cloud.sonic.common.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Web 请求日志切面标记注解
 *
 * <p>
 * 用于标记需要被 AOP 记录入参/出参日志的 Controller 类或方法。 具体日志打印逻辑见 {@link WebAspectConfig}。
 * </p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface WebAspect {
}
