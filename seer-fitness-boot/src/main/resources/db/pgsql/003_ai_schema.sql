-- ====================================================================================================
-- Seer Fitness Edu - AI 智能查询模块建表脚本
-- 执行前提：已执行 001_schema.sql（pgvector 扩展由镜像提供）
-- ====================================================================================================

-- ai_provider_config (AI 模型配置，可插拔)
DROP TABLE IF EXISTS public.ai_provider_config;
CREATE TABLE public.ai_provider_config (
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
CREATE INDEX idx_ai_provider_active ON public.ai_provider_config(is_active) WHERE delete_flag = 0;

COMMENT ON TABLE  public.ai_provider_config IS 'AI 模型提供商配置表，支持 Ollama/Claude/OpenAI 等多种模型热切换，同一时刻只有一条 is_active=1';
COMMENT ON COLUMN public.ai_provider_config.id          IS '配置唯一标识';
COMMENT ON COLUMN public.ai_provider_config.config_name IS '配置名称，便于区分多套配置，如"本地Ollama"、"Claude生产环境"';
COMMENT ON COLUMN public.ai_provider_config.provider    IS 'AI 提供商类型：ollama=本地Ollama claude=Anthropic Claude openai=OpenAI';
COMMENT ON COLUMN public.ai_provider_config.chat_model  IS '对话模型名称，如 qwen2.5:7b、claude-3-5-sonnet、gpt-4o';
COMMENT ON COLUMN public.ai_provider_config.embed_model IS 'Embedding 向量化模型名称，如 nomic-embed-text，用于将文本转换为向量';
COMMENT ON COLUMN public.ai_provider_config.base_url    IS 'AI 服务地址，Ollama 本地部署示例：http://localhost:11434';
COMMENT ON COLUMN public.ai_provider_config.api_key     IS 'API 访问密钥，本地模型可为空，Claude/OpenAI 必填';
COMMENT ON COLUMN public.ai_provider_config.is_active   IS '是否为当前激活配置：1=激活 0=未激活，激活后系统使用此配置处理 AI 请求';
COMMENT ON COLUMN public.ai_provider_config.config      IS '扩展配置 JSON，如 {"temperature":0.7,"max_tokens":2048}';
COMMENT ON COLUMN public.ai_provider_config.remark      IS '配置备注说明';
COMMENT ON COLUMN public.ai_provider_config.create_time IS '记录创建时间';
COMMENT ON COLUMN public.ai_provider_config.update_time IS '记录最后修改时间';
COMMENT ON COLUMN public.ai_provider_config.delete_flag IS '逻辑删除标志：0=正常 1=已删除';

-- ai_table_catalog (数据目录 - 表级)
DROP TABLE IF EXISTS public.ai_table_catalog;
CREATE TABLE public.ai_table_catalog (
    id           BIGSERIAL    PRIMARY KEY,
    table_name   VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description  TEXT,
    is_enabled   SMALLINT     NOT NULL DEFAULT 0,
    sort_order   INT          DEFAULT 0,
    create_time  TIMESTAMP,
    update_time  TIMESTAMP
);

COMMENT ON TABLE  public.ai_table_catalog IS 'AI 可查询数据目录（表级），管理员配置哪些表允许 AI 查询，description 会注入到 Prompt 帮助 LLM 理解表用途';
COMMENT ON COLUMN public.ai_table_catalog.id           IS '目录记录唯一标识';
COMMENT ON COLUMN public.ai_table_catalog.table_name   IS '数据库表名，与 information_schema 中的 table_name 一致';
COMMENT ON COLUMN public.ai_table_catalog.display_name IS '表的中文名称，如"用户信息表"，注入 Prompt 供 LLM 理解';
COMMENT ON COLUMN public.ai_table_catalog.description  IS '表用途的语义描述，如"存储系统所有用户的账号信息，含教师和学生"，越详细 AI 查询越准确';
COMMENT ON COLUMN public.ai_table_catalog.is_enabled   IS '是否开放给 AI 查询：1=开放 0=禁用，禁用的表 AI 不会生成查询该表的 SQL';
COMMENT ON COLUMN public.ai_table_catalog.sort_order   IS '展示排序顺序';
COMMENT ON COLUMN public.ai_table_catalog.create_time  IS '记录创建时间';
COMMENT ON COLUMN public.ai_table_catalog.update_time  IS '记录最后修改时间';

-- ai_field_catalog (数据目录 - 字段级)
DROP TABLE IF EXISTS public.ai_field_catalog;
CREATE TABLE public.ai_field_catalog (
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
CREATE INDEX idx_field_catalog_table  ON public.ai_field_catalog(table_id);
CREATE INDEX idx_field_catalog_vector ON public.ai_field_catalog
    USING hnsw (embed_vector vector_cosine_ops);

COMMENT ON TABLE  public.ai_field_catalog IS 'AI 可查询数据目录（字段级），管理员为每个字段配置中文名和语义描述，description 向量化后存入 embed_vector 用于 RAG 检索';
COMMENT ON COLUMN public.ai_field_catalog.id           IS '字段目录记录唯一标识';
COMMENT ON COLUMN public.ai_field_catalog.table_id     IS '所属表目录ID，关联 ai_table_catalog.id';
COMMENT ON COLUMN public.ai_field_catalog.table_name   IS '所属数据库表名（冗余存储，加速查询）';
COMMENT ON COLUMN public.ai_field_catalog.field_name   IS '数据库字段名，与 information_schema 中的 column_name 一致';
COMMENT ON COLUMN public.ai_field_catalog.field_type   IS '字段数据类型，如 varchar、bigint、timestamp，来自 information_schema';
COMMENT ON COLUMN public.ai_field_catalog.display_name IS '字段中文名称，如"用户真实姓名"，注入 Prompt 供 LLM 理解';
COMMENT ON COLUMN public.ai_field_catalog.description  IS '字段语义描述，如"用户的真实姓名，用于页面展示，区别于登录账号 username"，此描述会被向量化用于相似度检索';
COMMENT ON COLUMN public.ai_field_catalog.is_enabled   IS '是否参与 AI 查询：1=启用 0=禁用，禁用的字段不会出现在 Prompt 的 Schema 上下文中';
COMMENT ON COLUMN public.ai_field_catalog.embed_vector IS '字段描述的 768 维向量（由 nomic-embed-text Embedding 模型生成），用于余弦相似度检索找最相关字段';
COMMENT ON COLUMN public.ai_field_catalog.sort_order   IS '展示排序顺序，默认与数据库字段顺序一致';
COMMENT ON COLUMN public.ai_field_catalog.create_time  IS '记录创建时间';
COMMENT ON COLUMN public.ai_field_catalog.update_time  IS '记录最后修改时间';

-- ai_session (会话元数据：id、标题、归属用户)
DROP TABLE IF EXISTS public.ai_session;
CREATE TABLE public.ai_session (
    id          BIGSERIAL    PRIMARY KEY,
    session_id  VARCHAR(64)  NOT NULL UNIQUE,
    tenant_id   BIGINT,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL DEFAULT '新对话',
    create_time TIMESTAMP    DEFAULT NOW(),
    update_time TIMESTAMP    DEFAULT NOW(),
    delete_flag SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX idx_ai_session_user ON public.ai_session(user_id, tenant_id) WHERE delete_flag = 0;

COMMENT ON TABLE  public.ai_session IS 'AI 对话会话表，存储会话元数据，与 ai_conversation 通过 session_id 关联';
COMMENT ON COLUMN public.ai_session.session_id  IS '会话唯一标识（UUID），与 ai_conversation.session_id 一致';
COMMENT ON COLUMN public.ai_session.tenant_id   IS '租户ID，用于租户隔离';
COMMENT ON COLUMN public.ai_session.user_id     IS '会话归属用户ID';
COMMENT ON COLUMN public.ai_session.title       IS '会话标题，默认取首条问题前30字，可由用户手动重命名';
COMMENT ON COLUMN public.ai_session.delete_flag IS '逻辑删除：0=正常 1=已删除';

-- ai_conversation (对话历史，仅追加，无 delete_flag)
DROP TABLE IF EXISTS public.ai_conversation;
CREATE TABLE public.ai_conversation (
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
CREATE INDEX idx_ai_conv_session ON public.ai_conversation(session_id);
CREATE INDEX idx_ai_conv_tenant  ON public.ai_conversation(tenant_id);
CREATE INDEX idx_ai_conv_time    ON public.ai_conversation(create_time DESC);

COMMENT ON TABLE  public.ai_conversation IS 'AI 对话历史表，记录每次用户提问和 AI 回答，仅追加不删除，用于展示对话记录';
COMMENT ON COLUMN public.ai_conversation.id            IS '对话消息唯一标识';
COMMENT ON COLUMN public.ai_conversation.tenant_id     IS '租户ID，关联 sys_tenant.id，用于租户数据隔离';
COMMENT ON COLUMN public.ai_conversation.session_id    IS '会话ID，由前端生成（UUID），同一轮对话使用同一 session_id';
COMMENT ON COLUMN public.ai_conversation.user_id       IS '提问用户ID，关联 sys_user.id';
COMMENT ON COLUMN public.ai_conversation.role          IS '消息角色：user=用户提问 assistant=AI 回答';
COMMENT ON COLUMN public.ai_conversation.content       IS '消息内容，user 时为用户原始问题，assistant 时为 AI 生成的文字摘要';
COMMENT ON COLUMN public.ai_conversation.generated_sql IS 'AI 本次生成并执行的 SQL 语句（仅 assistant 消息有值）';
COMMENT ON COLUMN public.ai_conversation.exec_rows     IS 'SQL 执行返回的数据行数（仅 assistant 消息有值）';
COMMENT ON COLUMN public.ai_conversation.create_time   IS '消息创建时间';
