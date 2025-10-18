# Flyway 多租户升级系统实施计划

> **项目目标**: 将Seer Fitness平台改造为通用化的多租户基础框架，实现自动化的数据库版本管理和升级能力
>
> **当前状态**: 5个租户schema，手动SQL模板创建，无版本追踪
>
> **目标状态**: Flyway驱动的自动化升级系统，支持批量升级、回滚、进度监控

---

## 📋 项目概览

### 当前系统分析

**数据库架构**:
- **Public Schema**: 15张表（平台数据）
- **租户Schema**: 5个活跃租户 (tenant_s1_*, ts1f_*, ts2_*, etc.)
- **每个租户Schema**: 7张表（用户、角色、菜单、角色菜单、用户角色、组织、操作日志）

**现有机制**:
- ✅ 使用 `tenant_schema_template.sql` 作为租户模板
- ✅ 有 `sys_tenant_init_log` 记录初始化日志
- ✅ 有 `TenantSchemaService` 处理Schema创建
- ❌ 无版本号管理
- ❌ 无升级历史追踪
- ❌ 无批量升级能力
- ❌ 无回滚机制

### 技术选型

| 组件 | 选择 | 原因 |
|------|------|------|
| **版本管理工具** | Flyway Community | 开源免费、成熟稳定、Spring Boot官方支持 |
| **版本号规范** | Semantic Versioning | 业界标准(Major.Minor.Patch) |
| **迁移文件格式** | SQL | 简单直观、易于维护 |
| **回滚策略** | Undo Migrations | Flyway Pro特性，我们自己实现 |

---

## 🎯 实施阶段划分

### Phase 1: 基础设施搭建 (预计2周)
**目标**: 引入Flyway，建立版本管理基础

**交付物**:
- ✅ Flyway依赖配置
- ✅ 版本管理数据表
- ✅ 迁移文件目录结构
- ✅ 多Schema配置

### Phase 2: 升级管理服务开发 (预计3周)
**目标**: 开发自动化升级服务

**交付物**:
- ✅ SchemaVersionService (版本查询)
- ✅ SchemaUpgradeService (升级执行)
- ✅ 批量升级功能
- ✅ 回滚机制

### Phase 3: 管理界面开发 (预计2周)
**目标**: 提供可视化管理界面

**交付物**:
- ✅ Schema版本查看页面
- ✅ 升级任务管理页面
- ✅ 升级进度监控页面

### Phase 4: 测试和文档 (预计1周)
**目标**: 完善测试和文档

**交付物**:
- ✅ 单元测试和集成测试
- ✅ 升级SOP文档
- ✅ 开发者文档

---

## 📅 Phase 1: 基础设施搭建 (详细步骤)

### 任务 1.1: 添加Flyway依赖和配置 (2天)

#### 步骤 1.1.1: 添加Maven依赖

**文件**: `seer-fitness-framework/pom.xml`

```xml
<dependencies>
    <!-- Flyway Core -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
        <version>10.21.0</version>
    </dependency>

    <!-- Flyway PostgreSQL Driver -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
        <version>10.21.0</version>
    </dependency>
</dependencies>
```

#### 步骤 1.1.2: 配置Flyway基础设置

**文件**: `seer-fitness-boot/src/main/resources/application.yml`

```yaml
spring:
  flyway:
    enabled: true                          # 启用Flyway
    baseline-on-migrate: true              # 已有数据库时自动baseline
    baseline-version: 1.0.0                # 基线版本
    locations: classpath:db/migration      # 迁移文件位置
    encoding: UTF-8
    validate-on-migrate: true              # 迁移前验证
    out-of-order: false                    # 不允许乱序执行
    placeholder-replacement: true          # 启用占位符替换
    placeholders:
      tablePrefix: sys_                    # 表前缀占位符

# 自定义配置：多租户支持
seer:
  flyway:
    multi-tenant:
      enabled: true                        # 启用多租户升级
      auto-upgrade: false                  # 不自动升级（手动触发）
      batch-size: 5                        # 批量升级：每批5个租户
      delay-between-batches: 10            # 批次间延迟（秒）
```

#### 步骤 1.1.3: 禁用默认Flyway自动执行

**文件**: `seer-fitness-boot/src/main/java/com/seer/fitness/edu/config/FlywayConfig.java` (新建)

```java
@Configuration
@ConditionalOnProperty(name = "seer.flyway.multi-tenant.enabled", havingValue = "true")
public class FlywayConfig {

    /**
     * 禁用Spring Boot默认的Flyway自动迁移
     * 我们需要手动控制每个租户schema的迁移
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // 不执行任何操作，我们自己控制
            log.info("Flyway自动迁移已禁用，将由SchemaUpgradeService手动控制");
        };
    }
}
```

**验收标准**:
- ✅ Maven依赖添加成功，`mvn dependency:tree` 能看到Flyway
- ✅ 应用启动无报错
- ✅ 日志显示 "Flyway自动迁移已禁用"

---

### 任务 1.2: 创建Schema版本管理表 (1天)

#### 步骤 1.2.1: 设计版本管理表

**文件**: `seer-fitness-system/src/main/resources/db/pgsql/flyway_management_tables.sql` (新建)

```sql
-- ====================================================================================================
-- Flyway 多租户版本管理表
-- 说明：用于追踪每个schema的版本、升级历史、回滚记录
-- ====================================================================================================

-- Schema版本信息表
CREATE TABLE IF NOT EXISTS public.sys_schema_version (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,                          -- 租户ID，NULL表示public schema
    schema_name VARCHAR(100) NOT NULL,         -- schema名称
    current_version VARCHAR(20) NOT NULL,      -- 当前版本号 如 "1.2.3"
    flyway_version VARCHAR(20),                -- Flyway记录的版本
    last_upgraded_at TIMESTAMP,                -- 最后升级时间
    last_upgraded_by VARCHAR(50),              -- 最后升级人
    is_baseline BOOLEAN DEFAULT FALSE,         -- 是否为基线版本
    baseline_version VARCHAR(20),              -- 基线版本号
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(schema_name)
);

COMMENT ON TABLE public.sys_schema_version IS 'Schema版本管理表';
COMMENT ON COLUMN public.sys_schema_version.tenant_id IS '租户ID，NULL表示public schema';
COMMENT ON COLUMN public.sys_schema_version.current_version IS '当前版本号，如1.2.3';
COMMENT ON COLUMN public.sys_schema_version.is_baseline IS '是否为基线版本（已有数据的schema）';

-- 创建索引
CREATE INDEX idx_schema_version_tenant ON public.sys_schema_version(tenant_id);
CREATE INDEX idx_schema_version_schema ON public.sys_schema_version(schema_name);

-- 升级任务表
CREATE TABLE IF NOT EXISTS public.sys_schema_upgrade_task (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(200) NOT NULL,           -- 任务名称
    target_version VARCHAR(20) NOT NULL,       -- 目标版本
    description TEXT,                          -- 任务描述
    total_schemas INT DEFAULT 0,               -- 需要升级的schema总数
    success_count INT DEFAULT 0,               -- 成功数量
    failed_count INT DEFAULT 0,                -- 失败数量
    skipped_count INT DEFAULT 0,               -- 跳过数量（已是目标版本）
    status SMALLINT DEFAULT 0,                 -- 0=待执行 1=进行中 2=已完成 3=部分失败 4=已取消
    started_at TIMESTAMP,                      -- 开始时间
    finished_at TIMESTAMP,                     -- 完成时间
    created_by VARCHAR(50),                    -- 创建人
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE public.sys_schema_upgrade_task IS '批量升级任务表';
COMMENT ON COLUMN public.sys_schema_upgrade_task.status IS '0=待执行 1=进行中 2=已完成 3=部分失败 4=已取消';

-- 升级详情表（每个schema的升级记录）
CREATE TABLE IF NOT EXISTS public.sys_schema_upgrade_detail (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,                   -- 关联的任务ID
    tenant_id BIGINT,                          -- 租户ID
    schema_name VARCHAR(100) NOT NULL,         -- schema名称
    from_version VARCHAR(20),                  -- 升级前版本
    to_version VARCHAR(20),                    -- 升级后版本
    script_name VARCHAR(200),                  -- 执行的脚本名
    script_checksum VARCHAR(64),               -- 脚本MD5校验和
    execution_time_ms INT,                     -- 执行耗时（毫秒）
    status SMALLINT DEFAULT 0,                 -- 0=待执行 1=进行中 2=成功 3=失败 4=已回滚
    error_message TEXT,                        -- 错误信息
    executed_at TIMESTAMP,                     -- 执行时间
    FOREIGN KEY (task_id) REFERENCES public.sys_schema_upgrade_task(id)
);

COMMENT ON TABLE public.sys_schema_upgrade_detail IS 'Schema升级详情表';
COMMENT ON COLUMN public.sys_schema_upgrade_detail.status IS '0=待执行 1=进行中 2=成功 3=失败 4=已回滚';

-- 创建索引
CREATE INDEX idx_upgrade_detail_task ON public.sys_schema_upgrade_detail(task_id);
CREATE INDEX idx_upgrade_detail_schema ON public.sys_schema_upgrade_detail(schema_name);

-- 回滚记录表
CREATE TABLE IF NOT EXISTS public.sys_schema_rollback_log (
    id BIGSERIAL PRIMARY KEY,
    detail_id BIGINT NOT NULL,                 -- 关联的升级详情ID
    schema_name VARCHAR(100) NOT NULL,
    from_version VARCHAR(20),                  -- 回滚前版本
    to_version VARCHAR(20),                    -- 回滚后版本
    rollback_script VARCHAR(200),              -- 回滚脚本名
    execution_time_ms INT,
    status SMALLINT DEFAULT 0,                 -- 0=进行中 1=成功 2=失败
    error_message TEXT,
    executed_by VARCHAR(50),
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (detail_id) REFERENCES public.sys_schema_upgrade_detail(id)
);

COMMENT ON TABLE public.sys_schema_rollback_log IS 'Schema回滚日志表';

-- 初始化public schema版本记录
INSERT INTO public.sys_schema_version (tenant_id, schema_name, current_version, is_baseline, baseline_version)
VALUES (NULL, 'public', '1.0.0', TRUE, '1.0.0')
ON CONFLICT (schema_name) DO NOTHING;
```

#### 步骤 1.2.2: 执行建表SQL

```bash
psql -U postgres -d fitness-edu -f seer-fitness-system/src/main/resources/db/pgsql/flyway_management_tables.sql
```

#### 步骤 1.2.3: 为现有租户schema创建版本记录

```sql
-- 为所有现有租户创建基线版本记录
INSERT INTO public.sys_schema_version (tenant_id, schema_name, current_version, is_baseline, baseline_version)
SELECT
    t.id,
    t.schema_name,
    '1.0.0',
    TRUE,
    '1.0.0'
FROM public.sys_tenant t
WHERE t.delete_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM public.sys_schema_version v WHERE v.schema_name = t.schema_name
  );
```

**验收标准**:
- ✅ 4张表创建成功
- ✅ public schema有版本记录 (version=1.0.0)
- ✅ 所有现有租户schema有版本记录

---

### 任务 1.3: 迁移现有SQL模板为Flyway格式 (3天)

#### 步骤 1.3.1: 创建迁移文件目录结构

```
seer-fitness-system/src/main/resources/db/migration/
├── common/                           # 所有schema通用的迁移
│   ├── V1.0.0__baseline_schema.sql   # 基线版本（现有表结构）
│   └── .gitkeep
├── public/                           # 仅public schema
│   ├── V1.0.0__baseline_public.sql
│   └── .gitkeep
├── tenant/                           # 仅租户schema
│   ├── V1.0.0__baseline_tenant.sql
│   └── .gitkeep
└── rollback/                         # 回滚脚本（自定义实现）
    ├── U1.0.0__rollback_baseline.sql
    └── .gitkeep
```

#### 步骤 1.3.2: 拆分现有模板为基线版本

**源文件**: `tenant_schema_template.sql` (570行)

**目标文件结构**:

1. **V1.0.0__baseline_tenant_tables.sql** - 表结构DDL
2. **V1.0.0__baseline_tenant_indexes.sql** - 索引创建
3. **V1.0.0__baseline_tenant_comments.sql** - 表注释

**示例**: `db/migration/tenant/V1.0.0__baseline_tenant_tables.sql`

```sql
-- ====================================================================================================
-- Flyway Migration: V1.0.0 - 租户Schema基线版本
-- 描述: 创建租户schema的所有核心表（用户、角色、菜单、组织等）
-- 作者: System
-- 日期: 2025-01-20
-- 注意: 此版本为现有租户的基线，不会在已有schema上执行
-- ====================================================================================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    org_id BIGINT,
    admin_flag SMALLINT DEFAULT 0,
    user_type SMALLINT DEFAULT 0,
    status SMALLINT DEFAULT 1,
    delete_flag SMALLINT DEFAULT 0,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ... 其他表 (sys_role, sys_menu, sys_role_menu, sys_user_role, sys_organization, sys_operation_log)
```

#### 步骤 1.3.3: 创建版本命名规范文档

**文件**: `docs/FLYWAY_MIGRATION_NAMING.md` (新建)

```markdown
# Flyway 迁移文件命名规范

## 版本号规范

采用语义化版本号: `Major.Minor.Patch`

- **Major**: 重大架构变更（如表重构、大规模字段改动）
- **Minor**: 功能性变更（新增表、新增字段、新增索引）
- **Patch**: 修复性变更（修改字段类型、修改默认值、数据修正）

## 文件命名格式

### 正向迁移 (Versioned Migration)

格式: `V{version}__{description}.sql`

示例:
- `V1.0.0__baseline_tenant_tables.sql`
- `V1.1.0__add_user_last_login_field.sql`
- `V1.1.1__fix_menu_type_default.sql`
- `V2.0.0__refactor_rbac_structure.sql`

### 回滚脚本 (Undo Migration)

格式: `U{version}__{description}.sql`

示例:
- `U1.1.0__rollback_user_last_login.sql`
- `U1.1.1__rollback_menu_type_fix.sql`

## 描述规范

使用动词开头，描述要做的事情：

- `add_` - 新增（表、字段、索引）
- `remove_` - 删除
- `rename_` - 重命名
- `modify_` - 修改（类型、长度）
- `fix_` - 修复（数据、约束）
- `refactor_` - 重构

## 最佳实践

1. ✅ 一个文件只做一件事
2. ✅ 包含完整的注释说明
3. ✅ 使用 `IF EXISTS` 避免重复执行错误
4. ✅ 每个版本都准备对应的回滚脚本
5. ✅ 在测试环境先验证再上生产
```

**验收标准**:
- ✅ 目录结构创建完成
- ✅ V1.0.0基线版本文件创建完成
- ✅ 文件命名符合规范
- ✅ 每个文件有完整注释

---

### 任务 1.4: 配置多Schema支持 (2天)

#### 步骤 1.4.1: 创建Flyway多租户配置类

**文件**: `seer-fitness-system/src/main/java/com/seer/fitness/system/config/FlywayMultiTenantConfig.java` (新建)

```java
/**
 * Flyway多租户配置
 * 为每个租户schema创建独立的Flyway实例
 */
@Configuration
@EnableConfigurationProperties(FlywayProperties.class)
public class FlywayMultiTenantConfig {

    @Autowired
    private DataSource dataSource;

    /**
     * 为指定schema创建Flyway实例
     */
    public Flyway createFlywayForSchema(String schemaName) {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration/common", "classpath:db/migration/tenant")
            .schemas(schemaName)  // 指定schema
            .baselineOnMigrate(true)
            .baselineVersion("1.0.0")
            .table("flyway_schema_history")  // Flyway自己的历史表
            .load();
    }

    /**
     * 为public schema创建Flyway实例
     */
    public Flyway createFlywayForPublic() {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration/common", "classpath:db/migration/public")
            .schemas("public")
            .baselineOnMigrate(true)
            .baselineVersion("1.0.0")
            .table("flyway_schema_history")
            .load();
    }
}
```

#### 步骤 1.4.2: 扩展TenantSchemaService

**修改文件**: `TenantSchemaService.java`

```java
/**
 * 创建Schema时自动初始化Flyway基线
 */
public void createSchemaAndInitTables(...) {
    // ... 现有创建逻辑 ...

    // 新增：初始化Flyway基线
    initFlywayBaseline(schemaName, tenantId);
}

/**
 * 为新建的schema初始化Flyway基线版本
 */
private void initFlywayBaseline(String schemaName, Long tenantId) {
    log.info("初始化Schema Flyway基线: {}", schemaName);

    Flyway flyway = flywayMultiTenantConfig.createFlywayForSchema(schemaName);
    flyway.baseline();  // 标记为基线版本1.0.0

    // 记录到版本管理表
    jdbcTemplate.update(
        "INSERT INTO sys_schema_version (tenant_id, schema_name, current_version, is_baseline, baseline_version) " +
        "VALUES (:tenantId, :schemaName, '1.0.0', TRUE, '1.0.0')",
        Map.of("tenantId", tenantId, "schemaName", schemaName)
    );

    log.info("Schema {} Flyway基线初始化完成", schemaName);
}
```

**验收标准**:
- ✅ FlywayMultiTenantConfig类创建成功
- ✅ 可以为任意schema创建Flyway实例
- ✅ 新建租户时自动初始化Flyway基线
- ✅ flyway_schema_history表在新schema中生成

---

## ✅ Phase 1 验收标准总览

完成以下所有项，Phase 1才算完成：

### 功能验收
- [ ] 1. Flyway依赖正确引入，应用正常启动
- [ ] 2. 4张版本管理表在public schema创建成功
- [ ] 3. 所有现有租户有版本记录（version=1.0.0）
- [ ] 4. 迁移文件目录结构完整
- [ ] 5. V1.0.0基线版本文件完整且可执行
- [ ] 6. 可以为任意schema创建Flyway实例
- [ ] 7. 新建租户时自动初始化Flyway

### 代码验收
- [ ] 8. 所有新增类有完整注释
- [ ] 9. 符合现有代码规范
- [ ] 10. 配置文件有详细说明

### 文档验收
- [ ] 11. 迁移文件命名规范文档完成
- [ ] 12. Phase 1实施记录文档完成

---

## 🚀 Phase 2-4 概要 (后续详细展开)

### Phase 2: 升级管理服务开发
- SchemaVersionService: 查询schema版本
- SchemaUpgradeService: 执行升级
- 批量升级逻辑
- 失败回滚逻辑

### Phase 3: 管理界面开发
- Schema版本一览表
- 升级任务创建页面
- 实时进度监控

### Phase 4: 测试和文档
- 单元测试覆盖率 >80%
- 集成测试场景覆盖
- 升级SOP文档
- API文档

---

## 📞 联系与支持

- **技术负责人**: [待定]
- **项目文档**: `docs/FLYWAY_*.md`
- **代码位置**: `seer-fitness-system/src/main/java/com/seer/fitness/system/flyway/`

**最后更新**: 2025-01-20
**文档版本**: v1.0.0
