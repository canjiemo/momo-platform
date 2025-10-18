package com.seer.fitness.system.security;

import com.seer.fitness.system.dto.UserCacheInfo;
import com.seer.fitness.system.tenant.TenantContext;
import com.seer.fitness.system.util.SecurityContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 租户安全审计切面
 * 记录租户相关的安全操作，用于审计和安全分析
 * <p>
 * 审计内容：
 * 1. 租户上下文切换
 * 2. 跨租户访问尝试
 * 3. 权限验证失败
 * 4. 数据源切换异常
 * 5. 敏感操作记录
 * <p>
 * 工作原理：
 * - 拦截Controller层的所有请求
 * - 记录租户访问日志
 * - 检测异常访问模式
 * - 记录安全事件到数据库
 * <p>
 * 安全加固（2024-10-18）：
 * - 实施全面的审计日志
 * - 支持安全事件告警
 * - 防止审计日志失败影响业务
 *
 * @author seer-fitness
 * @since 2024-10-18 安全加固
 */
@Aspect
@Component
@Order(5) // 在权限验证之后执行
@Slf4j
public class TenantSecurityAuditAspect {

    @Autowired(required = false)
    private NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * 定义切点：拦截所有Controller方法
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerMethods() {
    }

    /**
     * 环绕通知：记录租户访问审计日志
     *
     * @param joinPoint 切点信息
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("controllerMethods()")
    public Object auditTenantAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取审计信息
        UserCacheInfo currentUser = SecurityContextUtil.getCurrentUser();
        Long tenantId = TenantContext.getTenantId();
        String tenantCode = TenantContext.getTenantCode();
        String schemaName = TenantContext.getSchemaName();
        String methodName = joinPoint.getSignature().toShortString();

        long startTime = System.currentTimeMillis();
        boolean success = true;
        String errorMessage = null;

        try {
            // 执行原方法
            Object result = joinPoint.proceed();

            // 记录成功访问（仅限租户操作）
            if (tenantId != null && currentUser != null) {
                log.debug("租户访问审计: userId={}, tenantId={}, schema={}, method={}",
                        currentUser.getUserId(), tenantId, schemaName, methodName);

                // 验证租户一致性
                validateTenantConsistency(currentUser, tenantId, methodName);
            }

            return result;

        } catch (SecurityException e) {
            // 安全异常 - 记录详细日志
            success = false;
            errorMessage = e.getMessage();

            log.error("【安全告警】租户访问被拒绝: userId={}, tenantId={}, method={}, error={}",
                    currentUser != null ? currentUser.getUserId() : null,
                    tenantId, methodName, errorMessage);

            // 记录安全事件
            recordSecurityEvent(currentUser, tenantId, methodName, "ACCESS_DENIED", errorMessage);

            throw e;

        } catch (Exception e) {
            // 其他异常
            success = false;
            errorMessage = e.getMessage();

            log.warn("租户操作异常: userId={}, tenantId={}, method={}, error={}",
                    currentUser != null ? currentUser.getUserId() : null,
                    tenantId, methodName, errorMessage);

            throw e;

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // 记录慢查询（超过3秒）
            if (duration > 3000 && tenantId != null) {
                log.warn("租户操作耗时过长: userId={}, tenantId={}, method={}, duration={}ms",
                        currentUser != null ? currentUser.getUserId() : null,
                        tenantId, methodName, duration);
            }

            // 异步记录访问日志（不影响业务）
            recordAccessLog(currentUser, tenantId, schemaName, methodName, success, duration, errorMessage);
        }
    }

    /**
     * 验证租户一致性
     * 检查Token中的租户与上下文租户是否匹配
     *
     * @param currentUser 当前用户
     * @param contextTenantId 上下文租户ID
     * @param methodName 方法名
     */
    private void validateTenantConsistency(UserCacheInfo currentUser, Long contextTenantId, String methodName) {
        if (currentUser.getTenantId() == null) {
            // 用户没有租户信息（可能是平台管理员）
            return;
        }

        if (!currentUser.getTenantId().equals(contextTenantId)) {
            log.error("【安全告警】租户上下文不一致: userId={}, tokenTenant={}, contextTenant={}, method={}",
                    currentUser.getUserId(),
                    currentUser.getTenantId(),
                    contextTenantId,
                    methodName);

            // 记录安全事件
            String desc = String.format("租户上下文不一致: token=%d, context=%d",
                    currentUser.getTenantId(), contextTenantId);
            recordSecurityEvent(currentUser, contextTenantId, methodName, "TENANT_MISMATCH", desc);
        }
    }

    /**
     * 记录访问日志
     * 异步执行，不影响业务性能
     *
     * @param currentUser 当前用户
     * @param tenantId 租户ID
     * @param schemaName Schema名称
     * @param methodName 方法名
     * @param success 是否成功
     * @param duration 执行时长
     * @param errorMessage 错误信息
     */
    private void recordAccessLog(UserCacheInfo currentUser, Long tenantId, String schemaName,
                                  String methodName, boolean success, long duration, String errorMessage) {
        // 仅记录租户操作
        if (tenantId == null) {
            return;
        }

        try {
            // 异步记录到数据库
            // 注意：这里简化处理，实际应使用异步方式（如@Async或消息队列）
            if (jdbcTemplate != null) {
                String sql = "INSERT INTO public.sys_tenant_access_log " +
                        "(user_id, tenant_id, schema_name, method_name, success, duration_ms, error_message, created_at) " +
                        "VALUES (:userId, :tenantId, :schemaName, :methodName, :success, :duration, :errorMessage, NOW())";

                Map<String, Object> params = new HashMap<>();
                params.put("userId", currentUser != null ? currentUser.getUserId() : null);
                params.put("tenantId", tenantId);
                params.put("schemaName", schemaName);
                params.put("methodName", methodName);
                params.put("success", success);
                params.put("duration", duration);
                params.put("errorMessage", errorMessage != null ? errorMessage.substring(0, Math.min(500, errorMessage.length())) : null);

                jdbcTemplate.update(sql, params);
            }
        } catch (Exception e) {
            // 审计日志失败不应影响业务
            log.warn("记录租户访问日志失败: {}", e.getMessage());
        }
    }

    /**
     * 记录安全事件
     * 用于安全分析和告警
     *
     * @param currentUser 当前用户
     * @param tenantId 租户ID
     * @param methodName 方法名
     * @param eventType 事件类型
     * @param eventDesc 事件描述
     */
    private void recordSecurityEvent(UserCacheInfo currentUser, Long tenantId,
                                      String methodName, String eventType, String eventDesc) {
        try {
            if (jdbcTemplate != null) {
                String sql = "INSERT INTO public.sys_security_event " +
                        "(user_id, tenant_id, event_type, event_desc, method_name, created_at) " +
                        "VALUES (:userId, :tenantId, :eventType, :eventDesc, :methodName, NOW())";

                Map<String, Object> params = new HashMap<>();
                params.put("userId", currentUser != null ? currentUser.getUserId() : null);
                params.put("tenantId", tenantId);
                params.put("eventType", eventType);
                params.put("eventDesc", eventDesc != null ? eventDesc.substring(0, Math.min(500, eventDesc.length())) : null);
                params.put("methodName", methodName);

                jdbcTemplate.update(sql, params);

                log.info("安全事件已记录: type={}, userId={}, tenantId={}, method={}",
                        eventType, currentUser != null ? currentUser.getUserId() : null, tenantId, methodName);
            }
        } catch (Exception e) {
            // 安全事件记录失败也不应影响业务
            log.error("记录安全事件失败: {}", e.getMessage());
        }
    }

    /**
     * 异常访问检测
     * 检测可疑的访问模式，如频繁的权限验证失败
     * 可以扩展为更复杂的异常检测算法
     *
     * @param userId 用户ID
     * @return 是否为异常访问
     */
    @SuppressWarnings("unused")
    private boolean isAnomalousAccess(Long userId) {
        // TODO: 实现异常访问检测逻辑
        // 1. 检查短时间内的失败次数
        // 2. 检查访问模式（如非工作时间访问）
        // 3. 检查访问频率
        // 4. 检查IP地址变化
        return false;
    }
}
