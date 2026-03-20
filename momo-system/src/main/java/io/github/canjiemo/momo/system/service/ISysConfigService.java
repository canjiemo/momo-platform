package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.system.dto.SysConfigCreateRequest;
import io.github.canjiemo.momo.system.dto.SysConfigDTO;
import io.github.canjiemo.momo.system.dto.SysConfigQueryParam;
import io.github.canjiemo.momo.system.dto.SysConfigUpdateRequest;
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
