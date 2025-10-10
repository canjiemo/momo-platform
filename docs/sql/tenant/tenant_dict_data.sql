-- ====================================================================================================
-- 租户模块字典数据
-- 说明：租户状态、初始化步骤、日志状态等字典数据
-- 创建时间：2025-01-09
-- ====================================================================================================

-- 切换到 public schema
SET search_path TO public;

-- ----------------------------
-- 1. 创建字典表（如果 public schema 中不存在）
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_dict_type (
  id BIGINT PRIMARY KEY,
  dict_name VARCHAR(100) NOT NULL,
  dict_type VARCHAR(100) NOT NULL UNIQUE,
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

CREATE TABLE IF NOT EXISTS public.sys_dict_data (
  id BIGINT PRIMARY KEY,
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

-- ----------------------------
-- 2. 插入租户状态字典类型
-- ----------------------------
INSERT INTO public.sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (1001, '租户状态', 'tenant_status', '租户（学校）的状态：待激活、正常、已禁用、已过期', 1, 1, 'system')
ON CONFLICT (dict_type) DO NOTHING;

-- ----------------------------
-- 3. 插入租户状态字典数据
-- ----------------------------
INSERT INTO public.sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (10001, 'tenant_status', '待激活', '0', '租户已创建但尚未激活', '', 'warning', 1, 1, 'system'),
  (10002, 'tenant_status', '正常', '1', '租户正常运行中', '', 'success', 1, 2, 'system'),
  (10003, 'tenant_status', '已禁用', '2', '租户已被管理员禁用', '', 'danger', 1, 3, 'system'),
  (10004, 'tenant_status', '已过期', '3', '租户已过期，需要续费', '', 'info', 1, 4, 'system')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 4. 插入初始化步骤类型字典
-- ----------------------------
INSERT INTO public.sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (1002, '租户初始化步骤', 'tenant_init_step', '租户 Schema 初始化过程的步骤类型', 1, 2, 'system')
ON CONFLICT (dict_type) DO NOTHING;

-- ----------------------------
-- 5. 插入初始化步骤字典数据
-- ----------------------------
INSERT INTO public.sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (10011, 'tenant_init_step', '创建Schema', 'CREATE_SCHEMA', '创建租户独立的 PostgreSQL Schema', '', 'primary', 1, 1, 'system'),
  (10012, 'tenant_init_step', '创建表结构', 'CREATE_TABLE', '在租户 Schema 中创建业务表', '', 'primary', 1, 2, 'system'),
  (10013, 'tenant_init_step', '插入基础数据', 'INSERT_DATA', '插入角色、菜单等基础数据', '', 'primary', 1, 3, 'system'),
  (10014, 'tenant_init_step', '创建管理员', 'CREATE_ADMIN', '创建租户管理员账号', '', 'primary', 1, 4, 'system'),
  (10015, 'tenant_init_step', '回滚清理', 'ROLLBACK', '初始化失败时的回滚操作', '', 'danger', 1, 5, 'system')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 6. 插入日志状态字典类型
-- ----------------------------
INSERT INTO public.sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (1003, '租户日志状态', 'tenant_log_status', '租户初始化日志的状态', 1, 3, 'system')
ON CONFLICT (dict_type) DO NOTHING;

-- ----------------------------
-- 7. 插入日志状态字典数据
-- ----------------------------
INSERT INTO public.sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (10021, 'tenant_log_status', '进行中', '0', '步骤正在执行', '', 'info', 1, 1, 'system'),
  (10022, 'tenant_log_status', '成功', '1', '步骤执行成功', '', 'success', 1, 2, 'system'),
  (10023, 'tenant_log_status', '失败', '2', '步骤执行失败', '', 'danger', 1, 3, 'system')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 验证插入结果
-- ----------------------------
SELECT 'Dictionary types inserted:' AS message, COUNT(*) AS count FROM public.sys_dict_type WHERE dict_type LIKE 'tenant%';
SELECT 'Dictionary data inserted:' AS message, COUNT(*) AS count FROM public.sys_dict_data WHERE dict_type LIKE 'tenant%';
