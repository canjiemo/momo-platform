-- ====================================================================================================
-- V3 - AI 智能查询模块建表
-- 前提：V1 已执行，数据库已安装 pgvector 扩展
-- ====================================================================================================

CREATE EXTENSION IF NOT EXISTS vector;

-- ai_provider_config (AI 模型配置，可插拔)
CREATE TABLE IF NOT EXISTS public.ai_provider_config (
    id           BIGSERIAL    PRIMARY KEY,
    config_name  VARCHAR(100) NOT NULL,
    provider     VARCHAR(50)  NOT NULL,
    chat_model   VARCHAR(100) NOT NULL,
    embed_model  VARCHAR(100) NOT NULL,
    base_url     VARCHAR(500),
    api_key      VARCHAR(500),
    is_active    SMALLINT     NOT NULL DEFAULT 0,
    config       JSONB,
    remark       VARCHAR(500),
    create_time  TIMESTAMP,
    update_time  TIMESTAMP,
    delete_flag  SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ai_provider_active ON public.ai_provider_config(is_active) WHERE delete_flag = 0;

COMMENT ON TABLE  public.ai_provider_config IS 'AI 模型提供商配置表，支持 Ollama/阿里百炼/OpenAI 等多种模型热切换，同一时刻只有一条 is_active=1';
COMMENT ON COLUMN public.ai_provider_config.provider    IS 'AI 提供商类型：ollama=本地Ollama aliyun=阿里百炼 openai=OpenAI';
COMMENT ON COLUMN public.ai_provider_config.is_active   IS '是否为当前激活配置：1=激活 0=未激活';

-- ai_table_catalog (数据目录 - 表级)
CREATE TABLE IF NOT EXISTS public.ai_table_catalog (
    id           BIGSERIAL    PRIMARY KEY,
    table_name   VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description  TEXT,
    is_enabled   SMALLINT     NOT NULL DEFAULT 0,
    sort_order   INT          DEFAULT 0,
    create_time  TIMESTAMP,
    update_time  TIMESTAMP
);

COMMENT ON TABLE  public.ai_table_catalog IS 'AI 可查询数据目录（表级），管理员配置哪些表允许 AI 查询';

-- ai_field_catalog (数据目录 - 字段级)
CREATE TABLE IF NOT EXISTS public.ai_field_catalog (
    id           BIGSERIAL    PRIMARY KEY,
    table_id     BIGINT       NOT NULL,
    table_name   VARCHAR(100) NOT NULL,
    field_name   VARCHAR(100) NOT NULL,
    field_type   VARCHAR(50),
    display_name VARCHAR(100) NOT NULL,
    description  TEXT,
    is_enabled   SMALLINT     NOT NULL DEFAULT 1,
    embed_vector vector(768),
    sort_order   INT          DEFAULT 0,
    create_time  TIMESTAMP,
    update_time  TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_field_catalog_table  ON public.ai_field_catalog(table_id);
CREATE INDEX IF NOT EXISTS idx_field_catalog_vector ON public.ai_field_catalog
    USING hnsw (embed_vector vector_cosine_ops);

COMMENT ON TABLE  public.ai_field_catalog IS 'AI 可查询数据目录（字段级），字段描述向量化后用于 RAG 检索';

-- ai_session (会话元数据)
CREATE TABLE IF NOT EXISTS public.ai_session (
    id          BIGSERIAL    PRIMARY KEY,
    session_id  VARCHAR(64)  NOT NULL UNIQUE,
    tenant_id   BIGINT,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL DEFAULT '新对话',
    create_time TIMESTAMP    DEFAULT NOW(),
    update_time TIMESTAMP    DEFAULT NOW(),
    delete_flag SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ai_session_user ON public.ai_session(user_id, tenant_id) WHERE delete_flag = 0;

COMMENT ON TABLE  public.ai_session IS 'AI 对话会话表，存储会话元数据，与 ai_conversation 通过 session_id 关联';

-- ai_conversation (对话历史，仅追加)
CREATE TABLE IF NOT EXISTS public.ai_conversation (
    id            BIGSERIAL   PRIMARY KEY,
    tenant_id     BIGINT,
    session_id    VARCHAR(64) NOT NULL,
    user_id       BIGINT,
    role          VARCHAR(10) NOT NULL,
    content       TEXT        NOT NULL,
    generated_sql TEXT,
    exec_rows     INT,
    create_time   TIMESTAMP   DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ai_conv_session ON public.ai_conversation(session_id);
CREATE INDEX IF NOT EXISTS idx_ai_conv_tenant  ON public.ai_conversation(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ai_conv_time    ON public.ai_conversation(create_time DESC);

COMMENT ON TABLE  public.ai_conversation IS 'AI 对话历史表，记录每次用户提问和 AI 回答，仅追加不删除';
