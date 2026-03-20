package io.github.canjiemo.momo.file.service;

import io.github.canjiemo.base.myjdbc.service.IBaseService;
import io.github.canjiemo.momo.file.dto.SysFileConfigCreateRequest;
import io.github.canjiemo.momo.file.dto.SysFileConfigDTO;
import io.github.canjiemo.momo.file.dto.SysFileConfigUpdateRequest;
import io.github.canjiemo.momo.file.entity.SysFileConfig;

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
