package com.seer.fitness.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 验证码配置
 *
 * @author mocanjie
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "security.captcha")
public class CaptchaConfig {

    /**
     * 验证码类型
     * DIGIT: 纯数字 (0-9)
     * LETTER: 纯字母 (A-Z)
     * MIXED: 数字+字母 (0-9,A-Z) - 推荐使用，安全性最高
     */
    private CaptchaType type = CaptchaType.MIXED;

    /**
     * 验证码长度
     */
    private int length = 6;

    /**
     * 图片宽度
     */
    private int width = 120;

    /**
     * 图片高度
     */
    private int height = 40;

    /**
     * 干扰线数量
     */
    private int lineCount = 5;

    /**
     * 验证码过期时间（秒）
     */
    private int expireSeconds = 300;

    /**
     * 是否启用验证码（默认启用）
     */
    private boolean enabled = true;

    /**
     * 验证码类型枚举
     */
    public enum CaptchaType {
        /**
         * 纯数字 (0-9)
         * 特点：用户输入简单，但安全性较低
         * 适用：内部系统或对安全要求不高的场景
         */
        DIGIT("0123456789"),

        /**
         * 纯字母 (A-Z)
         * 特点：避免数字混淆，但字符集较小，验证时不区分大小写
         * 适用：需要避免数字输入的场景
         */
        LETTER("ABCDEFGHIJKLMNOPQRSTUVWXYZ"),

        /**
         * 数字+字母 (0-9,A-Z)
         * 特点：字符集最大，安全性最高，验证时不区分大小写，推荐使用
         * 适用：对安全要求较高的登录验证场景
         */
        MIXED("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");

        private final String characters;

        CaptchaType(String characters) {
            this.characters = characters;
        }

        public String getCharacters() {
            return characters;
        }
    }
}