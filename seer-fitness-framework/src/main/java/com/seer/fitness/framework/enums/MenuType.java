package com.seer.fitness.framework.enums;

/**
 * 菜单类型枚举
 *
 * @author seer-fitness
 */
public enum MenuType {

    /**
     * 目录 - 用于菜单分组，无实际路由
     */
    DIRECTORY(0, "目录"),

    /**
     * 菜单 - 对应具体页面路由
     */
    MENU(1, "菜单"),

    /**
     * 按钮 - 对应操作权限
     */
    BUTTON(2, "按钮");

    private final Integer code;
    private final String desc;

    MenuType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
