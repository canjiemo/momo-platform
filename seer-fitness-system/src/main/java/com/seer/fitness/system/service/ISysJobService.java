package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.JobCreateRequest;
import com.seer.fitness.system.dto.JobDTO;
import com.seer.fitness.system.dto.JobQueryParam;
import com.seer.fitness.system.dto.JobUpdateRequest;
import io.github.canjiemo.base.myjdbc.service.IBaseService;
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
