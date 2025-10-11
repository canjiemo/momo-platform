# @PublicSchema 注解使用指南

## 📌 概述

`@PublicSchema` 是一个用于多租户架构的路由注解，用于标记需要访问 public schema 的类或方法。

### 核心功能

- **类级别注解**：标记实体类，使其所有数据库操作自动路由到 public schema
- **方法级别注解**：标记 Service/Controller 方法，使该方法内的所有数据库操作路由到 public schema
- **自动路由**：无需手动编写 `SET search_path` 语句，框架自动处理
- **嵌套支持**：支持方法嵌套调用，通过引用计数器正确管理上下文

---

## 🎯 使用场景

### 1. 全局共享数据（推荐：类级别注解）

所有租户共享的数据字典、配置表等：

```java
@PublicSchema(reason = "字典数据所有租户共享")
@Data
@MyTable("sys_dict_type")
public class SysDictType {
    // 字段定义...
}
```

### 2. 跨租户查询（推荐：方法级别注解）

管理员需要查看所有租户的统计数据：

```java
@Service
public class AdminService {

    @PublicSchema(reason = "管理员查看所有租户统计")
    public List<TenantStatDTO> getAllTenantsStats() {
        // 此方法内的所有查询都会路由到 public schema
        return tenantDao.queryAll();
    }
}
```

### 3. 临时切换到 public schema

某个 Controller 方法需要访问 public 数据：

```java
@RestController
@RequestMapping("/admin")
public class AdminController {

    @PublicSchema(reason = "查询全局配置")
    @GetMapping("/global-config")
    public Result getGlobalConfig() {
        // 查询会自动路由到 public schema
        return dictService.getConfig();
    }
}
```

---

## 📖 注解参数说明

### reason (必填，建议填写)

```java
@PublicSchema(reason = "说明为什么需要访问 public schema")
```

- **类型**：String
- **默认值**：`"访问全局共享数据"`
- **作用**：文档说明和代码审计
- **建议**：填写清晰的业务原因，便于后续维护

### propagate (可选)

```java
@PublicSchema(reason = "...", propagate = true)
```

- **类型**：boolean
- **默认值**：`true`
- **作用**：
  - `true`：当前线程的所有后续查询都走 public（推荐）
  - `false`：仅当前方法的直接查询走 public
- **注意**：当前版本 `propagate=false` 仍会传播，未来版本会支持更细粒度控制

---

## 🔧 工作原理

### 架构组成

1. **@PublicSchema 注解**：标记需要路由的类/方法
2. **PublicSchemaContext**：ThreadLocal 上下文管理器
3. **PublicSchemaMethodInterceptor**：AOP 拦截器（方法级别）
4. **PublicSchemaDAOInterceptor**：AOP 拦截器（DAO 级别）
5. **SchemaRoutingAspect**：最终执行 `SET search_path` 的切面

### 路由优先级

```
PublicSchemaContext (方法级/实体级 @PublicSchema)
    ↓
TenantContext (租户上下文)
    ↓
public schema (默认)
```

### 执行流程

```
请求进入
  ↓
TenantInterceptor (设置 TenantContext)
  ↓
@PublicSchema 方法 (PublicSchemaMethodInterceptor.enter())
  ↓
DAO 操作 (PublicSchemaDAOInterceptor 检查实体注解)
  ↓
SchemaRoutingAspect (SET search_path TO <target_schema>)
  ↓
数据库查询
  ↓
PublicSchemaMethodInterceptor.exit()
  ↓
响应返回
```

---

## 💡 最佳实践

### ✅ 推荐做法

#### 1. 实体类使用类级别注解

```java
// ✅ 好：清晰、简洁
@PublicSchema(reason = "字典数据全局共享")
@MyTable("sys_dict_type")
public class SysDictType { }
```

#### 2. 业务方法使用方法级别注解

```java
// ✅ 好：精确控制
@Service
public class ReportService {

    @PublicSchema(reason = "生成跨租户报表")
    public ReportDTO generateCrossTenanReporting() { }
}
```

#### 3. 注解要写清楚原因

```java
// ✅ 好：说明清晰
@PublicSchema(reason = "管理员查看所有租户的订阅状态")

// ❌ 差：原因不明确
@PublicSchema(reason = "查询数据")
```

### ❌ 不推荐做法

#### 1. 不要在 Service 类上使用类级别注解

```java
// ❌ 不推荐：整个 Service 的所有方法都会路由到 public
@PublicSchema(reason = "字典服务")
@Service
public class DictService {
    // 所有方法都会强制路由到 public，可能不符合预期
}
```

**推荐替代方案**：在需要的方法上使用方法级别注解

```java
// ✅ 推荐
@Service
public class DictService {

    @PublicSchema(reason = "查询全局字典")
    public List<DictType> getGlobalDictTypes() { }

    // 其他方法不受影响，正常路由到租户 schema
    public List<DictData> getTenantDictData() { }
}
```

#### 2. 避免过度使用

```java
// ❌ 不必要：如果实体已经标记了 @PublicSchema，方法无需再标记
@PublicSchema(reason = "查询字典")
public List<SysDictType> queryDict() {
    // SysDictType 已经标记了 @PublicSchema，会自动路由
}

// ✅ 推荐：直接调用即可
public List<SysDictType> queryDict() {
    // 自动路由到 public schema
}
```

---

## 🧪 测试示例

### 测试实体类注解

```java
@Test
public void testEntityLevelPublicSchema() {
    // SysDictType 标记了 @PublicSchema
    // 以下查询会自动路由到 public schema
    List<SysDictType> dictTypes = baseDao.queryListForSql(
        "SELECT * FROM sys_dict_type",
        Maps.newHashMap(),
        SysDictType.class
    );

    // 验证查询的是 public schema 的数据
    assertNotNull(dictTypes);
}
```

### 测试方法级别注解

```java
@Service
public class TestService {

    @PublicSchema(reason = "测试方法级别路由")
    public List<Tenant> getAllTenants() {
        // 即使 Tenant 实体没有 @PublicSchema
        // 此方法内的查询也会路由到 public
        return baseDao.queryListForSql(
            "SELECT * FROM sys_tenant",
            Maps.newHashMap(),
            Tenant.class
        );
    }
}
```

### 测试嵌套调用

```java
@Service
public class NestedTestService {

    @PublicSchema(reason = "外层方法")
    public void outerMethod() {
        // PublicSchemaContext.counter = 1
        innerMethod();
        // PublicSchemaContext.counter = 1 (恢复)
    }

    @PublicSchema(reason = "内层方法")
    public void innerMethod() {
        // PublicSchemaContext.counter = 2
        queryData();
        // PublicSchemaContext.counter = 2 (未归零，保持 public)
    }
}
```

---

## 📋 已应用 @PublicSchema 的实体

### 字典表（位于 seer-fitness-framework 模块）

| 实体类 | 表名 | 说明 |
|--------|------|------|
| `SysDictType` | `sys_dict_type` | 字典类型表，所有租户共享 |
| `SysDictData` | `sys_dict_data` | 字典数据表，所有租户共享 |

### 使用示例

```java
// 查询字典类型 - 自动路由到 public schema
List<SysDictType> dictTypes = dictTypeService.getAll();

// 查询字典数据 - 自动路由到 public schema
List<SysDictData> dictData = dictDataService.getByType("user_gender");
```

---

## 🚀 初始化 Public Schema 字典数据

### 方式一：执行 SQL 脚本

```bash
psql -U postgres -d seer_fitness_db -f seer-fitness-system/src/main/resources/sql/public_schema_dict_init.sql
```

### 方式二：使用数据库管理工具

1. 打开 `seer-fitness-system/src/main/resources/sql/public_schema_dict_init.sql`
2. 在数据库管理工具中执行

### 验证初始化结果

```sql
-- 查看字典类型总数
SELECT COUNT(*) FROM public.sys_dict_type;
-- 预期：10 条（3条租户管理 + 7条通用业务）

-- 查看字典数据总数
SELECT COUNT(*) FROM public.sys_dict_data;
-- 预期：约30条

-- 查看所有字典类型
SELECT dict_type, dict_name FROM public.sys_dict_type ORDER BY sort_order;
```

---

## 🔍 故障排查

### 1. 查询结果为空

**症状**：查询字典数据返回空列表

**原因**：
- public schema 的字典数据未初始化
- search_path 路由未生效

**解决方案**：
```sql
-- 检查 public schema 是否有数据
SET search_path TO public;
SELECT COUNT(*) FROM sys_dict_type;

-- 如果为 0，执行初始化脚本
\i seer-fitness-system/src/main/resources/sql/public_schema_dict_init.sql
```

### 2. 注解未生效

**症状**：标记了 @PublicSchema，但仍然查询租户 schema

**检查步骤**：
1. 确认注解包路径正确：`com.seer.fitness.framework.annotation.PublicSchema`
2. 确认 AOP 切面已启用：检查日志是否有 `@PublicSchema method entered`
3. 确认拦截器顺序正确：`SchemaRoutingAspect` 的 Order=10

**调试日志**：
```java
// 在 application.yml 中启用 DEBUG 日志
logging:
  level:
    com.seer.fitness.system.interceptor: DEBUG
    com.seer.fitness.system.config: DEBUG
    com.seer.fitness.system.tenant: DEBUG
```

### 3. 嵌套调用问题

**症状**：嵌套方法调用时路由异常

**检查**：
```java
// 在方法中打印嵌套层级
log.debug("Nested level: {}", PublicSchemaContext.getNestedLevel());
```

**预期行为**：
- 进入第一个 @PublicSchema 方法：level = 1
- 进入嵌套的 @PublicSchema 方法：level = 2
- 退出嵌套方法：level = 1
- 退出第一个方法：level = 0

---

## 📚 相关文档

- [多租户字典解决方案](./multi-tenant-dictionary-solution.md)
- [租户实施计划](./tenant-implementation-plan.md)
- [Public Schema 字典初始化脚本](../seer-fitness-system/src/main/resources/sql/public_schema_dict_init.sql)

---

## 🎓 总结

### 开发者使用指南

1. **查询字典数据**：直接调用 Service，无需关心路由
   ```java
   List<DictData> data = dictDataService.getByType("user_gender");
   ```

2. **创建新的 Public Schema 实体**：添加类级别注解
   ```java
   @PublicSchema(reason = "全局配置表")
   @MyTable("sys_global_config")
   public class SysGlobalConfig { }
   ```

3. **临时跨租户查询**：添加方法级别注解
   ```java
   @PublicSchema(reason = "生成跨租户报表")
   public ReportDTO generateReport() { }
   ```

### 核心优势

- ✅ **自动化**：无需手动编写 `SET search_path`
- ✅ **声明式**：使用注解清晰表达意图
- ✅ **灵活性**：支持类级别和方法级别两种粒度
- ✅ **可维护性**：通过 `reason` 参数记录业务原因
- ✅ **嵌套安全**：通过引用计数器正确处理嵌套调用

---

**最后更新**：2025-01-10
**版本**：1.0.0
**作者**：seer-fitness
