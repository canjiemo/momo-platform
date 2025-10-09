package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 组织架构更新请求参数
 * 包含完整的验证注解和业务字段
 *
 * @author seer-fitness
 */
@Data
public class OrganizationUpdateRequest {

    /**
     * 组织ID
     */
    @NotNull(message = "组织ID不能为空")
    private Long id;

    /**
     * 组织编码，唯一标识
     */
    @Size(max = 50, message = "组织编码长度不能超过50个字符")
    @Pattern(regexp = "^$|^[A-Z0-9_]+$", message = "组织编码只能包含大写字母、数字和下划线")
    private String orgCode;

    /**
     * 组织名称
     */
    @NotBlank(message = "组织名称不能为空")
    @Size(max = 100, message = "组织名称长度不能超过100个字符")
    private String orgName;


    /**
     * 父组织ID，"0"表示顶级组织
     */
    private Long parentId;

    /**
     * 排序字段
     */
    private Integer sortOrder;

    /**
     * 负责人用户ID
     */
    private Long leaderId;

    /**
     * 联系电话
     */
    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    @Pattern(regexp = "^[0-9-+()\\s]*$", message = "联系电话格式不正确")
    private String contactPhone;

    /**
     * 邮箱
     */
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    @Pattern(regexp = "^$|^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "邮箱格式不正确")
    private String email;

    /**
     * 办公地址
     */
    @Size(max = 200, message = "办公地址长度不能超过200个字符")
    private String address;

    /**
     * 组织描述
     */
    @Size(max = 500, message = "组织描述长度不能超过500个字符")
    private String description;

    /**
     * 状态：1启用 0禁用
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}