package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.framework.entity.SysDictData;
import com.seer.fitness.system.dto.DictDataCreateRequest;
import com.seer.fitness.system.dto.DictDataDTO;
import com.seer.fitness.system.dto.DictDataQueryParam;
import com.seer.fitness.system.dto.DictDataUpdateRequest;
import com.seer.fitness.system.util.SecurityContextUtil;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 字典数据管理服务
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class DictDataService extends BaseServiceImpl implements IDictDataService {

    @Autowired
    private DictCacheService dictCacheService;

    /**
     * 分页查询字典数据
     */
    public Pager<DictDataDTO> search(DictDataQueryParam param, Pager pager) {
        Map<String, Object> queryMap = Maps.newHashMap();

        String sql = "SELECT id, dict_type, dict_label, dict_value, dict_description, " +
                    "css_class, list_class, is_default, status, sort_order, remark, " +
                    "create_by, create_time, update_by, update_time " +
                    "FROM sys_dict_data";

        List<String> conditions = new ArrayList<>();

        if (StringUtils.hasText(param.getDictType())) {
            conditions.add("dict_type = :dictType");
            queryMap.put("dictType", param.getDictType());
        }

        if (StringUtils.hasText(param.getDictLabel())) {
            conditions.add("dict_label LIKE :dictLabel");
            queryMap.put("dictLabel", "%" + param.getDictLabel() + "%");
        }

        if (StringUtils.hasText(param.getDictValue())) {
            conditions.add("dict_value LIKE :dictValue");
            queryMap.put("dictValue", "%" + param.getDictValue() + "%");
        }

        if (param.getStatus() != null) {
            conditions.add("status = :status");
            queryMap.put("status", param.getStatus());
        }

        if (!conditions.isEmpty()) {
            sql += " WHERE " + String.join(" AND ", conditions);
        }

        sql += " ORDER BY sort_order ASC, create_time DESC";

        log.info("字典数据分页查询SQL: {}", sql);

        return baseDao.queryPageForSqlWithDeleteCondition(sql, queryMap, pager, DictDataDTO.class);
    }

    /**
     * 根据字典类型获取字典数据列表（优先从缓存获取）
     */
    public List<DictDataDTO> getByDictType(String dictType) {
        if (!StringUtils.hasText(dictType)) {
            throw new BusinessException("字典类型不能为空");
        }

        // 先从缓存获取
        List<DictDataDTO> cachedList = dictCacheService.getDictDataFromCache(dictType);
        if (cachedList != null) {
            log.debug("从缓存获取字典数据成功: {}, 数量: {}", dictType, cachedList.size());
            return cachedList;
        }

        // 缓存未命中，从数据库查询
        String sql = "SELECT id, dict_type, dict_label, dict_value, dict_description, " +
                    "css_class, list_class, is_default, status, sort_order, remark, " +
                    "create_by, create_time, update_by, update_time " +
                    "FROM sys_dict_data WHERE dict_type = :dictType AND status = 1 " +
                    "ORDER BY sort_order ASC, create_time DESC";

        Map<String, Object> params = Maps.newHashMap();
        params.put("dictType", dictType);

        List<DictDataDTO> list = baseDao.queryListForSqlWithDeleteCondition(sql, params, DictDataDTO.class);

        // 缓存结果
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

        String sql = "SELECT id, dict_type, dict_label, dict_value, dict_description, " +
                    "css_class, list_class, is_default, status, sort_order, remark, " +
                    "create_by, create_time, update_by, update_time " +
                    "FROM sys_dict_data WHERE id = :id";

        Map<String, Object> params = Maps.newHashMap();
        params.put("id", Long.valueOf(id));

        DictDataDTO dictData = baseDao.querySingleForSqlWithDeleteCondition(sql, params, DictDataDTO.class);
        if (dictData == null) {
            throw new BusinessException("字典数据不存在");
        }

        return dictData;
    }

    /**
     * 根据字典类型和字典值获取字典标签
     */
    public String getDictLabel(String dictType, String dictValue) {
        if (!StringUtils.hasText(dictType) || !StringUtils.hasText(dictValue)) {
            return null;
        }

        // 先从缓存获取
        String cachedLabel = dictCacheService.getDictLabelByValue(dictType, dictValue);
        if (cachedLabel != null) {
            log.debug("从缓存获取字典标签成功: {}={}", dictType + ":" + dictValue, cachedLabel);
            return cachedLabel;
        }

        // 缓存未命中，从数据库查询
        String sql = "SELECT dict_label FROM sys_dict_data " +
                    "WHERE dict_type = :dictType AND dict_value = :dictValue AND status = 1";

        Map<String, Object> params = Maps.newHashMap();
        params.put("dictType", dictType);
        params.put("dictValue", dictValue);

        String label = baseDao.querySingleForSqlWithDeleteCondition(sql, params, String.class);
        log.info("从数据库获取字典标签: {}={}", dictType + ":" + dictValue, label);

        return label;
    }

    /**
     * 创建字典数据
     */
    @Transactional(readOnly = false)
    public void create(DictDataCreateRequest request) {
        // 检查字典值是否已存在
        if (isDictValueExists(request.getDictType(), request.getDictValue())) {
            throw new BusinessException("字典值已存在: " + request.getDictValue());
        }

        // 如果设置为默认值，需要将其他项的默认状态置为false
        if (request.getIsDefault()==1) {
            clearDefaultStatus(request.getDictType());
        }

        String currentUser = SecurityContextUtil.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

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
        dictData.setCreateBy(currentUser);
        dictData.setCreateTime(now);
        dictData.setUpdateBy(currentUser);
        dictData.setUpdateTime(now);
        dictData.setDeleteFlag(0);

        baseDao.insertPO(dictData, true);

        // 清除相关缓存
        dictCacheService.deleteDictDataCache(request.getDictType());

        log.info("创建字典数据成功: dictType={}, dictValue={}", request.getDictType(), request.getDictValue());
    }

    /**
     * 更新字典数据
     */
    @Transactional(readOnly = false)
    public void update(DictDataUpdateRequest request) {
        SysDictData existingData = baseDao.queryByIdWithDeleteCondition(request.getId(), SysDictData.class);
        if (existingData == null) {
            throw new BusinessException("字典数据不存在");
        }

        // 如果修改了字典值，检查是否重复
        if (!existingData.getDictValue().equals(request.getDictValue()) &&
            isDictValueExists(request.getDictType(), request.getDictValue())) {
            throw new BusinessException("字典值已存在: " + request.getDictValue());
        }

        // 如果设置为默认值，需要将其他项的默认状态置为false
        if (request.getIsDefault()==1 && existingData.getIsDefault()!=1) {
            clearDefaultStatus(request.getDictType());
        }

        String currentUser = SecurityContextUtil.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

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
        existingData.setUpdateBy(currentUser);
        existingData.setUpdateTime(now);

        baseDao.updatePO(existingData);

        // 清除相关缓存
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

            Long dictDataId = Long.valueOf(id);
            SysDictData dictData = baseDao.queryByIdWithDeleteCondition(dictDataId, SysDictData.class);
            if (dictData == null) {
                throw new BusinessException("字典数据不存在");
            }

            // 清除相关缓存
            dictCacheService.deleteDictDataCache(dictData.getDictType());

            log.info("删除字典数据成功: id={}, dictType={}, dictValue={}",
                    dictDataId, dictData.getDictType(), dictData.getDictValue());
        }

        // 逻辑删除
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

        String currentUser = SecurityContextUtil.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

        String dictType = null;

        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            Integer sortOrder = sortOrders.get(i);

            SysDictData dictData = baseDao.queryByIdWithDeleteCondition(Long.valueOf(id), SysDictData.class);
            if (dictData == null) {
                continue;
            }

            if (dictType == null) {
                dictType = dictData.getDictType();
            }

            dictData.setSortOrder(sortOrder);
            dictData.setUpdateBy(currentUser);
            dictData.setUpdateTime(now);

            baseDao.updatePO(dictData);
        }

        // 清除相关缓存
        if (dictType != null) {
            dictCacheService.deleteDictDataCache(dictType);
        }

        log.info("批量更新字典数据排序成功，数量: {}", ids.size());
    }

    /**
     * 检查指定字典类型下是否存在字典数据
     */
    public boolean hasDictDataByType(String dictType) {
        String sql = "SELECT COUNT(*) FROM sys_dict_data WHERE dict_type = :dictType";
        Map<String, Object> params = Maps.newHashMap();
        params.put("dictType", dictType);

        Long count = baseDao.querySingleForSqlWithDeleteCondition(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * 更新字典数据表中的字典类型（当字典类型更改时调用）
     */
    @Transactional(readOnly = false)
    public void updateDictTypeInData(String oldDictType, String newDictType) {
        String currentUser = SecurityContextUtil.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

        String sql = "SELECT * FROM sys_dict_data WHERE dict_type = :dictType";
        Map<String, Object> params = Maps.newHashMap();
        params.put("dictType", oldDictType);

        List<SysDictData> dictDataList = baseDao.queryListForSqlWithDeleteCondition(sql, params, SysDictData.class);

        for (SysDictData dictData : dictDataList) {
            dictData.setDictType(newDictType);
            dictData.setUpdateBy(currentUser);
            dictData.setUpdateTime(now);
            baseDao.updatePO(dictData);
        }

        log.info("同步更新字典数据表中的字典类型: {} -> {}, 影响行数: {}",
                oldDictType, newDictType, dictDataList.size());
    }

    /**
     * 检查字典值是否已存在
     */
    private boolean isDictValueExists(String dictType, String dictValue) {
        String sql = "SELECT COUNT(*) FROM sys_dict_data " +
                    "WHERE dict_type = :dictType AND dict_value = :dictValue";
        Map<String, Object> params = Maps.newHashMap();
        params.put("dictType", dictType);
        params.put("dictValue", dictValue);

        Long count = baseDao.querySingleForSqlWithDeleteCondition(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * 清除指定字典类型下所有数据的默认状态
     */
    private void clearDefaultStatus(String dictType) {
        String currentUser = SecurityContextUtil.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

        String sql = "SELECT * FROM sys_dict_data WHERE dict_type = :dictType AND is_default = 1";
        Map<String, Object> params = Maps.newHashMap();
        params.put("dictType", dictType);

        List<SysDictData> dictDataList = baseDao.queryListForSqlWithDeleteCondition(sql, params, SysDictData.class);

        for (SysDictData dictData : dictDataList) {
            dictData.setIsDefault(0);
            dictData.setUpdateBy(currentUser);
            dictData.setUpdateTime(now);
            baseDao.updatePO(dictData);
        }
    }

    /**
     * 根据字典类型和值获取字典描述
     */
    public String getDesc(String dictType, Object dictValue) {
        return dictCacheService.getDictLabelByValue(dictType, String.valueOf(dictValue));
    }
}