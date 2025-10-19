package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.UpgradeTaskDTO;
import com.seer.fitness.system.dto.UpgradeTaskDetail;
import com.seer.fitness.system.dto.UpgradeTaskQueryParam;
import com.seer.fitness.system.entity.SysUpgradeTask;
import com.seer.fitness.system.entity.SysUpgradeTaskLog;
import io.github.mocanjie.base.mycommon.pager.Pager;

/**
 * 升级任务管理服务接口
 * 负责创建、更新、查询升级任务及日志
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
public interface IUpgradeTaskService {

    /**
     * 创建升级任务
     *
     * @param task 任务对象
     * @return 任务ID
     */
    Long createTask(SysUpgradeTask task);

    /**
     * 更新任务状态
     *
     * @param taskId 任务ID
     * @param status 新状态
     */
    void updateTaskStatus(Long taskId, String status);

    /**
     * 更新任务统计信息
     *
     * @param taskId       任务ID
     * @param successCount 成功数量
     * @param failedCount  失败数量
     */
    void updateTaskStats(Long taskId, Integer successCount, Integer failedCount);

    /**
     * 记录Schema升级日志
     *
     * @param log 日志对象
     */
    void logSchemaUpgrade(SysUpgradeTaskLog log);

    /**
     * 更新Schema升级日志状态
     *
     * @param logId        日志ID
     * @param status       状态
     * @param errorMessage 错误信息
     */
    void updateLogStatus(Long logId, String status, String errorMessage);

    /**
     * 查询任务详情
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    UpgradeTaskDetail getTaskDetail(Long taskId);

    /**
     * 根据ID查询任务
     *
     * @param taskId 任务ID
     * @return 任务对象
     */
    SysUpgradeTask getTaskById(Long taskId);

    /**
     * 分页查询升级历史
     *
     * @param param 查询参数
     * @param pager 分页对象
     * @return 分页结果
     */
    Pager<UpgradeTaskDTO> searchHistory(UpgradeTaskQueryParam param, Pager pager);
}
