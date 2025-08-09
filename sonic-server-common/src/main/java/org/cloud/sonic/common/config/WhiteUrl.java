package org.cloud.sonic.common.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 鉴权白名单标记注解
 *
 * <p>
 * 用于标记无需鉴权的 Controller 类或方法。被标记的接口将跳过权限校验， 常用于登录、注册、健康检查、对外开放的回调等场景。
 * </p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface WhiteUrl {
}
