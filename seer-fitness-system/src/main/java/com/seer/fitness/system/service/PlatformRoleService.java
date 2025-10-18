package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.framework.annotation.PublicSchema;
import com.seer.fitness.system.dto.RoleCreateRequest;
import com.seer.fitness.system.dto.RoleDTO;
import com.seer.fitness.system.dto.RoleQueryParam;
import com.seer.fitness.system.dto.RoleUpdateRequest;
import com.seer.fitness.system.entity.SysRole;
import com.seer.fitness.system.entity.SysRoleMenu;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 平台角色管理服务
 * 负责管理 public.sys_role 表中的角色数据
 * <p>
 * 功能：
 * 1. CRUD操作：创建、更新、删除、查询平台角色
 * 2. 权限配置：配置角色-菜单关联关系
 * 3. 同步功能：将平台角色更新同步到所有已分配该角色的租户
 * 4. 角色分类：区分平台角色(role_type=1)和租户模板角色(role_type=2)
 * <p>
 * 注意：
 * - 所有方法使用 @PublicSchema 注解确保操作 public.sys_role
 * - 平台角色(role_type=1)不可分配给租户
 * - 租户模板角色(role_type=2)可同步给租户
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Service
@Slf4j
@PublicSchema(reason = "平台角色管理")
public class PlatformRoleService extends BaseServiceImpl {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * 分页查询平台角色
     *
     * @param param 查询参数
     * @param pager 分页参数
     * @return 分页结果
     */
    public Pager<RoleDTO> search(RoleQueryParam param, Pager pager) {
        Map<String, Object> queryMap = Maps.newHashMap();

        // 基础SQL
        String sql = "SELECT id, role_name, description, role_type, feature_level, status, " +
                    "created_by, created_at, updated_by, updated_at " +
                    "FROM sys_role";

        // 动态添加查询条件
        List<String> conditions = new ArrayList<>();

        if (StringUtils.hasText(param.getRoleName())) {
            conditions.add("role_name LIKE :roleName");
            queryMap.put("roleName", "%" + param.getRoleName() + "%");
        }

        if (param.getStatus() != null) {
            conditions.add("status = :status");
            queryMap.put("status", param.getStatus());
        }

        // 拼接WHERE条件
        if (!conditions.isEmpty()) {
            sql += " WHERE " + String.join(" AND ", conditions);
        }

        // 排序
        sql += " ORDER BY id";

        log.info("平台角色分页查询SQL: {}", sql);

        return baseDao.queryPageForSqlWithDeleteCondition(sql, queryMap, pager, RoleDTO.class);
    }

    /**
     * 获取所有平台角色列表（包含平台角色和租户模板角色）
     *
     * @return 角色列表
     */
    public List<RoleDTO> list() {
        String sql = "SELECT id, role_name, description, role_type, feature_level, status, " +
                    "created_by, created_at, updated_by, updated_at " +
                    "FROM sys_role " +
                    "WHERE delete_flag = 0 " +
                    "ORDER BY id";

        return baseDao.queryListForSql(sql, Maps.newHashMap(), RoleDTO.class);
    }

    /**
     * 根据角色类型获取角色列表
     *
     * @param roleType 角色类型：1-平台角色 2-租户模板角色
     * @return 角色列表
     */
    public List<RoleDTO> listByRoleType(Integer roleType) {
        String sql = "SELECT id, role_name, description, role_type, feature_level, status, " +
                    "created_by, created_at, updated_by, updated_at " +
                    "FROM sys_role " +
                    "WHERE delete_flag = 0 AND role_type = :roleType " +
                    "ORDER BY id";

        Map<String, Object> params = Maps.newHashMap();
        params.put("roleType", roleType);

        return baseDao.queryListForSql(sql, params, RoleDTO.class);
    }

    /**
     * 获取租户模板角色列表（仅返回可同步给租户的角色）
     *
     * @return 租户模板角色列表
     */
    public List<RoleDTO> getTenantTemplateRoles() {
        return listByRoleType(2);
    }

    /**
     * 获取平台专用角色列表（仅返回平台管理功能角色）
     *
     * @return 平台角色列表
     */
    public List<RoleDTO> getPlatformRoles() {
        return listByRoleType(1);
    }

    /**
     * 根据ID获取角色详情
     *
     * @param id 角色ID
     * @return 角色详情
     * @throws BusinessException 当角色不存在时抛出
     */
    public RoleDTO getById(Long id) {
        if (id == null) {
            throw new BusinessException("角色ID不能为空");
        }

        String sql = "SELECT id, role_name, description, role_type, feature_level, status, " +
                    "created_by, created_at, updated_by, updated_at " +
                    "FROM sys_role " +
                    "WHERE id = :id AND delete_flag = 0";

        Map<String, Object> params = Maps.newHashMap();
        params.put("id", id);

        SysRole role = baseDao.querySingleForSql(sql, params, SysRole.class);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        return convertToDTO(role);
    }

    /**
     * 创建平台角色
     *
     * @param request 创建请求参数
     * @param currentUserId 当前用户ID
     * @throws BusinessException 当业务错误时抛出
     */
    @Transactional(readOnly = false)
    public void create(RoleCreateRequest request, Long currentUserId) {
        // 验证 roleType 必须设置
        if (request.getRoleType() == null) {
            throw new BusinessException("角色类型不能为空");
        }

        // 检查角色名是否已存在
        String checkSql = "SELECT COUNT(*) FROM sys_role WHERE role_name = :roleName AND delete_flag = 0";
        Map<String, Object> checkParams = Maps.newHashMap();
        checkParams.put("roleName", request.getRoleName());
        Long count = baseDao.querySingleForSql(checkSql, checkParams, Long.class);
        if (count != null && count > 0) {
            throw new BusinessException("角色名已存在");
        }

        SysRole role = new SysRole();
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setRoleType(request.getRoleType());
        role.setFeatureLevel(request.getFeatureLevel() != null ? request.getFeatureLevel() : 1);
        role.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        role.setDeleteFlag(0);
        role.setCreatedBy(currentUserId);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedBy(currentUserId);
        role.setUpdatedAt(LocalDateTime.now());

        baseDao.insertPO(role, true);

        log.info("创建平台角色成功: roleName={}, id={}, roleType={}",
                request.getRoleName(), role.getId(), request.getRoleType());
    }

    /**
     * 更新平台角色
     *
     * @param request 更新请求参数
     * @param currentUserId 当前用户ID
     * @throws BusinessException 当角色不存在或其他业务错误时抛出
     */
    @Transactional(readOnly = false)
    public void update(RoleUpdateRequest request, Long currentUserId) {
        if (request.getId() == null) {
            throw new BusinessException("角色ID不能为空");
        }

        String sql = "SELECT * FROM sys_role WHERE id = :id AND delete_flag = 0";
        Map<String, Object> params = Maps.newHashMap();
        params.put("id", request.getId());

        SysRole role = baseDao.querySingleForSql(sql, params, SysRole.class);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        // 检查角色名是否与其他角色重复
        if (request.getRoleName() != null && !request.getRoleName().equals(role.getRoleName())) {
            String checkSql = "SELECT COUNT(*) FROM sys_role WHERE role_name = :roleName AND id != :id AND delete_flag = 0";
            Map<String, Object> checkParams = Maps.newHashMap();
            checkParams.put("roleName", request.getRoleName());
            checkParams.put("id", request.getId());
            Long count = baseDao.querySingleForSql(checkSql, checkParams, Long.class);
            if (count != null && count > 0) {
                throw new BusinessException("角色名已存在");
            }
        }

        // 更新角色信息
        if (request.getRoleName() != null) {
            role.setRoleName(request.getRoleName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getRoleType() != null) {
            role.setRoleType(request.getRoleType());
        }
        if (request.getFeatureLevel() != null) {
            role.setFeatureLevel(request.getFeatureLevel());
        }
        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }
        role.setUpdatedBy(currentUserId);
        role.setUpdatedAt(LocalDateTime.now());

        baseDao.updatePO(role);

        log.info("更新平台角色成功: id={}, roleName={}", request.getId(), role.getRoleName());

        // 如果是租户模板角色(roleType=2)，同步更新到已分配的租户
        if (role.getRoleType() != null && role.getRoleType() == 2) {
            syncRoleToTenants(role);
        }
    }

    /**
     * 删除平台角色
     * 会检查是否已分配给租户
     *
     * @param id 角色ID
     * @param currentUserId 当前用户ID
     * @throws BusinessException 当角色不存在或已分配给租户时抛出
     */
    @Transactional(readOnly = false)
    public void delete(Long id, Long currentUserId) {
        if (id == null) {
            throw new BusinessException("角色ID不能为空");
        }

        String sql = "SELECT * FROM sys_role WHERE id = :id AND delete_flag = 0";
        Map<String, Object> params = Maps.newHashMap();
        params.put("id", id);

        SysRole role = baseDao.querySingleForSql(sql, params, SysRole.class);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        // 如果是租户模板角色，检查是否已分配给租户
        if (role.getRoleType() != null && role.getRoleType() == 2) {
            if (hasAssignedToTenants(id)) {
                throw new BusinessException("该角色已分配给租户，无法删除。请先取消租户分配。");
            }
        }

        // 逻辑删除
        role.setDeleteFlag(1);
        role.setUpdatedBy(currentUserId);
        role.setUpdatedAt(LocalDateTime.now());

        baseDao.updatePO(role);

        log.info("删除平台角色成功: id={}, roleName={}", id, role.getRoleName());
    }

    /**
     * 配置角色菜单权限
     *
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * @param currentUserId 当前用户ID
     * @throws BusinessException 当角色不存在时抛出
     */
    @Transactional(readOnly = false)
    public void assignMenus(Long roleId, List<Long> menuIds, Long currentUserId) {
        if (roleId == null) {
            throw new BusinessException("角色ID不能为空");
        }

        // 验证角色是否存在
        String checkSql = "SELECT * FROM sys_role WHERE id = :id AND delete_flag = 0";
        Map<String, Object> checkParams = Maps.newHashMap();
        checkParams.put("id", roleId);
        SysRole role = baseDao.querySingleForSql(checkSql, checkParams, SysRole.class);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        // 删除原有的角色-菜单关联
        String deleteSql = "DELETE FROM sys_role_menu WHERE role_id = :roleId";
        Map<String, Object> deleteParams = Maps.newHashMap();
        deleteParams.put("roleId", roleId);
        jdbcTemplate.update(deleteSql, deleteParams);

        // 插入新的角色-菜单关联
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                SysRoleMenu roleMenu = new SysRoleMenu();
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                roleMenu.setCreatedAt(LocalDateTime.now());
                baseDao.insertPO(roleMenu, true);
            }
        }

        log.info("配置角色菜单权限成功: roleId={}, menuCount={}", roleId, menuIds != null ? menuIds.size() : 0);

        // 如果是租户模板角色，同步角色-菜单关联到已分配的租户
        if (role.getRoleType() != null && role.getRoleType() == 2) {
            syncRoleMenusToTenants(roleId, menuIds);
        }
    }

    /**
     * 获取角色的菜单ID列表
     *
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    public List<Long> getRoleMenuIds(Long roleId) {
        String sql = "SELECT menu_id FROM sys_role_menu WHERE role_id = :roleId";
        Map<String, Object> params = Maps.newHashMap();
        params.put("roleId", roleId);

        return baseDao.queryListForSql(sql, params, Long.class);
    }

    /**
     * 同步角色更新到所有已分配该角色的租户
     * 仅适用于租户模板角色(roleType=2)
     *
     * @param platformRole 平台角色对象
     */
    @Transactional(readOnly = false)
    public void syncRoleToTenants(SysRole platformRole) {
        if (platformRole.getRoleType() == null || platformRole.getRoleType() != 2) {
            log.warn("仅租户模板角色可以同步到租户，roleId={}, roleType={}",
                    platformRole.getId(), platformRole.getRoleType());
            return;
        }

        // 查询所有分配了该角色的租户
        String sql = "SELECT DISTINCT t.schema_name " +
                    "FROM sys_tenant t " +
                    "INNER JOIN sys_tenant_role tr ON t.id = tr.tenant_id " +
                    "WHERE tr.platform_role_id = :platformRoleId AND t.status = 1 AND t.delete_flag = 0";

        Map<String, Object> params = Maps.newHashMap();
        params.put("platformRoleId", platformRole.getId());

        List<String> schemaNames = baseDao.queryListForSql(sql, params, String.class);

        if (schemaNames.isEmpty()) {
            log.info("角色未分配给任何租户，无需同步: roleId={}", platformRole.getId());
            return;
        }

        log.info("开始同步角色到 {} 个租户: roleId={}", schemaNames.size(), platformRole.getId());

        // 为每个租户更新角色
        for (String schemaName : schemaNames) {
            try {
                syncRoleToTenant(platformRole, schemaName);
            } catch (Exception e) {
                log.error("同步角色到租户失败: schemaName={}, roleId={}",
                        schemaName, platformRole.getId(), e);
                // 继续同步其他租户，不中断流程
            }
        }

        log.info("角色同步完成: roleId={}, 成功同步租户数={}", platformRole.getId(), schemaNames.size());
    }

    /**
     * 同步角色-菜单关联关系到所有已分配该角色的租户
     *
     * @param platformRoleId 平台角色ID
     * @param menuIds 菜单ID列表
     */
    @Transactional(readOnly = false)
    public void syncRoleMenusToTenants(Long platformRoleId, List<Long> menuIds) {
        // 查询所有分配了该角色的租户
        String sql = "SELECT DISTINCT t.schema_name " +
                    "FROM sys_tenant t " +
                    "INNER JOIN sys_tenant_role tr ON t.id = tr.tenant_id " +
                    "WHERE tr.platform_role_id = :platformRoleId AND t.status = 1 AND t.delete_flag = 0";

        Map<String, Object> params = Maps.newHashMap();
        params.put("platformRoleId", platformRoleId);

        List<String> schemaNames = baseDao.queryListForSql(sql, params, String.class);

        if (schemaNames.isEmpty()) {
            log.info("角色未分配给任何租户，无需同步权限: roleId={}", platformRoleId);
            return;
        }

        log.info("开始同步角色权限到 {} 个租户: roleId={}", schemaNames.size(), platformRoleId);

        // 为每个租户更新角色-菜单关联
        for (String schemaName : schemaNames) {
            try {
                syncRoleMenusToTenant(platformRoleId, menuIds, schemaName);
            } catch (Exception e) {
                log.error("同步角色权限到租户失败: schemaName={}, roleId={}",
                        schemaName, platformRoleId, e);
                // 继续同步其他租户，不中断流程
            }
        }

        log.info("角色权限同步完成: roleId={}, 成功同步租户数={}", platformRoleId, schemaNames.size());
    }

    /**
     * 同步角色到单个租户
     *
     * @param platformRole 平台角色对象
     * @param schemaName 租户schema名称
     */
    private void syncRoleToTenant(SysRole platformRole, String schemaName) {
        // 切换到租户schema
        setSearchPath(schemaName);

        try {
            // 查找租户schema中对应的角色（通过platform_role_id）
            String findSql = "SELECT id FROM sys_role WHERE platform_role_id = :platformRoleId AND delete_flag = 0";
            Map<String, Object> findParams = Maps.newHashMap();
            findParams.put("platformRoleId", platformRole.getId());

            Long tenantRoleId = baseDao.querySingleForSql(findSql, findParams, Long.class);

            if (tenantRoleId == null) {
                log.warn("租户schema中未找到对应角色，跳过同步: schema={}, platformRoleId={}",
                        schemaName, platformRole.getId());
                return;
            }

            // 更新租户角色
            String updateSql = "UPDATE sys_role SET " +
                    "role_name = :roleName, " +
                    "description = :description, " +
                    "status = :status, " +
                    "updated_at = :updatedAt " +
                    "WHERE id = :id";

            Map<String, Object> updateParams = Maps.newHashMap();
            updateParams.put("roleName", platformRole.getRoleName());
            updateParams.put("description", platformRole.getDescription());
            updateParams.put("status", platformRole.getStatus());
            updateParams.put("updatedAt", LocalDateTime.now());
            updateParams.put("id", tenantRoleId);

            jdbcTemplate.update(updateSql, updateParams);

            log.info("同步角色到租户成功: schema={}, platformRoleId={}, tenantRoleId={}",
                    schemaName, platformRole.getId(), tenantRoleId);

        } finally {
            // 切换回public schema
            resetSearchPath();
        }
    }

    /**
     * 同步角色-菜单关联到单个租户
     *
     * @param platformRoleId 平台角色ID
     * @param platformMenuIds 平台菜单ID列表
     * @param schemaName 租户schema名称
     */
    private void syncRoleMenusToTenant(Long platformRoleId, List<Long> platformMenuIds, String schemaName) {
        // 切换到租户schema
        setSearchPath(schemaName);

        try {
            // 查找租户schema中对应的角色
            String findRoleSql = "SELECT id FROM sys_role WHERE platform_role_id = :platformRoleId AND delete_flag = 0";
            Map<String, Object> findRoleParams = Maps.newHashMap();
            findRoleParams.put("platformRoleId", platformRoleId);

            Long tenantRoleId = baseDao.querySingleForSql(findRoleSql, findRoleParams, Long.class);

            if (tenantRoleId == null) {
                log.warn("租户schema中未找到对应角色，跳过权限同步: schema={}, platformRoleId={}",
                        schemaName, platformRoleId);
                return;
            }

            // 删除原有的角色-菜单关联
            String deleteSql = "DELETE FROM sys_role_menu WHERE role_id = :roleId";
            Map<String, Object> deleteParams = Maps.newHashMap();
            deleteParams.put("roleId", tenantRoleId);
            jdbcTemplate.update(deleteSql, deleteParams);

            // 插入新的角色-菜单关联（需要将平台菜单ID转换为租户菜单ID）
            if (platformMenuIds != null && !platformMenuIds.isEmpty()) {
                for (Long platformMenuId : platformMenuIds) {
                    // 查找租户schema中对应的菜单ID
                    String findMenuSql = "SELECT id FROM sys_menu WHERE platform_menu_id = :platformMenuId AND delete_flag = 0";
                    Map<String, Object> findMenuParams = Maps.newHashMap();
                    findMenuParams.put("platformMenuId", platformMenuId);

                    Long tenantMenuId = baseDao.querySingleForSql(findMenuSql, findMenuParams, Long.class);

                    if (tenantMenuId != null) {
                        SysRoleMenu roleMenu = new SysRoleMenu();
                        roleMenu.setRoleId(tenantRoleId);
                        roleMenu.setMenuId(tenantMenuId);
                        roleMenu.setCreatedAt(LocalDateTime.now());
                        baseDao.insertPO(roleMenu, true);
                    }
                }
            }

            log.info("同步角色权限到租户成功: schema={}, platformRoleId={}, tenantRoleId={}, menuCount={}",
                    schemaName, platformRoleId, tenantRoleId, platformMenuIds != null ? platformMenuIds.size() : 0);

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
     * 检查角色是否已分配给租户
     */
    private boolean hasAssignedToTenants(Long roleId) {
        String sql = "SELECT COUNT(*) FROM sys_tenant_role WHERE platform_role_id = :roleId";
        Map<String, Object> params = Maps.newHashMap();
        params.put("roleId", roleId);

        Long count = baseDao.querySingleForSql(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * 转换为DTO
     */
    private RoleDTO convertToDTO(SysRole role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setRoleName(role.getRoleName());
        dto.setDescription(role.getDescription());
        dto.setRoleType(role.getRoleType());
        dto.setFeatureLevel(role.getFeatureLevel());
        dto.setStatus(role.getStatus());
        dto.setCreatedBy(role.getCreatedBy());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedBy(role.getUpdatedBy());
        dto.setUpdatedAt(role.getUpdatedAt());
        return dto;
    }
}
