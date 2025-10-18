package com.seer.fitness.system.controller;

import com.seer.fitness.framework.annotation.PublicSchema;
import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.system.entity.SysMenu;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.system.service.TenantMenuAssignmentService;
import com.seer.fitness.system.util.SecurityContextUtil;
import io.github.mocanjie.base.mymvc.controller.MyBaseController;
import io.github.mocanjie.base.mymvc.data.MyResponseResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户菜单分配控制器
 * 负责将平台菜单模板分配给租户
 * <p>
 * 功能：
 * 1. 分配菜单：单个分配、批量分配
 * 2. 取消分配：取消单个菜单分配
 * 3. 查询已分配菜单：获取租户已分配的菜单列表
 * <p>
 * 权限要求：
 * - 所有接口需要租户菜单分配权限（tenant:assign-menu）
 * - 使用 @PublicSchema 确保操作 public schema
 *
 * @author seer-fitness
 * @since 2025-10-17
 */
@RestController
@RequestMapping("/platform/tenant/menu")
@PublicSchema(reason = "租户菜单分配管理")
public class TenantMenuAssignmentController extends MyBaseController {

    @Autowired
    private TenantMenuAssignmentService assignmentService;

    /**
     * 分配单个菜单给租户
     *
     * @param request 分配请求
     * @return 操作结果
     */
    @PostMapping("/assign")
    @RequireAuth(permissions = {"tenant:assign-menu"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "tenant_menu_assignment",
        description = "分配菜单给租户"
    )
    public MyResponseResult<Void> assignMenu(@Valid @RequestBody AssignMenuRequest request) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdAsLong();
        assignmentService.assignMenu(request.getTenantId(), request.getPlatformMenuId(), currentUserId);
        return super.doJsonDefaultMsg();
    }

    /**
     * 批量分配菜单给租户
     *
     * @param request 批量分配请求
     * @return 操作结果（包含成功分配的数量）
     */
    @PostMapping("/assign-batch")
    @RequireAuth(permissions = {"tenant:assign-menu"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "tenant_menu_assignment",
        description = "批量分配菜单给租户"
    )
    public MyResponseResult<BatchAssignResult> assignMenus(@Valid @RequestBody BatchAssignMenuRequest request) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdAsLong();
        int successCount = assignmentService.assignMenus(
                request.getTenantId(),
                request.getPlatformMenuIds(),
                currentUserId
        );

        BatchAssignResult result = new BatchAssignResult();
        result.setSuccessCount(successCount);
        result.setTotalCount(request.getPlatformMenuIds().size());

        return super.doJsonOut(result);
    }

    /**
     * 取消菜单分配
     *
     * @param request 取消分配请求
     * @return 操作结果
     */
    @PostMapping("/unassign")
    @RequireAuth(permissions = {"tenant:assign-menu"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "tenant_menu_assignment",
        description = "取消菜单分配"
    )
    public MyResponseResult<Void> unassignMenu(@Valid @RequestBody AssignMenuRequest request) {
        assignmentService.unassignMenu(request.getTenantId(), request.getPlatformMenuId());
        return super.doJsonDefaultMsg();
    }

    /**
     * 获取租户已分配的菜单ID列表
     *
     * @param tenantId 租户ID
     * @return 已分配的菜单ID列表
     */
    @GetMapping("/assigned-ids/{tenantId}")
    @RequireAuth(permissions = {"tenant:assign-menu"})
    public MyResponseResult<List<Long>> getAssignedMenuIds(@PathVariable Long tenantId) {
        List<Long> menuIds = assignmentService.getAssignedMenuIds(tenantId);
        return super.doJsonOut(menuIds);
    }

    /**
     * 获取租户已分配的菜单详情列表
     *
     * @param tenantId 租户ID
     * @return 已分配的菜单详情列表
     */
    @GetMapping("/assigned-menus/{tenantId}")
    @RequireAuth(permissions = {"tenant:assign-menu"})
    public MyResponseResult<List<SysMenu>> getAssignedMenus(@PathVariable Long tenantId) {
        List<SysMenu> menus = assignmentService.getAssignedMenus(tenantId);
        return super.doJsonOut(menus);
    }

    /**
     * 分配菜单请求DTO
     */
    @Data
    public static class AssignMenuRequest {
        @NotNull(message = "租户ID不能为空")
        private Long tenantId;

        @NotNull(message = "菜单ID不能为空")
        private Long platformMenuId;
    }

    /**
     * 批量分配菜单请求DTO
     */
    @Data
    public static class BatchAssignMenuRequest {
        @NotNull(message = "租户ID不能为空")
        private Long tenantId;

        @NotEmpty(message = "菜单ID列表不能为空")
        private List<Long> platformMenuIds;
    }

    /**
     * 批量分配结果DTO
     */
    @Data
    public static class BatchAssignResult {
        private Integer successCount;
        private Integer totalCount;
    }
}
