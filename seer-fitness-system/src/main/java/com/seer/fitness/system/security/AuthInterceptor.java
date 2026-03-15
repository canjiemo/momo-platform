package com.seer.fitness.system.security;

import com.seer.fitness.framework.annotation.RequireAuth;
import com.seer.fitness.framework.dto.UserCacheInfo;
import com.seer.fitness.framework.enums.AuthMode;
import com.seer.fitness.system.config.AuthConfig;
import com.seer.fitness.system.service.IAuthService;
import io.github.canjiemo.mycommon.exception.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 权限拦截器
 *
 * @author seer-fitness
 */
@Component
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthConfig authConfig;

    @Autowired
    private IAuthService authService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        log.debug("权限拦截: {} {}", method, requestPath);

        // 1. 检查是否在白名单中
        if (isPermitAll(requestPath)) {
            log.debug("白名单路径，直接放行: {}", requestPath);
            return true;
        }

        // 2. 获取方法上的注解
        RequireAuth authAnnotation = getAuthAnnotation(handler);

        // 3. 如果注解指定不需要登录
        if (authAnnotation != null && !authAnnotation.login()) {
            log.debug("注解指定不需要登录，直接放行: {}", requestPath);
            return true;
        }

        // 4. 验证登录状态
        String token = extractToken(request);
        UserCacheInfo currentUser = authService.getCurrentUser(token);
        if (currentUser == null) {
            throw new AuthenticationException("请先登录");
        }

        // 5. 将当前用户信息存储到请求中，供Controller使用
        request.setAttribute("currentUser", currentUser);

        // 6. 检查是否只需要登录（不需要特定权限）
        if (authAnnotation != null &&
            authAnnotation.permissions().length == 0 &&
            authAnnotation.roles().length == 0) {
            log.debug("只需要登录验证，放行: {}", requestPath);
            return true;
        }

        // 7. 检查是否超级管理员（平台超管或租户管理员均绕过权限检查）
        // - 平台超管：adminFlag=1 且 tenantId=null，可访问一切
        // - 租户管理员：adminFlag=1 且 tenantId!=null，默认拥有平台为该租户授权的所有权限
        if (isSuperAdmin(currentUser) || isTenantAdmin(currentUser)) {
            log.debug("管理员用户，绕过权限检查: user={}, path={}", currentUser.getUsername(), requestPath);
            return true;
        }

        // 8. 验证具体权限
        if (authAnnotation != null) {
            return checkPermissions(currentUser, authAnnotation, requestPath);
        }

        // 9. 默认需要登录
        log.debug("默认需要登录，已验证通过: {}", requestPath);
        return true;
    }

    /**
     * 检查是否在白名单中
     */
    private boolean isPermitAll(String path) {
        return authConfig.getPermitAll().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 获取方法上的权限注解
     */
    private RequireAuth getAuthAnnotation(Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return null;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        // 先检查方法上的注解
        RequireAuth methodAuth = method.getAnnotation(RequireAuth.class);
        if (methodAuth != null) {
            return methodAuth;
        }

        // 再检查类上的注解
        return handlerMethod.getBeanType().getAnnotation(RequireAuth.class);
    }

    /**
     * 从请求中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader)) {
            return authHeader.replaceAll("Bearer ", "");
        }

        // 也可以从参数中获取（用于调试）
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }

        return null;
    }

    /**
     * 检查是否平台超级管理员
     * 必须满足：admin_flag=1 且 tenant_id=null
     */
    private boolean isSuperAdmin(UserCacheInfo user) {
        return user.getAdminFlag() != null && user.getAdminFlag() == 1 && user.getTenantId() == null;
    }

    /**
     * 检查是否租户管理员
     * 必须满足：admin_flag=1 且 tenant_id!=null
     */
    private boolean isTenantAdmin(UserCacheInfo user) {
        return user.getAdminFlag() != null && user.getAdminFlag() == 1 && user.getTenantId() != null;
    }

    /**
     * 检查权限
     */
    private boolean checkPermissions(UserCacheInfo user, RequireAuth auth, String requestPath) {
        // 检查角色
        if (auth.roles().length > 0) {
            boolean hasRole = checkRoles(user, auth.roles(), auth.mode());
            if (!hasRole) {
                log.warn("权限不足-缺少角色: user={}, requiredRoles={}, userRoles={}, path={}",
                        user.getUsername(), Arrays.toString(auth.roles()), user.getRoleCodes(), requestPath);
                throw new AuthenticationException("权限不足：缺少必要角色");
            }
        }

        // 检查权限
        if (auth.permissions().length > 0) {
            boolean hasPermission = checkUserPermissions(user, auth.permissions(), auth.mode());
            if (!hasPermission) {
                log.warn("权限不足-缺少权限: user={}, requiredPermissions={}, userPermissions={}, path={}",
                        user.getUsername(), Arrays.toString(auth.permissions()), user.getPermissions(), requestPath);
                throw new AuthenticationException("权限不足：缺少必要权限");
            }
        }

        log.debug("权限验证通过: user={}, path={}", user.getUsername(), requestPath);
        return true;
    }

    /**
     * 检查角色
     */
    private boolean checkRoles(UserCacheInfo user, String[] requiredRoles, AuthMode mode) {
        if (user.getRoleCodes() == null) {
            return false;
        }

        if (mode == AuthMode.ALL) {
            // 需要所有角色
            return user.getRoleCodes().containsAll(Arrays.asList(requiredRoles));
        } else {
            // 任意一个角色即可
            return user.getRoleCodes().stream()
                    .anyMatch(role -> Arrays.asList(requiredRoles).contains(role));
        }
    }

    /**
     * 检查权限
     */
    private boolean checkUserPermissions(UserCacheInfo user, String[] requiredPermissions, AuthMode mode) {
        if (user.getPermissions() == null) {
            return false;
        }

        if (mode == AuthMode.ALL) {
            // 需要所有权限
            return user.getPermissions().containsAll(Arrays.asList(requiredPermissions));
        } else {
            // 任意一个权限即可
            return user.getPermissions().stream()
                    .anyMatch(permission -> Arrays.asList(requiredPermissions).contains(permission));
        }
    }
}