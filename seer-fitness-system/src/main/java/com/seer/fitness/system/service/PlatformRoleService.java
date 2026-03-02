package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.dto.RoleCreateRequest;
import com.seer.fitness.system.dto.RoleDTO;
import com.seer.fitness.system.dto.RoleQueryParam;
import com.seer.fitness.system.dto.RoleUpdateRequest;
import com.seer.fitness.system.entity.SysRole;
import com.seer.fitness.system.entity.SysRoleMenu;
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
 * 平台角色管理服务（只操作 tenant_id=NULL 的角色）
 * 平台管理员通过此服务管理可分配给租户的平台角色模板
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class PlatformRoleService extends BaseServiceImpl implements IPlatformRoleService {

    @Override
    public Pager<RoleDTO> search(RoleQueryParam param, Pager pager) {
        Map<String, Object> queryMap = Maps.newHashMap();
        List<String> conditions = new ArrayList<>();
        conditions.add("tenant_id IS NULL");

        String sql = "SELECT id, role_name, role_code, description, status, create_time, update_time FROM sys_role";

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

        final String finalSql = sql + " WHERE " + String.join(" AND ", conditions) + " ORDER BY create_time DESC";

        return TenantContext.withoutTenant(() ->
                baseDao.queryPageForSql(finalSql, queryMap, pager, RoleDTO.class));
    }

    @Override
    public List<RoleDTO> list() {
        String sql = "SELECT id, role_name, role_code, description, status, create_time, update_time " +
                "FROM sys_role WHERE tenant_id IS NULL AND status = 1 ORDER BY create_time DESC";
        return TenantContext.withoutTenant(() ->
                baseDao.queryListForSql(sql, Maps.newHashMap(), RoleDTO.class));
    }

    @Override
    public RoleDTO getById(Long id) {
        String sql = "SELECT id, role_name, role_code, description, status, create_time, update_time " +
                "FROM sys_role WHERE id = :id AND tenant_id IS NULL";
        Map<String, Object> params = Maps.newHashMap();
        params.put("id", id);
        SysRole role = TenantContext.withoutTenant(() ->
                baseDao.querySingleForSql(sql, params, SysRole.class));
        if (role == null) throw new BusinessException("平台角色不存在");
        return convertToDTO(role);
    }

    @Override
    @Transactional
    public void create(RoleCreateRequest request) {
        if (isRoleNameExists(request.getRoleName())) throw new BusinessException("角色名称已存在");
        if (isRoleCodeExists(request.getRoleCode(), null)) throw new BusinessException("角色编码已存在");

        SysRole role = new SysRole();
        role.setTenantId(null);
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus());
        role.setDeleteFlag(0);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());

        TenantContext.withoutTenant(() -> {
            baseDao.insertPO(role, true);
            return null;
        });

        if (request.getMenuIds() != null && !request.getMenuIds().isEmpty()) {
            assignMenus(role.getId(), request.getMenuIds());
        }

        log.info("创建平台角色成功: roleName={}, id={}", request.getRoleName(), role.getId());
    }

    @Override
    @Transactional
    public void update(RoleUpdateRequest request) {
        SysRole role = getPlatformRoleEntity(request.getId());

        if (!role.getRoleName().equals(request.getRoleName()) && isRoleNameExists(request.getRoleName())) {
            throw new BusinessException("角色名称已存在");
        }
        if (!role.getRoleCode().equals(request.getRoleCode()) && isRoleCodeExists(request.getRoleCode(), request.getId())) {
            throw new BusinessException("角色编码已存在");
        }

        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus());
        role.setUpdatedAt(LocalDateTime.now());

        TenantContext.withoutTenant(() -> {
            baseDao.updatePO(role);
            return null;
        });

        if (request.getMenuIds() != null) {
            removeRoleMenus(request.getId());
            if (!request.getMenuIds().isEmpty()) {
                assignMenus(request.getId(), request.getMenuIds());
            }
        }

        log.info("更新平台角色成功: id={}", request.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getPlatformRoleEntity(id); // 验证存在且是平台角色

        // 检查是否有租户正在使用该角色
        String checkSql = "SELECT COUNT(*) FROM sys_tenant_role WHERE role_id = :roleId";
        Map<String, Object> checkParams = Maps.newHashMap();
        checkParams.put("roleId", id);
        Long count = baseDao.querySingleForSql(checkSql, checkParams, Long.class);
        if (count != null && count > 0) {
            throw new BusinessException("该角色已被租户使用，无法删除");
        }

        removeRoleMenus(id);
        baseDao.delByIds(SysRole.class, String.valueOf(id));

        log.info("删除平台角色成功: id={}", id);
    }

    @Override
    public List<String> getRoleMenuIds(Long roleId) {
        getPlatformRoleEntity(roleId); // 验证存在且是平台角色
        String sql = "SELECT menu_id FROM sys_role_menu WHERE role_id = :roleId AND tenant_id IS NULL";
        Map<String, Object> params = Maps.newHashMap();
        params.put("roleId", roleId);
        List<Long> menuIds = TenantContext.withoutTenant(() ->
                baseDao.queryListForSql(sql, params, Long.class));
        return menuIds.stream().map(String::valueOf).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<String> menuIds) {
        getPlatformRoleEntity(roleId);
        removeRoleMenus(roleId);

        for (String menuIdStr : menuIds) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setTenantId(null);
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(Long.parseLong(menuIdStr));
            roleMenu.setCreatedAt(LocalDateTime.now());
            TenantContext.withoutTenant(() -> {
                baseDao.insertPO(roleMenu, true);
                return null;
            });
        }

        log.info("分配平台角色菜单成功: roleId={}, menuCount={}", roleId, menuIds.size());
    }

    private SysRole getPlatformRoleEntity(Long id) {
        String sql = "SELECT * FROM sys_role WHERE id = :id AND tenant_id IS NULL";
        Map<String, Object> params = Maps.newHashMap();
        params.put("id", id);
        SysRole role = TenantContext.withoutTenant(() ->
                baseDao.querySingleForSql(sql, params, SysRole.class));
        if (role == null) throw new BusinessException("平台角色不存在");
        return role;
    }

    private void removeRoleMenus(Long roleId) {
        String sql = "SELECT * FROM sys_role_menu WHERE role_id = :roleId AND tenant_id IS NULL";
        Map<String, Object> params = Maps.newHashMap();
        params.put("roleId", roleId);
        List<SysRoleMenu> roleMenus = TenantContext.withoutTenant(() ->
                baseDao.queryListForSql(sql, params, SysRoleMenu.class));
        for (SysRoleMenu rm : roleMenus) {
            baseDao.delPO(rm);
        }
    }

    private boolean isRoleNameExists(String roleName) {
        String sql = "SELECT COUNT(*) FROM sys_role WHERE role_name = :roleName AND tenant_id IS NULL";
        Map<String, Object> params = Maps.newHashMap();
        params.put("roleName", roleName);
        Long count = TenantContext.withoutTenant(() ->
                baseDao.querySingleForSql(sql, params, Long.class));
        return count != null && count > 0;
    }

    private boolean isRoleCodeExists(String roleCode, Long excludeId) {
        String sql = "SELECT COUNT(*) FROM sys_role WHERE role_code = :roleCode AND tenant_id IS NULL";
        Map<String, Object> params = Maps.newHashMap();
        params.put("roleCode", roleCode);
        if (excludeId != null) {
            sql += " AND id != :excludeId";
            params.put("excludeId", excludeId);
        }
        final String finalSql = sql;
        Long count = TenantContext.withoutTenant(() ->
                baseDao.querySingleForSql(finalSql, params, Long.class));
        return count != null && count > 0;
    }

    private RoleDTO convertToDTO(SysRole role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setRoleName(role.getRoleName());
        dto.setRoleCode(role.getRoleCode());
        dto.setDescription(role.getDescription());
        dto.setStatus(role.getStatus());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());
        return dto;
    }
}
