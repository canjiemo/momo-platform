package com.seer.fitness.framework.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 账户失败记录
 *
 * @author seer-fitness
 */
@Data
public class AccountFailRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 失败次数
     */
    private int attempts = 0;

    /**
     * 首次失败时间
     */
    private long firstFailTime;

    /**
     * 最后失败时间
     */
    private long lastFailTime;

    /**
     * 增加失败次数
     */
    public void incrementAttempts() {
        this.attempts++;
        long now = System.currentTimeMillis();
        if (this.firstFailTime == 0) {
            this.firstFailTime = now;
        }
        this.lastFailTime = now;
    }
}
