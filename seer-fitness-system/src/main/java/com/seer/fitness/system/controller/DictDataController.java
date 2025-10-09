package com.seer.fitness.system.controller;

import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.dto.*;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.system.service.IDictDataService;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.mycommon.pager.PagerHandler;
import io.github.mocanjie.base.mymvc.controller.MyBaseController;
import io.github.mocanjie.base.mymvc.data.MyResponseResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典数据管理控制器
 * 提供字典数据的增删改查功能，支持Redis缓存
 *
 * @author seer-fitness
 */
@RestController
@RequestMapping("/system/dict-data")
public class DictDataController extends MyBaseController {

    @Autowired
    private IDictDataService dictDataService;

    /**
     * 分页查询字典数据
     * 支持复杂查询条件、分页、排序
     *
     * @param param 查询参数
     * @return 分页结果
     */
    @PostMapping("/search")
//    @RequireAuth(permissions = {"dict:data:view"})
    public MyResponseResult<Pager<DictDataDTO>> search(@RequestBody DictDataQueryParam param) {
        return super.doJsonPagerOut(dictDataService.search(param, PagerHandler.createPager(param)));
    }

    /**
     * 根据字典类型获取字典数据列表
     * GET方式，用于前端下拉选择等场景
     *
     * @param dictType 字典类型编码
     * @return 字典数据列表
     */
    @GetMapping("/list/{dictType}")
//    @RequireAuth(permissions = {"dict:data:view"})
    public MyResponseResult<List<DictDataDTO>> list(@PathVariable String dictType) {
        List<DictDataDTO> dataList = dictDataService.getByDictType(dictType);
        return super.doJsonOut(dataList);
    }

    /**
     * 根据ID获取字典数据详情
     *
     * @param id 字典数据ID
     * @return 字典数据详情
     */
    @GetMapping("/{id}")
//    @RequireAuth(permissions = {"dict:data:view"})
    public MyResponseResult<DictDataDTO> getById(@PathVariable String id) {
        return super.doJsonOut(dictDataService.getById(id));
    }

    /**
     * 根据字典类型和字典值获取字典标签
     * 无需权限验证，供系统内部调用
     *
     * @param dictType 字典类型编码
     * @param dictValue 字典值
     * @return 字典标签
     */
    @GetMapping("/label/{dictType}/{dictValue}")
    public MyResponseResult<String> getDictLabel(@PathVariable String dictType, @PathVariable String dictValue) {
        String label = dictDataService.getDictLabel(dictType, dictValue);
        return super.doJsonOut(label);
    }

    /**
     * 创建字典数据
     *
     * @param request 创建请求参数
     * @return 操作结果
     */
    @PostMapping("/create")
    @RequireAuth(permissions = {"dict:data:create"})
    @OperationLog(
        type = OperationType.CREATE,
        module = "dict",
        description = "创建字典数据"
    )
    public MyResponseResult create(@Valid @RequestBody DictDataCreateRequest request) {
        dictDataService.create(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 更新字典数据
     *
     * @param request 更新请求参数
     * @return 操作结果
     */
    @PostMapping("/update")
    @RequireAuth(permissions = {"dict:data:update"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "dict",
        description = "更新字典数据"
    )
    public MyResponseResult update(@Valid @RequestBody DictDataUpdateRequest request) {
        dictDataService.update(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 批量删除字典数据
     *
     * @param request 删除请求参数
     * @return 操作结果
     */
    @PostMapping("/delete")
    @RequireAuth(permissions = {"dict:data:delete"})
    @OperationLog(
        type = OperationType.DELETE,
        module = "dict",
        description = "删除字典数据"
    )
    public MyResponseResult delete(@Valid @RequestBody DictDataDeleteRequest request) {
        dictDataService.delete(request.getIds());
        return super.doJsonDefaultMsg();
    }

    /**
     * 批量更新排序
     *
     * @param request 批量更新排序请求参数
     * @return 操作结果
     */
    @PostMapping("/batch-update-sort")
    @RequireAuth(permissions = {"dict:data:update"})
    public MyResponseResult batchUpdateSortOrder(@Valid @RequestBody DictDataBatchUpdateSortRequest request) {
        dictDataService.batchUpdateSortOrder(request.getIds(), request.getSortOrders());
        return super.doJsonDefaultMsg();
    }
}