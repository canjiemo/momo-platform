package com.seer.fitness.system.config;

import com.seer.fitness.system.tenant.PublicSchemaContext;
import com.seer.fitness.system.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Schema 路由切面
 * <p>
 * 作用：在 BaseDao 方法执行前，根据上下文动态设置 PostgreSQL 的 search_path
 * <p>
 * 工作流程：
 * 1. 拦截 BaseDao 的所有方法
 * 2. 根据 PublicSchemaContext 和 TenantContext 确定目标 schema
 * 3. 执行 SET search_path TO <schema>
 * 4. 执行原方法（数据库操作）
 * 5. 方法执行完毕后无需恢复（下次执行会重新设置）
 * <p>
 * 优先级：
 * - Order=10：在所有拦截器之后执行，确保上下文已完全设置
 * <p>
 * Schema 路由优先级：
 * 1. PublicSchemaContext.isForcePublic() == true → public
 * 2. TenantContext.getSchemaName() != null → 租户 schema
 * 3. 默认 → public
 *
 * @author seer-fitness
 */
@Aspect
@Component
@Order(10)
@Slf4j
public class SchemaRoutingAspect {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * 定义切点：拦截 BaseDao 的所有方法
     */
    @Pointcut("execution(* io.github.mocanjie.base.myjpa.dao.BaseDao+.*(..))")
    public void baseDaoMethod() {
    }

    /**
     * 环绕通知：在 DAO 方法执行前设置 search_path
     *
     * @param joinPoint 切点信息
     * @return DAO 方法执行结果
     * @throws Throwable DAO 方法执行异常
     */
    @Around("baseDaoMethod()")
    public Object aroundDaoMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // 确定目标 schema
        String targetSchema = determineTargetSchema();

        // 设置 search_path
        setSearchPath(targetSchema);

        // 执行原方法
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            log.error("DAO method execution failed in schema: {}", targetSchema, e);
            throw e;
        }
    }

    /**
     * 确定目标 schema
     *
     * @return schema 名称
     */
    private String determineTargetSchema() {
        // 优先级1：PublicSchemaContext（@PublicSchema 注解）
        if (PublicSchemaContext.isForcePublic()) {
            log.debug("PublicSchemaContext is active, target schema: public");
            return "public";
        }

        // 优先级2：TenantContext（租户上下文）
        String schemaName = TenantContext.getSchemaName();
        if (schemaName != null && !schemaName.isEmpty()) {
            log.debug("TenantContext is active, target schema: {}", schemaName);
            return schemaName;
        }

        // 优先级3：默认 public schema
        log.debug("No context found, target schema: public (default)");
        return "public";
    }

    /**
     * 设置 PostgreSQL 的 search_path
     *
     * @param schemaName schema 名称
     */
    private void setSearchPath(String schemaName) {
        try {
            String sql = "SET search_path TO " + schemaName;
            jdbcTemplate.getJdbcTemplate().execute(sql);
            log.debug("search_path set to: {}", schemaName);
        } catch (Exception e) {
            log.error("Failed to set search_path to: {}", schemaName, e);
            // 不抛出异常，允许查询继续执行（可能会使用之前的 search_path）
        }
    }
}
