package com.seer.fitness.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置类
 *
 * @author seer-fitness
 */
@ConfigurationProperties(prefix = "jwt")
@Data
@Component
public class JwtConfig {

    /**
     * JWT密钥 (从配置文件读取)
     */
    private String secret;

    /**
     * Token有效期(毫秒，从配置文件读取)
     */
    private long expiration;
}