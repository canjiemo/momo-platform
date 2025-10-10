package com.seer.fitness.system.dto;

import lombok.Data;

/**
 * 租户查询参数DTO
 *
 * @author seer-fitness
 */
@Data
public class TenantQueryParam {

    /**
     * 租户编码（模糊查询）
     */
    private String tenantCode;

    /**
     * 租户名称（模糊查询）
     */
    private String tenantName;

    /**
     * Schema名称（精确查询）
     */
    private String schemaName;

    /**
     * 状态：0-待激活 1-正常 2-已禁用 3-已过期
     */
    private Integer status;

    /**
     * 管理员用户名（模糊查询）
     */
    private String adminUsername;

    /**
     * 联系电话（模糊查询）
     */
    private String contactPhone;

    /**
     * 是否包含已删除的数据
     */
    private Boolean includeDeleted = false;

    /**
     * 分页参数：页码（从1开始）
     */
    private Integer page = 1;

    /**
     * 分页参数：每页大小
     */
    private Integer pageSize = 20;

    /**
     * 排序字段
     */
    private String sortField = "created_at";

    /**
     * 排序方向：asc/desc
     */
    private String sortOrder = "desc";
}
