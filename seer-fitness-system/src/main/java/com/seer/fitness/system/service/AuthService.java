package com.seer.fitness.system.service;

import com.seer.fitness.system.entity.SysUser;
import com.seer.fitness.system.dto.UserCacheInfo;
import com.seer.fitness.system.dto.LoginRequest;
import com.seer.fitness.system.dto.LoginResponse;
import com.seer.fitness.system.dto.RoleDTO;
import com.seer.fitness.system.utils.JwtUtil;
import com.seer.fitness.system.utils.PasswordUtil;
import com.seer.fitness.system.utils.RedisUtil;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private AccountLockService accountLockService;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private CaptchaService captchaService;

    @Autowired(required = false)
    private ITenantService tenantService;

    /**
     * 用户登录
     * 阶段5改造：支持多租户隔离
     */
    public LoginResponse login(LoginRequest request, String ip) {
        String username = request.getUsername();
        String password = request.getPassword();
        String tenantCode = request.getTenantCode();

        // 1. 验证验证码
        if (!captchaService.verifyCaptcha(request.getCaptchaId(), request.getCaptcha())) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 2. 查询租户信息
        com.seer.fitness.system.dto.TenantDTO tenant = null;
        if (tenantService != null) {
            tenant = tenantService.getByCode(tenantCode);
            if (tenant == null) {
                throw new BusinessException("租户不存在或已被禁用");
            }

            // 检查租户状态
            if (tenant.getStatus() != 1) { // 1=正常
                throw new BusinessException("租户已被禁用，请联系平台管理员");
            }

            // 检查租户是否过期
            if (tenant.getExpiredAt() != null && tenant.getExpiredAt().isBefore(java.time.LocalDateTime.now())) {
                throw new BusinessException("租户已过期，请联系平台管理员");
            }

            // 设置租户上下文（用于查询用户）
            com.seer.fitness.system.tenant.TenantContext.setTenant(
                tenant.getId(),
                tenant.getTenantCode(),
                tenant.getSchemaName()
            );
        }

        try {
            // 3. 查询用户（此时会自动路由到租户Schema）
            SysUser user = userService.findByUsername(username);
            if (user == null) {
                throw new BusinessException("用户名或密码错误");
            }

            // 4. 检查用户状态
            if (user.getStatus() != 1) {
                throw new BusinessException("账户已被禁用");
            }

            // 5. 验证密码
            if (!passwordUtil.verifyPassword(password, user.getPassword())) {
                // 记录登录失败
                accountLockService.recordFailedAttempt(username, ip);
                throw new BusinessException("用户名或密码错误");
            }

            // 6. 登录成功，重置失败记录
            accountLockService.onLoginSuccess(username, ip);

            // 7. 生成JWT Token（包含租户信息）
            String token;
            if (tenant != null) {
                token = jwtUtil.generateTokenWithTenant(
                    username,
                    user.getId(),
                    tenant.getId(),
                    tenant.getTenantCode(),
                    tenant.getSchemaName()
                );
            } else {
                token = jwtUtil.generateToken(username, user.getId());
            }
            String tokenId = jwtUtil.getTokenIdFromToken(token);

        // 7. 获取用户角色和权限
        List<RoleDTO> roles = roleService.getUserRoles(user.getId());
        List<String> permissions = menuService.getUserPermissions(user.getId());

        // 8. 缓存用户信息到Redis
        UserCacheInfo userCacheInfo = new UserCacheInfo(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                roles,
                permissions,
                user.getAdminFlag(),
                user.getUserType(),
                tokenId
        );

        // 缓存24小时
        redisUtil.set("user:token:" + tokenId, userCacheInfo, 24, TimeUnit.HOURS);


            LoginResponse result = new LoginResponse(token);

            log.info("用户登录成功: username={}, tenantCode={}, ip={}", username, tenantCode, ip);
            return result;

        } finally {
            // 清理租户上下文
            com.seer.fitness.system.tenant.TenantContext.clear();
        }
    }

    /**
     * 获取密码策略配置
     */
    public Map<String, Object> getPasswordPolicyConfig() {
        return passwordUtil.getPasswordPolicyConfig();
    }

    /**
     * 获取当前用户信息
     */
    public UserCacheInfo getCurrentUser(String token) {
        if (token == null) {
            return null;
        }

        try {
            String tokenId = jwtUtil.getTokenIdFromToken(token);
            UserCacheInfo userInfo = redisUtil.get("user:token:" + tokenId, UserCacheInfo.class);

            if (userInfo != null) {
                // 更新最后访问时间，重新缓存24小时
                userInfo.setLastAccessTime(System.currentTimeMillis());
                redisUtil.set("user:token:" + tokenId, userInfo, 24, TimeUnit.HOURS);
            }

            return userInfo;
        } catch (Exception e) {
            log.warn("获取当前用户失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 清除用户缓存（登出）
     */
    public void clearUserCache(String token) {
        if (token == null) {
            return;
        }

        try {
            String tokenId = jwtUtil.getTokenIdFromToken(token);
            redisUtil.delete("user:token:" + tokenId);
            log.info("清除用户缓存成功: tokenId={}", tokenId);
        } catch (Exception e) {
            log.warn("清除用户缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 检查用户是否有权限
     */
    public boolean hasPermission(UserCacheInfo user, String permission) {
        if (user == null || permission == null) {
            return false;
        }

        return user.getPermissions() != null && user.getPermissions().contains(permission);
    }

    /**
     * 检查用户是否有角色
     */
    public boolean hasRole(UserCacheInfo user, String role) {
        if (user == null || role == null) {
            return false;
        }

        return user.getRoles() != null && user.getRoles().contains(role);
    }
}