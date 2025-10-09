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

    public UserCacheInfo() {
    }

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
}