package com.seer.fitness.framework.config;

import com.seer.fitness.framework.enums.LockStrategy;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 账户锁定配置类
 *
 * @author seer-fitness
 */
@ConfigurationProperties(prefix = "security.account-lock")
@Data
@Component
@Validated
public class AccountLockConfig {

    /**
     * 是否启用账户锁定
     */
    private boolean enabled = true;

    /**
     * 失败次数配置
     */
    private Attempts attempts = new Attempts();

    /**
     * 锁定时间配置
     */
    private LockTime lockTime = new LockTime();

    /**
     * 重置策略配置
     */
    private Reset reset = new Reset();

    /**
     * IP锁定配置
     */
    private IpLock ipLock = new IpLock();

    /**
     * 白名单配置
     */
    private Whitelist whitelist = new Whitelist();

    /**
     * 提示信息配置
     */
    private Messages messages = new Messages();

    @Data
    public static class Attempts {
        @Min(1)
        private int maxFailCount = 5;

        @Min(1)
        private int autoResetHours = 24;
    }

    @Data
    public static class LockTime {
        /**
         * 锁定策略：FIXED, PROGRESSIVE, CUSTOM
         */
        private LockStrategy strategy = LockStrategy.PROGRESSIVE;

        private Fixed fixed = new Fixed();
        private Progressive progressive = new Progressive();
        private Custom custom = new Custom();

        @Data
        public static class Fixed {
            @Min(1)
            private int lockMinutes = 30;
        }

        @Data
        public static class Progressive {
            @Min(1)
            private int baseMinutes = 30;

            @DecimalMin("1.0")
            private double multiplier = 2.0;

            @Min(1)
            private int maxMinutes = 1440;
        }

        @Data
        public static class Custom {
            /**
             * 失败次数到锁定分钟数的映射
             * Key: 失败次数, Value: 锁定分钟数
             */
            private Map<Integer, Integer> steps = new HashMap<>();
        }
    }

    @Data
    public static class Reset {
        private boolean onSuccess = true;
        private boolean timeBased = true;
        @Min(1)
        private int autoResetHours = 24;
    }

    @Data
    public static class IpLock {
        private boolean enabled = true;

        @Min(1)
        private int maxAttempts = 20;

        @Min(1)
        private int lockMinutes = 60;

        @Min(1)
        private int recordHours = 2;
    }

    @Data
    public static class Whitelist {
        private List<String> users = new ArrayList<>();
        private List<String> ips = new ArrayList<>();
    }

    @Data
    public static class Messages {
        private String failTemplate = "用户名或密码错误，还有{remaining}次机会";
        private String lockTemplate = "账户已被锁定{minutes}分钟，解锁时间：{unlock-time}";
        private String ipLockMessage = "IP地址访问频繁，已被临时锁定，请稍后再试";
    }
}
