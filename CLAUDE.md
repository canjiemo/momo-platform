# CLAUDE.md

这个文件为Claude Code提供项目指导信息。

---

## 📋 项目概述

**Seer Fitness Edu (先知智慧体育-校园)** 是一个校园健身教育管理系统，采用Spring Boot 3.5.6 + Java 17开发，实现了完整的RBAC权限控制系统。

**技术栈**:
- Spring Boot 3.5.6 + Java 17 + Maven
- PostgreSQL 16.10 + Redis 8.2.1
- MyJPA (自定义ORM) + MyMVC
- JWT认证 + BCrypt密码 + Druid连接池 + Undertow服务器
- Flyway 数据库版本管理

**核心功能**:
- 多租户管理 (Schema隔离 + Flyway版本控制)
- 用户管理、角色管理、菜单管理、组织架构
- RBAC权限控制 (30个权限点)
- JWT + Redis会话管理
- 操作日志审计
- 数据字典管理
- 平台角色/菜单模板自动同步到租户

---

## 🚀 快速开始

### 构建和运行

```bash
# 编译项目
mvn clean package -DskipTests

# 运行应用 (开发环境)
mvn spring-boot:run -pl seer-fitness-boot

# 运行JAR
java -jar seer-fitness-boot/target/seer-fitness-boot-1.0.0.jar

# 指定环境
java -jar seer-fitness-boot/target/seer-fitness-boot-1.0.0.jar --spring.profiles.active=prod
```

### 数据库初始化

```bash
# 1. 创建数据库
createdb -U postgres seer_fitness_edu

# 2. 执行初始化脚本
psql -U postgres -d seer_fitness_edu -f src/main/resources/db/pgsql/001_create_tables.sql
psql -U postgres -d seer_fitness_edu -f src/main/resources/db/pgsql/002_init_data.sql

# 3. 导入测试数据 (可选)
psql -U postgres -d seer_fitness_edu -f docs/test/test_roles_menus.sql
```

### 默认账号

- **管理员**: superadmin / Aa123456!
- **测试账号**: superadmin, testuser1-10 / Aa123456!

### 运行测试

```bash
# 运行完整的120个安全测试
cd docs/test && bash complete_120_tests.sh

# 运行快速测试 (10个用例)
cd docs/test && bash security_test.sh
```

---

## 🏗️ 项目架构

### 模块结构

```
seer-fitness-edu/                    (父POM)
├── seer-fitness-framework/          (框架核心库)
│   ├── 基础实体: SeerDictType, SeerDictData
│   ├── 枚举: AuthMode, LockStrategy, MenuType
│   └── 模型: AccountFailRecord, AccountLockInfo
│
├── seer-fitness-system/             (系统模块库)
│   ├── 用户、角色、菜单、组织管理
│   ├── JWT认证 + RBAC权限控制
│   ├── Redis缓存 + 操作日志
│   └── 数据字典管理
│
└── seer-fitness-boot/               (启动模块)
    └── 主类: SeerFitnessEduApplication
```

### 核心包结构

- `com.seer.fitness.framework` - 框架基础
- `com.seer.fitness.system` - 系统业务
- `com.seer.fitness.edu` - 启动配置

### 关键技术点

**1. ORM框架**: MyJPA (自定义)
- 使用 `@MyTable` 注解 (非标准JPA `@Table`)
- BaseDao 提供CRUD操作
- 支持软删除 (deleteFlag)

**2. 连接池**: Druid (阿里巴巴)
```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource  # 必须指定
    druid:
      initial-size: 5
      max-active: 20
```

**3. 缓存策略**: 两级缓存
- **L1**: Caffeine本地缓存 (字典标签，5分钟TTL)
- **L2**: Redis分布式缓存 (用户会话，24小时TTL)

**4. 类型约定**:
- Java: `Boolean` (非 `boolean`)
- PostgreSQL: `SMALLINT`
- 自增主键: `BIGSERIAL`

---

## 🏢 多租户架构与Flyway版本管理

### 多租户架构

**设计模式**: Schema-based Multi-tenancy (基于PostgreSQL Schema的租户隔离)

**核心概念**:
- **public schema**: 存储平台级数据（租户表、平台菜单/角色模板）
- **tenant schema**: 每个租户独立的schema（用户、角色、菜单、组织）
- **数据隔离**: 完全的数据隔离，避免跨租户数据泄露
- **动态路由**: 根据请求上下文自动切换到正确的tenant schema

**租户创建流程** (5个步骤):
1. **CREATE_SCHEMA** - 创建租户专属PostgreSQL Schema
2. **CREATE_TABLE** - 在租户Schema中创建业务表
3. **INIT_FLYWAY** - 初始化Flyway版本管理基线 (✨ 2025-10-18新增)
4. **CREATE_ADMIN** - 创建租户管理员账号
5. **SYNC_TEMPLATES** - 自动同步平台菜单/角色模板到租户

**关键服务**:
- `TenantService` - 租户CRUD管理
- `TenantSchemaService` - 租户Schema创建和初始化
- `TenantTemplateAutoSyncService` - 平台模板自动同步
- `DynamicTenantDataSourceManager` - 动态数据源路由

### Flyway数据库版本管理

**Flyway版本**: 最新稳定版 (通过Spring Boot 3.5.6管理)
**完成状态**: ✅ Phase 1 基础设施搭建完成 (2025-10-18)

**核心功能**:
- 为每个租户schema独立管理数据库版本
- 支持批量升级所有租户
- 迁移脚本自动执行和回滚能力
- 版本一致性验证

**目录结构**:
```
seer-fitness-system/src/main/resources/db/migration/
├── common/          # 通用迁移脚本（同时适用于public和tenant）
├── public/          # public schema专用迁移脚本
├── tenant/          # 租户schema专用迁移脚本
│   └── V1.0.0__baseline.sql    # 租户基线版本
└── rollback/        # 回滚脚本
```

**版本管理表** (public schema):
```
sys_schema_version          # 记录每个schema的当前版本
sys_schema_upgrade_task     # 批量升级任务管理
sys_schema_upgrade_detail   # 单个schema升级详情
sys_schema_rollback_log     # 回滚历史记录
```

**配置文件** (`application.yml`):
```yaml
flyway:
  enabled: false                  # 禁用自动迁移（手动控制）
  baseline-on-migrate: true       # 首次迁移时自动基线化
  baseline-version: 1.0.0         # 基线版本号
  locations: classpath:db/migration
  encoding: UTF-8
  validate-on-migrate: true
  out-of-order: false
  placeholder-replacement: false

flyway:
  multi-tenant:
    enabled: true                 # 启用多租户Flyway
    auto-upgrade: false           # 禁用自动升级（手动触发）
    batch-size: 5                 # 批量升级租户数量
    delay-between-batches: 10     # 批次间延迟(秒)
    version-table: sys_schema_version
```

**核心配置类**:
- `FlywayConfig` - 禁用Spring Boot自动迁移
- `FlywayMultiTenantConfig` - 多租户Flyway管理（8个核心方法）
  - `createFlywayForTenantSchema()` - 创建租户Flyway实例
  - `createFlywayForPublicSchema()` - 创建平台Flyway实例
  - `baselineSchema()` - 执行基线初始化
  - `migrateSchema()` - 执行迁移
  - `validateSchema()` - 验证迁移状态
  - `getCurrentVersion()` - 获取当前版本
  - `cleanSchema()` - 清理历史（开发用）
  - `repairSchema()` - 修复迁移状态

**命名规范**:
- **版本化迁移**: `V{version}__{description}.sql`
  - 示例: `V1.0.1__add_user_email_column.sql`
- **可重复迁移**: `R__{description}.sql`
  - 示例: `R__refresh_user_statistics_view.sql`
- **撤销迁移**: `U{version}__{description}.sql`
  - 示例: `U1.0.1__remove_user_email_column.sql`

**版本号规则**: 语义化版本 (Major.Minor.Patch)
- **Major**: 重大架构变更（不兼容）
- **Minor**: 新功能（向后兼容）
- **Patch**: Bug修复和优化

**使用示例**:
```java
// 为租户schema创建Flyway实例
Flyway flyway = flywayMultiTenantConfig.createFlywayForTenantSchema("tenant_abc");

// 执行基线初始化
String baselineVersion = flywayMultiTenantConfig.baselineSchema("tenant_abc");

// 执行迁移
int migratedCount = flywayMultiTenantConfig.migrateSchema("tenant_abc");

// 验证迁移状态
boolean isValid = flywayMultiTenantConfig.validateSchema("tenant_abc");

// 查询当前版本
String currentVersion = flywayMultiTenantConfig.getCurrentVersion("tenant_abc");
```

**相关文档**:
- `docs/FLYWAY_IMPLEMENTATION_PLAN.md` - Flyway实现计划（4个阶段）
- `docs/FLYWAY_MIGRATION_NAMING.md` - 迁移脚本命名规范（500+行）
- `docs/FLYWAY_PHASE1_COMPLETION.md` - Phase 1完成总结

**当前状态**:
- ✅ Phase 1: 基础设施搭建完成 (2025-10-18, commit f341025)
- ⏳ Phase 2: Schema迁移管理 (待开发)
- ⏳ Phase 3: 迁移脚本管理 (待开发)
- ⏳ Phase 4: 监控和运维 (待开发)

---

## 🔐 认证与授权

### 登录流程

```bash
# 1. 获取验证码
curl http://localhost:8080/auth/captcha

# 2. 登录 (测试可从Redis读取验证码)
CAPTCHA_CODE=$(redis-cli GET "captcha:验证码ID")

curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "superadmin",
    "password": "Aa123456!",
    "captchaId": "验证码ID",
    "captcha": "'$CAPTCHA_CODE'"
  }'

# 3. 使用Token访问API
curl -H "Authorization: Bearer <token>" http://localhost:8080/system/user/search
```

### 权限控制

**认证流程** (`AuthInterceptor:39`):
1. 从 `Authorization: Bearer <token>` 提取JWT
2. 从Redis获取用户缓存信息 (`user:token:{tokenId}`)
3. 检查白名单
4. 验证 `@RequireAuth` 注解
5. 超级管理员 (adminFlag=1) 绕过权限检查
6. 验证权限/角色

**权限注解**:
```java
@RequireAuth(login = false)                    // 公开接口
@RequireAuth(login = true)                     // 仅需登录
@RequireAuth(permissions = {"user:create"})    // 需要特定权限
@RequireAuth(roles = {"admin"}, mode = AuthMode.ALL)  // 需要所有角色
```

**获取当前用户** (重要！):
```java
import com.seer.fitness.system.util.SecurityContextUtil;
import com.seer.fitness.system.dto.UserCacheInfo;

// ✅ 正确做法
UserCacheInfo currentUser = SecurityContextUtil.getCurrentUser();
Long userId = currentUser.getUserId();

// ❌ 错误做法
Long userId = 1L;  // 永远不要硬编码用户ID！
```

### 30个权限点

- **用户**: user:view, user:create, user:update, user:delete, user:init, user:reset
- **角色**: role:view, role:create, role:update, role:delete, role:assign
- **菜单**: menu:view, menu:create, menu:update, menu:delete
- **组织**: organization:view, organization:create, organization:update, organization:delete
- **日志**: log:view, log:delete, log:export
- **字典**: dict:view, dict:create, dict:update, dict:delete

---

## 📡 API接口清单

### 认证接口

```
POST /auth/login          # 用户登录 (公开)
GET  /auth/captcha        # 获取验证码 (公开)
```

### 用户管理

```
POST /system/user/search              # 分页查询 [user:view]
GET  /system/user/{id}                # 用户详情 [user:view]
POST /system/user/create              # 创建用户 [user:create]
POST /system/user/update              # 更新用户 [user:update]
POST /system/user/delete              # 删除用户 [user:delete]
POST /system/user/change-password     # 修改密码 [登录即可]
POST /system/user/init-password       # 初始化密码 [user:init]
POST /system/user/reset-password-admin # 重置密码 [user:reset]
GET  /system/user/profile             # 个人信息 [登录即可]
```

### 角色管理

```
POST /system/role/search              # 分页查询 [role:view]
GET  /system/role/list                # 角色列表 [role:view]
POST /system/role/create              # 创建角色 [role:create]
POST /system/role/update              # 更新角色 [role:update]
POST /system/role/delete              # 删除角色 [role:delete]
POST /system/role/assign-menus        # 分配权限 [role:assign]
GET  /system/role/menus/{id}          # 角色权限 [role:view]
```

### 菜单管理

```
GET  /system/menu/tree                # 菜单树 [menu:view]
GET  /system/menu/user-menus          # 当前用户菜单 [登录即可]
POST /system/menu/create              # 创建菜单 [menu:create]
POST /system/menu/update              # 更新菜单 [menu:update]
POST /system/menu/delete              # 删除菜单 [menu:delete]
```

### 组织管理

```
POST /system/organization/search      # 分页查询 [organization:view]
GET  /system/organization/tree        # 组织树 [organization:view]
POST /system/organization/create      # 创建组织 [organization:create]
POST /system/organization/update      # 更新组织 [organization:update]
POST /system/organization/delete      # 删除组织 [organization:delete]
```

### 操作日志

```
POST /system/operation-log/search     # 分页查询 [log:view]
GET  /system/operation-log/{id}       # 日志详情 [log:view]
POST /system/operation-log/delete     # 删除日志 [log:delete]
POST /system/operation-log/export     # 导出日志 [log:export]
GET  /system/operation-log/stats/*    # 统计接口 [log:view]
```

完整API文档: `docs/test/API_ENDPOINTS.md`

---

## 🧪 测试与质量保证

### 自动化安全测试

**位置**: `docs/test/`

**测试脚本**:
- `complete_120_tests.sh` - 完整120个测试用例
- `security_test.sh` - 快速10个测试样例

**测试覆盖**:
- ✅ 120个测试用例 (6组: A-F)
- ✅ 30个权限点全覆盖
- ✅ 10个测试角色
- ✅ 10个测试用户
- ✅ 认证、授权、CRUD、权限绕过测试

**测试用户** (密码: `Aa123456!`):
- `superadmin` - 超级管理员 (admin_flag=1, 绕过所有权限检查)
- `testadmin` - 完整权限管理员
- `testuser1` - 完整用户管理权限
- `testuser2` - 部分用户权限 (查看+创建)
- `testuser3` - 仅查看用户权限
- `testuser4-10` - 各种角色/菜单/组织权限组合

### 测试数据

**数据库脚本**: `docs/test/test_roles_menus.sql`
- 10个测试角色 (ID: 100-109)
- 10个测试用户 (ID: 10-100)
- 40个菜单项 (30个权限按钮)
- 76个角色-菜单关联

---

## 🔧 开发规范

### 1. 获取当前用户

```java
// ✅ 正确
import com.seer.fitness.system.util.SecurityContextUtil;

UserCacheInfo user = SecurityContextUtil.getCurrentUser();
if (user != null && user.getUserId() != null) {
    Long userId = user.getUserId();
    String username = user.getUsername();
}

// ❌ 错误
Long userId = 1L;  // 永远不要硬编码！
```

### 2. 验证外键引用

```java
// ✅ 正确 - 先验证再设置
if (request.getLeaderId() != null) {
    SysUser leader = baseDao.queryByIdWithDeleteCondition(
        request.getLeaderId(), SysUser.class
    );
    if (leader == null) {
        throw new BusinessException("指定的负责人不存在");
    }
    org.setLeaderId(request.getLeaderId());
}

// ❌ 错误 - 直接信任客户端输入
org.setLeaderId(request.getLeaderId());
```

### 3. 密码管理

**当前标准**:
- 测试用户密码: `Aa123456!`
- BCrypt Hash: `$2a$12$S1Fu/0.DthE.9JTvUDwQQeUwLabpWmBeKgebsBT11KrhgBqWr13HS`
- 默认admin密码: `admin123`

### 4. 实体类规范

```java
@Data
@MyTable("sys_user")  // 使用 @MyTable (非 @Table)
public class SysUser {
    private Long id;           // BIGSERIAL
    private String username;
    private Integer deleteFlag;  // Integer (非 Boolean)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## 🐛 已知问题与修复

### 最近修复 (2025-10-04)

详见: `docs/test/SECURITY_FIXES.md`

1. ✅ **已修复**: UserController.changePassword 硬编码userId
   - 问题: 所有用户修改密码时都会改admin的密码
   - 修复: 使用 `SecurityContextUtil.getCurrentUser().getUserId()`
   - 文件: `UserController.java:133`

2. ✅ **已修复**: OrganizationService 缺少负责人验证
   - 问题: 可创建关联到不存在用户的组织
   - 修复: 创建/更新前验证leaderId存在性
   - 文件: `OrganizationService.java:221, 285`

### 待实现功能

详见: `docs/test/TODO_ANALYSIS.md`

**低优先级** (OperationLogService - 7个):
- 历史日志清理 (line 215)
- 统计功能: 操作类型、模块、用户活跃度、失败率、趋势 (lines 225-261)
- 导出功能 (line 270)

所有TODO都有详细实现代码在TODO_ANALYSIS.md中。

---

## 🔌 MCP工具使用

项目已配置MCP (Model Context Protocol) 用于Redis和PostgreSQL操作。

### Redis操作

```bash
# 查看Redis信息
mcp__redis-mcp-server__info

# 获取所有键
mcp__redis-mcp-server__scan_all_keys

# 获取值
mcp__redis-mcp-server__get "user:token:xxx"

# 删除键
mcp__redis-mcp-server__delete "captcha:xxx"
```

### 数据库操作

```bash
# 执行SQL查询
mcp__postgres__execute_sql "SELECT * FROM sys_user WHERE username = 'superadmin'"

# 更新密码
mcp__postgres__execute_sql "UPDATE sys_user SET password = '$2a$12$...' WHERE id = 10"

# 查看测试用户
mcp__postgres__execute_sql "SELECT id, username, admin_flag FROM sys_user WHERE id >= 10 ORDER BY id"
```

---

## 📚 文档索引

### Flyway与多租户文档 (`docs/`):

| 文档 | 说明 |
|------|------|
| `FLYWAY_IMPLEMENTATION_PLAN.md` | Flyway实现计划（4个阶段） |
| `FLYWAY_MIGRATION_NAMING.md` | 迁移脚本命名规范（500+行详细说明） |
| `FLYWAY_PHASE1_COMPLETION.md` | Phase 1完成总结和验收报告 |

### 测试文档 (`docs/test/`):

| 文档 | 说明 |
|------|------|
| `README.md` | 完整使用指南，包含登录流程 |
| `QUICK_START.md` | 快速开始指南 |
| `API_ENDPOINTS.md` | 完整API端点目录 (70+接口) |
| `API_PATH_FIXES.md` | API路径修正清单 |
| `CHANGELOG.md` | 测试文档更新日志 |
| `TODO_ANALYSIS.md` | TODO详细分析 (含实现代码) |
| `SECURITY_FIXES.md` | 安全漏洞修复报告 |
| `comprehensive_test_plan.md` | 120个测试用例设计 |
| `FINAL_TEST_SUMMARY.md` | 测试执行结果 |
| `complete_120_tests.sh` | 自动化120测试脚本 |
| `security_test.sh` | 快速10测试脚本 |
| `test_roles_menus.sql` | 测试数据SQL脚本 |

### 流程文档 (`docs/相关流程类文档/`):

| 文档 | 说明 |
|------|------|
| `租户创建流程时序.md` | 租户创建完整流程时序图 |
| `角色同步流程时序.md` | 平台角色同步到租户流程 |

---

## ⚙️ 配置文件

### application.yml (主配置)

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource  # ⚠️ 必须指定
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/seer_fitness_edu
    username: postgres
    password: postgres
    druid:
      initial-size: 5
      max-active: 20

  redis:
    host: localhost
    port: 6379
    database: 0

server:
  port: 8080

jwt:
  secret: your-secret-key-change-in-production
  expiration: 86400000  # 24小时
```

### 环境切换

```bash
# 开发环境 (默认)
java -jar app.jar

# 生产环境
java -jar app.jar --spring.profiles.active=prod

# 本地环境
java -jar app.jar --spring.profiles.active=local
```

---

## 🎯 重要提醒

### ⚠️ 必须遵守的规则

1. **永远不要硬编码用户ID** - 使用 `SecurityContextUtil.getCurrentUser()`
2. **验证所有外键引用** - 插入/更新前必须验证关联对象存在
3. **使用正确的类型映射** - Java `Boolean` ↔ PostgreSQL `BOOLEAN`
4. **指定Druid DataSource类型** - 必须在配置中指定 `type: com.alibaba.druid.pool.DruidDataSource`
5. **使用@MyTable注解** - 实体类使用 `@MyTable` (非 `@Table`)

### ✅ 最佳实践

1. 所有Controller方法使用 `@RequireAuth` 注解控制权限
2. 使用 `@OperationLog` 记录关键操作
3. Service方法使用 `@Transactional` 控制事务
4. 验证输入参数使用 `@Valid` + Jakarta Validation
5. 抛出业务异常使用 `BusinessException`
6. 使用MCP工具进行Redis和数据库操作

---

## 📝 更新日志

### 2025-10-18: Flyway数据库版本管理 Phase 1
- ✅ 实现Flyway基础设施（依赖、配置、版本管理表）
- ✅ 创建迁移脚本目录结构（common/public/tenant/rollback）
- ✅ 实现FlywayMultiTenantConfig（8个核心方法）
- ✅ 集成到租户创建流程（新增INIT_FLYWAY步骤）
- ✅ 编写完整规范文档（命名规范、实现计划、完成总结）
- 📦 提交: commit f341025

### 2025-10-04: 安全修复和多租户架构完善
- ✅ 修复UserController.changePassword硬编码userId问题
- ✅ 修复OrganizationService缺少负责人验证
- ✅ 实现平台角色/菜单模板管理
- ✅ 实现租户模板自动同步机制
- ✅ 完成120个安全测试用例

---

**项目版本**: 1.0.0
**最后更新**: 2025-10-18
**维护者**: Claude (AI助手)
