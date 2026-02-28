package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.framework.entity.SysDictType;
import com.seer.fitness.system.dto.DictTypeCreateRequest;
import com.seer.fitness.system.dto.DictTypeDTO;
import com.seer.fitness.system.dto.DictTypeQueryParam;
import com.seer.fitness.system.dto.DictTypeUpdateRequest;
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
 * 字典类型管理服务
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class DictTypeService extends BaseServiceImpl implements IDictTypeService {

    @Autowired
    private DictCacheService dictCacheService;

    @Autowired
    private IDictDataService dictDataService;

    /**
     * 分页查询字典类型
     */
    public Pager<DictTypeDTO> search(DictTypeQueryParam param, Pager pager) {
        Map<String, Object> queryMap = Maps.newHashMap();

        String sql = "SELECT id, dict_name, dict_type, dict_description, status, " +
                    "sort_order, remark, create_by, create_time, update_by, update_time " +
                    "FROM sys_dict_type";

        List<String> conditions = new ArrayList<>();

        if (StringUtils.hasText(param.getDictName())) {
            conditions.add("dict_name LIKE :dictName");
            queryMap.put("dictName", "%" + param.getDictName() + "%");
        }

        if (StringUtils.hasText(param.getDictType())) {
            conditions.add("dict_type LIKE :dictType");
            queryMap.put("dictType", "%" + param.getDictType() + "%");
        }

        if (param.getStatus() != null) {
            conditions.add("status = :status");
            queryMap.put("status", param.getStatus());
        }

        if (!conditions.isEmpty()) {
            sql += " WHERE " + String.join(" AND ", conditions);
        }

        sql += " ORDER BY sort_order ASC, create_time DESC";

        log.info("字典类型分页查询SQL: {}", sql);

        return baseDao.queryPageForSql(sql, queryMap, pager, DictTypeDTO.class);
    }

    /**
     * 获取所有字典类型列表（不分页，优先从缓存获取）
     */
    public List<DictTypeDTO> list() {
        // 先从缓存获取
        List<DictTypeDTO> cachedList = dictCacheService.getAllDictTypesFromCache();
        if (cachedList != null) {
            log.debug("从缓存获取字典类型列表成功，数量: {}", cachedList.size());
            return cachedList;
        }

        // 缓存未命中，从数据库查询
        String sql = "SELECT id, dict_name, dict_type, dict_description, status, " +
                    "sort_order, remark, create_by, create_time, update_by, update_time " +
                    "FROM sys_dict_type WHERE status = 1 ORDER BY sort_order ASC, create_time DESC";

        List<DictTypeDTO> list = baseDao.queryListForSql(sql, Maps.newHashMap(), DictTypeDTO.class);

        // 缓存结果
        if (list != null && !list.isEmpty()) {
            dictCacheService.cacheAllDictTypes(list);
        }

        log.info("从数据库获取字典类型列表成功，数量: {}", list.size());
        return list;
    }

    /**
     * 根据ID获取字典类型详情
     */
    public DictTypeDTO getById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException("字典类型ID不能为空");
        }

        String sql = "SELECT id, dict_name, dict_type, dict_description, status, " +
                    "sort_order, remark, create_by, create_time, update_by, update_time " +
                    "FROM sys_dict_type WHERE id = :id";

        Map<String, Object> params = Maps.newHashMap();
        params.put("id", Long.valueOf(id));

        DictTypeDTO dictType = baseDao.querySingleForSql(sql, params, DictTypeDTO.class);
        if (dictType == null) {
            throw new BusinessException("字典类型不存在");
        }

        return dictType;
    }

    /**
     * 根据字典类型获取详情（优先从缓存获取）
     */
    public DictTypeDTO getByDictType(String dictType) {
        if (!StringUtils.hasText(dictType)) {
            throw new BusinessException("字典类型不能为空");
        }

        // 先从缓存获取
        DictTypeDTO cached = dictCacheService.getDictTypeFromCache(dictType);
        if (cached != null) {
            log.debug("从缓存获取字典类型成功: {}", dictType);
            return cached;
        }

        // 缓存未命中，从数据库查询
        String sql = "SELECT id, dict_name, dict_type, dict_description, status, " +
                    "sort_order, remark, create_by, create_time, update_by, update_time " +
                    "FROM sys_dict_type WHERE dict_type = :dictType";

        Map<String, Object> params = Maps.newHashMap();
        params.put("dictType", dictType);

        DictTypeDTO result = baseDao.querySingleForSql(sql, params, DictTypeDTO.class);
        if (result == null) {
            throw new BusinessException("字典类型不存在: " + dictType);
        }

        // 缓存结果
        dictCacheService.cacheDictType(result);

        log.info("从数据库获取字典类型成功: {}", dictType);
        return result;
    }

    /**
     * 创建字典类型
     */
    @Transactional(readOnly = false)
    public void create(DictTypeCreateRequest request) {
        // 检查字典类型是否已存在
        if (isDictTypeExists(request.getDictType())) {
            throw new BusinessException("字典类型已存在: " + request.getDictType());
        }

        String currentUser = SecurityContextUtil.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

        SysDictType dictType = new SysDictType();
        dictType.setDictName(request.getDictName());
        dictType.setDictType(request.getDictType());
        dictType.setDictDescription(request.getDictDescription());
        dictType.setStatus(request.getStatus());
        dictType.setSortOrder(request.getSortOrder());
        dictType.setRemark(request.getRemark());
        dictType.setCreateBy(currentUser);
        dictType.setCreateTime(now);
        dictType.setUpdateBy(currentUser);
        dictType.setUpdateTime(now);
        dictType.setDeleteFlag(0);

        baseDao.insertPO(dictType, true);

        // 清除相关缓存
        dictCacheService.clearAllDictCache();

        log.info("创建字典类型成功: dictType={}, dictName={}", request.getDictType(), request.getDictName());
    }

    /**
     * 更新字典类型
     */
    @Transactional(readOnly = false)
    public void update(DictTypeUpdateRequest request) {
        SysDictType existingDict = baseDao.queryById(request.getId(), SysDictType.class);
        if (existingDict == null) {
            throw new BusinessException("字典类型不存在");
        }

        // 如果修改了字典类型，检查是否重复
        if (!existingDict.getDictType().equals(request.getDictType()) &&
            isDictTypeExists(request.getDictType())) {
            throw new BusinessException("字典类型已存在: " + request.getDictType());
        }

        String currentUser = SecurityContextUtil.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

        existingDict.setDictName(request.getDictName());
        existingDict.setDictType(request.getDictType());
        existingDict.setDictDescription(request.getDictDescription());
        existingDict.setStatus(request.getStatus());
        existingDict.setSortOrder(request.getSortOrder());
        existingDict.setRemark(request.getRemark());
        existingDict.setUpdateBy(currentUser);
        existingDict.setUpdateTime(now);

        baseDao.updatePO(existingDict);

        // 如果修改了字典类型，需要同步更新字典数据表
        String oldDictType = existingDict.getDictType();
        if (!oldDictType.equals(request.getDictType())) {
            dictDataService.updateDictTypeInData(oldDictType, request.getDictType());
        }

        // 清除相关缓存
        dictCacheService.deleteDictTypeCache(oldDictType);
        dictCacheService.deleteDictDataCache(oldDictType);
        if (!oldDictType.equals(request.getDictType())) {
            dictCacheService.deleteDictDataCache(request.getDictType());
        }
        dictCacheService.clearAllDictCache();

        log.info("更新字典类型成功: id={}, dictType={}", request.getId(), request.getDictType());
    }

    /**
     * 删除字典类型
     */
    @Transactional(readOnly = false)
    public void delete(String... ids) {
        if (ids == null || ids.length == 0) {
            throw new BusinessException("删除的字典类型ID不能为空");
        }

        for (String id : ids) {
            if (!StringUtils.hasText(id)) {
                throw new BusinessException("字典类型ID不能为空");
            }

            Long dictTypeId = Long.valueOf(id);
            SysDictType dictType = baseDao.queryById(dictTypeId, SysDictType.class);
            if (dictType == null) {
                throw new BusinessException("字典类型不存在");
            }

            // 检查是否有字典数据引用
            if (dictDataService.hasDictDataByType(dictType.getDictType())) {
                throw new BusinessException("该字典类型下存在字典数据，无法删除: " + dictType.getDictName());
            }

            // 清除相关缓存
            dictCacheService.deleteDictTypeCache(dictType.getDictType());
            dictCacheService.deleteDictDataCache(dictType.getDictType());

            log.info("删除字典类型成功: id={}, dictType={}", dictTypeId, dictType.getDictType());
        }

        // 逻辑删除
        baseDao.delByIds(SysDictType.class, ids);

        // 清除所有字典类型缓存
        dictCacheService.clearAllDictCache();
    }

    /**
     * 刷新字典缓存
     */
    public void refreshCache(String dictType) {
        dictCacheService.refreshDictCache(dictType);
        log.info("刷新字典缓存完成: {}", dictType);
    }

    /**
     * 检查字典类型是否已存在
     */
    private boolean isDictTypeExists(String dictType) {
        String sql = "SELECT COUNT(*) FROM sys_dict_type WHERE dict_type = :dictType";
        Map<String, Object> params = Maps.newHashMap();
        params.put("dictType", dictType);

        Long count = baseDao.querySingleForSql(sql, params, Long.class);
        return count != null && count > 0;
    }
}