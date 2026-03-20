package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.base.myjdbc.service.IBaseService;
import io.github.canjiemo.momo.system.dto.DictDataCreateRequest;
import io.github.canjiemo.momo.system.dto.DictDataDTO;
import io.github.canjiemo.momo.system.dto.DictDataQueryParam;
import io.github.canjiemo.momo.system.dto.DictDataUpdateRequest;
import io.github.canjiemo.mycommon.pager.Pager;

import java.util.List;

/**
 * 字典数据管理服务接口
 *
 * @author canjiemo@gmail.com
 */
public interface IDictDataService extends IBaseService {

    /**
     * 分页查询字典数据
     *
     * @param param 查询参数
     * @param pager 分页参数
     * @return 分页结果
     */
    Pager<DictDataDTO> search(DictDataQueryParam param, Pager<DictDataDTO> pager);

    /**
     * 根据字典类型获取字典数据列表（优先从缓存获取）
     *
     * @param dictType 字典类型编码
     * @return 字典数据列表
     */
    List<DictDataDTO> getByDictType(String dictType);

    /**
     * 根据ID获取字典数据详情
     *
     * @param id 字典数据ID
     * @return 字典数据详情
     */
    DictDataDTO getById(String id);

    /**
     * 根据字典类型和字典值获取字典标签
     *
     * @param dictType 字典类型编码
     * @param dictValue 字典值
     * @return 字典标签
     */
    String getDictLabel(String dictType, String dictValue);

    /**
     * 创建字典数据
     *
     * @param request 创建请求参数
     */
    void create(DictDataCreateRequest request);

    /**
     * 更新字典数据
     *
     * @param request 更新请求参数
     */
    void update(DictDataUpdateRequest request);

    /**
     * 批量删除字典数据
     *
     * @param ids 字典数据ID数组
     */
    void delete(String... ids);

    /**
     * 批量更新排序
     *
     * @param ids 字典数据ID列表
     * @param sortOrders 对应的排序值列表
     */
    void batchUpdateSortOrder(List<String> ids, List<Integer> sortOrders);

    /**
     * 检查指定字典类型下是否存在字典数据
     *
     * @param dictType 字典类型编码
     * @return 是否存在
     */
    boolean hasDictDataByType(String dictType);

    /**
     * 更新字典数据表中的字典类型（当字典类型更改时调用）
     *
     * @param oldDictType 原字典类型编码
     * @param newDictType 新字典类型编码
     */
    void updateDictTypeInData(String oldDictType, String newDictType);
}