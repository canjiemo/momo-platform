package com.seer.fitness.system.enums;

/**
 * 操作类型枚举
 * 定义系统中所有可能的操作类型
 *
 * @author seer-fitness
 */
public enum OperationType {

    /**
     * 新增操作
     */
    CREATE("CREATE", "新增"),

    /**
     * 修改操作
     */
    UPDATE("UPDATE", "修改"),

    /**
     * 删除操作
     */
    DELETE("DELETE", "删除"),

    /**
     * 查询操作
     */
    QUERY("QUERY", "查询"),

    /**
     * 登录操作
     */
    LOGIN("LOGIN", "登录"),

    /**
     * 登出操作
     */
    LOGOUT("LOGOUT", "登出"),

    /**
     * 导入操作
     */
    IMPORT("IMPORT", "导入"),

    /**
     * 导出操作
     */
    EXPORT("EXPORT", "导出"),

    /**
     * 审核操作
     */
    AUDIT("AUDIT", "审核"),

    /**
     * 其他操作
     */
    OTHER("OTHER", "其他");

    /**
     * 操作代码
     */
    private final String code;

    /**
     * 操作描述
     */
    private final String description;

    OperationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 操作代码
     * @return 操作类型枚举
     */
    public static OperationType fromCode(String code) {
        for (OperationType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return OTHER;
    }
}