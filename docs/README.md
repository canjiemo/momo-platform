# Seer Fitness Edu - 系统架构与模块说明

## 📐 系统架构

### 整体架构
Seer Fitness Edu 是一个基于 **RBAC (Role-Based Access Control)** 和 **多租户 (Multi-Tenant)** 的健身教育管理系统，采用前后端分离架构，支持 IoT 设备实时数据推送。

```
┌──────────────────────────────────────────────────────────────────────┐
│                          IoT 设备层                                   │
│       跳绳机、跑步机、动感单车等健身设备（MQTT客户端）                  │
└──────────────────────────┬───────────────────────────────────────────┘
                           │ MQTT Publish
                           │ device/{tenantId}/{deviceId}/realtime
┌──────────────────────────▼───────────────────────────────────────────┐
│                       EMQX Broker (消息代理)                          │
│                    支持租户隔离的 MQTT 主题路由                        │
└──────┬────────────────────────────────────────────┬──────────────────┘
       │ $share/backend (负载均衡订阅)               │ MQTT/WebSocket
       │                                             │ (直接订阅设备数据)
┌──────▼─────────────────────────────────┐   ┌──────▼──────────────────┐
│         后端服务层 (K8s 多实例)          │   │      前端应用层          │
│         Spring Boot (无状态)            │   │  Vue/React + MQTT.js    │
├─────────────────────────────────────────┤   ├─────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐│   │ • 学生App (实时计数)     │
│ │认证授权   │ │租户隔离   │ │日志审计 ││   │ • 教室大屏 (班级排名)    │
│ └──────────┘ └──────────┘ └──────────┘│   │ • 管理后台 (数据统计)    │
│ ┌────────────────────────────────────┐│   └─────────────────────────┘
│ │ @PublicSchema 注解路由系统          ││
│ │ (自动 Schema 路由 + 字典共享)       ││
│ └────────────────────────────────────┘│
│ ┌────────────────────────────────────┐│
│ │ MQTT 消息处理 (异步)                ││
│ │ • 批量持久化                        ││
│ │ • 实时统计聚合                      ││
│ │ • 异常告警                          ││
│ └────────────────────────────────────┘│
└──────────────┬──────────────────────────┘
               │ JDBC (动态 Schema 路由)
┌──────────────▼──────────────────────────────────────────────────────┐
│                        数据持久层 (PostgreSQL)                        │
├──────────────────────────────────────────────────────────────────────┤
│  public schema (平台管理 + 共享字典) 🆕                               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐               │
│  │ 平台用户  │ │ 平台角色  │ │ 平台菜单  │ │ 租户表    │               │
│  │ 平台项目  │ │ 字典表    │ │ 配置表    │ │ 初始化日志│               │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘               │
├──────────────────────────────────────────────────────────────────────┤
│  school_001 schema (学校A租户数据)                                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐               │
│  │ 用户表    │ │ 角色表    │ │ 菜单表    │ │ 设备数据表│               │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘               │
├──────────────────────────────────────────────────────────────────────┤
│  school_002 schema (学校B租户数据)                                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐               │
│  │ 用户表    │ │ 角色表    │ │ 菜单表    │ │ 设备数据表│               │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘               │
└──────────────────────────────────────────────────────────────────────┘
```

### 技术栈
- **后端**: Spring Boot 3.5.6 + MyJPA + PostgreSQL 16.10
- **前端**: Vue.js/React + Ant Design
- **消息代理**: EMQX (MQTT Broker)
- **实时推送**: MQTT + WebSocket
- **认证**: JWT Token
- **权限**: RBAC (基于角色的访问控制)
- **密码加密**: BCrypt
- **多租户**: PostgreSQL Schema 隔离
- **AOP**: AspectJ (注解路由、日志记录)
- **容器化**: Docker + Kubernetes

---

## 🏗️ 项目模块结构

### Maven 模块划分

```
seer-fitness-edu (父模块)
├── seer-fitness-framework (框架层)
│   ├── @PublicSchema 注解
│   ├── MyJPA/MyMVC 集成
│   └── 通用工具类
├── seer-fitness-system (系统管理层)
│   ├── 用户、角色、菜单管理
│   ├── 租户管理 (Tenant)
│   ├── 字典管理 (Dictionary)
│   └── 操作日志
├── seer-fitness-business (业务功能层) 🆕
│   ├── 平台项目管理
│   ├── 租户项目管理
│   └── 其他业务功能
└── seer-fitness-boot (启动入口)
    ├── Spring Boot 主类
    └── 配置文件
```

### 依赖关系

```
boot → business → system → framework
  ↓       ↓         ↓         ↓
web     guava   redis    myjpa
jdbc            jwt      mymvc
druid           security jackson
fastjson2       fastjson2
postgresql      druid
              caffeine
```

### Maven 依赖管理 🆕

**父 pom.xml**：
- ✅ 统一版本管理（`<properties>`）
- ✅ 依赖版本控制（`<dependencyManagement>`）
- ✅ 子模块按需引用，无需指定版本号

**关键版本**：
```xml
<properties>
    <myjpa.version>spring3</myjpa.version>
    <jwt.version>0.11.5</jwt.version>
    <guava.version>32.1.3-jre</guava.version>
    <druid.version>1.2.23</druid.version>
    <fastjson2.version>2.0.43</fastjson2.version>
    <postgresql.version>42.7.7</postgresql.version>
</properties>
```

**优化成果**：
- ✅ 版本统一管理，易于升级
- ✅ 消除重复依赖
- ✅ 依赖传递关系清晰
- ✅ Druid 版本统一为 1.2.23

---

## 🏢 多租户架构 (Multi-Tenant Architecture)

### 概述
系统采用 **PostgreSQL Schema 隔离** 方案实现 SaaS 多租户架构，每个租户（学校/机构）拥有独立的数据库 Schema，实现数据完全隔离。

### 双层管理架构 🆕

系统支持**平台管理员**和**租户管理员**两级管理：

#### 1. 平台管理层 (Public Schema)
- **数据存储**: `public` schema
- **管理对象**: 所有租户、平台级资源
- **访问权限**: 平台管理员（Platform Admin）
- **核心功能**:
  - 租户管理（创建、启用、禁用、删除）
  - 平台项目库管理（供租户分配）
  - 平台用户管理
  - 平台字典管理（所有租户共享）
  - 平台日志管理

#### 2. 租户管理层 (Tenant Schema)
- **数据存储**: 租户独立 schema（如 `school_001`）
- **管理对象**: 租户内部资源
- **访问权限**: 租户管理员（Tenant Admin）
- **核心功能**:
  - 租户用户管理
  - 租户角色管理
  - 租户菜单管理
  - 租户项目管理（从平台分配）
  - 租户日志管理

### 核心特性

#### 1. Schema 自动隔离
- 每个租户自动分配独立 Schema（如 `school_001`, `school_002`）
- 租户数据完全隔离，互不干扰
- 支持租户独立备份和迁移

#### 2. @PublicSchema 注解系统
系统实现了智能的 Schema 路由机制：

```java
// 实体类级别：字典数据所有租户共享
@PublicSchema(reason = "字典数据所有租户共享")
@MyTable("sys_dict_type")
public class SysDictType { }

// 方法级别：平台管理员查询
@PublicSchema(reason = "平台管理员登录")
public SysUser findPlatformUserByUsername(String username) { }
```

**工作原理**：
- 自动拦截标记了 `@PublicSchema` 的类/方法
- 动态设置 PostgreSQL 的 `search_path`
- 支持嵌套调用（通过引用计数器）
- 优先级：`PublicSchemaContext` > `TenantContext` > `public`

#### 3. 租户生命周期管理

**创建流程**：
1. 在 `public.sys_tenant` 表创建租户记录
2. 自动创建租户 Schema（如 `school_001`）
3. 执行 DDL 脚本创建所有业务表
4. 插入基础数据（角色、菜单等）
5. 创建租户管理员账号
6. 记录初始化日志

**状态管理**：
- 待激活(0)：已创建但未激活
- 正常(1)：正常运行中
- 已禁用(2)：被管理员禁用
- 已过期(3)：订阅已过期

#### 4. 字典数据共享机制

所有租户共享 `public schema` 的字典数据：

| 字典类型 | 说明 | 示例 |
|---------|------|------|
| 租户管理字典 | 平台管理使用 | tenant_status, tenant_init_step |
| 通用业务字典 | 业务功能使用 | user_gender, student_status, course_status |

**开发者使用**：
```java
// 查询字典 - 自动路由到 public schema
List<SysDictType> types = dictTypeService.getAll();
List<SysDictData> data = dictDataService.getByType("user_gender");
```

### 数据库 Schema 结构 🆕

#### Public Schema（平台管理）
```sql
-- RBAC 权限表
public.sys_user          -- 平台管理员用户
public.sys_role          -- 平台角色
public.sys_menu          -- 平台菜单
public.sys_role_menu     -- 角色-菜单关联
public.sys_user_role     -- 用户-角色关联

-- 租户管理表
public.sys_tenant        -- 租户信息
public.sys_tenant_init_log -- 租户初始化日志

-- 共享数据表
public.sys_dict_type     -- 字典类型（所有租户共享）
public.sys_dict_data     -- 字典数据（所有租户共享）

-- 业务表
public.seer_project_info -- 平台项目库

-- 日志表
public.sys_operation_log -- 平台操作日志
```

#### Tenant Schema（租户数据）
```sql
-- RBAC 权限表
{tenant_schema}.sys_user          -- 租户用户
{tenant_schema}.sys_role          -- 租户角色
{tenant_schema}.sys_menu          -- 租户菜单
{tenant_schema}.sys_role_menu     -- 角色-菜单关联
{tenant_schema}.sys_user_role     -- 用户-角色关联

-- 组织架构表
{tenant_schema}.sys_organization  -- 组织架构

-- 业务表
{tenant_schema}.seer_project_info -- 租户项目（从平台分配）

-- 日志表
{tenant_schema}.sys_operation_log -- 租户操作日志
```

### 租户隔离效果

```
平台管理层 (public):
- 用户: platform_admin、admin（平台管理员）
- 功能: 管理所有租户、平台项目库、字典
- 数据: 跨租户统计、全局配置

租户A (school_001):
- 用户: 张老师、李同学
- 功能: 租户内部管理、项目使用
- 数据: 完全独立

租户B (school_002):
- 用户: 王校长、陈教练
- 功能: 租户内部管理、项目使用
- 数据: 完全独立

共享资源 (public):
- 字典: 性别、课程状态等
- 配置: 系统参数
```

### 相关文档
- [租户实施计划](./tenant-implementation-plan.md)
- [租户创建流程时序](相关流程类文档/租户创建流程时序.md) 🆕
- [@PublicSchema 使用指南](./public-schema-annotation-guide.md)
- [多租户字典解决方案](./multi-tenant-dictionary-solution.md)

---

## 🔐 双层 RBAC 权限模型

### 平台管理员权限（Public Schema）🆕

#### 平台菜单结构
```
平台管理 (目录)
├── 租户管理
│   ├── 查看租户
│   ├── 创建租户
│   ├── 更新租户
│   ├── 禁用租户
│   ├── 启用租户
│   └── 初始化租户
├── 平台项目库
│   ├── 查看项目
│   ├── 创建项目
│   ├── 更新项目
│   └── 删除项目
├── 平台用户
│   ├── 查看用户
│   ├── 创建用户
│   ├── 更新用户
│   ├── 删除用户
│   └── 重置密码
├── 平台字典
│   ├── 查看字典
│   ├── 创建字典
│   ├── 更新字典
│   └── 删除字典
└── 操作日志
    ├── 查看日志
    └── 导出日志
```

#### 平台角色
| 角色 | 说明 | 权限 |
|------|------|------|
| 平台超级管理员 | 拥有所有权限 | 所有平台管理权限 |
| 租户运营 | 管理租户 | 租户管理 + 日志查看 |
| 平台只读 | 只读权限 | 所有模块只读 |

#### 平台管理员账号 🆕
| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| platform_admin | Platform@2025 | 超级管理员 | admin_flag=1，绕过权限检查 |
| admin | Admin@2025 | 平台超级管理员 | 通过角色获得所有权限 |
| tenant_operator | Operator@2025 | 租户运营 | 只能管理租户和查看日志 |
| platform_readonly | Readonly@2025 | 平台只读 | 所有模块只读 |

### 租户管理员权限（Tenant Schema）

#### 租户菜单结构
```
系统管理
├── 用户管理
├── 角色管理
├── 菜单管理
└── 组织管理

业务管理
├── 项目管理（从平台分配）
└── 其他业务功能
```

### 权限控制流程

```
用户登录 → 判断登录类型 → 设置上下文 → 查询权限 → 前后端验证

平台管理员：tenantCode 为空 → 查询 public.sys_user → TenantContext 为空
租户用户：tenantCode 有值 → 查询 tenant.sys_user → 设置 TenantContext
```

### 核心表关系

#### 平台管理（Public Schema）
```sql
public.sys_user (平台用户)
    ↓ (N:M)
public.sys_user_role (用户角色关联)
    ↓
public.sys_role (平台角色)
    ↓ (N:M)
public.sys_role_menu (角色菜单关联)
    ↓
public.sys_menu (平台菜单)
```

#### 租户管理（Tenant Schema）
```sql
{tenant}.sys_user (租户用户)
    ↓ (N:M)
{tenant}.sys_user_role (用户角色关联)
    ↓
{tenant}.sys_role (租户角色)
    ↓ (N:M)
{tenant}.sys_role_menu (角色菜单关联)
    ↓
{tenant}.sys_menu (租户菜单)
```

### 特殊权限

#### 超级管理员 (admin_flag=1)
- 绕过所有权限检查
- 拥有系统所有功能访问权限
- 用于系统初始化和紧急维护

#### 数据权限(未来扩展)
- 本人数据权限
- 本部门数据权限
- 本部门及子部门数据权限
- 全部数据权限

---

## 🏗️ 核心模块说明

### 0. 平台管理模块 (Platform Management) 🆕

#### 0.1 租户管理 (Tenant Management)
**模块路径**: `/platform/tenant`
**存储位置**: `public` schema
**核心功能**:
- 租户（学校/机构）的创建、查询、更新、删除
- 自动 Schema 初始化和数据隔离
- 租户状态管理（激活/禁用/过期）
- 租户初始化日志查询

**权限点**:
- `tenant:view` - 查看租户列表
- `tenant:create` - 创建新租户
- `tenant:update` - 编辑租户信息
- `tenant:disable` - 禁用租户
- `tenant:enable` - 启用租户
- `tenant:init` - 初始化租户

**业务场景**:
- 平台管理员创建新学校租户
- 查看租户订阅到期时间
- 禁用违规租户
- 排查租户初始化失败原因

**自动化流程**:
```java
// 创建租户 - 自动完成 Schema 初始化
tenantService.createTenant(
    tenantCode: "SCHOOL001",
    tenantName: "阳光健身学院",
    schemaName: "school_001",
    adminUsername: "admin",
    adminPassword: "Aa123456!"
);

// 系统自动执行：
// 1. 创建 school_001 Schema
// 2. 创建所有业务表
// 3. 插入角色、菜单基础数据
// 4. 创建管理员账号
// 5. 记录初始化日志
```

---

#### 0.2 平台项目管理 (Platform Project) 🆕
**模块路径**: `/platform/project`
**模块位置**: `seer-fitness-business`
**存储位置**: `public.seer_project_info`
**核心功能**:
- 平台级项目库管理（体育项目：跳绳、跑步等）
- 项目创建、编辑、删除
- 项目分配给租户使用
- 项目类型、评分规则管理

**权限点**:
- `platform:project:view` - 查看平台项目
- `platform:project:create` - 创建项目
- `platform:project:update` - 更新项目
- `platform:project:delete` - 删除项目

**业务场景**:
- 平台管理员创建"跳绳"项目
- 设置项目评分标准
- 将项目分配给多个租户使用

---

#### 0.3 平台用户管理 (Platform User) 🆕
**模块路径**: `/platform/user`
**存储位置**: `public.sys_user`
**核心功能**:
- 平台管理员账号管理
- 用户创建、编辑、删除
- 密码重置
- 角色分配

**权限点**:
- `platform:user:view` - 查看平台用户
- `platform:user:create` - 创建用户
- `platform:user:update` - 更新用户
- `platform:user:delete` - 删除用户
- `platform:user:reset-password` - 重置密码

---

#### 0.4 平台字典管理 (Platform Dictionary) 🆕
**模块路径**: `/platform/dict`
**存储位置**: `public.sys_dict_type/data`
**核心功能**:
- 字典类型和数据管理
- 所有租户共享字典
- 字典项排序和状态管理

**权限点**:
- `platform:dict:view` - 查看字典
- `platform:dict:create` - 创建字典
- `platform:dict:update` - 更新字典
- `platform:dict:delete` - 删除字典

---

#### 0.5 平台菜单管理 (Platform Menu Management) 🆕
**模块路径**: `/platform/menu`
**存储位置**: `public.sys_menu`
**核心功能**:
- 平台菜单模板的统一管理
- 菜单分类：平台专用菜单(menu_type=1) 和 租户模板菜单(menu_type=2)
- 菜单版本控制：基础版(feature_level=1)、标准版(feature_level=2)、企业版(feature_level=3)
- 菜单分配给租户使用
- 菜单修改自动同步到已分配租户

**权限点**:
- `platform:menu:view` - 查看平台菜单
- `platform:menu:create` - 创建菜单
- `platform:menu:update` - 更新菜单
- `platform:menu:delete` - 删除菜单
- `tenant:assign-menu` - 分配菜单给租户

**业务场景**:
- 平台管理员创建"用户管理"菜单模板
- 将菜单分配给租户A、租户B使用
- 修改菜单权限字符，自动同步到所有已分配租户
- 按订阅版本分配不同功能级别的菜单

**架构说明**:

**新架构（2025-10-17 更新）**：
```
平台管理员创建菜单模板 (public.sys_menu)
        ↓
通过分配接口分配给租户 (sys_tenant_menu 记录)
        ↓
菜单数据复制到租户 schema ({tenant}.sys_menu)
        ↓
租户用户使用菜单（只读）
```

**核心特点**：
1. **统一管理**：所有菜单由平台统一创建和维护
2. **数据复制**：分配时复制到租户schema，保持开发者编码透明性
3. **自动同步**：平台菜单更新后自动同步到已分配租户
4. **版本控制**：通过 platform_menu_id 关联平台模板

**与旧架构对比**：
| 特性 | 旧架构 | 新架构 |
|------|--------|--------|
| 菜单创建 | 租户自行创建 | 平台统一创建 |
| 数据存储 | 仅租户schema | 平台 + 租户（复制） |
| 菜单修改 | 租户可修改 | 租户只读，平台修改 |
| 同步机制 | 无 | 自动同步到租户 |
| 适用场景 | 私有化部署 | SaaS多租户 |

**API 接口**：
```bash
# 平台菜单管理
GET  /platform/menu/tree                    # 获取完整菜单树
GET  /platform/menu/tree/platform           # 获取平台专用菜单树
GET  /platform/menu/tree/tenant-template    # 获取租户模板菜单树
POST /platform/menu/create                  # 创建菜单
POST /platform/menu/update                  # 更新菜单（自动同步）
DELETE /platform/menu/{id}                  # 删除菜单

# 菜单分配
POST /platform/tenant/menu/assign           # 分配单个菜单
POST /platform/tenant/menu/assign-batch     # 批量分配菜单
POST /platform/tenant/menu/unassign         # 取消分配
GET  /platform/tenant/menu/assigned-ids/{tenantId}    # 已分配菜单ID
GET  /platform/tenant/menu/assigned-menus/{tenantId}  # 已分配菜单详情
```

**技术实现**：
```java
// 平台菜单实体
@PublicSchema(reason = "平台菜单管理")
public class SysMenu {
    private Long id;
    private String menuName;
    private Integer menuType;      // 1-平台菜单 2-租户模板菜单
    private Integer featureLevel;  // 1-基础版 2-标准版 3-企业版
    // ...
}

// 租户菜单实体
public class SysMenu {
    private Long platformMenuId;   // 关联平台菜单ID（用于同步）
    // ...其他字段与平台菜单一致
}

// 分配菜单
@Transactional
public void assignMenu(Long tenantId, Long platformMenuId, Long currentUserId) {
    // 1. 验证租户和菜单
    // 2. 复制菜单到租户schema
    // 3. 记录分配关系（sys_tenant_menu）
}

// 同步菜单更新
@Transactional
public void syncMenuToTenants(SysMenu platformMenu) {
    // 查询已分配该菜单的所有租户
    // 逐个更新租户schema中的菜单数据
}
```

---

#### 0.6 平台角色管理 (Platform Role Management) 🆕
**模块路径**: `/platform/role`
**存储位置**: `public.sys_role`
**核心功能**:
- 平台角色模板的统一管理
- 角色分类：平台专用角色(role_type=1) 和 租户模板角色(role_type=2)
- 角色版本控制：基础版(feature_level=1)、标准版(feature_level=2)、企业版(feature_level=3)
- 角色分配给租户使用
- 角色修改自动同步到已分配租户
- 为角色配置菜单权限

**权限点**:
- `platform:role:view` - 查看平台角色
- `platform:role:create` - 创建角色
- `platform:role:update` - 更新角色
- `platform:role:delete` - 删除角色
- `platform:role:assign` - 配置角色菜单权限
- `platform:tenant:role:sync` - 同步角色给租户

**业务场景**:
- 平台管理员创建"教师"角色模板
- 将角色分配给租户A、租户B使用
- 修改角色权限，自动同步到所有已分配租户
- 按订阅版本分配不同功能级别的角色

**架构说明**:

**新架构（2025-10-18 更新）**：
```
平台管理员创建角色模板 (public.sys_role)
        ↓
通过同步接口同步给租户 (sys_tenant_role 记录)
        ↓
角色数据复制到租户 schema ({tenant}.sys_role)
        ↓
租户用户使用角色（只读）
```

**核心特点**：
1. **统一管理**：所有角色由平台统一创建和维护
2. **数据复制**：同步时复制到租户schema，保持开发者编码透明性
3. **自动同步**：平台角色更新后自动同步到已分配租户
4. **版本控制**：通过 platform_role_id 关联平台模板
5. **权限级联**：角色-菜单关联关系同步复制

**与旧架构对比**：
| 特性 | 旧架构 | 新架构 |
|------|--------|--------|
| 角色创建 | 租户自行创建 | 平台统一创建 |
| 数据存储 | 仅租户schema | 平台 + 租户（复制） |
| 角色修改 | 租户可修改 | 租户只读，平台修改 |
| 同步机制 | 无 | 自动同步到租户 |
| 适用场景 | 私有化部署 | SaaS多租户 |

**API 接口**：
```bash
# 平台角色管理
POST /platform/role/search              # 分页查询角色
GET  /platform/role/list                # 获取角色列表
GET  /platform/role/list/{roleType}     # 按类型获取角色列表
GET  /platform/role/{id}                # 获取角色详情
POST /platform/role/create              # 创建角色
POST /platform/role/update              # 更新角色（自动同步）
DELETE /platform/role/{id}              # 删除角色
POST /platform/role/{id}/assign-menus   # 配置角色菜单权限
GET  /platform/role/{id}/menus          # 获取角色菜单权限

# 角色同步
POST /platform/tenant/role/sync         # 同步单个角色到租户
POST /platform/tenant/role/sync/batch   # 批量同步角色
POST /platform/tenant/role/sync/all     # 同步角色到所有租户
```

**技术实现**：
```java
// 平台角色实体
@PublicSchema(reason = "平台角色管理")
public class SysRole {
    private Long id;
    private String roleName;
    private Integer roleType;      // 1-平台角色 2-租户模板角色
    private Integer featureLevel;  // 1-基础版 2-标准版 3-企业版
    // ...
}

// 租户角色实体
public class SysRole {
    private Long platformRoleId;   // 关联平台角色ID（用于同步和只读保护）
    // ...其他字段与平台角色一致
}

// 同步角色
@Transactional
public void syncRole(Long tenantId, Long platformRoleId, Long currentUserId) {
    // 1. 验证租户和角色
    // 2. 复制角色到租户schema
    // 3. 同步角色-菜单关联关系
    // 4. 记录同步关系（sys_tenant_role）
}

// 同步角色更新
@Transactional
public void syncRoleToTenants(SysRole platformRole) {
    // 查询已分配该角色的所有租户
    // 逐个更新租户schema中的角色数据
}

// 租户侧只读保护
@Service
public class RoleService {
    public void update(RoleUpdateRequest request) {
        // 检查是否为平台同步的角色
        if (isPlatformSyncedRole(request.getId())) {
            throw new BusinessException("平台同步的角色不能修改，请联系平台管理员");
        }
        // 允许更新自建角色
    }
}
```

**租户创建时自动同步** 🆕：
```java
// 新租户创建时自动同步角色和菜单模板
TenantSchemaService.createSchemaAndInitTables() {
    // Step 1: CREATE_SCHEMA
    // Step 2: CREATE_TABLE
    // Step 3: CREATE_ADMIN
    // Step 4: SYNC_TEMPLATES (自动同步，根据 feature_level 筛选)
    autoSyncTemplates(tenantId, featureLevel);
}

// 根据租户功能级别筛选模板
TenantTemplateAutoSyncService.autoSyncRoleTemplates() {
    // 查询: WHERE role_type = 2 AND feature_level <= tenantFeatureLevel
    // 基础版租户(1): 仅获得 feature_level = 1 的角色
    // 标准版租户(2): 获得 feature_level <= 2 的角色
    // 企业版租户(3): 获得所有角色模板
}
```

---

### 1. 系统管理模块 (System Management)

#### 1.1 用户管理 (User Management)
**模块路径**: `/system/user`
**模块位置**: `seer-fitness-system`
**核心功能**:
- 管理系统中的所有用户账号(运维人员、教师、学生)
- 支持用户类型区分: `user_type` (0-运维 1-教师 2-学生)
- 用户的增删改查操作
- 密码初始化和重置功能
- 用户状态管理(启用/禁用)

**权限点**:
- `user:view` - 查看用户列表和详情
- `user:create` - 创建新用户
- `user:update` - 编辑用户信息
- `user:delete` - 删除用户(逻辑删除)
- `user:init-password` - 初始化用户密码
- `user:reset-password` - 重置用户密码

**业务场景**:
- 运维人员创建教师账号
- 教师创建学生账号
- 忘记密码时的重置操作

---

#### 1.2 角色管理 (Role Management)
**模块路径**: `/system/role`
**模块位置**: `seer-fitness-system`
**核心功能**:
- 定义系统中的角色(如:系统管理员、教师、学生等)
- 角色的增删改查
- 为角色分配菜单权限

**权限点**:
- `role:view` - 查看角色列表
- `role:create` - 创建新角色
- `role:update` - 编辑角色信息
- `role:delete` - 删除角色
- `role:assign` - 为角色分配菜单权限

**业务场景**:
- 创建"教学主管"角色,分配课程管理权限
- 创建"财务人员"角色,分配收费管理权限

---

#### 1.3 菜单管理 (Menu Management) 🔄
**模块路径**: `/system/menu`
**模块位置**: `seer-fitness-system`
**核心功能**:
- 查询租户已分配的菜单树结构
- 查询当前用户的菜单权限
- 支持三种类型: 目录(0)、菜单(1)、按钮(2)
- 前端根据菜单配置渲染路由和权限

**⚠️ 重要变更（2025-10-17）**:
- **租户不能创建、修改、删除菜单**
- **所有菜单由平台管理员创建并分配**
- **租户只能查看和使用已分配的菜单**
- **菜单增删改接口已移除**

**权限点**:
- `menu:view` - 查看菜单树（仅查询权限）

**业务场景**:
- 租户用户登录后查看自己有权限的菜单
- 前端根据菜单权限渲染导航栏
- 角色管理时查看可分配的菜单列表

**API 接口**:
```bash
GET /system/menu/tree           # 获取租户菜单树
GET /system/menu/user-menus     # 获取当前用户菜单
GET /system/menu/list           # 获取菜单列表（扁平）
GET /system/menu/{id}           # 获取菜单详情
```

**如需修改菜单**:
请联系平台管理员，通过以下接口操作：
- `/platform/menu/create` - 创建新菜单模板
- `/platform/menu/update` - 更新菜单模板
- `/platform/tenant/menu/assign` - 分配菜单给租户

**架构优势**:
- ✅ 统一管理，避免租户菜单混乱
- ✅ 版本控制，支持分级订阅
- ✅ 自动同步，菜单更新无需租户操作
- ✅ 权限清晰，职责分离

---

#### 1.4 组织管理 (Organization Management)
**模块路径**: `/system/organization`
**模块位置**: `seer-fitness-system`
**核心功能**:
- 管理公司/学校的组织架构(部门、分校等)
- 支持树形层级结构
- 配置组织负责人、联系方式、地址等
- 组织启用/禁用管理

**权限点**:
- `organization:view` - 查看组织架构树
- `organization:create` - 创建部门/分校
- `organization:update` - 编辑组织信息
- `organization:delete` - 删除组织节点

**业务场景**:
- 健身连锁机构管理多个分店
- 按部门划分管理权限
- 数据权限隔离(用户只能看自己部门的数据)

---

### 2. 业务管理模块 (Business Management) 🆕

#### 2.1 租户项目管理 (Tenant Project)
**模块路径**: `/tenant/project`
**模块位置**: `seer-fitness-business`
**存储位置**: `{tenant}.seer_project_info`
**核心功能**:
- 租户使用平台分配的项目
- 项目查询和使用
- 项目数据统计

**权限点**:
- `tenant:project:view` - 查看租户项目
- `tenant:project:use` - 使用项目

---

### 3. 日志管理模块 (Log Management)

#### 3.1 操作日志 (Operation Log)
**模块路径**: `/log/operation`
**模块位置**: `seer-fitness-system`
**核心功能**:
- 记录所有用户的操作行为
- 支持日志类型: CREATE/UPDATE/DELETE/QUERY/LOGIN/LOGOUT
- 记录请求参数、响应结果、IP地址、执行耗时
- 操作成功/失败状态跟踪
- 日志导出功能(Excel/CSV)

**权限点**:
- `operation_log:view` - 查看操作日志
- `operation_log:delete` - 删除历史日志
- `operation_log:export` - 导出日志数据

**业务场景**:
- 审计员查看系统操作记录
- 排查问题时追溯操作历史
- 定期导出日志进行归档
- 安全事件分析

**记录内容**:
```json
{
  "user": "张老师",
  "operation": "UPDATE",
  "module": "user",
  "desc": "修改学生李明的手机号",
  "ip": "192.168.1.100",
  "time": "2025-01-09 14:30:25",
  "duration": "125ms",
  "result": "成功"
}
```

---

### 4. 数据字典模块 (Data Dictionary) 🔄

> **重要提示**：字典数据存储在 `public schema`，所有租户共享，通过 `@PublicSchema` 注解自动路由。

#### 4.1 字典类型管理 (Dictionary Type)
**模块路径**: `/dict/type`
**模块位置**: `seer-fitness-system`
**存储位置**: `public.sys_dict_type`
**核心功能**:
- 管理字典分类（如：性别、学历、课程类型等）
- 字典类型的增删改查
- 字典类型编码唯一性控制
- **所有租户共享字典类型**

**权限点**:
- `dict:type:view` - 查看字典类型
- `dict:type:create` - 创建字典类型
- `dict:type:update` - 编辑字典类型
- `dict:type:delete` - 删除字典类型

**预置字典类型**:

| 分类 | 类型编码 | 类型名称 | 说明 |
|------|---------|---------|------|
| 租户管理 | tenant_status | 租户状态 | 待激活/正常/已禁用/已过期 |
| 租户管理 | tenant_init_step | 初始化步骤 | Schema创建、表创建等 |
| 租户管理 | tenant_log_status | 日志状态 | 进行中/成功/失败 |
| 通用业务 | user_gender | 用户性别 | 男/女/未知 |
| 通用业务 | user_status | 用户状态 | 启用/禁用 |
| 通用业务 | student_status | 学员状态 | 在读/休学/毕业/退学 |
| 通用业务 | course_status | 课程状态 | 草稿/已发布/已下架 |
| 通用业务 | payment_status | 缴费状态 | 待缴费/已缴费/部分缴费/已退费 |
| 通用业务 | yes_no | 是否标识 | 是/否 |
| 通用业务 | enable_status | 启用状态 | 启用/禁用 |

---

#### 4.2 字典数据管理 (Dictionary Data)
**模块路径**: `/dict/data`
**模块位置**: `seer-fitness-system`
**存储位置**: `public.sys_dict_data`
**核心功能**:
- 管理字典类型下的具体数据项
- 字典值的增删改查
- 字典排序和状态管理
- **所有租户共享字典数据**

**权限点**:
- `dict:data:view` - 查看字典数据
- `dict:data:create` - 创建字典数据
- `dict:data:update` - 编辑字典数据
- `dict:data:delete` - 删除字典数据

**示例数据**:
| 字典类型 | 字典标签 | 字典值 | 样式 | 排序 |
|---------|---------|-------|------|-----|
| user_gender | 男 | 1 | - | 1 |
| user_gender | 女 | 2 | - | 2 |
| student_status | 在读 | 1 | success | 1 |
| student_status | 休学 | 2 | warning | 2 |
| student_status | 毕业 | 3 | info | 3 |
| student_status | 退学 | 4 | danger | 4 |

**技术实现**：
```java
// 实体类标记 @PublicSchema - 自动路由到 public schema
@PublicSchema(reason = "字典数据所有租户共享")
@MyTable("sys_dict_type")
public class SysDictType { }

// 开发者使用 - 无需关心路由
List<SysDictData> genders = dictDataService.getByType("user_gender");
```

**业务价值**:
- 前端下拉框数据源统一管理
- 避免硬编码，提高可维护性
- 支持动态配置，无需发版
- **多租户共享，统一标准，便于管理**

---

## 📡 IoT 设备实时数据推送架构

### 概述
系统支持健身设备（跳绳机、跑步机、动感单车等）通过 MQTT 协议实时上报运动数据，前端应用可以订阅设备数据实现毫秒级延迟的实时展示。

### 核心架构设计

#### 数据流向
```
设备 --MQTT--> EMQX Broker --直接订阅--> 前端 (超低延迟，<50ms)
                   │
                   └--$share订阅--> 后端 (业务处理：持久化/统计/告警)
```

**设计思想**：
- ✅ **实时展示数据**：前端直接订阅 EMQX，无需后端转发，延迟最低
- ✅ **业务处理数据**：后端异步订阅，处理持久化、统计、告警等业务逻辑
- ✅ **数据流解耦**：实时展示和业务处理互不影响

### MQTT 主题设计（租户隔离）

#### 设备上报主题
```
device/{tenantId}/{deviceId}/realtime      # 高频实时数据（每次跳动）
device/{tenantId}/{deviceId}/summary       # 中频汇总数据（每10秒）
device/{tenantId}/{deviceId}/event         # 低频事件数据（开始/暂停/结束）
```

**示例**：
- `device/1/rope_001/realtime` - 租户1的跳绳机rope_001的实时数据
- `device/1/treadmill_005/summary` - 租户1的跑步机005的汇总数据

#### 控制指令主题（后端发布，设备订阅）
```
device/{tenantId}/{deviceId}/control       # 设备控制（开始/暂停/重置）
```

#### 聚合数据主题（后端发布，前端订阅）
```
classroom/{tenantId}/{classId}/ranking     # 班级排行榜（每5秒更新）
tenant/{tenantId}/statistics               # 租户统计数据（管理员大屏）
```

### 租户隔离与权限控制

#### EMQX ACL 权限规则（简化版）

**核心原则**：租户只能访问自己租户的数据，不能跨租户访问。

```erlang
# 1. 设备：只能发布自己的主题
{allow, {user, "device_1_rope_001"}, publish, ["device/1/rope_001/#"]}.
{deny, {user, "device_1_rope_001"}, subscribe, ["#"]}.

# 2. 前端用户：只能订阅自己租户的主题
{allow, {user, "tenant_1_user_*"}, subscribe, ["device/1/#"]}.
{allow, {user, "tenant_1_user_*"}, subscribe, ["classroom/1/#"]}.
{allow, {user, "tenant_1_user_*"}, subscribe, ["tenant/1/#"]}.
{deny, {user, "tenant_1_user_*"}, subscribe, ["device/2/#"]}.  # 禁止订阅其他租户

# 3. 后端服务：共享订阅所有租户（负载均衡）
{allow, {user, "backend"}, subscribe, ["$share/backend-group/device/+/+/#"]}.
{allow, {user, "backend"}, publish, ["#"]}.
```

**说明**：
- 设备用户名格式：`device_{tenantId}_{deviceId}`
- 前端用户名格式：`tenant_{tenantId}_user_{userId}`
- 后端用户名：`backend`
- 使用通配符 `#` 和 `+` 简化配置
- ACL 规则由后端动态生成，用户登录时创建临时 MQTT 凭证

---

## 🔧 使用说明

### 1. 系统初始化（多租户版）🆕

#### 1.1 初始化 Public Schema（平台管理 + 共享字典）

```bash
# 1. 创建平台管理 RBAC 表（平台用户、角色、菜单等）
psql -U postgres -d seer_fitness_db -f docs/sql/platform/platform_rbac_tables.sql

# 2. 初始化平台管理员数据（4个初始账号 + 角色权限）
psql -U postgres -d seer_fitness_db -f docs/sql/platform/platform_init_data.sql

# 3. 初始化字典数据（所有租户共享）
psql -U postgres -d seer_fitness_db \
  -f seer-fitness-system/src/main/resources/sql/public_schema_dict_init.sql
```

#### 1.2 创建租户（自动初始化 Schema）

**方式一：通过 API 创建**
```bash
POST /tenant/create
{
  "tenantCode": "SCHOOL001",
  "tenantName": "阳光健身学院",
  "schemaName": "school_001",
  "adminUsername": "admin",
  "adminRealName": "管理员",
  "adminPassword": "Aa123456!",
  "contactPhone": "13800138000",
  "address": "北京市朝阳区XX路XX号"
}
```

**方式二：通过 Service 创建**
```java
@Autowired
private TenantService tenantService;

// 创建租户 - 自动完成 Schema 初始化
tenantService.createTenant(createParam);

// 系统自动执行：
// 1. 创建 school_001 Schema
// 2. 创建用户表、角色表、菜单表等
// 3. 插入基础角色和菜单数据
// 4. 创建租户管理员账号
```

#### 1.3 验证初始化

```sql
-- 1. 查看平台管理员
SELECT id, username, real_name, admin_flag
FROM public.sys_user;

-- 2. 查看租户列表
SELECT tenant_code, tenant_name, schema_name, status
FROM public.sys_tenant;

-- 3. 查看租户 Schema 是否创建成功
SELECT schema_name
FROM information_schema.schemata
WHERE schema_name LIKE 'school_%';

-- 4. 查看租户初始化日志
SELECT * FROM public.sys_tenant_init_log
WHERE tenant_id = 1
ORDER BY created_at DESC;
```

### 2. 登录测试 🆕

#### 2.1 平台管理员登录（访问 public schema）

**登录请求**：
```json
{
  "tenantCode": "",  // 留空或不传
  "username": "platform_admin",
  "password": "Platform@2025",
  "captcha": "1234",
  "captchaId": "xxx"
}
```

**平台管理员账号**：
| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| platform_admin | Platform@2025 | 超级管理员 | admin_flag=1，拥有所有权限 |
| admin | Admin@2025 | 平台超级管理员 | 通过角色获得所有权限 |
| tenant_operator | Operator@2025 | 租户运营 | 只能管理租户和查看日志 |
| platform_readonly | Readonly@2025 | 平台只读 | 所有模块只读权限 |

#### 2.2 租户用户登录（访问租户 schema）

**登录请求**：
```json
{
  "tenantCode": "SCHOOL001",  // 必须提供租户编码
  "username": "admin",
  "password": "Aa123456!",
  "captcha": "1234",
  "captchaId": "xxx"
}
```

**登录流程**：
1. 系统根据 tenantCode 判断登录类型
2. 平台管理员：查询 `public.sys_user`，TenantContext 为空
3. 租户用户：查询 `{tenant}.sys_user`，设置 TenantContext
4. 生成 JWT Token（包含租户信息）
5. 查询角色和权限
6. 返回 Token

### 3. 开发接入

#### 3.1 权限控制
```java
// 后端权限注解示例
@RequireAuth(permission = "user:create")
public Result createUser(UserDTO dto) {
    // 创建用户逻辑 - 自动路由到当前租户的 Schema
}
```

#### 3.2 使用 @PublicSchema 注解
```java
// 查询字典数据 - 自动路由到 public schema
@Service
public class DictDataService {

    public List<DictData> getByType(String dictType) {
        // SysDictData 实体标记了 @PublicSchema
        // 查询自动路由到 public.sys_dict_data
        return baseDao.queryListForSql(sql, params, SysDictData.class);
    }
}

// 平台管理员查询 - 方法级别注解
@Service
public class RoleService {

    @PublicSchema(reason = "平台管理员角色查询")
    public List<RoleDTO> getPlatformUserRoles(Long userId) {
        // 此方法内的所有查询都路由到 public schema
        return baseDao.queryListForSql(sql, params, RoleDTO.class);
    }
}
```

#### 3.3 前端权限指令
```vue
<!-- 按钮级权限控制 -->
<a-button v-if="$auth('user:delete')" @click="deleteUser">
  删除用户
</a-button>

<!-- 租户信息显示 -->
<div>当前租户: {{ $store.state.tenant.tenantName }}</div>
```

---

## 📝 扩展方向

### 已完成功能 ✅
- [x] **多租户支持（SaaS模式）** - PostgreSQL Schema 隔离
- [x] **@PublicSchema 注解系统** - 自动 Schema 路由
- [x] **字典数据共享机制** - 所有租户共享字典
- [x] **租户自动化初始化** - Schema + 表 + 数据 + 管理员
- [x] **RBAC 权限模型** - 基于角色的访问控制
- [x] **操作日志审计** - 完整的操作记录
- [x] **IoT 设备实时推送** - MQTT + EMQX + 租户隔离
- [x] **K8s 无状态后端** - $share 共享订阅 + 负载均衡
- [x] **平台管理员系统** - 双层管理架构（平台 + 租户）🆕
- [x] **模块化架构** - framework/system/business/boot 🆕
- [x] **Maven 依赖优化** - 统一版本管理 + 消除重复依赖 🆕

### 近期规划
- [ ] 完善 IoT 设备管理模块（设备注册、状态监控）
- [ ] 实现设备数据历史查询与分析
- [ ] 添加数据权限（部门数据隔离）
- [ ] 完善用户类型的业务功能（教师、学生模块）
- [ ] 租户订阅和计费管理
- [ ] 租户数据导出/导入功能
- [ ] 增加在线人数统计
- [ ] 登录日志单独管理

### 远期规划
- [ ] 设备异常检测与智能告警（AI 模型）
- [ ] 教室大屏实时排行榜（React/Vue 组件）
- [ ] 租户数据备份和恢复
- [ ] 跨租户数据分析（BI 大屏）
- [ ] API 接口权限管理
- [ ] 动态权限刷新（无需重新登录）
- [ ] 审批流程引擎

---

## 📞 相关文件

### 核心文档
- **系统说明文档**: `docs/README.md` (本文档)
- **租户实施计划**: `docs/tenant-implementation-plan.md`
- **租户创建流程时序**: `docs/租户创建流程时序.md` 🆕
- **@PublicSchema 使用指南**: `docs/public-schema-annotation-guide.md`
- **多租户字典解决方案**: `docs/multi-tenant-dictionary-solution.md`

### SQL 脚本
- **平台 RBAC 表**: `docs/sql/platform/platform_rbac_tables.sql` 🆕
- **平台初始化数据**: `docs/sql/platform/platform_init_data.sql` 🆕
- **字典数据初始化**: `seer-fitness-system/src/main/resources/sql/public_schema_dict_init.sql`
- **租户 Schema 模板**: `seer-fitness-system/src/main/resources/sql/tenant/tenant_schema_template.sql`
- **租户基础数据**: `seer-fitness-system/src/main/resources/sql/tenant/tenant_init_data.sql`

### 核心代码
- **@PublicSchema 注解**: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/annotation/PublicSchema.java`
- **PublicSchemaContext**: `seer-fitness-system/src/main/java/com/seer/fitness/system/tenant/PublicSchemaContext.java`
- **租户上下文管理**: `seer-fitness-system/src/main/java/com/seer/fitness/system/tenant/TenantContext.java`
- **租户服务**: `seer-fitness-system/src/main/java/com/seer/fitness/system/service/TenantService.java`
- **Schema 管理服务**: `seer-fitness-system/src/main/java/com/seer/fitness/system/service/TenantSchemaService.java`
- **认证服务**: `seer-fitness-system/src/main/java/com/seer/fitness/system/service/AuthService.java` 🆕

---

## 🎯 系统亮点

### 1. 完善的多租户架构
- ✅ PostgreSQL Schema 完全隔离
- ✅ 租户自动化创建和初始化
- ✅ 智能 Schema 路由（@PublicSchema 注解）
- ✅ 租户数据完全独立，安全可靠
- ✅ 双层管理架构（平台 + 租户）🆕

### 2. 灵活的权限体系
- ✅ RBAC 角色权限模型
- ✅ 菜单、按钮级权限控制
- ✅ 支持平台管理员和租户管理员 🆕
- ✅ 完整的操作日志审计

### 3. 高效的开发体验
- ✅ 基于注解的声明式编程
- ✅ 自动化 Schema 路由，开发者无需关心
- ✅ 字典数据统一管理和共享
- ✅ 清晰的模块划分和代码结构 🆕
- ✅ 统一的 Maven 依赖管理 🆕

### 4. 企业级特性
- ✅ 密码 BCrypt 加密
- ✅ JWT Token 认证
- ✅ 操作日志完整记录
- ✅ 租户状态和生命周期管理

### 5. IoT 实时推送架构
- ✅ MQTT 协议支持 IoT 设备接入
- ✅ EMQX 消息代理，支持百万级连接
- ✅ 前端直接订阅设备数据，延迟 <50ms
- ✅ 后端 $share 共享订阅，自动负载均衡
- ✅ K8s 友好，后端完全无状态
- ✅ 租户级 MQTT ACL 权限隔离
- ✅ 动态 MQTT 凭证签发，安全可靠

### 6. 模块化架构设计 🆕
- ✅ 清晰的模块划分（framework/system/business/boot）
- ✅ 统一的依赖版本管理
- ✅ 合理的依赖传递关系
- ✅ 易于扩展和维护

---

## 📊 数据统计 🆕

### 平台管理（Public Schema）
- **平台菜单**: 48个初始菜单模板
  - 平台专用菜单 (menu_type=1): 30个 (1个目录 + 5个菜单 + 24个按钮)
  - 租户模板菜单 (menu_type=2): 18个 (系统管理相关菜单)
- **平台角色**: 3个
- **平台管理员**: 4个初始账号
- **平台权限点**: 25个 (新增 5个菜单管理权限)

### 租户管理（Tenant Schema）
- **租户菜单**: 由平台分配，不再通过SQL初始化
- **租户角色**: 租户初始化时创建
- **租户用户**: 每个租户独立管理
- **菜单关联**: platform_menu_id 关联平台模板

### 共享数据（Public Schema）
- **字典类型**: 10+ 种
- **字典数据**: 所有租户共享
- **菜单模板**: 所有租户共享（通过分配机制）

### 核心模块
- **平台管理**: 5个模块（租户、项目、用户、字典、菜单）🆕
- **系统管理**: 4个模块（用户、角色、菜单（只读）、组织）
- **业务管理**: 2个模块（平台项目、租户项目）
- **日志审计**: 1个模块

### 菜单管理架构 🆕
- **管理模式**: 平台统一管理 + 租户分配使用
- **数据存储**: 平台 (public.sys_menu) + 租户 (复制到租户schema)
- **同步机制**: 平台更新自动同步到已分配租户
- **分配记录**: sys_tenant_menu (public schema)

---

**文档版本**: v4.0 (平台管理员 + 模块化架构版)
**最后更新**: 2025-10-17
**作者**: Seer Fitness Team
