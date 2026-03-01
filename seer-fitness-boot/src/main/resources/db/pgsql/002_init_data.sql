-- ====================================================================================================
-- Seer Fitness Edu - 平台初始数据
-- tenant_id = NULL 表示平台级数据
-- 执行前提：已执行 001_create_tables.sql
-- ====================================================================================================

-- ========================================
-- 第一部分: 平台菜单 (tenant_id=NULL)
-- ========================================

-- 平台管理(目录)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES (10000, NULL, '平台管理', 0, 0, '/platform', NULL, 'CloudServerOutlined', 1, 1, 0, NOW(), NOW());

-- 租户管理(菜单 + 按钮)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (10100, NULL, '租户管理', 10000, 1, '/platform/tenant', NULL, 'ShopOutlined', 1, 1, 0, NOW(), NOW()),
    (10101, NULL, '查看租户', 10100, 2, NULL, 'tenant:view', NULL, 1, 1, 0, NOW(), NOW()),
    (10102, NULL, '创建租户', 10100, 2, NULL, 'tenant:create', NULL, 2, 1, 0, NOW(), NOW()),
    (10103, NULL, '更新租户', 10100, 2, NULL, 'tenant:update', NULL, 3, 1, 0, NOW(), NOW()),
    (10104, NULL, '禁用租户', 10100, 2, NULL, 'tenant:disable', NULL, 4, 1, 0, NOW(), NOW()),
    (10105, NULL, '启用租户', 10100, 2, NULL, 'tenant:enable', NULL, 5, 1, 0, NOW(), NOW());

-- 租户角色管理(菜单 + 按钮)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (10200, NULL, '租户角色', 10000, 1, '/platform/role', NULL, 'TeamOutlined', 2, 1, 0, NOW(), NOW()),
    (10201, NULL, '查看角色', 10200, 2, NULL, 'platform:role:view', NULL, 1, 1, 0, NOW(), NOW()),
    (10202, NULL, '创建角色', 10200, 2, NULL, 'platform:role:create', NULL, 2, 1, 0, NOW(), NOW()),
    (10203, NULL, '更新角色', 10200, 2, NULL, 'platform:role:update', NULL, 3, 1, 0, NOW(), NOW()),
    (10204, NULL, '删除角色', 10200, 2, NULL, 'platform:role:delete', NULL, 4, 1, 0, NOW(), NOW()),
    (10205, NULL, '分配菜单', 10200, 2, NULL, 'platform:role:assign', NULL, 5, 1, 0, NOW(), NOW());

-- 菜单授权(菜单 + 按钮)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (10400, NULL, '菜单授权', 10000, 1, '/platform/menu-auth', NULL, 'BlockOutlined', 4, 1, 0, NOW(), NOW()),
    (10401, NULL, '查看授权', 10400, 2, NULL, 'platform:role:view', NULL, 1, 1, 0, NOW(), NOW()),
    (10402, NULL, '配置授权', 10400, 2, NULL, 'platform:role:assign', NULL, 2, 1, 0, NOW(), NOW());

-- 平台用户管理(菜单 + 按钮)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (10300, NULL, '平台用户', 10000, 1, '/platform/user', NULL, 'UserOutlined', 3, 1, 0, NOW(), NOW()),
    (10301, NULL, '查看用户', 10300, 2, NULL, 'platform:user:view', NULL, 1, 1, 0, NOW(), NOW()),
    (10302, NULL, '创建用户', 10300, 2, NULL, 'platform:user:create', NULL, 2, 1, 0, NOW(), NOW()),
    (10303, NULL, '更新用户', 10300, 2, NULL, 'platform:user:update', NULL, 3, 1, 0, NOW(), NOW()),
    (10304, NULL, '删除用户', 10300, 2, NULL, 'platform:user:delete', NULL, 4, 1, 0, NOW(), NOW());

-- 平台日志(菜单 + 按钮)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (10500, NULL, '操作日志', 10000, 1, '/platform/log', NULL, 'FileTextOutlined', 5, 1, 0, NOW(), NOW()),
    (10501, NULL, '查看日志', 10500, 2, NULL, 'platform:log:view', NULL, 1, 1, 0, NOW(), NOW()),
    (10502, NULL, '导出日志', 10500, 2, NULL, 'platform:log:export', NULL, 2, 1, 0, NOW(), NOW());

-- 平台组织管理(菜单 + 按钮)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (10600, NULL, '平台组织', 10000, 1, '/platform/organization', NULL, 'ApartmentOutlined', 6, 1, 0, NOW(), NOW()),
    (10601, NULL, '查看组织', 10600, 2, NULL, 'platform:org:view', NULL, 1, 1, 0, NOW(), NOW()),
    (10602, NULL, '创建组织', 10600, 2, NULL, 'platform:org:create', NULL, 2, 1, 0, NOW(), NOW()),
    (10603, NULL, '更新组织', 10600, 2, NULL, 'platform:org:update', NULL, 3, 1, 0, NOW(), NOW()),
    (10604, NULL, '删除组织', 10600, 2, NULL, 'platform:org:delete', NULL, 4, 1, 0, NOW(), NOW());

-- ========================================
-- 租户菜单模板 (tenant_id=NULL，供租户同步)
-- ========================================

-- 系统管理(目录)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES (1000, NULL, '系统管理', 0, 0, '/system', NULL, 'SettingOutlined', 1, 1, 0, NOW(), NOW());

-- 用户管理(菜单 + 6个按钮)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (1100, NULL, '用户管理', 1000, 1, '/system/user', NULL, 'UserOutlined', 1, 1, 0, NOW(), NOW()),
    (1101, NULL, '查看用户', 1100, 2, NULL, 'user:view', NULL, 1, 1, 0, NOW(), NOW()),
    (1102, NULL, '创建用户', 1100, 2, NULL, 'user:create', NULL, 2, 1, 0, NOW(), NOW()),
    (1103, NULL, '更新用户', 1100, 2, NULL, 'user:update', NULL, 3, 1, 0, NOW(), NOW()),
    (1104, NULL, '删除用户', 1100, 2, NULL, 'user:delete', NULL, 4, 1, 0, NOW(), NOW()),
    (1105, NULL, '初始化密码', 1100, 2, NULL, 'user:init-password', NULL, 5, 1, 0, NOW(), NOW()),
    (1106, NULL, '重置密码', 1100, 2, NULL, 'user:reset-password', NULL, 6, 1, 0, NOW(), NOW());

-- 角色管理(菜单 + 5个按钮)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (1200, NULL, '角色管理', 1000, 1, '/system/role', NULL, 'SafetyOutlined', 2, 1, 0, NOW(), NOW()),
    (1201, NULL, '查看角色', 1200, 2, NULL, 'role:view', NULL, 1, 1, 0, NOW(), NOW()),
    (1202, NULL, '创建角色', 1200, 2, NULL, 'role:create', NULL, 2, 1, 0, NOW(), NOW()),
    (1203, NULL, '更新角色', 1200, 2, NULL, 'role:update', NULL, 3, 1, 0, NOW(), NOW()),
    (1204, NULL, '删除角色', 1200, 2, NULL, 'role:delete', NULL, 4, 1, 0, NOW(), NOW()),
    (1205, NULL, '分配权限', 1200, 2, NULL, 'role:assign', NULL, 5, 1, 0, NOW(), NOW());

-- 菜单管理(菜单 + 4个按钮) — 挂在平台管理(10000)下，菜单由平台统一定义，租户不参与管理
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (1300, NULL, '菜单管理', 10000, 1, '/platform/menu', NULL, 'MenuOutlined', 3, 1, 0, NOW(), NOW()),
    (1301, NULL, '查看菜单', 1300, 2, NULL, 'menu:view', NULL, 1, 1, 0, NOW(), NOW()),
    (1302, NULL, '创建菜单', 1300, 2, NULL, 'menu:create', NULL, 2, 1, 0, NOW(), NOW()),
    (1303, NULL, '更新菜单', 1300, 2, NULL, 'menu:update', NULL, 3, 1, 0, NOW(), NOW()),
    (1304, NULL, '删除菜单', 1300, 2, NULL, 'menu:delete', NULL, 4, 1, 0, NOW(), NOW());

-- 组织管理(菜单 + 4个按钮)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (1400, NULL, '组织管理', 1000, 1, '/system/organization', NULL, 'ApartmentOutlined', 4, 1, 0, NOW(), NOW()),
    (1401, NULL, '查看组织', 1400, 2, NULL, 'organization:view', NULL, 1, 1, 0, NOW(), NOW()),
    (1402, NULL, '创建组织', 1400, 2, NULL, 'organization:create', NULL, 2, 1, 0, NOW(), NOW()),
    (1403, NULL, '更新组织', 1400, 2, NULL, 'organization:update', NULL, 3, 1, 0, NOW(), NOW()),
    (1404, NULL, '删除组织', 1400, 2, NULL, 'organization:delete', NULL, 4, 1, 0, NOW(), NOW());

-- 日志管理(目录)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES (2000, NULL, '日志管理', 0, 0, '/log', NULL, 'AuditOutlined', 2, 1, 0, NOW(), NOW());

-- 操作日志(菜单 + 3个按钮)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (2100, NULL, '操作日志', 2000, 1, '/log/operation', NULL, 'HistoryOutlined', 1, 1, 0, NOW(), NOW()),
    (2101, NULL, '查看日志', 2100, 2, NULL, 'operation_log:view', NULL, 1, 1, 0, NOW(), NOW()),
    (2102, NULL, '删除日志', 2100, 2, NULL, 'operation_log:delete', NULL, 2, 1, 0, NOW(), NOW()),
    (2103, NULL, '导出日志', 2100, 2, NULL, 'operation_log:export', NULL, 3, 1, 0, NOW(), NOW());

-- 数据字典(目录)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES (3000, NULL, '数据字典', 0, 0, '/dict', NULL, 'BookOutlined', 3, 1, 0, NOW(), NOW());

-- 字典类型(菜单 + 4个按钮)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (3100, NULL, '字典类型', 3000, 1, '/dict/type', NULL, 'DatabaseOutlined', 1, 1, 0, NOW(), NOW()),
    (3101, NULL, '查看字典类型', 3100, 2, NULL, 'dict:type:view', NULL, 1, 1, 0, NOW(), NOW()),
    (3102, NULL, '创建字典类型', 3100, 2, NULL, 'dict:type:create', NULL, 2, 1, 0, NOW(), NOW()),
    (3103, NULL, '更新字典类型', 3100, 2, NULL, 'dict:type:update', NULL, 3, 1, 0, NOW(), NOW()),
    (3104, NULL, '删除字典类型', 3100, 2, NULL, 'dict:type:delete', NULL, 4, 1, 0, NOW(), NOW());

-- 字典数据(菜单 + 4个按钮)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (3200, NULL, '字典数据', 3000, 1, '/dict/data', NULL, 'UnorderedListOutlined', 2, 1, 0, NOW(), NOW()),
    (3201, NULL, '查看字典数据', 3200, 2, NULL, 'dict:data:view', NULL, 1, 1, 0, NOW(), NOW()),
    (3202, NULL, '创建字典数据', 3200, 2, NULL, 'dict:data:create', NULL, 2, 1, 0, NOW(), NOW()),
    (3203, NULL, '更新字典数据', 3200, 2, NULL, 'dict:data:update', NULL, 3, 1, 0, NOW(), NOW()),
    (3204, NULL, '删除字典数据', 3200, 2, NULL, 'dict:data:delete', NULL, 4, 1, 0, NOW(), NOW());

-- ========================================
-- 第二部分: 平台角色 (tenant_id=NULL)
-- ========================================

-- 平台超级管理员
INSERT INTO public.sys_role (id, tenant_id, role_name, role_code, description, status, delete_flag, created_at, updated_at)
VALUES (1, NULL, '平台超级管理员', 'PLATFORM_ADMIN', '拥有平台所有管理权限', 1, 0, NOW(), NOW());

-- 租户管理员模板（供租户同步）
INSERT INTO public.sys_role (id, tenant_id, role_name, role_code, description, status, delete_flag, created_at, updated_at)
VALUES (100, NULL, '系统管理员', 'TENANT_ADMIN', '拥有所有系统管理权限', 1, 0, NOW(), NOW());

-- ========================================
-- 第三部分: 平台角色-菜单关联 (tenant_id=NULL)
-- ========================================

-- 平台超级管理员(id=1) -> 所有平台菜单（含目录、菜单、按钮节点）
INSERT INTO public.sys_role_menu (tenant_id, role_id, menu_id) VALUES
    -- 目录节点 (type=0)
    (NULL, 1, 10000),
    -- 菜单节点 (type=1)
    (NULL, 1, 10100), (NULL, 1, 10200), (NULL, 1, 1300), (NULL, 1, 10300), (NULL, 1, 10400), (NULL, 1, 10500), (NULL, 1, 10600),
    -- 按钮节点 (type=2)
    (NULL, 1, 10101), (NULL, 1, 10102), (NULL, 1, 10103), (NULL, 1, 10104), (NULL, 1, 10105),
    (NULL, 1, 10201), (NULL, 1, 10202), (NULL, 1, 10203), (NULL, 1, 10204), (NULL, 1, 10205),
    (NULL, 1, 1301), (NULL, 1, 1302), (NULL, 1, 1303), (NULL, 1, 1304),
    (NULL, 1, 10301), (NULL, 1, 10302), (NULL, 1, 10303), (NULL, 1, 10304),
    (NULL, 1, 10401), (NULL, 1, 10402),
    (NULL, 1, 10501), (NULL, 1, 10502),
    (NULL, 1, 10601), (NULL, 1, 10602), (NULL, 1, 10603), (NULL, 1, 10604);

-- 系统管理员模板(id=100) -> 所有租户菜单（含目录、菜单、按钮节点，保证导航树可正常渲染）
-- 注意：菜单管理(1300)已移至平台管理，租户角色不包含菜单管理权限
INSERT INTO public.sys_role_menu (tenant_id, role_id, menu_id) VALUES
    -- 目录节点 (type=0)
    (NULL, 100, 1000), (NULL, 100, 2000), (NULL, 100, 3000),
    -- 菜单节点 (type=1)
    (NULL, 100, 1100), (NULL, 100, 1200), (NULL, 100, 1400),
    (NULL, 100, 2100),
    (NULL, 100, 3100), (NULL, 100, 3200),
    -- 按钮节点 (type=2)
    (NULL, 100, 1101), (NULL, 100, 1102), (NULL, 100, 1103), (NULL, 100, 1104), (NULL, 100, 1105), (NULL, 100, 1106),
    (NULL, 100, 1201), (NULL, 100, 1202), (NULL, 100, 1203), (NULL, 100, 1204), (NULL, 100, 1205),
    (NULL, 100, 1401), (NULL, 100, 1402), (NULL, 100, 1403), (NULL, 100, 1404),
    (NULL, 100, 2101), (NULL, 100, 2102), (NULL, 100, 2103),
    (NULL, 100, 3101), (NULL, 100, 3102), (NULL, 100, 3103), (NULL, 100, 3104),
    (NULL, 100, 3201), (NULL, 100, 3202), (NULL, 100, 3203), (NULL, 100, 3204);

-- ========================================
-- 第四部分: 平台管理员用户 (tenant_id=NULL)
-- ========================================

-- 超级管理员 (admin_flag=1, 绕过所有权限检查)
-- 密码: 123456
INSERT INTO public.sys_user (id, tenant_id, username, password, real_name, admin_flag, user_type, status, delete_flag, created_at, updated_at)
VALUES (1, NULL, 'admin', '$2a$12$zdxSVanWeNihjqgNN/A/yej4PKwqUlb4ymtVKnkt.GQpJD3daqBie', '超级管理员', 1, 0, 1, 0, NOW(), NOW());

-- ========================================
-- 第五部分: 平台字典数据 (tenant_id=NULL)
-- ========================================

-- 用户状态字典
INSERT INTO public.sys_dict_type (id, tenant_id, dict_name, dict_type, dict_description, status, sort_order, delete_flag, create_time, update_time)
VALUES (1, NULL, '用户状态', 'user_status', '用户账号状态', 1, 1, 0, NOW(), NOW());

INSERT INTO public.sys_dict_data (id, tenant_id, dict_type, dict_label, dict_value, sort_order, status, is_default, delete_flag, create_time, update_time)
VALUES
    (1, NULL, 'user_status', '启用', '1', 1, 1, 1, 0, NOW(), NOW()),
    (2, NULL, 'user_status', '禁用', '0', 2, 1, 0, 0, NOW(), NOW());

-- 租户状态字典
INSERT INTO public.sys_dict_type (id, tenant_id, dict_name, dict_type, dict_description, status, sort_order, delete_flag, create_time, update_time)
VALUES (2, NULL, '租户状态', 'tenant_status', '租户账号状态', 1, 2, 0, NOW(), NOW());

INSERT INTO public.sys_dict_data (id, tenant_id, dict_type, dict_label, dict_value, sort_order, status, is_default, delete_flag, create_time, update_time)
VALUES
    (10, NULL, 'tenant_status', '待激活', '0', 1, 1, 1, 0, NOW(), NOW()),
    (11, NULL, 'tenant_status', '正常', '1', 2, 1, 0, 0, NOW(), NOW()),
    (12, NULL, 'tenant_status', '已禁用', '2', 3, 1, 0, 0, NOW(), NOW()),
    (13, NULL, 'tenant_status', '已过期', '3', 4, 1, 0, 0, NOW(), NOW());
