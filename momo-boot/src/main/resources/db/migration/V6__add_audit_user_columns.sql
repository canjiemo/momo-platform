-- ====================================================================================================
-- V6 - 补齐 audit 用户列 create_by / update_by
-- ----------------------------------------------------------------------------------------------------
-- 背景：sys_role / sys_user / sys_tenant / sys_organization / sys_menu / sys_job /
--      sys_role_menu / sys_user_role 这 8 张表的 entity 已声明 createBy/updateBy（Long 类型，
--      由 AuditFieldProvider 自动填充），但 V1 schema 中遗漏了对应的列。
-- 影响：updatePO(po, false) 全量更新时，myjdbc 会把 createBy/updateBy 一并写入 SQL ，
--      报 column does not exist。
-- 修复：本迁移给上述表补 create_by / update_by BIGINT 列，与 entity 字段类型一致。
-- 注：sys_dict_* / sys_config / sys_file_config 等表已有相同语义的 create_by VARCHAR(64) 列，
--    这是另一套历史遗留命名（对应 entity 字段 createBy 类型 String），本迁移不动它们。
-- ====================================================================================================

ALTER TABLE public.sys_role         ADD COLUMN IF NOT EXISTS create_by BIGINT;
ALTER TABLE public.sys_role         ADD COLUMN IF NOT EXISTS update_by BIGINT;

ALTER TABLE public.sys_user         ADD COLUMN IF NOT EXISTS create_by BIGINT;
ALTER TABLE public.sys_user         ADD COLUMN IF NOT EXISTS update_by BIGINT;

ALTER TABLE public.sys_tenant       ADD COLUMN IF NOT EXISTS create_by BIGINT;
ALTER TABLE public.sys_tenant       ADD COLUMN IF NOT EXISTS update_by BIGINT;

ALTER TABLE public.sys_organization ADD COLUMN IF NOT EXISTS create_by BIGINT;
ALTER TABLE public.sys_organization ADD COLUMN IF NOT EXISTS update_by BIGINT;

ALTER TABLE public.sys_menu         ADD COLUMN IF NOT EXISTS create_by BIGINT;
ALTER TABLE public.sys_menu         ADD COLUMN IF NOT EXISTS update_by BIGINT;

ALTER TABLE public.sys_job          ADD COLUMN IF NOT EXISTS create_by BIGINT;
ALTER TABLE public.sys_job          ADD COLUMN IF NOT EXISTS update_by BIGINT;

ALTER TABLE public.sys_role_menu    ADD COLUMN IF NOT EXISTS create_by BIGINT;

ALTER TABLE public.sys_user_role    ADD COLUMN IF NOT EXISTS create_by BIGINT;
