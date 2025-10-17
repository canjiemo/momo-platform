package com.seer.fitness.system.controller;

import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.system.dto.*;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.system.service.ITenantProjectService;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.mycommon.pager.PagerHandler;
import io.github.mocanjie.base.mymvc.controller.MyBaseController;
import io.github.mocanjie.base.mymvc.data.MyResponseResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户项目管理控制器
 * 管理租户 schema 中的 seer_project_info（学校实际使用的项目）
 *
 * @author seer-fitness
 */
@RestController
@RequestMapping("/tenant/project")
public class TenantProjectController extends MyBaseController {

    @Autowired
    private ITenantProjectService tenantProjectService;

    /**
     * 分页查询租户项目
     *
     * @param param 查询参数
     * @return 分页结果
     */
    @PostMapping("/search")
    @RequireAuth(permissions = {"tenant:project:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "tenant_project",
        description = "分页查询租户项目"
    )
    public MyResponseResult<Pager<ProjectInfoDTO>> search(@RequestBody ProjectInfoQueryParam param) {
        return super.doJsonPagerOut(tenantProjectService.search(param, PagerHandler.createPager(param)));
    }

    /**
     * 根据ID获取项目详情
     *
     * @param id 项目ID
     * @return 项目详情
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = {"tenant:project:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "tenant_project",
        description = "查询租户项目详情"
    )
    public MyResponseResult<ProjectInfoDTO> getById(@PathVariable Long id) {
        return super.doJsonOut(tenantProjectService.getById(id));
    }

    /**
     * 创建租户项目（学校自定义项目）
     *
     * @param request 创建请求参数
     * @return 操作结果
     */
    @PostMapping("/create")
    @RequireAuth(permissions = {"tenant:project:create"})
    @OperationLog(
        type = OperationType.CREATE,
        module = "tenant_project",
        description = "创建租户项目"
    )
    public MyResponseResult create(@Valid @RequestBody ProjectInfoCreateRequest request) {
        tenantProjectService.create(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 更新租户项目
     *
     * @param request 更新请求参数
     * @return 操作结果
     */
    @PostMapping("/update")
    @RequireAuth(permissions = {"tenant:project:update"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "tenant_project",
        description = "更新租户项目"
    )
    public MyResponseResult update(@Valid @RequestBody ProjectInfoUpdateRequest request) {
        tenantProjectService.update(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 删除租户项目
     *
     * @param request 删除请求参数
     * @return 操作结果
     */
    @PostMapping("/delete")
    @RequireAuth(permissions = {"tenant:project:delete"})
    @OperationLog(
        type = OperationType.DELETE,
        module = "tenant_project",
        description = "删除租户项目"
    )
    public MyResponseResult delete(@Valid @RequestBody ProjectInfoDeleteRequest request) {
        tenantProjectService.delete(request.getIds());
        return super.doJsonDefaultMsg();
    }

    /**
     * 获取所有平台项目（供租户选择分配）
     *
     * @return 平台项目列表
     */
    @GetMapping("/platform-projects")
    @RequireAuth(permissions = {"tenant:project:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "tenant_project",
        description = "查询平台项目列表"
    )
    public MyResponseResult<List<ProjectInfoDTO>> getPlatformProjects() {
        return super.doJsonOut(tenantProjectService.getPlatformProjects());
    }

    /**
     * 从平台分配项目到租户
     * 将选中的平台项目复制到当前租户的项目表
     *
     * @param request 分配请求参数
     * @return 操作结果
     */
    @PostMapping("/assign-from-platform")
    @RequireAuth(permissions = {"tenant:project:assign"})
    @OperationLog(
        type = OperationType.CREATE,
        module = "tenant_project",
        description = "从平台分配项目"
    )
    public MyResponseResult assignFromPlatform(@Valid @RequestBody ProjectAssignRequest request) {
        tenantProjectService.assignFromPlatform(request);
        return super.doJsonDefaultMsg();
    }
}
