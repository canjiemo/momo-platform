package io.github.canjiemo.momo.system.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.AuditFill;
import io.github.canjiemo.base.myjdbc.annotation.MyField;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统角色实体类
 * 对应数据库表 sys_role
 *
 * @author canjiemo@gmail.com
 */
@Data
@MyTable("sys_role")
public class SysRole implements MyTableEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID，NULL表示平台角色模板
     */
    private Long tenantId;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色编码，同租户内唯一，用于数据权限识别
     */
    private String roleCode;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    /**
     * 逻辑删除：0正常 1删除
     */
    private Integer deleteFlag;

    @MyField(fill = AuditFill.CREATE_BY)
    private Long createBy;

    @MyField(fill = AuditFill.CREATE_TIME)
    private LocalDateTime createTime;

    @MyField(fill = AuditFill.UPDATE_BY)
    private Long updateBy;

    @MyField(fill = AuditFill.UPDATE_TIME)
    private LocalDateTime updateTime;
}