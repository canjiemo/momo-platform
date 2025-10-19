package com.seer.fitness.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 升级任务DTO（列表展示）
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Data
public class UpgradeTaskDTO {

    /**
     * 任务ID
     */
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 目标版本
     */
    private String targetVersion;

    /**
     * 起始版本
     */
    private String fromVersion;

    /**
     * 升级类型：SINGLE-单个 BATCH-批量 ALL-全部
     */
    private String upgradeType;

    /**
     * 总Schema数量
     */
    private Integer totalSchemas;

    /**
     * 成功数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failedCount;

    /**
     * 任务状态：PENDING-待执行 RUNNING-执行中 COMPLETED-已完成 FAILED-失败 CANCELLED-已取消
     */
    private String status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 完成时间
     */
    private LocalDateTime endTime;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
