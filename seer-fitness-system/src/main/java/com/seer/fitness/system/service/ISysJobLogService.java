package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.JobLogDTO;
import com.seer.fitness.system.dto.JobLogQueryParam;
import io.github.canjiemo.base.myjdbc.service.IBaseService;
import io.github.canjiemo.mycommon.pager.Pager;

public interface ISysJobLogService extends IBaseService {

    Pager<JobLogDTO> search(JobLogQueryParam param, Pager<JobLogDTO> pager);

    JobLogDTO getById(Long id);
}
