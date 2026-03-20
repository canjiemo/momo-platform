package io.github.canjiemo.momo.framework.utils;

/**
 * 异步线程租户上下文持有者
 * <p>
 * 用于在异步线程（如 AI 查询线程池）中传递 tenantId，
 * 供 TenantIdProvider 在 SecurityContextUtil 不可用时兜底读取。
 * <p>
 * 使用方：
 * 1. 提交异步任务前：AsyncTenantHolder.set(tenantId)
 * 2. 任务执行完后：AsyncTenantHolder.clear()（在 finally 块中）
 */
public class AsyncTenantHolder {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    public static void set(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Long get() {
        return TENANT_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
