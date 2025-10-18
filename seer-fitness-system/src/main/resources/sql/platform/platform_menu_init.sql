-- ====================================================================================================
-- 平台菜单模板初始化脚本
-- ====================================================================================================
--
-- 说明：
-- 1. 本脚本在平台启动时执行，初始化 public.sys_menu 表
-- 2. 包含两类菜单：
--    - menu_type = 1: 平台专用菜单（租户管理、平台菜单管理等）
--    - menu_type = 2: 租户模板菜单（可分配给租户的功能菜单）
-- 3. 所有主键使用 id（BIGINT，雪花算法或固定ID）
-- 4. 租户模板菜单会在分配时复制到租户 schema
--
-- 使用方式：
-- 1. 方式一：应用启动时自动执行（推荐）
-- 2. 方式二：手动执行
--    psql -U postgres -d seer_fitness_edu -f platform_menu_init.sql
--
-- 创建时间：2025-10-17
-- ====================================================================================================

-- ====================================================================================================
-- 第一部分：平台专用菜单 (menu_type = 1)
-- ====================================================================================================

-- 1. 租户管理(目录)
INSERT INTO sys_menu (id, menu_name, parent_id, type, menu_type, path, permission, icon, sort_order, feature_level, status, delete_flag, created_at, updated_at)
VALUES (1000000, '租户管理', 0, 0, 1, '/platform/tenant', NULL, 'cluster', 1, 1, 1, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 1.1 租户管理(菜单 + 5个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, menu_type, path, permission, icon, sort_order, feature_level, status, delete_flag, created_at, updated_at)
VALUES
    (1001000, '租户列表', 1000000, 1, 1, '/platform/tenant/list', NULL, 'team', 1, 1, 1, 0, NOW(), NOW()),
    (1001001, '查看租户', 1001000, 2, 1, NULL, 'tenant:view', NULL, 1, 1, 1, 0, NOW(), NOW()),
    (1001002, '创建租户', 1001000, 2, 1, NULL, 'tenant:create', NULL, 2, 1, 1, 0, NOW(), NOW()),
    (1001003, '更新租户', 1001000, 2, 1, NULL, 'tenant:update', NULL, 3, 1, 1, 0, NOW(), NOW()),
    (1001004, '删除租户', 1001000, 2, 1, NULL, 'tenant:delete', NULL, 4, 1, 1, 0, NOW(), NOW()),
    (1001005, '分配菜单', 1001000, 2, 1, NULL, 'tenant:assign-menu', NULL, 5, 1, 1, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 2. 平台菜单管理(目录)
INSERT INTO sys_menu (id, menu_name, parent_id, type, menu_type, path, permission, icon, sort_order, feature_level, status, delete_flag, created_at, updated_at)
VALUES (1100000, '平台菜单', 0, 0, 1, '/platform/menu', NULL, 'menu', 2, 1, 1, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 2.1 平台菜单管理(菜单 + 4个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, menu_type, path, permission, icon, sort_order, feature_level, status, delete_flag, created_at, updated_at)
VALUES
    (1101000, '菜单模板', 1100000, 1, 1, '/platform/menu/template', NULL, 'menu', 1, 1, 1, 0, NOW(), NOW()),
    (1101001, '查看菜单', 1101000, 2, 1, NULL, 'platform:menu:view', NULL, 1, 1, 1, 0, NOW(), NOW()),
    (1101002, '创建菜单', 1101000, 2, 1, NULL, 'platform:menu:create', NULL, 2, 1, 1, 0, NOW(), NOW()),
    (1101003, '更新菜单', 1101000, 2, 1, NULL, 'platform:menu:update', NULL, 3, 1, 1, 0, NOW(), NOW()),
    (1101004, '删除菜单', 1101000, 2, 1, NULL, 'platform:menu:delete', NULL, 4, 1, 1, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ====================================================================================================
-- 第二部分：租户模板菜单 (menu_type = 2)
-- 说明：这些菜单可以分配给租户，分配后会复制到租户 schema
-- ====================================================================================================

-- 1. 系统管理(目录)
INSERT INTO sys_menu (id, menu_name, parent_id, type, menu_type, path, permission, icon, sort_order, feature_level, status, delete_flag, created_at, updated_at)
VALUES (2000000, '系统管理', 0, 0, 2, '/system', NULL, 'setting', 1, 1, 1, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 1.1 用户管理(菜单 + 6个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, menu_type, path, permission, icon, sort_order, feature_level, status, delete_flag, created_at, updated_at)
VALUES
    (2001000, '用户管理', 2000000, 1, 2, '/system/user', NULL, 'user', 1, 1, 1, 0, NOW(), NOW()),
    (2001001, '查看用户', 2001000, 2, 2, NULL, 'user:view', NULL, 1, 1, 1, 0, NOW(), NOW()),
    (2001002, '创建用户', 2001000, 2, 2, NULL, 'user:create', NULL, 2, 1, 1, 0, NOW(), NOW()),
    (2001003, '更新用户', 2001000, 2, 2, NULL, 'user:update', NULL, 3, 1, 1, 0, NOW(), NOW()),
    (2001004, '删除用户', 2001000, 2, 2, NULL, 'user:delete', NULL, 4, 1, 1, 0, NOW(), NOW()),
    (2001005, '初始化密码', 2001000, 2, 2, NULL, 'user:init-password', NULL, 5, 2, 1, 0, NOW(), NOW()),
    (2001006, '重置密码', 2001000, 2, 2, NULL, 'user:reset-password', NULL, 6, 2, 1, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 1.2 角色管理(菜单 + 5个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, menu_type, path, permission, icon, sort_order, feature_level, status, delete_flag, created_at, updated_at)
VALUES
    (2002000, '角色管理', 2000000, 1, 2, '/system/role', NULL, 'team', 2, 1, 1, 0, NOW(), NOW()),
    (2002001, '查看角色', 2002000, 2, 2, NULL, 'role:view', NULL, 1, 1, 1, 0, NOW(), NOW()),
    (2002002, '创建角色', 2002000, 2, 2, NULL, 'role:create', NULL, 2, 1, 1, 0, NOW(), NOW()),
    (2002003, '更新角色', 2002000, 2, 2, NULL, 'role:update', NULL, 3, 1, 1, 0, NOW(), NOW()),
    (2002004, '删除角色', 2002000, 2, 2, NULL, 'role:delete', NULL, 4, 1, 1, 0, NOW(), NOW()),
    (2002005, '分配权限', 2002000, 2, 2, NULL, 'role:assign', NULL, 5, 1, 1, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 注意：不包含"菜单管理"模块，租户不能自定义菜单

-- 1.3 组织管理(菜单 + 4个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, menu_type, path, permission, icon, sort_order, feature_level, status, delete_flag, created_at, updated_at)
VALUES
    (2003000, '组织管理', 2000000, 1, 2, '/system/organization', NULL, 'apartment', 3, 1, 1, 0, NOW(), NOW()),
    (2003001, '查看组织', 2003000, 2, 2, NULL, 'organization:view', NULL, 1, 1, 1, 0, NOW(), NOW()),
    (2003002, '创建组织', 2003000, 2, 2, NULL, 'organization:create', NULL, 2, 1, 1, 0, NOW(), NOW()),
    (2003003, '更新组织', 2003000, 2, 2, NULL, 'organization:update', NULL, 3, 1, 1, 0, NOW(), NOW()),
    (2003004, '删除组织', 2003000, 2, 2, NULL, 'organization:delete', NULL, 4, 1, 1, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 2. 日志管理(目录)
INSERT INTO sys_menu (id, menu_name, parent_id, type, menu_type, path, permission, icon, sort_order, feature_level, status, delete_flag, created_at, updated_at)
VALUES (2100000, '日志管理', 0, 0, 2, '/log', NULL, 'file-text', 2, 1, 1, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 2.1 操作日志(菜单 + 3个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, menu_type, path, permission, icon, sort_order, feature_level, status, delete_flag, created_at, updated_at)
VALUES
    (2101000, '操作日志', 2100000, 1, 2, '/log/operation', NULL, 'history', 1, 1, 1, 0, NOW(), NOW()),
    (2101001, '查看日志', 2101000, 2, 2, NULL, 'operation_log:view', NULL, 1, 1, 1, 0, NOW(), NOW()),
    (2101002, '删除日志', 2101000, 2, 2, NULL, 'operation_log:delete', NULL, 2, 2, 1, 0, NOW(), NOW()),
    (2101003, '导出日志', 2101000, 2, 2, NULL, 'operation_log:export', NULL, 3, 2, 1, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 3. 数据字典(目录)
INSERT INTO sys_menu (id, menu_name, parent_id, type, menu_type, path, permission, icon, sort_order, feature_level, status, delete_flag, created_at, updated_at)
VALUES (2200000, '数据字典', 0, 0, 2, '/dict', NULL, 'book', 3, 2, 1, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 3.1 字典类型(菜单 + 4个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, menu_type, path, permission, icon, sort_order, feature_level, status, delete_flag, created_at, updated_at)
VALUES
    (2201000, '字典类型', 2200000, 1, 2, '/dict/type', NULL, 'database', 1, 2, 1, 0, NOW(), NOW()),
    (2201001, '查看字典类型', 2201000, 2, 2, NULL, 'dict:type:view', NULL, 1, 2, 1, 0, NOW(), NOW()),
    (2201002, '创建字典类型', 2201000, 2, 2, NULL, 'dict:type:create', NULL, 2, 2, 1, 0, NOW(), NOW()),
    (2201003, '更新字典类型', 2201000, 2, 2, NULL, 'dict:type:update', NULL, 3, 2, 1, 0, NOW(), NOW()),
    (2201004, '删除字典类型', 2201000, 2, 2, NULL, 'dict:type:delete', NULL, 4, 2, 1, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 3.2 字典数据(菜单 + 4个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, menu_type, path, permission, icon, sort_order, feature_level, status, delete_flag, created_at, updated_at)
VALUES
    (2202000, '字典数据', 2200000, 1, 2, '/dict/data', NULL, 'database', 2, 2, 1, 0, NOW(), NOW()),
    (2202001, '查看字典数据', 2202000, 2, 2, NULL, 'dict:data:view', NULL, 1, 2, 1, 0, NOW(), NOW()),
    (2202002, '创建字典数据', 2202000, 2, 2, NULL, 'dict:data:create', NULL, 2, 2, 1, 0, NOW(), NOW()),
    (2202003, '更新字典数据', 2202000, 2, 2, NULL, 'dict:data:update', NULL, 3, 2, 1, 0, NOW(), NOW()),
    (2202004, '删除字典数据', 2202000, 2, 2, NULL, 'dict:data:delete', NULL, 4, 2, 1, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ====================================================================================================
-- 数据验证
-- ====================================================================================================

-- 查看所有平台菜单
-- SELECT id, menu_name, parent_id, type, menu_type, permission, feature_level
-- FROM sys_menu
-- WHERE menu_type = 1
-- ORDER BY id;

-- 查看所有租户模板菜单
-- SELECT id, menu_name, parent_id, type, menu_type, permission, feature_level
-- FROM sys_menu
-- WHERE menu_type = 2
-- ORDER BY id;

-- 统计菜单数量
-- SELECT
--     menu_type,
--     CASE menu_type
--         WHEN 1 THEN '平台菜单'
--         WHEN 2 THEN '租户模板'
--     END AS menu_type_name,
--     type,
--     CASE type
--         WHEN 0 THEN '目录'
--         WHEN 1 THEN '菜单'
--         WHEN 2 THEN '按钮'
--     END AS type_name,
--     COUNT(*) AS count
-- FROM sys_menu
-- GROUP BY menu_type, type
-- ORDER BY menu_type, type;

-- ====================================================================================================
-- 菜单统计
-- ====================================================================================================
-- 平台菜单 (menu_type = 1):
--   - 2 个目录 (租户管理、平台菜单)
--   - 2 个菜单 (租户列表、菜单模板)
--   - 11 个按钮 (5个租户管理 + 4个菜单管理 + 2个扩展)
--
-- 租户模板菜单 (menu_type = 2):
--   - 3 个目录 (系统管理、日志管理、数据字典)
--   - 6 个菜单 (用户、角色、组织、操作日志、字典类型、字典数据)
--   - 22 个按钮 (6+5+4+3+4+4 = 26个权限点)
--
-- 总计：48 个菜单项
-- ====================================================================================================
