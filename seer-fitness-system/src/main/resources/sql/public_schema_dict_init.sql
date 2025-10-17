-- ====================================================================================================
-- Public Schema 字典数据初始化脚本
-- 说明：此脚本用于初始化 public schema 中的所有字典数据
-- 包含：
--   1. 租户管理字典（tenant_status, tenant_init_step, tenant_log_status）
--   2. 通用业务字典（user_gender, user_status, student_status, course_status, payment_status）
-- 使用场景：
--   - 应用首次启动时执行
--   - 手动初始化或修复字典数据
-- 注意：
--   - 使用 ON CONFLICT DO NOTHING 避免重复插入
--   - 所有租户共享这些字典数据，通过 @PublicSchema 注解自动路由
-- 创建时间：2025-01-10
-- ====================================================================================================

-- 切换到 public schema
SET search_path TO public;

-- ========================================
-- 第一部分：创建字典表（如果不存在）
-- ========================================

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

COMMENT ON TABLE public.sys_dict_type IS '数据字典类型表（所有租户共享）';

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

COMMENT ON TABLE public.sys_dict_data IS '数据字典数据表（所有租户共享）';

-- ========================================
-- 第二部分：租户管理字典
-- ========================================

-- 1. 租户状态字典类型
INSERT INTO public.sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (1001, '租户状态', 'tenant_status', '租户（学校）的状态：待激活、正常、已禁用、已过期', 1, 1, 'system')
ON CONFLICT (dict_type) DO NOTHING;

-- 租户状态字典数据
INSERT INTO public.sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (10001, 'tenant_status', '待激活', '0', '租户已创建但尚未激活', '', 'warning', 1, 1, 'system'),
  (10002, 'tenant_status', '正常', '1', '租户正常运行中', '', 'success', 1, 2, 'system'),
  (10003, 'tenant_status', '已禁用', '2', '租户已被管理员禁用', '', 'danger', 1, 3, 'system'),
  (10004, 'tenant_status', '已过期', '3', '租户已过期，需要续费', '', 'info', 1, 4, 'system')
ON CONFLICT (id) DO NOTHING;

-- 2. 租户初始化步骤字典类型
INSERT INTO public.sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (1002, '租户初始化步骤', 'tenant_init_step', '租户 Schema 初始化过程的步骤类型', 1, 2, 'system')
ON CONFLICT (dict_type) DO NOTHING;

-- 租户初始化步骤字典数据
INSERT INTO public.sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (10011, 'tenant_init_step', '创建Schema', 'CREATE_SCHEMA', '创建租户独立的 PostgreSQL Schema', '', 'primary', 1, 1, 'system'),
  (10012, 'tenant_init_step', '创建表结构', 'CREATE_TABLE', '在租户 Schema 中创建业务表', '', 'primary', 1, 2, 'system'),
  (10013, 'tenant_init_step', '插入基础数据', 'INSERT_DATA', '插入角色、菜单等基础数据', '', 'primary', 1, 3, 'system'),
  (10014, 'tenant_init_step', '创建管理员', 'CREATE_ADMIN', '创建租户管理员账号', '', 'primary', 1, 4, 'system'),
  (10015, 'tenant_init_step', '回滚清理', 'ROLLBACK', '初始化失败时的回滚操作', '', 'danger', 1, 5, 'system')
ON CONFLICT (id) DO NOTHING;

-- 3. 租户日志状态字典类型
INSERT INTO public.sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (1003, '租户日志状态', 'tenant_log_status', '租户初始化日志的状态', 1, 3, 'system')
ON CONFLICT (dict_type) DO NOTHING;

-- 租户日志状态字典数据
INSERT INTO public.sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (10021, 'tenant_log_status', '进行中', '0', '步骤正在执行', '', 'info', 1, 1, 'system'),
  (10022, 'tenant_log_status', '成功', '1', '步骤执行成功', '', 'success', 1, 2, 'system'),
  (10023, 'tenant_log_status', '失败', '2', '步骤执行失败', '', 'danger', 1, 3, 'system')
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 第三部分：通用业务字典
-- ========================================

-- 4. 用户性别字典类型
INSERT INTO public.sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (5001, '用户性别', 'user_gender', '用户性别：男、女、未知', 1, 10, 'system')
ON CONFLICT (dict_type) DO NOTHING;

-- 用户性别字典数据
INSERT INTO public.sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, status, sort_order, create_by)
VALUES
  (50001, 'user_gender', '男', '1', '男性', 1, 1, 'system'),
  (50002, 'user_gender', '女', '2', '女性', 1, 2, 'system'),
  (50003, 'user_gender', '未知', '0', '未知性别', 1, 3, 'system')
ON CONFLICT (id) DO NOTHING;

-- 5. 用户状态字典类型
INSERT INTO public.sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (5002, '用户状态', 'user_status', '用户账号状态', 1, 11, 'system')
ON CONFLICT (dict_type) DO NOTHING;

-- 用户状态字典数据
INSERT INTO public.sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (50011, 'user_status', '启用', '1', '账号正常使用', '', 'success', 1, 1, 'system'),
  (50012, 'user_status', '禁用', '0', '账号被禁用', '', 'danger', 1, 2, 'system')
ON CONFLICT (id) DO NOTHING;

-- 6. 学员状态字典类型
INSERT INTO public.sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (5003, '学员状态', 'student_status', '学员当前学习状态', 1, 12, 'system')
ON CONFLICT (dict_type) DO NOTHING;

-- 学员状态字典数据
INSERT INTO public.sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (50021, 'student_status', '在读', '1', '正在学习中', '', 'success', 1, 1, 'system'),
  (50022, 'student_status', '休学', '2', '暂停学习', '', 'warning', 1, 2, 'system'),
  (50023, 'student_status', '毕业', '3', '已完成学业', '', 'info', 1, 3, 'system'),
  (50024, 'student_status', '退学', '4', '已退学', '', 'danger', 1, 4, 'system')
ON CONFLICT (id) DO NOTHING;

-- 7. 课程状态字典类型
INSERT INTO public.sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (5004, '课程状态', 'course_status', '课程发布状态', 1, 13, 'system')
ON CONFLICT (dict_type) DO NOTHING;

-- 课程状态字典数据
INSERT INTO public.sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (50031, 'course_status', '草稿', '0', '课程编辑中', '', 'info', 1, 1, 'system'),
  (50032, 'course_status', '已发布', '1', '课程已上线', '', 'success', 1, 2, 'system'),
  (50033, 'course_status', '已下架', '2', '课程已下线', '', 'warning', 1, 3, 'system')
ON CONFLICT (id) DO NOTHING;

-- 8. 缴费状态字典类型
INSERT INTO public.sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (5005, '缴费状态', 'payment_status', '学员缴费状态', 1, 14, 'system')
ON CONFLICT (dict_type) DO NOTHING;

-- 缴费状态字典数据
INSERT INTO public.sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (50041, 'payment_status', '待缴费', '0', '尚未缴费', '', 'warning', 1, 1, 'system'),
  (50042, 'payment_status', '已缴费', '1', '已全额缴费', '', 'success', 1, 2, 'system'),
  (50043, 'payment_status', '部分缴费', '2', '已缴纳部分费用', '', 'info', 1, 3, 'system'),
  (50044, 'payment_status', '已退费', '3', '已办理退费', '', 'danger', 1, 4, 'system')
ON CONFLICT (id) DO NOTHING;

-- 9. 是否标识字典类型
INSERT INTO public.sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (5006, '是否标识', 'yes_no', '通用的是/否标识', 1, 15, 'system')
ON CONFLICT (dict_type) DO NOTHING;

-- 是否标识字典数据
INSERT INTO public.sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (50051, 'yes_no', '是', '1', '是', '', 'success', 1, 1, 'system'),
  (50052, 'yes_no', '否', '0', '否', '', 'info', 1, 2, 'system')
ON CONFLICT (id) DO NOTHING;

-- 10. 启用状态字典类型
INSERT INTO public.sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (5007, '启用状态', 'enable_status', '通用的启用/禁用状态', 1, 16, 'system')
ON CONFLICT (dict_type) DO NOTHING;

-- 启用状态字典数据
INSERT INTO public.sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (50061, 'enable_status', '启用', '1', '功能已启用', '', 'success', 1, 1, 'system'),
  (50062, 'enable_status', '禁用', '0', '功能已禁用', '', 'danger', 1, 2, 'system')
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 验证插入结果
-- ========================================

DO $$
DECLARE
    type_count INTEGER;
    data_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO type_count FROM public.sys_dict_type;
    SELECT COUNT(*) INTO data_count FROM public.sys_dict_data;

    RAISE NOTICE '=====================================';
    RAISE NOTICE 'Public Schema 字典初始化完成';
    RAISE NOTICE '字典类型总数: %', type_count;
    RAISE NOTICE '字典数据总数: %', data_count;
    RAISE NOTICE '=====================================';
END $$;

-- 显示各类字典统计
SELECT
    '租户管理字典' AS category,
    COUNT(*) AS type_count
FROM public.sys_dict_type
WHERE dict_type LIKE 'tenant%'
UNION ALL
SELECT
    '通用业务字典' AS category,
    COUNT(*) AS type_count
FROM public.sys_dict_type
WHERE dict_type NOT LIKE 'tenant%';

SELECT
    dict_type,
    dict_name,
    COUNT(*) AS data_count
FROM public.sys_dict_type dt
LEFT JOIN public.sys_dict_data dd ON dt.dict_type = dd.dict_type
GROUP BY dt.dict_type, dt.dict_name
ORDER BY dt.sort_order;

-- ========================================
-- 第四部分：项目管理字典
-- ========================================

-- 11. 单位字典类型
INSERT INTO public.sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (6001, '计量单位', 'unit_type', '体育项目计量单位：次、厘米、秒、米等', 1, 20, 'system')
ON CONFLICT (dict_type) DO NOTHING;

-- 单位字典数据
INSERT INTO public.sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (60001, 'unit_type', '次', '1', '计数单位（如跳绳次数）', '', '', 1, 1, 'system'),
  (60002, 'unit_type', '厘米', '2', '长度单位（如跳远距离）', '', '', 1, 2, 'system'),
  (60003, 'unit_type', '秒', '3', '时间单位（如跑步时间）', '', '', 1, 3, 'system'),
  (60004, 'unit_type', '米', '4', '距离单位（如跑步距离）', '', '', 1, 4, 'system'),
  (60005, 'unit_type', '分钟', '5', '时间单位（如训练时长）', '', '', 1, 5, 'system'),
  (60006, 'unit_type', '千克', '6', '重量单位（如杠铃重量）', '', '', 1, 6, 'system')
ON CONFLICT (id) DO NOTHING;
