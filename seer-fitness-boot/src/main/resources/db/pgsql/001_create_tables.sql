-- ====================================================================================================
-- Seer Fitness Edu - 统一建表脚本 (tenant_id 模式)
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
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_tenant_code ON public.sys_tenant(tenant_code);
CREATE INDEX idx_sys_tenant_status ON public.sys_tenant(status, delete_flag);

COMMENT ON TABLE public.sys_tenant IS '租户表（学校表），平台级';
COMMENT ON COLUMN public.sys_tenant.id IS '主键ID';
COMMENT ON COLUMN public.sys_tenant.tenant_code IS '租户编码，全局唯一';
COMMENT ON COLUMN public.sys_tenant.tenant_name IS '租户名称（学校名称），创建后不可修改';
COMMENT ON COLUMN public.sys_tenant.real_name IS '管理员真实姓名';
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
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
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
  description VARCHAR(255),
  status SMALLINT DEFAULT 1,
  delete_flag SMALLINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_role_tenant ON public.sys_role(tenant_id);
CREATE INDEX idx_sys_role_status ON public.sys_role(status, delete_flag);

COMMENT ON TABLE public.sys_role IS '角色表';
COMMENT ON COLUMN public.sys_role.tenant_id IS '租户ID，NULL表示平台角色模板';

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
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_menu_tenant ON public.sys_menu(tenant_id);
CREATE INDEX idx_sys_menu_parent_id ON public.sys_menu(parent_id);
CREATE INDEX idx_sys_menu_permission ON public.sys_menu(permission);
CREATE INDEX idx_sys_menu_status_delete ON public.sys_menu(status, delete_flag);

COMMENT ON TABLE public.sys_menu IS '菜单表';
COMMENT ON COLUMN public.sys_menu.tenant_id IS '租户ID，NULL表示平台菜单模板';
COMMENT ON COLUMN public.sys_menu.type IS '类型：0目录 1菜单 2按钮';
COMMENT ON COLUMN public.sys_menu.icon IS 'Ant Design 图标名，如 DashboardOutlined、UserOutlined';

-- ----------------------------
-- sys_role_menu (角色菜单关联表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_role_menu;
CREATE TABLE public.sys_role_menu (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_role_menu_tenant ON public.sys_role_menu(tenant_id);
CREATE INDEX idx_sys_role_menu_role ON public.sys_role_menu(role_id);
CREATE INDEX idx_sys_role_menu_menu ON public.sys_role_menu(menu_id);

COMMENT ON TABLE public.sys_role_menu IS '角色-菜单关联表';
COMMENT ON COLUMN public.sys_role_menu.tenant_id IS '租户ID，NULL表示平台角色菜单';

-- ----------------------------
-- sys_user_role (用户角色关联表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_user_role;
CREATE TABLE public.sys_user_role (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_user_role_tenant ON public.sys_user_role(tenant_id);
CREATE INDEX idx_sys_user_role_user ON public.sys_user_role(user_id);
CREATE INDEX idx_sys_user_role_role ON public.sys_user_role(role_id);

COMMENT ON TABLE public.sys_user_role IS '用户-角色关联表';
COMMENT ON COLUMN public.sys_user_role.tenant_id IS '租户ID';

-- ----------------------------
-- sys_organization (组织架构表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_organization;
CREATE TABLE public.sys_organization (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
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
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_org_tenant ON public.sys_organization(tenant_id);
CREATE INDEX idx_sys_org_parent ON public.sys_organization(parent_id);
CREATE INDEX idx_sys_org_status ON public.sys_organization(status, delete_flag);

COMMENT ON TABLE public.sys_organization IS '组织架构表';
COMMENT ON COLUMN public.sys_organization.tenant_id IS '租户ID，必填';

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
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_op_log_tenant ON public.sys_operation_log(tenant_id);
CREATE INDEX idx_op_log_user ON public.sys_operation_log(user_id);
CREATE INDEX idx_op_log_type ON public.sys_operation_log(operation_type);
CREATE INDEX idx_op_log_module ON public.sys_operation_log(module_name);
CREATE INDEX idx_op_log_time ON public.sys_operation_log(created_at);

COMMENT ON TABLE public.sys_operation_log IS '操作日志表';
COMMENT ON COLUMN public.sys_operation_log.tenant_id IS '租户ID，NULL表示平台操作日志';

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
COMMENT ON COLUMN public.sys_dict_type.tenant_id IS '租户ID，NULL表示平台字典';

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
COMMENT ON COLUMN public.sys_dict_data.tenant_id IS '租户ID，NULL表示平台字典数据';
