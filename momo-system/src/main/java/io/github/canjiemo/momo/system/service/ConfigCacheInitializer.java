package io.github.canjiemo.momo.system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 系统启动后全量预热配置缓存
 */
@Component
@Slf4j
public class ConfigCacheInitializer {

    @Autowired
    private ISysConfigService sysConfigService;

    @EventListener(ApplicationReadyEvent.class)
    public void initConfigCache() {
        log.info("系统启动，开始初始化配置缓存...");
        try {
            sysConfigService.refreshCache();
            log.info("配置缓存初始化完成");
        } catch (Exception e) {
            log.error("配置缓存初始化失败，将按需懒加载", e);
        }
    }
}
