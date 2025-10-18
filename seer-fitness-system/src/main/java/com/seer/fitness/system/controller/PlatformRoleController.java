package com.seer.fitness.system.controller;

import com.seer.fitness.framework.annotation.PublicSchema;
import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.system.dto.*;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.system.service.PlatformRoleService;
import com.seer.fitness.system.util.SecurityContextUtil;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.mycommon.pager.PagerHandler;
import io.github.mocanjie.base.mymvc.controller.MyBaseController;
import io.github.mocanjie.base.mymvc.data.MyResponseResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台角色管理控制器
 * 负责平台角色模板的管理（public.sys_role）
 * <p>
 * 功能：
 * 1. 角色CRUD：创建、更新、删除、查询平台角色
 * 2. 权限配置：为角色配置菜单权限
 * 3. 角色分类：按 role_type 区分平台角色和租户模板角色
 * 4. 自动同步：更新租户模板角色时自动同步到已分配的租户
 * <p>
 * 权限要求：
 * - 所有接口需要平台角色管理权限（platform:role:*）
 * - 使用 @PublicSchema 确保操作 public.sys_role
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@RestController
@RequestMapping("/platform/role")
@PublicSchema(reason = "平台角色管理")
public class PlatformRoleController extends MyBaseController {

    @Autowired
    private PlatformRoleService platformRoleService;

    /**
     * 分页查询平台角色
     *
     * @param param 查询参数
     * @return 分页结果
     */
    @PostMapping("/search")
    @RequireAuth(permissions = {"platform:role:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "platform_role",
        description = "分页查询平台角色"
    )
    public MyResponseResult<Pager<RoleDTO>> search(@RequestBody RoleQueryParam param) {
        Pager<RoleDTO> result = platformRoleService.search(param, PagerHandler.createPager(param));
        return super.doJsonOut(result);
    }

    /**
     * 获取角色列表（不分页）
     *
     * @return 角色列表
     */
    @GetMapping("/list")
    @RequireAuth(permissions = {"platform:role:view"})
    public MyResponseResult<List<RoleDTO>> list() {
        List<RoleDTO> roles = platformRoleService.list();
        return super.doJsonOut(roles);
    }

    /**
     * 根据角色类型获取角色列表
     *
     * @param roleType 角色类型：1-平台专用角色 2-租户模板角色
     * @return 角色列表
     */
    @GetMapping("/list/{roleType}")
    @RequireAuth(permissions = {"platform:role:view"})
    public MyResponseResult<List<RoleDTO>> listByRoleType(@PathVariable Integer roleType) {
        List<RoleDTO> roles = platformRoleService.listByRoleType(roleType);
        return super.doJsonOut(roles);
    }

    /**
     * 根据ID获取角色详情
     *
     * @param id 角色ID
     * @return 角色详情
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = {"platform:role:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "platform_role",
        description = "查询平台角色详情"
    )
    public MyResponseResult<RoleDTO> getById(@PathVariable Long id) {
        return super.doJsonOut(platformRoleService.getById(id));
    }

    /**
     * 创建平台角色
     *
     * @param request 创建请求参数
     * @return 操作结果
     */
    @PostMapping("/create")
    @RequireAuth(permissions = {"platform:role:create"})
    @OperationLog(
        type = OperationType.CREATE,
        module = "platform_role",
        description = "创建平台角色"
    )
    public MyResponseResult<Void> create(@Valid @RequestBody RoleCreateRequest request) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdAsLong();
        platformRoleService.create(request, currentUserId);
        return super.doJsonDefaultMsg();
    }

    /**
     * 更新平台角色
     * 如果是租户模板角色（role_type=2），会自动同步到已分配的租户
     *
     * @param request 更新请求参数
     * @return 操作结果
     */
    @PostMapping("/update")
    @RequireAuth(permissions = {"platform:role:update"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "platform_role",
        description = "更新平台角色"
    )
    public MyResponseResult<Void> update(@Valid @RequestBody RoleUpdateRequest request) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdAsLong();
        platformRoleService.update(request, currentUserId);
        return super.doJsonDefaultMsg();
    }

    /**
     * 删除平台角色
     * 会检查：
     * 1. 是否有用户使用该角色
     * 2. 租户模板角色是否已分配给租户
     *
     * @param id 角色ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @RequireAuth(permissions = {"platform:role:delete"})
    @OperationLog(
        type = OperationType.DELETE,
        module = "platform_role",
        description = "删除平台角色"
    )
    public MyResponseResult<Void> delete(@PathVariable Long id) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdAsLong();
        platformRoleService.delete(id, currentUserId);
        return super.doJsonDefaultMsg();
    }

    /**
     * 为角色配置菜单权限
     * 如果是租户模板角色，会自动同步到已分配的租户
     *
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * @return 操作结果
     */
    @PostMapping("/{roleId}/assign-menus")
    @RequireAuth(permissions = {"platform:role:assign"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "platform_role",
        description = "配置平台角色菜单权限"
    )
    public MyResponseResult<Void> assignMenus(
            @PathVariable Long roleId,
            @RequestBody List<Long> menuIds) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdAsLong();
        platformRoleService.assignMenus(roleId, menuIds, currentUserId);
        return super.doJsonDefaultMsg();
    }

    /**
     * 获取角色的菜单权限
     *
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    @GetMapping("/{roleId}/menus")
    @RequireAuth(permissions = {"platform:role:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "platform_role",
        description = "查询平台角色菜单权限"
    )
    public MyResponseResult<List<Long>> getRoleMenus(@PathVariable Long roleId) {
        List<Long> menuIds = platformRoleService.getRoleMenuIds(roleId);
        return super.doJsonOut(menuIds);
    }
}
