package com.seer.fitness.system.controller;

import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.dto.*;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.system.service.TenantMenuService;
import com.seer.fitness.system.util.SecurityContextUtil;
import io.github.mocanjie.base.mymvc.controller.MyBaseController;
import io.github.mocanjie.base.mymvc.data.MyResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户菜单查询控制器（只读）
 * 提供租户侧菜单的只读查询功能
 * <p>
 * 注意（2025-10-17 更新）：
 * - 租户不能创建、更新、删除菜单
 * - 菜单由平台分配后复制到租户schema
 * - 租户可以通过角色管理控制菜单权限分配
 * - 所有增删改接口已移除
 *
 * @author seer-fitness
 */
@RestController
@RequestMapping("/system/menu")
public class MenuController extends MyBaseController {

    @Autowired
    private TenantMenuService menuService;

    /**
     * 获取菜单树
     * 返回完整的菜单树形结构
     *
     * @return 菜单树形结构
     */
    @GetMapping("/tree")
    @RequireAuth(permissions = {"menu:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "menu",
        description = "获取菜单树"
    )
    public MyResponseResult<List<MenuTreeVO>> getMenuTree() {
        List<MenuTreeVO> tree = menuService.getMenuTree();
        return super.doJsonOut(tree);
    }

    /**
     * 获取当前用户菜单
     * 根据用户权限返回用户可访问的菜单树
     *
     * @return 用户菜单树形结构
     */
    @GetMapping("/user-menus")
    @RequireAuth(login = true)
    public MyResponseResult<List<MenuTreeVO>> getUserMenus() {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdAsLong();
        List<MenuTreeVO> userMenus = menuService.getUserMenuTree(currentUserId);
        return super.doJsonOut(userMenus);
    }

    /**
     * 获取菜单列表（不分页）
     * 返回扁平的菜单列表，用于下拉选择框等场景
     *
     * @return 菜单列表
     */
    @GetMapping("/list")
    @RequireAuth(permissions = {"menu:view"})
    public MyResponseResult<List<MenuDTO>> list() {
        List<MenuDTO> menus = menuService.list();
        return super.doJsonOut(menus);
    }

    /**
     * 根据ID获取菜单详情
     *
     * @param id 菜单ID
     * @return 菜单详情
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = {"menu:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "menu",
        description = "查询菜单详情"
    )
    public MyResponseResult<MenuDTO> getById(@PathVariable Long id) {
        return super.doJsonOut(menuService.getById(id));
    }

    // ========================================
    // 以下接口已移除（2025-10-17）
    // ========================================
    //
    // 租户不能创建、更新、删除菜单
    // 菜单由平台通过菜单分配接口分配
    // 已移除的接口：
    // - POST /system/menu/create  (menu:create)
    // - POST /system/menu/update  (menu:update)
    // - POST /system/menu/delete  (menu:delete)
    //
    // 替代方案：
    // - 平台菜单管理：使用 /platform/menu/*
    // - 菜单分配：使用 /platform/tenant/menu/assign
    // ========================================
}
