package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.DictTypeDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 字典缓存初始化器
 * <p>
 * 职责：
 * 1. 系统启动后（ApplicationReadyEvent）清空 Redis 中所有 dict:* key，并全量加载字典数据。
 * 2. 提供手动刷新接口（全量 / 单类型），供 Controller 调用。
 * <p>
 * 设计说明：
 * - IDictTypeService / IDictDataService 使用 @Lazy 注入，避免与 DictCacheService 形成循环依赖。
 * - 实际数据写入 Redis 由各 Service 内部的懒加载逻辑完成（cache miss → DB → 回填），
 *   此处只负责"清空 + 触发加载"。
 *
 * @author seer-fitness
 */
@Component
@Slf4j
public class DictCacheInitializer {

    @Autowired
    private DictCacheService dictCacheService;

    @Lazy
    @Autowired
    private IDictTypeService dictTypeService;

    @Lazy
    @Autowired
    private IDictDataService dictDataService;

    /**
     * 系统启动后自动执行：清空旧缓存，全量加载字典到 Redis
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initDictCache() {
        log.info("系统启动，开始初始化字典缓存...");
        try {
            dictCacheService.clearAllDictCache();
            loadAll();
            log.info("字典缓存初始化完成");
        } catch (Exception e) {
            log.error("字典缓存初始化失败，系统仍可正常运行（缓存将按需懒加载）", e);
        }
    }

    /**
     * 刷新全部字典缓存（清空 + 重新从 DB 加载）
     */
    public void refreshAllDictCache() {
        log.info("开始刷新全部字典缓存...");
        dictCacheService.clearAllDictCache();
        loadAll();
        log.info("全部字典缓存刷新完成");
    }

    /**
     * 刷新单个字典类型的缓存（删除 + 从 DB 重新加载）
     *
     * @param dictType 字典类型编码
     */
    public void refreshDictType(String dictType) {
        dictCacheService.deleteDictTypeCache(dictType);
        dictCacheService.deleteDictDataCache(dictType);
        // 主动触发 DB 重新加载并回填缓存
        try {
            dictTypeService.getByDictType(dictType);
        } catch (Exception ignore) {
            // 字典类型不存在时忽略异常
        }
        try {
            dictDataService.getByDictType(dictType);
        } catch (Exception ignore) {
            // 字典数据为空时忽略异常
        }
        log.info("刷新字典缓存完成: {}", dictType);
    }

    // ─── 私有方法 ─────────────────────────────────────────────

    private void loadAll() {
        try {
            List<DictTypeDTO> types = dictTypeService.list();
            log.info("正在加载 {} 个字典类型的数据...", types.size());
            for (DictTypeDTO type : types) {
                try {
                    dictDataService.getByDictType(type.getDictType());
                } catch (Exception e) {
                    log.warn("加载字典数据失败，跳过: {}", type.getDictType(), e);
                }
            }
        } catch (Exception e) {
            log.error("加载字典数据失败", e);
        }
    }
}
