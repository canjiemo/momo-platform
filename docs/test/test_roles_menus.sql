-- ============================================
-- Seer Fitness Edu - 测试角色和菜单数据
-- 基于30个权限点的完整RBAC测试数据
-- ============================================

-- ========================================
-- 第一部分: 菜单数据 (包含所有权限按钮)
-- ========================================

-- 1. 系统管理(目录)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, component, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES (1000, '系统管理', 0, 0, '/system', NULL, NULL, 'setting', 1, 1, 0, NOW(), NOW());

-- 1.1 用户管理(菜单 + 6个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, component, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
(1100, '用户管理', 1000, 1, '/system/user', 'system/user/index', NULL, 'user', 1, 1, 0, NOW(), NOW()),
(1101, '查看用户', 1100, 2, NULL, NULL, 'user:view', NULL, 1, 1, 0, NOW(), NOW()),
(1102, '创建用户', 1100, 2, NULL, NULL, 'user:create', NULL, 2, 1, 0, NOW(), NOW()),
(1103, '更新用户', 1100, 2, NULL, NULL, 'user:update', NULL, 3, 1, 0, NOW(), NOW()),
(1104, '删除用户', 1100, 2, NULL, NULL, 'user:delete', NULL, 4, 1, 0, NOW(), NOW()),
(1105, '初始化密码', 1100, 2, NULL, NULL, 'user:init-password', NULL, 5, 1, 0, NOW(), NOW()),
(1106, '重置密码', 1100, 2, NULL, NULL, 'user:reset-password', NULL, 6, 1, 0, NOW(), NOW());

-- 1.2 角色管理(菜单 + 5个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, component, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
(1200, '角色管理', 1000, 1, '/system/role', 'system/role/index', NULL, 'team', 2, 1, 0, NOW(), NOW()),
(1201, '查看角色', 1200, 2, NULL, NULL, 'role:view', NULL, 1, 1, 0, NOW(), NOW()),
(1202, '创建角色', 1200, 2, NULL, NULL, 'role:create', NULL, 2, 1, 0, NOW(), NOW()),
(1203, '更新角色', 1200, 2, NULL, NULL, 'role:update', NULL, 3, 1, 0, NOW(), NOW()),
(1204, '删除角色', 1200, 2, NULL, NULL, 'role:delete', NULL, 4, 1, 0, NOW(), NOW()),
(1205, '分配权限', 1200, 2, NULL, NULL, 'role:assign', NULL, 5, 1, 0, NOW(), NOW());

-- 1.3 菜单管理(菜单 + 4个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, component, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
(1300, '菜单管理', 1000, 1, '/system/menu', 'system/menu/index', NULL, 'menu', 3, 1, 0, NOW(), NOW()),
(1301, '查看菜单', 1300, 2, NULL, NULL, 'menu:view', NULL, 1, 1, 0, NOW(), NOW()),
(1302, '创建菜单', 1300, 2, NULL, NULL, 'menu:create', NULL, 2, 1, 0, NOW(), NOW()),
(1303, '更新菜单', 1300, 2, NULL, NULL, 'menu:update', NULL, 3, 1, 0, NOW(), NOW()),
(1304, '删除菜单', 1300, 2, NULL, NULL, 'menu:delete', NULL, 4, 1, 0, NOW(), NOW());

-- 1.4 组织管理(菜单 + 4个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, component, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
(1400, '组织管理', 1000, 1, '/system/organization', 'system/organization/index', NULL, 'apartment', 4, 1, 0, NOW(), NOW()),
(1401, '查看组织', 1400, 2, NULL, NULL, 'organization:view', NULL, 1, 1, 0, NOW(), NOW()),
(1402, '创建组织', 1400, 2, NULL, NULL, 'organization:create', NULL, 2, 1, 0, NOW(), NOW()),
(1403, '更新组织', 1400, 2, NULL, NULL, 'organization:update', NULL, 3, 1, 0, NOW(), NOW()),
(1404, '删除组织', 1400, 2, NULL, NULL, 'organization:delete', NULL, 4, 1, 0, NOW(), NOW());

-- 2. 日志管理(目录)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, component, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES (2000, '日志管理', 0, 0, '/log', NULL, NULL, 'file-text', 2, 1, 0, NOW(), NOW());

-- 2.1 操作日志(菜单 + 3个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, component, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
(2100, '操作日志', 2000, 1, '/log/operation', 'log/operation/index', NULL, 'history', 1, 1, 0, NOW(), NOW()),
(2101, '查看日志', 2100, 2, NULL, NULL, 'operation_log:view', NULL, 1, 1, 0, NOW(), NOW()),
(2102, '删除日志', 2100, 2, NULL, NULL, 'operation_log:delete', NULL, 2, 1, 0, NOW(), NOW()),
(2103, '导出日志', 2100, 2, NULL, NULL, 'operation_log:export', NULL, 3, 1, 0, NOW(), NOW());

-- 3. 数据字典(目录)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, component, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES (3000, '数据字典', 0, 0, '/dict', NULL, NULL, 'book', 3, 1, 0, NOW(), NOW());

-- 3.1 字典类型(菜单 + 4个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, component, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
(3100, '字典类型', 3000, 1, '/dict/type', 'dict/type/index', NULL, 'database', 1, 1, 0, NOW(), NOW()),
(3101, '查看字典类型', 3100, 2, NULL, NULL, 'dict:type:view', NULL, 1, 1, 0, NOW(), NOW()),
(3102, '创建字典类型', 3100, 2, NULL, NULL, 'dict:type:create', NULL, 2, 1, 0, NOW(), NOW()),
(3103, '更新字典类型', 3100, 2, NULL, NULL, 'dict:type:update', NULL, 3, 1, 0, NOW(), NOW()),
(3104, '删除字典类型', 3100, 2, NULL, NULL, 'dict:type:delete', NULL, 4, 1, 0, NOW(), NOW());

-- 3.2 字典数据(菜单 + 4个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, component, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
(3200, '字典数据', 3000, 1, '/dict/data', 'dict/data/index', NULL, 'database', 2, 1, 0, NOW(), NOW()),
(3201, '查看字典数据', 3200, 2, NULL, NULL, 'dict:data:view', NULL, 1, 1, 0, NOW(), NOW()),
(3202, '创建字典数据', 3200, 2, NULL, NULL, 'dict:data:create', NULL, 2, 1, 0, NOW(), NOW()),
(3203, '更新字典数据', 3200, 2, NULL, NULL, 'dict:data:update', NULL, 3, 1, 0, NOW(), NOW()),
(3204, '删除字典数据', 3200, 2, NULL, NULL, 'dict:data:delete', NULL, 4, 1, 0, NOW(), NOW());

-- ========================================
-- 第二部分: 角色数据 (10个测试角色)
-- ========================================

-- 角色1: 系统管理员(所有权限,非admin_flag)
INSERT INTO sys_role (id, role_name, description, status, delete_flag, created_at, updated_at)
VALUES (100, '系统管理员', '拥有所有权限(非admin_flag bypass)', 1, 0, NOW(), NOW());

-- 角色2: 用户管理员(用户相关权限)
INSERT INTO sys_role (id, role_name, description, status, delete_flag, created_at, updated_at)
VALUES (200, '用户管理员', '管理用户和查看组织', 1, 0, NOW(), NOW());

-- 角色3: 角色管理员(角色相关权限)
INSERT INTO sys_role (id, role_name, description, status, delete_flag, created_at, updated_at)
VALUES (300, '角色管理员', '管理角色和查看菜单', 1, 0, NOW(), NOW());

-- 角色4: 内容管理员(菜单和字典权限)
INSERT INTO sys_role (id, role_name, description, status, delete_flag, created_at, updated_at)
VALUES (400, '内容管理员', '管理菜单和数据字典', 1, 0, NOW(), NOW());

-- 角色5: 审计员(只读日志和查看用户角色)
INSERT INTO sys_role (id, role_name, description, status, delete_flag, created_at, updated_at)
VALUES (500, '审计员', '查看日志、导出日志、查看用户角色', 1, 0, NOW(), NOW());

-- 角色6: 只读用户(所有view权限)
INSERT INTO sys_role (id, role_name, description, status, delete_flag, created_at, updated_at)
VALUES (600, '只读用户', '所有模块只读权限', 1, 0, NOW(), NOW());

-- 角色7: 部分权限用户(用户view+create,角色view)
INSERT INTO sys_role (id, role_name, description, status, delete_flag, created_at, updated_at)
VALUES (700, '部分权限用户', '用户查看创建,角色查看', 1, 0, NOW(), NOW());

-- 角色8: 无权限角色(测试用)
INSERT INTO sys_role (id, role_name, description, status, delete_flag, created_at, updated_at)
VALUES (800, '无权限角色', '没有任何权限的测试角色', 1, 0, NOW(), NOW());

-- 角色9: 组织管理员
INSERT INTO sys_role (id, role_name, description, status, delete_flag, created_at, updated_at)
VALUES (900, '组织管理员', '管理组织架构', 1, 0, NOW(), NOW());

-- 角色10: 日志管理员
INSERT INTO sys_role (id, role_name, description, status, delete_flag, created_at, updated_at)
VALUES (1000, '日志管理员', '管理操作日志', 1, 0, NOW(), NOW());

-- ========================================
-- 第三部分: 角色-菜单关联 (权限分配)
-- ========================================

-- 100. 系统管理员 - 所有权限(30个)
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
-- 用户管理(6)
(100, 1101), (100, 1102), (100, 1103), (100, 1104), (100, 1105), (100, 1106),
-- 角色管理(5)
(100, 1201), (100, 1202), (100, 1203), (100, 1204), (100, 1205),
-- 菜单管理(4)
(100, 1301), (100, 1302), (100, 1303), (100, 1304),
-- 组织管理(4)
(100, 1401), (100, 1402), (100, 1403), (100, 1404),
-- 操作日志(3)
(100, 2101), (100, 2102), (100, 2103),
-- 字典类型(4)
(100, 3101), (100, 3102), (100, 3103), (100, 3104),
-- 字典数据(4)
(100, 3201), (100, 3202), (100, 3203), (100, 3204);

-- 200. 用户管理员 - 用户所有权限 + 组织查看
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(200, 1101), (200, 1102), (200, 1103), (200, 1104), (200, 1105), (200, 1106),
(200, 1401); -- 组织查看

-- 300. 角色管理员 - 角色所有权限 + 菜单查看
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(300, 1201), (300, 1202), (300, 1203), (300, 1204), (300, 1205),
(300, 1301); -- 菜单查看

-- 400. 内容管理员 - 菜单 + 字典所有权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(400, 1301), (400, 1302), (400, 1303), (400, 1304),
(400, 3101), (400, 3102), (400, 3103), (400, 3104),
(400, 3201), (400, 3202), (400, 3203), (400, 3204);

-- 500. 审计员 - 日志查看导出 + 用户角色查看
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(500, 2101), (500, 2103), -- 日志查看和导出
(500, 1101), (500, 1201); -- 用户查看,角色查看

-- 600. 只读用户 - 所有view权限(8个)
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(600, 1101), (600, 1201), (600, 1301), (600, 1401),
(600, 2101), (600, 3101), (600, 3201);

-- 700. 部分权限用户 - 用户view+create, 角色view
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(700, 1101), (700, 1102), -- 用户查看+创建
(700, 1201); -- 角色查看

-- 800. 无权限角色 - 无任何权限(不插入数据)

-- 900. 组织管理员 - 组织所有权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(900, 1401), (900, 1402), (900, 1403), (900, 1404);

-- 1000. 日志管理员 - 日志所有权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1000, 2101), (1000, 2102), (1000, 2103);

-- ========================================
-- 第四部分: 测试用户数据 (10个用户)
-- ========================================

-- 用户1: 超级管理员 (admin_flag=1)
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (10, 'superadmin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5gyFUHc/TsBfe', '超级管理员', 1, 1, 0, NOW(), NOW());
-- 密码: Admin123!

-- 用户2: 系统管理员 (系统管理员角色)
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (20, 'sysadmin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5gyFUHc/TsBfe', '系统管理员', 0, 1, 0, NOW(), NOW());
-- 密码: Sysadmin123!

-- 用户3: 用户管理员
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (30, 'usermgr', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5gyFUHc/TsBfe', '用户管理员', 0, 1, 0, NOW(), NOW());
-- 密码: Usermgr123!

-- 用户4: 角色管理员
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (40, 'rolemgr', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5gyFUHc/TsBfe', '角色管理员', 0, 1, 0, NOW(), NOW());
-- 密码: Rolemgr123!

-- 用户5: 内容管理员
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (50, 'contentmgr', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5gyFUHc/TsBfe', '内容管理员', 0, 1, 0, NOW(), NOW());
-- 密码: Contentmgr123!

-- 用户6: 审计员
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (60, 'auditor', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5gyFUHc/TsBfe', '审计员', 0, 1, 0, NOW(), NOW());
-- 密码: Auditor123!

-- 用户7: 只读用户
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (70, 'readonly', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5gyFUHc/TsBfe', '只读用户', 0, 1, 0, NOW(), NOW());
-- 密码: Readonly123!

-- 用户8: 部分权限用户
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (80, 'partial', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5gyFUHc/TsBfe', '部分权限用户', 0, 1, 0, NOW(), NOW());
-- 密码: Partial123!

-- 用户9: 无权限用户
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (90, 'noperm', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5gyFUHc/TsBfe', '无权限用户', 0, 1, 0, NOW(), NOW());
-- 密码: Noperm123!

-- 用户10: 禁用用户 (status=0)
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (100, 'disabled', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5gyFUHc/TsBfe', '禁用用户', 0, 0, 0, NOW(), NOW());
-- 密码: Disabled123!

-- ========================================
-- 第五部分: 用户-角色关联
-- ========================================

-- superadmin无需分配角色(admin_flag=1直接bypass)

INSERT INTO sys_user_role (user_id, role_id) VALUES
(20, 100),  -- sysadmin → 系统管理员
(30, 200),  -- usermgr → 用户管理员
(40, 300),  -- rolemgr → 角色管理员
(50, 400),  -- contentmgr → 内容管理员
(60, 500),  -- auditor → 审计员
(70, 600),  -- readonly → 只读用户
(80, 700),  -- partial → 部分权限用户
(90, 800),  -- noperm → 无权限角色
(100, 100); -- disabled → 系统管理员(但被禁用)

-- ========================================
-- 数据验证查询
-- ========================================

-- 查看所有菜单和权限
-- SELECT id, menu_name, type, permission, parent_id FROM sys_menu ORDER BY id;

-- 查看所有角色
-- SELECT id, role_name, description, status FROM sys_role ORDER BY id;

-- 查看角色权限分配
-- SELECT r.role_name, m.menu_name, m.permission
-- FROM sys_role r
-- JOIN sys_role_menu rm ON r.id = rm.role_id
-- JOIN sys_menu m ON rm.menu_id = m.id
-- ORDER BY r.id, m.id;

-- 查看用户角色权限
-- SELECT u.username, u.real_name, u.admin_flag, r.role_name, m.permission
-- FROM sys_user u
-- LEFT JOIN sys_user_role ur ON u.id = ur.user_id
-- LEFT JOIN sys_role r ON ur.role_id = r.id
-- LEFT JOIN sys_role_menu rm ON r.id = rm.role_id
-- LEFT JOIN sys_menu m ON rm.menu_id = m.id
-- ORDER BY u.id, m.permission;
