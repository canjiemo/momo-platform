package com.seer.fitness.framework.utils;

import io.github.canjiemo.mycommon.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码工具类单元测试
 * <p>
 * 使用参数化版本 validatePasswordStrength / encryptPassword，不依赖 Spring 容器。
 */
class PasswordUtilTest {

    private PasswordUtil passwordUtil;

    // 标准策略参数
    private static final int MIN = 8;
    private static final int MAX = 30;
    private static final boolean LOWER   = true;
    private static final boolean UPPER   = true;
    private static final boolean DIGIT   = true;
    private static final boolean SPECIAL = true;
    private static final String  CHARS   = "!@#$%^&*";

    @BeforeEach
    void setUp() {
        passwordUtil = new PasswordUtil();
    }

    // -----------------------------------------------------------------------
    // validatePasswordStrength（参数化版本）
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("符合所有规则的密码 → 不抛异常")
    void validPassword_noException() {
        assertDoesNotThrow(() ->
                passwordUtil.validatePasswordStrength("Abc123!@", MIN, MAX,
                        LOWER, UPPER, DIGIT, SPECIAL, CHARS));
    }

    @Test
    @DisplayName("密码过短 → 抛出 BusinessException")
    void tooShort_throws() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                passwordUtil.validatePasswordStrength("Aa1!", MIN, MAX,
                        LOWER, UPPER, DIGIT, SPECIAL, CHARS));
        assertTrue(ex.getMessage().contains("少于"));
    }

    @Test
    @DisplayName("密码过长 → 抛出 BusinessException")
    void tooLong_throws() {
        String longPwd = "Aa1!".repeat(10); // 40 chars
        BusinessException ex = assertThrows(BusinessException.class, () ->
                passwordUtil.validatePasswordStrength(longPwd, MIN, MAX,
                        LOWER, UPPER, DIGIT, SPECIAL, CHARS));
        assertTrue(ex.getMessage().contains("超过"));
    }

    @Test
    @DisplayName("缺少小写字母 → 抛出 BusinessException")
    void missingLowercase_throws() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                passwordUtil.validatePasswordStrength("ABC123!@", MIN, MAX,
                        LOWER, UPPER, DIGIT, SPECIAL, CHARS));
        assertTrue(ex.getMessage().contains("小写"));
    }

    @Test
    @DisplayName("缺少大写字母 → 抛出 BusinessException")
    void missingUppercase_throws() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                passwordUtil.validatePasswordStrength("abc123!@", MIN, MAX,
                        LOWER, UPPER, DIGIT, SPECIAL, CHARS));
        assertTrue(ex.getMessage().contains("大写"));
    }

    @Test
    @DisplayName("缺少数字 → 抛出 BusinessException")
    void missingDigit_throws() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                passwordUtil.validatePasswordStrength("Abcdef!@", MIN, MAX,
                        LOWER, UPPER, DIGIT, SPECIAL, CHARS));
        assertTrue(ex.getMessage().contains("数字"));
    }

    @Test
    @DisplayName("缺少特殊字符 → 抛出 BusinessException")
    void missingSpecial_throws() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                passwordUtil.validatePasswordStrength("Abc12345", MIN, MAX,
                        LOWER, UPPER, DIGIT, SPECIAL, CHARS));
        assertTrue(ex.getMessage().contains("特殊字符"));
    }

    @Test
    @DisplayName("不要求特殊字符时，普通密码通过")
    void specialNotRequired_passes() {
        assertDoesNotThrow(() ->
                passwordUtil.validatePasswordStrength("Abc12345", MIN, MAX,
                        LOWER, UPPER, DIGIT, false, CHARS));
    }

    @Test
    @DisplayName("null 密码 → 抛出 BusinessException")
    void nullPassword_throws() {
        assertThrows(BusinessException.class, () ->
                passwordUtil.validatePasswordStrength(null, MIN, MAX,
                        LOWER, UPPER, DIGIT, SPECIAL, CHARS));
    }

    // -----------------------------------------------------------------------
    // verifyPassword（BCrypt 验证）
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("正确密码验证 → 返回 true")
    void verifyPassword_correct() {
        String hash = BCrypt.hashpw("Abc123!@", BCrypt.gensalt(4));
        assertTrue(passwordUtil.verifyPassword("Abc123!@", hash));
    }

    @Test
    @DisplayName("错误密码验证 → 返回 false")
    void verifyPassword_wrong() {
        String hash = BCrypt.hashpw("Abc123!@", BCrypt.gensalt(4));
        assertFalse(passwordUtil.verifyPassword("WrongPwd1!", hash));
    }

    @Test
    @DisplayName("verifyPassword 传入空密码 → 抛出 BusinessException")
    void verifyPassword_emptyThrows() {
        String hash = BCrypt.hashpw("Abc123!@", BCrypt.gensalt(4));
        assertThrows(BusinessException.class, () ->
                passwordUtil.verifyPassword("", hash));
    }

    // -----------------------------------------------------------------------
    // encryptPassword（参数化版本，验证输出是有效 BCrypt hash）
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("参数化加密 → 输出合法 BCrypt hash 且可验证")
    void encryptPassword_validHash() {
        String hash = passwordUtil.encryptPassword("Abc123!@",
                MIN, MAX, LOWER, UPPER, DIGIT, SPECIAL, CHARS, 4);
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"));
        assertTrue(BCrypt.checkpw("Abc123!@", hash));
    }

    @Test
    @DisplayName("参数化加密 → 相同密码两次加密结果不同（salt 随机）")
    void encryptPassword_randomSalt() {
        String h1 = passwordUtil.encryptPassword("Abc123!@",
                MIN, MAX, LOWER, UPPER, DIGIT, SPECIAL, CHARS, 4);
        String h2 = passwordUtil.encryptPassword("Abc123!@",
                MIN, MAX, LOWER, UPPER, DIGIT, SPECIAL, CHARS, 4);
        assertNotEquals(h1, h2);
    }
}
