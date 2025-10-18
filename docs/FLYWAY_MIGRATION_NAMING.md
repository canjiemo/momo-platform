# Flyway迁移文件命名规范

## 📋 概述

本文档定义了Seer Fitness Edu项目中Flyway数据库迁移文件的命名规范和最佳实践。

**版本**: 1.0.0
**创建日期**: 2025-10-18
**适用范围**: 所有数据库迁移脚本

---

## 🎯 命名规范

### 1. 版本化迁移 (Versioned Migration)

**格式**: `V{版本号}__{描述}.sql`

**组成部分**:
- **前缀**: `V` (大写V，表示版本化迁移)
- **版本号**: 采用语义化版本号 `Major.Minor.Patch`
- **分隔符**: 双下划线 `__`
- **描述**: 简洁的英文描述（使用下划线连接单词）
- **扩展名**: `.sql`

**示例**:
```
V1.0.0__baseline.sql
V1.1.0__add_student_table.sql
V1.1.1__fix_user_email_length.sql
V1.2.0__add_course_management.sql
V2.0.0__refactor_user_system.sql
```

### 2. 可重复迁移 (Repeatable Migration)

**格式**: `R__{描述}.sql`

**组成部分**:
- **前缀**: `R` (大写R，表示可重复迁移)
- **分隔符**: 双下划线 `__`
- **描述**: 简洁的英文描述
- **扩展名**: `.sql`

**示例**:
```
R__create_user_views.sql
R__update_stored_procedures.sql
R__refresh_materialized_views.sql
```

### 3. 回滚脚本 (Undo Migration)

**格式**: `U{版本号}__{描述}.sql`

**组成部分**:
- **前缀**: `U` (大写U，表示回滚脚本)
- **版本号**: 与对应的V版本号相同
- **分隔符**: 双下划线 `__`
- **描述**: 与对应的V脚本相同
- **扩展名**: `.sql`

**示例**:
```
U1.1.0__add_student_table.sql      (回滚 V1.1.0__add_student_table.sql)
U1.2.0__add_course_management.sql  (回滚 V1.2.0__add_course_management.sql)
```

---

## 📁 目录结构与命名

### 目录分类

```
db/migration/
├── common/          # 通用迁移（public + tenant）
├── public/          # 仅public schema
├── tenant/          # 仅tenant schema
└── rollback/        # 回滚脚本
```

### 文件存放规则

| 迁移类型 | 存放目录 | 命名示例 |
|----------|---------|----------|
| 租户基线 | `tenant/` | `V1.0.0__baseline.sql` |
| 租户表新增 | `tenant/` | `V1.1.0__add_student_table.sql` |
| 平台表新增 | `public/` | `V1.1.0__add_platform_config_table.sql` |
| 通用修复 | `common/` | `V1.1.1__fix_timestamp_defaults.sql` |
| 回滚脚本 | `rollback/` | `U1.1.0__add_student_table.sql` |

---

## 🔢 版本号规则

### 语义化版本号 (Semantic Versioning)

格式: `Major.Minor.Patch`

#### Major (主版本号)
- **递增时机**: 架构重大变更、破坏性更新
- **示例场景**:
  - 数据库引擎升级 (PostgreSQL 14 → 15)
  - 表结构重构
  - 删除已废弃的表/字段
- **影响**: 可能导致应用不兼容

**示例**:
```
V1.0.0__baseline.sql                  (初始版本)
V2.0.0__refactor_user_authentication.sql  (重构认证系统)
V3.0.0__migrate_to_pg15.sql          (数据库升级)
```

#### Minor (次版本号)
- **递增时机**: 功能新增、向后兼容的变更
- **示例场景**:
  - 新增业务表
  - 新增字段（带默认值）
  - 新增索引
- **影响**: 完全向后兼容

**示例**:
```
V1.1.0__add_student_table.sql        (新增学生表)
V1.2.0__add_course_management.sql    (新增课程管理)
V1.3.0__add_exam_system.sql          (新增考试系统)
```

#### Patch (补丁版本号)
- **递增时机**: Bug修复、性能优化
- **示例场景**:
  - 修复字段长度不足
  - 修复索引缺失
  - 修正数据类型
- **影响**: 不改变功能

**示例**:
```
V1.1.1__fix_user_email_length.sql    (修复邮箱长度)
V1.1.2__add_missing_index.sql        (补充索引)
V1.1.3__fix_default_values.sql       (修正默认值)
```

### 版本号递增规则

1. **同一目录内严格递增**
   - tenant目录: V1.0.0 → V1.1.0 → V1.1.1 → V1.2.0
   - public目录: V1.0.0 → V1.1.0 → V1.2.0

2. **不同目录独立管理**
   - `tenant/V1.1.0` 和 `public/V1.1.0` 可以同时存在
   - 版本号相同，但作用于不同Schema

3. **禁止版本号重复**
   - ❌ 错误: `tenant/V1.1.0__add_table_a.sql` 和 `tenant/V1.1.0__add_table_b.sql`
   - ✅ 正确: `tenant/V1.1.0__add_table_a.sql` 和 `tenant/V1.2.0__add_table_b.sql`

4. **禁止修改已执行的脚本**
   - Flyway会记录文件哈希值
   - 修改已执行的脚本会导致验证失败

---

## 📝 描述部分规范

### 基本原则

1. **使用英文**: 所有描述必须使用英文
2. **使用下划线**: 单词之间使用下划线 `_` 连接
3. **动词开头**: 描述以动词开头，表明操作类型
4. **简洁明确**: 控制在50字符以内
5. **有意义**: 能清楚表达迁移目的

### 常用动词

| 动词 | 含义 | 示例 |
|------|------|------|
| `add` | 新增 | `add_student_table`, `add_email_column` |
| `create` | 创建 | `create_indexes`, `create_views` |
| `update` | 更新 | `update_user_schema`, `update_constraints` |
| `modify` | 修改 | `modify_user_type`, `modify_table_structure` |
| `fix` | 修复 | `fix_email_length`, `fix_null_values` |
| `remove` | 移除 | `remove_deprecated_columns`, `remove_old_indexes` |
| `rename` | 重命名 | `rename_old_table`, `rename_user_type_column` |
| `refactor` | 重构 | `refactor_auth_system`, `refactor_menu_structure` |
| `migrate` | 迁移 | `migrate_data_to_new_schema`, `migrate_legacy_users` |
| `optimize` | 优化 | `optimize_query_performance`, `optimize_indexes` |

### 描述示例

#### ✅ 良好的描述
```
V1.1.0__add_student_table.sql
V1.1.1__fix_user_email_length.sql
V1.2.0__create_course_indexes.sql
V1.3.0__migrate_legacy_data.sql
V2.0.0__refactor_authentication_system.sql
```

#### ❌ 不良的描述
```
V1.1.0__new_table.sql                (不明确)
V1.1.1__bug_fix.sql                  (太模糊)
V1.2.0__update.sql                   (没有说明更新什么)
V1.3.0__临时修改.sql                  (使用中文)
V2.0.0__add-new-feature.sql          (使用连字符而非下划线)
```

---

## 🏗️ 迁移脚本最佳实践

### 1. 文件头注释

每个迁移文件应包含完整的注释说明：

```sql
-- ====================================================================================================
-- Flyway Migration: V1.1.0 - Add Student Table
-- 说明：添加学生信息表，支持学生基本信息管理
-- 版本：1.1.0
-- 创建时间：2025-10-18
-- 作者：张三
-- 适用对象：租户Schema
-- ====================================================================================================
-- 变更内容：
--   1. 创建 sys_student 表
--   2. 添加学生基本信息字段（姓名、学号、班级等）
--   3. 创建唯一索引 idx_student_no
--   4. 添加外键关联到 sys_user 表
-- ====================================================================================================
```

### 2. 幂等性 (Idempotent)

使用 `IF NOT EXISTS` 或 `DROP IF EXISTS` 确保脚本可重复执行：

```sql
-- ✅ 推荐
DROP TABLE IF EXISTS sys_student;
CREATE TABLE sys_student (...);

-- ✅ 推荐
CREATE INDEX IF NOT EXISTS idx_student_no ON sys_student(student_no);

-- ❌ 不推荐
CREATE TABLE sys_student (...);  -- 重复执行会报错
```

### 3. 原子性

每个迁移脚本应该是一个完整的、不可分割的单元：

```sql
-- ✅ 推荐: 一个迁移完成一个完整功能
-- V1.1.0__add_student_management.sql
CREATE TABLE sys_student (...);
CREATE TABLE sys_student_course (...);
CREATE INDEX idx_student_no ON sys_student(student_no);

-- ❌ 不推荐: 拆分成多个细碎的迁移
-- V1.1.0__create_student_table.sql
-- V1.1.1__create_student_course_table.sql
-- V1.1.2__add_student_index.sql
```

### 4. 向后兼容

新增字段时提供默认值，避免破坏现有数据：

```sql
-- ✅ 推荐
ALTER TABLE sys_user ADD COLUMN email VARCHAR(100) DEFAULT '';

-- ❌ 不推荐 (对现有数据不友好)
ALTER TABLE sys_user ADD COLUMN email VARCHAR(100) NOT NULL;
```

### 5. 数据迁移分离

大规模数据迁移应单独成为一个迁移版本：

```sql
-- V1.1.0__add_new_field.sql (结构变更)
ALTER TABLE sys_user ADD COLUMN user_level SMALLINT DEFAULT 1;

-- V1.1.1__migrate_user_level_data.sql (数据迁移)
UPDATE sys_user SET user_level = 2 WHERE user_type = 1;
UPDATE sys_user SET user_level = 3 WHERE user_type = 2;
```

---

## 🔄 回滚脚本规范

### 1. 回滚脚本必要性

**强烈推荐**为每个版本化迁移编写对应的回滚脚本。

**原因**:
- 快速回滚失败的迁移
- 降低生产环境风险
- 便于版本降级

### 2. 回滚脚本示例

```sql
-- V1.1.0__add_student_table.sql (迁移)
CREATE TABLE sys_student (
    id BIGINT PRIMARY KEY,
    student_no VARCHAR(20) NOT NULL,
    student_name VARCHAR(50) NOT NULL
);

-- U1.1.0__add_student_table.sql (回滚)
DROP TABLE IF EXISTS sys_student;
```

### 3. 复杂回滚

对于复杂的迁移，回滚脚本需要完整还原：

```sql
-- V1.2.0__refactor_user_type.sql (迁移)
ALTER TABLE sys_user RENAME COLUMN user_type TO old_user_type;
ALTER TABLE sys_user ADD COLUMN user_category VARCHAR(20);
UPDATE sys_user SET user_category = CASE
    WHEN old_user_type = 0 THEN 'admin'
    WHEN old_user_type = 1 THEN 'teacher'
    WHEN old_user_type = 2 THEN 'student'
END;
ALTER TABLE sys_user DROP COLUMN old_user_type;

-- U1.2.0__refactor_user_type.sql (回滚)
ALTER TABLE sys_user ADD COLUMN user_type SMALLINT;
UPDATE sys_user SET user_type = CASE
    WHEN user_category = 'admin' THEN 0
    WHEN user_category = 'teacher' THEN 1
    WHEN user_category = 'student' THEN 2
END;
ALTER TABLE sys_user DROP COLUMN user_category;
```

---

## 📊 完整示例

### 场景：添加考试管理模块

#### 1. 新增表结构 (V1.3.0)

```
文件名: tenant/V1.3.0__add_exam_management.sql
```

```sql
-- ====================================================================================================
-- Flyway Migration: V1.3.0 - Add Exam Management
-- 说明：添加考试管理模块，包含考试、试题、成绩表
-- 版本：1.3.0
-- 创建时间：2025-10-20
-- ====================================================================================================

-- 考试表
CREATE TABLE sys_exam (
    id BIGINT PRIMARY KEY,
    exam_name VARCHAR(100) NOT NULL,
    exam_type SMALLINT DEFAULT 1,
    total_score INT DEFAULT 100,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 考试成绩表
CREATE TABLE sys_exam_score (
    id BIGINT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    score DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(exam_id, student_id)
);

COMMENT ON TABLE sys_exam IS '考试表';
COMMENT ON TABLE sys_exam_score IS '考试成绩表';
```

#### 2. 对应回滚脚本 (U1.3.0)

```
文件名: rollback/U1.3.0__add_exam_management.sql
```

```sql
-- ====================================================================================================
-- Flyway Undo Migration: U1.3.0 - Rollback Exam Management
-- 说明：回滚考试管理模块的所有变更
-- ====================================================================================================

DROP TABLE IF EXISTS sys_exam_score;
DROP TABLE IF EXISTS sys_exam;
```

#### 3. Bug修复 (V1.3.1)

```
文件名: tenant/V1.3.1__fix_exam_score_precision.sql
```

```sql
-- ====================================================================================================
-- Flyway Migration: V1.3.1 - Fix Exam Score Precision
-- 说明：修正成绩字段精度，从DECIMAL(5,2)改为DECIMAL(6,2)以支持三位整数
-- ====================================================================================================

ALTER TABLE sys_exam_score ALTER COLUMN score TYPE DECIMAL(6,2);
```

---

## ⚠️ 注意事项

### 禁止事项

1. ❌ **禁止修改已执行的迁移文件**
   - Flyway会检查文件哈希值
   - 修改会导致验证失败

2. ❌ **禁止删除已执行的迁移文件**
   - 会导致Flyway历史记录不一致
   - 新环境初始化会失败

3. ❌ **禁止重复使用版本号**
   - 同一目录下版本号必须唯一
   - 严格按照递增顺序

4. ❌ **禁止乱序执行**
   - 必须按照版本号顺序执行
   - 不要使用 `out-of-order` 模式

### 推荐做法

1. ✅ **版本控制**
   - 所有迁移文件纳入Git管理
   - 通过Pull Request审查迁移脚本

2. ✅ **测试验证**
   - 在测试环境充分验证
   - 执行回滚脚本测试

3. ✅ **文档记录**
   - 重大变更编写详细文档
   - 记录数据迁移策略

4. ✅ **分阶段发布**
   - 大规模变更分多个小版本
   - 降低单次变更风险

---

## 📚 参考资料

- [Flyway官方文档](https://flywaydb.org/documentation/)
- [Semantic Versioning](https://semver.org/)
- [SQL命名规范](https://www.sqlstyle.guide/)
- [PostgreSQL文档](https://www.postgresql.org/docs/)

---

**文档维护者**: Seer Fitness Edu Team
**最后更新**: 2025-10-18
**版本**: 1.0.0
