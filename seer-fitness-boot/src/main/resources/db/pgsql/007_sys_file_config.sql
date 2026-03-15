CREATE TABLE sys_file_config (
    id           BIGSERIAL PRIMARY KEY,
    config_name  VARCHAR(100)  NOT NULL,
    storage_type VARCHAR(20)   NOT NULL,
    is_active    SMALLINT      NOT NULL DEFAULT 0,
    config       JSONB         NOT NULL,
    remark       VARCHAR(500),
    create_by    VARCHAR(50),
    create_time  TIMESTAMP,
    update_by    VARCHAR(50),
    update_time  TIMESTAMP,
    delete_flag  SMALLINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_file_config_type   ON sys_file_config (storage_type);
CREATE INDEX idx_file_config_active ON sys_file_config (is_active) WHERE delete_flag = 0;

-- 默认本地存储（开发用）
INSERT INTO sys_file_config (config_name, storage_type, is_active, config, remark, create_by, create_time, update_by, update_time, delete_flag)
VALUES ('本地存储', 'local', 1,
        '{"basePath":"./uploads","urlPrefix":"http://localhost:8070/system/file/local"}',
        '文件存储到本地磁盘，仅适合开发环境', 'system', NOW(), 'system', NOW(), 0);

-- MinIO 配置模板（未激活，管理员填写参数后可激活）
INSERT INTO sys_file_config (config_name, storage_type, is_active, config, remark, create_by, create_time, update_by, update_time, delete_flag)
VALUES ('MinIO 存储', 'minio', 0,
        '{"endpoint":"http://minio:9000","bucket":"seer-fitness","accessKey":"","secretKey":"","publicBucket":true,"presignedExpireSeconds":3600}',
        '生产环境使用 MinIO 对象存储', 'system', NOW(), 'system', NOW(), 0);
