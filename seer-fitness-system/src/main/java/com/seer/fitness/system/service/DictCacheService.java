package com.seer.fitness.system.service;

import com.seer.fitness.system.constants.DictConstants;
import com.seer.fitness.system.dto.DictDataDTO;
import com.seer.fitness.system.dto.DictTypeDTO;
import com.seer.fitness.framework.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 字典缓存服务
 * <p>
 * 使用 Redis 持久化缓存（无 TTL），由 DictCacheInitializer 在系统启动时完成全量加载。
 * 增删改操作后调用对应的 delete/clear 方法使缓存失效，下次读取由各 Service 懒加载回填。
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class DictCacheService implements IDictCacheService {

    @Autowired
    private RedisUtil redisUtil;

    // ─── 字典类型 ─────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<DictTypeDTO> getAllDictTypesFromCache() {
        try {
            return redisUtil.get(DictConstants.ALL_DICT_TYPES_CACHE_KEY, List.class);
        } catch (Exception e) {
            log.error("获取字典类型列表缓存失败", e);
            return null;
        }
    }

    public void cacheAllDictTypes(List<DictTypeDTO> dictTypes) {
        try {
            redisUtil.set(DictConstants.ALL_DICT_TYPES_CACHE_KEY, dictTypes);
            log.debug("缓存字典类型列表成功，数量: {}", dictTypes.size());
        } catch (Exception e) {
            log.error("缓存字典类型列表失败", e);
        }
    }

    public DictTypeDTO getDictTypeFromCache(String dictType) {
        if (!StringUtils.hasText(dictType)) return null;
        try {
            return redisUtil.get(DictConstants.DICT_TYPE_CACHE_KEY + dictType, DictTypeDTO.class);
        } catch (Exception e) {
            log.error("获取字典类型缓存失败: {}", dictType, e);
            return null;
        }
    }

    public void cacheDictType(DictTypeDTO dictTypeDTO) {
        if (dictTypeDTO == null || !StringUtils.hasText(dictTypeDTO.getDictType())) return;
        try {
            redisUtil.set(DictConstants.DICT_TYPE_CACHE_KEY + dictTypeDTO.getDictType(), dictTypeDTO);
            log.debug("缓存字典类型成功: {}", dictTypeDTO.getDictType());
        } catch (Exception e) {
            log.error("缓存字典类型失败: {}", dictTypeDTO.getDictType(), e);
        }
    }

    public void deleteDictTypeCache(String dictType) {
        if (!StringUtils.hasText(dictType)) return;
        try {
            redisUtil.delete(DictConstants.DICT_TYPE_CACHE_KEY + dictType);
            redisUtil.delete(DictConstants.ALL_DICT_TYPES_CACHE_KEY);
            log.debug("删除字典类型缓存成功: {}", dictType);
        } catch (Exception e) {
            log.error("删除字典类型缓存失败: {}", dictType, e);
        }
    }

    // ─── 字典数据 ─────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<DictDataDTO> getDictDataFromCache(String dictType) {
        if (!StringUtils.hasText(dictType)) return null;
        try {
            return redisUtil.get(DictConstants.DICT_DATA_CACHE_KEY + dictType, List.class);
        } catch (Exception e) {
            log.error("获取字典数据缓存失败: {}", dictType, e);
            return null;
        }
    }

    public void cacheDictData(String dictType, List<DictDataDTO> dictDataList) {
        if (!StringUtils.hasText(dictType)) return;
        try {
            redisUtil.set(DictConstants.DICT_DATA_CACHE_KEY + dictType, dictDataList);
            log.debug("缓存字典数据成功: {}, 数量: {}", dictType, dictDataList.size());
        } catch (Exception e) {
            log.error("缓存字典数据失败: {}", dictType, e);
        }
    }

    public void deleteDictDataCache(String dictType) {
        if (!StringUtils.hasText(dictType)) return;
        try {
            redisUtil.delete(DictConstants.DICT_DATA_CACHE_KEY + dictType);
            log.debug("删除字典数据缓存成功: {}", dictType);
        } catch (Exception e) {
            log.error("删除字典数据缓存失败: {}", dictType, e);
        }
    }

    // ─── 标签查询 ─────────────────────────────────────────────

    /**
     * 根据字典类型和字典值从 Redis 缓存中查找标签（dictLabel）
     */
    public String getDictLabelByValue(String dictType, String dictValue) {
        if (!StringUtils.hasText(dictType) || !StringUtils.hasText(dictValue)) return null;
        try {
            List<DictDataDTO> list = getDictDataFromCache(dictType);
            if (list == null) return null;
            return list.stream()
                    .filter(item -> dictValue.equals(item.getDictValue()) && item.getStatus() == 1)
                    .map(DictDataDTO::getDictLabel)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("获取字典标签失败: {}:{}", dictType, dictValue, e);
            return null;
        }
    }

    // ─── 全量清空 ─────────────────────────────────────────────

    /**
     * 清空所有字典缓存（删除 Redis 中所有 dict:* key）
     */
    public void clearAllDictCache() {
        try {
            long deleted = redisUtil.deleteByPattern(DictConstants.CACHE_KEY_PREFIX + "*");
            log.info("清空所有字典缓存完成，共删除 {} 个 key", deleted);
        } catch (Exception e) {
            log.error("清空字典缓存失败", e);
        }
    }
}
