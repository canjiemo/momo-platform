package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 菜单删除请求参数
 *
 * @author seer-fitness
 */
@Data
public class MenuDeleteRequest {

    /**
     * 要删除的菜单ID列表
     * 不能为空，至少要有一个ID
     */
    @NotEmpty(message = "删除的菜单ID列表不能为空")
    private String[] ids;
}