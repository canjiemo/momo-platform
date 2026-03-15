package com.seer.fitness.system.dto;

import io.github.canjiemo.tools.dict.MyDict;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户DTO
 *
 * @author seer-fitness
 */
@Data
public class UserDTO {

    /**
     * 用户ID
     * (FastJSON2 已配置 WriteLongAsString，JSON 输出为字符串，无精度问题)
     */
    private Long id;

    /**
     * 租户ID（平台管理员为null）
     */
    private Long tenantId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 状态：1启用 0禁用
     */
    @MyDict(type = "user_status")
    private Integer status;

    /**
     * 管理员标识：1管理员 0普通用户
     */
    @MyDict(type = "admin_flag")
    private Integer adminFlag;

    /**
     * 用户类型：0-运维人员 1-教师 2-学生
     * @see com.seer.fitness.framework.enums.UserType
     */
    @MyDict(type = "user_type")
    private Integer userType;

    /**
     * 所属组织ID
     */
    private Long orgId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 角色列表
     */
    private List<RoleDTO> roles;

    /**
     * 用户菜单权限列表（扁平列表，包含目录和菜单，不含按钮）
     */
    private List<MenuDTO> menus;

    /**
     * 用户权限字符串列表（主要用于按钮权限控制）
     */
    private List<String> permissions;

}