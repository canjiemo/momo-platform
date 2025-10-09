package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 重置密码请求DTO
 *
 * @author seer-fitness
 */
@Data
public class ResetPasswordRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;  // 明文密码，通过HTTPS传输
}