package org.cloud.sonic.common.config;

import com.alibaba.fastjson.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * Web 请求日志 AOP 配置
 *
 * <p>
 * 拦截标记了 {@link WebAspect} 的类或方法，统一打印 Web 请求的关键信息， 包括：URL、HTTP
 * 方法、Token、请求类与方法、入参与返回体。默认以 JSON 格式输出， 便于接入 ELK 等日志分析系统。
 * </p>
 */
@Aspect
@Component
public class WebAspectConfig {

    private final Logger logger = LoggerFactory.getLogger(WebAspectConfig.class);

    /**
     * 定义切点：匹配所有被 {@link WebAspect} 注解标记的方法
     */
    @Pointcut("@annotation(WebAspect)")
    public void webAspect() {
    }

    /**
     * 请求前记录入参与基本信息
     */
    @Before("webAspect()")
    public void deBefore(JoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        //默认打印为json格式，接入elasticsearch等会方便查看
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("url", request.getRequestURL().toString());
        jsonObject.put("method", request.getMethod());
        jsonObject.put("auth", request.getHeader("SonicToken"));
        jsonObject.put("class", joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());
        jsonObject.put("request", Arrays.toString(joinPoint.getArgs()));
        logger.info(jsonObject.toJSONString());
    }

    /**
     * 请求成功返回后记录响应结果
     */
    @AfterReturning(returning = "ret", pointcut = "webAspect()")
    public void doAfterReturning(Object ret) throws Throwable {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("response", ret);
        logger.info(jsonObject.toJSONString());
    }

    /**
     * 异常场景记录错误信息
     */
    @AfterThrowing(throwing = "ex", pointcut = "webAspect()")
    public void error(JoinPoint joinPoint, Exception ex) {
        logger.info("error : " + ex.getMessage());
    }

}
