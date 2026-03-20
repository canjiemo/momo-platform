package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.base.myjdbc.service.IBaseService;
import io.github.canjiemo.momo.system.dto.JobLogDTO;
import io.github.canjiemo.momo.system.dto.JobLogQueryParam;
import io.github.canjiemo.mycommon.pager.Pager;

public interface ISysJobLogService extends IBaseService {

    Pager<JobLogDTO> search(JobLogQueryParam param, Pager<JobLogDTO> pager);


    JobLogDTO getById(Long id);
}
