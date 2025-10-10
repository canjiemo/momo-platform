package com.seer.fitness.system.tenant;

import lombok.extern.slf4j.Slf4j;

/**
 * 租户上下文 - 基于ThreadLocal实现
 * 用于在当前线程中存储和传递租户信息
 * <p>
 * 核心功能：
 * - 存储当前线程的租户ID、租户编码、Schema名称
 * - 线程隔离，确保不同请求之间的租户信息不会混淆
 * - 支持清理，防止内存泄漏
 * <p>
 * 使用场景：
 * 1. 登录时设置租户信息（从JWT Token解析）
 * 2. 拦截器中设置租户上下文
 * 3. 数据访问时获取当前租户的Schema
 * 4. 请求结束后清理上下文
 * <p>
 * 注意事项：
 * - 必须在请求结束时调用clear()，否则会导致内存泄漏
 * - 适合Servlet容器的线程池模型
 * - 不适合异步任务（需要手动传递租户信息）
 *
 * @author seer-fitness
 */
@Slf4j
public class TenantContext {

    /**
     * 租户ID（ThreadLocal）
     */
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    /**
     * 租户编码（ThreadLocal）
     */
    private static final ThreadLocal<String> TENANT_CODE = new ThreadLocal<>();

    /**
     * Schema名称（ThreadLocal）
     */
    private static final ThreadLocal<String> SCHEMA_NAME = new ThreadLocal<>();

    /**
     * 设置租户ID
     */
    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
        log.debug("TenantContext设置租户ID: {}", tenantId);
    }

    /**
     * 获取租户ID
     */
    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * 设置租户编码
     */
    public static void setTenantCode(String tenantCode) {
        TENANT_CODE.set(tenantCode);
        log.debug("TenantContext设置租户编码: {}", tenantCode);
    }

    /**
     * 获取租户编码
     */
    public static String getTenantCode() {
        return TENANT_CODE.get();
    }

    /**
     * 设置Schema名称
     */
    public static void setSchemaName(String schemaName) {
        SCHEMA_NAME.set(schemaName);
        log.debug("TenantContext设置Schema名称: {}", schemaName);
    }

    /**
     * 获取Schema名称
     */
    public static String getSchemaName() {
        return SCHEMA_NAME.get();
    }

    /**
     * 一次性设置所有租户信息
     *
     * @param tenantId   租户ID
     * @param tenantCode 租户编码
     * @param schemaName Schema名称
     */
    public static void setTenant(Long tenantId, String tenantCode, String schemaName) {
        setTenantId(tenantId);
        setTenantCode(tenantCode);
        setSchemaName(schemaName);
        log.debug("TenantContext设置租户信息: tenantId={}, tenantCode={}, schemaName={}",
                tenantId, tenantCode, schemaName);
    }

    /**
     * 清理当前线程的租户上下文
     * 必须在请求结束时调用，防止内存泄漏
     * 通常在拦截器的afterCompletion()中调用
     */
    public static void clear() {
        Long tenantId = TENANT_ID.get();
        String tenantCode = TENANT_CODE.get();
        String schemaName = SCHEMA_NAME.get();

        TENANT_ID.remove();
        TENANT_CODE.remove();
        SCHEMA_NAME.remove();

        log.debug("TenantContext已清理: tenantId={}, tenantCode={}, schemaName={}",
                tenantId, tenantCode, schemaName);
    }

    /**
     * 检查当前线程是否已设置租户信息
     */
    public static boolean hasTenant() {
        return TENANT_ID.get() != null && SCHEMA_NAME.get() != null;
    }

    /**
     * 获取当前租户的完整信息（用于日志）
     */
    public static String getCurrentTenantInfo() {
        return String.format("TenantInfo[id=%d, code=%s, schema=%s]",
                getTenantId(), getTenantCode(), getSchemaName());
    }
}
