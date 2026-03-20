package io.github.canjiemo.momo.system.controller;

import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.momo.framework.annotation.OperationLog;
import io.github.canjiemo.momo.framework.annotation.RequireAuth;
import io.github.canjiemo.momo.framework.enums.OperationType;
import io.github.canjiemo.momo.system.dto.*;
import io.github.canjiemo.momo.system.service.ISysJobLogService;
import io.github.canjiemo.momo.system.service.ISysJobService;
import io.github.canjiemo.mycommon.pager.Pager;
import io.github.canjiemo.mycommon.pager.PagerHandler;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/job")
public class SysJobController extends MyBaseController {

    @Autowired
    private ISysJobService jobService;

    @Autowired
    private ISysJobLogService jobLogService;

    @PostMapping("/search")
    @RequireAuth(permissions = {"job:view"})
    @OperationLog(type = OperationType.QUERY, module = "job", description = "分页查询定时任务")
    public MyResponseResult<Pager<JobDTO>> search(@RequestBody JobQueryParam param) {
        return super.doJsonPagerOut(jobService.search(param, PagerHandler.createPager(param)));
    }

    @GetMapping("/{id}")
    @RequireAuth(permissions = {"job:view"})
    public MyResponseResult<JobDTO> getById(@PathVariable Long id) {
        return super.doJsonOut(jobService.getById(id));
    }

    @PostMapping("/create")
    @RequireAuth(permissions = {"job:create"})
    @OperationLog(type = OperationType.CREATE, module = "job", description = "创建定时任务")
    public MyResponseResult create(@Valid @RequestBody JobCreateRequest request) {
        jobService.create(request);
        return super.doJsonDefaultMsg();
    }

    @PostMapping("/update")
    @RequireAuth(permissions = {"job:update"})
    @OperationLog(type = OperationType.UPDATE, module = "job", description = "更新定时任务")
    public MyResponseResult update(@Valid @RequestBody JobUpdateRequest request) {
        jobService.update(request);
        return super.doJsonDefaultMsg();
    }

    @PostMapping("/delete/{id}")
    @RequireAuth(permissions = {"job:delete"})
    @OperationLog(type = OperationType.DELETE, module = "job", description = "删除定时任务")
    public MyResponseResult delete(@PathVariable Long id) {
        jobService.delete(id);
        return super.doJsonDefaultMsg();
    }

    @PostMapping("/enable/{id}")
    @RequireAuth(permissions = {"job:update"})
    @OperationLog(type = OperationType.UPDATE, module = "job", description = "启用定时任务")
    public MyResponseResult enable(@PathVariable Long id) {
        jobService.enable(id);
        return super.doJsonDefaultMsg();
    }

    @PostMapping("/disable/{id}")
    @RequireAuth(permissions = {"job:update"})
    @OperationLog(type = OperationType.UPDATE, module = "job", description = "停用定时任务")
    public MyResponseResult disable(@PathVariable Long id) {
        jobService.disable(id);
        return super.doJsonDefaultMsg();
    }

    @PostMapping("/trigger/{id}")
    @RequireAuth(permissions = {"job:trigger"})
    @OperationLog(type = OperationType.UPDATE, module = "job", description = "手动触发定时任务")
    public MyResponseResult trigger(@PathVariable Long id) {
        jobService.trigger(id);
        return super.doJsonDefaultMsg();
    }

    // ---------- 执行历史 ----------

    @PostMapping("/log/search")
    @RequireAuth(permissions = {"job:view"})
    @OperationLog(type = OperationType.QUERY, module = "job", description = "查询定时任务执行历史")
    public MyResponseResult<Pager<JobLogDTO>> logSearch(@RequestBody JobLogQueryParam param) {
        return super.doJsonPagerOut(jobLogService.search(param, PagerHandler.createPager(param)));
    }

    @GetMapping("/log/{id}")
    @RequireAuth(permissions = {"job:view"})
    public MyResponseResult<JobLogDTO> logDetail(@PathVariable Long id) {
        return super.doJsonOut(jobLogService.getById(id));
    }
}
