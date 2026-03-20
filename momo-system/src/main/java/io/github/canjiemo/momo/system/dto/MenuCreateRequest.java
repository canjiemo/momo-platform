package io.github.canjiemo.momo.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 菜单创建请求DTO
 *
 * @author canjiemo@gmail.com
 */
@Data
public class MenuCreateRequest {

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过50位")
    private String menuName;

    @Size(max = 255, message = "路径长度不能超过255位")
    private String path;

    /**
     * 父菜单ID, NULL为一级菜单
     */
    private Long parentId;

    /**
     * 类型：0目录 1菜单 2按钮
     */
    @NotNull(message = "菜单类型不能为空")
    private Integer type;

    /**
     * 菜单类型：1-平台菜单 2-租户模板菜单
     * (2025-10-17 新增)
     */
    private Integer menuType;

    /**
     * 权限字符，用于后端权限校验
     */
    @Size(max = 100, message = "权限字符长度不能超过100位")
    private String permission;

    /**
     * 菜单图标
     */
    @Size(max = 100, message = "图标长度不能超过100位")
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

    @NotNull(message = "状态不能为空")
    private Integer status;
}