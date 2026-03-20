package io.github.canjiemo.momo.framework.enums;

public enum OperationType {

    CREATE("CREATE", "新增"),
    UPDATE("UPDATE", "修改"),
    DELETE("DELETE", "删除"),
    QUERY("QUERY", "查询"),
    LOGIN("LOGIN", "登录"),
    LOGOUT("LOGOUT", "登出"),
    IMPORT("IMPORT", "导入"),
    EXPORT("EXPORT", "导出"),
    SYNC("SYNC", "同步"),
    AUDIT("AUDIT", "审核"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String description;

    OperationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static OperationType fromCode(String code) {
        for (OperationType type : values()) {
            if (type.getCode().equals(code)) return type;
        }
        return OTHER;
    }
}
