package com.seer.fitness.framework.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 账户锁定信息
 *
 * @author seer-fitness
 */
@Data
public class AccountLockInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 锁定时间
     */
    private long lockTime;

    /**
     * 解锁时间
     */
    private long unlockTime;

    /**
     * 导致锁定的失败次数
     */
    private int failAttempts;

    /**
     * 锁定原因
     */
    private String lockReason;
}
