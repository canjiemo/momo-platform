package com.seer.fitness.system.tenant;

import lombok.extern.slf4j.Slf4j;

/**
 * Public Schema 上下文管理器
 * <p>
 * 作用：通过 ThreadLocal 管理当前线程是否强制路由到 public schema
 * <p>
 * 核心功能：
 * 1. 嵌套调用支持：通过引用计数器避免嵌套方法互相干扰
 * 2. 线程隔离：每个请求线程独立管理自己的路由状态
 * 3. 自动清理：提供 clear() 方法避免线程池复用导致的污染
 * <p>
 * 使用场景：
 * - PublicSchemaMethodInterceptor：方法级别 @PublicSchema 注解拦截
 * - PublicSchemaDAOInterceptor：检查是否需要路由到 public schema
 * <p>
 * 示例：
 * <pre>
 * // 方法1标记了 @PublicSchema
 * public void method1() {
 *     PublicSchemaContext.enter();  // counter = 1, flag = true
 *     method2();  // 嵌套调用
 *     PublicSchemaContext.exit();   // counter = 0, flag = false
 * }
 *
 * // 方法2也标记了 @PublicSchema
 * public void method2() {
 *     PublicSchemaContext.enter();  // counter = 2, flag 保持 true
 *     queryDict();  // 所有查询都路由到 public
 *     PublicSchemaContext.exit();   // counter = 1, flag 保持 true
 * }
 * </pre>
 *
 * @author seer-fitness
 */
@Slf4j
public class PublicSchemaContext {

    /**
     * 强制使用 public schema 的标志
     * true：所有数据库操作路由到 public schema
     * false：根据租户上下文路由到租户 schema
     */
    private static final ThreadLocal<Boolean> FORCE_PUBLIC_SCHEMA = ThreadLocal.withInitial(() -> false);

    /**
     * 嵌套调用计数器
     * <p>
     * 用途：支持 @PublicSchema 方法的嵌套调用
     * <p>
     * 示例：
     * - method1() 调用 enter() → counter = 1
     * - method1() 内部调用 method2()，method2() 也调用 enter() → counter = 2
     * - method2() 调用 exit() → counter = 1（仍然保持 public schema）
     * - method1() 调用 exit() → counter = 0（恢复到租户 schema）
     */
    private static final ThreadLocal<Integer> NESTED_COUNTER = ThreadLocal.withInitial(() -> 0);

    /**
     * 进入 public schema 上下文
     * <p>
     * 调用时机：
     * - PublicSchemaMethodInterceptor 拦截到 @PublicSchema 方法时调用
     * - 手动需要切换到 public schema 时调用
     * <p>
     * 行为：
     * - 计数器 +1
     * - 如果是第一次进入（counter = 1），则设置 flag = true
     */
    public static void enter() {
        int count = NESTED_COUNTER.get() + 1;
        NESTED_COUNTER.set(count);

        if (count == 1) {
            FORCE_PUBLIC_SCHEMA.set(true);
            log.debug("PublicSchemaContext entered: counter={}", count);
        } else {
            log.debug("PublicSchemaContext nested enter: counter={}", count);
        }
    }

    /**
     * 退出 public schema 上下文
     * <p>
     * 调用时机：
     * - PublicSchemaMethodInterceptor 方法执行完毕后调用（finally块）
     * <p>
     * 行为：
     * - 计数器 -1
     * - 如果计数器归零（counter = 0），则设置 flag = false
     * - 防止计数器变负数
     */
    public static void exit() {
        int count = NESTED_COUNTER.get();
        if (count <= 0) {
            log.warn("PublicSchemaContext exit called but counter is already 0");
            return;
        }

        count--;
        NESTED_COUNTER.set(count);

        if (count == 0) {
            FORCE_PUBLIC_SCHEMA.set(false);
            log.debug("PublicSchemaContext exited: counter={}", count);
        } else {
            log.debug("PublicSchemaContext nested exit: counter={}", count);
        }
    }

    /**
     * 检查当前线程是否强制路由到 public schema
     * <p>
     * 调用时机：
     * - PublicSchemaDAOInterceptor 在执行数据库操作前调用
     * - 动态数据源路由器判断路由目标时调用
     * <p>
     * 返回值：
     * - true：当前线程的所有数据库操作应路由到 public schema
     * - false：根据租户上下文路由到租户 schema
     */
    public static boolean isForcePublic() {
        return FORCE_PUBLIC_SCHEMA.get();
    }

    /**
     * 清空当前线程的上下文
     * <p>
     * 调用时机：
     * - 请求处理完毕后（Interceptor 的 afterCompletion）
     * - 线程池回收线程前
     * <p>
     * 目的：
     * - 防止线程池复用导致的上下文污染
     * - 确保每个新请求都从干净状态开始
     */
    public static void clear() {
        FORCE_PUBLIC_SCHEMA.remove();
        NESTED_COUNTER.remove();
        log.debug("PublicSchemaContext cleared");
    }

    /**
     * 获取当前嵌套层级（用于调试）
     */
    public static int getNestedLevel() {
        return NESTED_COUNTER.get();
    }
}
