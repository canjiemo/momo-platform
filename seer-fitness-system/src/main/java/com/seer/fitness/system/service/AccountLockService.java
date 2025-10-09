package com.seer.fitness.system.service;

import com.seer.fitness.framework.model.AccountFailRecord;
import com.seer.fitness.framework.model.AccountLockInfo;
import com.seer.fitness.system.config.AccountLockConfig;
import com.seer.fitness.system.utils.LockMessageBuilder;
import com.seer.fitness.system.utils.LockTimeCalculator;
import com.seer.fitness.system.utils.RedisUtil;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 账户锁定服务
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class AccountLockService {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private AccountLockConfig lockConfig;

    @Autowired
    private LockTimeCalculator lockTimeCalculator;

    @Autowired
    private LockMessageBuilder messageBuilder;

    /**
     * Redis Key前缀
     */
    private static final String ACCOUNT_FAIL_KEY = "account:fail:";
    private static final String ACCOUNT_LOCK_KEY = "account:lock:";
    private static final String IP_FAIL_KEY = "ip:fail:";
    private static final String IP_LOCK_KEY = "ip:lock:";

    /**
     * 记录登录失败
     */
    public void recordFailedAttempt(String username, String ip) {
        if (!lockConfig.isEnabled()) {
            return;
        }

        // 检查是否在白名单中
        if (isInWhitelist(username, ip)) {
            return;
        }

        // 1. 检查账户是否已被锁定
        if (isAccountLocked(username)) {
            AccountLockInfo lockInfo = getAccountLockInfo(username);
            throw new BusinessException(getAccountLockMessage(lockInfo));
        }

        // 2. 检查IP是否被锁定
        if (lockConfig.getIpLock().isEnabled() && isIpLocked(ip)) {
            throw new BusinessException(messageBuilder.getIpLockMessage());
        }

        // 3. 记录账户失败次数
        recordAccountFailure(username);

        // 4. 记录IP失败次数 (如果启用)
        if (lockConfig.getIpLock().isEnabled()) {
            recordIpFailure(ip);
        }
    }

    /**
     * 检查是否在白名单中
     */
    private boolean isInWhitelist(String username, String ip) {
        return lockConfig.getWhitelist().getUsers().contains(username) ||
                lockConfig.getWhitelist().getIps().contains(ip);
    }

    /**
     * 记录账户失败次数并检查是否需要锁定
     */
    private void recordAccountFailure(String username) {
        String key = ACCOUNT_FAIL_KEY + username;

        // 获取当前失败记录
        AccountFailRecord record = redisUtil.get(key, AccountFailRecord.class);
        if (record == null) {
            record = new AccountFailRecord();
        }

        // 增加失败次数
        record.incrementAttempts();

        // 检查是否需要锁定
        if (record.getAttempts() >= lockConfig.getAttempts().getMaxFailCount()) {
            lockAccount(username, record.getAttempts());
        } else {
            // 保存失败记录，过期时间为自动重置时间
            long resetHours = lockConfig.getReset().getAutoResetHours();
            redisUtil.set(key, record, resetHours, TimeUnit.HOURS);

            String message = messageBuilder.buildFailMessage(record.getAttempts());
            throw new BusinessException(message);
        }
    }

    /**
     * 锁定账户
     */
    private void lockAccount(String username, int failAttempts) {
        long lockMinutes = lockTimeCalculator.calculateLockMinutes(failAttempts);

        AccountLockInfo lockInfo = new AccountLockInfo();
        lockInfo.setUsername(username);
        lockInfo.setLockTime(System.currentTimeMillis());
        lockInfo.setUnlockTime(System.currentTimeMillis() + lockMinutes * 60 * 1000);
        lockInfo.setFailAttempts(failAttempts);
        lockInfo.setLockReason("连续登录失败");

        String lockKey = ACCOUNT_LOCK_KEY + username;
        redisUtil.set(lockKey, lockInfo, lockMinutes, TimeUnit.MINUTES);

        // 清除失败记录
        redisUtil.delete(ACCOUNT_FAIL_KEY + username);

        log.warn("账户已被锁定: username={}, lockMinutes={}, failAttempts={}",
                username, lockMinutes, failAttempts);

        throw new BusinessException(getAccountLockMessage(lockInfo));
    }

    /**
     * 记录IP失败次数
     */
    private void recordIpFailure(String ip) {
        String key = IP_FAIL_KEY + ip;
        Long attempts = redisUtil.increment(key);

        // 设置过期时间
        if (attempts == 1) {
            redisUtil.expire(key, lockConfig.getIpLock().getRecordHours(), TimeUnit.HOURS);
        }

        // 检查是否需要锁定IP
        if (attempts >= lockConfig.getIpLock().getMaxAttempts()) {
            lockIp(ip);
        }
    }

    /**
     * 锁定IP
     */
    private void lockIp(String ip) {
        String lockKey = IP_LOCK_KEY + ip;
        long lockMinutes = lockConfig.getIpLock().getLockMinutes();

        redisUtil.set(lockKey, "locked", lockMinutes, TimeUnit.MINUTES);
        redisUtil.delete(IP_FAIL_KEY + ip);

        log.warn("IP已被锁定: ip={}, lockMinutes={}", ip, lockMinutes);
    }

    /**
     * 检查账户是否被锁定
     */
    public boolean isAccountLocked(String username) {
        String lockKey = ACCOUNT_LOCK_KEY + username;
        AccountLockInfo lockInfo = redisUtil.get(lockKey, AccountLockInfo.class);

        if (lockInfo != null && System.currentTimeMillis() < lockInfo.getUnlockTime()) {
            return true;
        }

        // 锁定时间已过，清除锁定记录
        if (lockInfo != null) {
            redisUtil.delete(lockKey);
        }

        return false;
    }

    /**
     * 检查IP是否被锁定
     */
    public boolean isIpLocked(String ip) {
        return redisUtil.hasKey(IP_LOCK_KEY + ip);
    }

    /**
     * 登录成功后的处理
     */
    public void onLoginSuccess(String username, String ip) {
        if (lockConfig.getReset().isOnSuccess()) {
            // 清除失败记录
            redisUtil.delete(ACCOUNT_FAIL_KEY + username);
            redisUtil.delete(IP_FAIL_KEY + ip);
            log.info("登录成功，已重置失败记录: username={}", username);
        }
    }

    /**
     * 手动解锁账户 (管理员功能)
     */
    public void unlockAccount(String username, String adminUser) {
        String lockKey = ACCOUNT_LOCK_KEY + username;
        redisUtil.delete(lockKey);
        redisUtil.delete(ACCOUNT_FAIL_KEY + username);

        log.info("账户已被管理员解锁: username={}, admin={}", username, adminUser);
    }

    /**
     * 获取账户锁定信息
     */
    public AccountLockInfo getAccountLockInfo(String username) {
        String lockKey = ACCOUNT_LOCK_KEY + username;
        return redisUtil.get(lockKey, AccountLockInfo.class);
    }

    /**
     * 获取账户锁定消息
     */
    private String getAccountLockMessage(AccountLockInfo lockInfo) {
        long remainingTime = lockInfo.getUnlockTime() - System.currentTimeMillis();
        long lockMinutes = remainingTime / (60 * 1000);
        return messageBuilder.buildLockMessage(lockMinutes, lockInfo.getUnlockTime());
    }
}