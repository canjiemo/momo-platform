package com.seer.fitness.business.entity;

import io.github.mocanjie.base.myjpa.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目信息实体类
 * 对应数据库表 seer_project_info
 *
 * 说明：
 * - 平台版本：存储在 public.seer_project_info（平台项目库）
 * - 租户版本：存储在 school_xxx.seer_project_info（学校实际项目）
 * - 表结构相同，通过 Schema 隔离
 *
 * @author seer-fitness
 */
@Data
@MyTable("seer_project_info")
public class SeerProjectInfo {

    /**
     * 项目ID
     */
    private Long id;

    /**
     * 项目编号（唯一标识）
     * 示例：pull_up, standing_jump, run_50m
     */
    private String projectCode;

    /**
     * 项目名称
     * 示例：引体向上、立定跳远、50米跑
     */
    private String projectName;

    /**
     * 单位（字典：unit_type）
     * 1=次，2=厘米，3=秒，4=米，5=分钟，6=千克
     */
    private Integer unit;

    /**
     * 训练时长（秒）
     * 建议的训练持续时间
     */
    private Integer trainingDuration;

    /**
     * 成绩越大越好（字典：yes_no）
     * 1=是（如跳远、引体向上），0=否（如跑步时间）
     */
    private Integer isHigherBetter;

    /**
     * 排序
     * 数字越小越靠前
     */
    private Integer sortOrder;

    /**
     * 状态（字典：enable_status）
     * 0=禁用，1=启用
     */
    private Integer status;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新人ID
     */
    private Long updatedBy;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 备注
     */
    private String remark;
}
