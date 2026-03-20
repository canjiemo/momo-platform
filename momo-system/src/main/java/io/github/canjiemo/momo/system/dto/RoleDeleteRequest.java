package io.github.canjiemo.momo.system.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 角色删除请求参数
 *
 * @author canjiemo@gmail.com
 */
@Data
public class RoleDeleteRequest {

    /**
     * 要删除的角色ID列表
     * 不能为空，至少要有一个ID
     */
    @NotEmpty(message = "删除的角色ID列表不能为空")
    private String[] ids;
}