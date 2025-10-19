-- ====================================================================================================
-- Flyway Baseline Migration - Tenant Schema V1.0.0
-- 说明：租户Schema基线版本，由Flyway负责创建Schema和表结构
-- 版本：1.0.0
-- 创建时间：2025-10-18
-- 适用对象：所有新创建的租户Schema
-- ====================================================================================================
-- 架构说明：
--   1. Flyway负责创建Schema和所有表结构
--   2. TenantSchemaService只负责数据初始化（创建管理员等）
--   3. 所有表使用BIGINT主键（雪花算法生成）
--   4. 所有表支持软删除（delete_flag字段）
-- ====================================================================================================

-- ----------------------------
-- Table structure for sys_user
-- 用户表：存储租户的用户信息
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user (
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

COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN sys_user.username IS '登录用户名（唯一）';
COMMENT ON COLUMN sys_user.password IS '加密密码（BCrypt）';
COMMENT ON COLUMN sys_user.real_name IS '真实姓名';
COMMENT ON COLUMN sys_user.org_id IS '所属组织ID';
COMMENT ON COLUMN sys_user.admin_flag IS '超级管理员标记：1-是 0-否（不可删除）';
COMMENT ON COLUMN sys_user.user_type IS '用户类型：0-运维人员 1-教师 2-学生';
COMMENT ON COLUMN sys_user.status IS '状态：1-启用 0-禁用';
COMMENT ON COLUMN sys_user.delete_flag IS '逻辑删除：0-正常 1-已删除';

-- ----------------------------
-- Table structure for sys_role
-- 角色表：存储租户的角色信息
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY,
  role_name VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  platform_role_id BIGINT DEFAULT NULL,
  status SMALLINT DEFAULT 1,
  delete_flag SMALLINT DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON COLUMN sys_role.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN sys_role.role_name IS '角色名称';
COMMENT ON COLUMN sys_role.description IS '角色描述';
COMMENT ON COLUMN sys_role.platform_role_id IS '平台角色ID（从平台同步的角色关联平台角色ID，自建角色为NULL）';
COMMENT ON COLUMN sys_role.status IS '状态：1-启用 0-禁用';
COMMENT ON COLUMN sys_role.delete_flag IS '逻辑删除：0-正常 1-已删除';

-- ----------------------------
-- Table structure for sys_menu
-- 菜单表：存储租户的菜单权限（由平台分配）
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT PRIMARY KEY,
  menu_name VARCHAR(50) NOT NULL,
  parent_id BIGINT DEFAULT 0,
  type SMALLINT DEFAULT 0,
  path VARCHAR(255),
  permission VARCHAR(100),
  icon VARCHAR(100),
  sort_order INT DEFAULT 0,
  platform_menu_id BIGINT NOT NULL,
  status SMALLINT DEFAULT 1,
  delete_flag SMALLINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_menu IS '菜单表（由平台分配）';
COMMENT ON COLUMN sys_menu.id IS '主键ID（雪花算法，租户独立生成）';
COMMENT ON COLUMN sys_menu.menu_name IS '菜单名称';
COMMENT ON COLUMN sys_menu.parent_id IS '父菜单ID，0为顶级';
COMMENT ON COLUMN sys_menu.type IS '类型：0-目录 1-菜单 2-按钮';
COMMENT ON COLUMN sys_menu.path IS '前端路由路径';
COMMENT ON COLUMN sys_menu.permission IS '权限标识（如：user:view）';
COMMENT ON COLUMN sys_menu.icon IS '图标';
COMMENT ON COLUMN sys_menu.sort_order IS '排序';
COMMENT ON COLUMN sys_menu.platform_menu_id IS '关联平台菜单ID（用于同步更新）';
COMMENT ON COLUMN sys_menu.status IS '状态：1-启用 0-禁用（租户可控）';
COMMENT ON COLUMN sys_menu.delete_flag IS '逻辑删除：0-正常 1-已删除';

-- ----------------------------
-- Table structure for sys_user_role
-- 用户角色关联表：多对多关系
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(user_id, role_id)
);

COMMENT ON TABLE sys_user_role IS '用户角色关联表';
COMMENT ON COLUMN sys_user_role.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN sys_user_role.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_role.role_id IS '角色ID';

-- ----------------------------
-- Table structure for sys_role_menu
-- 角色菜单关联表：多对多关系
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_role_menu (
  id BIGINT PRIMARY KEY,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(role_id, menu_id)
);

COMMENT ON TABLE sys_role_menu IS '角色菜单关联表';
COMMENT ON COLUMN sys_role_menu.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN sys_role_menu.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_menu.menu_id IS '菜单ID（本schema的sys_menu.id）';

-- ----------------------------
-- Table structure for sys_organization
-- 组织架构表：树形结构
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_organization (
  id BIGINT PRIMARY KEY,
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

COMMENT ON TABLE sys_organization IS '组织架构表';
COMMENT ON COLUMN sys_organization.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN sys_organization.org_code IS '组织编码';
COMMENT ON COLUMN sys_organization.org_name IS '组织名称';
COMMENT ON COLUMN sys_organization.parent_id IS '父组织ID，0为顶级';
COMMENT ON COLUMN sys_organization.sort_order IS '排序';
COMMENT ON COLUMN sys_organization.leader_id IS '负责人用户ID';
COMMENT ON COLUMN sys_organization.contact_phone IS '联系电话';
COMMENT ON COLUMN sys_organization.email IS '邮箱';
COMMENT ON COLUMN sys_organization.address IS '办公地址';
COMMENT ON COLUMN sys_organization.description IS '组织描述';
COMMENT ON COLUMN sys_organization.status IS '状态：1-启用 0-禁用';
COMMENT ON COLUMN sys_organization.delete_flag IS '逻辑删除：0-正常 1-已删除';

-- ----------------------------
-- Table structure for sys_operation_log
-- 操作日志表：记录用户操作行为
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_operation_log (
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

COMMENT ON TABLE sys_operation_log IS '操作日志表';
COMMENT ON COLUMN sys_operation_log.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN sys_operation_log.user_id IS '操作用户ID';
COMMENT ON COLUMN sys_operation_log.username IS '操作用户名';
COMMENT ON COLUMN sys_operation_log.real_name IS '操作用户真实姓名';
COMMENT ON COLUMN sys_operation_log.operation_type IS '操作类型：CREATE/UPDATE/DELETE/QUERY/LOGIN/LOGOUT';
COMMENT ON COLUMN sys_operation_log.module_name IS '操作模块';
COMMENT ON COLUMN sys_operation_log.business_id IS '业务数据ID';
COMMENT ON COLUMN sys_operation_log.business_name IS '业务数据名称';
COMMENT ON COLUMN sys_operation_log.operation_desc IS '操作描述';
COMMENT ON COLUMN sys_operation_log.request_method IS '请求方式：GET/POST/PUT/DELETE';
COMMENT ON COLUMN sys_operation_log.request_url IS '请求URL';
COMMENT ON COLUMN sys_operation_log.request_params IS '请求参数（JSONB）';
COMMENT ON COLUMN sys_operation_log.response_data IS '响应数据（JSONB）';
COMMENT ON COLUMN sys_operation_log.ip_address IS '操作IP地址';
COMMENT ON COLUMN sys_operation_log.user_agent IS '用户代理信息';
COMMENT ON COLUMN sys_operation_log.operation_result IS '操作结果：1-成功 0-失败';
COMMENT ON COLUMN sys_operation_log.error_message IS '错误信息';
COMMENT ON COLUMN sys_operation_log.execution_time IS '执行耗时（毫秒）';
COMMENT ON COLUMN sys_operation_log.created_at IS '操作时间';
