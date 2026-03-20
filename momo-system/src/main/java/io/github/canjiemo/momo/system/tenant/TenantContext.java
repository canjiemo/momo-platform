package io.github.canjiemo.momo.system.tenant;

import lombok.extern.slf4j.Slf4j;

/**
 * 租户上下文 - 基于ThreadLocal实现（简化版 - tenant_id 模式）
 * 存储当前线程的租户ID和租户编码
 * 不再管理Schema路由
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_CODE = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static void setTenantCode(String tenantCode) {
        TENANT_CODE.set(tenantCode);
    }

    public static String getTenantCode() {
        return TENANT_CODE.get();
    }

    public static void setTenant(Long tenantId, String tenantCode) {
        TENANT_ID.set(tenantId);
        TENANT_CODE.set(tenantCode);
        log.debug("TenantContext设置租户信息: tenantId={}, tenantCode={}", tenantId, tenantCode);
    }

    public static void clear() {
        TENANT_ID.remove();
        TENANT_CODE.remove();
        log.debug("TenantContext已清理");
    }

    public static boolean hasTenant() {
        return TENANT_ID.get() != null;
    }
}
