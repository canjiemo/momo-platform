package com.seer.fitness.system.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.AuditFill;
import io.github.canjiemo.base.myjdbc.annotation.MyField;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户角色关联实体类
 * 对应数据库表 sys_user_role
 *
 * @author seer-fitness
 */
@Data
@MyTable("sys_user_role")
public class SysUserRole implements MyTableEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID(关联sys_user.id)
     */
    private Long userId;

    /**
     * 角色ID(关联sys_role.id)
     */
    private Long roleId;

    @MyField(fill = AuditFill.CREATE_BY)
    private Long createdBy;

    @MyField(fill = AuditFill.CREATE_TIME)
    private LocalDateTime createTime;
}