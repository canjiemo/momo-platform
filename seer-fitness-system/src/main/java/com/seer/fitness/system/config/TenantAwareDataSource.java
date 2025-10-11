package com.seer.fitness.system.config;

import com.seer.fitness.system.tenant.PublicSchemaContext;
import com.seer.fitness.system.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 租户感知数据源
 * <p>
 * 作用：根据当前线程的上下文动态设置 PostgreSQL 的 search_path
 * <p>
 * 工作流程：
 * 1. 检查 PublicSchemaContext.isForcePublic()
 *    - 如果为 true：返回 "public"（强制路由到 public schema）
 *    - 如果为 false：继续检查租户上下文
 * 2. 检查 TenantContext.getTenantId()
 *    - 如果有租户信息：返回租户的 schema 名称
 *    - 如果没有租户信息：返回 "public"（默认）
 * <p>
 * 优先级：
 * PublicSchemaContext（方法级/实体级 @PublicSchema）> TenantContext（租户上下文）> public（默认）
 * <p>
 * 注意：
 * - 本类返回的是逻辑标识，实际的 search_path 设置由 SchemaRoutingConnectionInterceptor 完成
 * - 这里不直接操作数据库连接，而是提供路由决策
 *
 * @author seer-fitness
 */
@Slf4j
public class TenantAwareDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        // 优先级1：检查是否强制使用 public schema
        if (PublicSchemaContext.isForcePublic()) {
            log.debug("PublicSchemaContext active, routing to: public");
            return "public";
        }

        // 优先级2：检查租户上下文
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            String schemaName = TenantContext.getSchemaName();
            log.debug("TenantContext active, routing to: {}", schemaName);
            return schemaName;
        }

        // 优先级3：默认使用 public schema
        log.debug("No context found, routing to: public (default)");
        return "public";
    }
}
