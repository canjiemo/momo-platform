package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.entity.SysMenu;
import com.seer.fitness.system.entity.SysRoleMenu;
import com.seer.fitness.system.entity.SysUser;
import com.seer.fitness.system.dto.MenuCreateRequest;
import com.seer.fitness.system.dto.MenuDTO;
import com.seer.fitness.system.dto.MenuTreeVO;
import com.seer.fitness.system.dto.MenuUpdateRequest;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单管理服务实现
 * 提供菜单的增删改查、树形结构管理、权限控制功能
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class MenuService extends BaseServiceImpl {

    @Autowired
    private RoleService roleService;

    /**
     * 获取完整的菜单树形结构
     * 返回所有启用状态的菜单，按排序字段排序
     *
     * @return 菜单树形结构列表
     */
    public List<MenuTreeVO> getMenuTree() {
        String sql = "SELECT id, menu_name, path, parent_id, type, permission, icon, sort_order, status " +
                    "FROM sys_menu WHERE status = 1 ORDER BY sort_order";

        List<MenuDTO> allMenus = baseDao.queryListForSqlWithDeleteCondition(sql, Maps.newHashMap(), MenuDTO.class);

        return buildMenuTree(allMenus, null);
    }

    /**
     * 获取用户菜单树
     * 根据用户角色权限返回用户可访问的菜单树（目录+菜单）
     * 超级管理员(admin_flag=1)返回所有菜单
     *
     * @param userId 用户ID
     * @return 用户菜单树形结构
     */
    public List<MenuTreeVO> getUserMenuTree(String userId) {
        // 检查用户是否为超级管理员
        SysUser user = baseDao.queryByIdWithDeleteCondition(Long.parseLong(userId), SysUser.class);
        if (user != null && user.getAdminFlag() != null && user.getAdminFlag() == 1) {
            // 超级管理员返回所有启用的菜单（目录+菜单，不含按钮）
            String allMenusSql = "SELECT id, menu_name, path, parent_id, type, permission, icon, sort_order, status " +
                                "FROM sys_menu WHERE status = 1 AND type IN (0, 1) ORDER BY sort_order";
            List<MenuDTO> allMenus = baseDao.queryListForSqlWithDeleteCondition(allMenusSql, Maps.newHashMap(), MenuDTO.class);
            return buildMenuTree(allMenus, null);
        }

        // 普通用户根据角色权限返回菜单
        String sql = "SELECT DISTINCT m.id, m.menu_name, m.path, m.parent_id, m.type, " +
                    "m.permission, m.icon, m.sort_order, m.status " +
                    "FROM sys_menu m " +
                    "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
                    "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
                    "WHERE ur.user_id = :userId AND m.status = 1 AND m.type IN (0, 1) " +
                    "ORDER BY m.sort_order";

        Map<String, Object> params = Maps.newHashMap();
        params.put("userId", userId);

        List<MenuDTO> userMenus = baseDao.queryListForSqlWithDeleteCondition(sql, params, MenuDTO.class);

        return buildMenuTree(userMenus, null);
    }

    /**
     * 获取用户扁平菜单列表
     * 返回扁平的菜单列表（目录+菜单，不含按钮），用于导航渲染
     *
     * @param userId 用户ID
     * @return 扁平的菜单列表
     */
    public List<MenuDTO> getUserMenus(String userId) {
        String sql = "SELECT DISTINCT m.id, m.menu_name, m.path, m.parent_id, m.type, " +
                    "m.permission, m.icon, m.sort_order, m.status " +
                    "FROM sys_menu m " +
                    "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
                    "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
                    "WHERE ur.user_id = :userId AND m.status = 1 AND m.type IN (0, 1) " +
                    "ORDER BY m.sort_order";

        Map<String, Object> params = Maps.newHashMap();
        params.put("userId", userId);

        return baseDao.queryListForSqlWithDeleteCondition(sql, params, MenuDTO.class);
    }

    /**
     * 获取用户权限字符串列表（租户用户）
     * 用于前端按钮权限控制和后端接口权限校验
     * 管理员返回所有权限，普通用户根据角色获取权限
     *
     * @param userId 用户ID
     * @return 权限字符串列表
     */
    public List<String> getUserPermissions(Long userId) {
        // 首先检查用户是否为管理员
        String checkAdminSql = "SELECT admin_flag FROM sys_user WHERE id = :userId";
        Map<String, Object> adminParams = Maps.newHashMap();
        adminParams.put("userId", userId);

        Boolean isAdmin = baseDao.querySingleForSqlWithDeleteCondition(checkAdminSql, adminParams, Boolean.class);

        // 如果是管理员，返回所有权限
        if (isAdmin != null && isAdmin) {
            String allPermissionsSql = "SELECT DISTINCT permission " +
                    "FROM sys_menu " +
                    "WHERE status = 1 AND permission IS NOT NULL";
            return baseDao.queryListForSqlWithDeleteCondition(allPermissionsSql, Maps.newHashMap(), String.class)
                    .stream()
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
        }

        // 普通用户通过角色获取权限
        String sql = "SELECT DISTINCT m.permission " +
                    "FROM sys_menu m " +
                    "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
                    "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
                    "WHERE ur.user_id = :userId AND m.status = 1 AND m.permission IS NOT NULL";

        Map<String, Object> params = Maps.newHashMap();
        params.put("userId", userId);

        return baseDao.queryListForSqlWithDeleteCondition(sql, params, String.class)
                .stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    /**
     * 获取平台管理员权限字符串列表
     * 使用 @PublicSchema 注解确保查询 public.sys_menu
     * 用于前端按钮权限控制和后端接口权限校验
     * 管理员返回所有权限，普通用户根据角色获取权限
     *
     * @param userId 用户ID
     * @return 权限字符串列表
     */
    @com.seer.fitness.framework.annotation.PublicSchema(reason = "平台管理员权限查询")
    public List<String> getPlatformUserPermissions(Long userId) {
        // 首先检查用户是否为管理员
        String checkAdminSql = "SELECT admin_flag FROM sys_user WHERE id = :userId";
        Map<String, Object> adminParams = Maps.newHashMap();
        adminParams.put("userId", userId);

        Boolean isAdmin = baseDao.querySingleForSqlWithDeleteCondition(checkAdminSql, adminParams, Boolean.class);

        // 如果是管理员，返回所有权限
        if (isAdmin != null && isAdmin) {
            String allPermissionsSql = "SELECT DISTINCT permission " +
                    "FROM sys_menu " +
                    "WHERE status = 1 AND permission IS NOT NULL";
            return baseDao.queryListForSqlWithDeleteCondition(allPermissionsSql, Maps.newHashMap(), String.class)
                    .stream()
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
        }

        // 普通用户通过角色获取权限
        String sql = "SELECT DISTINCT m.permission " +
                    "FROM sys_menu m " +
                    "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
                    "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
                    "WHERE ur.user_id = :userId AND m.status = 1 AND m.permission IS NOT NULL";

        Map<String, Object> params = Maps.newHashMap();
        params.put("userId", userId);

        return baseDao.queryListForSqlWithDeleteCondition(sql, params, String.class)
                .stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有菜单列表（不分页）
     * 用于管理界面的下拉选择框等场景
     *
     * @return 菜单列表
     */
    public List<MenuDTO> list() {
        String sql = "SELECT id, menu_name, path, parent_id, type, permission, icon, sort_order, " +
                    "status, created_at, updated_at " +
                    "FROM sys_menu ORDER BY sort_order";

        return baseDao.queryListForSqlWithDeleteCondition(sql, Maps.newHashMap(), MenuDTO.class);
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

        SysMenu menu = baseDao.queryByIdWithDeleteCondition(id, SysMenu.class);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }

        return convertToDTO(menu);
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
            SysMenu parentMenu = baseDao.queryByIdWithDeleteCondition(parentId, SysMenu.class);
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
        menu.setCreatedAt(LocalDateTime.now());
        menu.setUpdatedAt(LocalDateTime.now());

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
        SysMenu menu = baseDao.queryByIdWithDeleteCondition(request.getId(), SysMenu.class);
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

            SysMenu parentMenu = baseDao.queryByIdWithDeleteCondition(parentId, SysMenu.class);
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
        menu.setUpdatedAt(LocalDateTime.now());

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
                    MenuTreeVO treeNode = convertToTreeVO(menu);
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

        Long count = baseDao.querySingleForSqlWithDeleteCondition(sql, params, Long.class);
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
     * 转换为DTO
     */
    private MenuDTO convertToDTO(SysMenu menu) {
        MenuDTO dto = new MenuDTO();
        dto.setId(menu.getId());
        dto.setMenuName(menu.getMenuName());
        dto.setPath(menu.getPath());
        dto.setParentId(menu.getParentId() != null ? menu.getParentId() : 0L);
        dto.setType(menu.getType());
        dto.setPermission(menu.getPermission());
        dto.setIcon(menu.getIcon());
        dto.setSortOrder(menu.getSortOrder());
        dto.setStatus(menu.getStatus());
        return dto;
    }

    /**
     * 转换为TreeVO
     */
    private MenuTreeVO convertToTreeVO(MenuDTO menu) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setId(menu.getId());
        vo.setMenuName(menu.getMenuName());
        vo.setPath(menu.getPath());
        vo.setParentId(menu.getParentId()); // MenuDTO中已经处理过了，直接使用
        vo.setType(menu.getType());
        vo.setPermission(menu.getPermission());
        vo.setIcon(menu.getIcon());
        vo.setSortOrder(menu.getSortOrder());
        vo.setStatus(menu.getStatus());
        return vo;
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