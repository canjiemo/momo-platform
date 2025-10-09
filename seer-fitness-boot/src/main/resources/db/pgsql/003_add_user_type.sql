-- ========================================
-- 添加用户类型字段
-- 版本: v1.1.0
-- 日期: 2025-10-06
-- 说明: 为 sys_user 表添加 user_type 字段，支持区分运维人员、教师、学生
-- ========================================

-- 1. 添加 user_type 字段
ALTER TABLE sys_user
ADD COLUMN user_type SMALLINT DEFAULT 0;

COMMENT ON COLUMN sys_user.user_type IS '用户类型: 0-运维人员 1-教师 2-学生';

-- 2. 更新现有数据 (所有现有用户默认为运维人员)
UPDATE sys_user SET user_type = 0 WHERE user_type IS NULL;

-- 3. 添加索引 (提升查询性能)
CREATE INDEX idx_user_type ON sys_user(user_type);

-- 4. 验证
SELECT
    user_type,
    COUNT(*) as count,
    CASE
        WHEN user_type = 0 THEN '运维人员'
        WHEN user_type = 1 THEN '教师'
        WHEN user_type = 2 THEN '学生'
        ELSE '未知'
    END as type_name
FROM sys_user
WHERE delete_flag = 0
GROUP BY user_type
ORDER BY user_type;
