package com.seer.fitness.framework.enums;

/**
 * 用户类型枚举
 *
 * @author Claude
 * @since 2025-10-06
 */
public enum UserType {

    /**
     * 运维人员
     */
    STAFF(0, "运维人员"),

    /**
     * 教师
     */
    TEACHER(1, "教师"),

    /**
     * 学生
     */
    STUDENT(2, "学生");

    private final Integer code;
    private final String description;

    UserType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 用户类型代码
     * @return UserType 枚举，未找到返回null
     */
    public static UserType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserType type : UserType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 验证code是否有效
     *
     * @param code 用户类型代码
     * @return 是否有效
     */
    public static boolean isValid(Integer code) {
        return fromCode(code) != null;
    }
}
