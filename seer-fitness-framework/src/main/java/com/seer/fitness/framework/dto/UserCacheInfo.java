package com.seer.fitness.framework.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Redis 用户缓存信息（认证上下文，跨模块共享）
 */
@Data
public class UserCacheInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String realName;

    /**
     * 角色编码列表（如 ["TENANT_ADMIN", "TEACHER"]）
     * 注意：原字段 roles(List<RoleDTO>) 已简化为 roleCodes(List<String>)，
     * 只保留角色码，用于权限判断。
     */
    private List<String> roleCodes;

    private List<String> permissions;
    private Integer adminFlag;
    private Integer userType;
    private Long lastAccessTime;
    private String tokenId;
    private Long tenantId;
    private String tenantCode;

    public UserCacheInfo() {}

    public UserCacheInfo(Long userId, String username, String realName,
                         List<String> roleCodes, List<String> permissions,
                         Integer adminFlag, Integer userType, String tokenId) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.roleCodes = roleCodes;
        this.permissions = permissions;
        this.adminFlag = adminFlag;
        this.userType = userType;
        this.tokenId = tokenId;
        this.lastAccessTime = System.currentTimeMillis();
    }

    public UserCacheInfo(Long userId, String username, String realName,
                         List<String> roleCodes, List<String> permissions,
                         Integer adminFlag, Integer userType, String tokenId,
                         Long tenantId, String tenantCode) {
        this(userId, username, realName, roleCodes, permissions, adminFlag, userType, tokenId);
        this.tenantId = tenantId;
        this.tenantCode = tenantCode;
    }
}
