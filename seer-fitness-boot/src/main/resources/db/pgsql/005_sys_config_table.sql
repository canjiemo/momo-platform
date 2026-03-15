-- 系统配置表
CREATE TABLE sys_config (
    id          BIGSERIAL PRIMARY KEY,
    config_key  VARCHAR(100) NOT NULL,
    config_value TEXT,
    config_name VARCHAR(200) NOT NULL,
    config_type SMALLINT NOT NULL DEFAULT 1,
    remark      VARCHAR(500),
    tenant_id   BIGINT,
    create_by   VARCHAR(50),
    create_time TIMESTAMP,
    update_by   VARCHAR(50),
    update_time TIMESTAMP,
    delete_flag SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_sys_config_key UNIQUE (config_key)
);

COMMENT ON TABLE sys_config IS '系统配置表';
COMMENT ON COLUMN sys_config.config_key IS '配置键（全局唯一）';
COMMENT ON COLUMN sys_config.config_value IS '配置值（列表用英文逗号分隔）';
COMMENT ON COLUMN sys_config.config_name IS '配置名称（可读描述）';
COMMENT ON COLUMN sys_config.config_type IS '1=系统内置（不可删）2=用户自定义';
COMMENT ON COLUMN sys_config.tenant_id IS '租户ID，预留字段，当前仅使用 NULL（平台级）';
