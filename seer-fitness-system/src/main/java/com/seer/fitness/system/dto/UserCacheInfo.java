package com.seer.fitness.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Redis用户缓存信息
 *
 * @author seer-fitness
 */
@Data
public class UserCacheInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 角色列表
     */
    private List<RoleDTO> roles;

    /**
     * 权限列表
     */
    private List<String> permissions;

    /**
     * 管理员标识：true-管理员，false-普通用户
     */
    private Integer adminFlag;

    /**
     * 用户类型：0-运维人员 1-教师 2-学生
     * @see com.seer.fitness.framework.enums.UserType
     */
    private Integer userType;

    /**
     * 最后访问时间
     */
    private Long lastAccessTime;

    /**
     * Token唯一标识
     */
    private String tokenId;

    /**
     * 租户ID（仅租户用户有值，平台用户为null）
     * 安全加固 - 2024-10-18
     */
    private Long tenantId;

    /**
     * 租户编码（仅租户用户有值，平台用户为null）
     * 安全加固 - 2024-10-18
     */
    private String tenantCode;

    /**
     * Schema名称（仅租户用户有值，平台用户为null）
     * 安全加固 - 2024-10-18
     */
    private String schemaName;

    public UserCacheInfo() {
    }

    /**
     * 平台用户构造函数（不包含租户信息）
     */
    public UserCacheInfo(Long userId, String username, String realName, List<RoleDTO> roles, List<String> permissions, Integer adminFlag, Integer userType, String tokenId) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.roles = roles;
        this.permissions = permissions;
        this.adminFlag = adminFlag;
        this.userType = userType;
        this.tokenId = tokenId;
        this.lastAccessTime = System.currentTimeMillis();
    }

    /**
     * 租户用户构造函数（包含租户信息）
     * 安全加固 - 2024-10-18
     */
    public UserCacheInfo(Long userId, String username, String realName, List<RoleDTO> roles, List<String> permissions,
                        Integer adminFlag, Integer userType, String tokenId,
                        Long tenantId, String tenantCode, String schemaName) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.roles = roles;
        this.permissions = permissions;
        this.adminFlag = adminFlag;
        this.userType = userType;
        this.tokenId = tokenId;
        this.tenantId = tenantId;
        this.tenantCode = tenantCode;
        this.schemaName = schemaName;
        this.lastAccessTime = System.currentTimeMillis();
    }
}