-- ============================================
-- Seer Fitness Edu - 测试角色和菜单数据
-- 基于30个权限点的完整RBAC测试数据
-- ============================================

-- ========================================
-- 第一部分: 菜单数据 (包含所有权限按钮)
-- ========================================

-- 1. 系统管理(目录)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES (1000, '系统管理', 0, 0, '/system', NULL, 'setting', 1, 1, 0, NOW(), NOW());

-- 1.1 用户管理(菜单 + 6个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (1100, '用户管理', 1000, 1, '/system/user', NULL, 'user', 1, 1, 0, NOW(), NOW()),
    (1101, '查看用户', 1100, 2, NULL, 'user:view', NULL, 1, 1, 0, NOW(), NOW()),
    (1102, '创建用户', 1100, 2, NULL, 'user:create', NULL, 2, 1, 0, NOW(), NOW()),
    (1103, '更新用户', 1100, 2, NULL, 'user:update', NULL, 3, 1, 0, NOW(), NOW()),
    (1104, '删除用户', 1100, 2, NULL, 'user:delete', NULL, 4, 1, 0, NOW(), NOW()),
    (1105, '初始化密码', 1100, 2, NULL, 'user:init-password', NULL, 5, 1, 0, NOW(), NOW()),
    (1106, '重置密码', 1100, 2, NULL, 'user:reset-password', NULL, 6, 1, 0, NOW(), NOW());

-- 1.2 角色管理(菜单 + 5个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (1200, '角色管理', 1000, 1, '/system/role', NULL, 'team', 2, 1, 0, NOW(), NOW()),
    (1201, '查看角色', 1200, 2, NULL, 'role:view', NULL, 1, 1, 0, NOW(), NOW()),
    (1202, '创建角色', 1200, 2, NULL, 'role:create', NULL, 2, 1, 0, NOW(), NOW()),
    (1203, '更新角色', 1200, 2, NULL, 'role:update', NULL, 3, 1, 0, NOW(), NOW()),
    (1204, '删除角色', 1200, 2, NULL, 'role:delete', NULL, 4, 1, 0, NOW(), NOW()),
    (1205, '分配权限', 1200, 2, NULL, 'role:assign', NULL, 5, 1, 0, NOW(), NOW());

-- 1.3 菜单管理(菜单 + 4个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (1300, '菜单管理', 1000, 1, '/system/menu', NULL, 'menu', 3, 1, 0, NOW(), NOW()),
    (1301, '查看菜单', 1300, 2, NULL, 'menu:view', NULL, 1, 1, 0, NOW(), NOW()),
    (1302, '创建菜单', 1300, 2, NULL, 'menu:create', NULL, 2, 1, 0, NOW(), NOW()),
    (1303, '更新菜单', 1300, 2, NULL, 'menu:update', NULL, 3, 1, 0, NOW(), NOW()),
    (1304, '删除菜单', 1300, 2, NULL, 'menu:delete', NULL, 4, 1, 0, NOW(), NOW());

-- 1.4 组织管理(菜单 + 4个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (1400, '组织管理', 1000, 1, '/system/organization', NULL, 'apartment', 4, 1, 0, NOW(), NOW()),
    (1401, '查看组织', 1400, 2, NULL, 'organization:view', NULL, 1, 1, 0, NOW(), NOW()),
    (1402, '创建组织', 1400, 2, NULL, 'organization:create', NULL, 2, 1, 0, NOW(), NOW()),
    (1403, '更新组织', 1400, 2, NULL, 'organization:update', NULL, 3, 1, 0, NOW(), NOW()),
    (1404, '删除组织', 1400, 2, NULL, 'organization:delete', NULL, 4, 1, 0, NOW(), NOW());

-- 2. 日志管理(目录)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES (2000, '日志管理', 0, 0, '/log', NULL, 'file-text', 2, 1, 0, NOW(), NOW());

-- 2.1 操作日志(菜单 + 3个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (2100, '操作日志', 2000, 1, '/log/operation', NULL, 'history', 1, 1, 0, NOW(), NOW()),
    (2101, '查看日志', 2100, 2, NULL, 'operation_log:view', NULL, 1, 1, 0, NOW(), NOW()),
    (2102, '删除日志', 2100, 2, NULL, 'operation_log:delete', NULL, 2, 1, 0, NOW(), NOW()),
    (2103, '导出日志', 2100, 2, NULL, 'operation_log:export', NULL, 3, 1, 0, NOW(), NOW());

-- 3. 数据字典(目录)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES (3000, '数据字典', 0, 0, '/dict', NULL, 'book', 3, 1, 0, NOW(), NOW());

-- 3.1 字典类型(菜单 + 4个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (3100, '字典类型', 3000, 1, '/dict/type', NULL, 'database', 1, 1, 0, NOW(), NOW()),
    (3101, '查看字典类型', 3100, 2, NULL, 'dict:type:view', NULL, 1, 1, 0, NOW(), NOW()),
    (3102, '创建字典类型', 3100, 2, NULL, 'dict:type:create', NULL, 2, 1, 0, NOW(), NOW()),
    (3103, '更新字典类型', 3100, 2, NULL, 'dict:type:update', NULL, 3, 1, 0, NOW(), NOW()),
    (3104, '删除字典类型', 3100, 2, NULL, 'dict:type:delete', NULL, 4, 1, 0, NOW(), NOW());

-- 3.2 字典数据(菜单 + 4个按钮)
INSERT INTO sys_menu (id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, created_at, updated_at)
VALUES
    (3200, '字典数据', 3000, 1, '/dict/data', NULL, 'database', 2, 1, 0, NOW(), NOW()),
    (3201, '查看字典数据', 3200, 2, NULL, 'dict:data:view', NULL, 1, 1, 0, NOW(), NOW()),
    (3202, '创建字典数据', 3200, 2, NULL, 'dict:data:create', NULL, 2, 1, 0, NOW(), NOW()),
    (3203, '更新字典数据', 3200, 2, NULL, 'dict:data:update', NULL, 3, 1, 0, NOW(), NOW()),
    (3204, '删除字典数据', 3200, 2, NULL, 'dict:data:delete', NULL, 4, 1, 0, NOW(), NOW());

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
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(7381869345930018530, 100, 1101), (7381869345930061811, 100, 1102), (7381869345930040971, 100, 1103),
(7381869345929958481, 100, 1104), (7381869345929984781, 100, 1105), (7381869345930005416, 100, 1106),
(7381869345930046610, 100, 1201), (7381869345930083176, 100, 1202), (7381869345930057184, 100, 1203),
(7381869345929974748, 100, 1204), (7381869345930053771, 100, 1205),
(7381869345929975251, 100, 1301), (7381869345929986422, 100, 1302), (7381869345930028902, 100, 1303),
(7381869345929988749, 100, 1304),
(7381869345930072355, 100, 1401), (7381869345930017653, 100, 1402), (7381869345930063752, 100, 1403),
(7381869345929966109, 100, 1404),
(7381869345929971716, 100, 2101), (7381869345930061203, 100, 2102), (7381869345929969442, 100, 2103),
(7381869345930058542, 100, 3101), (7381869345929990298, 100, 3102), (7381869345929991054, 100, 3103),
(7381869345930033717, 100, 3104),
(7381869345930077709, 100, 3201), (7381869345929991680, 100, 3202), (7381869345930078768, 100, 3203),
(7381869345929956094, 100, 3204);

-- 200. 用户管理员 - 用户所有权限 + 组织查看
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(7381869345929979741, 200, 1101), (7381869345930062710, 200, 1102), (7381869345929985022, 200, 1103),
(7381869345929970087, 200, 1104), (7381869345930014331, 200, 1105), (7381869345930030937, 200, 1106),
(7381869345929974478, 200, 1401);

-- 300. 角色管理员 - 角色所有权限 + 菜单查看
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(7381870145141419913, 300, 1201), (7381870145141482355, 300, 1202), (7381870145141497787, 300, 1203),
(7381870145141502864, 300, 1204), (7381870145141461423, 300, 1205),
(7381870145141407075, 300, 1301);

-- 400. 内容管理员 - 菜单 + 字典所有权限
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(7381870245141419913, 400, 1301), (7381870245141482355, 400, 1302), (7381870245141497787, 400, 1303),
(7381870245141502864, 400, 1304),
(7381870245141461423, 400, 3101), (7381870245141407075, 400, 3102), (7381870245141461096, 400, 3103),
(7381870245141490151, 400, 3104),
(7381870245141450896, 400, 3201), (7381870245141419914, 400, 3202), (7381870245141482356, 400, 3203),
(7381870245141497788, 400, 3204);

-- 500. 审计员 - 日志查看导出 + 用户角色查看
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(7381870345141419913, 500, 2101), (7381870345141482355, 500, 2103),
(7381870345141497787, 500, 1101), (7381870345141502864, 500, 1201);

-- 600. 只读用户 - 所有view权限(7个)
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(7381870445141419913, 600, 1101), (7381870445141482355, 600, 1201), (7381870445141497787, 600, 1301),
(7381870445141502864, 600, 1401),
(7381870445141461423, 600, 2101), (7381870445141407075, 600, 3101), (7381870445141461096, 600, 3201);

-- 700. 部分权限用户 - 用户view+create, 角色view
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(7381870545141419913, 700, 1101), (7381870545141482355, 700, 1102),
(7381870545141497787, 700, 1201);

-- 900. 组织管理员 - 组织所有权限
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(7381870645141419913, 900, 1401), (7381870645141482355, 900, 1402), (7381870645141497787, 900, 1403),
(7381870645141502864, 900, 1404);

-- 1000. 日志管理员 - 日志所有权限
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(7381870745141419913, 1000, 2101), (7381870745141482355, 1000, 2102), (7381870745141497787, 1000, 2103);


-- ========================================
-- 第四部分: 测试用户数据 (10个用户) 密码：Aa123456!
-- ========================================

-- 用户1: 超级管理员 (admin_flag=1)
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (10, 'superadmin', '$2a$12$ZcTn.iSGO93QuorJrZix6elFfTFHJSTScYwqPyqbiZKa6GYQtfvhK', '超级管理员', 1, 1, 0, NOW(), NOW());
-- 密码: Admin123!

-- 用户2: 系统管理员 (系统管理员角色)
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (20, 'sysadmin', '$2a$12$ZcTn.iSGO93QuorJrZix6elFfTFHJSTScYwqPyqbiZKa6GYQtfvhK', '系统管理员', 0, 1, 0, NOW(), NOW());
-- 密码: Sysadmin123!

-- 用户3: 用户管理员
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (30, 'usermgr', '$2a$12$ZcTn.iSGO93QuorJrZix6elFfTFHJSTScYwqPyqbiZKa6GYQtfvhK', '用户管理员', 0, 1, 0, NOW(), NOW());
-- 密码: Usermgr123!

-- 用户4: 角色管理员
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (40, 'rolemgr', '$2a$12$ZcTn.iSGO93QuorJrZix6elFfTFHJSTScYwqPyqbiZKa6GYQtfvhK', '角色管理员', 0, 1, 0, NOW(), NOW());
-- 密码: Rolemgr123!

-- 用户5: 内容管理员
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (50, 'contentmgr', '$2a$12$ZcTn.iSGO93QuorJrZix6elFfTFHJSTScYwqPyqbiZKa6GYQtfvhK', '内容管理员', 0, 1, 0, NOW(), NOW());
-- 密码: Contentmgr123!

-- 用户6: 审计员
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (60, 'auditor', '$2a$12$ZcTn.iSGO93QuorJrZix6elFfTFHJSTScYwqPyqbiZKa6GYQtfvhK', '审计员', 0, 1, 0, NOW(), NOW());
-- 密码: Auditor123!

-- 用户7: 只读用户
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (70, 'readonly', '$2a$12$ZcTn.iSGO93QuorJrZix6elFfTFHJSTScYwqPyqbiZKa6GYQtfvhK', '只读用户', 0, 1, 0, NOW(), NOW());
-- 密码: Readonly123!

-- 用户8: 部分权限用户
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (80, 'partial', '$2a$12$ZcTn.iSGO93QuorJrZix6elFfTFHJSTScYwqPyqbiZKa6GYQtfvhK', '部分权限用户', 0, 1, 0, NOW(), NOW());
-- 密码: Partial123!

-- 用户9: 无权限用户
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (90, 'noperm', '$2a$12$ZcTn.iSGO93QuorJrZix6elFfTFHJSTScYwqPyqbiZKa6GYQtfvhK', '无权限用户', 0, 1, 0, NOW(), NOW());
-- 密码: Noperm123!

-- 用户10: 禁用用户 (status=0)
INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at)
VALUES (100, 'disabled', '$2a$12$ZcTn.iSGO93QuorJrZix6elFfTFHJSTScYwqPyqbiZKa6GYQtfvhK', '禁用用户', 0, 0, 0, NOW(), NOW());
-- 密码: Disabled123!

-- ========================================
-- 第五部分: 用户-角色关联
-- ========================================

-- superadmin无需分配角色(admin_flag=1直接bypass)

INSERT INTO sys_user_role (id, user_id, role_id) VALUES
(7381870045141419913, 20, 100),  -- sysadmin → 系统管理员
(7381870045141482355, 30, 200),  -- usermgr → 用户管理员
(7381870045141497787, 40, 300),  -- rolemgr → 角色管理员
(7381870045141502864, 50, 400),  -- contentmgr → 内容管理员
(7381870045141461423, 60, 500),  -- auditor → 审计员
(7381870045141407075, 70, 600),  -- readonly → 只读用户
(7381870045141461096, 80, 700),  -- partial → 部分权限用户
(7381870045141490151, 90, 800),  -- noperm → 无权限角色
(7381870045141450896, 100, 100); -- disabled → 系统管理员(但被禁用)


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
