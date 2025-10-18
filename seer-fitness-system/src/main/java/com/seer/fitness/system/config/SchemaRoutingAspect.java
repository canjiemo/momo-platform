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
     * 修复SQL注入漏洞 - 2024-10-18
     *
     * @param schemaName schema 名称
     */
    private void setSearchPath(String schemaName) {
        try {
            // 1. 白名单验证 - 防止SQL注入
            if (!isValidSchemaName(schemaName)) {
                log.error("非法的schema名称尝试: {}", schemaName);
                throw new SecurityException("Invalid schema name: " + schemaName);
            }

            // 2. 额外验证：只允许public或已知的租户schema
            if (!"public".equals(schemaName) && !isKnownTenantSchema(schemaName)) {
                log.error("未知的租户schema尝试: {}", schemaName);
                throw new SecurityException("Unknown tenant schema: " + schemaName);
            }

            // 3. 使用参数化方式设置search_path
            // 使用PostgreSQL的set_config函数，避免直接拼接SQL
            String sql = "SELECT set_config('search_path', ?, false)";
            jdbcTemplate.getJdbcTemplate().queryForObject(sql, String.class, schemaName);

            log.debug("search_path安全切换到: {}", schemaName);
        } catch (SecurityException e) {
            log.error("Schema切换被拒绝: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Schema切换失败: schemaName={}", schemaName, e);
            throw new RuntimeException("Failed to switch schema: " + schemaName, e);
        }
    }

    /**
     * 验证schema名称是否符合PostgreSQL命名规范
     * 防止SQL注入攻击
     *
     * @param schemaName schema名称
     * @return 是否有效
     */
    private boolean isValidSchemaName(String schemaName) {
        if (schemaName == null || schemaName.isEmpty()) {
            return false;
        }

        // PostgreSQL schema命名规范：
        // - 只允许小写字母、数字、下划线
        // - 必须以字母开头
        // - 长度不超过63字符
        return schemaName.matches("^[a-z][a-z0-9_]{0,62}$") || "public".equals(schemaName);
    }

    /**
     * 检查是否为已知的租户schema
     * 从sys_tenant表验证，增加安全性
     *
     * @param schemaName schema名称
     * @return 是否存在
     */
    private boolean isKnownTenantSchema(String schemaName) {
        try {
            // 切换到public schema查询租户表
            String sql = "SELECT COUNT(*) FROM public.sys_tenant WHERE schema_name = ? AND status = 1";
            Integer count = jdbcTemplate.getJdbcTemplate().queryForObject(sql, Integer.class, schemaName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("验证租户schema失败: {}", schemaName, e);
            return false;
        }
    }
}
