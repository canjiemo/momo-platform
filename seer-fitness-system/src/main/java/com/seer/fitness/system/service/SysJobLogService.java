package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.JobLogDTO;
import com.seer.fitness.system.dto.JobLogQueryParam;
import com.seer.fitness.system.entity.SysJobLog;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SysJobLogService extends BaseServiceImpl implements ISysJobLogService {

    public Pager<JobLogDTO> search(JobLogQueryParam param, Pager<JobLogDTO> pager) {

        return lambdaQuery(SysJobLog.class,JobLogDTO.class)
                .eq(SysJobLog::getJobId,param.getJobId())
                .eq(SysJobLog::getTriggerType,param.getTriggerType())
                .eq(SysJobLog::getStatus,param.getStatus())
                .ge(SysJobLog::getStartTime,param.getStartTimeBegin())
                .le(SysJobLog::getStartTime,param.getStartTimeEnd())
                .orderByDesc(SysJobLog::getStartTime)
                .page(pager);
    }

    public JobLogDTO getById(Long id) {
        JobLogDTO dto = lambdaQuery(SysJobLog.class, JobLogDTO.class)
                .eq(SysJobLog::getId,id)
                .one();
        if (dto == null) throw new BusinessException("日志记录不存在");
        return dto;
    }
}
