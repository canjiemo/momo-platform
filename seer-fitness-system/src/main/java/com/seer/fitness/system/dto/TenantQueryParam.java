package com.seer.fitness.system.dto;

import io.github.canjiemo.mycommon.pager.PagerParam;
import lombok.Data;

/**
 * 租户查询参数DTO
 *
 * @author seer-fitness
 */
@Data
public class TenantQueryParam extends PagerParam {

    /**
     * 租户编码（模糊查询）
     */
    private String tenantCode;

    /**
     * 租户名称（模糊查询）
     */
    private String tenantName;

    /**
     * 状态：0-待激活 1-正常 2-已禁用 3-已过期
     */
    private Integer status;

    /**
     * 联系电话（模糊查询）
     */
    private String contactPhone;

}
