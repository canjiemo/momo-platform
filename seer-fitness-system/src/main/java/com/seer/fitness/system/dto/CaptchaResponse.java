package com.seer.fitness.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码响应结果
 *
 * @author seer-fitness
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {

    /**
     * 验证码ID
     */
    private String captchaId;

    /**
     * 验证码图片（Base64编码的Data URL）
     */
    private String captchaImage;

    /**
     * 验证码过期时间（秒）
     */
    private Integer expireSeconds;
}