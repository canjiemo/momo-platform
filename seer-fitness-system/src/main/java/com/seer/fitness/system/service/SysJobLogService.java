package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.dto.JobLogDTO;
import com.seer.fitness.system.dto.JobLogQueryParam;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class SysJobLogService extends BaseServiceImpl implements ISysJobLogService {

    public Pager<JobLogDTO> search(JobLogQueryParam param, Pager<JobLogDTO> pager) {
        StringBuilder sql = new StringBuilder(
            "SELECT id, job_id, job_name, handler_name, trigger_type, start_time, end_time, " +
            "duration_ms, status, error_msg, operator_id FROM sys_job_log WHERE 1=1");
        Map<String, Object> params = Maps.newHashMap();

        if (param.getJobId() != null) {
            sql.append(" AND job_id = :jobId");
            params.put("jobId", param.getJobId());
        }
        if (param.getTriggerType() != null) {
            sql.append(" AND trigger_type = :triggerType");
            params.put("triggerType", param.getTriggerType());
        }
        if (param.getStatus() != null) {
            sql.append(" AND status = :status");
            params.put("status", param.getStatus());
        }
        if (param.getStartTimeBegin() != null) {
            sql.append(" AND start_time >= :startTimeBegin");
            params.put("startTimeBegin", param.getStartTimeBegin());
        }
        if (param.getStartTimeEnd() != null) {
            sql.append(" AND start_time <= :startTimeEnd");
            params.put("startTimeEnd", param.getStartTimeEnd());
        }
        sql.append(" ORDER BY start_time DESC");
        return baseDao.queryPageForSql(sql.toString(), params, pager, JobLogDTO.class);
    }

    public JobLogDTO getById(Long id) {
        String sql = "SELECT id, job_id, job_name, handler_name, trigger_type, start_time, end_time, " +
                     "duration_ms, status, error_msg, operator_id FROM sys_job_log WHERE id = :id";
        Map<String, Object> params = Maps.newHashMap();
        params.put("id", id);
        JobLogDTO dto = baseDao.querySingleForSql(sql, params, JobLogDTO.class);
        if (dto == null) throw new BusinessException("日志记录不存在");
        return dto;
    }
}
