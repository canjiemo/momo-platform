package com.seer.fitness.system.controller;

import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.system.dto.OperationLogDTO;
import com.seer.fitness.system.dto.OperationLogQueryParam;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.system.service.IOperationLogService;
import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.mycommon.pager.Pager;
import io.github.canjiemo.mycommon.pager.PagerHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 操作日志管理控制器
 * 提供操作日志的查询、统计、导出、清理等功能
 *
 * @author seer-fitness
 */
@RestController
@RequestMapping("/system/operation-log")
public class OperationLogController extends MyBaseController {

    @Autowired
    private IOperationLogService operationLogService;

    /**
     * 分页查询操作日志
     * 支持复杂查询条件、分页、排序
     *
     * @param param 查询参数
     * @return 分页结果
     */
    @PostMapping("/search")
    @RequireAuth(permissions = {"operation_log:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "operation_log",
        description = "查询操作日志",
        recordResponse = false
    )
    public MyResponseResult<Pager<OperationLogDTO>> search(@RequestBody OperationLogQueryParam param) {
        return super.doJsonPagerOut(operationLogService.search(param, PagerHandler.createPager(param)));
    }

    /**
     * 根据ID获取操作日志详情
     *
     * @param id 日志ID
     * @return 操作日志详情
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = {"operation_log:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "operation_log",
        description = "查看操作日志详情",
        businessId = "#id",
        recordResponse = false
    )
    public MyResponseResult<OperationLogDTO> getById(@PathVariable Long id) {
        return super.doJsonOut(operationLogService.getById(id));
    }

    /**
     * 批量删除操作日志
     * 物理删除，谨慎使用
     *
     * @param ids 日志ID数组
     * @return 操作结果
     */
    @PostMapping("/delete")
    @RequireAuth(permissions = {"operation_log:delete"})
    @OperationLog(
        type = OperationType.DELETE,
        module = "operation_log",
        description = "删除操作日志",
        businessId = "#ids",
        recordRequest = true
    )
    public MyResponseResult delete(@RequestBody Long[] ids) {
        operationLogService.delete(ids);
        return super.doJsonDefaultMsg();
    }

    /**
     * 清理历史日志
     * 删除指定天数之前的日志记录
     *
     * @param days 保留天数
     * @return 清理结果
     */
    @PostMapping("/clean/{days}")
    @RequireAuth(permissions = {"operation_log:delete"})
    @OperationLog(
        type = OperationType.DELETE,
        module = "operation_log",
        description = "清理历史操作日志",
        businessId = "#days",
        recordRequest = true,
        recordResponse = true
    )
    public MyResponseResult<Map<String, Object>> cleanHistoryLogs(@PathVariable int days) {
        int deletedCount = operationLogService.cleanHistoryLogs(days);
        return super.doJsonOut(Map.of("deletedCount", deletedCount, "days", days));
    }

    /**
     * 统计操作类型分布
     *
     * @param days 统计天数，0表示全部
     * @return 操作类型统计结果
     */
    @GetMapping("/stats/operation-type")
    @RequireAuth(permissions = {"operation_log:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "operation_log",
        description = "统计操作类型分布",
        recordResponse = false
    )
    public MyResponseResult<Map<String, Long>> getOperationTypeStats(@RequestParam(defaultValue = "7") int days) {
        return super.doJsonOut(operationLogService.getOperationTypeStats(days));
    }

    /**
     * 统计模块操作分布
     *
     * @param days 统计天数，0表示全部
     * @return 模块操作统计结果
     */
    @GetMapping("/stats/module")
    @RequireAuth(permissions = {"operation_log:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "operation_log",
        description = "统计模块操作分布",
        recordResponse = false
    )
    public MyResponseResult<Map<String, Long>> getModuleStats(@RequestParam(defaultValue = "7") int days) {
        return super.doJsonOut(operationLogService.getModuleStats(days));
    }

    /**
     * 统计用户操作活跃度
     *
     * @param days 统计天数
     * @param limit 返回结果数量限制
     * @return 用户操作统计结果
     */
    @GetMapping("/stats/user-activity")
    @RequireAuth(permissions = {"operation_log:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "operation_log",
        description = "统计用户操作活跃度",
        recordResponse = false
    )
    public MyResponseResult<List<Map<String, Object>>> getUserActivityStats(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit) {
        return super.doJsonOut(operationLogService.getUserActivityStats(days, limit));
    }

    /**
     * 统计操作失败情况
     *
     * @param days 统计天数
     * @return 失败操作统计结果
     */
    @GetMapping("/stats/failure")
    @RequireAuth(permissions = {"operation_log:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "operation_log",
        description = "统计操作失败情况",
        recordResponse = false
    )
    public MyResponseResult<Map<String, Object>> getFailureStats(@RequestParam(defaultValue = "7") int days) {
        return super.doJsonOut(operationLogService.getFailureStats(days));
    }

    /**
     * 获取操作趋势数据
     *
     * @param days 统计天数
     * @return 操作趋势数据
     */
    @GetMapping("/stats/trend")
    @RequireAuth(permissions = {"operation_log:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "operation_log",
        description = "查看操作趋势",
        recordResponse = false
    )
    public MyResponseResult<List<Map<String, Object>>> getOperationTrend(@RequestParam(defaultValue = "7") int days) {
        return super.doJsonOut(operationLogService.getOperationTrend(days));
    }

    /**
     * 导出操作日志
     *
     * @param param 查询参数
     * @return 导出的日志列表
     */
    @PostMapping("/export")
    @RequireAuth(permissions = {"operation_log:export"})
    @OperationLog(
        type = OperationType.EXPORT,
        module = "operation_log",
        description = "导出操作日志",
        recordRequest = true,
        recordResponse = false
    )
    public MyResponseResult<List<OperationLogDTO>> exportLogs(@RequestBody OperationLogQueryParam param) {
        return super.doJsonOut(operationLogService.exportLogs(param));
    }

    /**
     * 获取所有操作类型选项
     * 用于前端下拉选择
     *
     * @return 操作类型列表
     */
    @GetMapping("/operation-types")
    @RequireAuth(permissions = {"operation_log:view"})
    public MyResponseResult<List<Map<String, String>>> getOperationTypes() {
        List<Map<String, String>> operationTypes = List.of(
            Map.of("code", OperationType.CREATE.getCode(), "name", OperationType.CREATE.getDescription()),
            Map.of("code", OperationType.UPDATE.getCode(), "name", OperationType.UPDATE.getDescription()),
            Map.of("code", OperationType.DELETE.getCode(), "name", OperationType.DELETE.getDescription()),
            Map.of("code", OperationType.QUERY.getCode(), "name", OperationType.QUERY.getDescription()),
            Map.of("code", OperationType.LOGIN.getCode(), "name", OperationType.LOGIN.getDescription()),
            Map.of("code", OperationType.LOGOUT.getCode(), "name", OperationType.LOGOUT.getDescription()),
            Map.of("code", OperationType.IMPORT.getCode(), "name", OperationType.IMPORT.getDescription()),
            Map.of("code", OperationType.EXPORT.getCode(), "name", OperationType.EXPORT.getDescription()),
            Map.of("code", OperationType.AUDIT.getCode(), "name", OperationType.AUDIT.getDescription()),
            Map.of("code", OperationType.OTHER.getCode(), "name", OperationType.OTHER.getDescription())
        );
        return super.doJsonOut(operationTypes);
    }
}