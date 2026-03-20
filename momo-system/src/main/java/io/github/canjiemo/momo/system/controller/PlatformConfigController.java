package io.github.canjiemo.momo.system.controller;

import io.github.canjiemo.momo.framework.annotation.OperationLog;
import io.github.canjiemo.momo.framework.annotation.RequireAuth;
import io.github.canjiemo.momo.framework.enums.OperationType;
import io.github.canjiemo.momo.system.dto.SysConfigCreateRequest;
import io.github.canjiemo.momo.system.dto.SysConfigDTO;
import io.github.canjiemo.momo.system.dto.SysConfigQueryParam;
import io.github.canjiemo.momo.system.dto.SysConfigUpdateRequest;
import io.github.canjiemo.momo.system.service.ISysConfigService;
import io.github.canjiemo.mycommon.pager.Pager;
import io.github.canjiemo.mycommon.pager.PagerHandler;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/config")
public class PlatformConfigController {

    @Autowired
    private ISysConfigService sysConfigService;

    @PostMapping("/search")
    @RequireAuth(permissions = {"config:view"})
    public Pager<SysConfigDTO> search(@RequestBody SysConfigQueryParam param) {
        return sysConfigService.search(param, PagerHandler.createPager(param));
    }

    @GetMapping("/{key}")
    @RequireAuth(permissions = {"config:view"})
    public SysConfigDTO getByKey(@PathVariable String key) {
        return sysConfigService.getByKey(key);
    }

    @PostMapping("/create")
    @RequireAuth(permissions = {"config:create"})
    @OperationLog(type = OperationType.CREATE, module = "配置管理", description = "新增配置项")
    public void create(@RequestBody @Valid SysConfigCreateRequest request) {
        sysConfigService.create(request);
    }

    @PostMapping("/update")
    @RequireAuth(permissions = {"config:update"})
    @OperationLog(type = OperationType.UPDATE, module = "配置管理", description = "修改配置项")
    public void update(@RequestBody @Valid SysConfigUpdateRequest request) {
        sysConfigService.update(request);
    }

    @PostMapping("/delete/{id}")
    @RequireAuth(permissions = {"config:delete"})
    @OperationLog(type = OperationType.DELETE, module = "配置管理", description = "删除配置项")
    public void delete(@PathVariable Long id) {
        sysConfigService.delete(id);
    }

    @PostMapping("/refresh")
    @RequireAuth(permissions = {"config:update"})
    @OperationLog(type = OperationType.OTHER, module = "配置管理", description = "刷新配置缓存")
    public void refresh(@RequestParam(required = false) String key) {
        if (key != null) {
            sysConfigService.refreshCache(key);
        } else {
            sysConfigService.refreshCache();
        }
    }
}
