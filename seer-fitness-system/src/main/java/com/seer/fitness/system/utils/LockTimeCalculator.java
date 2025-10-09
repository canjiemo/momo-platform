package com.seer.fitness.system.utils;

import com.seer.fitness.system.config.AccountLockConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 锁定时间计算器
 *
 * @author seer-fitness
 */
@Component
public class LockTimeCalculator {

    @Autowired
    private AccountLockConfig config;

    /**
     * 根据配置计算锁定时间
     */
    public long calculateLockMinutes(int failAttempts) {
        AccountLockConfig.LockTime lockTimeConfig = config.getLockTime();

        switch (lockTimeConfig.getStrategy()) {
            case FIXED:
                return lockTimeConfig.getFixed().getLockMinutes();

            case PROGRESSIVE:
                return calculateProgressiveLockTime(failAttempts, lockTimeConfig.getProgressive());

            case CUSTOM:
                return calculateCustomLockTime(failAttempts, lockTimeConfig.getCustom());

            default:
                return lockTimeConfig.getFixed().getLockMinutes();
        }
    }

    /**
     * 渐进式锁定时间计算
     */
    private long calculateProgressiveLockTime(int failAttempts, AccountLockConfig.LockTime.Progressive config) {
        int baseMinutes = config.getBaseMinutes();
        double multiplier = config.getMultiplier();
        int maxMinutes = config.getMaxMinutes();

        int overAttempts = failAttempts - this.config.getAttempts().getMaxFailCount();
        long lockMinutes = (long) (baseMinutes * Math.pow(multiplier, overAttempts));

        return Math.min(lockMinutes, maxMinutes);
    }

    /**
     * 自定义阶梯锁定时间计算
     */
    private long calculateCustomLockTime(int failAttempts, AccountLockConfig.LockTime.Custom config) {
        Map<Integer, Integer> steps = config.getSteps();

        // 找到对应的锁定时间，如果没有精确匹配则找最大的小于等于失败次数的配置
        int lockMinutes = 0;
        for (Map.Entry<Integer, Integer> entry : steps.entrySet()) {
            if (failAttempts >= entry.getKey()) {
                lockMinutes = Math.max(lockMinutes, entry.getValue());
            }
        }

        return lockMinutes > 0 ? lockMinutes : steps.values().stream().min(Integer::compareTo).orElse(30);
    }
}