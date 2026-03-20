package io.github.canjiemo.momo.system.controller;

import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.momo.framework.annotation.OperationLog;
import io.github.canjiemo.momo.framework.annotation.RequireAuth;
import io.github.canjiemo.momo.framework.enums.OperationType;
import io.github.canjiemo.momo.framework.utils.SecurityContextUtil;
import io.github.canjiemo.momo.system.dto.MenuCreateRequest;
import io.github.canjiemo.momo.system.dto.MenuDTO;
import io.github.canjiemo.momo.system.dto.MenuTreeVO;
import io.github.canjiemo.momo.system.dto.MenuUpdateRequest;
import io.github.canjiemo.momo.system.service.IMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单查询控制器
 *
 * @author canjiemo@gmail.com
 */
@RestController
@RequestMapping("/system/menu")
public class MenuController extends MyBaseController {

    @Autowired
    private IMenuService menuService;

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
        List<MenuTreeVO> userMenus = menuService.getUserMenuTree(String.valueOf(currentUserId));
        return super.doJsonOut(userMenus);
    }

    /**
     * 获取菜单列表（不分页）
     * 返回扁平的菜单列表，用于下拉选择框等场景
     *
     * @return 菜单列表
     */
    @GetMapping("/list")
    @RequireAuth(login = true)
    public MyResponseResult<List<MenuDTO>> list() {
        List<MenuDTO> menus = menuService.list();
        return super.doJsonOut(menus);
    }

    /**
     * 根据ID获取菜单详情
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = {"menu:view"})
    @OperationLog(type = OperationType.QUERY, module = "menu", description = "查询菜单详情")
    public MyResponseResult<MenuDTO> getById(@PathVariable Long id) {
        return super.doJsonOut(menuService.getById(id));
    }

    /**
     * 创建菜单
     */
    @PostMapping("/create")
    @RequireAuth(permissions = {"menu:create"})
    @OperationLog(type = OperationType.CREATE, module = "menu", description = "创建菜单")
    public MyResponseResult create(@RequestBody MenuCreateRequest request) {
        menuService.create(request);
        return super.doJsonMsg("菜单创建成功");
    }

    /**
     * 更新菜单
     */
    @PostMapping("/update")
    @RequireAuth(permissions = {"menu:update"})
    @OperationLog(type = OperationType.UPDATE, module = "menu", description = "更新菜单")
    public MyResponseResult update(@RequestBody MenuUpdateRequest request) {
        menuService.update(request);
        return super.doJsonMsg("菜单更新成功");
    }

    /**
     * 删除菜单
     */
    @PostMapping("/delete")
    @RequireAuth(permissions = {"menu:delete"})
    @OperationLog(type = OperationType.DELETE, module = "menu", description = "删除菜单")
    public MyResponseResult delete(@RequestBody java.util.Map<String, Long> body) {
        menuService.delete(String.valueOf(body.get("id")));
        return super.doJsonMsg("菜单删除成功");
    }
}
