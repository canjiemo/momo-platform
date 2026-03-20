package io.github.canjiemo.momo.system.utils;

import io.github.canjiemo.momo.system.service.ISysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 系统配置工具类
 * 从 Redis（优先）或数据库读取 sys_config 配置值
 */
@Component
@Slf4j
public class ConfigUtil {

    private static ISysConfigService configService;

    @Autowired
    public void setConfigService(ISysConfigService configService) {
        ConfigUtil.configService = configService;
    }

    public static String getString(String key, String defaultValue) {
        try {
            String val = configService.getValue(key);
            return StringUtils.hasText(val) ? val : defaultValue;
        } catch (Exception e) {
            log.warn("读取配置失败: key={}, 使用默认值: {}", key, defaultValue, e);
            return defaultValue;
        }
    }

    public static int getInt(String key, int defaultValue) {
        try {
            String val = configService.getValue(key);
            return StringUtils.hasText(val) ? Integer.parseInt(val.trim()) : defaultValue;
        } catch (Exception e) {
            log.warn("读取配置失败: key={}, 使用默认值: {}", key, defaultValue, e);
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        try {
            String val = configService.getValue(key);
            return StringUtils.hasText(val) ? Boolean.parseBoolean(val.trim()) : defaultValue;
        } catch (Exception e) {
            log.warn("读取配置失败: key={}, 使用默认值: {}", key, defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * 读取逗号分隔的字符串配置，返回 List<String>（每项 trim）
     */
    public static List<String> getList(String key) {
        try {
            String val = configService.getValue(key);
            if (!StringUtils.hasText(val)) return Collections.emptyList();
            return Arrays.stream(val.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        } catch (Exception e) {
            log.warn("读取配置失败: key={}", key, e);
            return Collections.emptyList();
        }
    }
}
