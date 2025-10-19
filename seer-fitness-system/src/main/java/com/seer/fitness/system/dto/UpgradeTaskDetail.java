package com.seer.fitness.system.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 升级任务详情DTO（包含日志）
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Data
public class UpgradeTaskDetail {

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
     * 目标Schema列表
     */
    private List<String> targetSchemas;

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
     * 任务状态
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

    /**
     * 升级日志列表
     */
    private List<UpgradeLogDTO> logs;

    /**
     * 升级日志DTO
     */
    @Data
    public static class UpgradeLogDTO {
        private Long id;
        private String schemaName;
        private String fromVersion;
        private String toVersion;
        private Integer migrationsExecuted;
        private String status;
        private String errorMessage;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer executionTime;
    }
}
