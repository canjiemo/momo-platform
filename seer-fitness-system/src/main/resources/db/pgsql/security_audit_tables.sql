-- 租户安全审计表
-- 创建日期: 2024-10-18
-- 用途: 记录租户访问日志和安全事件

-- 1. 租户访问日志表
-- 记录所有租户相关的API访问
CREATE TABLE IF NOT EXISTS public.sys_tenant_access_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,                    -- 用户ID
    tenant_id BIGINT,                  -- 租户ID
    schema_name VARCHAR(100),          -- Schema名称
    method_name VARCHAR(255),          -- 方法名
    success BOOLEAN DEFAULT true,      -- 是否成功
    duration_ms BIGINT,                -- 执行时长（毫秒）
    error_message TEXT,                -- 错误信息
    created_at TIMESTAMP DEFAULT NOW(),-- 创建时间
    CONSTRAINT fk_tenant_access_user FOREIGN KEY (user_id) REFERENCES public.sys_user(id),
    CONSTRAINT fk_tenant_access_tenant FOREIGN KEY (tenant_id) REFERENCES public.sys_tenant(id)
);

-- 创建索引以提升查询性能
CREATE INDEX IF NOT EXISTS idx_tenant_access_user_id ON public.sys_tenant_access_log(user_id);
CREATE INDEX IF NOT EXISTS idx_tenant_access_tenant_id ON public.sys_tenant_access_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_access_created_at ON public.sys_tenant_access_log(created_at);
CREATE INDEX IF NOT EXISTS idx_tenant_access_success ON public.sys_tenant_access_log(success);

-- 添加表注释
COMMENT ON TABLE public.sys_tenant_access_log IS '租户访问日志表 - 记录所有租户相关的API访问';
COMMENT ON COLUMN public.sys_tenant_access_log.user_id IS '用户ID';
COMMENT ON COLUMN public.sys_tenant_access_log.tenant_id IS '租户ID';
COMMENT ON COLUMN public.sys_tenant_access_log.schema_name IS 'Schema名称';
COMMENT ON COLUMN public.sys_tenant_access_log.method_name IS '访问的方法名';
COMMENT ON COLUMN public.sys_tenant_access_log.success IS '是否成功执行';
COMMENT ON COLUMN public.sys_tenant_access_log.duration_ms IS '执行时长（毫秒）';
COMMENT ON COLUMN public.sys_tenant_access_log.error_message IS '错误信息（如果失败）';
COMMENT ON COLUMN public.sys_tenant_access_log.created_at IS '创建时间';

-- 2. 安全事件表（如果不存在）
-- 记录安全相关的重要事件
CREATE TABLE IF NOT EXISTS public.sys_security_event (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,                    -- 用户ID
    tenant_id BIGINT,                  -- 租户ID（可选）
    event_type VARCHAR(50) NOT NULL,   -- 事件类型
    event_desc TEXT,                   -- 事件描述
    method_name VARCHAR(255),          -- 触发的方法名
    ip_address VARCHAR(50),            -- IP地址（可选）
    user_agent TEXT,                   -- 用户代理（可选）
    created_at TIMESTAMP DEFAULT NOW(),-- 创建时间
    CONSTRAINT fk_security_event_user FOREIGN KEY (user_id) REFERENCES public.sys_user(id) ON DELETE SET NULL
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_security_event_user_id ON public.sys_security_event(user_id);
CREATE INDEX IF NOT EXISTS idx_security_event_tenant_id ON public.sys_security_event(tenant_id);
CREATE INDEX IF NOT EXISTS idx_security_event_type ON public.sys_security_event(event_type);
CREATE INDEX IF NOT EXISTS idx_security_event_created_at ON public.sys_security_event(created_at);

-- 添加表注释
COMMENT ON TABLE public.sys_security_event IS '安全事件表 - 记录系统安全相关的重要事件';
COMMENT ON COLUMN public.sys_security_event.user_id IS '触发事件的用户ID';
COMMENT ON COLUMN public.sys_security_event.tenant_id IS '关联的租户ID（如果有）';
COMMENT ON COLUMN public.sys_security_event.event_type IS '事件类型: TENANT_ACCESS_VIOLATION, ACCESS_DENIED, TENANT_MISMATCH等';
COMMENT ON COLUMN public.sys_security_event.event_desc IS '事件详细描述';
COMMENT ON COLUMN public.sys_security_event.method_name IS '触发事件的方法名';
COMMENT ON COLUMN public.sys_security_event.ip_address IS '客户端IP地址';
COMMENT ON COLUMN public.sys_security_event.user_agent IS '客户端User-Agent';
COMMENT ON COLUMN public.sys_security_event.created_at IS '事件发生时间';

-- 3. 审计日志清理策略
-- 创建定期清理旧日志的函数（保留90天）

-- 清理租户访问日志（保留90天）
CREATE OR REPLACE FUNCTION clean_tenant_access_logs()
RETURNS void AS $$
BEGIN
    DELETE FROM public.sys_tenant_access_log
    WHERE created_at < NOW() - INTERVAL '90 days';

    RAISE NOTICE '已清理超过90天的租户访问日志';
END;
$$ LANGUAGE plpgsql;

-- 清理安全事件日志（保留180天，安全事件保留更长）
CREATE OR REPLACE FUNCTION clean_security_events()
RETURNS void AS $$
BEGIN
    DELETE FROM public.sys_security_event
    WHERE created_at < NOW() - INTERVAL '180 days'
    AND event_type NOT IN ('TENANT_ACCESS_VIOLATION', 'ACCESS_DENIED'); -- 保留重要安全事件

    RAISE NOTICE '已清理超过180天的安全事件日志（保留重要事件）';
END;
$$ LANGUAGE plpgsql;

-- 4. 创建视图：租户访问统计
CREATE OR REPLACE VIEW public.v_tenant_access_stats AS
SELECT
    tenant_id,
    schema_name,
    COUNT(*) AS total_access,
    COUNT(*) FILTER (WHERE success = true) AS success_count,
    COUNT(*) FILTER (WHERE success = false) AS failure_count,
    ROUND(AVG(duration_ms), 2) AS avg_duration_ms,
    MAX(duration_ms) AS max_duration_ms,
    MIN(created_at) AS first_access,
    MAX(created_at) AS last_access
FROM public.sys_tenant_access_log
GROUP BY tenant_id, schema_name;

COMMENT ON VIEW public.v_tenant_access_stats IS '租户访问统计视图 - 汇总每个租户的访问情况';

-- 5. 创建视图：用户安全事件统计
CREATE OR REPLACE VIEW public.v_user_security_stats AS
SELECT
    user_id,
    event_type,
    COUNT(*) AS event_count,
    MIN(created_at) AS first_event,
    MAX(created_at) AS last_event
FROM public.sys_security_event
GROUP BY user_id, event_type;

COMMENT ON VIEW public.v_user_security_stats IS '用户安全事件统计视图 - 汇总每个用户的安全事件';

-- 6. 创建视图：最近24小时的安全事件
CREATE OR REPLACE VIEW public.v_recent_security_events AS
SELECT
    e.id,
    e.user_id,
    u.username,
    e.tenant_id,
    e.event_type,
    e.event_desc,
    e.method_name,
    e.created_at
FROM public.sys_security_event e
LEFT JOIN public.sys_user u ON e.user_id = u.id
WHERE e.created_at > NOW() - INTERVAL '24 hours'
ORDER BY e.created_at DESC;

COMMENT ON VIEW public.v_recent_security_events IS '最近24小时安全事件 - 用于实时安全监控';

-- 7. 示例查询

-- 查询某个租户的访问统计
-- SELECT * FROM public.v_tenant_access_stats WHERE tenant_id = 1;

-- 查询某个用户的安全事件
-- SELECT * FROM public.v_user_security_stats WHERE user_id = 1;

-- 查询最近的安全告警
-- SELECT * FROM public.v_recent_security_events WHERE event_type IN ('TENANT_ACCESS_VIOLATION', 'ACCESS_DENIED');

-- 查询慢查询（超过3秒）
-- SELECT * FROM public.sys_tenant_access_log WHERE duration_ms > 3000 ORDER BY duration_ms DESC LIMIT 100;

-- 查询失败的访问记录
-- SELECT * FROM public.sys_tenant_access_log WHERE success = false ORDER BY created_at DESC LIMIT 100;
