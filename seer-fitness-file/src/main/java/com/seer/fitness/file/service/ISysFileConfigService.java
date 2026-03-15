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
    /** 更新配置，返回是否影响当前激活配置（true 时调用方需刷新存储缓存） */
    boolean update(SysFileConfigUpdateRequest request);
    void activate(Long id);
    void delete(Long id);
}
