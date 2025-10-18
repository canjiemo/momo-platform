# RBAC 模块专业测试计划

**项目**: Seer Fitness Edu (先知智慧体育-校园)
**测试模块**: RBAC (基于角色的访问控制)
**测试日期**: 2025-10-02
**测试人员**: Claude Code

---

## 📋 测试环境

### 环境配置
- **数据库**: PostgreSQL 14+ (localhost:5432/seer_fitness_edu)
- **缓存**: Redis 6+ (localhost:6379)
- **应用**: Spring Boot 3.5.6 (localhost:8080)
- **JDK**: Java 17

### 初始数据
- 1 个用户：admin (密码: Aa123456!)
- 3 个角色：超级管理员、系统管理员、普通用户
- 13 个菜单：1个目录 + 4个菜单 + 8个按钮权限
- 1 个组织：总部(HQ)

---

## 🎯 测试目标

1. 验证用户认证与授权流程完整性
2. 验证 RBAC 权限控制准确性
3. 验证数据一致性和完整性
4. 验证缓存机制有效性
5. 验证系统安全性（越权、绕过攻击）

---

## 📊 测试用例清单

### 一、用户认证与授权 (4个用例)

#### TC-001: 用户登录成功
**前置条件**: 数据库存在 admin 用户
**测试步骤**:
1. POST `/api/auth/login`
   ```json
   {
     "username": "admin",
     "password": "Aa123456!",
     "captchaCode": "mock"
   }
   ```
2. 验证响应包含 token
3. 验证 JWT token 格式正确
4. 验证 Redis 缓存存在 `user:token:{tokenId}`

**期望结果**:
- HTTP 200
- 返回 token 字符串
- Redis 缓存创建成功
- Token 过期时间 24 小时

---

#### TC-002: 登录失败 - 密码错误
**测试步骤**:
1. POST `/api/auth/login` with `password: "wrongpassword"`

**期望结果**:
- HTTP 401
- 返回 "用户名或密码错误"
- 失败次数 +1（账户锁定机制）

---

#### TC-003: 超级管理员权限绕过
**前置条件**: 使用 admin 用户 token
**测试步骤**:
1. GET `/api/user/list` (需要 `user:list` 权限)
2. 验证 `adminFlag = true` 的用户无需检查权限

**期望结果**:
- HTTP 200
- 成功访问，无权限检查

---

#### TC-004: Token 过期验证
**测试步骤**:
1. 使用过期或无效 token 访问受保护接口

**期望结果**:
- HTTP 401
- 返回 "未登录或token已过期"

---

### 二、用户管理 (5个用例)

#### TC-005: 创建普通用户
**前置条件**: admin 用户 token
**测试步骤**:
1. POST `/api/user/create`
   ```json
   {
     "username": "testuser",
     "password": "Test@123",
     "realName": "测试用户",
     "status": true,
     "orgId": 1,
     "roleIds": [3]
   }
   ```
2. 验证数据库插入成功
3. 验证密码 BCrypt 加密
4. 验证角色关联创建

**期望结果**:
- HTTP 200
- sys_user 表新增记录
- sys_user_role 表新增关联
- password 字段为 BCrypt 密文

---

#### TC-006: 查询用户列表（分页）
**测试步骤**:
1. GET `/api/user/list?pageNum=1&pageSize=10`

**期望结果**:
- HTTP 200
- 返回分页数据
- 包含用户角色信息
- ID 字段为字符串格式（FastJSON2 序列化）

---

#### TC-007: 更新用户信息
**测试步骤**:
1. PUT `/api/user/update`
   ```json
   {
     "id": 2,
     "realName": "测试用户（已更新）",
     "roleIds": [2, 3]
   }
   ```

**期望结果**:
- HTTP 200
- sys_user 表更新成功
- sys_user_role 表重新分配

---

#### TC-008: 删除用户（逻辑删除）
**测试步骤**:
1. DELETE `/api/user/delete`
   ```json
   {
     "ids": [2]
   }
   ```

**期望结果**:
- HTTP 200
- delete_flag = 1
- 用户仍存在数据库，但被标记删除

---

#### TC-009: 用户-组织关联
**测试步骤**:
1. 创建用户时设置 orgId = 1
2. 查询用户详情
3. 验证 orgId 正确

**期望结果**:
- 用户正确关联到组织
- 可通过组织查询用户

---

### 三、角色与权限管理 (4个用例)

#### TC-010: 创建角色
**测试步骤**:
1. POST `/api/role/create`
   ```json
   {
     "roleName": "测试角色",
     "description": "用于测试的角色",
     "status": true
   }
   ```

**期望结果**:
- HTTP 200
- sys_role 表新增记录

---

#### TC-011: 角色分配菜单权限
**测试步骤**:
1. POST `/api/role/assign-menus`
   ```json
   {
     "roleId": 4,
     "menuIds": [1, 2, 6, 7]
   }
   ```
2. 验证 sys_role_menu 表关联

**期望结果**:
- HTTP 200
- sys_role_menu 表正确插入关联记录
- 旧关联被删除

---

#### TC-012: 获取角色菜单权限
**测试步骤**:
1. GET `/api/role/menus/{roleId}`

**期望结果**:
- HTTP 200
- 返回该角色拥有的所有菜单 ID 列表

---

#### TC-013: 权限拦截 - 无权限访问
**前置条件**: 使用普通用户 token（仅有基础权限）
**测试步骤**:
1. GET `/api/role/list` (需要 `role:list` 权限)

**期望结果**:
- HTTP 403
- 返回 "权限不足"

---

### 四、菜单管理 (3个用例)

#### TC-014: 创建菜单
**测试步骤**:
1. POST `/api/menu/create`
   ```json
   {
     "parentId": 1,
     "menuName": "测试菜单",
     "type": 1,
     "path": "test",
     "permission": "test:list",
     "sortOrder": 10,
     "status": true
   }
   ```

**期望结果**:
- HTTP 200
- sys_menu 表新增记录
- parentId 正确关联

---

#### TC-015: 获取菜单树
**测试步骤**:
1. GET `/api/menu/tree`

**期望结果**:
- HTTP 200
- 返回树形结构
- 父子关系正确
- 排序正确（按 sort_order）

---

#### TC-016: 菜单循环引用检测
**测试步骤**:
1. 尝试将菜单 A 的 parentId 设置为其子菜单 B

**期望结果**:
- HTTP 400
- 返回 "不能将菜单设为自己的子菜单"

---

### 五、组织管理 (2个用例)

#### TC-017: 创建组织
**测试步骤**:
1. POST `/api/organization/create`
   ```json
   {
     "orgCode": "TEST_DEPT",
     "orgName": "测试部门",
     "parentId": 1,
     "leaderId": 1,
     "sortOrder": 1,
     "status": true
   }
   ```

**期望结果**:
- HTTP 200
- sys_organization 表新增记录
- leaderId 正确关联到用户

---

#### TC-018: 获取组织树
**测试步骤**:
1. GET `/api/organization/tree`

**期望结果**:
- HTTP 200
- 返回组织树形结构
- 包含人员统计、子组织统计

---

### 六、操作日志 (2个用例)

#### TC-019: 操作日志自动记录
**前置条件**: 任意带 `@OperationLog` 注解的接口
**测试步骤**:
1. 执行任意操作（如创建用户）
2. 查询 sys_operation_log 表

**期望结果**:
- 日志自动插入
- 包含：操作人、操作类型、请求参数、响应结果、耗时

---

#### TC-020: 操作日志查询
**测试步骤**:
1. GET `/api/log/list?pageNum=1&pageSize=10&username=admin`

**期望结果**:
- HTTP 200
- 返回分页日志数据
- 支持按用户、时间、模块筛选

---

### 七、账户锁定机制 (2个用例)

#### TC-021: 失败次数统计
**测试步骤**:
1. 连续 5 次使用错误密码登录
2. 检查账户状态

**期望结果**:
- 前 4 次返回密码错误
- 第 5 次返回 "账户已锁定"
- 锁定时间 15 分钟

---

#### TC-022: 账户自动解锁
**测试步骤**:
1. 等待锁定时间过期
2. 再次尝试登录

**期望结果**:
- 锁定过期后可正常登录
- 失败次数重置

---

### 八、Redis 缓存验证 (3个用例)

#### TC-023: Token 缓存验证
**测试步骤**:
1. 登录成功后检查 Redis
   ```bash
   redis-cli KEYS "user:token:*"
   redis-cli GET "user:token:{tokenId}"
   ```

**期望结果**:
- 缓存存在
- TTL = 86400 秒（24小时）
- 数据包含：userId, username, roles, permissions

---

#### TC-024: 字典缓存验证
**测试步骤**:
1. 查询字典数据
2. 检查 Caffeine 本地缓存和 Redis 缓存

**期望结果**:
- L1 缓存（Caffeine）命中
- L2 缓存（Redis）命中
- 缓存失效后重新加载

---

#### TC-025: 缓存失效测试
**测试步骤**:
1. 登出用户
2. 验证 Redis token 被删除
3. 使用旧 token 访问接口

**期望结果**:
- Token 缓存被清除
- 接口返回 401

---

### 九、数据完整性验证 (3个用例)

#### TC-026: RBAC 关联数据一致性
**测试步骤**:
1. 查询所有用户-角色关联
2. 查询所有角色-菜单关联
3. 验证没有孤儿记录

**SQL 验证**:
```sql
-- 检查是否有无效的用户-角色关联
SELECT * FROM sys_user_role ur
LEFT JOIN sys_user u ON ur.user_id = u.id
LEFT JOIN sys_role r ON ur.role_id = r.id
WHERE u.id IS NULL OR r.id IS NULL;

-- 检查是否有无效的角色-菜单关联
SELECT * FROM sys_role_menu rm
LEFT JOIN sys_role r ON rm.role_id = r.id
LEFT JOIN sys_menu m ON rm.menu_id = m.id
WHERE r.id IS NULL OR m.id IS NULL;
```

**期望结果**:
- 所有关联都有效
- 无孤儿记录

---

#### TC-027: 审计字段完整性
**测试步骤**:
1. 检查所有表的审计字段

**SQL 验证**:
```sql
-- 检查主实体表审计字段
SELECT 'sys_user' as table_name, COUNT(*) as total,
       SUM(CASE WHEN created_by IS NULL THEN 1 ELSE 0 END) as missing_created_by,
       SUM(CASE WHEN created_at IS NULL THEN 1 ELSE 0 END) as missing_created_at
FROM sys_user;
```

**期望结果**:
- created_at 字段全部有值
- created_by 字段符合预期

---

#### TC-028: JSON 序列化验证
**测试步骤**:
1. 调用任意返回 ID 的接口
2. 验证 JSON 响应中 ID 为字符串

**期望结果**:
```json
{
  "id": "1234567890123456789",  // 字符串格式
  "username": "admin"
}
```

---

## 🔒 安全测试用例 (4个用例)

#### TC-029: JWT Token 篡改
**测试步骤**:
1. 修改 JWT payload 中的 userId
2. 使用篡改后的 token 访问接口

**期望结果**:
- Token 验证失败
- HTTP 401

---

#### TC-030: SQL 注入测试
**测试步骤**:
1. 在查询参数中注入 SQL 语句
   ```
   GET /api/user/list?username=' OR '1'='1
   ```

**期望结果**:
- 参数被正确转义
- 无 SQL 注入漏洞

---

#### TC-031: 越权访问测试
**测试步骤**:
1. 普通用户尝试删除其他用户
2. 普通用户尝试分配角色

**期望结果**:
- HTTP 403
- 返回 "权限不足"

---

#### TC-032: 批量操作权限验证
**测试步骤**:
1. 尝试批量删除包含 admin 用户的列表

**期望结果**:
- 系统阻止删除超级管理员
- 返回错误提示

---

## 📈 性能测试用例 (2个用例)

#### TC-033: 权限检查性能
**测试步骤**:
1. 并发 100 次请求受保护接口
2. 测量平均响应时间

**期望结果**:
- 平均响应时间 < 100ms
- Redis 缓存命中率 > 95%

---

#### TC-034: 菜单树构建性能
**测试步骤**:
1. 插入 1000 个菜单节点
2. 查询菜单树

**期望结果**:
- 响应时间 < 500ms
- 树形结构正确

---

## 📝 测试报告模板

### 测试执行摘要
- **测试用例总数**: 34
- **通过**:
- **失败**:
- **阻塞**:
- **通过率**:

### 缺陷汇总
| 缺陷ID | 严重级别 | 模块 | 描述 | 状态 |
|--------|---------|------|------|------|
| BUG-001 | 高 | 认证 | ... | 待修复 |

### 测试结论
- 功能完整性:
- 安全性:
- 性能:
- 建议:

---

## 🛠️ 测试工具

- **API 测试**: cURL / Postman / HTTPie
- **数据库验证**: PostgreSQL CLI / DBeaver
- **缓存验证**: Redis CLI
- **性能测试**: JMeter / ab (Apache Bench)

---

## 📌 注意事项

1. 测试前备份数据库
2. 每个测试用例独立执行，避免数据污染
3. 记录所有 API 响应和数据库状态
4. 敏感数据（密码）不记录到日志
5. 测试完成后清理测试数据

---

**文档版本**: v1.0
**最后更新**: 2025-10-02
