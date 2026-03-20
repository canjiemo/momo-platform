package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.base.myjdbc.service.IBaseService;
import io.github.canjiemo.momo.system.dto.JobCreateRequest;
import io.github.canjiemo.momo.system.dto.JobDTO;
import io.github.canjiemo.momo.system.dto.JobQueryParam;
import io.github.canjiemo.momo.system.dto.JobUpdateRequest;
import io.github.canjiemo.mycommon.pager.Pager;

public interface ISysJobService extends IBaseService {

    Pager<JobDTO> search(JobQueryParam param, Pager<JobDTO> pager);

    JobDTO getById(Long id);

    void create(JobCreateRequest request);

    void update(JobUpdateRequest request);

    void delete(Long id);

    void enable(Long id);

    void disable(Long id);

    void trigger(Long id);
}
