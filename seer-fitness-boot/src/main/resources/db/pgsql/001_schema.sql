-- ====================================================================================================
-- Seer Fitness Edu - 统一建表脚本
-- 所有表在 public schema，通过 tenant_id 区分租户数据
-- tenant_id = NULL 表示平台级数据（平台菜单/角色）
-- tenant_id = 具体值 表示租户数据
-- ====================================================================================================

-- ----------------------------
-- sys_tenant (租户主表，平台级)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_tenant;
CREATE TABLE public.sys_tenant (
  id BIGSERIAL PRIMARY KEY,
  tenant_code VARCHAR(50) NOT NULL UNIQUE,
  tenant_name VARCHAR(100) NOT NULL,
  real_name VARCHAR(50),
  contact_phone VARCHAR(20),
  contact_email VARCHAR(100),
  address VARCHAR(200),
  description TEXT,
  status SMALLINT DEFAULT 0,
  activated_at TIMESTAMP,
  expired_at TIMESTAMP,
  max_users INT DEFAULT 1000,
  delete_flag SMALLINT DEFAULT 0,
  created_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_tenant_code ON public.sys_tenant(tenant_code);
CREATE INDEX idx_sys_tenant_status ON public.sys_tenant(status, delete_flag);

COMMENT ON TABLE public.sys_tenant IS '租户表（学校表），平台级';
COMMENT ON COLUMN public.sys_tenant.tenant_code IS '租户编码，全局唯一';
COMMENT ON COLUMN public.sys_tenant.tenant_name IS '管理员登录账号，字母开头，只能包含字母/数字/下划线，创建后不可修改';
COMMENT ON COLUMN public.sys_tenant.real_name IS '学校中文名称，作为管理员账号的显示名称';
COMMENT ON COLUMN public.sys_tenant.status IS '状态：0-待激活 1-正常 2-已禁用 3-已过期';

-- ----------------------------
-- sys_user (用户表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_user;
CREATE TABLE public.sys_user (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(255) NOT NULL,
  real_name VARCHAR(50),
  org_id BIGINT,
  admin_flag SMALLINT DEFAULT 0,
  user_type SMALLINT DEFAULT 0,
  status SMALLINT DEFAULT 1,
  delete_flag SMALLINT DEFAULT 0,
  created_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(tenant_id, username)
);

CREATE INDEX idx_sys_user_tenant ON public.sys_user(tenant_id);
CREATE INDEX idx_sys_user_username ON public.sys_user(username);
CREATE INDEX idx_sys_user_status ON public.sys_user(status, delete_flag);

COMMENT ON TABLE public.sys_user IS '用户表';
COMMENT ON COLUMN public.sys_user.tenant_id IS '租户ID，NULL表示平台管理员';
COMMENT ON COLUMN public.sys_user.admin_flag IS '超级管理员标识：1是 0否';
COMMENT ON COLUMN public.sys_user.user_type IS '用户类型: 0-管理员 1-教师 2-学生';

-- ----------------------------
-- sys_role (角色表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_role;
CREATE TABLE public.sys_role (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT,
  role_name VARCHAR(50) NOT NULL,
  role_code VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  status SMALLINT DEFAULT 1,
  delete_flag SMALLINT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_role_tenant ON public.sys_role(tenant_id);
CREATE INDEX idx_sys_role_status ON public.sys_role(status, delete_flag);
CREATE UNIQUE INDEX idx_sys_role_code ON public.sys_role(COALESCE(tenant_id, -1), role_code) WHERE delete_flag = 0;

COMMENT ON TABLE public.sys_role IS '角色表';
COMMENT ON COLUMN public.sys_role.tenant_id IS '租户ID，NULL表示平台角色模板';
COMMENT ON COLUMN public.sys_role.role_code IS '角色编码，同租户内唯一';

-- ----------------------------
-- sys_menu (菜单表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_menu;
CREATE TABLE public.sys_menu (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT,
  menu_name VARCHAR(50) NOT NULL,
  path VARCHAR(255),
  parent_id BIGINT DEFAULT 0,
  type SMALLINT DEFAULT 0,
  permission VARCHAR(100),
  icon VARCHAR(100),
  sort_order INT DEFAULT 0,
  status SMALLINT DEFAULT 1,
  delete_flag SMALLINT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_menu_tenant ON public.sys_menu(tenant_id);
CREATE INDEX idx_sys_menu_parent_id ON public.sys_menu(parent_id);
CREATE INDEX idx_sys_menu_permission ON public.sys_menu(permission);
CREATE INDEX idx_sys_menu_status_delete ON public.sys_menu(status, delete_flag);

COMMENT ON TABLE public.sys_menu IS '菜单表';
COMMENT ON COLUMN public.sys_menu.tenant_id IS '租户ID，NULL表示平台菜单模板';
COMMENT ON COLUMN public.sys_menu.type IS '类型：0目录 1菜单 2按钮';

-- ----------------------------
-- sys_role_menu (角色菜单关联表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_role_menu;
CREATE TABLE public.sys_role_menu (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_role_menu_tenant ON public.sys_role_menu(tenant_id);
CREATE INDEX idx_sys_role_menu_role ON public.sys_role_menu(role_id);
CREATE INDEX idx_sys_role_menu_menu ON public.sys_role_menu(menu_id);

COMMENT ON TABLE public.sys_role_menu IS '角色-菜单关联表';

-- ----------------------------
-- sys_user_role (用户角色关联表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_user_role;
CREATE TABLE public.sys_user_role (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_user_role_tenant ON public.sys_user_role(tenant_id);
CREATE INDEX idx_sys_user_role_user ON public.sys_user_role(user_id);
CREATE INDEX idx_sys_user_role_role ON public.sys_user_role(role_id);

COMMENT ON TABLE public.sys_user_role IS '用户-角色关联表';

-- ----------------------------
-- sys_tenant_role (租户-平台角色映射表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_tenant_role;
CREATE TABLE public.sys_tenant_role (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(tenant_id, role_id)
);

CREATE INDEX idx_sys_tenant_role_tenant ON public.sys_tenant_role(tenant_id);
CREATE INDEX idx_sys_tenant_role_role ON public.sys_tenant_role(role_id);

COMMENT ON TABLE public.sys_tenant_role IS '租户-平台角色映射表';

-- ----------------------------
-- sys_organization (组织架构表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_organization;
CREATE TABLE public.sys_organization (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT,
  org_code VARCHAR(50),
  org_name VARCHAR(100) NOT NULL,
  parent_id BIGINT DEFAULT 0,
  sort_order INT DEFAULT 0,
  leader_id BIGINT,
  contact_phone VARCHAR(20),
  email VARCHAR(100),
  address VARCHAR(200),
  description TEXT,
  status SMALLINT DEFAULT 1,
  delete_flag SMALLINT DEFAULT 0,
  created_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_org_tenant ON public.sys_organization(tenant_id);
CREATE INDEX idx_sys_org_parent ON public.sys_organization(parent_id);
CREATE INDEX idx_sys_org_status ON public.sys_organization(status, delete_flag);

COMMENT ON TABLE public.sys_organization IS '组织架构表';

-- ----------------------------
-- sys_operation_log (操作日志表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_operation_log;
CREATE TABLE public.sys_operation_log (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT,
  user_id BIGINT,
  username VARCHAR(50),
  real_name VARCHAR(50),
  operation_type VARCHAR(20) NOT NULL,
  module_name VARCHAR(50) NOT NULL,
  business_id VARCHAR(100),
  business_name VARCHAR(200),
  operation_desc TEXT,
  request_method VARCHAR(10),
  request_url VARCHAR(500),
  request_params JSONB,
  response_data JSONB,
  ip_address VARCHAR(50),
  user_agent TEXT,
  operation_result SMALLINT DEFAULT 1,
  error_message TEXT,
  execution_time INT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_op_log_tenant ON public.sys_operation_log(tenant_id);
CREATE INDEX idx_op_log_user ON public.sys_operation_log(user_id);
CREATE INDEX idx_op_log_type ON public.sys_operation_log(operation_type);
CREATE INDEX idx_op_log_module ON public.sys_operation_log(module_name);
CREATE INDEX idx_op_log_time ON public.sys_operation_log(create_time);

COMMENT ON TABLE public.sys_operation_log IS '操作日志表';

-- ----------------------------
-- sys_dict_type (数据字典类型表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_dict_type;
CREATE TABLE public.sys_dict_type (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT,
  dict_name VARCHAR(100) NOT NULL,
  dict_type VARCHAR(100) NOT NULL,
  dict_description VARCHAR(500),
  status SMALLINT DEFAULT 1,
  sort_order INT DEFAULT 0,
  remark VARCHAR(500),
  create_by VARCHAR(64),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64),
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  delete_flag SMALLINT DEFAULT 0
);

CREATE INDEX idx_dict_type_tenant ON public.sys_dict_type(tenant_id);
CREATE INDEX idx_dict_type_type ON public.sys_dict_type(dict_type);
CREATE INDEX idx_dict_type_status ON public.sys_dict_type(status, delete_flag);

COMMENT ON TABLE public.sys_dict_type IS '数据字典类型表';

-- ----------------------------
-- sys_dict_data (数据字典数据表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_dict_data;
CREATE TABLE public.sys_dict_data (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT,
  dict_type VARCHAR(100) NOT NULL,
  dict_label VARCHAR(100) NOT NULL,
  dict_value VARCHAR(100) NOT NULL,
  dict_description VARCHAR(500),
  css_class VARCHAR(100),
  list_class VARCHAR(100),
  is_default SMALLINT DEFAULT 0,
  status SMALLINT DEFAULT 1,
  sort_order INT DEFAULT 0,
  remark VARCHAR(500),
  create_by VARCHAR(64),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64),
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  delete_flag SMALLINT DEFAULT 0
);

CREATE INDEX idx_dict_data_tenant ON public.sys_dict_data(tenant_id);
CREATE INDEX idx_dict_data_type ON public.sys_dict_data(dict_type);
CREATE INDEX idx_dict_data_status ON public.sys_dict_data(status, delete_flag);

COMMENT ON TABLE public.sys_dict_data IS '数据字典数据表';

-- ----------------------------
-- sys_job (定时任务定义表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_job;
CREATE TABLE public.sys_job (
  id              BIGSERIAL     PRIMARY KEY,
  job_name        VARCHAR(100)  NOT NULL,
  job_group       VARCHAR(50)   NOT NULL DEFAULT 'DEFAULT',
  handler_name    VARCHAR(100)  NOT NULL,
  cron_expression VARCHAR(50)   NOT NULL,
  job_params      TEXT,
  status          SMALLINT      NOT NULL DEFAULT 0,
  remark          VARCHAR(255),
  delete_flag     SMALLINT      NOT NULL DEFAULT 0,
  created_by      BIGINT,
  create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by      BIGINT,
  update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  public.sys_job IS '定时任务定义表';
COMMENT ON COLUMN public.sys_job.handler_name IS 'Spring Bean 名称';
COMMENT ON COLUMN public.sys_job.status IS '0=停用 1=启用';

-- ----------------------------
-- sys_job_log (定时任务执行历史表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_job_log;
CREATE TABLE public.sys_job_log (
  id            BIGSERIAL    PRIMARY KEY,
  job_id        BIGINT       NOT NULL,
  job_name      VARCHAR(100) NOT NULL,
  handler_name  VARCHAR(100) NOT NULL,
  trigger_type  SMALLINT     NOT NULL DEFAULT 0,
  start_time    TIMESTAMP    NOT NULL,
  end_time      TIMESTAMP,
  duration_ms   BIGINT,
  status        SMALLINT     NOT NULL DEFAULT 0,
  error_msg     TEXT,
  operator_id   BIGINT
);

CREATE INDEX idx_sys_job_log_job_id     ON public.sys_job_log(job_id);
CREATE INDEX idx_sys_job_log_start_time ON public.sys_job_log(start_time DESC);

COMMENT ON TABLE  public.sys_job_log IS '定时任务执行历史表';
COMMENT ON COLUMN public.sys_job_log.trigger_type IS '0=定时触发 1=手动触发';
COMMENT ON COLUMN public.sys_job_log.status IS '0=失败 1=成功';

-- ----------------------------
-- sys_config (系统配置表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_config;
CREATE TABLE public.sys_config (
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

COMMENT ON TABLE  public.sys_config IS '系统配置表';
COMMENT ON COLUMN public.sys_config.config_type IS '1=系统内置（不可删）2=用户自定义';

-- ----------------------------
-- sys_file_config (文件存储配置表，平台级)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_file_config;
CREATE TABLE public.sys_file_config (
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

CREATE INDEX idx_file_config_type   ON public.sys_file_config(storage_type);
CREATE INDEX idx_file_config_active ON public.sys_file_config(is_active) WHERE delete_flag = 0;

COMMENT ON TABLE  public.sys_file_config IS '文件存储配置表，平台级';
COMMENT ON COLUMN public.sys_file_config.config IS '存储配置JSON，结构因 storage_type 而异';

-- ----------------------------
-- sys_file (文件记录表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_file;
CREATE TABLE public.sys_file (
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

CREATE INDEX idx_sys_file_tenant ON public.sys_file(tenant_id);
CREATE INDEX idx_sys_file_biz    ON public.sys_file(biz_type, biz_id);

COMMENT ON TABLE  public.sys_file IS '文件记录表';
COMMENT ON COLUMN public.sys_file.file_key IS '存储路径/对象Key，用于删除和访问';

-- ========================================
-- AI 智能查询模块
-- ========================================

-- ai_provider_config (AI 模型配置，可插拔)
DROP TABLE IF EXISTS public.ai_provider_config;
CREATE TABLE public.ai_provider_config (
    id           BIGSERIAL    PRIMARY KEY,
    config_name  VARCHAR(100) NOT NULL,
    provider     VARCHAR(50)  NOT NULL,          -- ollama / claude / openai
    chat_model   VARCHAR(100) NOT NULL,          -- 对话模型（如 qwen2.5:7b）
    embed_model  VARCHAR(100) NOT NULL,          -- Embedding 模型（如 nomic-embed-text）
    base_url     VARCHAR(500),                   -- 服务地址
    api_key      VARCHAR(500),                   -- API Key（本地可为空）
    is_active    SMALLINT     NOT NULL DEFAULT 0,
    config       JSONB,                          -- 扩展配置（temperature 等）
    remark       VARCHAR(500),
    create_time  TIMESTAMP,
    update_time  TIMESTAMP,
    delete_flag  SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX idx_ai_provider_active ON public.ai_provider_config(is_active) WHERE delete_flag = 0;
COMMENT ON TABLE public.ai_provider_config IS 'AI 模型提供商配置，支持热切换';

-- ai_table_catalog (数据目录 - 表级)
DROP TABLE IF EXISTS public.ai_table_catalog;
CREATE TABLE public.ai_table_catalog (
    id           BIGSERIAL    PRIMARY KEY,
    table_name   VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,          -- 中文名
    description  TEXT,                           -- 语义描述（注入 Prompt）
    is_enabled   SMALLINT     NOT NULL DEFAULT 0,
    sort_order   INT          DEFAULT 0,
    create_time  TIMESTAMP,
    update_time  TIMESTAMP
);
COMMENT ON TABLE public.ai_table_catalog IS 'AI 可查询数据目录 - 表级';

-- ai_field_catalog (数据目录 - 字段级)
DROP TABLE IF EXISTS public.ai_field_catalog;
CREATE TABLE public.ai_field_catalog (
    id           BIGSERIAL    PRIMARY KEY,
    table_id     BIGINT       NOT NULL,
    table_name   VARCHAR(100) NOT NULL,
    field_name   VARCHAR(100) NOT NULL,
    field_type   VARCHAR(50),
    display_name VARCHAR(100) NOT NULL,
    description  TEXT,                           -- 语义描述（向量化）
    is_enabled   SMALLINT     NOT NULL DEFAULT 1,
    embed_vector vector(1024),                   -- pgvector 向量
    sort_order   INT          DEFAULT 0,
    create_time  TIMESTAMP,
    update_time  TIMESTAMP
);
CREATE INDEX idx_field_catalog_table  ON public.ai_field_catalog(table_id);
CREATE INDEX idx_field_catalog_vector ON public.ai_field_catalog
    USING hnsw (embed_vector vector_cosine_ops);
COMMENT ON TABLE public.ai_field_catalog IS 'AI 可查询数据目录 - 字段级，含向量';

-- ai_conversation (对话历史)
DROP TABLE IF EXISTS public.ai_conversation;
CREATE TABLE public.ai_conversation (
    id            BIGSERIAL   PRIMARY KEY,
    session_id    VARCHAR(64) NOT NULL,
    user_id       BIGINT,
    role          VARCHAR(10) NOT NULL,           -- user / assistant
    content       TEXT        NOT NULL,
    generated_sql TEXT,
    exec_rows     INT,
    create_time   TIMESTAMP   DEFAULT NOW()
);
CREATE INDEX idx_ai_conv_session ON public.ai_conversation(session_id);
CREATE INDEX idx_ai_conv_time    ON public.ai_conversation(create_time DESC);
COMMENT ON TABLE public.ai_conversation IS 'AI 对话历史';
