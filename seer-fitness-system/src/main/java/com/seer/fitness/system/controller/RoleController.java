package com.seer.fitness.system.controller;

import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.system.dto.*;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.system.service.IRoleService;
import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.mycommon.pager.Pager;
import io.github.canjiemo.mycommon.pager.PagerHandler;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器（租户侧）
 * 提供角色的增删改查、权限分配功能
 * <p>
 * 更新说明（2025-10-18）：
 * - 平台同步的角色（platform_role_id 不为空）受服务层保护
 * - 租户无法修改/删除平台同步的角色
 * - 尝试修改/删除时会收到 BusinessException 错误
 * - 这些角色由平台管理，租户只能查看和使用
 *
 * @author seer-fitness
 */
@RestController
@RequestMapping("/system/role")
public class RoleController extends MyBaseController {

    @Autowired
    private IRoleService roleService;

    /**
     * 分页查询角色列表
     * 支持复杂查询条件、分页、排序
     *
     * @param param 查询参数
     * @return 分页结果
     */
    @PostMapping("/search")
    @RequireAuth(permissions = {"role:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "role",
        description = "分页查询角色"
    )
    public MyResponseResult<Pager<RoleDTO>> search(@RequestBody RoleQueryParam param) {
        return super.doJsonPagerOut(roleService.search(param, PagerHandler.createPager(param)));
    }

    /**
     * 获取角色列表（不分页）
     * 用于下拉选择框等场景
     *
     * @return 角色列表
     */
    @GetMapping("/list")
    @RequireAuth(login = true)
    public MyResponseResult<List<RoleDTO>> list(@RequestParam(required = false) Long tenantId) {
        List<RoleDTO> roles = roleService.list(tenantId);
        return super.doJsonOut(roles);
    }

    /**
     * 根据ID获取角色详情
     *
     * @param id 角色ID
     * @return 角色详情
     */
    @GetMapping("/{id}")
    @RequireAuth(login = true)
    @OperationLog(
        type = OperationType.QUERY,
        module = "role",
        description = "查询角色详情"
    )
    public MyResponseResult<RoleDTO> getById(@PathVariable String id) {
        return super.doJsonOut(roleService.getById(id));
    }

    /**
     * 创建角色
     *
     * @param request 创建请求参数
     * @return 操作结果
     */
    @PostMapping("/create")
    @RequireAuth(permissions = {"role:create"})
    @OperationLog(
        type = OperationType.CREATE,
        module = "role",
        description = "创建角色"
    )
    public MyResponseResult create(@Valid @RequestBody RoleCreateRequest request) {
        roleService.create(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 更新角色信息
     *
     * @param request 更新请求参数
     * @return 操作结果
     */
    @PostMapping("/update")
    @RequireAuth(permissions = {"role:update"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "role",
        description = "更新角色"
    )
    public MyResponseResult update(@Valid @RequestBody RoleUpdateRequest request) {
        roleService.update(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 批量逻辑删除角色
     *
     * @param request 删除请求参数
     * @return 操作结果
     */
    @PostMapping("/delete")
    @RequireAuth(permissions = {"role:delete"})
    @OperationLog(
        type = OperationType.DELETE,
        module = "role",
        description = "删除角色"
    )
    public MyResponseResult delete(@Valid @RequestBody RoleDeleteRequest request) {
        roleService.delete(request.getIds());
        return super.doJsonDefaultMsg();
    }

    /**
     * 分配菜单权限
     *
     * @param request 分配请求参数
     * @return 操作结果
     */
    @PostMapping("/assign-menus")
    @RequireAuth(permissions = {"role:assign"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "role",
        description = "分配角色菜单权限"
    )
    public MyResponseResult assignMenus(@RequestBody AssignMenusRequest request) {
        roleService.assignMenus(String.valueOf(request.getRoleId()), request.getMenuIds());
        return super.doJsonDefaultMsg();
    }

    /**
     * 获取角色的菜单ID列表
     *
     * @param id 角色ID
     * @return 菜单ID列表
     */
    @GetMapping("/menus/{id}")
    @RequireAuth(permissions = {"role:assign"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "role",
        description = "查询角色菜单权限"
    )
    public MyResponseResult<List<String>> getRoleMenus(@PathVariable String id) {
        List<String> menuIds = roleService.getRoleMenuIds(id);
        return super.doJsonOut(menuIds);
    }

    /**
     * 查询指定角色下的用户列表
     *
     * @param roleId 角色ID
     * @return 用户列表
     */
    @GetMapping("/{roleId}/users")
    @RequireAuth(login = true)
    public MyResponseResult<List<UserDTO>> getUsersByRole(@PathVariable Long roleId) {
        return super.doJsonOut(roleService.getUsersByRole(roleId));
    }
}