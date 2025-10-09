package com.seer.fitness.system.annotation;

import com.seer.fitness.system.enums.OperationType;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法
 *
 * 使用示例：
 * @OperationLog(
 *     type = OperationType.CREATE,
 *     module = "user",
 *     description = "创建用户",
 *     recordRequest = true,
 *     recordResponse = false
 * )
 *
 * @author seer-fitness
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作类型
     * 必填项，用于标识具体的操作行为
     *
     * @return 操作类型枚举
     */
    OperationType type();

    /**
     * 操作模块
     * 必填项，用于标识操作所属的功能模块
     *
     * @return 模块名称，如：user, role, menu, organization等
     */
    String module();

    /**
     * 操作描述
     * 必填项，用于描述具体的操作行为
     * 支持SpEL表达式，可以获取方法参数和返回值
     *
     * @return 操作描述，如：创建用户、删除角色等
     */
    String description();

    /**
     * 业务数据ID的SpEL表达式
     * 可选项，用于提取业务数据的主键ID
     * 支持从方法参数或返回值中提取
     *
     * @return SpEL表达式，如：#request.id, #result.data.id等
     */
    String businessId() default "";

    /**
     * 业务数据名称的SpEL表达式
     * 可选项，用于提取业务数据的名称或标识
     * 支持从方法参数或返回值中提取
     *
     * @return SpEL表达式，如：#request.username, #result.data.name等
     */
    String businessName() default "";

    /**
     * 是否记录请求参数
     * 默认为true，将方法的请求参数转换为JSON格式记录
     *
     * @return true-记录请求参数，false-不记录
     */
    boolean recordRequest() default true;

    /**
     * 是否记录响应数据
     * 默认为false，避免记录大量响应数据
     * 可以根据业务需要选择性开启
     *
     * @return true-记录响应数据，false-不记录
     */
    boolean recordResponse() default false;

    /**
     * 是否异步记录日志
     * 默认为true，使用异步方式记录日志，避免影响主业务性能
     * 设置为false时将同步记录，可能影响接口响应时间
     *
     * @return true-异步记录，false-同步记录
     */
    boolean async() default true;

    /**
     * 排除记录的参数名
     * 可选项，用于排除敏感参数，如密码、token等
     * 这些参数不会被记录到请求参数中
     *
     * @return 需要排除的参数名数组
     */
    String[] excludeParams() default {"password", "token", "secret", "key"};
}