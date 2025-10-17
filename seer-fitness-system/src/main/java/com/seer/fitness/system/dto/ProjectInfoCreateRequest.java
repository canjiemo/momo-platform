package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建项目请求
 *
 * @author seer-fitness
 */
@Data
public class ProjectInfoCreateRequest {

    /**
     * 项目编号
     */
    @NotBlank(message = "项目编号不能为空")
    private String projectCode;

    /**
     * 项目名称
     */
    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    /**
     * 单位（字典：unit_type）
     */
    @NotNull(message = "单位不能为空")
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
}
