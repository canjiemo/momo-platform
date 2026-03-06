package com.seer.fitness.system.controller;

import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.system.dto.*;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.system.service.IPlatformOrganizationService;
import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.mycommon.pager.Pager;
import io.github.canjiemo.mycommon.pager.PagerHandler;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台组织管理控制器（/platform/organization）
 * 管理平台自身的组织架构（tenant_id=NULL 的组织）
 *
 * @author seer-fitness
 */
@RestController
@RequestMapping("/platform/organization")
public class PlatformOrganizationController extends MyBaseController {

    @Autowired
    private IPlatformOrganizationService platformOrgService;

    @PostMapping("/search")
    @RequireAuth(permissions = {"platform:org:view"})
    public MyResponseResult<Pager<OrganizationDTO>> search(@RequestBody OrganizationQueryParam param) {
        return super.doJsonPagerOut(platformOrgService.search(param, PagerHandler.createPager(param)));
    }

    @GetMapping("/tree")
    @RequireAuth(permissions = {"platform:org:view"})
    public MyResponseResult<List<OrganizationTreeVO>> getOrganizationTree() {
        return super.doJsonOut(platformOrgService.getOrganizationTree());
    }

    @GetMapping("/list")
    @RequireAuth(permissions = {"platform:org:view"})
    public MyResponseResult<List<OrganizationDTO>> list() {
        return super.doJsonOut(platformOrgService.list());
    }

    @GetMapping("/{id}")
    @RequireAuth(permissions = {"platform:org:view"})
    public MyResponseResult<OrganizationDTO> getById(@PathVariable Long id) {
        return super.doJsonOut(platformOrgService.getById(id));
    }

    @PostMapping("/create")
    @RequireAuth(permissions = {"platform:org:create"})
    @OperationLog(type = OperationType.CREATE, module = "platform_org", description = "创建平台组织")
    public MyResponseResult create(@Valid @RequestBody OrganizationCreateRequest request) {
        platformOrgService.create(request);
        return super.doJsonDefaultMsg();
    }

    @PostMapping("/update")
    @RequireAuth(permissions = {"platform:org:update"})
    @OperationLog(type = OperationType.UPDATE, module = "platform_org", description = "更新平台组织")
    public MyResponseResult update(@Valid @RequestBody OrganizationUpdateRequest request) {
        platformOrgService.update(request);
        return super.doJsonDefaultMsg();
    }

    @PostMapping("/delete")
    @RequireAuth(permissions = {"platform:org:delete"})
    @OperationLog(type = OperationType.DELETE, module = "platform_org", description = "删除平台组织")
    public MyResponseResult delete(@Valid @RequestBody OrganizationDeleteRequest request) {
        platformOrgService.delete(request.getIds());
        return super.doJsonDefaultMsg();
    }
}
