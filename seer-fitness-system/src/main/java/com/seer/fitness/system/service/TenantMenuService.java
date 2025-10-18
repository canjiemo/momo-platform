package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.dto.MenuDTO;
import com.seer.fitness.system.dto.MenuTreeVO;
import com.seer.fitness.system.entity.SysMenu;
import com.seer.fitness.system.entity.SysUser;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 租户菜单服务（只读）
 * 负责租户侧菜单的查询操作
 * <p>
 * 功能：
 * 1. 查询菜单树（所有菜单、用户菜单）
 * 2. 查询用户权限列表
 * 3. 查询菜单详情
 * <p>
 * 注意：
 * - 租户不能创建、更新、删除菜单
 * - 菜单由平台分配后复制到租户schema
 * - 所有方法操作租户自己的 schema（通过多租户切换机制）
 *
 * @author seer-fitness
 * @since 2025-10-17
 */
@Service
@Slf4j
public class TenantMenuService extends BaseServiceImpl {

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
    public List<MenuTreeVO> getUserMenuTree(Long userId) {
        // 检查用户是否为超级管理员
        SysUser user = baseDao.queryByIdWithDeleteCondition(userId, SysUser.class);
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
    public List<MenuDTO> getUserMenus(Long userId) {
        // 检查是否为超级管理员
        SysUser user = baseDao.queryByIdWithDeleteCondition(userId, SysUser.class);
        if (user != null && user.getAdminFlag() != null && user.getAdminFlag() == 1) {
            String allMenusSql = "SELECT id, menu_name, path, parent_id, type, permission, icon, sort_order, status " +
                                "FROM sys_menu WHERE status = 1 AND type IN (0, 1) ORDER BY sort_order";
            return baseDao.queryListForSqlWithDeleteCondition(allMenusSql, Maps.newHashMap(), MenuDTO.class);
        }

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
     * 获取用户权限字符串列表
     * 用于前端按钮权限控制和后端接口权限校验
     * 超级管理员返回所有权限，普通用户根据角色获取权限
     *
     * @param userId 用户ID
     * @return 权限字符串列表
     */
    public List<String> getUserPermissions(Long userId) {
        // 首先检查用户是否为超级管理员
        String checkAdminSql = "SELECT admin_flag FROM sys_user WHERE id = :userId";
        Map<String, Object> adminParams = Maps.newHashMap();
        adminParams.put("userId", userId);

        Integer adminFlag = baseDao.querySingleForSqlWithDeleteCondition(checkAdminSql, adminParams, Integer.class);

        // 如果是超级管理员，返回所有权限
        if (adminFlag != null && adminFlag == 1) {
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
                    "status, platform_menu_id, created_at, updated_at " +
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
     * 转换为DTO
     */
    private MenuDTO convertToDTO(SysMenu menu) {
        MenuDTO dto = new MenuDTO();
        dto.setId(menu.getId());
        dto.setMenuName(menu.getMenuName());
        dto.setPath(menu.getPath());
        dto.setParentId(menu.getParentId() != null ? menu.getParentId() : 0L);
        dto.setType(menu.getType());
        dto.setMenuType(menu.getMenuType());
        dto.setPermission(menu.getPermission());
        dto.setIcon(menu.getIcon());
        dto.setSortOrder(menu.getSortOrder());
        dto.setFeatureLevel(menu.getFeatureLevel());
        dto.setStatus(menu.getStatus());
        dto.setCreatedAt(menu.getCreatedAt());
        dto.setUpdatedAt(menu.getUpdatedAt());
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
        vo.setParentId(menu.getParentId());
        vo.setType(menu.getType());
        vo.setPermission(menu.getPermission());
        vo.setIcon(menu.getIcon());
        vo.setSortOrder(menu.getSortOrder());
        vo.setStatus(menu.getStatus());
        return vo;
    }
}
