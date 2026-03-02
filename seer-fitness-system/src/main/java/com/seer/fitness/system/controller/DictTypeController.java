package com.seer.fitness.system.controller;

import com.seer.fitness.system.dto.*;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.system.service.IDictTypeService;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.mycommon.pager.PagerHandler;
import io.github.mocanjie.base.mymvc.controller.MyBaseController;
import io.github.mocanjie.base.mymvc.data.MyResponseResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典类型管理控制器
 * 提供字典类型的增删改查功能，支持Redis缓存
 *
 * @author seer-fitness
 */
@RestController
@RequestMapping("/system/dict-type")
public class DictTypeController extends MyBaseController {

    @Autowired
    private IDictTypeService dictTypeService;

    /**
     * 分页查询字典类型
     * 支持复杂查询条件、分页、排序
     *
     * @param param 查询参数
     * @return 分页结果
     */
    @PostMapping("/search")
    @RequireAuth(permissions = {"dict:type:view"})
    public MyResponseResult<Pager<DictTypeDTO>> search(@RequestBody DictTypeQueryParam param) {
        return super.doJsonPagerOut(dictTypeService.search(param, PagerHandler.createPager(param)));
    }

    /**
     * 获取字典类型列表
     * GET方式，不分页，用于前端下拉选择等场景
     *
     * @return 字典类型列表
     */
    @GetMapping("/list")
    @RequireAuth(login = true)
    public MyResponseResult<List<DictTypeDTO>> list() {
        List<DictTypeDTO> types = dictTypeService.list();
        return super.doJsonOut(types);
    }

    /**
     * 根据ID获取字典类型详情
     *
     * @param id 字典类型ID
     * @return 字典类型详情
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = {"dict:type:view"})
    public MyResponseResult<DictTypeDTO> getById(@PathVariable String id) {
        return super.doJsonOut(dictTypeService.getById(id));
    }

    /**
     * 根据字典类型获取详情
     *
     * @param dictType 字典类型编码
     * @return 字典类型详情
     */
    @GetMapping("/type/{dictType}")
    @RequireAuth(permissions = {"dict:type:view"})
    public MyResponseResult<DictTypeDTO> getByDictType(@PathVariable String dictType) {
        return super.doJsonOut(dictTypeService.getByDictType(dictType));
    }

    /**
     * 创建字典类型
     *
     * @param request 创建请求参数
     * @return 操作结果
     */
    @PostMapping("/create")
    @RequireAuth(permissions = {"dict:type:create"})
    public MyResponseResult create(@Valid @RequestBody DictTypeCreateRequest request) {
        dictTypeService.create(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 更新字典类型
     *
     * @param request 更新请求参数
     * @return 操作结果
     */
    @PostMapping("/update")
    @RequireAuth(permissions = {"dict:type:update"})
    public MyResponseResult update(@Valid @RequestBody DictTypeUpdateRequest request) {
        dictTypeService.update(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 批量删除字典类型
     *
     * @param request 删除请求参数
     * @return 操作结果
     */
    @PostMapping("/delete")
    @RequireAuth(permissions = {"dict:type:delete"})
    public MyResponseResult delete(@Valid @RequestBody DictTypeDeleteRequest request) {
        dictTypeService.delete(request.getIds());
        return super.doJsonDefaultMsg();
    }

    /**
     * 刷新字典缓存
     *
     * @param dictType 字典类型编码
     * @return 操作结果
     */
    @PostMapping("/refresh")
    @RequireAuth(permissions = {"dict:type:update"})
    public MyResponseResult refreshCache(@RequestParam String dictType) {
        dictTypeService.refreshCache(dictType);
        return super.doJsonDefaultMsg();
    }
}