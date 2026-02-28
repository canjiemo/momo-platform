package com.seer.fitness.system.entity;

import io.github.mocanjie.base.myjpa.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色菜单关联实体类
 * 对应数据库表 sys_role_menu
 *
 * @author seer-fitness
 */
@Data
@MyTable("sys_role_menu")
public class SysRoleMenu {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 角色ID(关联sys_role.id)
     */
    private Long roleId;

    /**
     * 菜单ID(关联sys_menu.id)
     */
    private Long menuId;

    /**
     * 分配人ID（谁分配的权限）
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}