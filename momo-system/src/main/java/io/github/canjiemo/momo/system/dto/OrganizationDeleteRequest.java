package io.github.canjiemo.momo.system.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 组织架构删除请求参数
 * 使用统一的删除请求封装模式
 *
 * @author canjiemo@gmail.com
 */
@Data
public class OrganizationDeleteRequest {

    /**
     * 要删除的组织ID数组
     */
    @NotEmpty(message = "删除的组织ID不能为空")
    private String[] ids;
}