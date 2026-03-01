package com.seer.fitness.system.dto;

import io.github.mocanjie.base.mycommon.pager.PagerParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 组织架构查询参数
 * 继承分页参数，支持复杂查询条件
 *
 * @author seer-fitness
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrganizationQueryParam extends PagerParam {

    /**
     * 组织编码（模糊查询）
     */
    private String orgCode;

    /**
     * 组织名称（模糊查询）
     */
    private String orgName;


    /**
     * 父组织ID
     */
    private Long parentId;

    /**
     * 负责人用户ID
     */
    private Long leaderId;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    /**
     * 租户ID（平台管理员专用，指定后只查该租户的组织；非平台用户传此参数无效）
     */
    private Long tenantId;
}