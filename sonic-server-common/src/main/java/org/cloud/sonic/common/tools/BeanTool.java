package org.cloud.sonic.common.tools;

import org.cloud.sonic.common.exception.BeanToolException;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bean 工具类
 *
 * <p>
 * 对 Spring BeanUtils 的常用能力做了轻量封装：
 * <ul>
 * <li>对象属性浅拷贝（忽略 null 值）</li>
 * <li>集合批量转换</li>
 * <li>对象属性更新（source 非空字段覆盖 target 对应字段）</li>
 * <li>获取对象中为 null 的属性名集合</li>
 * </ul>
 * 主要用于 DTO 与 Domain 之间的属性转换，避免重复样板代码。
 * </p>
 */
public class BeanTool {

    private BeanTool() {
    }

    /**
     * 将对象转换为目标类型（浅拷贝，忽略 null 字段）
     *
     * @param source 源对象
     * @param targetClass 目标类型，不能为空
     * @param <T> 目标类型参数
     * @return 转换后的新实例；当 source 为 null 时返回 null
     * @throws BeanToolException 构造目标对象或拷贝失败时抛出
     */
    @Nullable
    public static <T> T transformFrom(@Nullable Object source, @NonNull Class<T> targetClass) {
        Assert.notNull(targetClass, "Target class must not be null");

        if (source == null) {
            return null;
        }

        // Init the instance
        try {
            // New instance for the target class
            T targetInstance = targetClass.getDeclaredConstructor().newInstance();
            // Copy properties
            org.springframework.beans.BeanUtils.copyProperties(source, targetInstance, getNullPropertyNames(source));
            // Return the target instance
            return targetInstance;
        } catch (Exception e) {
            throw new BeanToolException("Failed to new " + targetClass.getName() + " instance or copy properties", e);
        }
    }

    /**
     * 批量转换集合元素为目标类型（浅拷贝，忽略 null 字段）
     *
     * @param sources 源集合
     * @param targetClass 目标类型，不能为空
     * @param <T> 目标类型参数
     * @return 转换后的目标集合；当 sources 为空时返回空集合
     * @throws BeanToolException 构造目标对象或拷贝失败时抛出
     */
    @NonNull
    public static <T> List<T> transformFromInBatch(Collection<?> sources, @NonNull Class<T> targetClass) {
        if (CollectionUtils.isEmpty(sources)) {
            return Collections.emptyList();
        }

        // Transform in batch
        return sources.stream()
                .map(source -> transformFrom(source, targetClass))
                .collect(Collectors.toList());
    }

    /**
     * 使用 source 的非空字段更新 target 对应字段
     *
     * @param source 源对象，不能为空
     * @param target 目标对象，不能为空
     * @throws BeanToolException 拷贝失败时抛出
     */
    public static void updateProperties(@NonNull Object source, @NonNull Object target) {
        Assert.notNull(source, "source object must not be null");
        Assert.notNull(target, "target object must not be null");

        // Set non null properties from source properties to target properties
        try {
            org.springframework.beans.BeanUtils.copyProperties(source, target, getNullPropertyNames(source));
        } catch (BeansException e) {
            throw new BeanToolException("Failed to copy properties", e);
        }
    }

    /**
     * 获取对象中值为 null 的属性名数组
     *
     * @param source 源对象，不能为空
     * @return 为 null 的属性名数组
     */
    @NonNull
    private static String[] getNullPropertyNames(@NonNull Object source) {
        return getNullPropertyNameSet(source).toArray(new String[0]);
    }

    /**
     * 获取对象中值为 null 的属性名集合
     *
     * @param source 源对象，不能为空
     * @return 为 null 的属性名集合
     */
    @NonNull
    private static Set<String> getNullPropertyNameSet(@NonNull Object source) {

        Assert.notNull(source, "source object must not be null");
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(source);
        PropertyDescriptor[] propertyDescriptors = beanWrapper.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();

        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            String propertyName = propertyDescriptor.getName();
            Object propertyValue = beanWrapper.getPropertyValue(propertyName);

            // if property value is equal to null, add it to empty name set
            if (propertyValue == null) {
                emptyNames.add(propertyName);
            }
        }

        return emptyNames;
    }

    @NonNull
    public static <T> T toBean(Object source, Class<T> clazz) {
        Assert.notNull(source, "object must not be null");
        Assert.notNull(clazz, "clazz must not be null");

        return BeanTool.toBean(source, clazz);
    }
}
