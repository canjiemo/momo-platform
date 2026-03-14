package com.seer.fitness.system.service;

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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class SysJobService extends BaseServiceImpl {

    @Autowired
    private JobScheduleManager scheduleManager;

    public Pager<JobDTO> search(JobQueryParam param, Pager<JobDTO> pager) {
        return lambdaQuery(SysJob.class, JobDTO.class)
            .like(SysJob::getJobName, param.getJobName())
            .eq(SysJob::getJobGroup, param.getJobGroup())
            .eq(SysJob::getHandlerName, param.getHandlerName())
            .eq(SysJob::getStatus, param.getStatus())
            .orderByDesc(SysJob::getCreateTime)
            .page(pager);
    }

    public JobDTO getById(Long id) {
        SysJob job = baseDao.queryById(id, SysJob.class);
        if (job == null) throw new BusinessException("任务不存在");
        JobDTO dto = new JobDTO();
        BeanUtils.copyProperties(job, dto);
        return dto;
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
}
