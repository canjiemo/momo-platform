package com.seer.fitness.business.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目信息DTO
 * 用于前端展示
 *
 * @author seer-fitness
 */
@Data
public class ProjectInfoDTO {

    /**
     * 项目ID
     */
    private Long id;

    /**
     * 项目编号
     */
    private String projectCode;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 单位（字典：unit_type）
     */
    private Integer unit;

    /**
     * 训练时长（秒）
     */
    private Integer trainingDuration;

    /**
     * 成绩越大越好（字典：yes_no）
     */
    private Integer isHigherBetter;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
