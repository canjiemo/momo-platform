package io.github.canjiemo.momo.system.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.AuditFill;
import io.github.canjiemo.base.myjdbc.annotation.MyField;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色菜单关联实体类
 * 对应数据库表 sys_role_menu
 *
 * @author canjiemo@gmail.com
 */
@Data
@MyTable("sys_role_menu")
public class SysRoleMenu implements MyTableEntity {

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

    @MyField(fill = AuditFill.CREATE_BY)
    private Long createdBy;

    @MyField(fill = AuditFill.CREATE_TIME)
    private LocalDateTime createTime;
}