package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.framework.dto.UserCacheInfo;
import io.github.canjiemo.momo.system.dto.LoginRequest;
import io.github.canjiemo.momo.system.dto.LoginResponse;

import java.util.Map;

public interface IAuthService {

    LoginResponse login(LoginRequest request, String ip);

    Map<String, Object> getPasswordPolicyConfig();

    UserCacheInfo getCurrentUser(String token);

    void clearUserCache(String token);

    boolean hasPermission(UserCacheInfo user, String permission);

    boolean hasRole(UserCacheInfo user, String role);
}
