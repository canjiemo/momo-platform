-- ====================================================================================================
-- momo-platform - AI 智能查询模块初始数据
-- 执行前提：已执行 001_schema.sql、002_data.sql、003_ai_schema.sql
-- ====================================================================================================

-- ========================================
-- 默认 Ollama 配置（本地开发用）
-- ========================================

INSERT INTO public.ai_provider_config
    (config_name, provider, chat_model, embed_model, base_url, is_active, remark, create_time, update_time, delete_flag)
VALUES
    ('本地 Ollama', 'ollama', 'qwen3:4b', 'nomic-embed-text',
     'http://localhost:11434', 1,
     '本地 Ollama 服务，需先安装 Ollama 并拉取模型（ollama pull qwen2.5:7b && ollama pull nomic-embed-text）',
     NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

INSERT INTO public.ai_provider_config
    (config_name, provider, chat_model, embed_model, base_url, api_key, is_active, remark, create_time, update_time, delete_flag)
VALUES
    ('阿里百炼', 'aliyun', 'qwen-plus', 'text-embedding-v3',
     'https://dashscope.aliyuncs.com/compatible-mode',
     'your-dashscope-api-key',
     0,
     '阿里云百炼平台（DashScope），使用 OpenAI 兼容接口。需在阿里云控制台申请 API Key 并替换 api_key 字段。切换前请在管理页面执行「全量同步向量」重建索引。',
     NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

-- ========================================
-- 顶级目录：AI 管理（与数据字典平级）
-- ========================================

INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES (20000, NULL, 'AI 管理', 0, 0, '/ai-admin', NULL, 'RobotOutlined', 4, 1, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- AI 管理 -> AI 模型
-- ========================================

INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (10600, NULL, 'AI 模型',      20000, 1, '/platform/ai/provider', NULL,                'ApiOutlined',  1, 1, 0, NOW(), NOW()),
    (10601, NULL, '查看模型配置', 10600, 2, NULL,                    'ai:provider:view',   NULL, 1, 1, 0, NOW(), NOW()),
    (10602, NULL, '新增模型配置', 10600, 2, NULL,                    'ai:provider:create', NULL, 2, 1, 0, NOW(), NOW()),
    (10603, NULL, '修改模型配置', 10600, 2, NULL,                    'ai:provider:update', NULL, 3, 1, 0, NOW(), NOW()),
    (10604, NULL, '删除模型配置', 10600, 2, NULL,                    'ai:provider:delete', NULL, 4, 1, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- AI 管理 -> 数据目录
-- ========================================

INSERT INTO public.sys_menu (id, tenant_id, menu_name, parent_id, type, path, permission, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
    (10700, NULL, '数据目录',    20000, 1, '/platform/ai/catalog', NULL,              'TableOutlined', 2, 1, 0, NOW(), NOW()),
    (10701, NULL, '扫描表结构',  10700, 2, NULL,                   'ai:catalog:scan', NULL, 1, 1, 0, NOW(), NOW()),
    (10702, NULL, '配置表/字段', 10700, 2, NULL,                   'ai:catalog:edit', NULL, 2, 1, 0, NOW(), NOW()),
    (10703, NULL, '同步向量',    10700, 2, NULL,                   'ai:catalog:sync', NULL, 3, 1, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
