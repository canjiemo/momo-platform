package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.framework.model.AccountLockInfo;

public interface IAccountLockService {

    void recordFailedAttempt(String username, String ip);

    boolean isAccountLocked(String username);

    boolean isIpLocked(String ip);

    void onLoginSuccess(String username, String ip);

    void unlockAccount(String username, String adminUser);

    AccountLockInfo getAccountLockInfo(String username);
}
