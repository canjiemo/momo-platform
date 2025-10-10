package com.seer.fitness.system.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;

/**
 * 动态租户数据源路由
 * 基于Spring的AbstractRoutingDataSource实现
 * 根据当前线程的租户上下文自动选择对应的数据源
 * <p>
 * 工作原理：
 * 1. Spring在执行SQL前调用determineCurrentLookupKey()获取数据源key
 * 2. 我们返回当前线程的schemaName作为key
 * 3. DynamicTenantDataSourceManager根据key返回对应的数据源
 * 4. Spring使用该数据源执行SQL
 * <p>
 * 核心优势：
 * - 自动路由：无需手动切换数据源
 * - 线程隔离：基于ThreadLocal，不同请求互不影响
 * - 透明化：业务代码无感知，只需设置TenantContext
 * <p>
 * 使用场景：
 * - 登录后，拦截器设置TenantContext
 * - 后续所有数据库操作自动路由到租户Schema
 * - 请求结束后，拦截器清理TenantContext
 *
 * @author seer-fitness
 */
@Slf4j
public class DynamicTenantDataSource extends AbstractRoutingDataSource {

    private final DynamicTenantDataSourceManager dataSourceManager;

    public DynamicTenantDataSource(DynamicTenantDataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    /**
     * 确定当前查询使用的数据源key
     * 返回当前线程的schemaName
     *
     * @return 数据源key（schemaName）
     */
    @Override
    protected Object determineCurrentLookupKey() {
        String schemaName = TenantContext.getSchemaName();

        if (schemaName == null) {
            // 如果没有设置租户上下文，返回null
            // 这种情况通常发生在：
            // 1. 平台管理员访问public schema
            // 2. 登录接口（还未设置租户）
            // 3. 公共接口（不需要租户隔离）
            log.debug("未设置租户上下文，使用默认数据源");
            return null;
        }

        log.debug("路由到租户数据源: schemaName={}", schemaName);
        return schemaName;
    }

    /**
     * 根据key获取数据源
     * 重写此方法以实现懒加载
     *
     * @return 租户数据源
     */
    @Override
    protected DataSource determineTargetDataSource() {
        Object lookupKey = determineCurrentLookupKey();

        if (lookupKey == null) {
            // 返回默认数据源（public schema）
            return getResolvedDefaultDataSource();
        }

        // 从DataSourceManager获取租户数据源（懒加载）
        String schemaName = (String) lookupKey;
        return dataSourceManager.getDataSource(schemaName);
    }
}
