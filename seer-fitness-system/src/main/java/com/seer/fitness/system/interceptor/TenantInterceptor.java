package com.seer.fitness.system.interceptor;

import com.seer.fitness.system.tenant.TenantContext;
import com.seer.fitness.system.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户拦截器
 * 从JWT Token中提取租户信息并设置到TenantContext
 * <p>
 * 工作流程：
 * 1. 从请求Header中提取JWT Token
 * 2. 解析Token，提取租户信息（tenantId, tenantCode, schemaName）
 * 3. 将租户信息设置到TenantContext（ThreadLocal）
 * 4. 后续的数据库操作会自动路由到租户Schema
 * 5. 请求结束后清理TenantContext
 * <p>
 * 特殊处理：
 * - 如果Token不包含租户信息，使用默认数据源（public schema）
 * - 登录接口不需要拦截（因为还未生成Token）
 * - 验证码接口不需要拦截（公共接口）
 *
 * @author seer-fitness
 */
@Component
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 请求处理前：提取租户信息并设置到上下文
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从Header中获取Token
        String token = extractToken(request);

        if (token == null || token.isEmpty()) {
            log.debug("未找到Token，使用默认数据源（public schema）");
            return true;
        }

        try {
            // 2. 从Token中提取租户信息
            Long tenantId = jwtUtil.getTenantIdFromToken(token);
            String tenantCode = jwtUtil.getTenantCodeFromToken(token);
            String schemaName = jwtUtil.getSchemaNameFromToken(token);

            // 3. 如果Token包含租户信息，设置到TenantContext
            if (tenantId != null && tenantCode != null && schemaName != null) {
                TenantContext.setTenant(tenantId, tenantCode, schemaName);
                log.debug("租户上下文已设置: tenantId={}, tenantCode={}, schemaName={}",
                        tenantId, tenantCode, schemaName);
            } else {
                log.debug("Token不包含租户信息，使用默认数据源（public schema）");
            }

        } catch (Exception e) {
            log.warn("解析Token失败，使用默认数据源: {}", e.getMessage());
        }

        return true;
    }

    /**
     * 请求处理后：清理租户上下文
     * 必须执行，防止内存泄漏
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理租户上下文
        TenantContext.clear();
        log.debug("租户上下文已清理");
    }

    /**
     * 从请求Header中提取Token
     * 支持两种方式：
     * 1. Authorization: Bearer {token}
     * 2. token: {token}
     */
    private String extractToken(HttpServletRequest request) {
        // 方式1：从Authorization Header提取
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 方式2：从token Header提取
        String token = request.getHeader("token");
        if (StringUtils.hasText(token)) {
            return token;
        }

        return null;
    }
}
