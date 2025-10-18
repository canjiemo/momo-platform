package com.seer.fitness.system.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色DTO
 *
 * @author seer-fitness
 */
@Data
public class RoleDTO {

    /**
     * 角色ID
     */
    private Long id;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 角色类型：1=平台角色 2=租户模板角色
     */
    private Integer roleType;

    /**
     * 功能级别：1=基础版 2=标准版 3=企业版
     */
    private Integer featureLevel;

    /**
     * 平台角色ID（租户侧使用，用于判断是否为平台同步的角色）
     * 如果不为null，表示该角色由平台同步而来，租户不能修改/删除
     */
    private Long platformRoleId;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新人ID
     */
    private Long updatedBy;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}