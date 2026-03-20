package io.github.canjiemo.momo.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 菜单树VO
 *
 * @author canjiemo@gmail.com
 */
@Data
public class MenuTreeVO {

    /**
     * 菜单ID
     */
    private Long id;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 前端路由路径
     */
    private String path;

    /**
     * 父菜单ID
     */
    private Long parentId;

    /**
     * 类型：0目录 1菜单 2按钮
     * 目录 - 用于菜单分组，无实际路由
     * 菜单 - 对应具体页面路由
     * 按钮 - 对应操作权限
     */
    private Integer type;

    /**
     * 权限字符
     */
    private String permission;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 排序字段
     */
    private Integer sortOrder;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    /**
     * 子菜单列表
     */
    private List<MenuTreeVO> children;
}