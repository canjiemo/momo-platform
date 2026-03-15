-- ====================================================================================================
-- Seer Fitness Edu - 文件模块菜单初始化
-- 执行前提：已执行 001_create_tables.sql + 002_init_data.sql
-- ====================================================================================================

-- ========================================
-- 平台管理 -> 文件存储配置 (tenant_id=NULL, 仅平台管理员可见)
-- ========================================
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (10300, NULL, '文件存储', 10000, 1, '/platform/file-config', NULL, 'CloudUploadOutlined', 5, 1, 0, NOW(), NOW()),
    (10301, NULL, '查看存储配置', 10300, 2, NULL, 'file:config:view',   NULL, 1, 1, 0, NOW(), NOW()),
    (10302, NULL, '创建存储配置', 10300, 2, NULL, 'file:config:create', NULL, 2, 1, 0, NOW(), NOW()),
    (10303, NULL, '更新存储配置', 10300, 2, NULL, 'file:config:update', NULL, 3, 1, 0, NOW(), NOW()),
    (10304, NULL, '删除存储配置', 10300, 2, NULL, 'file:config:delete', NULL, 4, 1, 0, NOW(), NOW());

-- ========================================
-- 系统管理 -> 文件管理 (tenant_id=NULL, 租户可见)
-- ========================================
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (1500, NULL, '文件管理', 1000, 1, '/system/file', NULL, 'FileOutlined', 4, 1, 0, NOW(), NOW()),
    (1501, NULL, '删除文件', 1500, 2, NULL, 'file:delete', NULL, 1, 1, 0, NOW(), NOW());

-- 将文件管理菜单加入租户管理员模板角色 (role_id=100)
INSERT INTO public.sys_role_menu (tenant_id, role_id, menu_id) VALUES
    (NULL, 100, 1500),
    (NULL, 100, 1501);
