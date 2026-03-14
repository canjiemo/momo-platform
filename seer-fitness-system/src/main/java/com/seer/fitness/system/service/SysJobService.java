package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.dto.JobCreateRequest;
import com.seer.fitness.system.dto.JobDTO;
import com.seer.fitness.system.dto.JobQueryParam;
import com.seer.fitness.system.dto.JobUpdateRequest;
import com.seer.fitness.system.entity.SysJob;
import com.seer.fitness.system.scheduler.JobScheduleManager;
import com.seer.fitness.system.utils.SecurityContextUtil;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
public class SysJobService extends BaseServiceImpl {

    @Autowired
    private JobScheduleManager scheduleManager;

    public Pager<JobDTO> search(JobQueryParam param, Pager pager) {
        StringBuilder sql = new StringBuilder(
            "SELECT id, job_name, job_group, handler_name, cron_expression, job_params, " +
            "status, remark, create_time, update_time FROM sys_job WHERE 1=1");
        Map<String, Object> params = Maps.newHashMap();

        if (param.getJobName() != null && !param.getJobName().isBlank()) {
            sql.append(" AND job_name LIKE :jobName");
            params.put("jobName", "%" + param.getJobName() + "%");
        }
        if (param.getJobGroup() != null && !param.getJobGroup().isBlank()) {
            sql.append(" AND job_group = :jobGroup");
            params.put("jobGroup", param.getJobGroup());
        }
        if (param.getHandlerName() != null && !param.getHandlerName().isBlank()) {
            sql.append(" AND handler_name = :handlerName");
            params.put("handlerName", param.getHandlerName());
        }
        if (param.getStatus() != null) {
            sql.append(" AND status = :status");
            params.put("status", param.getStatus());
        }
        sql.append(" ORDER BY create_time DESC");
        return baseDao.queryPageForSql(sql.toString(), params, pager, JobDTO.class);
    }

    public JobDTO getById(Long id) {
        SysJob job = baseDao.queryById(id, SysJob.class);
        if (job == null) throw new BusinessException("任务不存在");
        return convertToDTO(job);
    }

    @Transactional
    public void create(JobCreateRequest request) {
        validateCron(request.getCronExpression());
        if (isJobNameExists(request.getJobName(), null)) {
            throw new BusinessException("任务名称已存在");
        }

        SysJob job = new SysJob();
        job.setJobName(request.getJobName());
        job.setJobGroup(request.getJobGroup() != null ? request.getJobGroup() : "DEFAULT");
        job.setHandlerName(request.getHandlerName());
        job.setCronExpression(request.getCronExpression());
        job.setJobParams(request.getJobParams());
        job.setStatus(request.getStatus());
        job.setRemark(request.getRemark());
        job.setDeleteFlag(0);
        job.setCreateTime(LocalDateTime.now());
        job.setUpdateTime(LocalDateTime.now());
        job.setCreatedBy(SecurityContextUtil.getCurrentUser().getUserId());
        job.setUpdatedBy(SecurityContextUtil.getCurrentUser().getUserId());

        baseDao.insertPO(job, true);

        if (job.getStatus() == 1) {
            scheduleManager.register(job);
        }
    }

    @Transactional
    public void update(JobUpdateRequest request) {
        validateCron(request.getCronExpression());
        SysJob job = baseDao.queryById(request.getId(), SysJob.class);
        if (job == null) throw new BusinessException("任务不存在");
        if (isJobNameExists(request.getJobName(), request.getId())) {
            throw new BusinessException("任务名称已存在");
        }

        job.setJobName(request.getJobName());
        job.setJobGroup(request.getJobGroup() != null ? request.getJobGroup() : "DEFAULT");
        job.setHandlerName(request.getHandlerName());
        job.setCronExpression(request.getCronExpression());
        job.setJobParams(request.getJobParams());
        job.setStatus(request.getStatus());
        job.setRemark(request.getRemark());
        job.setUpdateTime(LocalDateTime.now());
        job.setUpdatedBy(SecurityContextUtil.getCurrentUser().getUserId());

        baseDao.updatePO(job);
        scheduleManager.refresh(job);
    }

    @Transactional
    public void delete(Long id) {
        SysJob job = baseDao.queryById(id, SysJob.class);
        if (job == null) throw new BusinessException("任务不存在");
        scheduleManager.cancel(id);
        baseDao.delByIds(SysJob.class, String.valueOf(id));
    }

    @Transactional
    public void enable(Long id) {
        SysJob job = baseDao.queryById(id, SysJob.class);
        if (job == null) throw new BusinessException("任务不存在");
        job.setStatus(1);
        job.setUpdateTime(LocalDateTime.now());
        job.setUpdatedBy(SecurityContextUtil.getCurrentUser().getUserId());
        baseDao.updatePO(job);
        scheduleManager.refresh(job);
    }

    @Transactional
    public void disable(Long id) {
        SysJob job = baseDao.queryById(id, SysJob.class);
        if (job == null) throw new BusinessException("任务不存在");
        job.setStatus(0);
        job.setUpdateTime(LocalDateTime.now());
        job.setUpdatedBy(SecurityContextUtil.getCurrentUser().getUserId());
        baseDao.updatePO(job);
        scheduleManager.cancel(id);
    }

    public void trigger(Long id) {
        SysJob job = baseDao.queryById(id, SysJob.class);
        if (job == null) throw new BusinessException("任务不存在");
        Long operatorId = SecurityContextUtil.getCurrentUser().getUserId();
        scheduleManager.triggerOnce(id, job.getJobName(), job.getHandlerName(), job.getJobParams(), operatorId);
    }

    private void validateCron(String cron) {
        if (!CronExpression.isValidExpression(cron)) {
            throw new BusinessException("无效的Cron表达式: " + cron);
        }
    }

    private boolean isJobNameExists(String jobName, Long excludeId) {
        var q = lambdaQuery(SysJob.class).eq(SysJob::getJobName, jobName);
        if (excludeId != null) q.ne(SysJob::getId, excludeId);
        return q.exists();
    }

    private JobDTO convertToDTO(SysJob job) {
        JobDTO dto = new JobDTO();
        dto.setId(job.getId());
        dto.setJobName(job.getJobName());
        dto.setJobGroup(job.getJobGroup());
        dto.setHandlerName(job.getHandlerName());
        dto.setCronExpression(job.getCronExpression());
        dto.setJobParams(job.getJobParams());
        dto.setStatus(job.getStatus());
        dto.setRemark(job.getRemark());
        dto.setCreateTime(job.getCreateTime());
        dto.setUpdateTime(job.getUpdateTime());
        return dto;
    }
}
