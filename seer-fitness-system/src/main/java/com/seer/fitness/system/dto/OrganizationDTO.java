package com.seer.fitness.system.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 组织架构DTO
 * 用于数据传输和前端展示
 *
 * @author seer-fitness
 */
@Data
public class OrganizationDTO {

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
     * 父组织ID，0表示顶级组织
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
     * 邮箱
     */
    private String email;

    /**
     * 办公地址
     */
    private String address;

    /**
     * 组织描述
     */
    private String description;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 子组织数量
     */
    private Integer childrenCount;

    /**
     * 人员数量
     */
    private Integer memberCount;
}