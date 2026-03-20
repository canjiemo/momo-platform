package io.github.canjiemo.momo.system.controller;

import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.momo.framework.annotation.OperationLog;
import io.github.canjiemo.momo.framework.annotation.RequireAuth;
import io.github.canjiemo.momo.framework.enums.OperationType;
import io.github.canjiemo.momo.system.dto.*;
import io.github.canjiemo.momo.system.service.ITenantService;
import io.github.canjiemo.momo.system.service.IUserService;
import io.github.canjiemo.mycommon.pager.Pager;
import io.github.canjiemo.mycommon.pager.PagerHandler;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户管理控制器
 * 提供租户的增删改查、启用/禁用等功能
 *
 * @author canjiemo@gmail.com
 */
@RestController
@RequestMapping("/platform/tenant")
public class TenantController extends MyBaseController {

    @Autowired
    private ITenantService tenantService;

    @Autowired
    private IUserService userService;

    /**
     * 分页查询租户
     */
    @PostMapping("/search")
    @RequireAuth(permissions = {"tenant:view"})
    @OperationLog(type = OperationType.QUERY, module = "tenant", description = "分页查询租户")
    public MyResponseResult<Pager<TenantDTO>> search(@RequestBody TenantQueryParam param) {
        return super.doJsonPagerOut(tenantService.search(param, PagerHandler.createPager(param)));
    }

    /**
     * 根据ID获取租户详情
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = {"tenant:view"})
    @OperationLog(type = OperationType.QUERY, module = "tenant", description = "查询租户详情")
    public MyResponseResult<TenantDTO> getById(@PathVariable Long id) {
        return super.doJsonOut(tenantService.getById(id));
    }

    /**
     * 根据租户编码获取租户信息
     */
    @GetMapping("/code/{tenantCode}")
    @RequireAuth(permissions = {"tenant:view"})
    @OperationLog(type = OperationType.QUERY, module = "tenant", description = "根据编码查询租户")
    public MyResponseResult<TenantDTO> getByCode(@PathVariable String tenantCode) {
        return super.doJsonOut(tenantService.getByCode(tenantCode));
    }

    /**
     * 创建租户
     * 在 sys_tenant 表中插入记录，租户即刻激活
     */
    @PostMapping("/create")
    @RequireAuth(permissions = {"tenant:create"})
    @OperationLog(type = OperationType.CREATE, module = "tenant", description = "创建租户")
    public MyResponseResult create(@Valid @RequestBody TenantCreateRequest request) {
        tenantService.create(request);
        return super.doJsonMsg("租户创建成功");
    }

    /**
     * 更新租户信息
     */
    @PostMapping("/update")
    @RequireAuth(permissions = {"tenant:update"})
    @OperationLog(type = OperationType.UPDATE, module = "tenant", description = "更新租户")
    public MyResponseResult update(@Valid @RequestBody TenantUpdateRequest request) {
        tenantService.update(request);
        return super.doJsonMsg("租户更新成功");
    }

    /**
     * 启用租户
     */
    @PostMapping("/enable/{id}")
    @RequireAuth(permissions = {"tenant:update"})
    @OperationLog(type = OperationType.UPDATE, module = "tenant", description = "启用租户")
    public MyResponseResult enable(@PathVariable Long id) {
        tenantService.enable(id);
        return super.doJsonMsg("租户启用成功");
    }

    /**
     * 禁用租户
     */
    @PostMapping("/disable/{id}")
    @RequireAuth(permissions = {"tenant:update"})
    @OperationLog(type = OperationType.UPDATE, module = "tenant", description = "禁用租户")
    public MyResponseResult disable(@PathVariable Long id) {
        tenantService.disable(id);
        return super.doJsonMsg("租户禁用成功");
    }

    /**
     * 删除租户（逻辑删除）
     */
    @PostMapping("/delete/{id}")
    @RequireAuth(permissions = {"tenant:delete"})
    @OperationLog(type = OperationType.DELETE, module = "tenant", description = "删除租户")
    public MyResponseResult delete(@PathVariable Long id) {
        tenantService.delete(id);
        return super.doJsonMsg("租户删除成功");
    }

    /**
     * 检查租户编码是否可用
     */
    @GetMapping("/check-code/{tenantCode}")
    @RequireAuth(permissions = {"tenant:create"})
    public MyResponseResult<Boolean> checkCode(@PathVariable String tenantCode) {
        boolean exists = tenantService.existsByCode(tenantCode);
        return super.doJsonOut(!exists);
    }

    /**
     * 平台用户列表（只查 tenant_id=null 的平台用户）
     */
    @GetMapping("/platform-users")
    @RequireAuth(permissions = {"tenant:view"})
    public MyResponseResult<List<UserDTO>> listPlatformUsers() {
        return super.doJsonOut(userService.listPlatformUsers());
    }

    /**
     * 获取租户已分配的平台角色 ID 列表
     */
    @GetMapping("/{id}/roles")
    @RequireAuth(permissions = {"tenant:view"})
    public MyResponseResult<List<Long>> getTenantRoles(@PathVariable Long id) {
        return super.doJsonOut(tenantService.getTenantRoleIds(id));
    }

    /**
     * 为租户分配/更新平台角色
     */
    @PostMapping("/{id}/assign-roles")
    @RequireAuth(permissions = {"tenant:update"})
    @OperationLog(type = OperationType.UPDATE, module = "tenant", description = "分配租户角色")
    public MyResponseResult assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        tenantService.assignRoles(id, roleIds);
        return super.doJsonMsg("角色分配成功");
    }
}
