package com.seer.fitness.system.constants;

public final class ConfigKeys {
    private ConfigKeys() {}

    public static final String CACHE_PREFIX = "config:";

    // 验证码
    public static final String CAPTCHA_ENABLED        = "security.captcha.enabled";
    public static final String CAPTCHA_EXPIRE_SECONDS = "security.captcha.expire-seconds";
    public static final String CAPTCHA_LENGTH         = "security.captcha.length";
    public static final String CAPTCHA_TYPE           = "security.captcha.type";

    // 密码策略
    public static final String PASSWORD_INITIAL           = "security.password.initial-password";
    public static final String PASSWORD_MIN_LENGTH        = "security.password.policy.min-length";
    public static final String PASSWORD_MAX_LENGTH        = "security.password.policy.max-length";
    public static final String PASSWORD_REQUIRE_LOWERCASE = "security.password.policy.require-lowercase";
    public static final String PASSWORD_REQUIRE_UPPERCASE = "security.password.policy.require-uppercase";
    public static final String PASSWORD_REQUIRE_DIGIT     = "security.password.policy.require-digit";
    public static final String PASSWORD_REQUIRE_SPECIAL   = "security.password.policy.require-special";

    // 账户锁定
    public static final String LOCK_ENABLED          = "security.account-lock.enabled";
    public static final String LOCK_MAX_FAIL_COUNT   = "security.account-lock.attempts.max-fail-count";
    public static final String LOCK_AUTO_RESET_HOURS = "security.account-lock.attempts.auto-reset-hours";
    public static final String LOCK_BASE_MINUTES     = "security.account-lock.lock-time.base-minutes";
    public static final String LOCK_IP_ENABLED       = "security.account-lock.ip-lock.enabled";
    public static final String LOCK_IP_MAX_ATTEMPTS  = "security.account-lock.ip-lock.max-attempts";
    public static final String LOCK_IP_LOCK_MINUTES  = "security.account-lock.ip-lock.lock-minutes";
    public static final String LOCK_IP_RECORD_HOURS  = "security.account-lock.ip-lock.record-hours";
    public static final String LOCK_RESET_ON_SUCCESS = "security.account-lock.reset.on-success";
    public static final String LOCK_WHITELIST_USERS  = "security.account-lock.whitelist.users";
    public static final String LOCK_WHITELIST_IPS    = "security.account-lock.whitelist.ips";

    // 定时任务
    public static final String SCHEDULER_POOL_SIZE = "scheduler.pool-size";
}
