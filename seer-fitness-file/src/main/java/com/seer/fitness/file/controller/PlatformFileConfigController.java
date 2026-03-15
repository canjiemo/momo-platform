package com.seer.fitness.file.controller;

import com.seer.fitness.file.dto.SysFileConfigCreateRequest;
import com.seer.fitness.file.dto.SysFileConfigDTO;
import com.seer.fitness.file.dto.SysFileConfigUpdateRequest;
import com.seer.fitness.file.service.ISysFileConfigService;
import com.seer.fitness.file.storage.FileStorageManager;
import com.seer.fitness.framework.annotation.RequireAuth;
import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/platform/file-config")
public class PlatformFileConfigController extends MyBaseController {

    @Autowired
    private ISysFileConfigService fileConfigService;

    @Autowired
    private FileStorageManager fileStorageManager;

    @GetMapping("/list")
    @RequireAuth(permissions = {"file:config:view"})
    public MyResponseResult<List<SysFileConfigDTO>> list() {
        return doJsonOut(fileConfigService.list());
    }

    @PostMapping("/create")
    @RequireAuth(permissions = {"file:config:create"})
    public MyResponseResult create(@Valid @RequestBody SysFileConfigCreateRequest request) {
        fileConfigService.create(request);
        return doJsonDefaultMsg();
    }

    @PostMapping("/update")
    @RequireAuth(permissions = {"file:config:update"})
    public MyResponseResult update(@Valid @RequestBody SysFileConfigUpdateRequest request) {
        if (fileConfigService.update(request)) {
            fileStorageManager.invalidate();
        }
        return doJsonDefaultMsg();
    }

    @PostMapping("/activate/{id}")
    @RequireAuth(permissions = {"file:config:update"})
    public MyResponseResult activate(@PathVariable Long id) {
        fileConfigService.activate(id);
        fileStorageManager.invalidate();
        return doJsonDefaultMsg();
    }

    @PostMapping("/delete/{id}")
    @RequireAuth(permissions = {"file:config:delete"})
    public MyResponseResult delete(@PathVariable Long id) {
        fileConfigService.delete(id);
        return doJsonDefaultMsg();
    }
}
