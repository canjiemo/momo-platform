package com.seer.fitness.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.seer.fitness.system.config.FlywayMultiTenantConfig;
import com.seer.fitness.system.dto.UpgradeResult;
import com.seer.fitness.system.dto.UpgradeTaskDetail;
import com.seer.fitness.system.entity.SysUpgradeTask;
import com.seer.fitness.system.entity.SysUpgradeTaskLog;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Schema升级服务实现
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Service
@Slf4j
public class SchemaUpgradeService extends BaseServiceImpl implements ISchemaUpgradeService {

    @Autowired
    private FlywayMultiTenantConfig flywayMultiTenantConfig;

    @Autowired
    private IUpgradeTaskService upgradeTaskService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 升级单个Schema到指定版本
     */
    @Override
    public UpgradeResult upgradeSchema(String schemaName, String targetVersion) {
        LocalDateTime startTime = LocalDateTime.now();
        String fromVersion = null;
        String toVersion = null;
        Integer migrationsExecuted = 0;

        try {
            log.info("开始升级Schema: schema={}, targetVersion={}", schemaName, targetVersion);

            // 1. 获取当前版本
            fromVersion = flywayMultiTenantConfig.getCurrentVersion(schemaName);
            log.info("Schema当前版本: schema={}, version={}", schemaName, fromVersion);

            // 2. 验证Schema状态
            boolean isValid = flywayMultiTenantConfig.validateSchema(schemaName);
            if (!isValid) {
                log.warn("Schema验证失败，尝试修复: schema={}", schemaName);
                flywayMultiTenantConfig.repairSchema(schemaName);
            }

            // 3. 执行迁移
            migrationsExecuted = flywayMultiTenantConfig.migrateSchema(schemaName);
            toVersion = flywayMultiTenantConfig.getCurrentVersion(schemaName);

            // 4. 更新sys_schema_version表
            updateSchemaVersion(schemaName, toVersion, migrationsExecuted);

            // 5. 计算执行时间
            int executionTime = (int) java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();

            log.info("Schema升级成功: schema={}, from={}, to={}, migrations={}",
                    schemaName, fromVersion, toVersion, migrationsExecuted);

            return UpgradeResult.builder()
                    .success(true)
                    .schemaName(schemaName)
                    .fromVersion(fromVersion)
                    .toVersion(toVersion)
                    .migrationsExecuted(migrationsExecuted)
                    .upgradedAt(LocalDateTime.now())
                    .executionTime(executionTime)
                    .build();

        } catch (Exception e) {
            log.error("Schema升级失败: schema={}, error={}", schemaName, e.getMessage(), e);

            int executionTime = (int) java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();

            return UpgradeResult.builder()
                    .success(false)
                    .schemaName(schemaName)
                    .fromVersion(fromVersion)
                    .toVersion(toVersion)
                    .migrationsExecuted(migrationsExecuted)
                    .errorMessage(e.getMessage())
                    .upgradedAt(LocalDateTime.now())
                    .executionTime(executionTime)
                    .build();
        }
    }

    /**
     * 批量升级多个Schema
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long batchUpgradeSchemas(String taskName, List<String> schemaNames, String targetVersion) {
        try {
            log.info("开始批量升级: taskName={}, schemas={}, targetVersion={}",
                    taskName, schemaNames.size(), targetVersion);

            // 1. 创建升级任务
            SysUpgradeTask task = new SysUpgradeTask();
            task.setTaskName(taskName);
            task.setTargetVersion(targetVersion != null ? targetVersion : "latest");
            task.setUpgradeType("BATCH");
            task.setTargetSchemas(objectMapper.writeValueAsString(schemaNames));
            task.setTotalSchemas(schemaNames.size());

            Long taskId = upgradeTaskService.createTask(task);

            // 2. 异步执行升级
            executeUpgradeAsync(taskId, schemaNames, targetVersion);

            return taskId;
        } catch (Exception e) {
            log.error("创建批量升级任务失败", e);
            throw new BusinessException("创建批量升级任务失败: " + e.getMessage());
        }
    }

    /**
     * 升级所有租户Schema
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long upgradeAllSchemas(String taskName, String targetVersion) {
        try {
            log.info("开始升级所有租户Schema: taskName={}, targetVersion={}", taskName, targetVersion);

            // 1. 查询所有租户Schema - 直接查询String列表
            String sql = "SELECT schema_name FROM public.sys_schema_version WHERE delete_flag = 0 ORDER BY schema_name";
            MapSqlParameterSource params = new MapSqlParameterSource();

            List<String> schemaNames = namedParameterJdbcTemplate.queryForList(sql, params, String.class);

            if (schemaNames.isEmpty()) {
                throw new BusinessException("没有找到任何租户Schema");
            }

            log.info("找到{}个租户Schema", schemaNames.size());

            // 2. 创建升级任务
            SysUpgradeTask task = new SysUpgradeTask();
            task.setTaskName(taskName);
            task.setTargetVersion(targetVersion != null ? targetVersion : "latest");
            task.setUpgradeType("ALL");
            task.setTargetSchemas(objectMapper.writeValueAsString(schemaNames));
            task.setTotalSchemas(schemaNames.size());

            Long taskId = upgradeTaskService.createTask(task);

            // 3. 异步执行升级
            executeUpgradeAsync(taskId, schemaNames, targetVersion);

            return taskId;
        } catch (Exception e) {
            log.error("创建全量升级任务失败", e);
            throw new BusinessException("创建全量升级任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询升级任务状态
     */
    @Override
    public UpgradeTaskDetail getTaskStatus(Long taskId) {
        return upgradeTaskService.getTaskDetail(taskId);
    }

    /**
     * 取消升级任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long taskId) {
        try {
            SysUpgradeTask task = upgradeTaskService.getTaskById(taskId);
            if (task == null) {
                throw new BusinessException("任务不存在: " + taskId);
            }

            if (!"PENDING".equals(task.getStatus()) && !"RUNNING".equals(task.getStatus())) {
                throw new BusinessException("只能取消待执行或执行中的任务");
            }

            upgradeTaskService.updateTaskStatus(taskId, "CANCELLED");
            log.info("已取消升级任务: taskId={}", taskId);
        } catch (Exception e) {
            log.error("取消升级任务失败: taskId={}", taskId, e);
            throw new BusinessException("取消升级任务失败: " + e.getMessage());
        }
    }

    /**
     * 异步执行升级任务
     */
    @Async
    protected void executeUpgradeAsync(Long taskId, List<String> schemaNames, String targetVersion) {
        try {
            log.info("异步执行升级任务: taskId={}, schemas={}", taskId, schemaNames.size());

            // 1. 更新任务状态为RUNNING
            upgradeTaskService.updateTaskStatus(taskId, "RUNNING");

            // 2. 遍历每个Schema执行升级
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failedCount = new AtomicInteger(0);

            for (String schemaName : schemaNames) {
                // 检查任务是否被取消
                SysUpgradeTask task = upgradeTaskService.getTaskById(taskId);
                if ("CANCELLED".equals(task.getStatus())) {
                    log.info("任务已取消，停止执行: taskId={}", taskId);
                    break;
                }

                // 创建日志记录
                SysUpgradeTaskLog taskLog = new SysUpgradeTaskLog();
                taskLog.setTaskId(taskId);
                taskLog.setSchemaName(schemaName);
                taskLog.setStatus("RUNNING");
                taskLog.setStartTime(LocalDateTime.now());

                upgradeTaskService.logSchemaUpgrade(taskLog);

                // 执行升级
                UpgradeResult result = upgradeSchema(schemaName, targetVersion);

                // 更新日志
                taskLog.setFromVersion(result.getFromVersion());
                taskLog.setToVersion(result.getToVersion());
                taskLog.setMigrationsExecuted(result.getMigrationsExecuted());
                taskLog.setEndTime(result.getUpgradedAt());
                taskLog.setExecutionTime(result.getExecutionTime());

                if (result.getSuccess()) {
                    upgradeTaskService.updateLogStatus(taskLog.getId(), "SUCCESS", null);
                    successCount.incrementAndGet();
                } else {
                    upgradeTaskService.updateLogStatus(taskLog.getId(), "FAILED", result.getErrorMessage());
                    failedCount.incrementAndGet();
                }

                // 更新任务统计
                upgradeTaskService.updateTaskStats(taskId, successCount.get(), failedCount.get());
            }

            // 3. 更新任务最终状态
            String finalStatus = (failedCount.get() == 0) ? "COMPLETED" : "FAILED";
            upgradeTaskService.updateTaskStatus(taskId, finalStatus);

            log.info("升级任务执行完成: taskId={}, success={}, failed={}",
                    taskId, successCount.get(), failedCount.get());

        } catch (Exception e) {
            log.error("执行升级任务异常: taskId={}", taskId, e);
            upgradeTaskService.updateTaskStatus(taskId, "FAILED");
        }
    }

    /**
     * 更新Schema版本记录
     */
    @Transactional(rollbackFor = Exception.class)
    protected void updateSchemaVersion(String schemaName, String newVersion, Integer migrationsExecuted) {
        try {
            String updateSql = "UPDATE public.sys_schema_version " +
                              "SET current_version = :newVersion, " +
                              "    flyway_version = :newVersion, " +
                              "    last_migration_at = :now, " +
                              "    updated_at = :now " +
                              "WHERE schema_name = :schemaName";

            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("newVersion", newVersion);
            params.addValue("schemaName", schemaName);
            params.addValue("now", LocalDateTime.now());

            int updated = namedParameterJdbcTemplate.update(updateSql, params);

            if (updated > 0) {
                log.debug("更新Schema版本记录: schema={}, version={}", schemaName, newVersion);

                // 记录版本历史
                String historySql = "INSERT INTO public.sys_schema_version_history " +
                                   "(id, schema_name, from_version, to_version, migrations_executed, " +
                                   " migrated_at, migrated_by, created_at, delete_flag) " +
                                   "VALUES (nextval('public.sys_schema_version_history_id_seq'), :schemaName, " +
                                   " (SELECT current_version FROM public.sys_schema_version WHERE schema_name = :schemaName), " +
                                   " :toVersion, :migrationsExecuted, :now, :userId, :now, 0)";

                MapSqlParameterSource historyParams = new MapSqlParameterSource();
                historyParams.addValue("schemaName", schemaName);
                historyParams.addValue("toVersion", newVersion);
                historyParams.addValue("migrationsExecuted", migrationsExecuted);
                historyParams.addValue("now", LocalDateTime.now());
                historyParams.addValue("userId", 0L); // 系统自动

                namedParameterJdbcTemplate.update(historySql, historyParams);
            }
        } catch (Exception e) {
            log.error("更新Schema版本记录失败: schema={}", schemaName, e);
            // 不抛出异常，允许升级继续
        }
    }
}
