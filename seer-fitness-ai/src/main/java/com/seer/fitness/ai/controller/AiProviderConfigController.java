package com.seer.fitness.ai.controller;

import com.seer.fitness.ai.provider.entity.AiProviderConfig;
import com.seer.fitness.ai.provider.service.IAiProviderConfigService;
import com.seer.fitness.framework.annotation.RequireAuth;
import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/platform/ai/provider")
public class AiProviderConfigController extends MyBaseController {

    @Autowired
    private IAiProviderConfigService configService;

    @RequireAuth(permissions = {"ai:provider:view"})
    @GetMapping("/list")
    public MyResponseResult<List<AiProviderConfig>> list() {
        return doJsonOut(configService.list());
    }

    @RequireAuth(permissions = {"ai:provider:create"})
    @PostMapping("/create")
    public MyResponseResult create(@RequestBody AiProviderConfig config) {
        configService.create(config);
        return doJsonDefaultMsg();
    }

    @RequireAuth(permissions = {"ai:provider:update"})
    @PostMapping("/update")
    public MyResponseResult update(@RequestBody AiProviderConfig config) {
        configService.update(config);
        return doJsonDefaultMsg();
    }

    @RequireAuth(permissions = {"ai:provider:update"})
    @PostMapping("/activate/{id}")
    public MyResponseResult activate(@PathVariable Long id) {
        configService.activate(id);
        return doJsonDefaultMsg();
    }

    @RequireAuth(permissions = {"ai:provider:delete"})
    @PostMapping("/delete/{id}")
    public MyResponseResult delete(@PathVariable Long id) {
        configService.delete(id);
        return doJsonDefaultMsg();
    }
}
