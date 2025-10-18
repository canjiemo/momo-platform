package com.seer.fitness.system.util;

import com.seer.fitness.system.dto.UserCacheInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 安全上下文工具类
 *
 * @author seer-fitness
 */
public class SecurityContextUtil {

    /**
     * 获取当前登录用户信息
     */
    public static UserCacheInfo getCurrentUser() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        return (UserCacheInfo) request.getAttribute("currentUser");
    }

    /**
     * 获取当前用户ID（字符串格式）
     */
    public static String getCurrentUserId() {
        UserCacheInfo currentUser = getCurrentUser();
        return currentUser != null && currentUser.getUserId() != null ? String.valueOf(currentUser.getUserId()) : null;
    }

    /**
     * 获取当前用户ID（Long格式）
     */
    public static Long getCurrentUserIdAsLong() {
        UserCacheInfo currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getUserId() : null;
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        UserCacheInfo currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getUsername() : null;
    }

    /**
     * 获取当前用户真实姓名
     */
    public static String getCurrentRealName() {
        UserCacheInfo currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getRealName() : null;
    }

    /**
     * 检查当前用户是否有指定角色
     */
    public static boolean hasRole(String role) {
        UserCacheInfo currentUser = getCurrentUser();
        return currentUser != null &&
               currentUser.getRoles() != null &&
               currentUser.getRoles().contains(role);
    }

    /**
     * 检查当前用户是否有指定权限
     */
    public static boolean hasPermission(String permission) {
        UserCacheInfo currentUser = getCurrentUser();
        return currentUser != null &&
               currentUser.getPermissions() != null &&
               currentUser.getPermissions().contains(permission);
    }
}