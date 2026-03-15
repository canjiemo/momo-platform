package com.seer.fitness.system.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.AuditFill;
import io.github.canjiemo.base.myjdbc.annotation.MyField;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户-平台角色映射实体
 * 记录平台分配给每个租户的平台角色，租户管理员的菜单权限由此动态决定
 *
 * @author seer-fitness
 */
@Data
@MyTable("sys_tenant_role")
public class SysTenantRole implements MyTableEntity {

    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 平台角色ID（对应 sys_role.id，且 sys_role.tenant_id=NULL） */
    private Long roleId;

    @MyField(fill = AuditFill.CREATE_TIME)
    private LocalDateTime createTime;
}
