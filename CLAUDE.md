# CLAUDE.md

这个文件为Claude Code提供项目指导信息。

---

## 📋 项目概述

**Seer Fitness Edu (先知智慧体育-校园)** 是一个校园健身教育管理系统，采用Spring Boot 3.5.6 + Java 21开发，实现了完整的RBAC权限控制系统。

**技术栈**:
- Spring Boot 3.5.6 + Java 21 + Maven
- PostgreSQL + Redis
- MyJPA 1.0-jdk21 (自定义ORM) + MyMVC
- JWT认证 + BCrypt密码 + Druid连接池 + Undertow服务器

**核心功能**:
- 多租户管理 (tenant_id 模式，单一 public schema)
- 用户管理、角色管理、菜单管理、组织架构
- RBAC权限控制
- JWT + Redis会话管理
- 操作日志审计
- 数据字典管理

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
```

### 数据库初始化

```bash
# 1. 创建数据库
createdb -U postgres seer_fitness_edu

# 2. 执行初始化脚本
psql -U postgres -d seer_fitness_edu -f seer-fitness-boot/src/main/resources/db/pgsql/001_create_tables.sql
psql -U postgres -d seer_fitness_edu -f seer-fitness-boot/src/main/resources/db/pgsql/002_init_data.sql
```

### 默认账号

- **管理员**: admin / 123456

---

## 🏗️ 项目架构

### 模块结构

```
seer-fitness-edu/                    (父POM)
├── seer-fitness-framework/          (框架核心库)
│   ├── 基础实体: SysDictType, SysDictData
│   └── 枚举: AuthMode, LockStrategy, MenuType
│
├── seer-fitness-system/             (系统模块库)
│   ├── 用户、角色、菜单、组织管理
│   ├── JWT认证 + RBAC权限控制
│   ├── Redis缓存 + 操作日志
│   └── 租户管理 (tenant_id 模式)
│
├── seer-fitness-business/           (业务模块)
└── seer-fitness-boot/               (启动模块)
    └── 主类: SeerFitnessEduApplication
```

### 关键技术点

**1. ORM框架**: MyJPA 1.0-jdk21
- 使用 `@MyTable` 注解（非标准JPA `@Table`）
- `BaseServiceImpl` 提供CRUD操作
- 框架自动注入 delete_flag 和 tenant_id 查询条件
- 支持 `TenantIdProvider` 自动注入 tenant_id

**2. 连接池**: Druid
```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource  # 必须指定
```

**3. 多租户配置**:
```yaml
myjpa:
  tenant:
    enabled: true
    column: tenant_id
```

**4. 类型约定**:
- Java: `Integer`（非 `boolean`）
- PostgreSQL: `SMALLINT` → Java `Integer`
- 自增主键: `BIGSERIAL`

---

## 🏢 多租户架构

### 设计模式

**tenant_id 模式**（单一 public schema）

**核心概念**:
- 所有表在同一个 **public schema**
- `tenant_id = NULL` → 平台级数据（平台菜单、平台角色、超管用户）
- `tenant_id = 具体值` → 租户数据
- myjpa `TenantIdProvider` 自动注入查询条件，无需手动过滤

**有 tenant_id 的表**:
- `sys_user`, `sys_role`, `sys_menu`, `sys_role_menu`, `sys_user_role`
- `sys_organization`（NOT NULL）, `sys_operation_log`, `sys_dict_type`, `sys_dict_data`

**无 tenant_id 的表**:
- `sys_tenant`（平台级元数据表）

### TenantIdProvider

```java
// TenantConfig.java
@Bean
public TenantIdProvider tenantIdProvider() {
    return () -> {
        UserCacheInfo user = SecurityContextUtil.getCurrentUser();
        if (user == null) return null;
        if (user.getAdminFlag() != null && user.getAdminFlag() == 1) return null; // 超管看所有
        return user.getTenantId();
    };
}
```

### 租户创建流程

仅需在 `sys_tenant` 插入一条记录，无需创建 Schema 或初始化表。

**API**: `POST /platform/tenant/create`

### 租户登录流程

1. 前端传 `tenantCode` 字段（平台管理员不传）
2. `AuthService` 查询 `sys_tenant` 验证租户状态
3. 按 `(username, tenant_id)` 查询用户
4. JWT 中携带 `tenantId` 和 `tenantCode`
5. `TenantInterceptor` 解析 JWT → 存入 ThreadLocal
6. myjpa `TenantIdProvider` 自动注入 tenant_id 查询条件

---

## 🔐 认证与授权

### 登录流程

```bash
# 1. 获取验证码
curl http://localhost:8070/auth/captcha

# 2. 平台管理员登录（不传tenantCode）
curl -X POST http://localhost:8070/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"Aa123456!","captchaId":"xxx","captcha":"1234"}'

# 3. 租户用户登录（传tenantCode）
curl -X POST http://localhost:8070/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tenant_admin","password":"Aa123456!","tenantCode":"SCHOOL_A","captchaId":"xxx","captcha":"1234"}'

# 4. 使用Token访问API
curl -H "Authorization: Bearer <token>" http://localhost:8070/system/user/search
```

### 权限控制

**认证流程** (`AuthInterceptor`):
1. 从 `Authorization: Bearer <token>` 提取JWT
2. 从Redis获取用户缓存信息 (`user:token:{tokenId}`)
3. 检查白名单
4. 验证 `@RequireAuth` 注解
5. 超级管理员 (adminFlag=1) 绕过权限检查

**权限注解**:
```java
@RequireAuth(login = false)                    // 公开接口
@RequireAuth(login = true)                     // 仅需登录
@RequireAuth(permissions = {"user:create"})    // 需要特定权限
```

**获取当前用户**（重要！）:
```java
import com.seer.fitness.system.util.SecurityContextUtil;
import com.seer.fitness.system.dto.UserCacheInfo;

// ✅ 正确做法
UserCacheInfo currentUser = SecurityContextUtil.getCurrentUser();
Long userId = currentUser.getUserId();
Long tenantId = currentUser.getTenantId(); // 租户ID（平台管理员为null）

// ❌ 错误做法
Long userId = 1L;  // 永远不要硬编码用户ID！
```

---

## 📡 API接口清单

### 认证接口

```
POST /auth/login          # 用户登录 (公开)
GET  /auth/captcha        # 获取验证码 (公开)
```

### 租户管理（平台管理员使用）

```
POST /platform/tenant/search            # 分页查询租户 [tenant:view]
GET  /platform/tenant/{id}              # 租户详情 [tenant:view]
GET  /platform/tenant/code/{code}       # 按编码查询租户 [tenant:view]
POST /platform/tenant/create            # 创建租户 [tenant:create]
POST /platform/tenant/update            # 更新租户 [tenant:update]
POST /platform/tenant/enable/{id}       # 启用租户 [tenant:update]
POST /platform/tenant/disable/{id}      # 禁用租户 [tenant:update]
POST /platform/tenant/delete/{id}       # 删除租户 [tenant:delete]
GET  /platform/tenant/check-code/{code} # 检查编码可用 [tenant:create]
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
GET  /system/menu/list                # 菜单列表 [menu:view]
GET  /system/menu/{id}                # 菜单详情 [menu:view]
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
```

---

## 🔧 开发规范

### 1. 获取当前用户

```java
// ✅ 正确
UserCacheInfo user = SecurityContextUtil.getCurrentUser();
Long userId = user.getUserId();
Long tenantId = user.getTenantId(); // null=平台管理员

// ❌ 错误
Long userId = 1L;  // 永远不要硬编码！
```

### 2. MyJPA 查询 API

```java
// 按ID查询（自动注入 delete_flag 和 tenant_id 条件）
SysUser user = baseDao.queryById(id, SysUser.class);

// 自定义SQL（自动注入条件）
List<SysRole> roles = baseDao.queryListForSql(sql, params, SysRole.class);
SysUser user = baseDao.querySingleForSql(sql, params, SysUser.class);

// 分页查询
Pager<UserDTO> result = baseDao.queryPageForSql(sql, params, pager, UserDTO.class);

// 插入（autoCreateId=true 使用数据库序列）
baseDao.insertPO(entity, true);

// 更新
baseDao.updatePO(entity);

// 逻辑删除
baseDao.delByIds(SysUser.class, String.valueOf(id));
```

### 3. 实体类规范

```java
@Data
@MyTable("sys_user")  // 使用 @MyTable（非 @Table），默认处理 delete_flag
public class SysUser {
    private Long id;
    private Long tenantId;       // 多租户字段，myjpa 自动注入查询条件
    private String username;
    private Integer deleteFlag;  // Integer 非 Boolean
    private LocalDateTime createdAt;
}
```

### 4. 密码管理

- 默认密码: `Aa123456!`
- BCrypt Hash: `$2a$12$S1Fu/0.DthE.9JTvUDwQQeUwLabpWmBeKgebsBT11KrhgBqWr13HS`

### 5. 验证外键引用

```java
// ✅ 正确 - 先验证再设置
if (request.getLeaderId() != null) {
    SysUser leader = baseDao.queryById(request.getLeaderId(), SysUser.class);
    if (leader == null) throw new BusinessException("指定的负责人不存在");
}
```

---

## ⚙️ 配置文件

### application.yml 关键配置

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource  # ⚠️ 必须指定
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/seer_fitness_edu

  data:
    redis:
      host: localhost
      port: 6379
      database: 0

# MyJPA多租户配置
myjpa:
  tenant:
    enabled: true       # 启用自动注入 tenant_id 条件
    column: tenant_id   # 租户字段名

jwt:
  secret: "your-secret-key"
  expiration: 86400000  # 24小时

server:
  port: 8070
```

---

## 🎯 重要提醒

### ⚠️ 必须遵守的规则

1. **永远不要硬编码用户ID** - 使用 `SecurityContextUtil.getCurrentUser()`
2. **验证所有外键引用** - 插入/更新前必须验证关联对象存在
3. **指定Druid DataSource类型** - 必须在配置中指定 `type: com.alibaba.druid.pool.DruidDataSource`
4. **使用@MyTable注解** - 实体类使用 `@MyTable`（非 `@Table`）
5. **不要使用WithDeleteCondition方法** - myjpa 1.0-jdk21 已删除，框架自动注入

### ✅ 最佳实践

1. 所有Controller方法使用 `@RequireAuth` 注解控制权限
2. 使用 `@OperationLog` 记录关键操作
3. Service方法使用 `@Transactional` 控制事务
4. 验证输入参数使用 `@Valid` + Jakarta Validation
5. 抛出业务异常使用 `BusinessException`
6. 使用MCP工具进行Redis和数据库操作

---

## 🔌 MCP工具使用

### Redis操作

```bash
mcp__redis-mcp-server__get "user:token:xxx"
mcp__redis-mcp-server__scan_all_keys
mcp__redis-mcp-server__delete "captcha:xxx"
```

---

## 📝 更新日志

### 2026-02-28: 切换为 tenant_id 多租户模式
- 删除全部 Schema 隔离代码（DynamicDataSource、Flyway多租户、SchemaRoutingAspect等）
- 删除 @PublicSchema 注解及所有使用
- 新增统一SQL脚本（001_create_tables.sql、002_init_data.sql）
- 所有业务表加入 tenant_id 列，实体类加入 tenantId 字段
- TenantService 简化为纯 sys_tenant CRUD
- 新增 TenantIdProvider Bean，myjpa 自动注入 tenant_id
- 升级 myjpa 版本到 1.0-jdk21

### 2025-10-04: 安全修复（历史）
- 修复 UserController.changePassword 硬编码 userId 问题
- 修复 OrganizationService 缺少负责人验证

---

**项目版本**: 1.0.0
**最后更新**: 2026-02-28
**维护者**: Claude (AI助手)
