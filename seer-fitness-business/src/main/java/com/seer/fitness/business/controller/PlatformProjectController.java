package com.seer.fitness.business.controller;

import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.business.dto.*;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.business.service.IPlatformProjectService;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.mycommon.pager.PagerHandler;
import io.github.mocanjie.base.mymvc.controller.MyBaseController;
import io.github.mocanjie.base.mymvc.data.MyResponseResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 平台项目管理控制器
 * 管理 public.seer_project_info 中的平台项目库
 *
 * @author seer-fitness
 */
@RestController
@RequestMapping("/platform/project")
public class PlatformProjectController extends MyBaseController {

    @Autowired
    private IPlatformProjectService platformProjectService;

    /**
     * 分页查询平台项目
     *
     * @param param 查询参数
     * @return 分页结果
     */
    @PostMapping("/search")
    @RequireAuth(permissions = {"platform:project:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "platform_project",
        description = "分页查询平台项目"
    )
    public MyResponseResult<Pager<ProjectInfoDTO>> search(@RequestBody ProjectInfoQueryParam param) {
        return super.doJsonPagerOut(platformProjectService.search(param, PagerHandler.createPager(param)));
    }

    /**
     * 根据ID获取项目详情
     *
     * @param id 项目ID
     * @return 项目详情
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = {"platform:project:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "platform_project",
        description = "查询平台项目详情"
    )
    public MyResponseResult<ProjectInfoDTO> getById(@PathVariable Long id) {
        return super.doJsonOut(platformProjectService.getById(id));
    }

    /**
     * 创建平台项目
     *
     * @param request 创建请求参数
     * @return 操作结果
     */
    @PostMapping("/create")
    @RequireAuth(permissions = {"platform:project:create"})
    @OperationLog(
        type = OperationType.CREATE,
        module = "platform_project",
        description = "创建平台项目"
    )
    public MyResponseResult create(@Valid @RequestBody ProjectInfoCreateRequest request) {
        platformProjectService.create(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 更新平台项目
     *
     * @param request 更新请求参数
     * @return 操作结果
     */
    @PostMapping("/update")
    @RequireAuth(permissions = {"platform:project:update"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "platform_project",
        description = "更新平台项目"
    )
    public MyResponseResult update(@Valid @RequestBody ProjectInfoUpdateRequest request) {
        platformProjectService.update(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 删除平台项目
     *
     * @param request 删除请求参数
     * @return 操作结果
     */
    @PostMapping("/delete")
    @RequireAuth(permissions = {"platform:project:delete"})
    @OperationLog(
        type = OperationType.DELETE,
        module = "platform_project",
        description = "删除平台项目"
    )
    public MyResponseResult delete(@Valid @RequestBody ProjectInfoDeleteRequest request) {
        platformProjectService.delete(request.getIds());
        return super.doJsonDefaultMsg();
    }
}
