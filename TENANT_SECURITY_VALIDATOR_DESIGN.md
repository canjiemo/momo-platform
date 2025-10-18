# 🔐 TenantSecurityValidator 设计方案

**创建日期**: 2024-10-18
**作者**: Claude (AI架构师)
**状态**: 实施中

---

## 📊 需求分析

### 真实的安全风险

1. **用户被删除，Token未过期**
   ```
   场景：管理员从租户schema中删除了某个用户
   问题：用户的Token还有24小时有效期
   风险：被删除的用户仍然可以访问系统
   ```

2. **租户被禁用，Token未过期**
   ```
   场景：租户因欠费/违规被禁用
   问题：租户用户的Token还有24小时有效期
   风险：禁用租户的用户仍可以访问
   ```

3. **用户被移出租户**
   ```
   场景：用户从租户A调到租户B
   问题：旧Token仍然指向租户A
   风险：用户可能同时访问两个租户
   ```

4. **Token与TenantContext不一致**
   ```
   场景：理论上不应该发生，但需要验证
   问题：代码Bug或攻击导致不一致
   风险：跨租户访问
   ```

### 性能要求

**TenantSecurityValidator在拦截器中执行，每个请求都会调用！**

- ✅ 必须极致优化性能
- ✅ 不能每次都查数据库
- ✅ 平均响应时间 < 5ms
- ✅ 使用多级缓存

---

## 🏗️ 架构设计

### 整体架构

```
请求
  ↓
TenantInterceptor (order=1)
  ↓ 从JWT提取租户信息
  ↓ 设置TenantContext
  ↓
AuthInterceptor (order=2)
  ↓ 从Redis获取UserCacheInfo
  ↓ 验证权限
  ↓
📌 TenantSecurityValidator ← 在这里插入验证
  ↓ 验证租户访问权限
  ↓
Controller
```

### 验证流程

```java
validateTenantAccess(UserCacheInfo userCacheInfo) {
    // 1. 快速失败：平台管理员跳过
    if (isPlatformAdmin) return;

    // 2. 验证Token租户 vs TenantContext
    if (token.tenantId != context.tenantId) {
        throw SecurityException;
    }

    // 3. 验证租户状态（Redis缓存5分钟）
    if (!isTenantActive(tenantId)) {
        throw BusinessException;
    }

    // 4. 验证用户存在性（Redis缓存5分钟）
    if (!userExistsInTenant(userId, schema)) {
        throw SecurityException;
    }
}
```

---

## 🚀 已完成的修复

### 1. 增强UserCacheInfo ✅

**文件**: `UserCacheInfo.java`

**添加字段**：
```java
// 新增字段
private Long tenantId;       // 租户ID
private String tenantCode;   // 租户编码
private String schemaName;   // Schema名称
```

**新增构造函数**：
```java
// 包含租户信息的完整构造函数
public UserCacheInfo(..., Long tenantId, String tenantCode, String schemaName) {
    // ...
}
```

### 2. 修改AuthService ✅

**文件**: `AuthService.java:171-204`

**修改点**：登录时将租户信息写入UserCacheInfo

```java
// 租户用户：包含租户信息
userCacheInfo = new UserCacheInfo(
    userId, username, realName,
    roles, permissions,
    adminFlag, userType, tokenId,
    tenant.getId(),          // ← 新增
    tenant.getTenantCode(),  // ← 新增
    tenant.getSchemaName()   // ← 新增
);
```

### 3. 重新设计TenantSecurityValidator ✅

**文件**: `TenantSecurityValidator.java:69-136`

**核心方法**：
```java
public void validateTenantAccess(UserCacheInfo userCacheInfo) {
    // 验证逻辑...
}
```

---

## ⚙️ 需要完成的实现

### 高性能缓存方法

#### 1. isTenantActive() - 验证租户状态

```java
/**
 * 验证租户是否活跃（使用Redis缓存）
 * 缓存时间：5分钟
 */
private boolean isTenantActive(Long tenantId) {
    if (tenantId == null) {
        return false;
    }

    // 1. 从Redis缓存获取
    String cacheKey = "tenant:status:" + tenantId;
    try {
        if (redisTemplate != null) {
            Integer status = (Integer) redisTemplate.opsForValue().get(cacheKey);
            if (status != null) {
                return status == 1; // 1=正常
            }
        }
    } catch (Exception e) {
        log.warn("获取租户状态缓存失败: {}", e.getMessage());
    }

    // 2. 查询数据库
    try {
        String sql = "SELECT status FROM public.sys_tenant WHERE id = :tenantId";
        Map<String, Object> params = Maps.newHashMap();
        params.put("tenantId", tenantId);

        Integer status = jdbcTemplate.queryForObject(sql, params, Integer.class);
        boolean isActive = (status != null && status == 1);

        // 3. 写入缓存（5分钟）
        if (redisTemplate != null && status != null) {
            redisTemplate.opsForValue().set(cacheKey, status,
                TENANT_STATUS_CACHE_MINUTES, TimeUnit.MINUTES);
        }

        return isActive;
    } catch (Exception e) {
        log.error("查询租户状态失败: tenantId={}", tenantId, e);
        return false;
    }
}
```

#### 2. isUserExistsInTenant() - 验证用户存在性

```java
/**
 * 验证用户是否存在于租户Schema中（使用Redis缓存）
 * 缓存时间：5分钟
 */
private boolean isUserExistsInTenant(Long userId, String username, String schemaName) {
    if (userId == null || schemaName == null) {
        return false;
    }

    // 1. 从Redis缓存获取
    String cacheKey = String.format("user:exists:%s:%d", schemaName, userId);
    try {
        if (redisTemplate != null) {
            Boolean exists = (Boolean) redisTemplate.opsForValue().get(cacheKey);
            if (exists != null) {
                return exists;
            }
        }
    } catch (Exception e) {
        log.warn("获取用户存在性缓存失败: {}", e.getMessage());
    }

    // 2. 查询租户schema
    try {
        // ⚠️ 注意：使用安全的schema名称验证
        if (!isValidSchemaName(schemaName)) {
            log.error("非法的schema名称: {}", schemaName);
            return false;
        }

        String sql = String.format(
            "SELECT COUNT(*) FROM %s.sys_user WHERE id = :userId AND username = :username AND delete_flag = 0",
            schemaName
        );
        Map<String, Object> params = Maps.newHashMap();
        params.put("userId", userId);
        params.put("username", username);

        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        boolean exists = (count != null && count > 0);

        // 3. 写入缓存（5分钟）
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(cacheKey, exists,
                USER_VALIDATION_CACHE_MINUTES, TimeUnit.MINUTES);
        }

        return exists;
    } catch (Exception e) {
        log.error("验证用户存在性失败: userId={}, schema={}", userId, schemaName, e);
        return false;
    }
}

/**
 * 验证schema名称格式（防止SQL注入）
 */
private boolean isValidSchemaName(String schemaName) {
    return schemaName != null && schemaName.matches("^[a-z][a-z0-9_]{0,62}$");
}
```

#### 3. recordSecurityEvent() - 记录安全事件

```java
/**
 * 记录安全事件
 * 简化版本，只记录必要信息
 */
private void recordSecurityEvent(Long userId, String eventType, String eventDesc) {
    try {
        if (jdbcTemplate != null) {
            String sql = "INSERT INTO public.sys_security_event " +
                        "(user_id, event_type, event_desc, created_at) VALUES " +
                        "(:userId, :eventType, :eventDesc, NOW())";

            Map<String, Object> params = Maps.newHashMap();
            params.put("userId", userId);
            params.put("eventType", eventType);
            params.put("eventDesc", eventDesc != null ? eventDesc.substring(0, Math.min(500, eventDesc.length())) : null);

            jdbcTemplate.update(sql, params);
        }
    } catch (Exception e) {
        log.error("记录安全事件失败", e);
    }
}
```

#### 4. 缓存清理方法

```java
/**
 * 清除用户验证缓存
 * 用于：用户被删除、用户权限变更时
 */
public void clearUserValidationCache(Long userId, String schemaName) {
    if (redisTemplate == null || userId == null || schemaName == null) {
        return;
    }

    try {
        String cacheKey = String.format("user:exists:%s:%d", schemaName, userId);
        redisTemplate.delete(cacheKey);
        log.info("清除用户验证缓存: userId={}, schema={}", userId, schemaName);
    } catch (Exception e) {
        log.warn("清除用户验证缓存失败: {}", e.getMessage());
    }
}

/**
 * 清除租户状态缓存
 * 用于：租户状态变更时（禁用/启用）
 */
public void clearTenantStatusCache(Long tenantId) {
    if (redisTemplate == null || tenantId == null) {
        return;
    }

    try {
        String cacheKey = "tenant:status:" + tenantId;
        redisTemplate.delete(cacheKey);
        log.info("清除租户状态缓存: tenantId={}", tenantId);
    } catch (Exception e) {
        log.warn("清除租户状态缓存失败: {}", e.getMessage());
    }
}
```

---

## 🔗 集成到AuthInterceptor

**文件**: `AuthInterceptor.java`

**修改位置**: `preHandle` 方法的第69行之后

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    // ... 现有代码 ...

    // 4. 验证登录状态
    String token = extractToken(request);
    UserCacheInfo currentUser = authService.getCurrentUser(token);
    if (currentUser == null) {
        throw new AuthenticationException("请先登录");
    }

    // 5. 将当前用户信息存储到请求中
    request.setAttribute("currentUser", currentUser);

    // ✨ 新增：验证租户访问权限
    tenantSecurityValidator.validateTenantAccess(currentUser);

    // 6. 检查是否只需要登录...
    // ... 后续代码 ...
}
```

---

## 📈 性能分析

### 最坏情况（缓存未命中）

```
验证流程：
1. Token vs Context验证    < 0.1ms  （内存对比）
2. 租户状态查询           < 2ms    （Redis未命中，查DB）
3. 用户存在性查询         < 3ms    （Redis未命中，查DB）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
总计                      < 5ms
```

### 最好情况（缓存命中）

```
验证流程：
1. Token vs Context验证    < 0.1ms  （内存对比）
2. 租户状态查询           < 0.5ms  （Redis命中）
3. 用户存在性查询         < 0.5ms  （Redis命中）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
总计                      < 1ms
```

### 缓存命中率预估

- **租户状态缓存**: >99%（租户状态很少变更）
- **用户存在性缓存**: >95%（5分钟内同一用户的重复请求）

---

## 🧪 测试场景

### 场景1：用户被删除

```sql
-- 1. 用户登录成功，获得Token
POST /auth/login → Token (24小时有效)

-- 2. 管理员删除用户
DELETE FROM school_test1.sys_user WHERE id = 10;

-- 3. 清除缓存
tenantSecurityValidator.clearUserValidationCache(10, "school_test1");

-- 4. 用户再次请求（使用旧Token）
GET /api/projects → 403 Forbidden: "User not found in tenant"
```

### 场景2：租户被禁用

```sql
-- 1. 租户用户正常访问
GET /api/projects → 200 OK

-- 2. 管理员禁用租户
UPDATE public.sys_tenant SET status = 2 WHERE id = 1;

-- 3. 清除缓存
tenantSecurityValidator.clearTenantStatusCache(1);

-- 4. 用户再次请求
GET /api/projects → 403 Forbidden: "Tenant is disabled"
```

### 场景3：Token与Context不一致

```
假设（理论上不应该发生）：
- Token中: tenantId=1, schema=school_test1
- TenantContext: tenantId=2, schema=school_test2

结果：
→ 403 Forbidden: "Tenant context mismatch"
→ 记录安全事件到 sys_security_event
```

---

## 📝 待办事项

- [ ] 完成TenantSecurityValidator的辅助方法实现
- [ ] 在AuthInterceptor中集成TenantSecurityValidator
- [ ] 在用户删除/禁用时调用clearUserValidationCache
- [ ] 在租户状态变更时调用clearTenantStatusCache
- [ ] 编写单元测试
- [ ] 编写集成测试
- [ ] 性能测试（压测）
- [ ] 更新安全报告

---

## 🎯 总结

### 核心价值

1. **实时保护**：检测用户被删除、租户被禁用
2. **一致性验证**：确保Token与TenantContext一致
3. **极致性能**：多级缓存，< 5ms响应时间
4. **安全审计**：记录所有异常访问尝试

### 设计亮点

- ✅ 正确理解架构（Schema物理隔离）
- ✅ 高性能设计（多级缓存）
- ✅ 安全优先（记录所有异常）
- ✅ 平台管理员特殊处理
- ✅ 缓存失效机制

---

**文档版本**: v1.0
**最后更新**: 2024-10-18
