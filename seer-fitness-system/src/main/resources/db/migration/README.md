# Flyway数据库迁移目录

此目录包含所有Flyway数据库迁移脚本，用于管理多租户架构下的数据库版本控制。

## 目录结构

```
db/migration/
├── common/          # 通用迁移脚本（同时应用于public和tenant schema）
├── public/          # 仅应用于public schema的迁移脚本
├── tenant/          # 仅应用于tenant schema的迁移脚本
└── rollback/        # 回滚脚本（用于撤销迁移）
```

## 目录说明

### 1. common/ - 通用迁移
- **用途**: 存放需要同时应用于public schema和所有tenant schema的迁移脚本
- **示例**:
  - 修复所有schema中的数据类型问题
  - 添加所有schema都需要的新索引
- **执行**: 在public和每个tenant schema中都会执行

### 2. public/ - 平台迁移
- **用途**: 仅应用于public schema的迁移脚本
- **示例**:
  - 添加新的平台管理表（如sys_platform_xxx）
  - 修改租户管理表结构（sys_tenant）
  - 平台级配置表变更
- **执行**: 仅在public schema中执行

### 3. tenant/ - 租户迁移
- **用途**: 仅应用于tenant schema的迁移脚本
- **示例**:
  - 添加新的业务表（如sys_student, sys_course）
  - 修改租户业务表结构（sys_user, sys_role）
  - 租户级数据迁移
- **执行**: 在每个tenant schema中执行（不包括public）

### 4. rollback/ - 回滚脚本
- **用途**: 存放迁移的回滚脚本
- **命名**: 与对应的迁移脚本相对应，例如：
  - 迁移: `V1.1.0__add_student_table.sql`
  - 回滚: `U1.1.0__add_student_table.sql`
- **执行**: 手动执行，用于撤销失败的迁移

## 命名规范

详见: [FLYWAY_MIGRATION_NAMING.md](../../../../../../../docs/FLYWAY_MIGRATION_NAMING.md)

### 版本化迁移 (Versioned Migration)
格式: `V{版本号}__{描述}.sql`

示例:
- `V1.0.0__baseline.sql` - 基线版本
- `V1.1.0__add_student_table.sql` - 添加学生表
- `V1.1.1__fix_user_email_length.sql` - 修复用户邮箱字段长度

### 可重复迁移 (Repeatable Migration)
格式: `R__{描述}.sql`

示例:
- `R__create_views.sql` - 创建视图
- `R__update_functions.sql` - 更新函数

## 使用流程

### 1. 新增迁移
1. 确定迁移类型（common/public/tenant）
2. 在对应目录创建SQL文件
3. 遵循命名规范
4. 编写SQL语句
5. 同时创建对应的回滚脚本（可选但推荐）

### 2. 执行迁移
- **新建租户**: 自动执行tenant目录下的迁移
- **批量升级**: 使用FlywayMultiTenantService手动触发
- **单个升级**: 调用相应的API接口

### 3. 回滚迁移
1. 从rollback目录找到对应的回滚脚本
2. 手动执行SQL
3. 更新sys_schema_version表
4. 记录到sys_schema_rollback_log表

## 最佳实践

1. **版本号递增**: 严格按照语义化版本号递增
2. **向后兼容**: 尽量保持向后兼容，避免破坏性变更
3. **原子操作**: 每个迁移脚本应该是原子性的
4. **可回滚**: 为每个迁移准备回滚脚本
5. **测试验证**: 在测试环境充分验证后再应用到生产环境
6. **文档记录**: 重要的迁移应该附带说明文档

## 注意事项

- ⚠️ 已执行的迁移脚本不应修改
- ⚠️ 文件名和内容的哈希值会被Flyway记录
- ⚠️ 修改已执行的脚本会导致验证失败
- ⚠️ 回滚操作需要谨慎，必须在测试环境验证

## 版本管理

所有迁移的执行记录存储在:
- **Flyway历史表**: `{schema}.flyway_schema_history`
- **自定义版本表**: `public.sys_schema_version`
- **升级详情表**: `public.sys_schema_upgrade_detail`
- **回滚日志表**: `public.sys_schema_rollback_log`
