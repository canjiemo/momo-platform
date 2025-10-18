package com.seer.fitness.system.controller;

import com.seer.fitness.framework.annotation.PublicSchema;
import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.system.service.TenantRoleSyncService;
import com.seer.fitness.system.util.SecurityContextUtil;
import io.github.mocanjie.base.mymvc.controller.MyBaseController;
import io.github.mocanjie.base.mymvc.data.MyResponseResult;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户角色同步控制器
 * 负责将平台角色模板同步到租户（从 public.sys_role 复制到 tenant_schema.sys_role）
 * <p>
 * 功能：
 * 1. 单个同步：同步一个角色到一个或多个租户
 * 2. 批量同步：同步多个角色到多个租户
 * 3. 全量同步：同步指定角色到所有租户
 * 4. 自动同步角色-菜单关联关系
 * <p>
 * 权限要求：
 * - 所有接口需要租户角色同步权限（platform:tenant:role:sync）
 * - 使用 @PublicSchema 确保查询 public 中的角色和租户信息
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@RestController
@RequestMapping("/platform/tenant/role")
@PublicSchema(reason = "租户角色同步管理")
public class TenantRoleSyncController extends MyBaseController {

    @Autowired
    private TenantRoleSyncService tenantRoleSyncService;

    /**
     * 同步单个角色到单个租户
     *
     * @param request 同步请求参数
     * @return 操作结果
     */
    @PostMapping("/sync")
    @RequireAuth(permissions = {"platform:tenant:role:sync"})
    @OperationLog(
        type = OperationType.SYNC,
        module = "tenant_role_sync",
        description = "同步平台角色到租户"
    )
    public MyResponseResult<Void> syncRole(@RequestBody SyncRoleRequest request) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdAsLong();
        tenantRoleSyncService.syncRole(request.getTenantId(), request.getPlatformRoleId(), currentUserId);
        return super.doJsonDefaultMsg();
    }

    /**
     * 批量同步角色到多个租户
     * 支持：
     * - 单个角色 → 多个租户
     * - 多个角色 → 单个租户
     * - 多个角色 → 多个租户
     *
     * @param request 批量同步请求参数
     * @return 同步结果统计
     */
    @PostMapping("/sync/batch")
    @RequireAuth(permissions = {"platform:tenant:role:sync"})
    @OperationLog(
        type = OperationType.SYNC,
        module = "tenant_role_sync",
        description = "批量同步平台角色到租户"
    )
    public MyResponseResult<Map<String, Object>> syncRoleBatch(@RequestBody SyncRoleBatchRequest request) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdAsLong();

        int successCount = 0;
        // 遍历每个租户，调用syncRoles方法
        for (Long tenantId : request.getTenantIds()) {
            int count = tenantRoleSyncService.syncRoles(
                tenantId,
                request.getPlatformRoleIds(),
                currentUserId
            );
            successCount += count;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalTenants", request.getTenantIds().size());
        result.put("totalRoles", request.getPlatformRoleIds().size());
        result.put("successCount", successCount);
        result.put("expectedCount", request.getTenantIds().size() * request.getPlatformRoleIds().size());

        return super.doJsonOut(result);
    }

    /**
     * 同步指定角色到所有租户
     * 适用场景：新增通用角色模板，需要推送到所有租户
     *
     * @param request 全量同步请求参数
     * @return 同步结果统计
     */
    @PostMapping("/sync/all")
    @RequireAuth(permissions = {"platform:tenant:role:sync"})
    @OperationLog(
        type = OperationType.SYNC,
        module = "tenant_role_sync",
        description = "同步平台角色到所有租户"
    )
    public MyResponseResult<Map<String, Object>> syncRoleToAllTenants(@RequestBody SyncRoleToAllRequest request) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdAsLong();

        int successCount = 0;
        for (Long platformRoleId : request.getPlatformRoleIds()) {
            int count = tenantRoleSyncService.syncRoleToAllTenants(platformRoleId, currentUserId);
            successCount += count;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalRoles", request.getPlatformRoleIds().size());
        result.put("successCount", successCount);

        return super.doJsonOut(result);
    }

    /**
     * 同步单个角色请求参数
     */
    @Data
    public static class SyncRoleRequest {
        /**
         * 租户ID
         */
        private Long tenantId;

        /**
         * 平台角色ID
         */
        private Long platformRoleId;
    }

    /**
     * 批量同步角色请求参数
     */
    @Data
    public static class SyncRoleBatchRequest {
        /**
         * 租户ID列表
         */
        private List<Long> tenantIds;

        /**
         * 平台角色ID列表
         */
        private List<Long> platformRoleIds;
    }

    /**
     * 同步角色到所有租户请求参数
     */
    @Data
    public static class SyncRoleToAllRequest {
        /**
         * 平台角色ID列表
         */
        private List<Long> platformRoleIds;
    }
}
