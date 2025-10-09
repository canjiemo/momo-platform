package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员初始化密码请求DTO
 *
 * @author seer-fitness
 */
@Data
public class InitPasswordRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;  // 要初始化密码的用户ID
}