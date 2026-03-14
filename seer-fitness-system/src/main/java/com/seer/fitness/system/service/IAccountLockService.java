package com.seer.fitness.system.service;

import com.seer.fitness.framework.model.AccountLockInfo;

public interface IAccountLockService {

    void recordFailedAttempt(String username, String ip);

    boolean isAccountLocked(String username);

    boolean isIpLocked(String ip);

    void onLoginSuccess(String username, String ip);

    void unlockAccount(String username, String adminUser);

    AccountLockInfo getAccountLockInfo(String username);
}
