package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 角色创建请求DTO
 *
 * @author seer-fitness
 */
@Data
public class RoleCreateRequest {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50位")
    private String roleName;

    @Size(max = 255, message = "角色描述长度不能超过255位")
    private String description;

    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 菜单ID列表
     */
    private List<String> menuIds;
}