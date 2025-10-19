package com.seer.fitness.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Schema回滚结果DTO
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RollbackResult {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * Schema名称
     */
    private String schemaName;

    /**
     * 回滚前版本
     */
    private String fromVersion;

    /**
     * 回滚后版本
     */
    private String toVersion;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 回滚时间
     */
    private LocalDateTime rolledBackAt;

    /**
     * 执行耗时（毫秒）
     */
    private Integer executionTime;
}
