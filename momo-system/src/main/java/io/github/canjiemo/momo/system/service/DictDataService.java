package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.momo.system.dto.DictDataCreateRequest;
import io.github.canjiemo.momo.system.dto.DictDataDTO;
import io.github.canjiemo.momo.system.dto.DictDataQueryParam;
import io.github.canjiemo.momo.system.dto.DictDataUpdateRequest;
import io.github.canjiemo.momo.system.entity.SysDictData;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 字典数据管理服务
 *
 * @author canjiemo@gmail.com
 */
@Service
@Slf4j
public class DictDataService extends BaseServiceImpl implements IDictDataService {

    @Autowired
    private IDictCacheService dictCacheService;

    /**
     * 分页查询字典数据
     */
    public Pager<DictDataDTO> search(DictDataQueryParam param, Pager<DictDataDTO> pager) {
        return lambdaQuery(SysDictData.class, DictDataDTO.class)
                .eq(SysDictData::getDictType, param.getDictType())
                .like(SysDictData::getDictLabel, param.getDictLabel())
                .like(SysDictData::getDictValue, param.getDictValue())
                .eq(SysDictData::getStatus, param.getStatus())
                .orderByAsc(SysDictData::getSortOrder)
                .orderByDesc(SysDictData::getCreateTime)
                .page(pager);
    }

    /**
     * 根据字典类型获取字典数据列表（优先从缓存获取）
     */
    public List<DictDataDTO> getByDictType(String dictType) {
        if (!StringUtils.hasText(dictType)) {
            throw new BusinessException("字典类型不能为空");
        }

        List<DictDataDTO> cachedList = dictCacheService.getDictDataFromCache(dictType);
        if (cachedList != null) {
            log.debug("从缓存获取字典数据成功: {}, 数量: {}", dictType, cachedList.size());
            return cachedList;
        }

        List<DictDataDTO> list = lambdaQuery(SysDictData.class, DictDataDTO.class)
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getSortOrder)
                .orderByDesc(SysDictData::getCreateTime)
                .list();

        if (list != null) {
            dictCacheService.cacheDictData(dictType, list);
        }

        log.info("从数据库获取字典数据成功: {}, 数量: {}", dictType, list.size());
        return list;
    }

    /**
     * 根据ID获取字典数据详情
     */
    public DictDataDTO getById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException("字典数据ID不能为空");
        }

        DictDataDTO dictData = lambdaQuery(SysDictData.class, DictDataDTO.class)
                .eq(SysDictData::getId, Long.valueOf(id)).one();
        if (dictData == null) {
            throw new BusinessException("字典数据不存在");
        }

        return dictData;
    }

    /**
     * 根据字典类型和字典值获取字典标签
     * <p>
     * 复用 getByDictType() 的完整链路（Redis → DB → 回写），缓存命中时只需内存过滤，无额外 IO。
     */
    public String getDictLabel(String dictType, String dictValue) {
        if (!StringUtils.hasText(dictType) || !StringUtils.hasText(dictValue)) {
            return null;
        }
        List<DictDataDTO> list = getByDictType(dictType);
        return list.stream()
                .filter(item -> dictValue.equals(item.getDictValue()))
                .map(DictDataDTO::getDictLabel)
                .findFirst()
                .orElse(null);
    }

    /**
     * 创建字典数据
     */
    @Transactional(readOnly = false)
    public void create(DictDataCreateRequest request) {
        if (isDictValueExists(request.getDictType(), request.getDictValue())) {
            throw new BusinessException("字典值已存在: " + request.getDictValue());
        }

        if (request.getIsDefault() == 1) {
            clearDefaultStatus(request.getDictType());
        }

        SysDictData dictData = new SysDictData();
        dictData.setDictType(request.getDictType());
        dictData.setDictLabel(request.getDictLabel());
        dictData.setDictValue(request.getDictValue());
        dictData.setDictDescription(request.getDictDescription());
        dictData.setCssClass(request.getCssClass());
        dictData.setListClass(request.getListClass());
        dictData.setIsDefault(request.getIsDefault());
        dictData.setStatus(request.getStatus());
        dictData.setSortOrder(request.getSortOrder());
        dictData.setRemark(request.getRemark());
        dictData.setDeleteFlag(0);

        baseDao.insertPO(dictData, true);

        dictCacheService.deleteDictDataCache(request.getDictType());
        log.info("创建字典数据成功: dictType={}, dictValue={}", request.getDictType(), request.getDictValue());
    }

    /**
     * 更新字典数据
     */
    @Transactional(readOnly = false)
    public void update(DictDataUpdateRequest request) {
        SysDictData existingData = baseDao.queryById(request.getId(), SysDictData.class);
        if (existingData == null) {
            throw new BusinessException("字典数据不存在");
        }

        if (!existingData.getDictValue().equals(request.getDictValue()) &&
            isDictValueExists(request.getDictType(), request.getDictValue())) {
            throw new BusinessException("字典值已存在: " + request.getDictValue());
        }

        if (request.getIsDefault() == 1 && existingData.getIsDefault() != 1) {
            clearDefaultStatus(request.getDictType());
        }

        existingData.setDictType(request.getDictType());
        existingData.setDictLabel(request.getDictLabel());
        existingData.setDictValue(request.getDictValue());
        existingData.setDictDescription(request.getDictDescription());
        existingData.setCssClass(request.getCssClass());
        existingData.setListClass(request.getListClass());
        existingData.setIsDefault(request.getIsDefault());
        existingData.setStatus(request.getStatus());
        existingData.setSortOrder(request.getSortOrder());
        existingData.setRemark(request.getRemark());

        // 全量更新：updatePO 默认跳过 null 字段，会导致可选字段（dictDescription/cssClass/listClass/remark）
        // 传 null 时清不掉。这里语义是"前端提交什么就保存什么"，所以强制写入所有字段。
        baseDao.updatePO(existingData, false);

        String oldDictType = existingData.getDictType();
        dictCacheService.deleteDictDataCache(oldDictType);
        if (!oldDictType.equals(request.getDictType())) {
            dictCacheService.deleteDictDataCache(request.getDictType());
        }

        log.info("更新字典数据成功: id={}, dictType={}, dictValue={}",
                request.getId(), request.getDictType(), request.getDictValue());
    }

    /**
     * 删除字典数据
     */
    @Transactional(readOnly = false)
    public void delete(String... ids) {
        if (ids == null || ids.length == 0) {
            throw new BusinessException("删除的字典数据ID不能为空");
        }

        for (String id : ids) {
            if (!StringUtils.hasText(id)) {
                throw new BusinessException("字典数据ID不能为空");
            }

            SysDictData dictData = baseDao.queryById(Long.valueOf(id), SysDictData.class);
            if (dictData == null) {
                throw new BusinessException("字典数据不存在");
            }

            dictCacheService.deleteDictDataCache(dictData.getDictType());
            log.info("删除字典数据成功: id={}, dictType={}, dictValue={}",
                    id, dictData.getDictType(), dictData.getDictValue());
        }

        baseDao.delByIds(SysDictData.class, ids);
    }

    /**
     * 批量更新排序
     */
    @Transactional(readOnly = false)
    public void batchUpdateSortOrder(List<String> ids, List<Integer> sortOrders) {
        if (ids == null || sortOrders == null || ids.size() != sortOrders.size()) {
            throw new BusinessException("参数错误");
        }

        String dictType = null;

        for (int i = 0; i < ids.size(); i++) {
            SysDictData dictData = baseDao.queryById(Long.valueOf(ids.get(i)), SysDictData.class);
            if (dictData == null) continue;

            if (dictType == null) dictType = dictData.getDictType();

            dictData.setSortOrder(sortOrders.get(i));
            baseDao.updatePO(dictData);
        }

        if (dictType != null) {
            dictCacheService.deleteDictDataCache(dictType);
        }

        log.info("批量更新字典数据排序成功，数量: {}", ids.size());
    }

    /**
     * 检查指定字典类型下是否存在字典数据
     */
    public boolean hasDictDataByType(String dictType) {
        return lambdaQuery(SysDictData.class).eq(SysDictData::getDictType, dictType).exists();
    }

    /**
     * 更新字典数据表中的字典类型（当字典类型更改时调用）
     */
    @Transactional(readOnly = false)
    public void updateDictTypeInData(String oldDictType, String newDictType) {
        List<SysDictData> dictDataList = lambdaQuery(SysDictData.class)
                .eq(SysDictData::getDictType, oldDictType).list();

        for (SysDictData dictData : dictDataList) {
            dictData.setDictType(newDictType);
            baseDao.updatePO(dictData);
        }

        log.info("同步更新字典数据表中的字典类型: {} -> {}, 影响行数: {}",
                oldDictType, newDictType, dictDataList.size());
    }

    private boolean isDictValueExists(String dictType, String dictValue) {
        return lambdaQuery(SysDictData.class)
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getDictValue, dictValue)
                .exists();
    }

    private void clearDefaultStatus(String dictType) {
        List<SysDictData> dictDataList = lambdaQuery(SysDictData.class)
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getIsDefault, 1)
                .list();

        for (SysDictData dictData : dictDataList) {
            dictData.setIsDefault(0);
            baseDao.updatePO(dictData);
        }
    }

    /**
     * 根据字典类型和值获取字典标签（供 DictUtil 调用）
     * <p>
     * 走 getByDictType() 的完整链路：Redis 命中 → 直接过滤；Redis 未命中 → 查 DB → 写入 Redis → 过滤。
     */
    public String getDesc(String dictType, Object dictValue) {
        if (!StringUtils.hasText(dictType) || dictValue == null) return null;
        String value = String.valueOf(dictValue);
        List<DictDataDTO> list = getByDictType(dictType);
        return list.stream()
                .filter(item -> value.equals(item.getDictValue()))
                .map(DictDataDTO::getDictLabel)
                .findFirst()
                .orElse(null);
    }
}
