package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.dto.OperationLogDTO;
import com.seer.fitness.system.dto.OperationLogQueryParam;
import com.seer.fitness.system.entity.SysOperationLog;
import com.seer.fitness.system.enums.OperationType;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.mycommon.pager.Pager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务实现类
 * 提供操作日志的查询、统计、清理等功能
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class OperationLogService extends BaseServiceImpl implements IOperationLogService {

    /**
     * 保存操作日志
     */
    @Override
    @Transactional
    public void save(SysOperationLog operationLog) {
        try {
            String requestParams = operationLog.getRequestParams();
            if(!StringUtils.hasText(requestParams)){
                operationLog.setRequestParams(null);// json格式
            }
            baseDao.insertPO(operationLog, true);
            log.debug("保存操作日志成功: {}", operationLog.getOperationDesc());
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    /**
     * 分页查询操作日志
     */
    @Override
    public Pager<OperationLogDTO> search(OperationLogQueryParam param, Pager pager) {
        Map<String, Object> queryMap = Maps.newHashMap();

        String sql = "SELECT ol.id, ol.user_id, ol.username, ol.real_name, ol.operation_type, " +
                    "ol.module_name, ol.business_id, ol.business_name, ol.operation_desc, " +
                    "ol.request_method, ol.request_url, ol.request_params, ol.response_data, " +
                    "ol.ip_address, ol.user_agent, ol.operation_result, ol.error_message, " +
                    "ol.execution_time, ol.create_time " +
                    "FROM sys_operation_log ol";

        List<String> conditions = new ArrayList<>();

        // 用户ID条件
        if (param.getUserId() != null) {
            conditions.add("ol.user_id = :userId");
            queryMap.put("userId", param.getUserId());
        }

        // 用户名模糊查询
        if (StringUtils.hasText(param.getUsername())) {
            conditions.add("ol.username LIKE :username");
            queryMap.put("username", "%" + param.getUsername() + "%");
        }

        // 真实姓名模糊查询
        if (StringUtils.hasText(param.getRealName())) {
            conditions.add("ol.real_name LIKE :realName");
            queryMap.put("realName", "%" + param.getRealName() + "%");
        }

        // 操作类型条件
        if (StringUtils.hasText(param.getOperationType())) {
            conditions.add("ol.operation_type = :operationType");
            queryMap.put("operationType", param.getOperationType());
        }

        // 模块名称条件
        if (StringUtils.hasText(param.getModuleName())) {
            conditions.add("ol.module_name = :moduleName");
            queryMap.put("moduleName", param.getModuleName());
        }

        // 业务ID条件
        if (StringUtils.hasText(param.getBusinessId())) {
            conditions.add("ol.business_id = :businessId");
            queryMap.put("businessId", param.getBusinessId());
        }

        // 业务名称模糊查询
        if (StringUtils.hasText(param.getBusinessName())) {
            conditions.add("ol.business_name LIKE :businessName");
            queryMap.put("businessName", "%" + param.getBusinessName() + "%");
        }

        // 操作描述模糊查询
        if (StringUtils.hasText(param.getOperationDesc())) {
            conditions.add("ol.operation_desc LIKE :operationDesc");
            queryMap.put("operationDesc", "%" + param.getOperationDesc() + "%");
        }

        // 请求方式条件
        if (StringUtils.hasText(param.getRequestMethod())) {
            conditions.add("ol.request_method = :requestMethod");
            queryMap.put("requestMethod", param.getRequestMethod());
        }

        // 请求URL模糊查询
        if (StringUtils.hasText(param.getRequestUrl())) {
            conditions.add("ol.request_url LIKE :requestUrl");
            queryMap.put("requestUrl", "%" + param.getRequestUrl() + "%");
        }

        // IP地址条件
        if (StringUtils.hasText(param.getIpAddress())) {
            conditions.add("ol.ip_address = :ipAddress");
            queryMap.put("ipAddress", param.getIpAddress());
        }

        // 操作结果条件
        if (param.getOperationResult() != null) {
            conditions.add("ol.operation_result = :operationResult");
            queryMap.put("operationResult", param.getOperationResult());
        }

        // 时间范围条件
        if (param.getStartTime() != null) {
            conditions.add("ol.create_time >= :startTime");
            queryMap.put("startTime", param.getStartTime());
        }

        if (param.getEndTime() != null) {
            conditions.add("ol.create_time <= :endTime");
            queryMap.put("endTime", param.getEndTime());
        }

        // 执行耗时范围条件
        if (param.getMinExecutionTime() != null) {
            conditions.add("ol.execution_time >= :minExecutionTime");
            queryMap.put("minExecutionTime", param.getMinExecutionTime());
        }

        if (param.getMaxExecutionTime() != null) {
            conditions.add("ol.execution_time <= :maxExecutionTime");
            queryMap.put("maxExecutionTime", param.getMaxExecutionTime());
        }

        // 拼接查询条件
        if (!conditions.isEmpty()) {
            sql += " WHERE " + String.join(" AND ", conditions);
        }

        sql += " ORDER BY ol.create_time DESC";

        log.info("操作日志分页查询SQL: {}", sql);

        Pager<OperationLogDTO> result = baseDao.queryPageForSql(sql, queryMap, pager, OperationLogDTO.class);

        // 补充扩展信息
        if (result.getPageData() != null && !result.getPageData().isEmpty()) {
            enrichOperationLogData(result.getPageData());
        }

        return result;
    }

    /**
     * 根据ID获取操作日志详情
     */
    @Override
    public OperationLogDTO getById(Long id) {
        if (id == null) {
            return null;
        }

        SysOperationLog operationLog = baseDao.queryById(id, SysOperationLog.class);
        if (operationLog == null) {
            return null;
        }

        OperationLogDTO dto = convertToDTO(operationLog);
        enrichSingleOperationLogData(dto);
        return dto;
    }

    /**
     * 批量删除操作日志
     */
    @Override
    @Transactional
    public void delete(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return;
        }

        baseDao.delByIds(SysOperationLog.class, ids);
        log.info("批量删除操作日志成功，数量: {}", ids.length);
    }

    /**
     * 清理历史日志
     */
    @Override
    public int cleanHistoryLogs(int days) {
        // TODO: 实现历史日志清理
        log.info("清理{}天前的操作日志", days);
        return 0;
    }

    /**
     * 统计操作类型分布
     */
    @Override
    public Map<String, Long> getOperationTypeStats(int days) {
        // TODO: 实现统计功能
        return Maps.newHashMap();
    }

    /**
     * 统计模块操作分布
     */
    @Override
    public Map<String, Long> getModuleStats(int days) {
        // TODO: 实现统计功能
        return Maps.newHashMap();
    }

    /**
     * 统计用户操作活跃度
     */
    @Override
    public List<Map<String, Object>> getUserActivityStats(int days, int limit) {
        // TODO: 实现统计功能
        return new ArrayList<>();
    }

    /**
     * 统计操作失败情况
     */
    @Override
    public Map<String, Object> getFailureStats(int days) {
        // TODO: 实现统计功能
        return Maps.newHashMap();
    }

    /**
     * 获取操作趋势数据
     */
    @Override
    public List<Map<String, Object>> getOperationTrend(int days) {
        // TODO: 实现统计功能
        return new ArrayList<>();
    }

    /**
     * 导出操作日志
     */
    @Override
    public List<OperationLogDTO> exportLogs(OperationLogQueryParam param) {
        // TODO: 实现导出功能
        return new ArrayList<>();
    }

    /**
     * 补充操作日志列表的扩展信息
     */
    private void enrichOperationLogData(List<OperationLogDTO> logs) {
        for (OperationLogDTO log : logs) {
            enrichSingleOperationLogData(log);
        }
    }

    /**
     * 补充单个操作日志的扩展信息
     */
    private void enrichSingleOperationLogData(OperationLogDTO log) {
        // 设置操作类型描述
        if (StringUtils.hasText(log.getOperationType())) {
            OperationType operationType = OperationType.fromCode(log.getOperationType());
            log.setOperationTypeDesc(operationType.getDescription());
        }

        // 设置操作结果描述
        if (log.getOperationResult() != null) {
            log.setOperationResultDesc(log.getOperationResult() == 1 ? "成功" : "失败");
        }
    }

    /**
     * 转换实体为DTO
     */
    private OperationLogDTO convertToDTO(SysOperationLog operationLog) {
        OperationLogDTO dto = new OperationLogDTO();
        dto.setId(operationLog.getId());
        dto.setUserId(operationLog.getUserId());
        dto.setUsername(operationLog.getUsername());
        dto.setRealName(operationLog.getRealName());
        dto.setOperationType(operationLog.getOperationType());
        dto.setModuleName(operationLog.getModuleName());
        dto.setBusinessId(operationLog.getBusinessId());
        dto.setBusinessName(operationLog.getBusinessName());
        dto.setOperationDesc(operationLog.getOperationDesc());
        dto.setRequestMethod(operationLog.getRequestMethod());
        dto.setRequestUrl(operationLog.getRequestUrl());
        dto.setRequestParams(operationLog.getRequestParams());
        dto.setResponseData(operationLog.getResponseData());
        dto.setIpAddress(operationLog.getIpAddress());
        dto.setUserAgent(operationLog.getUserAgent());
        dto.setOperationResult(operationLog.getOperationResult());
        dto.setErrorMessage(operationLog.getErrorMessage());
        dto.setExecutionTime(operationLog.getExecutionTime());
        dto.setCreateTime(operationLog.getCreateTime());
        return dto;
    }
}