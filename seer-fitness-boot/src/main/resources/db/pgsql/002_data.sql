-- ====================================================================================================
-- Seer Fitness Edu - 统一初始化数据脚本
-- 执行前提：已执行 001_schema.sql
-- tenant_id = NULL 表示平台级数据
-- ====================================================================================================

-- ========================================
-- 第一部分：平台菜单 (tenant_id=NULL)
-- ========================================

-- 平台管理目录
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES (10000, NULL, '平台管理', 0, 0, '/platform', NULL, 'CloudServerOutlined', 1, 1, 0, NOW(), NOW());

-- 租户管理
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (10100, NULL, '租户管理',  10000, 1, '/platform/tenant', NULL, 'ShopOutlined', 1, 1, 0, NOW(), NOW()),
    (10101, NULL, '查看租户',  10100, 2, NULL, 'tenant:view',   NULL, 1, 1, 0, NOW(), NOW()),
    (10102, NULL, '创建租户',  10100, 2, NULL, 'tenant:create', NULL, 2, 1, 0, NOW(), NOW()),
    (10103, NULL, '更新租户',  10100, 2, NULL, 'tenant:update', NULL, 3, 1, 0, NOW(), NOW()),
    (10104, NULL, '禁用租户',  10100, 2, NULL, 'tenant:update', NULL, 4, 1, 0, NOW(), NOW()),
    (10105, NULL, '启用租户',  10100, 2, NULL, 'tenant:update', NULL, 5, 1, 0, NOW(), NOW()),
    (10106, NULL, '删除租户',  10100, 2, NULL, 'tenant:delete', NULL, 6, 1, 0, NOW(), NOW());

-- 租户角色管理
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (10200, NULL, '租户角色',  10000, 1, '/platform/role', NULL, 'TeamOutlined', 2, 1, 0, NOW(), NOW()),
    (10201, NULL, '查看角色',  10200, 2, NULL, 'platform:role:view',   NULL, 1, 1, 0, NOW(), NOW()),
    (10202, NULL, '创建角色',  10200, 2, NULL, 'platform:role:create', NULL, 2, 1, 0, NOW(), NOW()),
    (10203, NULL, '更新角色',  10200, 2, NULL, 'platform:role:update', NULL, 3, 1, 0, NOW(), NOW()),
    (10204, NULL, '删除角色',  10200, 2, NULL, 'platform:role:delete', NULL, 4, 1, 0, NOW(), NOW()),
    (10205, NULL, '分配菜单',  10200, 2, NULL, 'platform:role:assign', NULL, 5, 1, 0, NOW(), NOW());

-- 菜单管理
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (1300, NULL, '菜单管理', 10000, 1, '/platform/menu', NULL, 'MenuOutlined', 3, 1, 0, NOW(), NOW()),
    (1301, NULL, '查看菜单', 1300,  2, NULL, 'menu:view',   NULL, 1, 1, 0, NOW(), NOW()),
    (1302, NULL, '创建菜单', 1300,  2, NULL, 'menu:create', NULL, 2, 1, 0, NOW(), NOW()),
    (1303, NULL, '更新菜单', 1300,  2, NULL, 'menu:update', NULL, 3, 1, 0, NOW(), NOW()),
    (1304, NULL, '删除菜单', 1300,  2, NULL, 'menu:delete', NULL, 4, 1, 0, NOW(), NOW());

-- 定时任务
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (10500, NULL, '定时任务',  10000, 1, '/platform/job', NULL, 'ClockCircleOutlined', 4, 1, 0, NOW(), NOW()),
    (10501, NULL, '查看任务',  10500, 2, NULL, 'job:view',    NULL, 1, 1, 0, NOW(), NOW()),
    (10502, NULL, '创建任务',  10500, 2, NULL, 'job:create',  NULL, 2, 1, 0, NOW(), NOW()),
    (10503, NULL, '编辑任务',  10500, 2, NULL, 'job:update',  NULL, 3, 1, 0, NOW(), NOW()),
    (10504, NULL, '删除任务',  10500, 2, NULL, 'job:delete',  NULL, 4, 1, 0, NOW(), NOW()),
    (10505, NULL, '手动触发',  10500, 2, NULL, 'job:trigger', NULL, 5, 1, 0, NOW(), NOW()),
    (10506, NULL, '查看日志',  10500, 2, NULL, 'job:view',    NULL, 6, 1, 0, NOW(), NOW());

-- 文件存储配置
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (10300, NULL, '文件存储',     10000, 1, '/platform/file-config', NULL, 'CloudUploadOutlined', 5, 1, 0, NOW(), NOW()),
    (10301, NULL, '查看存储配置', 10300, 2, NULL, 'file:config:view',   NULL, 1, 1, 0, NOW(), NOW()),
    (10302, NULL, '创建存储配置', 10300, 2, NULL, 'file:config:create', NULL, 2, 1, 0, NOW(), NOW()),
    (10303, NULL, '更新存储配置', 10300, 2, NULL, 'file:config:update', NULL, 3, 1, 0, NOW(), NOW()),
    (10304, NULL, '删除存储配置', 10300, 2, NULL, 'file:config:delete', NULL, 4, 1, 0, NOW(), NOW());

-- 系统配置
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (10400, NULL, '系统配置', 10000, 1, '/platform/config', NULL, 'ControlOutlined', 6, 1, 0, NOW(), NOW()),
    (10401, NULL, '查看配置', 10400, 2, NULL, 'config:view',   NULL, 1, 1, 0, NOW(), NOW()),
    (10402, NULL, '新增配置', 10400, 2, NULL, 'config:create', NULL, 2, 1, 0, NOW(), NOW()),
    (10403, NULL, '修改配置', 10400, 2, NULL, 'config:update', NULL, 3, 1, 0, NOW(), NOW()),
    (10404, NULL, '删除配置', 10400, 2, NULL, 'config:delete', NULL, 4, 1, 0, NOW(), NOW());

-- ========================================
-- 第二部分：租户菜单模板 (tenant_id=NULL)
-- ========================================

-- 系统管理目录
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES (1000, NULL, '系统管理', 0, 0, '/system', NULL, 'SettingOutlined', 1, 1, 0, NOW(), NOW());

-- 用户管理
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (1100, NULL, '用户管理',   1000, 1, '/system/user', NULL, 'UserOutlined', 1, 1, 0, NOW(), NOW()),
    (1101, NULL, '查看用户',   1100, 2, NULL, 'user:view',           NULL, 1, 1, 0, NOW(), NOW()),
    (1102, NULL, '创建用户',   1100, 2, NULL, 'user:create',         NULL, 2, 1, 0, NOW(), NOW()),
    (1103, NULL, '更新用户',   1100, 2, NULL, 'user:update',         NULL, 3, 1, 0, NOW(), NOW()),
    (1104, NULL, '删除用户',   1100, 2, NULL, 'user:delete',         NULL, 4, 1, 0, NOW(), NOW()),
    (1105, NULL, '初始化密码', 1100, 2, NULL, 'user:init-password',  NULL, 5, 1, 0, NOW(), NOW()),
    (1106, NULL, '重置密码',   1100, 2, NULL, 'user:reset-password', NULL, 6, 1, 0, NOW(), NOW());

-- 角色管理
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (1200, NULL, '角色管理', 1000, 1, '/system/role', NULL, 'SafetyOutlined', 2, 1, 0, NOW(), NOW()),
    (1201, NULL, '查看角色', 1200, 2, NULL, 'role:view',   NULL, 1, 1, 0, NOW(), NOW()),
    (1202, NULL, '创建角色', 1200, 2, NULL, 'role:create', NULL, 2, 1, 0, NOW(), NOW()),
    (1203, NULL, '更新角色', 1200, 2, NULL, 'role:update', NULL, 3, 1, 0, NOW(), NOW()),
    (1204, NULL, '删除角色', 1200, 2, NULL, 'role:delete', NULL, 4, 1, 0, NOW(), NOW()),
    (1205, NULL, '分配权限', 1200, 2, NULL, 'role:assign', NULL, 5, 1, 0, NOW(), NOW());

-- 组织管理
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (1400, NULL, '组织管理', 1000, 1, '/system/organization', NULL, 'ApartmentOutlined', 3, 1, 0, NOW(), NOW()),
    (1401, NULL, '查看组织', 1400, 2, NULL, 'organization:view',   NULL, 1, 1, 0, NOW(), NOW()),
    (1402, NULL, '创建组织', 1400, 2, NULL, 'organization:create', NULL, 2, 1, 0, NOW(), NOW()),
    (1403, NULL, '更新组织', 1400, 2, NULL, 'organization:update', NULL, 3, 1, 0, NOW(), NOW()),
    (1404, NULL, '删除组织', 1400, 2, NULL, 'organization:delete', NULL, 4, 1, 0, NOW(), NOW());

-- 文件管理
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (1500, NULL, '文件管理', 1000, 1, '/system/file', NULL, 'FileOutlined', 4, 1, 0, NOW(), NOW()),
    (1501, NULL, '删除文件', 1500, 2, NULL, 'file:delete', NULL, 1, 1, 0, NOW(), NOW());

-- 数据字典目录
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES (3000, NULL, '数据字典', 0, 0, '/dict', NULL, 'BookOutlined', 3, 1, 0, NOW(), NOW());

-- 字典类型
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (3100, NULL, '字典类型',     3000, 1, '/dict/type', NULL, 'DatabaseOutlined', 1, 1, 0, NOW(), NOW()),
    (3101, NULL, '查看字典类型', 3100, 2, NULL, 'dict:type:view',   NULL, 1, 1, 0, NOW(), NOW()),
    (3102, NULL, '创建字典类型', 3100, 2, NULL, 'dict:type:create', NULL, 2, 1, 0, NOW(), NOW()),
    (3103, NULL, '更新字典类型', 3100, 2, NULL, 'dict:type:update', NULL, 3, 1, 0, NOW(), NOW()),
    (3104, NULL, '删除字典类型', 3100, 2, NULL, 'dict:type:delete', NULL, 4, 1, 0, NOW(), NOW());

-- 字典数据
INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (3200, NULL, '字典数据',     3000, 1, '/dict/data', NULL, 'UnorderedListOutlined', 2, 1, 0, NOW(), NOW()),
    (3201, NULL, '查看字典数据', 3200, 2, NULL, NULL,                NULL, 1, 1, 0, NOW(), NOW()),
    (3202, NULL, '创建字典数据', 3200, 2, NULL, 'dict:data:create', NULL, 2, 1, 0, NOW(), NOW()),
    (3203, NULL, '更新字典数据', 3200, 2, NULL, 'dict:data:update', NULL, 3, 1, 0, NOW(), NOW()),
    (3204, NULL, '删除字典数据', 3200, 2, NULL, 'dict:data:delete', NULL, 4, 1, 0, NOW(), NOW());

-- ========================================
-- 第三部分：平台角色模板 (tenant_id=NULL)
-- ========================================

INSERT INTO public.sys_role (id, tenant_id, role_name, role_code, description, status, delete_flag, create_time, update_time)
VALUES (100, NULL, '租户管理员', 'TENANT_ADMIN', '拥有所有系统管理权限', 1, 0, NOW(), NOW());

-- 租户管理员模板菜单权限（系统管理 + 数据字典 + 文件管理）
INSERT INTO public.sys_role_menu (tenant_id, role_id, menu_id) VALUES
    -- 系统管理
    (NULL, 100, 1000),
    (NULL, 100, 1100), (NULL, 100, 1101), (NULL, 100, 1102), (NULL, 100, 1103),
    (NULL, 100, 1104), (NULL, 100, 1105), (NULL, 100, 1106),
    (NULL, 100, 1200), (NULL, 100, 1201), (NULL, 100, 1202), (NULL, 100, 1203),
    (NULL, 100, 1204), (NULL, 100, 1205),
    (NULL, 100, 1400), (NULL, 100, 1401), (NULL, 100, 1402), (NULL, 100, 1403),
    (NULL, 100, 1404),
    -- 文件管理
    (NULL, 100, 1500), (NULL, 100, 1501);

-- ========================================
-- 第四部分：平台管理员用户 (tenant_id=NULL)
-- ========================================

-- 超级管理员，密码: Aa123456!
INSERT INTO public.sys_user (id, tenant_id, username, password, real_name, admin_flag, user_type, status, delete_flag, create_time, update_time)
VALUES (1, NULL, 'admin', '$2a$12$zdxSVanWeNihjqgNN/A/yej4PKwqUlb4ymtVKnkt.GQpJD3daqBie', '超级管理员', 1, 0, 1, 0, NOW(), NOW());

-- ========================================
-- 第五部分：数据字典
-- ========================================

INSERT INTO public.sys_dict_type (id, tenant_id, dict_name, dict_type, dict_description, status, sort_order, delete_flag, create_time, update_time) VALUES
    (1,  NULL, '用户状态',     'user_status',      '用户账号状态',               1, 1,  0, NOW(), NOW()),
    (2,  NULL, '租户状态',     'tenant_status',    '租户账号状态',               1, 2,  0, NOW(), NOW()),
    (3,  NULL, '通用状态',     'common_status',    '通用的启用/禁用状态',        1, 3,  0, NOW(), NOW()),
    (4,  NULL, '菜单节点类型', 'menu_type',        '菜单树节点的展示类型',       1, 4,  0, NOW(), NOW()),
    (5,  NULL, '菜单归属类型', 'menu_display_type','区分平台菜单与租户模板菜单', 1, 5,  0, NOW(), NOW()),
    (6,  NULL, '管理员标识',   'admin_flag',       '区分管理员与普通用户',       1, 6,  0, NOW(), NOW()),
    (7,  NULL, '用户类型',     'user_type',        '系统用户的业务角色类型',     1, 7,  0, NOW(), NOW()),
    (8,  NULL, '任务触发类型', 'job_trigger_type', '定时任务的触发方式',         1, 8,  0, NOW(), NOW()),
    (9,  NULL, '任务执行状态', 'job_exec_status',  '定时任务单次执行的结果状态', 1, 9,  0, NOW(), NOW()),
    (10, NULL, '操作结果',     'operation_result', '系统操作日志的执行结果',     1, 10, 0, NOW(), NOW());

INSERT INTO public.sys_dict_data (id, tenant_id, dict_type, dict_label, dict_value, sort_order, status, is_default, delete_flag, create_time, update_time) VALUES
    -- user_status
    (1,  NULL, 'user_status',       '启用',       '1', 1, 1, 1, 0, NOW(), NOW()),
    (2,  NULL, 'user_status',       '禁用',       '0', 2, 1, 0, 0, NOW(), NOW()),
    -- tenant_status
    (10, NULL, 'tenant_status',     '待激活',     '0', 1, 1, 1, 0, NOW(), NOW()),
    (11, NULL, 'tenant_status',     '正常',       '1', 2, 1, 0, 0, NOW(), NOW()),
    (12, NULL, 'tenant_status',     '已禁用',     '2', 3, 1, 0, 0, NOW(), NOW()),
    (13, NULL, 'tenant_status',     '已过期',     '3', 4, 1, 0, 0, NOW(), NOW()),
    -- common_status
    (20, NULL, 'common_status',     '启用',       '1', 1, 1, 1, 0, NOW(), NOW()),
    (21, NULL, 'common_status',     '禁用',       '0', 2, 1, 0, 0, NOW(), NOW()),
    -- menu_type
    (30, NULL, 'menu_type',         '目录',       '0', 1, 1, 0, 0, NOW(), NOW()),
    (31, NULL, 'menu_type',         '菜单',       '1', 2, 1, 1, 0, NOW(), NOW()),
    (32, NULL, 'menu_type',         '按钮',       '2', 3, 1, 0, 0, NOW(), NOW()),
    -- menu_display_type
    (40, NULL, 'menu_display_type', '平台菜单',   '1', 1, 1, 1, 0, NOW(), NOW()),
    (41, NULL, 'menu_display_type', '租户模板菜单','2', 2, 1, 0, 0, NOW(), NOW()),
    -- admin_flag
    (50, NULL, 'admin_flag',        '普通用户',   '0', 1, 1, 1, 0, NOW(), NOW()),
    (51, NULL, 'admin_flag',        '管理员',     '1', 2, 1, 0, 0, NOW(), NOW()),
    -- user_type
    (60, NULL, 'user_type',         '运维人员',   '0', 1, 1, 0, 0, NOW(), NOW()),
    (61, NULL, 'user_type',         '教师',       '1', 2, 1, 1, 0, NOW(), NOW()),
    (62, NULL, 'user_type',         '学生',       '2', 3, 1, 0, 0, NOW(), NOW()),
    -- job_trigger_type
    (70, NULL, 'job_trigger_type',  '定时触发',   '0', 1, 1, 1, 0, NOW(), NOW()),
    (71, NULL, 'job_trigger_type',  '手动触发',   '1', 2, 1, 0, 0, NOW(), NOW()),
    -- job_exec_status
    (80, NULL, 'job_exec_status',   '成功',       '1', 1, 1, 1, 0, NOW(), NOW()),
    (81, NULL, 'job_exec_status',   '失败',       '0', 2, 1, 0, 0, NOW(), NOW()),
    -- operation_result
    (90, NULL, 'operation_result',  '成功',       '1', 1, 1, 1, 0, NOW(), NOW()),
    (91, NULL, 'operation_result',  '失败',       '0', 2, 1, 0, 0, NOW(), NOW());

-- ========================================
-- 第六部分：系统配置
-- ========================================

INSERT INTO public.sys_config (config_key, config_value, config_name, config_type, remark, create_by, create_time, update_by, update_time, delete_flag) VALUES
    -- 验证码
    ('security.captcha.enabled',                        'true',         '验证码开关',               1, '是否启用图形验证码',                               'system', NOW(), 'system', NOW(), 0),
    ('security.captcha.expire-seconds',                 '300',          '验证码过期时间(秒)',        1, '验证码Redis缓存过期秒数',                          'system', NOW(), 'system', NOW(), 0),
    ('security.captcha.length',                         '4',            '验证码位数',               1, '验证码字符个数（4-8）',                            'system', NOW(), 'system', NOW(), 0),
    ('security.captcha.type',                           'DIGIT',        '验证码类型',               1, 'DIGIT=纯数字 CHAR=字母 MIXED=混合',                'system', NOW(), 'system', NOW(), 0),
    -- 密码策略
    ('security.password.initial-password',              'Aa123456!',    '用户初始密码',             1, '管理员重置/初始化密码时使用的默认值',              'system', NOW(), 'system', NOW(), 0),
    ('security.password.policy.min-length',             '8',            '密码最小长度',             1, '用户密码最少字符数',                               'system', NOW(), 'system', NOW(), 0),
    ('security.password.policy.max-length',             '15',           '密码最大长度',             1, '用户密码最多字符数',                               'system', NOW(), 'system', NOW(), 0),
    ('security.password.policy.require-lowercase',      'true',         '密码需含小写字母',         1, 'true=必须包含 a-z',                                'system', NOW(), 'system', NOW(), 0),
    ('security.password.policy.require-uppercase',      'true',         '密码需含大写字母',         1, 'true=必须包含 A-Z',                                'system', NOW(), 'system', NOW(), 0),
    ('security.password.policy.require-digit',          'true',         '密码需含数字',             1, 'true=必须包含 0-9',                                'system', NOW(), 'system', NOW(), 0),
    ('security.password.policy.require-special',        'true',         '密码需含特殊字符',         1, 'true=必须包含特殊字符',                            'system', NOW(), 'system', NOW(), 0),
    -- 账户锁定
    ('security.account-lock.enabled',                   'true',         '账户锁定开关',             1, '登录失败超限后是否锁定账户',                       'system', NOW(), 'system', NOW(), 0),
    ('security.account-lock.attempts.max-fail-count',   '5',            '最大登录失败次数',         1, '超过此次数账户被锁定',                             'system', NOW(), 'system', NOW(), 0),
    ('security.account-lock.attempts.auto-reset-hours', '24',           '失败次数自动重置(小时)',   1, 'N小时内无失败则重置计数',                          'system', NOW(), 'system', NOW(), 0),
    ('security.account-lock.lock-time.base-minutes',    '30',           '基础锁定时长(分钟)',       1, '渐进式锁定的初始锁定分钟数',                       'system', NOW(), 'system', NOW(), 0),
    ('security.account-lock.ip-lock.enabled',           'true',         'IP锁定开关',               1, '同一IP失败过多时锁定',                             'system', NOW(), 'system', NOW(), 0),
    ('security.account-lock.ip-lock.max-attempts',      '20',           'IP最大失败次数',           1, '超过后锁定该IP',                                   'system', NOW(), 'system', NOW(), 0),
    ('security.account-lock.ip-lock.lock-minutes',      '60',           'IP锁定时长(分钟)',         1, 'IP被锁定的持续时间',                               'system', NOW(), 'system', NOW(), 0),
    ('security.account-lock.ip-lock.record-hours',      '2',            'IP失败记录保留时长(小时)', 1, 'N小时内IP失败次数会被累计',                        'system', NOW(), 'system', NOW(), 0),
    ('security.account-lock.reset.on-success',          'true',         '登录成功重置失败记录',     1, 'true=登录成功后清除该用户的失败计数',              'system', NOW(), 'system', NOW(), 0),
    ('security.account-lock.whitelist.users',           'admin,system', '账户白名单',               1, '不受锁定限制的用户名，英文逗号分隔',               'system', NOW(), 'system', NOW(), 0),
    ('security.account-lock.whitelist.ips',             '127.0.0.1',    'IP白名单',                 1, '不受锁定限制的IP，英文逗号分隔',                   'system', NOW(), 'system', NOW(), 0),
    -- 定时任务
    ('scheduler.pool-size',                             '5',            '定时任务线程池大小',       1, '修改后重启生效',                                   'system', NOW(), 'system', NOW(), 0),
    -- 文件上传限制
    ('file.upload.image-max-mb',                        '10',           '图片上传大小限制(MB)',     1, '图片类文件最大允许上传大小',                       'system', NOW(), 'system', NOW(), 0),
    ('file.upload.video-max-mb',                        '500',          '视频上传大小限制(MB)',     1, '视频类文件最大允许上传大小',                       'system', NOW(), 'system', NOW(), 0),
    ('file.upload.other-max-mb',                        '100',          '其他文件上传大小限制(MB)', 1, '非图片非视频文件最大允许上传大小',                 'system', NOW(), 'system', NOW(), 0);

-- ========================================
-- 第七部分：文件存储配置
-- ========================================

-- 默认本地存储（开发用）
INSERT INTO public.sys_file_config (config_name, storage_type, is_active, config, remark, create_by, create_time, update_by, update_time, delete_flag)
VALUES ('本地存储', 'local', 1,
        '{"basePath":"./uploads","urlPrefix":"http://localhost:8070/system/file/local"}',
        '文件存储到本地磁盘，仅适合开发环境', 'system', NOW(), 'system', NOW(), 0);

-- MinIO 配置模板（未激活，管理员填写参数后可激活）
INSERT INTO public.sys_file_config (config_name, storage_type, is_active, config, remark, create_by, create_time, update_by, update_time, delete_flag)
VALUES ('MinIO 存储', 'minio', 0,
        '{"endpoint":"http://minio:9000","bucket":"seer-fitness","accessKey":"","secretKey":"","publicBucket":true,"presignedExpireSeconds":3600}',
        '生产环境使用 MinIO 对象存储', 'system', NOW(), 'system', NOW(), 0);
