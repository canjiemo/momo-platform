package com.seer.fitness.system.controller;

import com.seer.fitness.framework.annotation.OperationLog;
import com.seer.fitness.system.dto.RoleCreateRequest;
import com.seer.fitness.system.dto.RoleDTO;
import com.seer.fitness.system.dto.RoleQueryParam;
import com.seer.fitness.system.dto.RoleUpdateRequest;
import com.seer.fitness.framework.enums.OperationType;
import com.seer.fitness.framework.annotation.RequireAuth;
import com.seer.fitness.system.service.IPlatformRoleService;
import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.mycommon.pager.Pager;
import io.github.canjiemo.mycommon.pager.PagerHandler;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台角色管理控制器（/platform/role）
 * 管理可分配给租户的平台角色模板（tenant_id=NULL 的角色）
 *
 * @author seer-fitness
 */
@RestController
@RequestMapping("/platform/role")
public class PlatformRoleController extends MyBaseController {

    @Autowired
    private IPlatformRoleService platformRoleService;

    /**
     * 分页查询平台角色
     */
    @PostMapping("/search")
    @RequireAuth(permissions = {"platform:role:view"})
    public MyResponseResult<Pager<RoleDTO>> search(@RequestBody RoleQueryParam param) {
        return super.doJsonPagerOut(platformRoleService.search(param, PagerHandler.createPager(param)));
    }

    /**
     * 平台角色列表（不分页，用于租户创建时的角色选择下拉）
     */
    @GetMapping("/list")
    @RequireAuth(login = true)
    public MyResponseResult<List<RoleDTO>> list() {
        return super.doJsonOut(platformRoleService.list());
    }

    /**
     * 根据ID获取平台角色详情
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = {"platform:role:view"})
    public MyResponseResult<RoleDTO> getById(@PathVariable Long id) {
        return super.doJsonOut(platformRoleService.getById(id));
    }

    /**
     * 创建平台角色
     */
    @PostMapping("/create")
    @RequireAuth(permissions = {"platform:role:create"})
    @OperationLog(type = OperationType.CREATE, module = "platform_role", description = "创建平台角色")
    public MyResponseResult create(@Valid @RequestBody RoleCreateRequest request) {
        platformRoleService.create(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 更新平台角色
     */
    @PostMapping("/update")
    @RequireAuth(permissions = {"platform:role:update"})
    @OperationLog(type = OperationType.UPDATE, module = "platform_role", description = "更新平台角色")
    public MyResponseResult update(@Valid @RequestBody RoleUpdateRequest request) {
        platformRoleService.update(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 删除平台角色（检查是否有租户在使用）
     */
    @PostMapping("/delete/{id}")
    @RequireAuth(permissions = {"platform:role:delete"})
    @OperationLog(type = OperationType.DELETE, module = "platform_role", description = "删除平台角色")
    public MyResponseResult delete(@PathVariable Long id) {
        platformRoleService.delete(id);
        return super.doJsonDefaultMsg();
    }

    /**
     * 获取平台角色已分配的菜单 ID 列表
     */
    @GetMapping("/{id}/menus")
    @RequireAuth(permissions = {"platform:role:view"})
    public MyResponseResult<List<String>> getRoleMenus(@PathVariable Long id) {
        return super.doJsonOut(platformRoleService.getRoleMenuIds(id));
    }

    /**
     * 为平台角色分配菜单（全量替换）
     */
    @PostMapping("/{id}/assign-menus")
    @RequireAuth(permissions = {"platform:role:assign"})
    @OperationLog(type = OperationType.UPDATE, module = "platform_role", description = "分配平台角色菜单")
    public MyResponseResult assignMenus(@PathVariable Long id, @RequestBody List<String> menuIds) {
        platformRoleService.assignMenus(id, menuIds);
        return super.doJsonDefaultMsg();
    }
}
