package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import org.springframework.beans.BeanUtils;
import com.seer.fitness.system.dto.MenuCreateRequest;
import com.seer.fitness.system.dto.MenuDTO;
import com.seer.fitness.system.dto.MenuTreeVO;
import com.seer.fitness.system.dto.MenuUpdateRequest;
import com.seer.fitness.system.entity.SysMenu;
import com.seer.fitness.system.entity.SysRoleMenu;
import com.seer.fitness.system.entity.SysUser;
import com.seer.fitness.system.utils.SecurityContextUtil;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.base.myjdbc.tenant.TenantContext;
import io.github.canjiemo.mycommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单管理服务实现
 * 提供菜单的增删改查、树形结构管理、权限控制功能
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class MenuService extends BaseServiceImpl implements IMenuService {

    @Autowired
    private IRoleService roleService;

    /**
     * 获取菜单树形结构
     * - 平台超管：返回全部启用菜单（用于菜单管理）
     * - 租户管理员：仅返回平台为该租户授权的菜单（用于角色分配菜单选择框）
     */
    public List<MenuTreeVO> getMenuTree() {
        com.seer.fitness.system.dto.UserCacheInfo currentUser =
                SecurityContextUtil.getCurrentUser();

        boolean isTenantAdmin = currentUser != null
                && Integer.valueOf(1).equals(currentUser.getAdminFlag())
                && currentUser.getTenantId() != null;

        if (isTenantAdmin) {
            List<Long> allowedIds = getTenantAllowedMenuIds(currentUser.getTenantId());
            if (allowedIds.isEmpty()) return List.of();
            String ids = allowedIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            String sql = "SELECT id, menu_name, path, parent_id, type, permission, icon, sort_order, status " +
                        "FROM sys_menu WHERE id IN (" + ids + ") AND status = 1 ORDER BY sort_order";
            List<MenuDTO> menus = TenantContext.withoutTenant(() ->
                    baseDao.queryListForSql(sql, Maps.newHashMap(), MenuDTO.class));
            return buildMenuTree(menus, null);
        }

        String sql = "SELECT id, menu_name, path, parent_id, type, permission, icon, sort_order, status " +
                    "FROM sys_menu WHERE status = 1 ORDER BY sort_order";
        List<MenuDTO> allMenus = baseDao.queryListForSql(sql, Maps.newHashMap(), MenuDTO.class);
        return buildMenuTree(allMenus, null);
    }

    /**
     * 获取用户菜单树（目录+菜单，不含按钮）
     * - 平台超管：所有菜单
     * - 租户管理员：sys_tenant_role → 平台角色菜单（动态，随平台调整实时生效）
     * - 普通用户：自身角色菜单 ∩ 租户允许菜单（平台收回菜单立即失效）
     */
    public List<MenuTreeVO> getUserMenuTree(String userId) {
        SysUser user = baseDao.queryById(Long.parseLong(userId), SysUser.class);
        List<Long> menuIds = resolveUserMenuIds(user, Long.parseLong(userId), true);
        if (menuIds.isEmpty()) return List.of();

        String ids = menuIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String menuSql = "SELECT id, menu_name, path, parent_id, type, permission, icon, sort_order, status " +
                        "FROM sys_menu WHERE id IN (" + ids + ") AND status = 1 AND type IN (0, 1) ORDER BY sort_order";
        List<MenuDTO> menus = TenantContext.withoutTenant(() ->
                baseDao.queryListForSql(menuSql, Maps.newHashMap(), MenuDTO.class));
        return buildMenuTree(menus, null);
    }

    /**
     * 获取用户扁平菜单列表（目录+菜单，不含按钮），用于导航渲染
     * 逻辑同 getUserMenuTree，返回扁平结构
     */
    public List<MenuDTO> getUserMenus(String userId) {
        SysUser user = baseDao.queryById(Long.parseLong(userId), SysUser.class);
        List<Long> menuIds = resolveUserMenuIds(user, Long.parseLong(userId), true);
        if (menuIds.isEmpty()) return List.of();

        String ids = menuIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String menuSql = "SELECT id, menu_name, path, parent_id, type, permission, icon, sort_order, status " +
                        "FROM sys_menu WHERE id IN (" + ids + ") AND status = 1 AND type IN (0, 1) ORDER BY sort_order";
        return TenantContext.withoutTenant(() ->
                baseDao.queryListForSql(menuSql, Maps.newHashMap(), MenuDTO.class));
    }

    /**
     * 获取用户权限字符串列表，用于前端按钮权限控制和后端接口权限校验
     * 注：平台超管由 AuthInterceptor 直接放行，此处返回空列表即可
     */
    public List<String> getUserPermissions(Long userId) {
        SysUser user = baseDao.queryById(userId, SysUser.class);
        List<Long> menuIds = resolveUserMenuIds(user, userId, false);
        if (menuIds.isEmpty()) return List.of();

        String ids = menuIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String permSql = "SELECT DISTINCT permission FROM sys_menu " +
                        "WHERE id IN (" + ids + ") AND status = 1 AND permission IS NOT NULL";
        return TenantContext.withoutTenant(() ->
                baseDao.queryListForSql(permSql, Maps.newHashMap(), String.class)
                        .stream()
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toList()));
    }

    /**
     * 解析用户可访问的菜单 ID 列表（核心分支逻辑）
     * @param navOnly true=只返回目录+菜单（type 0,1），false=包含全部（用于权限字符串）
     */
    private List<Long> resolveUserMenuIds(SysUser user, Long userId, boolean navOnly) {
        if (user == null) return List.of();

        boolean isPlatformSuperAdmin = Integer.valueOf(1).equals(user.getAdminFlag()) && user.getTenantId() == null;
        if (isPlatformSuperAdmin) {
            // 平台超管：返回所有菜单（AuthInterceptor 已放行，此分支主要用于 profile 接口展示）
            String sql = navOnly
                    ? "SELECT id FROM sys_menu WHERE status = 1 AND type IN (0, 1)"
                    : "SELECT id FROM sys_menu WHERE status = 1";
            return baseDao.queryListForSql(sql, Maps.newHashMap(), Long.class);
        }

        boolean isTenantAdmin = Integer.valueOf(1).equals(user.getAdminFlag()) && user.getTenantId() != null;
        if (isTenantAdmin) {
            // 租户管理员：通过 sys_tenant_role → 平台角色菜单动态获取（永远全部）
            List<Long> allIds = getTenantAllowedMenuIds(user.getTenantId());
            return navOnly ? filterNavIds(allIds) : allIds;
        }

        // 普通租户用户：取自身角色菜单 ∩ 租户允许菜单
        List<Long> allIds = getAccessibleMenuIds(userId, user.getTenantId());
        return navOnly ? filterNavIds(allIds) : allIds;
    }

    /**
     * 从菜单 ID 列表中筛选导航节点（type 0,1），并自动补全按钮的父节点
     * 确保即使 sys_role_menu 只存按钮 ID 也能正常渲染导航树
     */
    private List<Long> filterNavIds(List<Long> menuIds) {
        if (menuIds.isEmpty()) return List.of();
        String ids = menuIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        // 1. 取 type IN (0,1) 的目录和菜单节点
        String navSql = "SELECT id FROM sys_menu WHERE id IN (" + ids + ") AND status = 1 AND type IN (0, 1)";
        List<Long> navIds = TenantContext.withoutTenant(() ->
                baseDao.queryListForSql(navSql, Maps.newHashMap(), Long.class));

        // 2. 取按钮节点的 parent_id（菜单层），防止只存按钮时导航树为空
        String menuLevelSql = "SELECT DISTINCT parent_id FROM sys_menu WHERE id IN (" + ids + ") AND type = 2 AND parent_id != 0";
        List<Long> menuLevelIds = TenantContext.withoutTenant(() ->
                baseDao.queryListForSql(menuLevelSql, Maps.newHashMap(), Long.class));

        Set<Long> result = new HashSet<>(navIds);
        if (!menuLevelIds.isEmpty()) {
            result.addAll(menuLevelIds);
            // 3. 再取菜单节点的 parent_id（目录层）
            String dirLevelSql = "SELECT DISTINCT parent_id FROM sys_menu WHERE id IN (" +
                    menuLevelIds.stream().map(String::valueOf).collect(Collectors.joining(",")) +
                    ") AND parent_id != 0";
            List<Long> dirLevelIds = TenantContext.withoutTenant(() ->
                    baseDao.queryListForSql(dirLevelSql, Maps.newHashMap(), Long.class));
            result.addAll(dirLevelIds);
        }
        return new ArrayList<>(result);
    }

    /**
     * 获取租户允许的菜单 ID 列表（通过 sys_tenant_role → 平台 sys_role_menu 动态查）
     * 平台调整角色菜单后，此处立即生效
     */
    private List<Long> getTenantAllowedMenuIds(Long tenantId) {
        String roleIdSql = "SELECT role_id FROM sys_tenant_role WHERE tenant_id = :tenantId";
        Map<String, Object> params = Maps.newHashMap();
        params.put("tenantId", tenantId);
        List<Long> platformRoleIds = baseDao.queryListForSql(roleIdSql, params, Long.class);

        if (platformRoleIds.isEmpty()) return List.of();

        String roleIds = platformRoleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String menuIdSql = "SELECT DISTINCT menu_id FROM sys_role_menu WHERE role_id IN (" + roleIds + ") AND tenant_id IS NULL";
        return TenantContext.withoutTenant(() ->
                baseDao.queryListForSql(menuIdSql, Maps.newHashMap(), Long.class));
    }

    /**
     * 获取普通租户用户实际可访问的菜单 ID（自身角色菜单 ∩ 租户允许菜单）
     * 保证：平台收回租户的菜单权限后，用户即使还有角色分配也无法访问
     */
    private List<Long> getAccessibleMenuIds(Long userId, Long tenantId) {
        // 用户的角色 IDs（myjpa 自动注入 tenant_id 过滤）
        String userRoleSql = "SELECT role_id FROM sys_user_role WHERE user_id = :userId";
        Map<String, Object> params = Maps.newHashMap();
        params.put("userId", userId);
        List<Long> roleIds = baseDao.queryListForSql(userRoleSql, params, Long.class);
        if (roleIds.isEmpty()) return List.of();

        // 用户角色的菜单 IDs（myjpa 自动注入 tenant_id 过滤）
        String roleIdStr = roleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String userMenuIdSql = "SELECT DISTINCT menu_id FROM sys_role_menu WHERE role_id IN (" + roleIdStr + ")";
        List<Long> userMenuIds = baseDao.queryListForSql(userMenuIdSql, Maps.newHashMap(), Long.class);
        if (userMenuIds.isEmpty()) return List.of();

        // 租户允许的菜单 IDs（基于平台角色动态决定）
        List<Long> allowedMenuIds = getTenantAllowedMenuIds(tenantId);
        if (allowedMenuIds.isEmpty()) return List.of();

        // 取交集：只返回租户还有效的菜单
        Set<Long> allowedSet = new HashSet<>(allowedMenuIds);
        return userMenuIds.stream().filter(allowedSet::contains).collect(Collectors.toList());
    }

    /**
     * 获取所有菜单列表（不分页）
     * 用于管理界面的下拉选择框等场景
     *
     * @return 菜单列表
     */
    public List<MenuDTO> list() {
        String sql = "SELECT id, menu_name, path, parent_id, type, permission, icon, sort_order, " +
                    "status, create_time, update_time " +
                    "FROM sys_menu ORDER BY sort_order";

        return baseDao.queryListForSql(sql, Maps.newHashMap(), MenuDTO.class);
    }

    /**
     * 根据ID获取菜单详情
     *
     * @param id 菜单ID
     * @return 菜单详情
     * @throws BusinessException 当菜单不存在时抛出
     */
    public MenuDTO getById(Long id) {
        if (id == null) {
            throw new BusinessException("菜单ID不能为空");
        }

        SysMenu menu = baseDao.queryById(id, SysMenu.class);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }

        MenuDTO dto = new MenuDTO();
        BeanUtils.copyProperties(menu, dto);
        dto.setParentId(menu.getParentId() != null ? menu.getParentId() : 0L);
        return dto;
    }

    /**
     * 创建菜单
     *
     * @param request 创建请求参数
     * @throws BusinessException 当父菜单不存在或其他业务错误时抛出
     */
    @Transactional(readOnly = false)
    public void create(MenuCreateRequest request) {
        // 处理parentId，null或0表示顶级菜单
        Long parentId = request.getParentId() != null ? request.getParentId() : 0L;

        // 验证父菜单是否存在（如果有设置）
        if (parentId != 0L) {
            SysMenu parentMenu = baseDao.queryById(parentId, SysMenu.class);
            if (parentMenu == null) {
                throw new BusinessException("父菜单不存在");
            }
        }

        SysMenu menu = new SysMenu();
        menu.setMenuName(request.getMenuName());
        menu.setPath(request.getPath());
        menu.setParentId(parentId);
        menu.setType(request.getType());
        menu.setPermission(request.getPermission());
        menu.setIcon(request.getIcon());
        menu.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        menu.setStatus(request.getStatus());
        menu.setDeleteFlag(0);
        menu.setCreateTime(LocalDateTime.now());
        menu.setUpdateTime(LocalDateTime.now());

        baseDao.insertPO(menu, true);

        log.info("创建菜单成功: menuName={}, id={}", request.getMenuName(), menu.getId());
    }

    /**
     * 更新菜单
     *
     * @param request 更新请求参数
     * @throws BusinessException 当菜单不存在、父菜单不存在或其他业务错误时抛出
     */
    @Transactional(readOnly = false)
    public void update(MenuUpdateRequest request) {
        SysMenu menu = baseDao.queryById(request.getId(), SysMenu.class);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }

        // 处理parentId，null或0表示顶级菜单
        Long parentId = request.getParentId() != null ? request.getParentId() : 0L;

        // 验证父菜单是否存在（如果有设置）
        if (parentId != 0L) {
            if (parentId.equals(request.getId())) {
                throw new BusinessException("不能将自己设为父菜单");
            }

            SysMenu parentMenu = baseDao.queryById(parentId, SysMenu.class);
            if (parentMenu == null) {
                throw new BusinessException("父菜单不存在");
            }
        }

        menu.setMenuName(request.getMenuName());
        menu.setPath(request.getPath());
        menu.setParentId(parentId);
        menu.setType(request.getType());
        menu.setPermission(request.getPermission());
        menu.setIcon(request.getIcon());
        menu.setSortOrder(request.getSortOrder());
        menu.setStatus(request.getStatus());
        menu.setUpdateTime(LocalDateTime.now());

        baseDao.updatePO(menu);

        log.info("更新菜单成功: id={}, menuName={}", request.getId(), request.getMenuName());
    }

    /**
     * 删除菜单
     * 支持批量删除，会检查子菜单和角色关联
     *
     * @param ids 菜单ID数组
     * @throws BusinessException 当菜单不存在或存在子菜单时抛出
     */
    @Transactional(readOnly = false)
    public void delete(String... ids) {
        if (ids == null || ids.length == 0) {
            throw new BusinessException("删除的菜单ID不能为空");
        }

        for (String id : ids) {
            if (!StringUtils.hasText(id)) {
                throw new BusinessException("菜单ID不能为空");
            }

            Long menuId = Long.valueOf(id);

            // 检查是否有子菜单
            if (hasChildren(menuId)) {
                throw new BusinessException("该菜单存在子菜单，无法删除");
            }

            // 删除角色菜单关联
            removeMenuFromRoles(menuId);
        }

        // 逻辑删除菜单
        baseDao.delByIds(SysMenu.class, ids);

        log.info("删除菜单成功: ids={}", (Object) ids);
    }

    /**
     * 构建菜单树
     */
    private List<MenuTreeVO> buildMenuTree(List<MenuDTO> menus, Long parentId) {
        return menus.stream()
                .filter(menu -> {
                    if (parentId == null) {
                        // 查找顶级菜单：parentId为0的菜单
                        return menu.getParentId() == 0L;
                    }
                    return parentId.equals(menu.getParentId());
                })
                .map(menu -> {
                    MenuTreeVO treeNode = new MenuTreeVO();
                    BeanUtils.copyProperties(menu, treeNode);
                    treeNode.setChildren(buildMenuTree(menus, menu.getId()));
                    return treeNode;
                })
                .collect(Collectors.toList());
    }

    /**
     * 检查是否有子菜单
     */
    private boolean hasChildren(Long menuId) {
        String sql = "SELECT COUNT(*) FROM sys_menu WHERE parent_id = :parentId";
        Map<String, Object> params = Maps.newHashMap();
        params.put("parentId", menuId);

        Long count = baseDao.querySingleForSql(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * 从所有角色中移除菜单
     */
    private void removeMenuFromRoles(Long menuId) {
        String sql = "SELECT * FROM sys_role_menu WHERE menu_id = :menuId";
        Map<String, Object> params = Maps.newHashMap();
        params.put("menuId", menuId);

        List<SysRoleMenu> roleMenus = baseDao.queryListForSql(sql, params, SysRoleMenu.class);
        for (SysRoleMenu roleMenu : roleMenus) {
            baseDao.delByIds(SysRoleMenu.class, roleMenu.getId().toString());
        }
    }

    /**
     * 标准化parentId处理
     * 将null、空字符串""、"0"统一处理为 0
     *
     * @param parentId 前端传递的parentId
     * @return 标准化后的parentId，null表示顶级菜单
     */
    private String normalizeParentId(String parentId) {
        // 处理null、空字符串或"0"的情况，都表示顶级菜单
        if (parentId == null || parentId.trim().isEmpty() || "0".equals(parentId.trim())) {
            return "0";
        }
        return parentId.trim();
    }
}