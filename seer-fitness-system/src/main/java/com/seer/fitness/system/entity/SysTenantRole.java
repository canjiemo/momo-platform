package com.seer.fitness.system.entity;

import com.seer.fitness.framework.annotation.PublicSchema;
import io.github.mocanjie.base.myjpa.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户角色同步关联表实体
 * 对应数据库表 public.sys_tenant_role
 * <p>
 * 记录平台角色同步给租户的关系
 * 同步后，角色数据会复制到租户的schema中
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Data
@MyTable("sys_tenant_role")
@PublicSchema(reason = "租户角色同步关联表")
public class SysTenantRole {

    /**
     * 主键ID（雪花算法）
     */
    private Long id;

    /**
     * 租户ID（关联 sys_tenant.id）
     */
    private Long tenantId;

    /**
     * 平台角色ID（关联 public.sys_role.id）
     */
    private Long platformRoleId;

    /**
     * 同步时间
     */
    private LocalDateTime syncedAt;

    /**
     * 同步人ID（平台管理员）
     */
    private Long syncedBy;
}
