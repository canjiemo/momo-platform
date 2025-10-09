package com.seer.fitness.system.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.seer.fitness.system.constants.DictConstants;
import com.seer.fitness.system.dto.DictDataDTO;
import com.seer.fitness.system.dto.DictTypeDTO;
import com.seer.fitness.system.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 字典缓存服务
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class DictCacheService {

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 本地缓存，存储字典值到标签的映射
     * 缓存格式：dictType:dictValue -> dictLabel
     */
    private Cache<String, String> localDictCache;

    /**
     * 初始化本地缓存
     */
    @PostConstruct
    public void initLocalCache() {
        this.localDictCache = Caffeine.newBuilder()
                .maximumSize(10000) // 最大缓存条目数
                .expireAfterWrite(5, TimeUnit.MINUTES) // 写入后5分钟过期
                .recordStats() // 启用统计功能
                .build();
        log.info("字典本地缓存初始化完成，最大缓存数: 10000, 过期时间: 5分钟");
    }

    /**
     * 获取所有字典类型(从缓存)
     */
    @SuppressWarnings("unchecked")
    public List<DictTypeDTO> getAllDictTypesFromCache() {
        try {
            return redisUtil.get(DictConstants.ALL_DICT_TYPES_CACHE_KEY, List.class);
        } catch (Exception e) {
            log.error("获取字典类型缓存失败", e);
            return null;
        }
    }

    /**
     * 缓存所有字典类型
     */
    public void cacheAllDictTypes(List<DictTypeDTO> dictTypes) {
        try {
            redisUtil.set(DictConstants.ALL_DICT_TYPES_CACHE_KEY, dictTypes,
                         DictConstants.DICT_TYPE_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("缓存所有字典类型成功，数量: {}", dictTypes.size());
        } catch (Exception e) {
            log.error("缓存字典类型失败", e);
        }
    }

    /**
     * 获取字典类型(从缓存)
     */
    public DictTypeDTO getDictTypeFromCache(String dictType) {
        if (!StringUtils.hasText(dictType)) {
            return null;
        }

        try {
            String cacheKey = DictConstants.DICT_TYPE_CACHE_KEY + dictType;
            return redisUtil.get(cacheKey, DictTypeDTO.class);
        } catch (Exception e) {
            log.error("获取字典类型缓存失败: {}", dictType, e);
            return null;
        }
    }

    /**
     * 缓存字典类型
     */
    public void cacheDictType(DictTypeDTO dictTypeDTO) {
        if (dictTypeDTO == null || !StringUtils.hasText(dictTypeDTO.getDictType())) {
            return;
        }

        try {
            String cacheKey = DictConstants.DICT_TYPE_CACHE_KEY + dictTypeDTO.getDictType();
            redisUtil.set(cacheKey, dictTypeDTO,
                         DictConstants.DICT_TYPE_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("缓存字典类型成功: {}", dictTypeDTO.getDictType());
        } catch (Exception e) {
            log.error("缓存字典类型失败: {}", dictTypeDTO.getDictType(), e);
        }
    }

    /**
     * 获取字典数据(从缓存)
     */
    @SuppressWarnings("unchecked")
    public List<DictDataDTO> getDictDataFromCache(String dictType) {
        if (!StringUtils.hasText(dictType)) {
            return null;
        }

        try {
            String cacheKey = DictConstants.DICT_DATA_CACHE_KEY + dictType;
            return redisUtil.get(cacheKey, List.class);
        } catch (Exception e) {
            log.error("获取字典数据缓存失败: {}", dictType, e);
            return null;
        }
    }

    /**
     * 缓存字典数据
     */
    public void cacheDictData(String dictType, List<DictDataDTO> dictDataList) {
        if (!StringUtils.hasText(dictType)) {
            return;
        }

        try {
            String cacheKey = DictConstants.DICT_DATA_CACHE_KEY + dictType;
            redisUtil.set(cacheKey, dictDataList,
                         DictConstants.DICT_DATA_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("缓存字典数据成功: {}, 数量: {}", dictType, dictDataList.size());
        } catch (Exception e) {
            log.error("缓存字典数据失败: {}", dictType, e);
        }
    }

    /**
     * 根据字典值获取字典标签(多级缓存策略)
     * 查询顺序：本地缓存 -> Redis缓存 -> 数据库
     */
    public String getDictLabelByValue(String dictType, String dictValue) {
        if (!StringUtils.hasText(dictType) || !StringUtils.hasText(dictValue)) {
            return null;
        }

        // 生成缓存键
        String cacheKey = dictType + ":" + dictValue;

        try {
            // 1. 先查本地 Caffeine 缓存
            String cachedLabel = localDictCache.getIfPresent(cacheKey);
            if (cachedLabel != null) {
                log.debug("从本地缓存获取字典标签: {} -> {}", cacheKey, cachedLabel);
                return cachedLabel;
            }

            // 2. 本地缓存未命中，查询 Redis 缓存
            List<DictDataDTO> dictDataList = getDictDataFromCache(dictType);
            if (dictDataList != null) {
                String dictLabel = dictDataList.stream()
                        .filter(item -> dictValue.equals(item.getDictValue()) &&
                                       Boolean.TRUE.equals(item.getStatus() == 1))
                        .map(DictDataDTO::getDictLabel)
                        .findFirst()
                        .orElse(null);

                if (dictLabel != null) {
                    // 将查询结果存入本地缓存
                    localDictCache.put(cacheKey, dictLabel);
                    log.debug("从Redis获取字典标签并缓存到本地: {} -> {}", cacheKey, dictLabel);
                    return dictLabel;
                }
            }

            log.debug("字典标签未找到: {}", cacheKey);
            return null;

        } catch (Exception e) {
            log.error("获取字典标签失败: {}", cacheKey, e);
            return null;
        }
    }

    /**
     * 删除字典类型缓存
     */
    public void deleteDictTypeCache(String dictType) {
        if (!StringUtils.hasText(dictType)) {
            return;
        }

        try {
            String cacheKey = DictConstants.DICT_TYPE_CACHE_KEY + dictType;
            redisUtil.delete(cacheKey);

            // 同时删除所有字典类型缓存
            redisUtil.delete(DictConstants.ALL_DICT_TYPES_CACHE_KEY);

            log.debug("删除字典类型缓存成功: {}", dictType);
        } catch (Exception e) {
            log.error("删除字典类型缓存失败: {}", dictType, e);
        }
    }

    /**
     * 删除字典数据缓存
     */
    public void deleteDictDataCache(String dictType) {
        if (!StringUtils.hasText(dictType)) {
            return;
        }

        try {
            // 删除 Redis 缓存
            String cacheKey = DictConstants.DICT_DATA_CACHE_KEY + dictType;
            redisUtil.delete(cacheKey);

            // 删除相关的本地缓存
            clearLocalDictCacheByType(dictType);

            log.debug("删除字典数据缓存成功: {}", dictType);
        } catch (Exception e) {
            log.error("删除字典数据缓存失败: {}", dictType, e);
        }
    }

    /**
     * 刷新字典缓存
     */
    public void refreshDictCache(String dictType) {
        deleteDictTypeCache(dictType);
        deleteDictDataCache(dictType);
        log.info("刷新字典缓存完成: {}", dictType);
    }

    /**
     * 清空所有字典缓存
     */
    public void clearAllDictCache() {
        try {
            // 删除所有字典类型缓存
            redisUtil.delete(DictConstants.ALL_DICT_TYPES_CACHE_KEY);

            // 清空所有本地缓存
            localDictCache.invalidateAll();

            log.info("清空所有字典缓存完成");
        } catch (Exception e) {
            log.error("清空字典缓存失败", e);
        }
    }

    /**
     * 根据字典类型清理本地缓存
     * 清理所有以 dictType: 开头的缓存项
     */
    private void clearLocalDictCacheByType(String dictType) {
        if (localDictCache == null || !StringUtils.hasText(dictType)) {
            return;
        }

        try {
            String keyPrefix = dictType + ":";
            localDictCache.asMap().keySet().removeIf(key -> key.startsWith(keyPrefix));
            log.debug("清理本地字典缓存: {}", keyPrefix);
        } catch (Exception e) {
            log.error("清理本地字典缓存失败: {}", dictType, e);
        }
    }

    /**
     * 获取本地缓存统计信息
     */
    public String getLocalCacheStats() {
        if (localDictCache == null) {
            return "本地缓存未初始化";
        }

        return String.format("本地缓存统计 - 大小: %d, 命中率: %.2f%%",
                localDictCache.estimatedSize(),
                localDictCache.stats().hitRate() * 100);
    }
}