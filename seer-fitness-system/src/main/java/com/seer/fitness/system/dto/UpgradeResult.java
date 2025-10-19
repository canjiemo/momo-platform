package com.seer.fitness.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Schema升级结果DTO
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeResult {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * Schema名称
     */
    private String schemaName;

    /**
     * 升级前版本
     */
    private String fromVersion;

    /**
     * 升级后版本
     */
    private String toVersion;

    /**
     * 执行的迁移数量
     */
    private Integer migrationsExecuted;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 升级时间
     */
    private LocalDateTime upgradedAt;

    /**
     * 执行耗时（毫秒）
     */
    private Integer executionTime;
}
