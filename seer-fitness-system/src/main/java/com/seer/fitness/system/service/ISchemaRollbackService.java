package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.RollbackResult;

import java.util.List;

/**
 * Schema回滚服务接口
 * 负责执行租户Schema的版本回滚
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
public interface ISchemaRollbackService {

    /**
     * 回滚Schema到指定版本
     *
     * @param schemaName    Schema名称
     * @param targetVersion 目标版本
     * @return 回滚结果
     */
    RollbackResult rollbackSchema(String schemaName, String targetVersion);

    /**
     * 验证回滚安全性
     *
     * @param schemaName    Schema名称
     * @param targetVersion 目标版本
     * @return 是否安全
     */
    boolean validateRollback(String schemaName, String targetVersion);

    /**
     * 获取可回滚的版本列表
     *
     * @param schemaName Schema名称
     * @return 版本列表
     */
    List<String> getAvailableVersions(String schemaName);
}
