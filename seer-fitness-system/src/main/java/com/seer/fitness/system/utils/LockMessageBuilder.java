package com.seer.fitness.system.utils;

import com.seer.fitness.system.config.AccountLockConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 锁定消息构建器
 *
 * @author seer-fitness
 */
@Component
public class LockMessageBuilder {

    @Autowired
    private AccountLockConfig config;

    /**
     * 构建失败提示消息
     */
    public String buildFailMessage(int currentAttempts) {
        int maxAttempts = config.getAttempts().getMaxFailCount();
        int remaining = maxAttempts - currentAttempts;

        return config.getMessages().getFailTemplate()
                .replace("{remaining}", String.valueOf(remaining))
                .replace("{max}", String.valueOf(maxAttempts));
    }

    /**
     * 构建锁定提示消息
     */
    public String buildLockMessage(long lockMinutes, long unlockTime) {
        String unlockTimeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(unlockTime));

        return config.getMessages().getLockTemplate()
                .replace("{minutes}", String.valueOf(lockMinutes))
                .replace("{unlock-time}", unlockTimeStr);
    }

    /**
     * 获取IP锁定消息
     */
    public String getIpLockMessage() {
        return config.getMessages().getIpLockMessage();
    }
}