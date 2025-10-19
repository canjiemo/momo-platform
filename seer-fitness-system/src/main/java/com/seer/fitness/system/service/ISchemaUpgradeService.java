package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.UpgradeResult;
import com.seer.fitness.system.dto.UpgradeTaskDetail;

import java.util.List;

/**
 * Schema升级服务接口
 * 负责执行租户Schema的版本升级
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
public interface ISchemaUpgradeService {

    /**
     * 升级单个Schema到指定版本
     *
     * @param schemaName    Schema名称
     * @param targetVersion 目标版本（null表示最新版本）
     * @return 升级结果
     */
    UpgradeResult upgradeSchema(String schemaName, String targetVersion);

    /**
     * 批量升级多个Schema
     *
     * @param taskName      任务名称
     * @param schemaNames   Schema名称列表
     * @param targetVersion 目标版本（null表示最新版本）
     * @return 升级任务ID
     */
    Long batchUpgradeSchemas(String taskName, List<String> schemaNames, String targetVersion);

    /**
     * 升级所有租户Schema
     *
     * @param taskName      任务名称
     * @param targetVersion 目标版本（null表示最新版本）
     * @return 升级任务ID
     */
    Long upgradeAllSchemas(String taskName, String targetVersion);

    /**
     * 查询升级任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    UpgradeTaskDetail getTaskStatus(Long taskId);

    /**
     * 取消升级任务
     *
     * @param taskId 任务ID
     */
    void cancelTask(Long taskId);
}
