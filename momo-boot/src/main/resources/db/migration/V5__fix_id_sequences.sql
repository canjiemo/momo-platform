-- ====================================================================================================
-- V5 - 修复 BIGSERIAL 序列推进
-- ----------------------------------------------------------------------------------------------------
-- 背景：V2/V4 中部分表使用了显式 id INSERT（sys_menu / sys_dict_type / sys_dict_data
--      / sys_role / sys_user 等），但 PostgreSQL 的 BIGSERIAL 序列不会因为带 id 的 INSERT
--      自动推进，导致序列下次取值仍为 1 与已有数据冲突，业务侧任何新增操作触发
--      duplicate key 错误。
-- 修复：对所有用显式 id 插入过种子数据的表，把 id 序列推进到 max(id)+1。
-- ====================================================================================================

SELECT setval(pg_get_serial_sequence('public.sys_menu',      'id'), COALESCE((SELECT MAX(id) FROM public.sys_menu),      1));
SELECT setval(pg_get_serial_sequence('public.sys_dict_type', 'id'), COALESCE((SELECT MAX(id) FROM public.sys_dict_type), 1));
SELECT setval(pg_get_serial_sequence('public.sys_dict_data', 'id'), COALESCE((SELECT MAX(id) FROM public.sys_dict_data), 1));
SELECT setval(pg_get_serial_sequence('public.sys_role',      'id'), COALESCE((SELECT MAX(id) FROM public.sys_role),      1));
SELECT setval(pg_get_serial_sequence('public.sys_user',      'id'), COALESCE((SELECT MAX(id) FROM public.sys_user),      1));
