package com.seer.fitness.system.service;

import com.seer.fitness.framework.dto.UserCacheInfo;
import com.seer.fitness.system.dto.LoginRequest;
import com.seer.fitness.system.dto.LoginResponse;

import java.util.Map;

public interface IAuthService {

    LoginResponse login(LoginRequest request, String ip);

    Map<String, Object> getPasswordPolicyConfig();

    UserCacheInfo getCurrentUser(String token);

    void clearUserCache(String token);

    boolean hasPermission(UserCacheInfo user, String permission);

    boolean hasRole(UserCacheInfo user, String role);
}
