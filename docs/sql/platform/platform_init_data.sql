-- ====================================================================================================
-- 平台管理员初始化数据 - Public Schema
-- 说明：此脚本用于初始化平台管理员账号、角色、菜单和权限
-- 前置条件：已执行 platform_rbac_tables.sql
-- 使用方式：psql -U postgres -d seer_fitness_db -f docs/sql/platform/platform_init_data.sql
-- 创建时间：2025-10-17
-- ====================================================================================================

-- 设置搜索路径到 public
SET search_path TO public;

-- ========================================
-- 第一部分: 平台菜单数据
-- ========================================

-- 1. 平台管理(目录)
INSERT INTO public.sys_menu (id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES (10000, '平台管理', 0, 0, '/platform', NULL, 'dashboard', 1, 1, 0, NOW(), NOW());

-- 1.1 租户管理(菜单 + 6个按钮)
INSERT INTO public.sys_menu (id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (10100, '租户管理', 10000, 1, '/platform/tenant', NULL, 'team', 1, 1, 0, NOW(), NOW()),
    (10101, '查看租户', 10100, 2, NULL, 'tenant:view', NULL, 1, 1, 0, NOW(), NOW()),
    (10102, '创建租户', 10100, 2, NULL, 'tenant:create', NULL, 2, 1, 0, NOW(), NOW()),
    (10103, '更新租户', 10100, 2, NULL, 'tenant:update', NULL, 3, 1, 0, NOW(), NOW()),
    (10104, '禁用租户', 10100, 2, NULL, 'tenant:disable', NULL, 4, 1, 0, NOW(), NOW()),
    (10105, '启用租户', 10100, 2, NULL, 'tenant:enable', NULL, 5, 1, 0, NOW(), NOW()),
    (10106, '初始化租户', 10100, 2, NULL, 'tenant:init', NULL, 6, 1, 0, NOW(), NOW());

-- 1.2 平台项目管理(菜单 + 5个按钮)
INSERT INTO public.sys_menu (id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (10200, '平台项目库', 10000, 1, '/platform/project', NULL, 'appstore', 2, 1, 0, NOW(), NOW()),
    (10201, '查看项目', 10200, 2, NULL, 'platform:project:view', NULL, 1, 1, 0, NOW(), NOW()),
    (10202, '创建项目', 10200, 2, NULL, 'platform:project:create', NULL, 2, 1, 0, NOW(), NOW()),
    (10203, '更新项目', 10200, 2, NULL, 'platform:project:update', NULL, 3, 1, 0, NOW(), NOW()),
    (10204, '删除项目', 10200, 2, NULL, 'platform:project:delete', NULL, 4, 1, 0, NOW(), NOW());

-- 1.3 平台用户管理(菜单 + 5个按钮)
INSERT INTO public.sys_menu (id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (10300, '平台用户', 10000, 1, '/platform/user', NULL, 'user', 3, 1, 0, NOW(), NOW()),
    (10301, '查看用户', 10300, 2, NULL, 'platform:user:view', NULL, 1, 1, 0, NOW(), NOW()),
    (10302, '创建用户', 10300, 2, NULL, 'platform:user:create', NULL, 2, 1, 0, NOW(), NOW()),
    (10303, '更新用户', 10300, 2, NULL, 'platform:user:update', NULL, 3, 1, 0, NOW(), NOW()),
    (10304, '删除用户', 10300, 2, NULL, 'platform:user:delete', NULL, 4, 1, 0, NOW(), NOW()),
    (10305, '重置密码', 10300, 2, NULL, 'platform:user:reset-password', NULL, 5, 1, 0, NOW(), NOW());

-- 1.4 平台字典管理(菜单 + 4个按钮)
INSERT INTO public.sys_menu (id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (10400, '平台字典', 10000, 1, '/platform/dict', NULL, 'book', 4, 1, 0, NOW(), NOW()),
    (10401, '查看字典', 10400, 2, NULL, 'platform:dict:view', NULL, 1, 1, 0, NOW(), NOW()),
    (10402, '创建字典', 10400, 2, NULL, 'platform:dict:create', NULL, 2, 1, 0, NOW(), NOW()),
    (10403, '更新字典', 10400, 2, NULL, 'platform:dict:update', NULL, 3, 1, 0, NOW(), NOW()),
    (10404, '删除字典', 10400, 2, NULL, 'platform:dict:delete', NULL, 4, 1, 0, NOW(), NOW());

-- 1.5 平台日志(菜单 + 2个按钮)
INSERT INTO public.sys_menu (id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (10500, '操作日志', 10000, 1, '/platform/log', NULL, 'file-text', 5, 1, 0, NOW(), NOW()),
    (10501, '查看日志', 10500, 2, NULL, 'platform:log:view', NULL, 1, 1, 0, NOW(), NOW()),
    (10502, '导出日志', 10500, 2, NULL, 'platform:log:export', NULL, 2, 1, 0, NOW(), NOW());

-- ========================================
-- 第二部分: 平台角色数据
-- ========================================

-- 角色1: 平台超级管理员(所有权限)
INSERT INTO public.sys_role (id, role_name, description, status, delete_flag, created_at, updated_at)
VALUES (1, '平台超级管理员', '拥有平台所有管理权限', 1, 0, NOW(), NOW());

-- 角色2: 租户管理员(仅租户管理权限)
INSERT INTO public.sys_role (id, role_name, description, status, delete_flag, created_at, updated_at)
VALUES (2, '租户运营', '管理租户和查看日志', 1, 0, NOW(), NOW());

-- 角色3: 只读管理员(所有查看权限)
INSERT INTO public.sys_role (id, role_name, description, status, delete_flag, created_at, updated_at)
VALUES (3, '平台只读', '平台所有模块只读权限', 1, 0, NOW(), NOW());

-- ========================================
-- 第三部分: 角色-菜单关联 (权限分配)
-- ========================================

-- 角色1: 平台超级管理员 - 所有权限
INSERT INTO public.sys_role_menu (id, role_id, menu_id) VALUES
    -- 租户管理权限
    (1001, 1, 10101), (1002, 1, 10102), (1003, 1, 10103), (1004, 1, 10104), (1005, 1, 10105), (1006, 1, 10106),
    -- 平台项目权限
    (1011, 1, 10201), (1012, 1, 10202), (1013, 1, 10203), (1014, 1, 10204),
    -- 平台用户权限
    (1021, 1, 10301), (1022, 1, 10302), (1023, 1, 10303), (1024, 1, 10304), (1025, 1, 10305),
    -- 平台字典权限
    (1031, 1, 10401), (1032, 1, 10402), (1033, 1, 10403), (1034, 1, 10404),
    -- 平台日志权限
    (1041, 1, 10501), (1042, 1, 10502);

-- 角色2: 租户运营 - 租户管理 + 日志查看
INSERT INTO public.sys_role_menu (id, role_id, menu_id) VALUES
    (2001, 2, 10101), (2002, 2, 10102), (2003, 2, 10103), (2004, 2, 10104), (2005, 2, 10105),
    (2011, 2, 10501);

-- 角色3: 平台只读 - 所有view权限
INSERT INTO public.sys_role_menu (id, role_id, menu_id) VALUES
    (3001, 3, 10101), (3002, 3, 10201), (3003, 3, 10301), (3004, 3, 10401), (3005, 3, 10501);

-- ========================================
-- 第四部分: 平台管理员用户数据
-- ========================================

-- 用户1: 平台超级管理员 (admin_flag=1, 绕过所有权限检查)
-- 密码: Platform@2025
INSERT INTO public.sys_user (id, username, password, real_name, admin_flag, user_type, status, delete_flag, created_at, updated_at)
VALUES (1, 'platform_admin', '$2a$12$eHj0wXqZyLVk8vZ5QX5rD.dGJXqZ5aOKyN8vZ8QX5rD.dGJXqZ5aO', '平台超级管理员', 1, 0, 1, 0, NOW(), NOW());
-- 注意：这个密码哈希是示例，实际使用时需要用真实的 BCrypt 加密

-- 用户2: 平台管理员 (有角色权限控制)
-- 密码: Admin@2025
INSERT INTO public.sys_user (id, username, password, real_name, admin_flag, user_type, status, delete_flag, created_at, updated_at)
VALUES (2, 'admin', '$2a$12$ZcTn.iSGO93QuorJrZix6elFfTFHJSTScYwqPyqbiZKa6GYQtfvhK', '平台管理员', 0, 0, 1, 0, NOW(), NOW());

-- 用户3: 租户运营
-- 密码: Operator@2025
INSERT INTO public.sys_user (id, username, password, real_name, admin_flag, user_type, status, delete_flag, created_at, updated_at)
VALUES (3, 'tenant_operator', '$2a$12$ZcTn.iSGO93QuorJrZix6elFfTFHJSTScYwqPyqbiZKa6GYQtfvhK', '租户运营', 0, 0, 1, 0, NOW(), NOW());

-- 用户4: 平台只读用户
-- 密码: Readonly@2025
INSERT INTO public.sys_user (id, username, password, real_name, admin_flag, user_type, status, delete_flag, created_at, updated_at)
VALUES (4, 'platform_readonly', '$2a$12$ZcTn.iSGO93QuorJrZix6elFfTFHJSTScYwqPyqbiZKa6GYQtfvhK', '平台只读', 0, 0, 1, 0, NOW(), NOW());

-- ========================================
-- 第五部分: 用户-角色关联
-- ========================================

-- platform_admin 是超级管理员，admin_flag=1，无需分配角色

INSERT INTO public.sys_user_role (id, user_id, role_id) VALUES
    (1, 2, 1),  -- admin → 平台超级管理员
    (2, 3, 2),  -- tenant_operator → 租户运营
    (3, 4, 3);  -- platform_readonly → 平台只读

-- ========================================
-- 数据验证查询
-- ========================================

-- 查看所有平台菜单
-- SELECT id, menu_name, type, permission, parent_id FROM public.sys_menu ORDER BY id;

-- 查看所有平台角色
-- SELECT id, role_name, description FROM public.sys_role ORDER BY id;

-- 查看平台管理员用户
-- SELECT id, username, real_name, admin_flag, status FROM public.sys_user ORDER BY id;

-- 查看角色权限分配
-- SELECT r.role_name, m.menu_name, m.permission
-- FROM public.sys_role r
-- JOIN public.sys_role_menu rm ON r.id = rm.role_id
-- JOIN public.sys_menu m ON rm.menu_id = m.id
-- ORDER BY r.id, m.id;

-- ========================================
-- 账号信息总结
-- ========================================

/*
平台管理员账号列表：

1. 超级管理员（绕过权限）
   用户名: platform_admin
   密码: Platform@2025
   说明: admin_flag=1，拥有所有权限

2. 普通管理员（权限控制）
   用户名: admin
   密码: Admin@2025
   说明: 通过角色"平台超级管理员"获得所有权限

3. 租户运营
   用户名: tenant_operator
   密码: Operator@2025
   说明: 只能管理租户和查看日志

4. 只读用户
   用户名: platform_readonly
   密码: Readonly@2025
   说明: 所有模块只读权限

登录方式：
{
  "tenantCode": "",  // 留空或不传，表示平台管理员登录
  "username": "platform_admin",
  "password": "Platform@2025",
  "captcha": "1234",
  "captchaId": "xxx"
}
*/
