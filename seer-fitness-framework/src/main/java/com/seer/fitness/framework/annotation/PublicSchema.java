package com.seer.fitness.framework.annotation;

import java.lang.annotation.*;

/**
 * Public Schema 路由注解
 * <p>
 * 作用：标记类或方法，使其数据库操作路由到 public schema
 * <p>
 * 使用位置：
 * 1. 类级别（ElementType.TYPE）：
 *    - 实体类：整个实体的所有数据库操作都路由到 public schema
 *    - Service类：整个Service的所有方法都路由到 public schema（不推荐）
 * <p>
 * 2. 方法级别（ElementType.METHOD）：
 *    - Service方法：该方法内的所有数据库操作都路由到 public schema
 *    - Controller方法：该请求处理过程中的所有查询都路由到 public schema
 * <p>
 * 使用场景：
 * - 全局共享的数据字典表（类级别）
 * - 平台级配置表（类级别）
 * - 临时跨租户统计查询（方法级别）
 * - 管理员查看所有租户数据（方法级别）
 * <p>
 * 优先级：方法级别 &gt; 类级别 &gt; 默认租户Schema
 * <p>
 * 示例：
 * <pre>
 * // 示例1：实体类级别
 * {@code @PublicSchema(reason = "全局字典数据")}
 * public class SysDictData { }
 *
 * // 示例2：方法级别
 * {@code @Service}
 * public class DictService {
 *     {@code @PublicSchema(reason = "查询全局字典")}
 *     public List&lt;DictData&gt; getGlobalDict() { }
 * }
 *
 * // 示例3：Controller级别
 * {@code @RestController}
 * public class AdminController {
 *     {@code @PublicSchema(reason = "管理员查看全局配置")}
 *     {@code @GetMapping("/admin/global-config")}
 *     public Result getGlobalConfig() { }
 * }
 * </pre>
 *
 * @author seer-fitness
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublicSchema {

    /**
     * 说明为什么需要路由到 public schema
     * 用于文档说明和代码审计
     */
    String reason() default "访问全局共享数据";

    /**
     * 是否传播到内部调用
     * true：当前线程的所有后续查询都走 public（默认）
     * false：仅当前方法的直接查询走 public
     */
    boolean propagate() default true;
}
