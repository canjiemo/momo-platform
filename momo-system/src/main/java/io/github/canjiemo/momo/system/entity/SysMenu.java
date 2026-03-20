package io.github.canjiemo.momo.system.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.AuditFill;
import io.github.canjiemo.base.myjdbc.annotation.MyField;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统菜单实体类
 * 对应数据库表 sys_menu
 *
 * @author canjiemo@gmail.com
 */
@Data
@MyTable("sys_menu")
public class SysMenu implements MyTableEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID，NULL表示平台菜单模板
     */
    private Long tenantId;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 前端路由路径或接口路径
     */
    private String path;

    /**
     * 父菜单ID, 0为一级菜单
     */
    private Long parentId;

    /**
     * 类型：0目录 1菜单 2按钮
     */
    private Integer type;

    /**
     * 权限字符，例如 user:create
     */
    private String permission;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 排序字段，数值越小越靠前
     */
    private Integer sortOrder;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    /**
     * 逻辑删除：0正常 1删除
     */
    private Integer deleteFlag;

    @MyField(fill = AuditFill.CREATE_BY)
    private Long createdBy;

    @MyField(fill = AuditFill.CREATE_TIME)
    private LocalDateTime createTime;

    @MyField(fill = AuditFill.UPDATE_BY)
    private Long updatedBy;

    @MyField(fill = AuditFill.UPDATE_TIME)
    private LocalDateTime updateTime;
}