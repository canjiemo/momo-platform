package io.github.canjiemo.momo.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 角色更新请求DTO
 *
 * @author canjiemo@gmail.com
 */
@Data
public class RoleUpdateRequest {

    @NotNull(message = "角色ID不能为空")
    private Long id;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50位")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码长度不能超过50位")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "角色编码只能包含大写字母、数字和下划线，且以大写字母开头")
    private String roleCode;

    @Size(max = 255, message = "角色描述长度不能超过255位")
    private String description;

    /**
     * 角色类型：1=平台角色 2=租户模板角色
     */
    private Integer roleType;

    /**
     * 功能级别：1=基础版 2=标准版 3=企业版
     */
    private Integer featureLevel;

    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 菜单ID列表
     */
    private List<String> menuIds;
}