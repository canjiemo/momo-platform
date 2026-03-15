package com.seer.fitness.system.enums;

import lombok.Getter;

/**
 * 租户状态枚举
 * 定义租户（学校）的所有可能状态
 *
 * @author seer-fitness
 */
@Getter
public enum TenantStatus {

    /**
     * 待激活
     * 租户已创建但尚未激活
     */
    PENDING(0, "待激活"),

    /**
     * 正常
     * 租户正常运行中
     */
    ACTIVE(1, "正常"),

    /**
     * 已禁用
     * 租户已被管理员禁用
     */
    DISABLED(2, "已禁用"),

    /**
     * 已过期
     * 租户已过期，需要续费
     */
    EXPIRED(3, "已过期");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String description;

    TenantStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 租户状态枚举，找不到则返回 null
     */
    public static TenantStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TenantStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断租户是否可用（正常状态）
     *
     * @param code 状态码
     * @return true 表示可用
     */
    public static boolean isAvailable(Integer code) {
        return ACTIVE.getCode().equals(code);
    }
}
