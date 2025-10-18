-- =====================================================================================
-- Flyway版本管理表
-- 用于多租户架构下的数据库版本控制和升级管理
--
-- 说明：
-- 1. 这些表创建在public schema中，用于管理所有租户schema的版本
-- 2. 每个表都包含delete_flag字段，支持软删除
-- 3. 使用BIGSERIAL作为主键类型，支持大规模数据
--
-- 创建时间: 2025-10-18
-- 版本: 1.0.0
-- =====================================================================================

-- =====================================================================================
-- 表1: sys_schema_version
-- 说明: Schema版本追踪表，记录每个租户Schema的当前版本信息
-- =====================================================================================
CREATE TABLE IF NOT EXISTS public.sys_schema_version (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,                        -- 租户ID (NULL表示public schema)
    schema_name VARCHAR(100) NOT NULL,       -- Schema名称
    current_version VARCHAR(20) NOT NULL,    -- 当前版本号 (如 1.0.0)
    flyway_version VARCHAR(20),              -- Flyway记录的版本号
    last_upgraded_at TIMESTAMP,              -- 最后升级时间
    last_upgraded_by VARCHAR(50),            -- 最后升级操作人
    is_baseline BOOLEAN DEFAULT FALSE,       -- 是否为基线版本
    baseline_version VARCHAR(20),            -- 基线版本号
    baseline_description TEXT,               -- 基线描述
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delete_flag SMALLINT DEFAULT 0,          -- 删除标志: 0=正常, 1=已删除
    UNIQUE(schema_name, delete_flag)
);

COMMENT ON TABLE public.sys_schema_version IS 'Schema版本追踪表';
COMMENT ON COLUMN public.sys_schema_version.tenant_id IS '租户ID，NULL表示public schema';
COMMENT ON COLUMN public.sys_schema_version.schema_name IS 'Schema名称';
COMMENT ON COLUMN public.sys_schema_version.current_version IS '当前版本号';
COMMENT ON COLUMN public.sys_schema_version.flyway_version IS 'Flyway记录的版本号';
COMMENT ON COLUMN public.sys_schema_version.last_upgraded_at IS '最后升级时间';
COMMENT ON COLUMN public.sys_schema_version.last_upgraded_by IS '最后升级操作人';
COMMENT ON COLUMN public.sys_schema_version.is_baseline IS '是否为基线版本';
COMMENT ON COLUMN public.sys_schema_version.baseline_version IS '基线版本号';

CREATE INDEX idx_schema_version_tenant ON public.sys_schema_version(tenant_id) WHERE delete_flag = 0;
CREATE INDEX idx_schema_version_name ON public.sys_schema_version(schema_name) WHERE delete_flag = 0;

-- =====================================================================================
-- 表2: sys_schema_upgrade_task
-- 说明: 批量升级任务表，记录每次批量升级的任务信息
-- =====================================================================================
CREATE TABLE IF NOT EXISTS public.sys_schema_upgrade_task (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(200) NOT NULL,         -- 任务名称
    target_version VARCHAR(20) NOT NULL,     -- 目标版本号
    total_schemas INTEGER DEFAULT 0,         -- 总Schema数量
    success_count INTEGER DEFAULT 0,         -- 成功数量
    failed_count INTEGER DEFAULT 0,          -- 失败数量
    skipped_count INTEGER DEFAULT 0,         -- 跳过数量
    status VARCHAR(20) NOT NULL,             -- 任务状态: PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    start_time TIMESTAMP,                    -- 开始时间
    end_time TIMESTAMP,                      -- 结束时间
    duration_seconds INTEGER,                -- 执行时长(秒)
    created_by VARCHAR(50),                  -- 创建人
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delete_flag SMALLINT DEFAULT 0,
    CONSTRAINT chk_upgrade_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

COMMENT ON TABLE public.sys_schema_upgrade_task IS '批量升级任务表';
COMMENT ON COLUMN public.sys_schema_upgrade_task.task_name IS '任务名称';
COMMENT ON COLUMN public.sys_schema_upgrade_task.target_version IS '目标版本号';
COMMENT ON COLUMN public.sys_schema_upgrade_task.total_schemas IS '总Schema数量';
COMMENT ON COLUMN public.sys_schema_upgrade_task.success_count IS '成功数量';
COMMENT ON COLUMN public.sys_schema_upgrade_task.failed_count IS '失败数量';
COMMENT ON COLUMN public.sys_schema_upgrade_task.status IS '任务状态';

CREATE INDEX idx_upgrade_task_status ON public.sys_schema_upgrade_task(status) WHERE delete_flag = 0;
CREATE INDEX idx_upgrade_task_created ON public.sys_schema_upgrade_task(created_at DESC) WHERE delete_flag = 0;

-- =====================================================================================
-- 表3: sys_schema_upgrade_detail
-- 说明: 升级详情表，记录每个Schema的升级详细信息
-- =====================================================================================
CREATE TABLE IF NOT EXISTS public.sys_schema_upgrade_detail (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,                 -- 关联的任务ID
    tenant_id BIGINT,                        -- 租户ID
    schema_name VARCHAR(100) NOT NULL,       -- Schema名称
    before_version VARCHAR(20),              -- 升级前版本
    after_version VARCHAR(20),               -- 升级后版本
    status VARCHAR(20) NOT NULL,             -- 升级状态: SUCCESS, FAILED, SKIPPED
    error_message TEXT,                      -- 错误信息
    sql_executed TEXT,                       -- 执行的SQL语句
    execution_time_ms INTEGER,               -- 执行时间(毫秒)
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delete_flag SMALLINT DEFAULT 0,
    CONSTRAINT chk_detail_status CHECK (status IN ('SUCCESS', 'FAILED', 'SKIPPED')),
    CONSTRAINT fk_detail_task FOREIGN KEY (task_id) REFERENCES public.sys_schema_upgrade_task(id) ON DELETE CASCADE
);

COMMENT ON TABLE public.sys_schema_upgrade_detail IS '升级详情表';
COMMENT ON COLUMN public.sys_schema_upgrade_detail.task_id IS '关联的任务ID';
COMMENT ON COLUMN public.sys_schema_upgrade_detail.schema_name IS 'Schema名称';
COMMENT ON COLUMN public.sys_schema_upgrade_detail.before_version IS '升级前版本';
COMMENT ON COLUMN public.sys_schema_upgrade_detail.after_version IS '升级后版本';
COMMENT ON COLUMN public.sys_schema_upgrade_detail.status IS '升级状态';
COMMENT ON COLUMN public.sys_schema_upgrade_detail.error_message IS '错误信息';

CREATE INDEX idx_upgrade_detail_task ON public.sys_schema_upgrade_detail(task_id) WHERE delete_flag = 0;
CREATE INDEX idx_upgrade_detail_schema ON public.sys_schema_upgrade_detail(schema_name) WHERE delete_flag = 0;
CREATE INDEX idx_upgrade_detail_status ON public.sys_schema_upgrade_detail(status) WHERE delete_flag = 0;

-- =====================================================================================
-- 表4: sys_schema_rollback_log
-- 说明: 回滚日志表，记录所有回滚操作的历史
-- =====================================================================================
CREATE TABLE IF NOT EXISTS public.sys_schema_rollback_log (
    id BIGSERIAL PRIMARY KEY,
    schema_name VARCHAR(100) NOT NULL,       -- Schema名称
    from_version VARCHAR(20) NOT NULL,       -- 回滚前版本
    to_version VARCHAR(20) NOT NULL,         -- 回滚到的版本
    rollback_reason TEXT,                    -- 回滚原因
    rollback_sql TEXT,                       -- 回滚SQL
    status VARCHAR(20) NOT NULL,             -- 回滚状态: SUCCESS, FAILED
    error_message TEXT,                      -- 错误信息
    executed_by VARCHAR(50),                 -- 执行人
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    execution_time_ms INTEGER,               -- 执行时间(毫秒)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delete_flag SMALLINT DEFAULT 0,
    CONSTRAINT chk_rollback_status CHECK (status IN ('SUCCESS', 'FAILED'))
);

COMMENT ON TABLE public.sys_schema_rollback_log IS '回滚日志表';
COMMENT ON COLUMN public.sys_schema_rollback_log.schema_name IS 'Schema名称';
COMMENT ON COLUMN public.sys_schema_rollback_log.from_version IS '回滚前版本';
COMMENT ON COLUMN public.sys_schema_rollback_log.to_version IS '回滚到的版本';
COMMENT ON COLUMN public.sys_schema_rollback_log.rollback_reason IS '回滚原因';
COMMENT ON COLUMN public.sys_schema_rollback_log.status IS '回滚状态';

CREATE INDEX idx_rollback_schema ON public.sys_schema_rollback_log(schema_name) WHERE delete_flag = 0;
CREATE INDEX idx_rollback_executed ON public.sys_schema_rollback_log(executed_at DESC) WHERE delete_flag = 0;

-- =====================================================================================
-- 初始化数据
-- =====================================================================================

-- 为public schema创建基线版本记录
INSERT INTO public.sys_schema_version (tenant_id, schema_name, current_version, is_baseline, baseline_version, baseline_description)
VALUES (NULL, 'public', '1.0.0', TRUE, '1.0.0', '平台初始版本')
ON CONFLICT (schema_name, delete_flag) DO NOTHING;
