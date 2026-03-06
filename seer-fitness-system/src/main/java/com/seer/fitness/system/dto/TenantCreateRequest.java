package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 租户创建请求DTO
 *
 * @author seer-fitness
 */
@Data
public class TenantCreateRequest {

    @NotBlank(message = "租户编码不能为空")
    @Size(min = 3, max = 50, message = "租户编码长度为3-50位")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "租户编码必须以字母开头，只能包含字母、数字和下划线")
    private String tenantCode;

    /**
     * 租户管理员登录账号，创建后不可修改
     */
    @NotBlank(message = "管理员账号不能为空")
    @Size(min = 3, max = 50, message = "管理员账号长度为3-50位")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "管理员账号必须以字母开头，只能包含字母、数字和下划线")
    private String tenantName;

    @Size(max = 20, message = "联系电话长度不能超过20位")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String contactPhone;

    @Size(max = 100, message = "联系邮箱长度不能超过100位")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "联系邮箱格式不正确")
    private String contactEmail;

    @Size(max = 200, message = "学校地址长度不能超过200位")
    private String address;

    @Size(max = 500, message = "租户描述长度不能超过500位")
    private String description;

    /**
     * 学校中文名称，如"某某学校"（可选，作为管理员账号的显示名称）
     */
    @Size(max = 50, message = "学校名称长度不能超过50位")
    private String realName;

    /**
     * 最大用户数限制（可选，默认1000）
     */
    private Integer maxUsers;

    /**
     * 过期时间（可选，不填表示永不过期）
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    private String expiredAt;

    /**
     * 授权给租户的平台角色ID列表（必填，至少一个）
     * 这些角色决定租户管理员可以访问哪些菜单
     */
    @NotEmpty(message = "必须为租户分配至少一个平台角色")
    private List<Long> roleIds;
}
