package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Schema回滚请求DTO
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Data
public class RollbackRequest {

    /**
     * Schema名称
     */
    @NotBlank(message = "Schema名称不能为空")
    private String schemaName;

    /**
     * 目标版本（回滚到哪个版本）
     */
    @NotBlank(message = "目标版本不能为空")
    private String targetVersion;

    /**
     * 是否强制回滚（忽略安全检查）
     */
    private Boolean forceRollback = false;
}
