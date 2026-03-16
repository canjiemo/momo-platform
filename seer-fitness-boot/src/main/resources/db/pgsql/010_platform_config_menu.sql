-- ====================================================================================================
-- Seer Fitness Edu - 系统配置管理菜单初始化
-- 执行前提：已执行 001_create_tables.sql + 002_init_data.sql
-- ====================================================================================================

-- 平台管理 -> 系统配置 (tenant_id=NULL, 仅平台管理员可见)
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (10400, NULL, '系统配置', 10000, 1, '/platform/config', NULL, 'ControlOutlined', 6, 1, 0, NOW(), NOW()),
    (10401, NULL, '查看配置', 10400, 2, NULL, 'config:view',   NULL, 1, 1, 0, NOW(), NOW()),
    (10402, NULL, '新增配置', 10400, 2, NULL, 'config:create', NULL, 2, 1, 0, NOW(), NOW()),
    (10403, NULL, '修改配置', 10400, 2, NULL, 'config:update', NULL, 3, 1, 0, NOW(), NOW()),
    (10404, NULL, '删除配置', 10400, 2, NULL, 'config:delete', NULL, 4, 1, 0, NOW(), NOW());
