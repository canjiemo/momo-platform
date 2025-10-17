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

    /**
     * 租户编码（可选）
     * - 为空或不传：平台管理员登录（查询 public.sys_user）
     * - 有值：租户用户登录（查询 tenant_schema.sys_user）
     */
    private String tenantCode;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;  // 明文密码，通过HTTPS传输

    @NotBlank(message = "验证码不能为空")
    private String captcha;   // 验证码

    @NotBlank(message = "验证码ID不能为空")
    private String captchaId; // 验证码ID
}