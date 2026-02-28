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
 * 租户拦截器（简化版 - tenant_id 模式）
 * 从JWT Token中提取租户信息并设置到TenantContext
 * 不再进行Schema切换，仅存储tenantId供TenantIdProvider使用
 */
@Component
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        TenantContext.clear();

        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            return true;
        }

        try {
            Long tenantId = jwtUtil.getTenantIdFromToken(token);
            String tenantCode = jwtUtil.getTenantCodeFromToken(token);
            if (tenantId != null) {
                TenantContext.setTenant(tenantId, tenantCode);
                log.debug("租户上下文已设置: tenantId={}, tenantCode={}", tenantId, tenantCode);
            }
        } catch (Exception e) {
            TenantContext.clear();
            log.debug("解析Token失败，使用默认上下文: {}", e.getMessage());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        TenantContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return request.getHeader("token");
    }
}
