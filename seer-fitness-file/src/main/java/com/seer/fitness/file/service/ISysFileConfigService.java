package com.seer.fitness.file.service;

import com.seer.fitness.file.dto.SysFileConfigCreateRequest;
import com.seer.fitness.file.dto.SysFileConfigDTO;
import com.seer.fitness.file.dto.SysFileConfigUpdateRequest;
import com.seer.fitness.file.entity.SysFileConfig;
import io.github.canjiemo.base.myjdbc.service.IBaseService;

import java.util.List;

public interface ISysFileConfigService extends IBaseService {
    List<SysFileConfigDTO> list();
    SysFileConfig getActiveConfig();
    void create(SysFileConfigCreateRequest request);
    void update(SysFileConfigUpdateRequest request);
    void activate(Long id);
    void delete(Long id);
}
