package com.seer.fitness.system.security;

import com.google.common.collect.Maps;
import com.seer.fitness.system.dto.UserCacheInfo;
import com.seer.fitness.system.entity.SysTenant;
import com.seer.fitness.system.tenant.TenantContext;
import com.seer.fitness.system.util.SecurityContextUtil;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.myjpa.dao.IBaseDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 租户安全验证器（高性能版本）
 *
 * 架构理解：
 * - 平台用户：存储在 public.sys_user（无租户归属）
 * - 租户用户：存储在 {schema}.sys_user（Schema物理隔离）
 * - JWT Token：包含租户信息，由签名保护
 *
 * 真实安全风险：
 * 1. 用户被删除，但Token未过期（24小时）
 * 2. 租户被禁用，但Token未过期
 * 3. 用户被移出租户，但Token未过期
 * 4. Token与TenantContext不一致（理论上不应该发生，但需要验证）
 *
 * 核心功能：
 * 1. 验证Token中的租户与TenantContext是否一致
 * 2. 验证租户状态是否正常（是否被禁用/过期）
 * 3. 验证用户是否还存在于租户Schema中
 * 4. 多级缓存，极致性能优化（每个请求都会执行）
 *
 * 性能优化策略：
 * - L1缓存：租户状态（Redis，5分钟TTL）
 * - L2缓存：用户验证结果（Redis，5分钟TTL）
 * - 平台管理员跳过验证
 * - 快速失败机制
 * - 异步清除缓存（用户/租户变更时）
 *
 * @author seer-fitness
 * @since 2024-10-18 安全加固
 */
@Component
@Slf4j
public class TenantSecurityValidator {


    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // 租户状态缓存时间（5分钟）
    private static final long TENANT_STATUS_CACHE_MINUTES = 5L;

    // 用户验证结果缓存时间（5分钟）
    private static final long USER_VALIDATION_CACHE_MINUTES = 5L;

    /**
     * 快速验证当前请求的租户访问权限
     * 高性能设计，每个请求都会调用
     *
     * 验证内容：
     * 1. Token中的租户与TenantContext是否一致
     * 2. 租户状态是否正常
     * 3. 用户是否还存在于租户Schema中（可选，性能考虑）
     *
     * @param userCacheInfo 用户缓存信息（来自Redis）
     * @throws SecurityException 验证失败时抛出
     */
    public void validateTenantAccess(UserCacheInfo userCacheInfo) {
        // 0. 快速检查：平台管理员跳过验证
        if (userCacheInfo == null) {
            return;
        }

        if (userCacheInfo.getAdminFlag() != null && userCacheInfo.getAdminFlag() == 1) {
            log.trace("平台管理员，跳过租户验证: userId={}", userCacheInfo.getUserId());
            return;
        }

        // 1. 获取当前租户上下文
        Long contextTenantId = TenantContext.getTenantId();
        String contextSchemaName = TenantContext.getSchemaName();

        // 如果没有租户上下文，说明是平台级操作
        if (contextTenantId == null || contextSchemaName == null) {
            log.trace("无租户上下文，平台级操作");
            return;
        }

        // 2. 验证Token中的租户与TenantContext是否一致
        if (userCacheInfo.getTenantId() != null) {
            if (!userCacheInfo.getTenantId().equals(contextTenantId)) {
                log.error("【安全告警】Token租户与上下文租户不一致: userId={}, tokenTenant={}, contextTenant={}",
                        userCacheInfo.getUserId(), userCacheInfo.getTenantId(), contextTenantId);
                recordSecurityEvent(userCacheInfo.getUserId(), "TENANT_MISMATCH",
                        String.format("Token租户=%d, 上下文租户=%d", userCacheInfo.getTenantId(), contextTenantId));
                throw new SecurityException("Tenant context mismatch");
            }

            if (!userCacheInfo.getSchemaName().equals(contextSchemaName)) {
                log.error("【安全告警】Token Schema与上下文Schema不一致: userId={}, tokenSchema={}, contextSchema={}",
                        userCacheInfo.getUserId(), userCacheInfo.getSchemaName(), contextSchemaName);
                throw new SecurityException("Schema context mismatch");
            }
        }

        // 3. 验证租户状态（使用缓存）
        if (!isTenantActive(contextTenantId)) {
            log.warn("访问被禁用的租户: userId={}, tenantId={}", userCacheInfo.getUserId(), contextTenantId);
            throw new BusinessException("Tenant is disabled or expired");
        }

        // 4. 验证用户是否存在于租户Schema中（使用缓存）
        if (!isUserExistsInTenant(userCacheInfo.getUserId(), userCacheInfo.getUsername(), contextSchemaName)) {
            log.error("【安全告警】用户不存在于租户Schema中: userId={}, username={}, schema={}",
                    userCacheInfo.getUserId(), userCacheInfo.getUsername(), contextSchemaName);
            recordSecurityEvent(userCacheInfo.getUserId(), "USER_NOT_FOUND_IN_TENANT",
                    String.format("用户已被从租户中删除: schema=%s", contextSchemaName));
            throw new SecurityException("User not found in tenant");
        }

        log.trace("租户访问验证通过: userId={}, tenantId={}, schema={}",
                userCacheInfo.getUserId(), contextTenantId, contextSchemaName);
    }

    /**
     * 验证当前上下文的租户访问权限
     * 从TenantContext和SecurityContext获取信息
     */
    public void validateCurrentContextAccess() {
        // 获取当前用户信息
        UserCacheInfo currentUser = SecurityContextUtil.getCurrentUser();
        if (currentUser == null) {
            log.debug("未登录用户，跳过租户验证");
            return;
        }

        // 获取当前租户上下文
        Long contextTenantId = TenantContext.getTenantId();
        if (contextTenantId == null) {
            log.debug("无租户上下文，使用public schema");
            return;
        }

        // 额外验证：Token中的租户与上下文租户是否一致
        if (currentUser.getTenantId() != null && !currentUser.getTenantId().equals(contextTenantId)) {
            log.error("Token租户与上下文租户不一致: tokenTenant={}, contextTenant={}",
                     currentUser.getTenantId(), contextTenantId);
            throw new SecurityException("Tenant context mismatch");
        }
    }

    /**
     * 验证资源的租户归属
     * 用于验证具体资源是否属于当前租户
     *
     * @param resourceTenantId 资源所属的租户ID
     * @throws SecurityException 如果资源不属于当前租户
     */
    public void validateResourceTenantOwnership(Long resourceTenantId) {
        Long currentTenantId = TenantContext.getTenantId();

        if (currentTenantId == null) {
            log.debug("无租户上下文，可能是平台级操作");
            return;
        }

        if (!currentTenantId.equals(resourceTenantId)) {
            log.error("跨租户资源访问尝试: currentTenant={}, resourceTenant={}",
                     currentTenantId, resourceTenantId);
            throw new SecurityException("Cross-tenant resource access denied");
        }
    }

    /**
     * 验证用户是否存在于指定租户的schema中
     * 正确的多租户架构：
     * - 平台用户存储在 public.sys_user (无租户归属)
     * - 租户用户存储在 tenant_schema.sys_user (天然隔离)
     *
     * @param userId 用户ID
     * @param schemaName 租户Schema名称
     * @return 是否存在
     */
    private boolean userExistsInTenantSchema(Long userId, String schemaName) {
        try {
            // 验证用户是否存在于租户schema中
            String sql = String.format(
                "SELECT COUNT(*) FROM %s.sys_user WHERE id = :userId AND delete_flag = 0",
                schemaName
            );
            Map<String, Object> params = Maps.newHashMap();
            params.put("userId", userId);

            Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("验证用户是否存在于租户schema失败: userId={}, schema={}", userId, schemaName, e);
            return false;
        }
    }

    /**
     * 检查租户是否处于活跃状态（高性能版本，使用Redis缓存）
     * 缓存时间：5分钟
     *
     * @param tenantId 租户ID
     * @return 是否活跃
     */
    private boolean isTenantActive(Long tenantId) {
        if (tenantId == null) {
            return false;
        }

        // 1. 从Redis缓存获取
        String cacheKey = "tenant:status:" + tenantId;
        try {
            if (redisTemplate != null) {
                Integer status = (Integer) redisTemplate.opsForValue().get(cacheKey);
                if (status != null) {
                    log.trace("租户状态缓存命中: tenantId={}, status={}", tenantId, status);
                    return status == 1; // 1=正常
                }
            }
        } catch (Exception e) {
            log.warn("获取租户状态缓存失败: {}", e.getMessage());
        }

        // 2. 查询数据库
        try {
            String sql = "SELECT status FROM public.sys_tenant WHERE id = :tenantId";
            Map<String, Object> params = Maps.newHashMap();
            params.put("tenantId", tenantId);

            Integer status = jdbcTemplate.queryForObject(sql, params, Integer.class);
            boolean isActive = (status != null && status == 1);

            // 3. 写入缓存（5分钟）
            if (redisTemplate != null && status != null) {
                redisTemplate.opsForValue().set(cacheKey, status,
                        TENANT_STATUS_CACHE_MINUTES, TimeUnit.MINUTES);
                log.trace("租户状态已缓存: tenantId={}, status={}", tenantId, status);
            }

            return isActive;
        } catch (Exception e) {
            log.error("查询租户状态失败: tenantId={}", tenantId, e);
            return false;
        }
    }

    /**
     * 记录安全事件（通用版本）
     * 用于审计和安全分析
     *
     * @param userId    用户ID
     * @param eventType 事件类型
     * @param eventDesc 事件描述
     */
    private void recordSecurityEvent(Long userId, String eventType, String eventDesc) {
        try {
            if (jdbcTemplate != null) {
                String sql = "INSERT INTO public.sys_security_event " +
                        "(user_id, event_type, event_desc, created_at) VALUES " +
                        "(:userId, :eventType, :eventDesc, NOW())";

                Map<String, Object> params = Maps.newHashMap();
                params.put("userId", userId);
                params.put("eventType", eventType);
                params.put("eventDesc", eventDesc != null ?
                        eventDesc.substring(0, Math.min(500, eventDesc.length())) : null);

                jdbcTemplate.update(sql, params);

                log.warn("【安全事件】类型={}, userId={}, 描述={}", eventType, userId, eventDesc);
            }
        } catch (Exception e) {
            log.error("记录安全事件失败", e);
        }
    }

    /**
     * 构建缓存键
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 缓存键
     */
    private String buildCacheKey(Long userId, Long tenantId) {
        return String.format("tenant:access:%d:%d", userId, tenantId);
    }

    /**
     * 检查验证结果是否已缓存
     *
     * @param cacheKey 缓存键
     * @return 是否已缓存
     */
    private boolean isValidationCached(String cacheKey) {
        if (redisTemplate == null) {
            return false;
        }

        try {
            Boolean exists = redisTemplate.hasKey(cacheKey);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("检查缓存失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 缓存验证结果
     *
     * @param cacheKey 缓存键
     */
    private void cacheValidationResult(String cacheKey) {
        if (redisTemplate == null) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(cacheKey, "1", USER_VALIDATION_CACHE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("缓存验证结果失败: {}", e.getMessage());
        }
    }

    /**
     * 验证用户是否存在于租户Schema中（高性能版本，使用Redis缓存）
     * 缓存时间：5分钟
     *
     * @param userId     用户ID
     * @param username   用户名
     * @param schemaName Schema名称
     * @return 是否存在
     */
    private boolean isUserExistsInTenant(Long userId, String username, String schemaName) {
        if (userId == null || schemaName == null) {
            return false;
        }

        // 1. 从Redis缓存获取
        String cacheKey = String.format("user:exists:%s:%d", schemaName, userId);
        try {
            if (redisTemplate != null) {
                Boolean exists = (Boolean) redisTemplate.opsForValue().get(cacheKey);
                if (exists != null) {
                    log.trace("用户存在性缓存命中: userId={}, schema={}, exists={}", userId, schemaName, exists);
                    return exists;
                }
            }
        } catch (Exception e) {
            log.warn("获取用户存在性缓存失败: {}", e.getMessage());
        }

        // 2. 查询租户schema
        try {
            // ⚠️ 注意：使用安全的schema名称验证
            if (!isValidSchemaName(schemaName)) {
                log.error("非法的schema名称: {}", schemaName);
                return false;
            }

            String sql = String.format(
                    "SELECT COUNT(*) FROM %s.sys_user WHERE id = :userId AND username = :username AND delete_flag = 0",
                    schemaName
            );
            Map<String, Object> params = Maps.newHashMap();
            params.put("userId", userId);
            params.put("username", username);

            Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
            boolean exists = (count != null && count > 0);

            // 3. 写入缓存（5分钟）
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(cacheKey, exists,
                        USER_VALIDATION_CACHE_MINUTES, TimeUnit.MINUTES);
                log.trace("用户存在性已缓存: userId={}, schema={}, exists={}", userId, schemaName, exists);
            }

            return exists;
        } catch (Exception e) {
            log.error("验证用户存在性失败: userId={}, schema={}", userId, schemaName, e);
            return false;
        }
    }

    /**
     * 验证schema名称格式（防止SQL注入）
     *
     * @param schemaName schema名称
     * @return 是否有效
     */
    private boolean isValidSchemaName(String schemaName) {
        // PostgreSQL schema命名规范：只允许小写字母、数字、下划线
        return schemaName != null && schemaName.matches("^[a-z][a-z0-9_]{0,62}$");
    }

    /**
     * 清除用户验证缓存
     * 用于：用户被删除、用户权限变更时
     *
     * @param userId     用户ID
     * @param schemaName Schema名称
     */
    public void clearUserValidationCache(Long userId, String schemaName) {
        if (redisTemplate == null || userId == null || schemaName == null) {
            return;
        }

        try {
            String cacheKey = String.format("user:exists:%s:%d", schemaName, userId);
            redisTemplate.delete(cacheKey);
            log.info("清除用户验证缓存: userId={}, schema={}", userId, schemaName);
        } catch (Exception e) {
            log.warn("清除用户验证缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 清除租户状态缓存
     * 用于：租户状态变更时（禁用/启用）
     *
     * @param tenantId 租户ID
     */
    public void clearTenantStatusCache(Long tenantId) {
        if (redisTemplate == null || tenantId == null) {
            return;
        }

        try {
            String cacheKey = "tenant:status:" + tenantId;
            redisTemplate.delete(cacheKey);
            log.info("清除租户状态缓存: tenantId={}", tenantId);
        } catch (Exception e) {
            log.warn("清除租户状态缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 清除租户下所有用户的验证缓存
     * 用于：租户被删除或大批量用户变更时
     *
     * @param schemaName Schema名称
     */
    public void clearAllUserCacheInTenant(String schemaName) {
        if (redisTemplate == null || schemaName == null) {
            return;
        }

        try {
            String pattern = String.format("user:exists:%s:*", schemaName);
            redisTemplate.delete(redisTemplate.keys(pattern));
            log.info("清除租户所有用户验证缓存: schema={}", schemaName);
        } catch (Exception e) {
            log.warn("清除租户用户缓存失败: {}", e.getMessage());
        }
    }
}