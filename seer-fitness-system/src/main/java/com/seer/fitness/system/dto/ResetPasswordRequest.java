package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 重置密码请求DTO（当前登录用户重置自己的密码）
 *
 * @author seer-fitness
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "新密码不能为空")
    private String newPassword;  // 明文密码，通过HTTPS传输
}