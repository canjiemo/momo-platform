package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.framework.annotation.PublicSchema;
import com.seer.fitness.system.entity.SysRole;
import com.seer.fitness.system.entity.SysRoleMenu;
import com.seer.fitness.system.entity.SysTenant;
import com.seer.fitness.system.entity.SysTenantRole;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 租户角色同步服务
 * 负责将平台角色模板同步给租户
 * <p>
 * 核心流程：
 * 1. 同步角色：
 *    - 检查租户和角色是否存在
 *    - 验证角色类型（仅租户模板角色可同步）
 *    - 将角色数据复制到租户schema
 *    - 同步角色-菜单关联关系
 *    - 记录同步关系到 sys_tenant_role
 * <p>
 * 2. 批量同步：
 *    - 支持一次性同步多个角色
 *    - 支持同步到所有租户
 * <p>
 * 3. 重新同步：
 *    - 支持更新已同步的角色数据
 *    - 自动同步角色-菜单关联关系
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Service
@Slf4j
@PublicSchema(reason = "租户角色同步管理")
public class TenantRoleSyncService extends BaseServiceImpl {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * 同步单个角色给租户
     *
     * @param tenantId 租户ID
     * @param platformRoleId 平台角色ID
     * @param currentUserId 当前操作用户ID
     * @throws BusinessException 当租户不存在、角色不存在或已同步时抛出
     */
    @Transactional(readOnly = false)
    public void syncRole(Long tenantId, Long platformRoleId, Long currentUserId) {
        // 1. 验证租户
        SysTenant tenant = validateTenant(tenantId);

        // 2. 验证平台角色
        SysRole platformRole = validatePlatformRole(platformRoleId);

        // 3. 检查是否已同步
        if (isRoleSynced(tenantId, platformRoleId)) {
            log.info("角色已同步，将重新同步: tenantId={}, platformRoleId={}", tenantId, platformRoleId);
            // 重新同步（更新现有数据）
            resyncRole(tenant.getSchemaName(), platformRole);
            return;
        }

        // 4. 将角色复制到租户schema
        copyRoleToTenantSchema(tenant.getSchemaName(), platformRole);

        // 5. 同步角色-菜单关联关系
        syncRoleMenusToTenant(tenant.getSchemaName(), platformRoleId);

        // 6. 记录同步关系
        recordSync(tenantId, platformRoleId, currentUserId);

        log.info("角色同步成功: tenantId={}, platformRoleId={}, roleName={}",
                tenantId, platformRoleId, platformRole.getRoleName());
    }

    /**
     * 批量同步角色给租户
     *
     * @param tenantId 租户ID
     * @param platformRoleIds 平台角色ID列表
     * @param currentUserId 当前操作用户ID
     * @return 成功同步的角色数量
     */
    @Transactional(readOnly = false)
    public int syncRoles(Long tenantId, List<Long> platformRoleIds, Long currentUserId) {
        if (platformRoleIds == null || platformRoleIds.isEmpty()) {
            throw new BusinessException("角色ID列表不能为空");
        }

        // 验证租户
        SysTenant tenant = validateTenant(tenantId);

        int successCount = 0;
        List<String> errors = new ArrayList<>();

        // 按顺序同步每个角色
        for (Long platformRoleId : platformRoleIds) {
            try {
                syncRole(tenantId, platformRoleId, currentUserId);
                successCount++;
            } catch (Exception e) {
                String errorMsg = String.format("同步角色失败: roleId=%d, error=%s",
                        platformRoleId, e.getMessage());
                errors.add(errorMsg);
                log.error(errorMsg, e);
                // 继续同步其他角色，不中断流程
            }
        }

        if (!errors.isEmpty() && successCount == 0) {
            throw new BusinessException("批量同步失败：" + String.join("; ", errors));
        }

        log.info("批量同步角色完成: tenantId={}, 成功={}, 失败={}",
                tenantId, successCount, errors.size());

        return successCount;
    }

    /**
     * 同步角色到所有租户
     *
     * @param platformRoleId 平台角色ID
     * @param currentUserId 当前操作用户ID
     * @return 成功同步的租户数量
     */
    @Transactional(readOnly = false)
    public int syncRoleToAllTenants(Long platformRoleId, Long currentUserId) {
        // 验证平台角色
        SysRole platformRole = validatePlatformRole(platformRoleId);

        // 查询所有启用的租户
        String sql = "SELECT id, schema_name FROM sys_tenant WHERE status = 1 AND delete_flag = 0";
        List<Map<String, Object>> tenants = jdbcTemplate.queryForList(sql, Maps.newHashMap());

        if (tenants.isEmpty()) {
            log.warn("没有启用的租户，无需同步角色: platformRoleId={}", platformRoleId);
            return 0;
        }

        int successCount = 0;
        List<String> errors = new ArrayList<>();

        for (Map<String, Object> tenant : tenants) {
            Long tenantId = ((Number) tenant.get("id")).longValue();
            try {
                syncRole(tenantId, platformRoleId, currentUserId);
                successCount++;
            } catch (Exception e) {
                String errorMsg = String.format("同步角色到租户失败: tenantId=%d, error=%s",
                        tenantId, e.getMessage());
                errors.add(errorMsg);
                log.error(errorMsg, e);
                // 继续同步其他租户，不中断流程
            }
        }

        log.info("同步角色到所有租户完成: platformRoleId={}, 总租户数={}, 成功={}, 失败={}",
                platformRoleId, tenants.size(), successCount, errors.size());

        return successCount;
    }

    /**
     * 批量同步角色到所有租户
     *
     * @param platformRoleIds 平台角色ID列表
     * @param currentUserId 当前操作用户ID
     * @return 成功同步的次数（租户数 x 角色数）
     */
    @Transactional(readOnly = false)
    public int syncRolesToAllTenants(List<Long> platformRoleIds, Long currentUserId) {
        if (platformRoleIds == null || platformRoleIds.isEmpty()) {
            throw new BusinessException("角色ID列表不能为空");
        }

        int totalSuccess = 0;
        for (Long platformRoleId : platformRoleIds) {
            totalSuccess += syncRoleToAllTenants(platformRoleId, currentUserId);
        }

        log.info("批量同步角色到所有租户完成: 角色数={}, 总成功次数={}",
                platformRoleIds.size(), totalSuccess);

        return totalSuccess;
    }

    /**
     * 查询租户已同步的角色ID列表
     *
     * @param tenantId 租户ID
     * @return 已同步的平台角色ID列表
     */
    public List<Long> getSyncedRoleIds(Long tenantId) {
        String sql = "SELECT platform_role_id FROM sys_tenant_role WHERE tenant_id = :tenantId";
        Map<String, Object> params = Maps.newHashMap();
        params.put("tenantId", tenantId);

        return baseDao.queryListForSql(sql, params, Long.class);
    }

    /**
     * 查询租户已同步的角色详情列表
     *
     * @param tenantId 租户ID
     * @return 已同步的角色列表
     */
    public List<SysRole> getSyncedRoles(Long tenantId) {
        String sql = "SELECT r.* FROM sys_role r " +
                    "INNER JOIN sys_tenant_role tr ON r.id = tr.platform_role_id " +
                    "WHERE tr.tenant_id = :tenantId AND r.delete_flag = 0 " +
                    "ORDER BY r.id";

        Map<String, Object> params = Maps.newHashMap();
        params.put("tenantId", tenantId);

        return baseDao.queryListForSql(sql, params, SysRole.class);
    }

    /**
     * 验证租户是否存在且有效
     */
    private SysTenant validateTenant(Long tenantId) {
        if (tenantId == null) {
            throw new BusinessException("租户ID不能为空");
        }

        String sql = "SELECT * FROM sys_tenant WHERE id = :id AND delete_flag = 0";
        Map<String, Object> params = Maps.newHashMap();
        params.put("id", tenantId);

        SysTenant tenant = baseDao.querySingleForSql(sql, params, SysTenant.class);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }

        // 允许PENDING(0)和ACTIVE(1)状态，因为在租户初始化期间状态可能是PENDING
        if (tenant.getStatus() == null || (tenant.getStatus() != 0 && tenant.getStatus() != 1)) {
            throw new BusinessException("租户状态异常，无法同步角色");
        }

        return tenant;
    }

    /**
     * 验证平台角色是否存在且可同步
     */
    private SysRole validatePlatformRole(Long platformRoleId) {
        if (platformRoleId == null) {
            throw new BusinessException("角色ID不能为空");
        }

        String sql = "SELECT * FROM sys_role WHERE id = :id AND delete_flag = 0";
        Map<String, Object> params = Maps.newHashMap();
        params.put("id", platformRoleId);

        SysRole role = baseDao.querySingleForSql(sql, params, SysRole.class);
        if (role == null) {
            throw new BusinessException("平台角色不存在");
        }

        // 验证角色类型（仅租户模板角色可同步）
        if (role.getRoleType() == null || role.getRoleType() != 2) {
            throw new BusinessException("仅租户模板角色可以同步给租户（role_type=2）");
        }

        return role;
    }

    /**
     * 检查角色是否已同步
     */
    private boolean isRoleSynced(Long tenantId, Long platformRoleId) {
        String sql = "SELECT COUNT(*) FROM sys_tenant_role " +
                    "WHERE tenant_id = :tenantId AND platform_role_id = :platformRoleId";

        Map<String, Object> params = Maps.newHashMap();
        params.put("tenantId", tenantId);
        params.put("platformRoleId", platformRoleId);

        Long count = baseDao.querySingleForSql(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * 将角色复制到租户schema
     */
    private void copyRoleToTenantSchema(String schemaName, SysRole platformRole) {
        // 切换到租户schema
        setSearchPath(schemaName);

        try {
            // 使用雪花算法生成新的租户角色ID
            long tenantRoleId = System.currentTimeMillis(); // 简化处理，实际应使用雪花算法

            String insertSql = "INSERT INTO sys_role (id, role_name, description, platform_role_id, " +
                    "status, delete_flag, created_by, created_at, updated_by, updated_at) " +
                    "VALUES (:id, :roleName, :description, :platformRoleId, " +
                    ":status, 0, :createdBy, :now, :updatedBy, :now)";

            Map<String, Object> params = Maps.newHashMap();
            params.put("id", tenantRoleId);
            params.put("roleName", platformRole.getRoleName());
            params.put("description", platformRole.getDescription());
            params.put("platformRoleId", platformRole.getId());
            params.put("status", platformRole.getStatus() != null ? platformRole.getStatus() : 1);
            params.put("createdBy", platformRole.getCreatedBy());
            params.put("updatedBy", platformRole.getUpdatedBy());
            params.put("now", LocalDateTime.now());

            jdbcTemplate.update(insertSql, params);

            log.info("角色复制到租户schema成功: schema={}, platformRoleId={}, tenantRoleId={}",
                    schemaName, platformRole.getId(), tenantRoleId);

        } finally {
            // 切换回public schema
            resetSearchPath();
        }
    }

    /**
     * 重新同步角色（更新现有数据）
     */
    private void resyncRole(String schemaName, SysRole platformRole) {
        // 切换到租户schema
        setSearchPath(schemaName);

        try {
            // 更新租户角色
            String updateSql = "UPDATE sys_role SET " +
                    "role_name = :roleName, " +
                    "description = :description, " +
                    "status = :status, " +
                    "updated_at = :now " +
                    "WHERE platform_role_id = :platformRoleId AND delete_flag = 0";

            Map<String, Object> params = Maps.newHashMap();
            params.put("roleName", platformRole.getRoleName());
            params.put("description", platformRole.getDescription());
            params.put("status", platformRole.getStatus());
            params.put("platformRoleId", platformRole.getId());
            params.put("now", LocalDateTime.now());

            int rows = jdbcTemplate.update(updateSql, params);

            log.info("重新同步角色成功: schema={}, platformRoleId={}, rows={}",
                    schemaName, platformRole.getId(), rows);

            // 重新同步角色-菜单关联关系
            syncRoleMenusToTenant(schemaName, platformRole.getId());

        } finally {
            // 切换回public schema
            resetSearchPath();
        }
    }

    /**
     * 同步角色-菜单关联关系到租户
     */
    private void syncRoleMenusToTenant(String schemaName, Long platformRoleId) {
        // 1. 查询平台角色的菜单ID列表
        String querySql = "SELECT menu_id FROM sys_role_menu WHERE role_id = :roleId";
        Map<String, Object> queryParams = Maps.newHashMap();
        queryParams.put("roleId", platformRoleId);
        List<Long> platformMenuIds = baseDao.queryListForSql(querySql, queryParams, Long.class);

        // 切换到租户schema
        setSearchPath(schemaName);

        try {
            // 2. 查找租户schema中对应的角色ID
            String findRoleSql = "SELECT id FROM sys_role WHERE platform_role_id = :platformRoleId AND delete_flag = 0";
            Map<String, Object> findRoleParams = Maps.newHashMap();
            findRoleParams.put("platformRoleId", platformRoleId);
            Long tenantRoleId = baseDao.querySingleForSql(findRoleSql, findRoleParams, Long.class);

            if (tenantRoleId == null) {
                log.warn("租户schema中未找到对应角色，跳过权限同步: schema={}, platformRoleId={}",
                        schemaName, platformRoleId);
                return;
            }

            // 3. 删除原有的角色-菜单关联
            String deleteSql = "DELETE FROM sys_role_menu WHERE role_id = :roleId";
            Map<String, Object> deleteParams = Maps.newHashMap();
            deleteParams.put("roleId", tenantRoleId);
            jdbcTemplate.update(deleteSql, deleteParams);

            // 4. 插入新的角色-菜单关联（需要将平台菜单ID转换为租户菜单ID）
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
     * 记录同步关系
     */
    private void recordSync(Long tenantId, Long platformRoleId, Long currentUserId) {
        SysTenantRole sync = new SysTenantRole();
        sync.setTenantId(tenantId);
        sync.setPlatformRoleId(platformRoleId);
        sync.setSyncedAt(LocalDateTime.now());
        sync.setSyncedBy(currentUserId);

        baseDao.insertPO(sync, true);
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
}
