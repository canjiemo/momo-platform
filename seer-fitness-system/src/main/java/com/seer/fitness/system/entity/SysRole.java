package com.seer.fitness.system.entity;

import io.github.mocanjie.base.myjpa.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统角色实体类
 * 对应数据库表 sys_role
 *
 * @author seer-fitness
 */
@Data
@MyTable("sys_role")
public class SysRole {

    /**
     * 主键ID
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
     * 平台角色ID（仅租户Schema使用）
     * 记录该角色由哪个平台角色同步而来，用于标识平台同步的角色（只读）
     * public.sys_role 中此字段为 NULL
     */
    private Long platformRoleId;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    /**
     * 逻辑删除：0正常 1删除
     */
    private Integer deleteFlag;

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