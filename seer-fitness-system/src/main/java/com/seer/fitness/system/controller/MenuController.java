package com.seer.fitness.system.controller;

import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.dto.*;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.system.service.MenuService;
import com.seer.fitness.system.util.SecurityContextUtil;
import io.github.mocanjie.base.mymvc.controller.MyBaseController;
import io.github.mocanjie.base.mymvc.data.MyResponseResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
 * 提供菜单的增删改查、树形结构管理功能
 *
 * @author seer-fitness
 */
@RestController
@RequestMapping("/system/menu")
public class MenuController extends MyBaseController {

    @Autowired
    private MenuService menuService;

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
        String currentUserId = SecurityContextUtil.getCurrentUserId();
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

    /**
     * 创建菜单
     *
     * @param request 创建请求参数
     * @return 操作结果
     */
    @PostMapping("/create")
    @RequireAuth(permissions = {"menu:create"})
    @OperationLog(
        type = OperationType.CREATE,
        module = "menu",
        description = "创建菜单"
    )
    public MyResponseResult<Void> create(@Valid @RequestBody MenuCreateRequest request) {
        menuService.create(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 更新菜单
     *
     * @param request 更新请求参数
     * @return 操作结果
     */
    @PostMapping("/update")
    @RequireAuth(permissions = {"menu:update"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "menu",
        description = "更新菜单"
    )
    public MyResponseResult<Void> update(@Valid @RequestBody MenuUpdateRequest request) {
        menuService.update(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 删除菜单
     *
     * @param request 删除请求参数
     * @return 操作结果
     */
    @PostMapping("/delete")
    @RequireAuth(permissions = {"menu:delete"})
    @OperationLog(
        type = OperationType.DELETE,
        module = "menu",
        description = "删除菜单"
    )
    public MyResponseResult<Void> delete(@Valid @RequestBody MenuDeleteRequest request) {
        menuService.delete(request.getIds());
        return super.doJsonDefaultMsg();
    }
}