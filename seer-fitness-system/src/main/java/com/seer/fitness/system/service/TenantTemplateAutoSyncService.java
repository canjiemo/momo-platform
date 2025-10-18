package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.framework.annotation.PublicSchema;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 租户模板自动同步服务
 * 负责在创建新租户时自动同步平台模板
 * <p>
 * 功能：
 * 1. 根据租户的 feature_level 自动同步对应级别的菜单模板
 * 2. 根据租户的 feature_level 自动同步对应级别的角色模板
 * 3. 自动同步角色-菜单关联关系
 * <p>
 * 同步规则：
 * - 基础版租户（feature_level=1）：同步 feature_level <= 1 的模板
 * - 标准版租户（feature_level=2）：同步 feature_level <= 2 的模板
 * - 企业版租户（feature_level=3）：同步所有模板
 * <p>
 * 调用时机：
 * - 在 TenantSchemaService.initializeTenantSchema() 中调用
 * - 租户Schema创建完成后，立即执行自动同步
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Service
@Slf4j
@PublicSchema(reason = "租户模板自动同步")
public class TenantTemplateAutoSyncService extends BaseServiceImpl {

    @Autowired
    private TenantMenuAssignmentService menuAssignmentService;

    @Autowired
    private TenantRoleSyncService roleSyncService;

    /**
     * 自动同步菜单和角色模板到新租户
     *
     * @param tenantId 租户ID
     * @param featureLevel 租户功能级别（1=基础版 2=标准版 3=企业版）
     * @param currentUserId 当前用户ID（通常是系统管理员）
     * @throws BusinessException 当同步失败时抛出
     */
    @Transactional(readOnly = false)
    public void autoSyncTemplates(Long tenantId, Integer featureLevel, Long currentUserId) {
        if (tenantId == null) {
            throw new BusinessException("租户ID不能为空");
        }

        if (featureLevel == null || featureLevel < 1 || featureLevel > 3) {
            log.warn("租户功能级别无效，使用默认基础版: tenantId={}, featureLevel={}", tenantId, featureLevel);
            featureLevel = 1;
        }

        log.info("开始自动同步模板到新租户: tenantId={}, featureLevel={}", tenantId, featureLevel);

        // 1. 同步菜单模板
        int menuCount = autoSyncMenuTemplates(tenantId, featureLevel, currentUserId);

        // 2. 同步角色模板
        int roleCount = autoSyncRoleTemplates(tenantId, featureLevel, currentUserId);

        log.info("自动同步模板完成: tenantId={}, 菜单数={}, 角色数={}", tenantId, menuCount, roleCount);
    }

    /**
     * 自动同步菜单模板到新租户
     * 根据 feature_level 筛选菜单
     *
     * @param tenantId 租户ID
     * @param featureLevel 租户功能级别
     * @param currentUserId 当前用户ID
     * @return 成功同步的菜单数量
     */
    @Transactional(readOnly = false)
    public int autoSyncMenuTemplates(Long tenantId, Integer featureLevel, Long currentUserId) {
        // 查询平台租户模板角色关联的菜单
        // 逻辑: 获取所有租户模板角色(role_type=2)关联的菜单,去重后同步
        String sql = "SELECT DISTINCT m.id " +
                    "FROM sys_menu m " +
                    "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
                    "INNER JOIN sys_role r ON rm.role_id = r.id " +
                    "WHERE m.delete_flag = 0 " +
                    "AND m.menu_type = 2 " + // 仅租户模板菜单
                    "AND r.role_type = 2 " + // 仅租户模板角色
                    "AND r.delete_flag = 0 " +
                    "AND r.status = 1 " + // 角色必须启用
                    "AND (m.feature_level IS NULL OR m.feature_level <= :featureLevel) " +
                    "ORDER BY m.id"; // 按ID排序,确保父菜单先同步

        Map<String, Object> params = Maps.newHashMap();
        params.put("featureLevel", featureLevel);

        List<Long> menuIds = baseDao.queryListForSql(sql, params, Long.class);

        if (menuIds.isEmpty()) {
            log.warn("没有平台租户模板角色或角色未分配菜单,跳过自动同步: featureLevel={}", featureLevel);
            return 0;
        }

        log.info("查询到 {} 个租户模板角色关联的菜单，开始同步: featureLevel={}", menuIds.size(), featureLevel);

        // 批量分配菜单
        try {
            int successCount = menuAssignmentService.assignMenus(tenantId, menuIds, currentUserId);
            log.info("自动同步菜单模板成功: tenantId={}, 成功数={}", tenantId, successCount);
            return successCount;
        } catch (Exception e) {
            log.error("自动同步菜单模板失败: tenantId={}", tenantId, e);
            throw new BusinessException("自动同步菜单模板失败: " + e.getMessage());
        }
    }

    /**
     * 自动同步角色模板到新租户
     * 根据 feature_level 筛选角色
     *
     * @param tenantId 租户ID
     * @param featureLevel 租户功能级别
     * @param currentUserId 当前用户ID
     * @return 成功同步的角色数量
     */
    @Transactional(readOnly = false)
    public int autoSyncRoleTemplates(Long tenantId, Integer featureLevel, Long currentUserId) {
        // 查询符合feature_level条件的租户模板角色
        String sql = "SELECT id FROM sys_role " +
                    "WHERE delete_flag = 0 " +
                    "AND role_type = 2 " + // 仅租户模板角色
                    "AND (feature_level IS NULL OR feature_level <= :featureLevel) " +
                    "ORDER BY id";

        Map<String, Object> params = Maps.newHashMap();
        params.put("featureLevel", featureLevel);

        List<Long> roleIds = baseDao.queryListForSql(sql, params, Long.class);

        if (roleIds.isEmpty()) {
            log.warn("没有符合条件的角色模板: featureLevel={}", featureLevel);
            return 0;
        }

        log.info("查询到 {} 个符合条件的角色模板，开始同步: featureLevel={}", roleIds.size(), featureLevel);

        // 批量同步角色
        try {
            int successCount = roleSyncService.syncRoles(tenantId, roleIds, currentUserId);
            log.info("自动同步角色模板成功: tenantId={}, 成功数={}", tenantId, successCount);
            return successCount;
        } catch (Exception e) {
            log.error("自动同步角色模板失败: tenantId={}", tenantId, e);
            throw new BusinessException("自动同步角色模板失败: " + e.getMessage());
        }
    }
}
