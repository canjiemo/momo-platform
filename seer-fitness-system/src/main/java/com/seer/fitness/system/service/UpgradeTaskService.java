package com.seer.fitness.system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.seer.fitness.system.dto.UpgradeTaskDTO;
import com.seer.fitness.system.dto.UpgradeTaskDetail;
import com.seer.fitness.system.dto.UpgradeTaskQueryParam;
import com.seer.fitness.system.entity.SysUpgradeTask;
import com.seer.fitness.system.entity.SysUpgradeTaskLog;
import com.seer.fitness.system.util.SecurityContextUtil;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 升级任务管理服务实现
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Service
@Slf4j
public class UpgradeTaskService extends BaseServiceImpl implements IUpgradeTaskService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建升级任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(SysUpgradeTask task) {
        try {
            // 设置默认值
            if (task.getStatus() == null) {
                task.setStatus("PENDING");
            }
            if (task.getTotalSchemas() == null) {
                task.setTotalSchemas(0);
            }
            if (task.getSuccessCount() == null) {
                task.setSuccessCount(0);
            }
            if (task.getFailedCount() == null) {
                task.setFailedCount(0);
            }
            if (task.getDeleteFlag() == null) {
                task.setDeleteFlag(0);
            }

            // 设置创建人
            if (SecurityContextUtil.getCurrentUser() != null) {
                task.setCreatedBy(SecurityContextUtil.getCurrentUser().getUserId());
            }

            // 设置时间
            task.setCreatedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());

            // 插入数据库（自动生成ID）
            baseDao.insertPO(task, true);
            log.info("创建升级任务成功: taskId={}, taskName={}, upgradeType={}",
                    task.getId(), task.getTaskName(), task.getUpgradeType());

            return task.getId();
        } catch (Exception e) {
            log.error("创建升级任务失败", e);
            throw new BusinessException("创建升级任务失败: " + e.getMessage());
        }
    }

    /**
     * 更新任务状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskStatus(Long taskId, String status) {
        try {
            String sql = "UPDATE public.sys_upgrade_task SET status = :status, updated_at = :updatedAt";

            // 如果是开始执行，设置start_time
            if ("RUNNING".equals(status)) {
                sql += ", start_time = :startTime";
            }

            // 如果是完成状态，设置end_time
            if ("COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status)) {
                sql += ", end_time = :endTime";
            }

            sql += " WHERE id = :taskId";

            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("status", status);
            params.addValue("updatedAt", LocalDateTime.now());
            params.addValue("taskId", taskId);

            if ("RUNNING".equals(status)) {
                params.addValue("startTime", LocalDateTime.now());
            }

            if ("COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status)) {
                params.addValue("endTime", LocalDateTime.now());
            }

            int updated = namedParameterJdbcTemplate.update(sql, params);
            if (updated == 0) {
                throw new BusinessException("任务不存在: " + taskId);
            }

            log.info("更新任务状态成功: taskId={}, status={}", taskId, status);
        } catch (Exception e) {
            log.error("更新任务状态失败: taskId={}, status={}", taskId, status, e);
            throw new BusinessException("更新任务状态失败: " + e.getMessage());
        }
    }

    /**
     * 更新任务统计信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskStats(Long taskId, Integer successCount, Integer failedCount) {
        try {
            String sql = "UPDATE public.sys_upgrade_task " +
                        "SET success_count = :successCount, failed_count = :failedCount, updated_at = :updatedAt " +
                        "WHERE id = :taskId";

            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("successCount", successCount);
            params.addValue("failedCount", failedCount);
            params.addValue("updatedAt", LocalDateTime.now());
            params.addValue("taskId", taskId);

            namedParameterJdbcTemplate.update(sql, params);
            log.debug("更新任务统计: taskId={}, success={}, failed={}", taskId, successCount, failedCount);
        } catch (Exception e) {
            log.error("更新任务统计失败: taskId={}", taskId, e);
            throw new BusinessException("更新任务统计失败: " + e.getMessage());
        }
    }

    /**
     * 记录Schema升级日志
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logSchemaUpgrade(SysUpgradeTaskLog log) {
        try {
            // 设置默认值
            if (log.getStatus() == null) {
                log.setStatus("PENDING");
            }
            if (log.getMigrationsExecuted() == null) {
                log.setMigrationsExecuted(0);
            }
            if (log.getExecutionTime() == null) {
                log.setExecutionTime(0);
            }
            if (log.getDeleteFlag() == null) {
                log.setDeleteFlag(0);
            }

            // 设置时间
            log.setCreatedAt(LocalDateTime.now());

            // 插入数据库（自动生成ID）
            baseDao.insertPO(log, true);
            UpgradeTaskService.log.info("记录Schema升级日志: logId={}, taskId={}, schema={}",
                    log.getId(), log.getTaskId(), log.getSchemaName());
        } catch (Exception e) {
            UpgradeTaskService.log.error("记录Schema升级日志失败", e);
            throw new BusinessException("记录升级日志失败: " + e.getMessage());
        }
    }

    /**
     * 更新Schema升级日志状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLogStatus(Long logId, String status, String errorMessage) {
        try {
            String sql = "UPDATE public.sys_schema_upgrade_detail SET status = :status";

            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("status", status);
            params.addValue("logId", logId);

            if (errorMessage != null) {
                sql += ", error_message = :errorMessage";
                params.addValue("errorMessage", errorMessage);
            }

            // 如果是完成状态，设置end_time
            if ("SUCCESS".equals(status) || "FAILED".equals(status) || "ROLLED_BACK".equals(status)) {
                sql += ", end_time = :endTime";
                params.addValue("endTime", LocalDateTime.now());
            }

            sql += " WHERE id = :logId";

            namedParameterJdbcTemplate.update(sql, params);
            log.debug("更新日志状态: logId={}, status={}", logId, status);
        } catch (Exception e) {
            log.error("更新日志状态失败: logId={}, status={}", logId, status, e);
            throw new BusinessException("更新日志状态失败: " + e.getMessage());
        }
    }

    /**
     * 查询任务详情
     */
    @Override
    public UpgradeTaskDetail getTaskDetail(Long taskId) {
        try {
            // 查询任务
            SysUpgradeTask task = getTaskById(taskId);
            if (task == null) {
                throw new BusinessException("任务不存在: " + taskId);
            }

            // 构建详情DTO
            UpgradeTaskDetail detail = new UpgradeTaskDetail();
            detail.setId(task.getId());
            detail.setTaskName(task.getTaskName());
            detail.setTargetVersion(task.getTargetVersion());
            detail.setFromVersion(task.getFromVersion());
            detail.setUpgradeType(task.getUpgradeType());
            detail.setTotalSchemas(task.getTotalSchemas());
            detail.setSuccessCount(task.getSuccessCount());
            detail.setFailedCount(task.getFailedCount());
            detail.setStatus(task.getStatus());
            detail.setStartTime(task.getStartTime());
            detail.setEndTime(task.getEndTime());
            detail.setCreatedBy(task.getCreatedBy());
            detail.setCreatedAt(task.getCreatedAt());

            // 解析targetSchemas JSON
            if (task.getTargetSchemas() != null) {
                try {
                    List<String> schemas = objectMapper.readValue(
                            task.getTargetSchemas(),
                            new TypeReference<List<String>>() {}
                    );
                    detail.setTargetSchemas(schemas);
                } catch (Exception e) {
                    log.warn("解析targetSchemas失败: {}", task.getTargetSchemas(), e);
                    detail.setTargetSchemas(new ArrayList<>());
                }
            }

            // 查询日志
            String logSql = "SELECT * FROM public.sys_schema_upgrade_detail " +
                           "WHERE task_id = :taskId AND delete_flag = 0 " +
                           "ORDER BY created_at ASC";

            Map<String, Object> params = Maps.newHashMap();
            params.put("taskId", taskId);

            List<SysUpgradeTaskLog> logs = baseDao.queryListForSql(logSql, params, SysUpgradeTaskLog.class);

            // 转换为DTO
            List<UpgradeTaskDetail.UpgradeLogDTO> logDTOs = new ArrayList<>();
            for (SysUpgradeTaskLog log : logs) {
                UpgradeTaskDetail.UpgradeLogDTO logDTO = new UpgradeTaskDetail.UpgradeLogDTO();
                logDTO.setId(log.getId());
                logDTO.setSchemaName(log.getSchemaName());
                logDTO.setFromVersion(log.getFromVersion());
                logDTO.setToVersion(log.getToVersion());
                logDTO.setMigrationsExecuted(log.getMigrationsExecuted());
                logDTO.setStatus(log.getStatus());
                logDTO.setErrorMessage(log.getErrorMessage());
                logDTO.setStartTime(log.getStartTime());
                logDTO.setEndTime(log.getEndTime());
                logDTO.setExecutionTime(log.getExecutionTime());
                logDTOs.add(logDTO);
            }

            detail.setLogs(logDTOs);
            return detail;
        } catch (Exception e) {
            log.error("查询任务详情失败: taskId={}", taskId, e);
            throw new BusinessException("查询任务详情失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查询任务
     */
    @Override
    public SysUpgradeTask getTaskById(Long taskId) {
        return baseDao.queryByIdWithDeleteCondition(taskId, SysUpgradeTask.class);
    }

    /**
     * 分页查询升级历史
     */
    @Override
    public Pager<UpgradeTaskDTO> searchHistory(UpgradeTaskQueryParam param, Pager pager) {
        try {
            // 构建查询SQL
            String sql = "SELECT id, task_name, target_version, from_version, upgrade_type, " +
                        "total_schemas, success_count, failed_count, status, " +
                        "start_time, end_time, created_by, created_at " +
                        "FROM public.sys_schema_upgrade_task WHERE delete_flag = 0";

            Map<String, Object> queryMap = Maps.newHashMap();
            List<String> conditions = new ArrayList<>();

            // 可选查询条件
            if (param.getStatus() != null) {
                conditions.add("status = :status");
                queryMap.put("status", param.getStatus());
            }

            if (param.getUpgradeType() != null) {
                conditions.add("upgrade_type = :upgradeType");
                queryMap.put("upgradeType", param.getUpgradeType());
            }

            if (param.getTaskName() != null) {
                conditions.add("task_name LIKE :taskName");
                queryMap.put("taskName", "%" + param.getTaskName() + "%");
            }

            if (!conditions.isEmpty()) {
                sql += " AND " + String.join(" AND ", conditions);
            }

            sql += " ORDER BY created_at DESC";

            log.info("升级历史查询SQL: {}", sql);

            // 直接返回DTO类型的Pager
            return baseDao.queryPageForSqlWithDeleteCondition(sql, queryMap, pager, UpgradeTaskDTO.class);

        } catch (Exception e) {
            log.error("查询升级历史失败", e);
            throw new BusinessException("查询升级历史失败: " + e.getMessage());
        }
    }
}
