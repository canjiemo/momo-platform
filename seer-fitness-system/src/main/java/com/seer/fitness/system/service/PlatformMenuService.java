package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.framework.annotation.PublicSchema;
import com.seer.fitness.system.dto.MenuCreateRequest;
import com.seer.fitness.system.dto.MenuDTO;
import com.seer.fitness.system.dto.MenuTreeVO;
import com.seer.fitness.system.dto.MenuUpdateRequest;
import com.seer.fitness.system.entity.SysMenu;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 平台菜单管理服务
 * 负责管理 public.sys_menu 表中的菜单数据
 * <p>
 * 功能：
 * 1. CRUD操作：创建、更新、删除、查询平台菜单
 * 2. 同步功能：将平台菜单更新同步到所有已分配该菜单的租户
 * 3. 菜单分类：区分平台菜单(menu_type=1)和租户模板菜单(menu_type=2)
 * <p>
 * 注意：
 * - 所有方法使用 @PublicSchema 注解确保操作 public.sys_menu
 * - 平台菜单(menu_type=1)不可分配给租户
 * - 租户模板菜单(menu_type=2)可分配给租户
 *
 * @author seer-fitness
 * @since 2025-10-17
 */
@Service
@Slf4j
@PublicSchema(reason = "平台菜单管理")
public class PlatformMenuService extends BaseServiceImpl {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * 获取完整的平台菜单树（包含平台菜单和租户模板菜单）
     *
     * @return 菜单树形结构列表
     */
    public List<MenuTreeVO> getMenuTree() {
        String sql = "SELECT id, menu_name, path, parent_id, type, menu_type, permission, icon, " +
                    "sort_order, feature_level, status " +
                    "FROM sys_menu " +
                    "WHERE status = 1 AND delete_flag = 0 " +
                    "ORDER BY sort_order";

        List<MenuDTO> allMenus = baseDao.queryListForSql(sql, Maps.newHashMap(), MenuDTO.class);

        return buildMenuTree(allMenus, null);
    }

    /**
     * 获取租户模板菜单树（仅返回可分配给租户的菜单）
     *
     * @return 租户模板菜单树
     */
    public List<MenuTreeVO> getTenantTemplateMenuTree() {
        String sql = "SELECT id, menu_name, path, parent_id, type, menu_type, permission, icon, " +
                    "sort_order, feature_level, status " +
                    "FROM sys_menu " +
                    "WHERE status = 1 AND delete_flag = 0 AND menu_type = 2 " +
                    "ORDER BY sort_order";

        List<MenuDTO> templateMenus = baseDao.queryListForSql(sql, Maps.newHashMap(), MenuDTO.class);

        return buildMenuTree(templateMenus, null);
    }

    /**
     * 获取平台专用菜单树（仅返回平台管理功能菜单）
     *
     * @return 平台菜单树
     */
    public List<MenuTreeVO> getPlatformMenuTree() {
        String sql = "SELECT id, menu_name, path, parent_id, type, menu_type, permission, icon, " +
                    "sort_order, feature_level, status " +
                    "FROM sys_menu " +
                    "WHERE status = 1 AND delete_flag = 0 AND menu_type = 1 " +
                    "ORDER BY sort_order";

        List<MenuDTO> platformMenus = baseDao.queryListForSql(sql, Maps.newHashMap(), MenuDTO.class);

        return buildMenuTree(platformMenus, null);
    }

    /**
     * 获取所有菜单列表（不分页）
     *
     * @return 菜单列表
     */
    public List<MenuDTO> list() {
        String sql = "SELECT id, menu_name, path, parent_id, type, menu_type, permission, icon, sort_order, " +
                    "feature_level, status, created_at, updated_at " +
                    "FROM sys_menu " +
                    "WHERE delete_flag = 0 " +
                    "ORDER BY sort_order";

        return baseDao.queryListForSql(sql, Maps.newHashMap(), MenuDTO.class);
    }

    /**
     * 根据菜单类型获取菜单列表
     *
     * @param menuType 菜单类型：1-平台菜单 2-租户模板菜单
     * @return 菜单列表
     */
    public List<MenuDTO> listByMenuType(Integer menuType) {
        String sql = "SELECT id, menu_name, path, parent_id, type, menu_type, permission, icon, sort_order, " +
                    "feature_level, status, created_at, updated_at " +
                    "FROM sys_menu " +
                    "WHERE delete_flag = 0 AND menu_type = :menuType " +
                    "ORDER BY sort_order";

        Map<String, Object> params = Maps.newHashMap();
        params.put("menuType", menuType);

        return baseDao.queryListForSql(sql, params, MenuDTO.class);
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

        String sql = "SELECT id, menu_name, path, parent_id, type, menu_type, permission, icon, sort_order, " +
                    "feature_level, status, created_by, created_at, updated_by, updated_at " +
                    "FROM sys_menu " +
                    "WHERE id = :id AND delete_flag = 0";

        Map<String, Object> params = Maps.newHashMap();
        params.put("id", id);

        SysMenu menu = baseDao.querySingleForSql(sql, params, SysMenu.class);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }

        return convertToDTO(menu);
    }

    /**
     * 创建平台菜单
     *
     * @param request 创建请求参数
     * @param currentUserId 当前用户ID
     * @throws BusinessException 当父菜单不存在或其他业务错误时抛出
     */
    @Transactional(readOnly = false)
    public void create(MenuCreateRequest request, Long currentUserId) {
        // 处理parentId，null或0表示顶级菜单
        Long parentId = request.getParentId() != null ? request.getParentId() : 0L;

        // 验证父菜单是否存在（如果有设置）
        if (parentId != 0L) {
            String checkSql = "SELECT COUNT(*) FROM sys_menu WHERE id = :id AND delete_flag = 0";
            Map<String, Object> params = Maps.newHashMap();
            params.put("id", parentId);
            Long count = baseDao.querySingleForSql(checkSql, params, Long.class);
            if (count == null || count == 0) {
                throw new BusinessException("父菜单不存在");
            }
        }

        // 验证 menuType 必须设置
        if (request.getMenuType() == null) {
            throw new BusinessException("菜单类型不能为空");
        }

        SysMenu menu = new SysMenu();
        menu.setMenuName(request.getMenuName());
        menu.setPath(request.getPath());
        menu.setParentId(parentId);
        menu.setType(request.getType());
        menu.setMenuType(request.getMenuType());
        menu.setPermission(request.getPermission());
        menu.setIcon(request.getIcon());
        menu.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        menu.setFeatureLevel(request.getFeatureLevel() != null ? request.getFeatureLevel() : 1);
        menu.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        menu.setDeleteFlag(0);
        menu.setCreatedBy(currentUserId);
        menu.setCreatedAt(LocalDateTime.now());
        menu.setUpdatedBy(currentUserId);
        menu.setUpdatedAt(LocalDateTime.now());

        baseDao.insertPO(menu, true);

        log.info("创建平台菜单成功: menuName={}, id={}, menuType={}",
                request.getMenuName(), menu.getId(), request.getMenuType());
    }

    /**
     * 更新平台菜单
     *
     * @param request 更新请求参数
     * @param currentUserId 当前用户ID
     * @throws BusinessException 当菜单不存在、父菜单不存在或其他业务错误时抛出
     */
    @Transactional(readOnly = false)
    public void update(MenuUpdateRequest request, Long currentUserId) {
        if (request.getId() == null) {
            throw new BusinessException("菜单ID不能为空");
        }

        String sql = "SELECT * FROM sys_menu WHERE id = :id AND delete_flag = 0";
        Map<String, Object> params = Maps.newHashMap();
        params.put("id", request.getId());

        SysMenu menu = baseDao.querySingleForSql(sql, params, SysMenu.class);
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

            String checkSql = "SELECT COUNT(*) FROM sys_menu WHERE id = :id AND delete_flag = 0";
            Map<String, Object> checkParams = Maps.newHashMap();
            checkParams.put("id", parentId);
            Long count = baseDao.querySingleForSql(checkSql, checkParams, Long.class);
            if (count == null || count == 0) {
                throw new BusinessException("父菜单不存在");
            }
        }

        // 更新菜单信息
        menu.setMenuName(request.getMenuName());
        menu.setPath(request.getPath());
        menu.setParentId(parentId);
        menu.setType(request.getType());
        if (request.getMenuType() != null) {
            menu.setMenuType(request.getMenuType());
        }
        menu.setPermission(request.getPermission());
        menu.setIcon(request.getIcon());
        menu.setSortOrder(request.getSortOrder());
        if (request.getFeatureLevel() != null) {
            menu.setFeatureLevel(request.getFeatureLevel());
        }
        menu.setStatus(request.getStatus());
        menu.setUpdatedBy(currentUserId);
        menu.setUpdatedAt(LocalDateTime.now());

        baseDao.updatePO(menu);

        log.info("更新平台菜单成功: id={}, menuName={}", request.getId(), request.getMenuName());

        // 如果是租户模板菜单(menuType=2)，同步更新到已分配的租户
        if (menu.getMenuType() != null && menu.getMenuType() == 2) {
            syncMenuToTenants(menu);
        }
    }

    /**
     * 删除平台菜单
     * 会检查子菜单和租户分配关系
     *
     * @param id 菜单ID
     * @param currentUserId 当前用户ID
     * @throws BusinessException 当菜单不存在、存在子菜单或已分配给租户时抛出
     */
    @Transactional(readOnly = false)
    public void delete(Long id, Long currentUserId) {
        if (id == null) {
            throw new BusinessException("菜单ID不能为空");
        }

        String sql = "SELECT * FROM sys_menu WHERE id = :id AND delete_flag = 0";
        Map<String, Object> params = Maps.newHashMap();
        params.put("id", id);

        SysMenu menu = baseDao.querySingleForSql(sql, params, SysMenu.class);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }

        // 检查是否有子菜单
        if (hasChildren(id)) {
            throw new BusinessException("该菜单存在子菜单，无法删除");
        }

        // 如果是租户模板菜单，检查是否已分配给租户
        if (menu.getMenuType() != null && menu.getMenuType() == 2) {
            if (hasAssignedToTenants(id)) {
                throw new BusinessException("该菜单已分配给租户，无法删除。请先取消租户分配。");
            }
        }

        // 逻辑删除
        menu.setDeleteFlag(1);
        menu.setUpdatedBy(currentUserId);
        menu.setUpdatedAt(LocalDateTime.now());

        baseDao.updatePO(menu);

        log.info("删除平台菜单成功: id={}, menuName={}", id, menu.getMenuName());
    }

    /**
     * 同步菜单更新到所有已分配该菜单的租户
     * 仅适用于租户模板菜单(menuType=2)
     *
     * @param platformMenu 平台菜单对象
     */
    @Transactional(readOnly = false)
    public void syncMenuToTenants(SysMenu platformMenu) {
        if (platformMenu.getMenuType() == null || platformMenu.getMenuType() != 2) {
            log.warn("仅租户模板菜单可以同步到租户，menuId={}, menuType={}",
                    platformMenu.getId(), platformMenu.getMenuType());
            return;
        }

        // 查询所有分配了该菜单的租户
        String sql = "SELECT DISTINCT t.schema_name " +
                    "FROM sys_tenant t " +
                    "INNER JOIN sys_tenant_menu tm ON t.id = tm.tenant_id " +
                    "WHERE tm.platform_menu_id = :platformMenuId AND t.status = 1 AND t.delete_flag = 0";

        Map<String, Object> params = Maps.newHashMap();
        params.put("platformMenuId", platformMenu.getId());

        List<String> schemaNames = baseDao.queryListForSql(sql, params, String.class);

        if (schemaNames.isEmpty()) {
            log.info("菜单未分配给任何租户，无需同步: menuId={}", platformMenu.getId());
            return;
        }

        log.info("开始同步菜单到 {} 个租户: menuId={}", schemaNames.size(), platformMenu.getId());

        // 为每个租户更新菜单
        for (String schemaName : schemaNames) {
            try {
                syncMenuToTenant(platformMenu, schemaName);
            } catch (Exception e) {
                log.error("同步菜单到租户失败: schemaName={}, menuId={}",
                        schemaName, platformMenu.getId(), e);
                // 继续同步其他租户，不中断流程
            }
        }

        log.info("菜单同步完成: menuId={}, 成功同步租户数={}", platformMenu.getId(), schemaNames.size());
    }

    /**
     * 同步菜单到单个租户
     *
     * @param platformMenu 平台菜单对象
     * @param schemaName 租户schema名称
     */
    private void syncMenuToTenant(SysMenu platformMenu, String schemaName) {
        // 切换到租户schema
        setSearchPath(schemaName);

        try {
            // 查找租户schema中对应的菜单（通过platform_menu_id）
            String findSql = "SELECT id FROM sys_menu WHERE platform_menu_id = :platformMenuId AND delete_flag = 0";
            Map<String, Object> findParams = Maps.newHashMap();
            findParams.put("platformMenuId", platformMenu.getId());

            Long tenantMenuId = baseDao.querySingleForSql(findSql, findParams, Long.class);

            if (tenantMenuId == null) {
                log.warn("租户schema中未找到对应菜单，跳过同步: schema={}, platformMenuId={}",
                        schemaName, platformMenu.getId());
                return;
            }

            // 更新租户菜单
            String updateSql = "UPDATE sys_menu SET " +
                    "menu_name = :menuName, " +
                    "path = :path, " +
                    "parent_id = :parentId, " +
                    "type = :type, " +
                    "permission = :permission, " +
                    "icon = :icon, " +
                    "sort_order = :sortOrder, " +
                    "updated_at = :updatedAt " +
                    "WHERE id = :id";

            Map<String, Object> updateParams = Maps.newHashMap();
            updateParams.put("menuName", platformMenu.getMenuName());
            updateParams.put("path", platformMenu.getPath());
            updateParams.put("parentId", platformMenu.getParentId());
            updateParams.put("type", platformMenu.getType());
            updateParams.put("permission", platformMenu.getPermission());
            updateParams.put("icon", platformMenu.getIcon());
            updateParams.put("sortOrder", platformMenu.getSortOrder());
            updateParams.put("updatedAt", LocalDateTime.now());
            updateParams.put("id", tenantMenuId);

            jdbcTemplate.update(updateSql, updateParams);

            log.info("同步菜单到租户成功: schema={}, platformMenuId={}, tenantMenuId={}",
                    schemaName, platformMenu.getId(), tenantMenuId);

        } finally {
            // 切换回public schema
            resetSearchPath();
        }
    }

    /**
     * 切换到指定Schema
     */
    private void setSearchPath(String schemaName) {
        String sql = "SET search_path TO " + schemaName;
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    /**
     * 切换回public schema
     */
    private void resetSearchPath() {
        String sql = "SET search_path TO public";
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    /**
     * 检查是否有子菜单
     */
    private boolean hasChildren(Long menuId) {
        String sql = "SELECT COUNT(*) FROM sys_menu WHERE parent_id = :parentId AND delete_flag = 0";
        Map<String, Object> params = Maps.newHashMap();
        params.put("parentId", menuId);

        Long count = baseDao.querySingleForSql(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * 检查菜单是否已分配给租户
     */
    private boolean hasAssignedToTenants(Long menuId) {
        String sql = "SELECT COUNT(*) FROM sys_tenant_menu WHERE platform_menu_id = :menuId";
        Map<String, Object> params = Maps.newHashMap();
        params.put("menuId", menuId);

        Long count = baseDao.querySingleForSql(sql, params, Long.class);
        return count != null && count > 0;
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
