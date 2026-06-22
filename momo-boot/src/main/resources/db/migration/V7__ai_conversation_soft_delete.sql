-- ====================================================================================================
-- V7 - 为 ai_conversation 增加逻辑删除列
-- 此前会话删除时物理删除对话历史，改为逻辑删除以保留审计痕迹。
-- myjdbc 会在实体声明 delete_flag 后自动注入 delete_flag = 0 过滤，历史查询无需改动。
-- ====================================================================================================

ALTER TABLE public.ai_conversation
    ADD COLUMN IF NOT EXISTS delete_flag SMALLINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN public.ai_conversation.delete_flag IS '逻辑删除标记：0=未删除 1=已删除';
