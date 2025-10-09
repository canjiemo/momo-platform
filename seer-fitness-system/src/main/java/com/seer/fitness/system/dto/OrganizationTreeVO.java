package com.seer.fitness.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 组织架构树VO
 * 用于前端树形组件展示
 *
 * @author seer-fitness
 */
@Data
public class OrganizationTreeVO {

    /**
     * 组织ID
     */
    private Long id;

    /**
     * 组织编码
     */
    private String orgCode;

    /**
     * 组织名称
     */
    private String orgName;


    /**
     * 父组织ID
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
     * 负责人姓名
     */
    private String leaderName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    /**
     * 子组织数量
     */
    private Integer childrenCount;

    /**
     * 人员数量
     */
    private Integer memberCount;

    /**
     * 子组织列表
     */
    private List<OrganizationTreeVO> children;
}