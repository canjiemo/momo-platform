package io.github.canjiemo.momo.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户创建请求DTO
 *
 * @author canjiemo@gmail.com
 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度为3-50位")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;  // 明文密码，通过HTTPS传输

    @Size(max = 50, message = "真实姓名长度不能超过50位")
    private String realName;

    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 管理员标识：1管理员 0普通用户
     */
    private Boolean adminFlag = false;

    /**
     * 所属组织ID（非必填）
     */
    private Long orgId;

    /**
     * 用户类型：0-运维人员 1-教师 2-学生（非必填，默认0）
     * @see io.github.canjiemo.momo.framework.enums.UserType
     */
    private Integer userType;

    /**
     * 角色ID列表
     */
    private List<String> roleIds;
}