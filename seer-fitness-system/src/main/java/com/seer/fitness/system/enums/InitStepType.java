package com.seer.fitness.system.enums;

/**
 * 租户初始化步骤类型枚举
 * 定义租户 Schema 初始化过程的各个步骤
 *
 * @author seer-fitness
 */
public enum InitStepType {

    /**
     * 创建Schema
     * 创建租户独立的 PostgreSQL Schema
     */
    CREATE_SCHEMA("CREATE_SCHEMA", "创建Schema"),

    /**
     * 创建表结构
     * 在租户 Schema 中创建业务表
     */
    CREATE_TABLE("CREATE_TABLE", "创建表结构"),

    /**
     * 插入基础数据
     * 插入角色、菜单等基础数据
     */
    INSERT_DATA("INSERT_DATA", "插入基础数据"),

    /**
     * 创建管理员
     * 创建租户管理员账号
     */
    CREATE_ADMIN("CREATE_ADMIN", "创建管理员"),

    /**
     * 初始化Flyway基线
     * 为租户Schema建立Flyway版本管理基线
     */
    INIT_FLYWAY("INIT_FLYWAY", "初始化Flyway版本管理"),

    /**
     * 自动同步模板
     * 自动同步菜单和角色模板到租户
     */
    SYNC_TEMPLATES("SYNC_TEMPLATES", "同步菜单和角色模板"),

    /**
     * 回滚清理
     * 初始化失败时的回滚操作
     */
    ROLLBACK("ROLLBACK", "回滚清理");

    /**
     * 步骤代码
     */
    private final String code;

    /**
     * 步骤描述
     */
    private final String description;

    InitStepType(String code, String description) {
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
     * @param code 步骤代码
     * @return 初始化步骤类型枚举，找不到则返回 null
     */
    public static InitStepType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (InitStepType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
