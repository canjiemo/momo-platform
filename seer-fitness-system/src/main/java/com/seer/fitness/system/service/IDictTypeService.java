package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.DictTypeCreateRequest;
import com.seer.fitness.system.dto.DictTypeDTO;
import com.seer.fitness.system.dto.DictTypeQueryParam;
import com.seer.fitness.system.dto.DictTypeUpdateRequest;
import io.github.canjiemo.base.myjdbc.service.IBaseService;
import io.github.canjiemo.mycommon.pager.Pager;

import java.util.List;

/**
 * 字典类型管理服务接口
 *
 * @author seer-fitness
 */
public interface IDictTypeService extends IBaseService {

    /**
     * 分页查询字典类型
     *
     * @param param 查询参数
     * @param pager 分页参数
     * @return 分页结果
     */
    Pager<DictTypeDTO> search(DictTypeQueryParam param, Pager pager);

    /**
     * 获取所有字典类型列表（不分页，优先从缓存获取）
     *
     * @return 字典类型列表
     */
    List<DictTypeDTO> list();

    /**
     * 根据ID获取字典类型详情
     *
     * @param id 字典类型ID
     * @return 字典类型详情
     */
    DictTypeDTO getById(String id);

    /**
     * 根据字典类型获取详情（优先从缓存获取）
     *
     * @param dictType 字典类型编码
     * @return 字典类型详情
     */
    DictTypeDTO getByDictType(String dictType);

    /**
     * 创建字典类型
     *
     * @param request 创建请求参数
     */
    void create(DictTypeCreateRequest request);

    /**
     * 更新字典类型
     *
     * @param request 更新请求参数
     */
    void update(DictTypeUpdateRequest request);

    /**
     * 批量删除字典类型
     *
     * @param ids 字典类型ID数组
     */
    void delete(String... ids);

    /**
     * 刷新字典缓存
     *
     * @param dictType 字典类型编码
     */
    void refreshCache(String dictType);
}