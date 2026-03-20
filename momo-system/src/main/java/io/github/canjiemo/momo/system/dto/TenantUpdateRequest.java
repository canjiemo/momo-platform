package io.github.canjiemo.momo.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 租户更新请求DTO
 *
 * @author canjiemo@gmail.com
 */
@Data
public class TenantUpdateRequest {

    @NotNull(message = "租户ID不能为空")
    private Long id;

    // tenant_name 创建后不可修改，此处不提供该字段

    @Size(max = 50, message = "管理员姓名长度不能超过50位")
    private String realName;

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
     * 最大用户数限制
     */
    private Integer maxUsers;

    /**
     * 过期时间
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    private String expiredAt;
}
