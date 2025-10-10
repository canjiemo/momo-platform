-- ====================================================================================================
-- 租户模块核心表
-- 说明：用于多租户（多学校）隔离，基于 PostgreSQL Schema 隔离方案
-- 创建时间：2025-01-09
-- ====================================================================================================

-- ----------------------------
-- Table structure for sys_tenant (租户主表)
-- 注意：此表存储在 public schema，作为全局租户注册表
-- ----------------------------
DROP TABLE IF EXISTS public.sys_tenant;
CREATE TABLE public.sys_tenant (
  id BIGSERIAL PRIMARY KEY,
  tenant_code VARCHAR(50) NOT NULL UNIQUE,
  tenant_name VARCHAR(100) NOT NULL,
  schema_name VARCHAR(50) NOT NULL UNIQUE,
  admin_username VARCHAR(50),
  admin_real_name VARCHAR(50),
  contact_phone VARCHAR(20),
  contact_email VARCHAR(100),
  address VARCHAR(200),
  description TEXT,
  status SMALLINT DEFAULT 0,
  activated_at TIMESTAMP,
  expired_at TIMESTAMP,
  max_users INT DEFAULT 1000,
  max_storage_gb INT DEFAULT 100,
  delete_flag SMALLINT DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_sys_tenant_code ON public.sys_tenant(tenant_code);
CREATE INDEX idx_sys_tenant_schema ON public.sys_tenant(schema_name);
CREATE INDEX idx_sys_tenant_status ON public.sys_tenant(status, delete_flag);
CREATE INDEX idx_sys_tenant_expired ON public.sys_tenant(expired_at);

-- 添加表和字段注释
COMMENT ON TABLE public.sys_tenant IS '租户表（学校表），存储在 public schema，作为全局租户注册表';
COMMENT ON COLUMN public.sys_tenant.id IS '主键ID';
COMMENT ON COLUMN public.sys_tenant.tenant_code IS '租户编码，全局唯一，例如：SCHOOL_001';
COMMENT ON COLUMN public.sys_tenant.tenant_name IS '租户名称（学校名称），例如：XX中学';
COMMENT ON COLUMN public.sys_tenant.schema_name IS 'PostgreSQL Schema 名称，全局唯一，例如：school_001';
COMMENT ON COLUMN public.sys_tenant.admin_username IS '管理员用户名';
COMMENT ON COLUMN public.sys_tenant.admin_real_name IS '管理员真实姓名';
COMMENT ON COLUMN public.sys_tenant.contact_phone IS '联系电话';
COMMENT ON COLUMN public.sys_tenant.contact_email IS '联系邮箱';
COMMENT ON COLUMN public.sys_tenant.address IS '学校地址';
COMMENT ON COLUMN public.sys_tenant.description IS '租户描述';
COMMENT ON COLUMN public.sys_tenant.status IS '状态：0-待激活 1-正常 2-已禁用 3-已过期';
COMMENT ON COLUMN public.sys_tenant.activated_at IS '激活时间';
COMMENT ON COLUMN public.sys_tenant.expired_at IS '过期时间，NULL表示永不过期';
COMMENT ON COLUMN public.sys_tenant.max_users IS '最大用户数限制';
COMMENT ON COLUMN public.sys_tenant.max_storage_gb IS '最大存储空间限制（GB）';
COMMENT ON COLUMN public.sys_tenant.delete_flag IS '逻辑删除：0正常 1删除';
COMMENT ON COLUMN public.sys_tenant.created_by IS '创建人ID';
COMMENT ON COLUMN public.sys_tenant.created_at IS '创建时间';
COMMENT ON COLUMN public.sys_tenant.updated_by IS '更新人ID';
COMMENT ON COLUMN public.sys_tenant.updated_at IS '更新时间';


-- ----------------------------
-- Table structure for sys_tenant_init_log (租户初始化日志表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_tenant_init_log;
CREATE TABLE public.sys_tenant_init_log (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  step_name VARCHAR(50) NOT NULL,
  step_type VARCHAR(20) NOT NULL,
  status SMALLINT DEFAULT 0,
  sql_script TEXT,
  error_message TEXT,
  execution_time INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_tenant_init_log_tenant_id ON public.sys_tenant_init_log(tenant_id);
CREATE INDEX idx_tenant_init_log_status ON public.sys_tenant_init_log(status);
CREATE INDEX idx_tenant_init_log_created ON public.sys_tenant_init_log(created_at);

-- 添加表和字段注释
COMMENT ON TABLE public.sys_tenant_init_log IS '租户初始化日志表，记录 Schema 创建和初始化过程';
COMMENT ON COLUMN public.sys_tenant_init_log.id IS '主键ID';
COMMENT ON COLUMN public.sys_tenant_init_log.tenant_id IS '租户ID（关联 sys_tenant.id）';
COMMENT ON COLUMN public.sys_tenant_init_log.step_name IS '初始化步骤名称，例如：创建Schema、初始化表结构、创建管理员';
COMMENT ON COLUMN public.sys_tenant_init_log.step_type IS '步骤类型：CREATE_SCHEMA, CREATE_TABLE, INSERT_DATA, CREATE_ADMIN';
COMMENT ON COLUMN public.sys_tenant_init_log.status IS '状态：0-进行中 1-成功 2-失败';
COMMENT ON COLUMN public.sys_tenant_init_log.sql_script IS '执行的 SQL 脚本（可选，用于调试）';
COMMENT ON COLUMN public.sys_tenant_init_log.error_message IS '错误信息（失败时记录）';
COMMENT ON COLUMN public.sys_tenant_init_log.execution_time IS '执行耗时（毫秒）';
COMMENT ON COLUMN public.sys_tenant_init_log.created_at IS '创建时间';


-- ----------------------------
-- 插入说明信息
-- ----------------------------
-- 使用说明：
-- 1. 此 SQL 脚本创建租户核心表，存储在 public schema
-- 2. sys_tenant 表记录所有租户（学校）的基本信息
-- 3. sys_tenant_init_log 表记录每个租户的 Schema 初始化过程
-- 4. 状态字典数据需要在 tenant_dict_data.sql 中定义
-- 5. 每个租户的业务数据存储在独立的 Schema 中（例如：school_001, school_002）
