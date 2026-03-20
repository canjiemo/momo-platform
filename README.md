# momo-platform

基于 Spring Boot 3 的通用后端开发脚手架，内置多租户、认证授权、AI 对话、文件存储等开箱即用的能力。

## 技术栈

| 类别 | 技术 |
|------|------|
| 核心框架 | Spring Boot 3.5、Java 21 |
| 数据层 | PostgreSQL、MyJDBC（多租户 ORM）、Druid 连接池、Flyway 迁移 |
| 缓存 | Redis、Caffeine 本地缓存 |
| 认证 | JWT（JJWT 0.11）|
| AI | Spring AI 1.0.0-M6、Ollama、OpenAI 协议（DashScope 兼容）、pgvector |
| 文件存储 | MinIO 对象存储 |
| 工具库 | Lombok、FastJSON2、Guava |
| Web 层 | MyMVC（统一响应、参数绑定） |
| 数据字典 | MyDict（编译期注解处理器） |

## 模块结构

```
momo-platform
├── momo-framework      # 基础框架层：JWT、Redis、密码策略、账号锁定、公共注解与工具类
├── momo-system         # 系统管理：租户、用户、角色、菜单、组织、字典、操作日志、定时任务
├── momo-file           # 文件服务：MinIO 对象存储、文件配置管理
├── momo-ai             # AI 模块：多 Provider 管理、会话、对话、NL2SQL 引擎、数据目录
├── momo-business       # 业务扩展层（按需添加业务模块）
└── momo-boot           # 启动模块：配置聚合、Flyway 迁移脚本
```

## 核心功能

### momo-framework
- JWT 签发与校验，可配置密钥和过期时间
- Redis 工具封装（String、Hash、List、Set、ZSet）
- 账号锁定：支持渐进式锁定策略，可配置失败次数和锁定时长
- 密码策略：强度校验、加密存储
- `@RequireAuth` 认证注解、`@OperationLog` 操作日志注解

### momo-system
- **多租户**：基于 MyJDBC 自动注入 `tenant_id`，租户间数据完全隔离
- **RBAC 权限**：用户 → 角色 → 菜单三级权限模型，支持平台级和租户级角色
- **组织架构**：树形组织管理，支持平台级和租户级组织
- **数据字典**：字典类型 + 字典数据，Redis 缓存，启动自动预热
- **操作日志**：AOP 切面自动记录，支持分页查询
- **定时任务**：基于 Quartz，支持 cron 表达式，运行时动态管理
- **认证**：登录、登出、验证码、修改密码、初始化密码

### momo-file
- 对接 MinIO，支持多存储配置
- 文件上传、下载、删除
- 平台级文件配置管理

### momo-ai
- **多 Provider**：统一管理 Ollama、OpenAI 协议的多个 AI 提供商，支持运行时切换
- **会话管理**：多轮对话，上下文保持
- **NL2SQL 引擎**：自然语言转 SQL，含 Schema 上下文构建和 SQL 合法性校验
- **数据目录**：管理可供 AI 查询的数据源元信息

## 快速启动

### 前置依赖

- Java 21+
- PostgreSQL 14+（需启用 pgvector 扩展）
- Redis 6+
- MinIO（可选，文件模块需要）
- Ollama 或 OpenAI 兼容接口（可选，AI 模块需要）

### 初始化数据库

```sql
CREATE DATABASE "momo-platform";
CREATE EXTENSION IF NOT EXISTS vector;
```

### 修改配置

编辑 `momo-boot/src/main/resources/application-local.yml`，配置数据库、Redis 连接信息。

### 启动

```bash
mvn spring-boot:run -pl momo-boot
```

Flyway 会在启动时自动执行 `momo-boot/src/main/resources/db/migration/` 下的 SQL 脚本完成表结构初始化。

## 配置说明

| 配置项 | 说明 |
|--------|------|
| `jwt.secret` | JWT 签名密钥（256 位） |
| `jwt.expiration` | Token 过期时间（毫秒，默认 24 小时） |
| `myjdbc.tenant.enabled` | 是否开启多租户隔离 |
| `myjdbc.show-sql.enabled` | 是否打印 SQL 调试日志 |
| `spring.flyway.enabled` | 是否启用 Flyway 自动迁移 |

## 项目信息

- **GroupId**: `io.github.canjiemo`
- **ArtifactId**: `momo-platform`
- **Version**: `1.0.0`
- **Author**: canjiemo@gmail.com
