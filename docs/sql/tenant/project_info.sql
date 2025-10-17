-- ====================================================================================================
-- 项目信息表 DDL
-- 说明：此脚本包含平台项目表（public schema）和租户项目表（租户 schema）
-- 创建时间：2025-01-17
-- ====================================================================================================

-- ==========================================
-- 第一部分：平台项目表（public schema）
-- 说明：平台统一管理的项目库，供学校分配使用
-- 执行方式：连接到数据库后，直接执行本段SQL
-- ==========================================

CREATE TABLE public.seer_project_info (
    -- 主键
    id BIGSERIAL PRIMARY KEY,

    -- 核心字段
    project_code VARCHAR(50) UNIQUE NOT NULL,      -- 项目编号（唯一标识）如：pull_up
    project_name VARCHAR(100) NOT NULL,            -- 项目名称：引体向上
    unit INT NOT NULL,                             -- 单位（字典：unit_type）
    training_duration INT,                         -- 训练时长（秒）
    is_higher_better INT DEFAULT 1,                -- 成绩越大越好（字典：yes_no，1=是，0=否）
    sort_order INT DEFAULT 0,                      -- 排序（数字越小越靠前）

    -- 状态管理
    status INT DEFAULT 1,                          -- 状态（字典：enable_status，0=禁用，1=启用）

    -- 审计字段
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 备注
    remark VARCHAR(500)
);

-- 添加注释
COMMENT ON TABLE public.seer_project_info IS '项目信息表（平台统一管理，供学校分配）';
COMMENT ON COLUMN public.seer_project_info.id IS '项目ID';
COMMENT ON COLUMN public.seer_project_info.project_code IS '项目编号（唯一）';
COMMENT ON COLUMN public.seer_project_info.project_name IS '项目名称';
COMMENT ON COLUMN public.seer_project_info.unit IS '单位（字典：unit_type，1=次，2=厘米，3=秒，4=米）';
COMMENT ON COLUMN public.seer_project_info.training_duration IS '建议训练时长（秒）';
COMMENT ON COLUMN public.seer_project_info.is_higher_better IS '成绩越大越好（字典：yes_no，1=是，0=否）';
COMMENT ON COLUMN public.seer_project_info.sort_order IS '排序';
COMMENT ON COLUMN public.seer_project_info.status IS '状态（字典：enable_status，0=禁用，1=启用）';


-- ==========================================
-- 第二部分：租户项目表（租户 schema）
-- 说明：学校实际使用的项目，通过平台分配功能从 public.seer_project_info 复制而来
-- 执行方式：
--   1. 在租户初始化时，由 Java 代码读取 tenant_schema_template.sql 执行
--   2. 或者手动切换到租户 schema 后执行：SET search_path TO school_001; 然后执行下面的SQL
-- 注意：
--   - 下面的 DROP 和 CREATE 语句会在当前 search_path 指向的 schema 中执行
--   - 不要添加 schema 前缀（如 school_001.）
-- ==========================================

DROP TABLE IF EXISTS seer_project_info;
CREATE TABLE seer_project_info (
    -- 主键
    id BIGSERIAL PRIMARY KEY,

    -- 核心字段
    project_code VARCHAR(50) NOT NULL,             -- 项目编号
    project_name VARCHAR(100) NOT NULL,            -- 项目名称（可自定义修改）
    unit INT NOT NULL,                             -- 单位（字典：unit_type）
    training_duration INT,                         -- 训练时长（秒）
    is_higher_better INT DEFAULT 1,                -- 成绩越大越好（字典：yes_no）
    sort_order INT DEFAULT 0,                      -- 排序

    -- 状态管理
    status INT DEFAULT 1,                          -- 状态（字典：enable_status，0=禁用，1=启用）

    -- 审计字段
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 备注
    remark VARCHAR(500),

    -- 唯一约束
    UNIQUE(project_code)
);

-- 添加注释
COMMENT ON TABLE seer_project_info IS '项目信息表（学校实际使用的项目，由平台分配）';
COMMENT ON COLUMN seer_project_info.id IS '项目ID';
COMMENT ON COLUMN seer_project_info.project_code IS '项目编号（在本学校唯一）';
COMMENT ON COLUMN seer_project_info.project_name IS '项目名称';
COMMENT ON COLUMN seer_project_info.unit IS '单位（字典：unit_type）';
COMMENT ON COLUMN seer_project_info.training_duration IS '训练时长（秒）';
COMMENT ON COLUMN seer_project_info.is_higher_better IS '成绩越大越好（字典：yes_no）';
COMMENT ON COLUMN seer_project_info.sort_order IS '排序';
COMMENT ON COLUMN seer_project_info.status IS '状态（字典：enable_status）';


-- ==========================================
-- 数据字典说明
-- ==========================================

-- 需要的字典数据（应在 public.sys_dict_data 中预置）：

-- 1. 单位字典（unit_type）
-- INSERT INTO public.sys_dict_data (dict_type, dict_label, dict_value, sort_order) VALUES
-- ('unit_type', '次', '1', 1),
-- ('unit_type', '厘米', '2', 2),
-- ('unit_type', '秒', '3', 3),
-- ('unit_type', '米', '4', 4),
-- ('unit_type', '分钟', '5', 5),
-- ('unit_type', '千克', '6', 6);

-- 2. 是否字典（yes_no）- 已存在，复用即可
-- 1=是，0=否

-- 3. 启用状态字典（enable_status）- 已存在，复用即可
-- 0=禁用，1=启用


-- ==========================================
-- 使用示例
-- ==========================================

-- 示例1：平台管理员添加项目到平台库
-- INSERT INTO public.seer_project_info (project_code, project_name, unit, training_duration, is_higher_better, sort_order, status)
-- VALUES ('pull_up', '引体向上', 1, 900, 1, 1, 1);

-- 示例2：学校从平台分配项目（批量复制）
-- -- 前提：已设置 search_path 到学校 schema：SET search_path TO school_001;
-- INSERT INTO seer_project_info (project_code, project_name, unit, training_duration, is_higher_better, sort_order, status, created_by)
-- SELECT project_code, project_name, unit, training_duration, is_higher_better, sort_order, status, 1
-- FROM public.seer_project_info
-- WHERE id IN (1, 3, 5);

-- 示例3：学校修改项目名称
-- UPDATE seer_project_info SET project_name = '单杠悬垂' WHERE id = 1;

-- 示例4：学校删除项目
-- DELETE FROM seer_project_info WHERE id = 1;
