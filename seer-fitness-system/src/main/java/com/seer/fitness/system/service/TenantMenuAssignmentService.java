package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.framework.annotation.PublicSchema;
import com.seer.fitness.system.entity.SysMenu;
import com.seer.fitness.system.entity.SysTenant;
import com.seer.fitness.system.entity.SysTenantMenu;
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
 * 租户菜单分配服务
 * 负责将平台菜单模板分配给租户
 * <p>
 * 核心流程：
 * 1. 分配菜单：
 *    - 检查租户和菜单是否存在
 *    - 验证菜单类型（仅租户模板菜单可分配）
 *    - 将菜单数据复制到租户schema
 *    - 记录分配关系到 sys_tenant_menu
 * <p>
 * 2. 取消分配：
 *    - 从租户schema删除菜单数据
 *    - 删除 sys_tenant_menu 记录
 * <p>
 * 3. 批量分配：
 *    - 支持一次性分配多个菜单
 *    - 递归处理菜单树（父菜单必须先分配）
 *
 * @author seer-fitness
 * @since 2025-10-17
 */
@Service
@Slf4j
@PublicSchema(reason = "租户菜单分配管理")
public class TenantMenuAssignmentService extends BaseServiceImpl {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * 分配单个菜单给租户
     *
     * @param tenantId 租户ID
     * @param platformMenuId 平台菜单ID
     * @param currentUserId 当前操作用户ID
     * @throws BusinessException 当租户不存在、菜单不存在或已分配时抛出
     */
    @Transactional(readOnly = false)
    public void assignMenu(Long tenantId, Long platformMenuId, Long currentUserId) {
        // 1. 验证租户
        SysTenant tenant = validateTenant(tenantId);

        // 2. 验证平台菜单
        SysMenu platformMenu = validatePlatformMenu(platformMenuId);

        // 3. 检查是否已分配
        if (isMenuAssigned(tenantId, platformMenuId)) {
            throw new BusinessException("该菜单已分配给此租户");
        }

        // 4. 如果有父菜单，确保父菜单也已分配
        if (platformMenu.getParentId() != null && platformMenu.getParentId() != 0) {
            ensureParentMenuAssigned(tenantId, platformMenu.getParentId(), currentUserId);
        }

        // 5. 将菜单复制到租户schema
        copyMenuToTenantSchema(tenant.getSchemaName(), platformMenu);

        // 6. 记录分配关系
        recordAssignment(tenantId, platformMenuId, currentUserId);

        log.info("菜单分配成功: tenantId={}, platformMenuId={}, menuName={}",
                tenantId, platformMenuId, platformMenu.getMenuName());
    }

    /**
     * 批量分配菜单给租户
     * 自动处理菜单树结构（父菜单会先于子菜单分配）
     *
     * @param tenantId 租户ID
     * @param platformMenuIds 平台菜单ID列表
     * @param currentUserId 当前操作用户ID
     * @return 成功分配的菜单数量
     */
    @Transactional(readOnly = false)
    public int assignMenus(Long tenantId, List<Long> platformMenuIds, Long currentUserId) {
        if (platformMenuIds == null || platformMenuIds.isEmpty()) {
            throw new BusinessException("菜单ID列表不能为空");
        }

        // 验证租户
        SysTenant tenant = validateTenant(tenantId);

        int successCount = 0;
        List<String> errors = new ArrayList<>();

        // 按顺序分配每个菜单
        for (Long platformMenuId : platformMenuIds) {
            try {
                // 如果已分配，跳过
                if (!isMenuAssigned(tenantId, platformMenuId)) {
                    assignMenu(tenantId, platformMenuId, currentUserId);
                    successCount++;
                } else {
                    log.info("菜单已分配，跳过: tenantId={}, platformMenuId={}", tenantId, platformMenuId);
                }
            } catch (Exception e) {
                String errorMsg = String.format("分配菜单失败: menuId=%d, error=%s",
                        platformMenuId, e.getMessage());
                errors.add(errorMsg);
                log.error(errorMsg, e);
                // 继续分配其他菜单，不中断流程
            }
        }

        if (!errors.isEmpty() && successCount == 0) {
            throw new BusinessException("批量分配失败：" + String.join("; ", errors));
        }

        log.info("批量分配菜单完成: tenantId={}, 成功={}, 失败={}",
                tenantId, successCount, errors.size());

        return successCount;
    }

    /**
     * 取消菜单分配
     * 会检查是否有子菜单依赖
     *
     * @param tenantId 租户ID
     * @param platformMenuId 平台菜单ID
     * @throws BusinessException 当菜单未分配或存在子菜单时抛出
     */
    @Transactional(readOnly = false)
    public void unassignMenu(Long tenantId, Long platformMenuId) {
        // 1. 验证租户
        SysTenant tenant = validateTenant(tenantId);

        // 2. 检查是否已分配
        if (!isMenuAssigned(tenantId, platformMenuId)) {
            throw new BusinessException("该菜单未分配给此租户");
        }

        // 3. 检查是否有子菜单依赖（已分配的子菜单）
        if (hasAssignedChildren(tenantId, platformMenuId)) {
            throw new BusinessException("该菜单存在已分配的子菜单，请先取消子菜单分配");
        }

        // 4. 从租户schema删除菜单
        deleteMenuFromTenantSchema(tenant.getSchemaName(), platformMenuId);

        // 5. 删除分配记录
        deleteAssignment(tenantId, platformMenuId);

        log.info("取消菜单分配成功: tenantId={}, platformMenuId={}", tenantId, platformMenuId);
    }

    /**
     * 查询租户已分配的菜单ID列表
     *
     * @param tenantId 租户ID
     * @return 已分配的平台菜单ID列表
     */
    public List<Long> getAssignedMenuIds(Long tenantId) {
        String sql = "SELECT platform_menu_id FROM sys_tenant_menu WHERE tenant_id = :tenantId";
        Map<String, Object> params = Maps.newHashMap();
        params.put("tenantId", tenantId);

        return baseDao.queryListForSql(sql, params, Long.class);
    }

    /**
     * 查询租户已分配的菜单详情列表
     *
     * @param tenantId 租户ID
     * @return 已分配的菜单列表
     */
    public List<SysMenu> getAssignedMenus(Long tenantId) {
        String sql = "SELECT m.* FROM sys_menu m " +
                    "INNER JOIN sys_tenant_menu tm ON m.id = tm.platform_menu_id " +
                    "WHERE tm.tenant_id = :tenantId AND m.delete_flag = 0 " +
                    "ORDER BY m.sort_order";

        Map<String, Object> params = Maps.newHashMap();
        params.put("tenantId", tenantId);

        return baseDao.queryListForSql(sql, params, SysMenu.class);
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
            throw new BusinessException("租户状态异常，无法分配菜单");
        }

        return tenant;
    }

    /**
     * 验证平台菜单是否存在且可分配
     */
    private SysMenu validatePlatformMenu(Long platformMenuId) {
        if (platformMenuId == null) {
            throw new BusinessException("菜单ID不能为空");
        }

        String sql = "SELECT * FROM sys_menu WHERE id = :id AND delete_flag = 0";
        Map<String, Object> params = Maps.newHashMap();
        params.put("id", platformMenuId);

        SysMenu menu = baseDao.querySingleForSql(sql, params, SysMenu.class);
        if (menu == null) {
            throw new BusinessException("平台菜单不存在");
        }

        // 验证菜单类型（仅租户模板菜单可分配）
        if (menu.getMenuType() == null || menu.getMenuType() != 2) {
            throw new BusinessException("仅租户模板菜单可以分配给租户（menu_type=2）");
        }

        return menu;
    }

    /**
     * 检查菜单是否已分配
     */
    private boolean isMenuAssigned(Long tenantId, Long platformMenuId) {
        String sql = "SELECT COUNT(*) FROM sys_tenant_menu " +
                    "WHERE tenant_id = :tenantId AND platform_menu_id = :platformMenuId";

        Map<String, Object> params = Maps.newHashMap();
        params.put("tenantId", tenantId);
        params.put("platformMenuId", platformMenuId);

        Long count = baseDao.querySingleForSql(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * 检查是否有已分配的子菜单
     */
    private boolean hasAssignedChildren(Long tenantId, Long platformMenuId) {
        String sql = "SELECT COUNT(*) FROM sys_menu m " +
                    "INNER JOIN sys_tenant_menu tm ON m.id = tm.platform_menu_id " +
                    "WHERE tm.tenant_id = :tenantId AND m.parent_id = :parentId AND m.delete_flag = 0";

        Map<String, Object> params = Maps.newHashMap();
        params.put("tenantId", tenantId);
        params.put("parentId", platformMenuId);

        Long count = baseDao.querySingleForSql(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * 确保父菜单已分配（如果未分配则自动分配）
     */
    private void ensureParentMenuAssigned(Long tenantId, Long parentMenuId, Long currentUserId) {
        if (!isMenuAssigned(tenantId, parentMenuId)) {
            log.info("父菜单未分配，自动分配: tenantId={}, parentMenuId={}", tenantId, parentMenuId);
            assignMenu(tenantId, parentMenuId, currentUserId);
        }
    }

    /**
     * 将菜单复制到租户schema
     */
    private void copyMenuToTenantSchema(String schemaName, SysMenu platformMenu) {
        // 切换到租户schema
        setSearchPath(schemaName);

        try {
            // 使用雪花算法生成新的租户菜单ID
            long tenantMenuId = System.currentTimeMillis(); // 简化处理，实际应使用雪花算法

            String insertSql = "INSERT INTO sys_menu (id, menu_name, parent_id, type, path, permission, icon, " +
                    "sort_order, platform_menu_id, status, delete_flag, created_at, updated_at) " +
                    "VALUES (:id, :menuName, :parentId, :type, :path, :permission, :icon, " +
                    ":sortOrder, :platformMenuId, :status, 0, :now, :now)";

            Map<String, Object> params = Maps.newHashMap();
            params.put("id", tenantMenuId);
            params.put("menuName", platformMenu.getMenuName());
            params.put("parentId", platformMenu.getParentId() != null ? platformMenu.getParentId() : 0);
            params.put("type", platformMenu.getType());
            params.put("path", platformMenu.getPath());
            params.put("permission", platformMenu.getPermission());
            params.put("icon", platformMenu.getIcon());
            params.put("sortOrder", platformMenu.getSortOrder());
            params.put("platformMenuId", platformMenu.getId());
            params.put("status", 1); // 默认启用
            params.put("now", LocalDateTime.now());

            jdbcTemplate.update(insertSql, params);

            log.info("菜单复制到租户schema成功: schema={}, platformMenuId={}, tenantMenuId={}",
                    schemaName, platformMenu.getId(), tenantMenuId);

        } finally {
            // 切换回public schema
            resetSearchPath();
        }
    }

    /**
     * 从租户schema删除菜单
     */
    private void deleteMenuFromTenantSchema(String schemaName, Long platformMenuId) {
        // 切换到租户schema
        setSearchPath(schemaName);

        try {
            // 逻辑删除
            String deleteSql = "UPDATE sys_menu SET delete_flag = 1, updated_at = :now " +
                    "WHERE platform_menu_id = :platformMenuId";

            Map<String, Object> params = Maps.newHashMap();
            params.put("platformMenuId", platformMenuId);
            params.put("now", LocalDateTime.now());

            int rows = jdbcTemplate.update(deleteSql, params);

            log.info("从租户schema删除菜单成功: schema={}, platformMenuId={}, rows={}",
                    schemaName, platformMenuId, rows);

        } finally {
            // 切换回public schema
            resetSearchPath();
        }
    }

    /**
     * 记录分配关系
     */
    private void recordAssignment(Long tenantId, Long platformMenuId, Long currentUserId) {
        SysTenantMenu assignment = new SysTenantMenu();
        assignment.setTenantId(tenantId);
        assignment.setPlatformMenuId(platformMenuId);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setAssignedBy(currentUserId);

        baseDao.insertPO(assignment, true);
    }

    /**
     * 删除分配记录
     */
    private void deleteAssignment(Long tenantId, Long platformMenuId) {
        String sql = "DELETE FROM sys_tenant_menu " +
                    "WHERE tenant_id = :tenantId AND platform_menu_id = :platformMenuId";

        Map<String, Object> params = Maps.newHashMap();
        params.put("tenantId", tenantId);
        params.put("platformMenuId", platformMenuId);

        jdbcTemplate.update(sql, params);
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
