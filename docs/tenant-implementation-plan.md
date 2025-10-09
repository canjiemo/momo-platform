# 🗓️ 租户模块实施计划

## 📋 项目概览

**目标**：为 seer-fitness-edu 系统增加多租户（多学校）隔离能力
**技术方案**：PostgreSQL Schema 隔离 + 分租户连接池
**租户规模**：100 所学校以内
**并发需求**：支持每个学校 1000 QPS（峰值场景）
**预计总工期**：8-10 个工作日

---

## 🎯 分阶段实施计划

### 📌 阶段 0：准备工作（0.5 天）

#### 任务清单

- [ ] **0.1 确认数据库环境**
  - 检查 PostgreSQL 版本（需 >= 10.0）
  - 确认 `max_connections` 配置（建议调整为 600）
  - 测试 Schema 切换功能

- [ ] **0.2 创建开发分支**
  ```bash
  git checkout -b feature/multi-tenant
  ```

- [ ] **0.3 确认模块依赖**
  - 检查 Druid 版本（需支持自定义 Filter）
  - 确认 MyJPA 版本
  - 确认 Redis/Kafka 环境（如果需要高并发方案）

**验收标准**：
- ✅ 数据库可以手动执行 `CREATE SCHEMA` 和 `SET search_path`
- ✅ Git 分支创建成功
- ✅ 依赖库版本确认

---

### 📌 阶段 1：数据库设计与初始化（1 天）

#### 任务清单

- [ ] **1.1 创建租户核心表**
  - 创建 `public.sys_tenant` 表
  - 创建 `public.sys_tenant_init_log` 表
  - 添加索引和注释

  **文件位置**：`docs/sql/tenant/tenant_core_tables.sql`

- [ ] **1.2 初始化数据字典**
  - 添加 `tenant_status` 字典类型及数据
  - 添加 `tenant_init_step` 字典类型及数据
  - 添加 `tenant_log_status` 字典类型及数据

  **文件位置**：`docs/sql/tenant/tenant_dict_data.sql`

- [ ] **1.3 准备租户 Schema 模板脚本**
  - 复制现有的 `docs/sql/rbac.sql` → `docs/sql/tenant/tenant_schema_template.sql`
  - 去掉 `public.` 前缀
  - 调整初始数据脚本

  **文件位置**：`docs/sql/tenant/tenant_schema_template.sql`

- [ ] **1.4 创建测试租户**
  - 手动创建 2 个测试 Schema（`school_test1`, `school_test2`）
  - 插入测试租户数据到 `sys_tenant`
  - 验证 Schema 切换

**验收标准**：
- ✅ 所有表创建成功，无 SQL 错误
- ✅ 数据字典可以通过前端接口查询到
- ✅ 手动执行 `SET search_path TO school_test1` 可以访问测试数据

**预计工时**：1 天

---

### 📌 阶段 2：Java 实体与基础服务（1.5 天）

#### 任务清单

- [ ] **2.1 创建租户模块包结构**
  ```
  seer-fitness-system/src/main/java/com/seer/fitness/system/
  ├── entity/
  │   ├── SysTenant.java
  │   └── SysTenantInitLog.java
  ├── dto/
  │   ├── TenantCreateRequest.java
  │   ├── TenantUpdateRequest.java
  │   ├── TenantQueryParam.java
  │   └── TenantDTO.java
  ├── enums/
  │   ├── TenantStatus.java
  │   └── InitStepType.java
  ├── service/
  │   ├── ITenantService.java
  │   ├── TenantService.java
  │   ├── ITenantSchemaService.java
  │   └── TenantSchemaService.java
  └── controller/
      └── TenantController.java
  ```

- [ ] **2.2 实现核心实体类**
  - `SysTenant.java`（租户实体）
  - `SysTenantInitLog.java`（初始化日志实体）
  - 使用 `@MyTable` 注解

- [ ] **2.3 实现枚举类**
  - `TenantStatus.java`（对应字典 `tenant_status`）
  - `InitStepType.java`（对应字典 `tenant_init_step`）

- [ ] **2.4 实现 DTO 类**
  - `TenantCreateRequest`（创建请求，包含 `@Valid` 校验）
  - `TenantUpdateRequest`（更新请求）
  - `TenantQueryParam`（查询参数）
  - `TenantDTO`（返回给前端的数据，包含 statusText）

- [ ] **2.5 实现基础服务接口**
  - `ITenantService`（定义接口方法）
  - `TenantService`（实现基本的 CRUD）
    - `search()`：分页查询
    - `getById()`：根据 ID 查询
    - `getByCode()`：根据编码查询
    - `create()`：创建租户（暂不包含 Schema 创建逻辑）
    - `update()`：更新租户
    - `enable()`/`disable()`：启用/禁用

**验收标准**：
- ✅ 所有实体类可以正常编译
- ✅ 可以通过 DAO 操作 `sys_tenant` 表
- ✅ 单元测试通过（CRUD 操作）

**预计工时**：1.5 天

---

### 📌 阶段 3：Schema 管理服务（2 天）

#### 任务清单

- [ ] **3.1 实现 TenantSchemaService**
  - `createSchemaAndInitTables()`：创建 Schema + 初始化表结构
  - `loadSqlScript()`：从 resources 加载 SQL 脚本
  - `logStep()`：记录初始化日志

- [ ] **3.2 实现 Schema 初始化流程**
  ```java
  1. 创建 Schema (CREATE SCHEMA)
  2. 切换到新 Schema (SET search_path)
  3. 执行 DDL 脚本（创建表）
  4. 执行默认数据脚本（角色、菜单）
  5. 创建管理员账号
  6. 记录每个步骤的日志
  7. 失败时自动回滚（DROP SCHEMA）
  ```

- [ ] **3.3 集成到 TenantService.create()**
  - 调用 `schemaService.createSchemaAndInitTables()`
  - 事务管理（失败时回滚租户记录）
  - 状态更新（PENDING → ENABLED）

- [ ] **3.4 编写单元测试**
  - 测试正常创建流程
  - 测试 DDL 失败的回滚逻辑
  - 验证日志记录

**验收标准**：
- ✅ 调用 `tenantService.create()` 可以自动创建 Schema
- ✅ Schema 中的表结构与模板一致
- ✅ 失败时可以正确回滚
- ✅ 初始化日志正确记录

**预计工时**：2 天

---

### 📌 阶段 4：分租户连接池（1.5 天）

#### 任务清单

- [ ] **4.1 实现动态数据源管理器**
  - 创建 `DynamicTenantDataSourceManager.java`
  - 实现 `getDataSource(tenantId)`（懒加载）
  - 实现 `createTenantDataSource()`（创建连接池）
  - 实现 `removeTenant()`（关闭连接池）

- [ ] **4.2 实现租户上下文**
  - 创建 `TenantContext.java`（ThreadLocal）
  - `setTenantId()` / `getTenantId()`
  - `setSchema()` / `getSchema()`
  - `setDataSource()` / `getDataSource()`
  - `clear()`（防止内存泄漏）

- [ ] **4.3 配置 Druid 连接池**
  - 调整 `application.yml`：去掉固定的 `currentSchema`
  - 配置连接初始化 SQL（动态设置）

- [ ] **4.4 编写集成测试**
  - 测试多线程并发访问不同租户
  - 验证 Schema 隔离性
  - 验证连接池复用

**验收标准**：
- ✅ 可以动态创建租户连接池
- ✅ 不同租户的查询互不干扰
- ✅ 新增租户无需重启应用

**预计工时**：1.5 天

---

### 📌 阶段 5：登录与鉴权改造（2 天）

#### 任务清单

- [ ] **5.1 调整登录接口**
  - 修改 `AuthController.login()`
  - 增加 `tenantCode` 参数（必填）
  - 根据 `tenantCode` 查询租户信息
  - 切换到租户 Schema 查询用户

- [ ] **5.2 扩展 JWT Token**
  - 修改 JWT 生成逻辑
  - 增加字段：`tenantId`, `tenantCode`, `schemaName`
  - 修改 JWT 解析逻辑

- [ ] **5.3 实现租户拦截器**
  - 创建 `TenantInterceptor.java`
  - 从 JWT Token 提取租户信息
  - 设置到 `TenantContext`
  - 处理平台管理员（访问 public schema）

- [ ] **5.4 注册拦截器**
  - 修改 `WebMvcConfig`
  - 添加 `TenantInterceptor`
  - 配置拦截路径（排除登录接口）

- [ ] **5.5 调整前端登录界面**
  - 增加"选择学校"下拉框
  - 或"租户编码"输入框
  - 调用 `/public/tenant/list` 获取学校列表

**验收标准**：
- ✅ 登录时必须选择学校
- ✅ JWT Token 包含租户信息
- ✅ 后续请求自动路由到正确的 Schema
- ✅ 不同学校的数据完全隔离

**预计工时**：2 天

---

### 📌 阶段 6：平台管理功能（1.5 天）

#### 任务清单

- [ ] **6.1 实现租户管理 Controller**
  - `POST /platform/tenant/create`：创建租户
  - `POST /platform/tenant/search`：分页查询
  - `GET /platform/tenant/{id}`：查询详情
  - `POST /platform/tenant/update`：更新租户
  - `POST /platform/tenant/enable/{id}`：启用
  - `POST /platform/tenant/disable/{id}`：禁用

- [ ] **6.2 实现查询接口优化**
  - 租户列表带统计信息（用户数、存储占用）
  - 过期租户告警

- [ ] **6.3 实现平台管理员权限**
  - 创建"平台管理"菜单
  - 配置权限点：`tenant:view`, `tenant:create` 等
  - 平台管理员切换租户功能（可选）

- [ ] **6.4 前端界面（可选，后期实现）**
  - 租户列表页面
  - 租户创建表单
  - 租户详情页

**验收标准**：
- ✅ 平台管理员可以通过接口创建租户
- ✅ 租户创建后自动完成 Schema 初始化
- ✅ 可以查询所有租户列表

**预计工时**：1.5 天

---

### 📌 阶段 7：测试与优化（1 天）

#### 任务清单

- [ ] **7.1 功能测试**
  - 创建 10 个测试租户
  - 每个租户创建测试用户
  - 登录并验证数据隔离
  - 测试 CRUD 操作

- [ ] **7.2 性能测试**
  - 使用 JMeter 或 wrk 进行压力测试
  - 测试目标：单租户 1000 QPS
  - 监控数据库连接数
  - 监控响应时间

- [ ] **7.3 边界测试**
  - 租户不存在
  - 租户已禁用
  - 租户已过期
  - Schema 创建失败

- [ ] **7.4 代码优化**
  - 添加日志
  - 异常处理优化
  - 代码注释完善

- [ ] **7.5 文档编写**
  - 更新 `docs/README.md`
  - 编写租户管理使用手册
  - 编写运维手册（DDL 升级流程）

**验收标准**：
- ✅ 所有功能测试通过
- ✅ 性能达标（P99 < 50ms）
- ✅ 边界情况处理正确
- ✅ 文档完整

**预计工时**：1 天

---

## 📊 总体时间规划

| 阶段 | 内容 | 工期 | 依赖 |
|------|------|------|------|
| 阶段0 | 准备工作 | 0.5天 | - |
| 阶段1 | 数据库设计 | 1天 | 阶段0 |
| 阶段2 | Java实体与服务 | 1.5天 | 阶段1 |
| 阶段3 | Schema管理 | 2天 | 阶段2 |
| 阶段4 | 分租户连接池 | 1.5天 | 阶段3 |
| 阶段5 | 登录与鉴权 | 2天 | 阶段4 |
| 阶段6 | 平台管理功能 | 1.5天 | 阶段5 |
| 阶段7 | 测试与优化 | 1天 | 阶段6 |
| **总计** | | **10-11天** | |

**关键里程碑**：
- 🎯 Day 3：完成数据库设计和基础服务
- 🎯 Day 5：完成 Schema 自动创建
- 🎯 Day 7：完成登录改造，支持租户隔离
- 🎯 Day 10：全部功能完成并测试通过

---

## 🚀 快速开始（首日执行）

### 第一步：创建数据库表

```bash
# 1. 连接数据库
psql -U admin -d fitness-edu

# 2. 创建租户表
\i docs/sql/tenant/tenant_core_tables.sql

# 3. 初始化字典数据
\i docs/sql/tenant/tenant_dict_data.sql

# 4. 创建测试租户
INSERT INTO public.sys_tenant
(tenant_code, tenant_name, schema_name, status, activated_at)
VALUES
('SCHOOL_TEST1', '测试学校1', 'school_test1', 1, NOW());
```

### 第二步：手动验证 Schema 切换

```sql
-- 创建测试 Schema
CREATE SCHEMA school_test1;

-- 切换 Schema
SET search_path TO school_test1;

-- 创建测试表
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50)
);

-- 插入测试数据
INSERT INTO sys_user (username) VALUES ('test_user');

-- 查询验证
SELECT * FROM sys_user;

-- 切换回 public
SET search_path TO public;
```

---

## ⚠️ 风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| Schema 创建失败 | 中 | 完善回滚逻辑，记录详细日志 |
| 连接池泄漏 | 高 | 严格测试，监控连接数 |
| 跨租户数据泄漏 | 高 | 多层测试验证，代码 Review |
| DDL 升级困难 | 中 | 编写自动化脚本，灰度升级 |
| 性能不达标 | 中 | 提前压测，优化索引 |

---

## 📝 开发规范

### Git 提交规范

```
feat(tenant): 添加租户基础表设计
feat(tenant): 实现租户CRUD服务
feat(tenant): 实现Schema自动创建
feat(tenant): 实现分租户连接池
feat(auth): 登录接口支持租户识别
test(tenant): 添加租户隔离性测试
docs(tenant): 更新租户管理文档
```

### 代码 Review 检查点

- [ ] 所有 SQL 操作前是否切换了 Schema
- [ ] ThreadLocal 是否正确清理
- [ ] 异常是否正确处理和记录
- [ ] 是否有单元测试覆盖
- [ ] 是否有足够的日志

---

## 📞 需要的资源

- **开发人员**：1-2 人
- **数据库权限**：创建 Schema、表的权限
- **测试环境**：独立的 PostgreSQL 实例
- **CI/CD**：自动化测试环境

---

## 🎯 技术架构总结

### 核心方案

```
跳绳设备上报（10万 QPS）
    ↓
API 服务（写入 Kafka）
    ↓
Kafka（缓冲峰值）
    ↓
消费者（100 个线程，按租户分组）
    ↓
分租户连接池（每个租户独立连接池，懒加载）
    ↓
PostgreSQL（100 个独立 Schema）
    ↓
数据量分散：每个 Schema 2GB（而不是单表 200GB）
```

### 关键优势

1. ✅ **查询性能 5-10 倍提升**（小表 + 小索引）
2. ✅ **缓存命中率提升 30-50%**（热点数据集中）
3. ✅ **维护成本降低**（VACUUM、ANALYZE 分散执行）
4. ✅ **故障隔离**（某个 Schema 故障不影响其他）
5. ✅ **灵活的分区策略**（根据学校特点定制）
6. ✅ **新增租户无需重启**（动态连接池热加载）

---

## 📚 相关文档

- [数据库设计详细文档](./tenant-database-design.md)
- [租户管理使用手册](./tenant-user-guide.md)
- [运维手册](./tenant-ops-guide.md)
- [性能测试报告](./tenant-performance-test.md)

---

**文档版本**: v1.0
**创建时间**: 2025-01-09
**最后更新**: 2025-01-09
**负责人**: 开发团队
