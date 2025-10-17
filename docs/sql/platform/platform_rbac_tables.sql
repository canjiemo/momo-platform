-- ====================================================================================================
-- 平台管理 RBAC 表结构 - Public Schema
-- 说明：此脚本用于在 public schema 中创建平台管理员的权限体系
-- 使用方式：psql -U postgres -d seer_fitness_db -f docs/sql/platform/platform_rbac_tables.sql
-- 创建时间：2025-10-17
-- ====================================================================================================

-- 设置搜索路径到 public
SET search_path TO public;

-- ----------------------------
-- Table structure for sys_user (平台管理员用户表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_user;
CREATE TABLE public.sys_user (
  id BIGINT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  real_name VARCHAR(50),
  org_id BIGINT,
  admin_flag SMALLINT DEFAULT 0,
  user_type SMALLINT DEFAULT 0,
  status SMALLINT DEFAULT 1,
  delete_flag SMALLINT DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE public.sys_user IS '平台管理员用户表（存储在 public schema）';
COMMENT ON COLUMN public.sys_user.id IS '主键ID';
COMMENT ON COLUMN public.sys_user.username IS '登录用户名';
COMMENT ON COLUMN public.sys_user.password IS '加密密码';
COMMENT ON COLUMN public.sys_user.real_name IS '真实姓名';
COMMENT ON COLUMN public.sys_user.org_id IS '所属组织ID';
COMMENT ON COLUMN public.sys_user.admin_flag IS '超级管理员标识：1是 0否';
COMMENT ON COLUMN public.sys_user.user_type IS '用户类型: 0-运维人员（平台管理员）';
COMMENT ON COLUMN public.sys_user.status IS '状态：1启用 0禁用';
COMMENT ON COLUMN public.sys_user.delete_flag IS '逻辑删除：0正常 1删除';
COMMENT ON COLUMN public.sys_user.created_by IS '创建人ID';
COMMENT ON COLUMN public.sys_user.created_at IS '创建时间';
COMMENT ON COLUMN public.sys_user.updated_by IS '更新人ID';
COMMENT ON COLUMN public.sys_user.updated_at IS '更新时间';

-- ----------------------------
-- Table structure for sys_role (平台角色表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_role;
CREATE TABLE public.sys_role (
  id BIGINT PRIMARY KEY,
  role_name VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  status SMALLINT DEFAULT 1,
  delete_flag SMALLINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE public.sys_role IS '平台角色表（存储在 public schema）';
COMMENT ON COLUMN public.sys_role.id IS '主键ID';
COMMENT ON COLUMN public.sys_role.role_name IS '角色名称';
COMMENT ON COLUMN public.sys_role.description IS '角色描述';
COMMENT ON COLUMN public.sys_role.status IS '状态：1启用 0禁用';
COMMENT ON COLUMN public.sys_role.delete_flag IS '逻辑删除：0正常 1删除';
COMMENT ON COLUMN public.sys_role.created_at IS '创建时间';
COMMENT ON COLUMN public.sys_role.updated_at IS '更新时间';

-- ----------------------------
-- Table structure for sys_menu (平台菜单表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_menu;
CREATE TABLE public.sys_menu (
  id BIGINT PRIMARY KEY,
  menu_name VARCHAR(50) NOT NULL,
  path VARCHAR(255),
  parent_id BIGINT DEFAULT 0,
  type SMALLINT DEFAULT 0,
  permission VARCHAR(100),
  icon VARCHAR(100),
  sort_order INT DEFAULT 0,
  status SMALLINT DEFAULT 1,
  delete_flag SMALLINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_public_menu_parent_id ON public.sys_menu(parent_id);
CREATE INDEX idx_public_menu_permission ON public.sys_menu(permission);
CREATE INDEX idx_public_menu_status_delete ON public.sys_menu(status, delete_flag);

COMMENT ON TABLE public.sys_menu IS '平台菜单表（含权限字符和排序）';
COMMENT ON COLUMN public.sys_menu.id IS '主键ID';
COMMENT ON COLUMN public.sys_menu.menu_name IS '菜单名称';
COMMENT ON COLUMN public.sys_menu.path IS '前端路由路径或接口路径';
COMMENT ON COLUMN public.sys_menu.parent_id IS '父菜单ID，0为顶级菜单';
COMMENT ON COLUMN public.sys_menu.type IS '类型：0目录 1菜单 2按钮';
COMMENT ON COLUMN public.sys_menu.permission IS '权限字符，例如 tenant:create';
COMMENT ON COLUMN public.sys_menu.icon IS '菜单图标';
COMMENT ON COLUMN public.sys_menu.sort_order IS '排序字段，数值越小越靠前';
COMMENT ON COLUMN public.sys_menu.status IS '状态：1启用 0禁用';
COMMENT ON COLUMN public.sys_menu.delete_flag IS '逻辑删除：0正常 1删除';
COMMENT ON COLUMN public.sys_menu.created_at IS '创建时间';
COMMENT ON COLUMN public.sys_menu.updated_at IS '更新时间';

-- ----------------------------
-- Table structure for sys_role_menu (平台角色菜单关联表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_role_menu;
CREATE TABLE public.sys_role_menu (
  id BIGINT PRIMARY KEY,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_public_role_menu_role ON public.sys_role_menu(role_id);
CREATE INDEX idx_public_role_menu_menu ON public.sys_role_menu(menu_id);

COMMENT ON TABLE public.sys_role_menu IS '平台角色-菜单关联表';
COMMENT ON COLUMN public.sys_role_menu.id IS '主键ID';
COMMENT ON COLUMN public.sys_role_menu.role_id IS '角色ID(关联sys_role.id)';
COMMENT ON COLUMN public.sys_role_menu.menu_id IS '菜单ID(关联sys_menu.id)';
COMMENT ON COLUMN public.sys_role_menu.created_at IS '创建时间';

-- ----------------------------
-- Table structure for sys_user_role (平台用户角色关联表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_user_role;
CREATE TABLE public.sys_user_role (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_public_user_role_user ON public.sys_user_role(user_id);
CREATE INDEX idx_public_user_role_role ON public.sys_user_role(role_id);

COMMENT ON TABLE public.sys_user_role IS '平台用户-角色关联表';
COMMENT ON COLUMN public.sys_user_role.id IS '主键ID';
COMMENT ON COLUMN public.sys_user_role.user_id IS '用户ID(关联sys_user.id)';
COMMENT ON COLUMN public.sys_user_role.role_id IS '角色ID(关联sys_role.id)';
COMMENT ON COLUMN public.sys_user_role.created_at IS '创建时间';

-- ----------------------------
-- Table structure for sys_operation_log (平台操作日志表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_operation_log;
CREATE TABLE public.sys_operation_log (
  id BIGINT PRIMARY KEY,
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
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_public_op_log_user ON public.sys_operation_log(user_id);
CREATE INDEX idx_public_op_log_type ON public.sys_operation_log(operation_type);
CREATE INDEX idx_public_op_log_module ON public.sys_operation_log(module_name);
CREATE INDEX idx_public_op_log_time ON public.sys_operation_log(created_at);

COMMENT ON TABLE public.sys_operation_log IS '平台操作日志表';
COMMENT ON COLUMN public.sys_operation_log.id IS '主键ID';
COMMENT ON COLUMN public.sys_operation_log.user_id IS '操作用户ID';
COMMENT ON COLUMN public.sys_operation_log.username IS '操作用户名';
COMMENT ON COLUMN public.sys_operation_log.real_name IS '操作用户真实姓名';
COMMENT ON COLUMN public.sys_operation_log.operation_type IS '操作类型：CREATE/UPDATE/DELETE/QUERY/LOGIN/LOGOUT';
COMMENT ON COLUMN public.sys_operation_log.module_name IS '操作模块：tenant/platform_user等';
COMMENT ON COLUMN public.sys_operation_log.business_id IS '业务数据ID';
COMMENT ON COLUMN public.sys_operation_log.business_name IS '业务数据名称';
COMMENT ON COLUMN public.sys_operation_log.operation_desc IS '操作描述';
COMMENT ON COLUMN public.sys_operation_log.request_method IS '请求方式：GET/POST/PUT/DELETE';
COMMENT ON COLUMN public.sys_operation_log.request_url IS '请求URL';
COMMENT ON COLUMN public.sys_operation_log.request_params IS '请求参数（JSON格式）';
COMMENT ON COLUMN public.sys_operation_log.response_data IS '响应数据（JSON格式，可选）';
COMMENT ON COLUMN public.sys_operation_log.ip_address IS '操作IP地址';
COMMENT ON COLUMN public.sys_operation_log.user_agent IS '用户代理信息';
COMMENT ON COLUMN public.sys_operation_log.operation_result IS '操作结果：1成功 0失败';
COMMENT ON COLUMN public.sys_operation_log.error_message IS '错误信息（操作失败时记录）';
COMMENT ON COLUMN public.sys_operation_log.execution_time IS '执行耗时（毫秒）';
COMMENT ON COLUMN public.sys_operation_log.created_at IS '操作时间';

-- ----------------------------
-- 验证表创建
-- ----------------------------
SELECT
    tablename,
    schemaname
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename LIKE 'sys_%'
ORDER BY tablename;
