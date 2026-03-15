package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.framework.enums.OperationType;
import com.seer.fitness.system.dto.OperationLogDTO;
import com.seer.fitness.system.dto.OperationLogQueryParam;
import com.seer.fitness.system.entity.SysOperationLog;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.mycommon.pager.Pager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
    public Pager<OperationLogDTO> search(OperationLogQueryParam param, Pager<OperationLogDTO> pager) {
        Pager<OperationLogDTO> result = lambdaQuery(SysOperationLog.class, OperationLogDTO.class)
                .eq(SysOperationLog::getUserId, param.getUserId())
                .like(SysOperationLog::getUsername, param.getUsername())
                .like(SysOperationLog::getRealName, param.getRealName())
                .eq(SysOperationLog::getOperationType, param.getOperationType())
                .eq(SysOperationLog::getModuleName, param.getModuleName())
                .eq(SysOperationLog::getBusinessId, param.getBusinessId())
                .like(SysOperationLog::getBusinessName, param.getBusinessName())
                .like(SysOperationLog::getOperationDesc, param.getOperationDesc())
                .eq(SysOperationLog::getRequestMethod, param.getRequestMethod())
                .like(SysOperationLog::getRequestUrl, param.getRequestUrl())
                .eq(SysOperationLog::getIpAddress, param.getIpAddress())
                .eq(SysOperationLog::getOperationResult, param.getOperationResult())
                .ge(SysOperationLog::getCreateTime, param.getStartTime())
                .le(SysOperationLog::getCreateTime, param.getEndTime())
                .ge(SysOperationLog::getExecutionTime, param.getMinExecutionTime())
                .le(SysOperationLog::getExecutionTime, param.getMaxExecutionTime())
                .orderByDesc(SysOperationLog::getCreateTime)
                .page(pager);

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

        OperationLogDTO dto = new OperationLogDTO();
        BeanUtils.copyProperties(operationLog, dto);
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
}
