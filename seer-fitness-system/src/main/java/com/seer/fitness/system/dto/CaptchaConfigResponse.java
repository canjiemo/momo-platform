package com.seer.fitness.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码配置响应结果
 *
 * @author seer-fitness
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaConfigResponse {

    /**
     * 是否启用验证码
     */
    private Boolean enabled;

    /**
     * 验证码类型（DIGIT/LETTER/MIXED）
     */
    private String type;

    /**
     * 验证码长度
     */
    private Integer length;

    /**
     * 验证码过期时间（秒）
     */
    private Integer expireSeconds;
}