package com.seer.fitness.system.controller;

import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.system.dto.TenantCreateRequest;
import com.seer.fitness.system.dto.TenantDTO;
import com.seer.fitness.system.dto.TenantQueryParam;
import com.seer.fitness.system.dto.TenantUpdateRequest;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.system.service.ITenantService;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.mycommon.pager.PagerHandler;
import io.github.mocanjie.base.mymvc.controller.MyBaseController;
import io.github.mocanjie.base.mymvc.data.MyResponseResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 租户管理控制器
 * 提供租户的增删改查、启用/禁用等功能
 * <p>
 * 注意：阶段2只提供基本CRUD接口
 * 阶段3将添加Schema自动创建功能
 *
 * @author seer-fitness
 */
@RestController
@RequestMapping("/platform/tenant")
public class TenantController extends MyBaseController {

    @Autowired
    private ITenantService tenantService;

    @Autowired(required = false)
    private com.seer.fitness.system.tenant.DynamicTenantDataSourceManager dataSourceManager;

    /**
     * 分页查询租户
     *
     * @param param 查询参数
     * @return 分页结果
     */
    @PostMapping("/search")
    @RequireAuth(permissions = {"tenant:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "tenant",
        description = "分页查询租户"
    )
    public MyResponseResult<Pager<TenantDTO>> search(@RequestBody TenantQueryParam param) {
        return super.doJsonPagerOut(tenantService.search(param, PagerHandler.createPager(param)));
    }

    /**
     * 根据ID获取租户详情
     *
     * @param id 租户ID
     * @return 租户详情
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = {"tenant:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "tenant",
        description = "查询租户详情"
    )
    public MyResponseResult<TenantDTO> getById(@PathVariable Long id) {
        return super.doJsonOut(tenantService.getById(id));
    }

    /**
     * 根据租户编码获取租户信息
     *
     * @param tenantCode 租户编码
     * @return 租户信息
     */
    @GetMapping("/code/{tenantCode}")
    @RequireAuth(permissions = {"tenant:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "tenant",
        description = "根据编码查询租户"
    )
    public MyResponseResult<TenantDTO> getByCode(@PathVariable String tenantCode) {
        return super.doJsonOut(tenantService.getByCode(tenantCode));
    }

    /**
     * 创建租户
     * 阶段3：自动创建租户记录 + Schema初始化
     * 完整流程：
     * 1. 创建租户记录
     * 2. 自动创建Schema
     * 3. 初始化表结构和数据
     * 4. 创建管理员账号
     * 5. 激活租户
     *
     * @param request 创建请求参数
     * @return 操作结果
     */
    @PostMapping("/create")
    @RequireAuth(permissions = {"tenant:create"})
    @OperationLog(
        type = OperationType.CREATE,
        module = "tenant",
        description = "创建租户"
    )
    public MyResponseResult create(@Valid @RequestBody TenantCreateRequest request) {
        tenantService.create(request);
        return super.doJsonMsg("租户创建成功，Schema已自动初始化并激活");
    }

    /**
     * 更新租户信息
     *
     * @param request 更新请求参数
     * @return 操作结果
     */
    @PostMapping("/update")
    @RequireAuth(permissions = {"tenant:update"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "tenant",
        description = "更新租户"
    )
    public MyResponseResult update(@Valid @RequestBody TenantUpdateRequest request) {
        tenantService.update(request);
        return super.doJsonMsg("租户更新成功");
    }

    /**
     * 启用租户
     *
     * @param id 租户ID
     * @return 操作结果
     */
    @PostMapping("/enable/{id}")
    @RequireAuth(permissions = {"tenant:update"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "tenant",
        description = "启用租户"
    )
    public MyResponseResult enable(@PathVariable Long id) {
        tenantService.enable(id);
        return super.doJsonMsg("租户启用成功");
    }

    /**
     * 禁用租户
     *
     * @param id 租户ID
     * @return 操作结果
     */
    @PostMapping("/disable/{id}")
    @RequireAuth(permissions = {"tenant:update"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "tenant",
        description = "禁用租户"
    )
    public MyResponseResult disable(@PathVariable Long id) {
        tenantService.disable(id);
        return super.doJsonMsg("租户禁用成功");
    }

    /**
     * 删除租户（逻辑删除）
     *
     * @param id 租户ID
     * @return 操作结果
     */
    @PostMapping("/delete/{id}")
    @RequireAuth(permissions = {"tenant:delete"})
    @OperationLog(
        type = OperationType.DELETE,
        module = "tenant",
        description = "删除租户"
    )
    public MyResponseResult delete(@PathVariable Long id) {
        tenantService.delete(id);
        return super.doJsonMsg("租户删除成功（逻辑删除，Schema未删除）");
    }

    /**
     * 检查租户编码是否可用
     *
     * @param tenantCode 租户编码
     * @return true表示可用，false表示已存在
     */
    @GetMapping("/check-code/{tenantCode}")
    @RequireAuth(permissions = {"tenant:create"})
    public MyResponseResult<Boolean> checkCode(@PathVariable String tenantCode) {
        boolean exists = tenantService.existsByCode(tenantCode);
        return super.doJsonOut(!exists); // 返回是否可用（不存在则可用）
    }

    /**
     * 检查Schema名称是否可用
     *
     * @param schemaName Schema名称
     * @return true表示可用，false表示已存在
     */
    @GetMapping("/check-schema/{schemaName}")
    @RequireAuth(permissions = {"tenant:create"})
    public MyResponseResult<Boolean> checkSchemaName(@PathVariable String schemaName) {
        boolean exists = tenantService.existsBySchemaName(schemaName);
        return super.doJsonOut(!exists); // 返回是否可用（不存在则可用）
    }

    // ==================== 数据源管理端点（运维功能）====================

    /**
     * 获取已加载的租户连接池信息
     * 用于监控和运维
     *
     * @return 租户连接池统计信息
     */
    @GetMapping("/datasource/stats")
    @RequireAuth(permissions = {"tenant:view"})
    public MyResponseResult<java.util.Map<String, Object>> getDataSourceStats() {
        if (dataSourceManager == null) {
            return super.doJsonOut(java.util.Collections.singletonMap("message", "多租户模式未启用"));
        }

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("tenantCount", dataSourceManager.getTenantCount());
        stats.put("loadedTenants", dataSourceManager.getLoadedTenants());
        stats.put("multiTenantEnabled", true);

        return super.doJsonOut(stats);
    }

    /**
     * 预热租户连接池
     * 提前创建连接池，避免首次访问延迟
     *
     * @param schemaName 租户Schema名称
     * @return 操作结果
     */
    @PostMapping("/datasource/warmup/{schemaName}")
    @RequireAuth(permissions = {"tenant:update"})
    public MyResponseResult warmUpDataSource(@PathVariable String schemaName) {
        if (dataSourceManager == null) {
            return super.doJsonMsg("多租户模式未启用");
        }

        try {
            dataSourceManager.warmUp(schemaName);
            return super.doJsonMsg("租户连接池预热成功");
        } catch (Exception e) {
            return super.doJsonMsg("租户连接池预热失败：" + e.getMessage());
        }
    }

    /**
     * 移除租户连接池
     * 租户下线或删除时使用
     *
     * @param schemaName 租户Schema名称
     * @return 操作结果
     */
    @PostMapping("/datasource/remove/{schemaName}")
    @RequireAuth(permissions = {"tenant:delete"})
    public MyResponseResult removeDataSource(@PathVariable String schemaName) {
        if (dataSourceManager == null) {
            return super.doJsonMsg("多租户模式未启用");
        }

        try {
            dataSourceManager.removeTenant(schemaName);
            return super.doJsonMsg("租户连接池已移除");
        } catch (Exception e) {
            return super.doJsonMsg("移除租户连接池失败：" + e.getMessage());
        }
    }
}
