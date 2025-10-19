package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Schema升级请求DTO
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Data
public class UpgradeRequest {

    /**
     * 任务名称
     */
    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    /**
     * 升级类型：SINGLE-单个 BATCH-批量 ALL-全部
     */
    @NotBlank(message = "升级类型不能为空")
    private String upgradeType;

    /**
     * 目标版本（null表示升级到最新版本）
     */
    private String targetVersion;

    /**
     * 目标Schema列表（upgradeType=SINGLE或BATCH时必填）
     */
    private List<String> targetSchemas;

    /**
     * 是否在失败时自动回滚
     */
    private Boolean autoRollbackOnFailure = false;
}
