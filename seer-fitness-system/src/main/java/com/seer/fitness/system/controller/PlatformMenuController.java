package com.seer.fitness.system.controller;

import com.seer.fitness.framework.annotation.PublicSchema;
import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.system.dto.MenuCreateRequest;
import com.seer.fitness.system.dto.MenuDTO;
import com.seer.fitness.system.dto.MenuTreeVO;
import com.seer.fitness.system.dto.MenuUpdateRequest;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.system.service.PlatformMenuService;
import com.seer.fitness.system.util.SecurityContextUtil;
import io.github.mocanjie.base.mymvc.controller.MyBaseController;
import io.github.mocanjie.base.mymvc.data.MyResponseResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台菜单管理控制器
 * 负责平台菜单模板的管理（public.sys_menu）
 * <p>
 * 功能：
 * 1. 菜单CRUD：创建、更新、删除、查询平台菜单
 * 2. 菜单树：获取完整菜单树、平台菜单树、租户模板菜单树
 * 3. 菜单分类：按 menu_type 区分平台菜单和租户模板菜单
 * <p>
 * 权限要求：
 * - 所有接口需要平台菜单管理权限（platform:menu:*）
 * - 使用 @PublicSchema 确保操作 public.sys_menu
 *
 * @author seer-fitness
 * @since 2025-10-17
 */
@RestController
@RequestMapping("/platform/menu")
@PublicSchema(reason = "平台菜单管理")
public class PlatformMenuController extends MyBaseController {

    @Autowired
    private PlatformMenuService platformMenuService;

    /**
     * 获取完整菜单树（平台菜单 + 租户模板菜单）
     *
     * @return 完整菜单树
     */
    @GetMapping("/tree")
    @RequireAuth(permissions = {"platform:menu:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "platform_menu",
        description = "获取平台菜单树"
    )
    public MyResponseResult<List<MenuTreeVO>> getMenuTree() {
        List<MenuTreeVO> tree = platformMenuService.getMenuTree();
        return super.doJsonOut(tree);
    }

    /**
     * 获取平台专用菜单树（仅 menu_type=1）
     *
     * @return 平台菜单树
     */
    @GetMapping("/tree/platform")
    @RequireAuth(permissions = {"platform:menu:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "platform_menu",
        description = "获取平台专用菜单树"
    )
    public MyResponseResult<List<MenuTreeVO>> getPlatformMenuTree() {
        List<MenuTreeVO> tree = platformMenuService.getPlatformMenuTree();
        return super.doJsonOut(tree);
    }

    /**
     * 获取租户模板菜单树（仅 menu_type=2）
     * 这些菜单可以分配给租户
     *
     * @return 租户模板菜单树
     */
    @GetMapping("/tree/tenant-template")
    @RequireAuth(permissions = {"platform:menu:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "platform_menu",
        description = "获取租户模板菜单树"
    )
    public MyResponseResult<List<MenuTreeVO>> getTenantTemplateMenuTree() {
        List<MenuTreeVO> tree = platformMenuService.getTenantTemplateMenuTree();
        return super.doJsonOut(tree);
    }

    /**
     * 获取菜单列表（不分页）
     *
     * @return 菜单列表
     */
    @GetMapping("/list")
    @RequireAuth(permissions = {"platform:menu:view"})
    public MyResponseResult<List<MenuDTO>> list() {
        List<MenuDTO> menus = platformMenuService.list();
        return super.doJsonOut(menus);
    }

    /**
     * 根据菜单类型获取菜单列表
     *
     * @param menuType 菜单类型：1-平台菜单 2-租户模板菜单
     * @return 菜单列表
     */
    @GetMapping("/list/{menuType}")
    @RequireAuth(permissions = {"platform:menu:view"})
    public MyResponseResult<List<MenuDTO>> listByMenuType(@PathVariable Integer menuType) {
        List<MenuDTO> menus = platformMenuService.listByMenuType(menuType);
        return super.doJsonOut(menus);
    }

    /**
     * 根据ID获取菜单详情
     *
     * @param id 菜单ID
     * @return 菜单详情
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = {"platform:menu:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "platform_menu",
        description = "查询平台菜单详情"
    )
    public MyResponseResult<MenuDTO> getById(@PathVariable Long id) {
        return super.doJsonOut(platformMenuService.getById(id));
    }

    /**
     * 创建平台菜单
     *
     * @param request 创建请求参数
     * @return 操作结果
     */
    @PostMapping("/create")
    @RequireAuth(permissions = {"platform:menu:create"})
    @OperationLog(
        type = OperationType.CREATE,
        module = "platform_menu",
        description = "创建平台菜单"
    )
    public MyResponseResult<Void> create(@Valid @RequestBody MenuCreateRequest request) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdAsLong();
        platformMenuService.create(request, currentUserId);
        return super.doJsonDefaultMsg();
    }

    /**
     * 更新平台菜单
     * 如果是租户模板菜单（menu_type=2），会自动同步到已分配的租户
     *
     * @param request 更新请求参数
     * @return 操作结果
     */
    @PostMapping("/update")
    @RequireAuth(permissions = {"platform:menu:update"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "platform_menu",
        description = "更新平台菜单"
    )
    public MyResponseResult<Void> update(@Valid @RequestBody MenuUpdateRequest request) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdAsLong();
        platformMenuService.update(request, currentUserId);
        return super.doJsonDefaultMsg();
    }

    /**
     * 删除平台菜单
     * 会检查：
     * 1. 是否有子菜单
     * 2. 租户模板菜单是否已分配给租户
     *
     * @param id 菜单ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @RequireAuth(permissions = {"platform:menu:delete"})
    @OperationLog(
        type = OperationType.DELETE,
        module = "platform_menu",
        description = "删除平台菜单"
    )
    public MyResponseResult<Void> delete(@PathVariable Long id) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdAsLong();
        platformMenuService.delete(id, currentUserId);
        return super.doJsonDefaultMsg();
    }
}
