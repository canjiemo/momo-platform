package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.DictTypeCreateRequest;
import com.seer.fitness.system.dto.DictTypeDTO;
import com.seer.fitness.system.dto.DictTypeQueryParam;
import com.seer.fitness.system.dto.DictTypeUpdateRequest;
import com.seer.fitness.system.entity.SysDictType;
import com.seer.fitness.system.utils.SecurityContextUtil;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

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
        return lambdaQuery(SysDictType.class, DictTypeDTO.class)
                .like(SysDictType::getDictName, param.getDictName())
                .like(SysDictType::getDictType, param.getDictType())
                .eq(SysDictType::getStatus, param.getStatus())
                .orderByAsc(SysDictType::getSortOrder)
                .orderByDesc(SysDictType::getCreateTime)
                .page(pager);
    }

    /**
     * 获取所有字典类型列表（不分页，优先从缓存获取）
     */
    public List<DictTypeDTO> list() {
        List<DictTypeDTO> cachedList = dictCacheService.getAllDictTypesFromCache();
        if (cachedList != null) {
            log.debug("从缓存获取字典类型列表成功，数量: {}", cachedList.size());
            return cachedList;
        }

        List<DictTypeDTO> list = lambdaQuery(SysDictType.class, DictTypeDTO.class)
                .eq(SysDictType::getStatus, 1)
                .orderByAsc(SysDictType::getSortOrder)
                .orderByDesc(SysDictType::getCreateTime)
                .list();

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

        DictTypeDTO dictType = lambdaQuery(SysDictType.class, DictTypeDTO.class)
                .eq(SysDictType::getId, Long.valueOf(id)).one();
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

        DictTypeDTO cached = dictCacheService.getDictTypeFromCache(dictType);
        if (cached != null) {
            log.debug("从缓存获取字典类型成功: {}", dictType);
            return cached;
        }

        DictTypeDTO result = lambdaQuery(SysDictType.class, DictTypeDTO.class)
                .eq(SysDictType::getDictType, dictType).one();
        if (result == null) {
            throw new BusinessException("字典类型不存在: " + dictType);
        }

        dictCacheService.cacheDictType(result);
        log.info("从数据库获取字典类型成功: {}", dictType);
        return result;
    }

    /**
     * 创建字典类型
     */
    @Transactional(readOnly = false)
    public void create(DictTypeCreateRequest request) {
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

        String oldDictType = existingDict.getDictType();
        if (!oldDictType.equals(request.getDictType())) {
            dictDataService.updateDictTypeInData(oldDictType, request.getDictType());
        }

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

            if (dictDataService.hasDictDataByType(dictType.getDictType())) {
                throw new BusinessException("该字典类型下存在字典数据，无法删除: " + dictType.getDictName());
            }

            dictCacheService.deleteDictTypeCache(dictType.getDictType());
            dictCacheService.deleteDictDataCache(dictType.getDictType());
            log.info("删除字典类型成功: id={}, dictType={}", dictTypeId, dictType.getDictType());
        }

        baseDao.delByIds(SysDictType.class, ids);
        dictCacheService.clearAllDictCache();
    }

    /**
     * 刷新字典缓存（删除缓存，下次读取时懒加载回填）
     * 如需主动重新加载，Controller 应调用 DictCacheInitializer.refreshDictType()
     */
    public void refreshCache(String dictType) {
        dictCacheService.deleteDictTypeCache(dictType);
        dictCacheService.deleteDictDataCache(dictType);
        log.info("刷新字典缓存完成: {}", dictType);
    }

    private boolean isDictTypeExists(String dictType) {
        return lambdaQuery(SysDictType.class).eq(SysDictType::getDictType, dictType).exists();
    }
}
