-- ====================================================================================================
-- Seer Fitness Edu - 补充字典数据
-- 执行前提：已执行 002_init_data.sql（user_status/tenant_status 已初始化）
-- ====================================================================================================

-- ========================================
-- 通用状态字典（0=禁用 1=启用）
-- 复用于：角色、菜单、组织、字典类型、字典数据、定时任务等
-- ========================================
INSERT INTO public.sys_dict_type (id, tenant_id, dict_name, dict_type, dict_description, status, sort_order, delete_flag, create_time, update_time)
VALUES (3, NULL, '通用状态', 'common_status', '通用的启用/禁用状态', 1, 3, 0, NOW(), NOW());

INSERT INTO public.sys_dict_data (id, tenant_id, dict_type, dict_label, dict_value, sort_order, status, is_default, delete_flag, create_time, update_time)
VALUES
    (20, NULL, 'common_status', '启用', '1', 1, 1, 1, 0, NOW(), NOW()),
    (21, NULL, 'common_status', '禁用', '0', 2, 1, 0, 0, NOW(), NOW());

-- ========================================
-- 菜单节点类型（0=目录 1=菜单 2=按钮）
-- ========================================
INSERT INTO public.sys_dict_type (id, tenant_id, dict_name, dict_type, dict_description, status, sort_order, delete_flag, create_time, update_time)
VALUES (4, NULL, '菜单节点类型', 'menu_type', '菜单树节点的展示类型', 1, 4, 0, NOW(), NOW());

INSERT INTO public.sys_dict_data (id, tenant_id, dict_type, dict_label, dict_value, sort_order, status, is_default, delete_flag, create_time, update_time)
VALUES
    (30, NULL, 'menu_type', '目录', '0', 1, 1, 0, 0, NOW(), NOW()),
    (31, NULL, 'menu_type', '菜单', '1', 2, 1, 1, 0, NOW(), NOW()),
    (32, NULL, 'menu_type', '按钮', '2', 3, 1, 0, 0, NOW(), NOW());

-- ========================================
-- 菜单归属类型（1=平台菜单 2=租户模板菜单）
-- ========================================
INSERT INTO public.sys_dict_type (id, tenant_id, dict_name, dict_type, dict_description, status, sort_order, delete_flag, create_time, update_time)
VALUES (5, NULL, '菜单归属类型', 'menu_display_type', '区分平台菜单与租户模板菜单', 1, 5, 0, NOW(), NOW());

INSERT INTO public.sys_dict_data (id, tenant_id, dict_type, dict_label, dict_value, sort_order, status, is_default, delete_flag, create_time, update_time)
VALUES
    (40, NULL, 'menu_display_type', '平台菜单', '1', 1, 1, 1, 0, NOW(), NOW()),
    (41, NULL, 'menu_display_type', '租户模板菜单', '2', 2, 1, 0, 0, NOW(), NOW());

-- ========================================
-- 管理员标识（0=普通用户 1=管理员）
-- ========================================
INSERT INTO public.sys_dict_type (id, tenant_id, dict_name, dict_type, dict_description, status, sort_order, delete_flag, create_time, update_time)
VALUES (6, NULL, '管理员标识', 'admin_flag', '区分管理员与普通用户', 1, 6, 0, NOW(), NOW());

INSERT INTO public.sys_dict_data (id, tenant_id, dict_type, dict_label, dict_value, sort_order, status, is_default, delete_flag, create_time, update_time)
VALUES
    (50, NULL, 'admin_flag', '普通用户', '0', 1, 1, 1, 0, NOW(), NOW()),
    (51, NULL, 'admin_flag', '管理员', '1', 2, 1, 0, 0, NOW(), NOW());

-- ========================================
-- 用户类型（0=运维人员 1=教师 2=学生）
-- ========================================
INSERT INTO public.sys_dict_type (id, tenant_id, dict_name, dict_type, dict_description, status, sort_order, delete_flag, create_time, update_time)
VALUES (7, NULL, '用户类型', 'user_type', '系统用户的业务角色类型', 1, 7, 0, NOW(), NOW());

INSERT INTO public.sys_dict_data (id, tenant_id, dict_type, dict_label, dict_value, sort_order, status, is_default, delete_flag, create_time, update_time)
VALUES
    (60, NULL, 'user_type', '运维人员', '0', 1, 1, 0, 0, NOW(), NOW()),
    (61, NULL, 'user_type', '教师', '1', 2, 1, 1, 0, NOW(), NOW()),
    (62, NULL, 'user_type', '学生', '2', 3, 1, 0, 0, NOW(), NOW());

-- ========================================
-- 定时任务触发类型（0=定时触发 1=手动触发）
-- ========================================
INSERT INTO public.sys_dict_type (id, tenant_id, dict_name, dict_type, dict_description, status, sort_order, delete_flag, create_time, update_time)
VALUES (8, NULL, '任务触发类型', 'job_trigger_type', '定时任务的触发方式', 1, 8, 0, NOW(), NOW());

INSERT INTO public.sys_dict_data (id, tenant_id, dict_type, dict_label, dict_value, sort_order, status, is_default, delete_flag, create_time, update_time)
VALUES
    (70, NULL, 'job_trigger_type', '定时触发', '0', 1, 1, 1, 0, NOW(), NOW()),
    (71, NULL, 'job_trigger_type', '手动触发', '1', 2, 1, 0, 0, NOW(), NOW());

-- ========================================
-- 定时任务执行状态（0=失败 1=成功）
-- ========================================
INSERT INTO public.sys_dict_type (id, tenant_id, dict_name, dict_type, dict_description, status, sort_order, delete_flag, create_time, update_time)
VALUES (9, NULL, '任务执行状态', 'job_exec_status', '定时任务单次执行的结果状态', 1, 9, 0, NOW(), NOW());

INSERT INTO public.sys_dict_data (id, tenant_id, dict_type, dict_label, dict_value, sort_order, status, is_default, delete_flag, create_time, update_time)
VALUES
    (80, NULL, 'job_exec_status', '成功', '1', 1, 1, 1, 0, NOW(), NOW()),
    (81, NULL, 'job_exec_status', '失败', '0', 2, 1, 0, 0, NOW(), NOW());

-- ========================================
-- 操作结果（0=失败 1=成功）
-- ========================================
INSERT INTO public.sys_dict_type (id, tenant_id, dict_name, dict_type, dict_description, status, sort_order, delete_flag, create_time, update_time)
VALUES (10, NULL, '操作结果', 'operation_result', '系统操作日志的执行结果', 1, 10, 0, NOW(), NOW());

INSERT INTO public.sys_dict_data (id, tenant_id, dict_type, dict_label, dict_value, sort_order, status, is_default, delete_flag, create_time, update_time)
VALUES
    (90, NULL, 'operation_result', '成功', '1', 1, 1, 1, 0, NOW(), NOW()),
    (91, NULL, 'operation_result', '失败', '0', 2, 1, 0, 0, NOW(), NOW());
