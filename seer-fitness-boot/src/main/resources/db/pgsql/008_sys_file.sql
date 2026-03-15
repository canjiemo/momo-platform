CREATE TABLE sys_file (
    id           BIGSERIAL PRIMARY KEY,
    file_name    VARCHAR(255)  NOT NULL,
    file_key     VARCHAR(500)  NOT NULL,
    file_url     VARCHAR(1000),
    file_size    BIGINT,
    content_type VARCHAR(100),
    storage_type VARCHAR(20),
    biz_type     VARCHAR(50),
    biz_id       VARCHAR(50),
    tenant_id    BIGINT,
    create_by    VARCHAR(50),
    create_time  TIMESTAMP,
    delete_flag  SMALLINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_sys_file_biz ON sys_file (biz_type, biz_id);
