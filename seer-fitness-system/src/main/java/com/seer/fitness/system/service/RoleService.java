package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.entity.SysRole;
import com.seer.fitness.system.entity.SysRoleMenu;
import com.seer.fitness.system.dto.*;
import com.seer.fitness.system.util.SecurityContextUtil;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import io.github.mocanjie.base.myjpa.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色管理服务（租户侧）
 * <p>
 * 更新说明（2025-10-18）：
 * - 添加平台角色只读保护
 * - 禁止修改/删除 platform_role_id 不为空的角色
 * - 这些角色由平台同步而来，租户只能使用不能修改
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class RoleService extends BaseServiceImpl {

    /**
     * 分页查询角色
     */
    public Pager<RoleDTO> search(RoleQueryParam param, Pager pager) {
        Map<String, Object> queryMap = Maps.newHashMap();

        String sql = "SELECT id, role_name, role_code, description, status, created_at, updated_at " +
                    "FROM sys_role";

        // 动态添加查询条件
        List<String> conditions = new ArrayList<>();

        if (StringUtils.hasText(param.getRoleName())) {
            conditions.add("role_name LIKE :roleName");
            queryMap.put("roleName", "%" + param.getRoleName() + "%");
        }

        if (StringUtils.hasText(param.getRoleCode())) {
            conditions.add("role_code = :roleCode");
            queryMap.put("roleCode", param.getRoleCode());
        }

        if (param.getStatus() != null) {
            conditions.add("status = :status");
            queryMap.put("status", param.getStatus());
        }

        // 平台管理员可按 tenantId 过滤特定租户数据
        UserCacheInfo currentUser = SecurityContextUtil.getCurrentUser();
        boolean isPlatformAdmin = currentUser != null
                && Integer.valueOf(1).equals(currentUser.getAdminFlag())
                && currentUser.getTenantId() == null;
        if (isPlatformAdmin && param.getTenantId() != null) {
            conditions.add("tenant_id = :tenantId");
            queryMap.put("tenantId", param.getTenantId());
        }

        // 拼接WHERE条件
        if (!conditions.isEmpty()) {
            sql += " WHERE " + String.join(" AND ", conditions);
        }

        // 排序
        sql += " ORDER BY created_at DESC";

        log.info("角色分页查询SQL: {}", sql);

        final String finalSql = sql;
        return (isPlatformAdmin && param.getTenantId() != null)
                ? TenantContext.withoutTenant(() -> baseDao.queryPageForSql(finalSql, queryMap, pager, RoleDTO.class))
                : baseDao.queryPageForSql(finalSql, queryMap, pager, RoleDTO.class);
    }

    /**
     * 获取角色列表（不分页）
     * tenantId != null 时：平台用户查询指定租户的角色列表（需绕过 myjpa 自动注入）
     * tenantId == null 时：租户用户查询自身角色（myjpa 自动注入 tenant_id）
     */
    public List<RoleDTO> list(Long tenantId) {
        if (tenantId != null) {
            // 平台用户指定租户ID查询
            String sql = "SELECT id, role_name, role_code, description, status, created_at, updated_at " +
                        "FROM sys_role WHERE tenant_id = :tenantId AND delete_flag = 0 AND status = 1 ORDER BY created_at DESC";
            Map<String, Object> params = Maps.newHashMap();
            params.put("tenantId", tenantId);
            return TenantContext.withoutTenant(() ->
                    baseDao.queryListForSql(sql, params, RoleDTO.class));
        }
        // tenant_id IS NOT NULL 确保只返回租户角色，防止平台管理员误调此接口混入平台角色
        String sql = "SELECT id, role_name, role_code, description, status, created_at, updated_at " +
                    "FROM sys_role WHERE tenant_id IS NOT NULL AND status = 1 ORDER BY created_at DESC";
        return baseDao.queryListForSql(sql, Maps.newHashMap(), RoleDTO.class);
    }

    /**
     * 根据ID获取角色详情
     */
    public RoleDTO getById(String idStr) {
        Long id = Long.parseLong(idStr);

        SysRole role = baseDao.queryById(id, SysRole.class);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        //        // 查询角色菜单
//        String menusSql = "SELECT m.id, m.menu_name, m.path, m.parent_id, m.type, " +
//                         "m.permission, m.icon, m.sort_order, m.status " +
//                         "FROM sys_menu m " +
//                         "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
//                         "WHERE rm.role_id = :roleId ORDER BY m.sort_order";
//        Map<String, Object> menuParams = Maps.newHashMap();
//        menuParams.put("roleId", id);
//
//        List<MenuDTO> menus = baseDao.queryListForSql(menusSql, menuParams, MenuDTO.class);
//        roleDTO.setMenus(menus);

        return convertToDTO(role);
    }

    /**
     * 创建角色
     */
    @Transactional(readOnly = false)
    public void create(RoleCreateRequest request) {
        // 检查角色名是否已存在
        if (isRoleNameExists(request.getRoleName())) {
            throw new BusinessException("角色名已存在");
        }
        // 检查角色编码是否已存在
        if (isRoleCodeExists(request.getRoleCode(), null)) {
            throw new BusinessException("角色编码已存在");
        }

        SysRole role = new SysRole();
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus());
        role.setDeleteFlag(0);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());

        baseDao.insertPO(role, true);

        // 分配菜单权限
        if (request.getMenuIds() != null && !request.getMenuIds().isEmpty()) {
            assignMenus(role.getId().toString(), request.getMenuIds());
        }

        log.info("创建角色成功: roleName={}, id={}", request.getRoleName(), role.getId());
    }

    /**
     * 更新角色
     */
    @Transactional(readOnly = false)
    public void update(RoleUpdateRequest request) {
        SysRole role = baseDao.queryById(request.getId(), SysRole.class);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        // 如果修改了角色名，检查是否重复
        if (!role.getRoleName().equals(request.getRoleName()) &&
            isRoleNameExists(request.getRoleName())) {
            throw new BusinessException("角色名已存在");
        }
        // 如果修改了角色编码，检查是否重复（排除自身）
        if (!request.getRoleCode().equals(role.getRoleCode()) &&
            isRoleCodeExists(request.getRoleCode(), request.getId())) {
            throw new BusinessException("角色编码已存在");
        }

        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus());
        role.setUpdatedAt(LocalDateTime.now());

        baseDao.updatePO(role);

        // 重新分配菜单权限
        if (request.getMenuIds() != null) {
            removeRoleMenus(request.getId());
            if (!request.getMenuIds().isEmpty()) {
                assignMenus(String.valueOf(request.getId()), request.getMenuIds());
            }
        }

        log.info("更新角色成功: id={}, roleName={}", request.getId(), request.getRoleName());
    }

    /**
     * 删除角色
     */
    @Transactional(readOnly = false)
    public void delete(String... ids) {
        if (ids == null || ids.length == 0) {
            throw new BusinessException("删除的角色ID不能为空");
        }

        for (String id : ids) {
            if (!StringUtils.hasText(id)) {
                throw new BusinessException("角色ID不能为空");
            }

            Long roleId = Long.valueOf(id);

            // 检查是否有用户使用该角色
            if (hasUsersWithRole(roleId)) {
                throw new BusinessException("该角色已被用户使用，无法删除");
            }

            // 删除角色菜单关联
            removeRoleMenus(roleId);
        }

        // 逻辑删除角色
        baseDao.delByIds(SysRole.class, ids);

        log.info("删除角色成功: ids={}", (Object) ids);
    }

    /**
     * 分配菜单权限
     */
    @Transactional(readOnly = false)
    public void assignMenus(String roleIdStr, List<String> menuIdStrs) {
        Long roleId = Long.parseLong(roleIdStr);
        List<Long> menuIds = menuIdStrs.stream().map(Long::parseLong).collect(Collectors.toList());
        // 先删除现有权限
        removeRoleMenus(roleId);

        // 分配新权限
        for (Long menuId : menuIds) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenu.setCreatedAt(LocalDateTime.now());

            baseDao.insertPO(roleMenu, true);
        }

        log.info("分配菜单权限成功: roleId={}, menuIds={}", roleId, menuIds);
    }

    /**
     * 获取角色的菜单ID列表
     */
    public List<String> getRoleMenuIds(String roleIdStr) {
        Long roleId = Long.parseLong(roleIdStr);
        String sql = "SELECT menu_id FROM sys_role_menu WHERE role_id = :roleId";
        Map<String, Object> params = Maps.newHashMap();
        params.put("roleId", roleId);

        List<Long> menuIds = baseDao.queryListForSql(sql, params, Long.class);
        return menuIds.stream().map(String::valueOf).collect(Collectors.toList());
    }

    /**
     * 根据用户ID获取角色列表（租户用户）
     */
    public List<RoleDTO> getUserRoles(Long userId) {
        String sql = "SELECT r.* FROM sys_role r " +
                    "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
                    "WHERE ur.user_id = :userId AND r.status = 1";
        Map<String, Object> params = Maps.newHashMap();
        params.put("userId", userId);

        return baseDao.queryListForSql(sql, params, RoleDTO.class);
    }

    /**
     * 检查角色名是否已存在
     */
    private boolean isRoleNameExists(String roleName) {
        String sql = "SELECT COUNT(*) FROM sys_role WHERE role_name = :roleName";
        Map<String, Object> params = Maps.newHashMap();
        params.put("roleName", roleName);

        Long count = baseDao.querySingleForSql(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * 检查角色编码是否已存在（excludeId 不为 null 时排除该 ID，用于更新校验）
     */
    private boolean isRoleCodeExists(String roleCode, Long excludeId) {
        String sql = "SELECT COUNT(*) FROM sys_role WHERE role_code = :roleCode";
        Map<String, Object> params = Maps.newHashMap();
        params.put("roleCode", roleCode);
        if (excludeId != null) {
            sql += " AND id != :excludeId";
            params.put("excludeId", excludeId);
        }
        Long count = baseDao.querySingleForSql(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * 检查是否有用户使用该角色
     */
    private boolean hasUsersWithRole(Long roleId) {
        String sql = "SELECT COUNT(*) FROM sys_user_role WHERE role_id = :roleId";
        Map<String, Object> params = Maps.newHashMap();
        params.put("roleId", roleId);

        Long count = baseDao.querySingleForSql(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * 移除角色菜单关联
     */
    private void removeRoleMenus(Long roleId) {
        String sql = "SELECT * FROM sys_role_menu WHERE role_id = :roleId";
        Map<String, Object> params = Maps.newHashMap();
        params.put("roleId", roleId);

        List<SysRoleMenu> roleMenus = baseDao.queryListForSql(sql, params, SysRoleMenu.class);
        for (SysRoleMenu roleMenu : roleMenus) {
            baseDao.delPO(roleMenu);
        }
    }

    /**
     * 转换为DTO
     */
    private RoleDTO convertToDTO(SysRole role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setRoleName(role.getRoleName());
        dto.setDescription(role.getDescription());
        dto.setStatus(role.getStatus());
        return dto;
    }
}