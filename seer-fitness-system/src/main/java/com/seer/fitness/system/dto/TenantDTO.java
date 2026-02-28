package com.seer.fitness.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户响应DTO
 * 返回给前端的租户数据
 *
 * @author seer-fitness
 */
@Data
public class TenantDTO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户编码
     */
    private String tenantCode;

    /**
     * 租户名称（学校名称）
     */
    private String tenantName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系邮箱
     */
    private String contactEmail;

    /**
     * 学校地址
     */
    private String address;

    /**
     * 租户描述
     */
    private String description;

    /**
     * 状态码：0-待激活 1-正常 2-已禁用 3-已过期
     */
    private Integer status;

    /**
     * 状态文本：待激活/正常/已禁用/已过期
     */
    private String statusText;

    /**
     * 激活时间
     */
    private LocalDateTime activatedAt;

    /**
     * 过期时间
     */
    private LocalDateTime expiredAt;

    /**
     * 最大用户数限制
     */
    private Integer maxUsers;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 更新人ID
     */
    private Long updatedBy;
}
