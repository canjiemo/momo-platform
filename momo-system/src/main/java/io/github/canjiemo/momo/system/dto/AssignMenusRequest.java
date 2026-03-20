package io.github.canjiemo.momo.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 分配菜单权限请求DTO
 *
 * @author canjiemo@gmail.com
 */
@Data
public class AssignMenusRequest {

    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /**
     * 菜单ID列表
     */
    private List<String> menuIds;
}