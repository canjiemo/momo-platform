package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.SysConfigCreateRequest;
import com.seer.fitness.system.dto.SysConfigDTO;
import com.seer.fitness.system.dto.SysConfigQueryParam;
import com.seer.fitness.system.dto.SysConfigUpdateRequest;
import io.github.canjiemo.mycommon.pager.Pager;

public interface ISysConfigService {
    Pager<SysConfigDTO> search(SysConfigQueryParam param, Pager<SysConfigDTO> pager);
    SysConfigDTO getByKey(String configKey);
    String getValue(String configKey);
    void create(SysConfigCreateRequest request);
    void update(SysConfigUpdateRequest request);
    void delete(Long id);
    void refreshCache();
    void refreshCache(String configKey);
}
