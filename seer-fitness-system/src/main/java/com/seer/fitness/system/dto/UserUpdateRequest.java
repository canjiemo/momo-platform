package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户更新请求DTO
 *
 * @author seer-fitness
 */
@Data
public class UserUpdateRequest {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    @Size(max = 50, message = "真实姓名长度不能超过50位")
    private String realName;

    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 管理员标识：1管理员 0普通用户
     */
    private Boolean adminFlag;

    /**
     * 所属组织ID（非必填）
     */
    private Long orgId;

    /**
     * 用户类型：0-运维人员 1-教师 2-学生（非必填）
     * @see com.seer.fitness.framework.enums.UserType
     */
    private Integer userType;

    /**
     * 角色ID列表
     */
    private List<String> roleIds;
}