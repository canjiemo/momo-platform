package io.github.canjiemo.momo.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限控制配置类
 *
 * @author canjiemo@gmail.com
 */
@ConfigurationProperties(prefix = "auth")
@Data
@Component
public class AuthConfig {

    /**
     * 不需要登录验证的接口路径
     */
    private List<String> permitAll = new ArrayList<>();

    /**
     * 超级管理员角色
     */
    private List<String> superAdminRoles = new ArrayList<>();
}