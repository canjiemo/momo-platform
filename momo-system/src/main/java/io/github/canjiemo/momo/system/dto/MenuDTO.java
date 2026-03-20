package io.github.canjiemo.momo.system.dto;

import io.github.canjiemo.tools.dict.MyDict;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单DTO
 *
 * @author canjiemo@gmail.com
 */
@Data
public class MenuDTO {

    /**
     * 菜单ID
     * (FastJSON2 已配置 WriteLongAsString，JSON 输出为字符串，无精度问题)
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
     * 父菜单ID
     */
    private Long parentId;

    /**
     * 类型：0目录 1菜单 2按钮
     * 目录 - 用于菜单分组，无实际路由
     * 菜单 - 对应具体页面路由
     * 按钮 - 对应操作权限
     */
    @MyDict(type = "menu_type")
    private Integer type;

    /**
     * 菜单类型：1-平台菜单 2-租户模板菜单
     * (2025-10-17 新增)
     */
    @MyDict(type = "menu_display_type")
    private Integer menuType;

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
     * 功能级别：1-基础版 2-标准版 3-企业版
     * (2025-10-17 新增)
     */
    private Integer featureLevel;

    /**
     * 状态：1启用 0禁用
     */
    @MyDict(type = "common_status")
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

//    /**
//     * 子菜单列表
//     */
//    private List<MenuDTO> children;

}