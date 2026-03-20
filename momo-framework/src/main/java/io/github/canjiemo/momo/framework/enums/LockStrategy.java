package io.github.canjiemo.momo.framework.enums;

/**
 * 账户锁定策略枚举
 *
 * @author canjiemo@gmail.com
 */
public enum LockStrategy {

    /**
     * 固定时间锁定
     */
    FIXED,

    /**
     * 渐进式锁定（时间递增）
     */
    PROGRESSIVE,

    /**
     * 自定义阶梯锁定
     */
    CUSTOM
}
