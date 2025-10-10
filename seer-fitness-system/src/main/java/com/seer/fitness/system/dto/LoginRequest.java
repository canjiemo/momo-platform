package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求DTO
 *
 * @author seer-fitness
 */
@Data
public class LoginRequest {

    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;  // 租户编码（必填）

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;  // 明文密码，通过HTTPS传输

    @NotBlank(message = "验证码不能为空")
    private String captcha;   // 验证码

    @NotBlank(message = "验证码ID不能为空")
    private String captchaId; // 验证码ID
}