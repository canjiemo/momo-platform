package com.seer.fitness.system.entity;

import io.github.mocanjie.base.myjpa.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统菜单实体类
 * 对应数据库表 sys_menu
 *
 * @author seer-fitness
 */
@Data
@MyTable("sys_menu")
public class SysMenu {

    /**
     * 主键ID
     */
    private Long id;

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
     * 菜单类型：1-平台菜单 2-租户模板菜单
     * (2025-10-17 新增)
     */
    private Integer menuType;

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
     * 功能级别：1-基础版 2-标准版 3-企业版
     * (2025-10-17 新增)
     */
    private Integer featureLevel;

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