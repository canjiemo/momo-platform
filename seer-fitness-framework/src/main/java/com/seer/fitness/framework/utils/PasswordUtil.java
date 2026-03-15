package com.seer.fitness.framework.utils;

import com.seer.fitness.framework.config.PasswordPolicyConfig;
import io.github.canjiemo.mycommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 密码工具类
 *
 * @author seer-fitness
 */
@Component
@Slf4j
public class PasswordUtil {

    @Autowired
    private PasswordPolicyConfig passwordConfig;

    /**
     * 验证密码强度 (验证前端传来的原始密码)
     */
    public void validatePasswordStrength(String originalPassword) {
        PasswordPolicyConfig.Policy policy = passwordConfig.getPolicy();

        if (originalPassword == null || originalPassword.length() < policy.getMinLength()) {
            throw new BusinessException(String.format("密码长度不能少于%d位", policy.getMinLength()));
        }

        if (originalPassword.length() > policy.getMaxLength()) {
            throw new BusinessException(String.format("密码长度不能超过%d位", policy.getMaxLength()));
        }

        if (policy.isRequireLowercase() && !originalPassword.matches(".*[a-z].*")) {
            throw new BusinessException("密码必须包含小写字母");
        }

        if (policy.isRequireUppercase() && !originalPassword.matches(".*[A-Z].*")) {
            throw new BusinessException("密码必须包含大写字母");
        }

        if (policy.isRequireDigit() && !originalPassword.matches(".*\\d.*")) {
            throw new BusinessException("密码必须包含数字");
        }

        if (policy.isRequireSpecial()) {
            String specialChars = Pattern.quote(policy.getSpecialChars());
            String pattern = ".*[" + specialChars + "].*";
            if (!originalPassword.matches(pattern)) {
                throw new BusinessException("密码必须包含特殊字符：" + policy.getSpecialChars());
            }
        }

        log.info("密码强度验证通过");
    }

    /**
     * 验证密码不为空
     */
    public void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        log.info("密码格式验证通过");
    }

    /**
     * 加密明文密码
     * 先验证密码强度，再使用BCrypt加密
     */
    public String encryptPassword(String plainPassword) {
        validatePassword(plainPassword);
        validatePasswordStrength(plainPassword);

        int strength = passwordConfig.getBackend().getBcryptStrength();
        String bcryptPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(strength));

        log.info("密码BCrypt加密完成，强度: {}", strength);
        return bcryptPassword;
    }

    /**
     * 验证密码 (明文密码 vs 数据库BCrypt密码)
     */
    public boolean verifyPassword(String plainPassword, String databaseBcryptPassword) {
        validatePassword(plainPassword);
        return BCrypt.checkpw(plainPassword, databaseBcryptPassword);
    }

    /**
     * 获取密码策略配置信息 (供前端验证使用)
     */
    public Map<String, Object> getPasswordPolicyConfig() {
        Map<String, Object> config = new HashMap<>();

        // 密码策略信息 (供前端验证使用)
        PasswordPolicyConfig.Policy policy = passwordConfig.getPolicy();
        config.put("minLength", policy.getMinLength());
        config.put("maxLength", policy.getMaxLength());
        config.put("requireLowercase", policy.isRequireLowercase());
        config.put("requireUppercase", policy.isRequireUppercase());
        config.put("requireDigit", policy.isRequireDigit());
        config.put("requireSpecial", policy.isRequireSpecial());
        config.put("specialChars", policy.getSpecialChars());

        return config;
    }

    /**
     * 验证密码强度（参数化版本，不依赖注入的 Config Bean）
     */
    public void validatePasswordStrength(String password,
                                         int minLen, int maxLen,
                                         boolean requireLower, boolean requireUpper,
                                         boolean requireDigit, boolean requireSpecial,
                                         String specialChars) {
        if (password == null || password.length() < minLen) {
            throw new BusinessException(String.format("密码长度不能少于%d位", minLen));
        }
        if (password.length() > maxLen) {
            throw new BusinessException(String.format("密码长度不能超过%d位", maxLen));
        }
        if (requireLower && !password.matches(".*[a-z].*")) {
            throw new BusinessException("密码必须包含小写字母");
        }
        if (requireUpper && !password.matches(".*[A-Z].*")) {
            throw new BusinessException("密码必须包含大写字母");
        }
        if (requireDigit && !password.matches(".*\\d.*")) {
            throw new BusinessException("密码必须包含数字");
        }
        if (requireSpecial && specialChars != null) {
            String pattern = ".*[" + Pattern.quote(specialChars) + "].*";
            if (!password.matches(pattern)) {
                throw new BusinessException("密码必须包含特殊字符：" + specialChars);
            }
        }
        log.info("密码强度验证通过");
    }

    /**
     * 加密密码（参数化版本，不依赖注入的 Config Bean）
     */
    public String encryptPassword(String plainPassword,
                                   int minLen, int maxLen,
                                   boolean requireLower, boolean requireUpper,
                                   boolean requireDigit, boolean requireSpecial,
                                   String specialChars, int bcryptStrength) {
        validatePassword(plainPassword);
        validatePasswordStrength(plainPassword, minLen, maxLen,
                requireLower, requireUpper, requireDigit, requireSpecial, specialChars);
        String bcryptPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(bcryptStrength));
        log.info("密码BCrypt加密完成，强度: {}", bcryptStrength);
        return bcryptPassword;
    }
}
