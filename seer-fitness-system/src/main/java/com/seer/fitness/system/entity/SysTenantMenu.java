package com.seer.fitness.system.entity;

import com.seer.fitness.framework.annotation.PublicSchema;
import io.github.mocanjie.base.myjpa.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户菜单分配关联表实体
 * 对应数据库表 public.sys_tenant_menu
 * <p>
 * 记录平台菜单分配给租户的关系
 * 分配后，菜单数据会复制到租户的schema中
 *
 * @author seer-fitness
 * @since 2025-10-17
 */
@Data
@MyTable("sys_tenant_menu")
@PublicSchema(reason = "租户菜单分配关联表")
public class SysTenantMenu {

    /**
     * 主键ID（雪花算法）
     */
    private Long id;

    /**
     * 租户ID（关联 sys_tenant.id）
     */
    private Long tenantId;

    /**
     * 平台菜单ID（关联 public.sys_menu.id）
     */
    private Long platformMenuId;

    /**
     * 分配时间
     */
    private LocalDateTime assignedAt;

    /**
     * 分配人ID（平台管理员）
     */
    private Long assignedBy;
}
