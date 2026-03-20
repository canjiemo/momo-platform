package io.github.canjiemo.momo.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 个人修改密码请求DTO
 *
 * @author canjiemo@gmail.com
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;  // 当前密码（明文）

    @NotBlank(message = "新密码不能为空")
    private String newPassword;  // 新密码（明文）
}