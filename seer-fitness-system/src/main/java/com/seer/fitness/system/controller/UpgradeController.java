package com.seer.fitness.system.controller;

import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.system.dto.*;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.system.service.ISchemaRollbackService;
import com.seer.fitness.system.service.ISchemaUpgradeService;
import com.seer.fitness.system.service.IUpgradeTaskService;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.mycommon.pager.PagerHandler;
import io.github.mocanjie.base.mymvc.controller.MyBaseController;
import io.github.mocanjie.base.mymvc.data.MyResponseResult;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Schema升级管理控制器
 * 提供租户Schema批量升级、回滚、任务管理等功能
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Slf4j
@RestController
@RequestMapping("/platform/upgrade")
public class UpgradeController extends MyBaseController {

    @Autowired
    private ISchemaUpgradeService schemaUpgradeService;

    @Autowired
    private ISchemaRollbackService schemaRollbackService;

    @Autowired
    private IUpgradeTaskService upgradeTaskService;

    /**
     * 执行批量升级
     *
     * @param request 升级请求
     * @return 升级任务ID
     */
    @PostMapping("/execute")
    @RequireAuth(permissions = {"upgrade:execute"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "upgrade",
        description = "执行Schema批量升级"
    )
    public MyResponseResult<Long> executeUpgrade(@Valid @RequestBody UpgradeRequest request) {
        try {
            log.info("收到升级请求: taskName={}, upgradeType={}, targetVersion={}",
                    request.getTaskName(), request.getUpgradeType(), request.getTargetVersion());

            Long taskId = null;

            switch (request.getUpgradeType()) {
                case "SINGLE":
                case "BATCH":
                    if (request.getTargetSchemas() == null || request.getTargetSchemas().isEmpty()) {
                        throw new BusinessException("批量升级必须指定目标Schema列表");
                    }
                    taskId = schemaUpgradeService.batchUpgradeSchemas(
                            request.getTaskName(),
                            request.getTargetSchemas(),
                            request.getTargetVersion()
                    );
                    break;

                case "ALL":
                    taskId = schemaUpgradeService.upgradeAllSchemas(
                            request.getTaskName(),
                            request.getTargetVersion()
                    );
                    break;

                default:
                    throw new BusinessException("不支持的升级类型: " + request.getUpgradeType());
            }

            return super.doJsonOut(taskId);
        } catch (Exception e) {
            log.error("执行升级失败", e);
            throw new BusinessException("执行升级失败: " + e.getMessage());
        }
    }

    /**
     * 查询升级任务状态
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    @GetMapping("/task/{taskId}")
    @RequireAuth(permissions = {"upgrade:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "upgrade",
        description = "查询升级任务状态"
    )
    public MyResponseResult<UpgradeTaskDetail> getTaskStatus(@PathVariable Long taskId) {
        try {
            UpgradeTaskDetail detail = schemaUpgradeService.getTaskStatus(taskId);
            return super.doJsonOut(detail);
        } catch (Exception e) {
            log.error("查询任务状态失败: taskId={}", taskId, e);
            throw new BusinessException("查询任务状态失败: " + e.getMessage());
        }
    }

    /**
     * 查询升级历史
     *
     * @param param 查询参数
     * @return 分页结果
     */
    @PostMapping("/history")
    @RequireAuth(permissions = {"upgrade:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "upgrade",
        description = "查询升级历史"
    )
    public MyResponseResult<Pager<UpgradeTaskDTO>> getUpgradeHistory(@RequestBody UpgradeTaskQueryParam param) {
        return super.doJsonPagerOut(upgradeTaskService.searchHistory(param, PagerHandler.createPager(param)));
    }

    /**
     * 取消升级任务
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @PostMapping("/cancel/{taskId}")
    @RequireAuth(permissions = {"upgrade:execute"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "upgrade",
        description = "取消升级任务"
    )
    public MyResponseResult<Void> cancelTask(@PathVariable Long taskId) {
        try {
            schemaUpgradeService.cancelTask(taskId);
            return super.doJsonOut(null);
        } catch (Exception e) {
            log.error("取消任务失败: taskId={}", taskId, e);
            throw new BusinessException("取消任务失败: " + e.getMessage());
        }
    }

    /**
     * 回滚Schema
     *
     * @param request 回滚请求
     * @return 回滚结果
     */
    @PostMapping("/rollback")
    @RequireAuth(permissions = {"upgrade:rollback"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "upgrade",
        description = "回滚Schema"
    )
    public MyResponseResult<RollbackResult> rollbackSchema(@Valid @RequestBody RollbackRequest request) {
        try {
            log.info("收到回滚请求: schema={}, targetVersion={}",
                    request.getSchemaName(), request.getTargetVersion());

            RollbackResult result = schemaRollbackService.rollbackSchema(
                    request.getSchemaName(),
                    request.getTargetVersion()
            );

            return super.doJsonOut(result);
        } catch (Exception e) {
            log.error("回滚失败", e);
            throw new BusinessException("回滚失败: " + e.getMessage());
        }
    }

    /**
     * 获取Schema可用的回滚版本列表
     *
     * @param schemaName Schema名称
     * @return 版本列表
     */
    @GetMapping("/versions/{schemaName}")
    @RequireAuth(permissions = {"upgrade:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "upgrade",
        description = "查询可回滚版本"
    )
    public MyResponseResult<List<String>> getAvailableVersions(@PathVariable String schemaName) {
        try {
            List<String> versions = schemaRollbackService.getAvailableVersions(schemaName);
            return super.doJsonOut(versions);
        } catch (Exception e) {
            log.error("查询可用版本失败: schema={}", schemaName, e);
            throw new BusinessException("查询可用版本失败: " + e.getMessage());
        }
    }
}
