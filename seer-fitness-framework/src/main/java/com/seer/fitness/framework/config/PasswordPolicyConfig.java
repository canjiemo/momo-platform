package com.seer.fitness.framework.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 密码策略配置类
 *
 * @author seer-fitness
 */
@ConfigurationProperties(prefix = "security.password")
@Data
@Component
@Validated
public class PasswordPolicyConfig {

    /**
     * 密码强度策略
     */
    private Policy policy = new Policy();

    /**
     * 初始密码配置
     */
    private String initialPassword = "Aa123456!";

    /**
     * 后端加密配置
     */
    private Backend backend = new Backend();

    @Data
    public static class Policy {
        @Min(6)
        @Max(50)
        private int minLength = 8;

        @Min(6)
        @Max(50)
        private int maxLength = 15;

        private boolean requireLowercase = true;
        private boolean requireUppercase = true;
        private boolean requireDigit = true;
        private boolean requireSpecial = true;
        private String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    }

    @Data
    public static class Backend {
        @Min(4)
        @Max(15)
        private int bcryptStrength = 12;
    }
}
