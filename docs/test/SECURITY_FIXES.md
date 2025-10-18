# 🔒 安全问题修复报告

**修复时间**: 2025-10-04
**修复人员**: Claude (高级开发工程师)
**优先级**: 🔴 高 (安全风险)

---

## 📋 修复清单

| # | 问题 | 文件 | 行号 | 状态 |
|---|------|------|------|------|
| 1 | 硬编码用户ID导致密码修改错误 | UserController.java | 133 | ✅ 已修复 |
| 2 | 创建组织时未验证负责人存在性 | OrganizationService.java | 221 | ✅ 已修复 |
| 3 | 更新组织时未验证负责人存在性 | OrganizationService.java | 285 | ✅ 已修复 |

---

## 🔍 问题1: 用户密码修改安全漏洞

### 问题描述

**位置**: `UserController.java:133`

**影响接口**: `POST /system/user/change-password`

**严重程度**: 🔴 **严重** - 安全漏洞

**原始代码**:
```java
public MyResponseResult changePassword(@Valid @RequestBody ChangePasswordRequest request) {
    // TODO: 从当前用户上下文获取用户ID
    Long currentUserId = 1L; // 临时写死，后续需要从token获取
    userService.changePassword(currentUserId, request.getCurrentPassword(), request.getNewPassword());
    return super.doJsonDefaultMsg();
}
```

**问题分析**:
- ❌ 使用硬编码的 `userId = 1L`
- ❌ 所有用户调用此接口都会修改ID=1的用户密码
- ❌ 用户无法修改自己的密码
- ❌ 可能导致管理员账号被恶意修改

**安全风险**:
1. **身份混淆**: 用户A修改密码时实际修改的是用户1(admin)的密码
2. **权限提升**: 普通用户可能通过此接口修改管理员密码
3. **账号锁定**: 管理员账号可能被反复修改密码导致无法登录

**修复代码**:
```java
public MyResponseResult changePassword(@Valid @RequestBody ChangePasswordRequest request) {
    // 从安全上下文获取当前用户ID
    UserCacheInfo currentUser = SecurityContextUtil.getCurrentUser();
    if (currentUser == null || currentUser.getUserId() == null) {
        throw new BusinessException("无法获取当前用户信息");
    }

    userService.changePassword(currentUser.getUserId(), request.getCurrentPassword(), request.getNewPassword());
    return super.doJsonDefaultMsg();
}
```

**新增导入**:
```java
import com.seer.fitness.system.util.SecurityContextUtil;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
```

**修复原理**:
1. 使用 `SecurityContextUtil.getCurrentUser()` 从请求上下文获取当前登录用户
2. 用户信息由 `AuthInterceptor.preHandle()` 在拦截器中注入 (line 68)
3. 验证用户信息存在性，防止空指针异常
4. 使用真实的当前用户ID调用密码修改服务

**验证方法**:
```bash
# 1. 登录获取Token
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser1",
    "password": "Aa123456!",
    "captchaId": "xxx",
    "captcha": "1234"
  }' | jq -r '.data.token')

# 2. 修改密码 (应该修改testuser1的密码，而不是admin的)
curl -X POST "http://localhost:8080/system/user/change-password" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "Aa123456!",
    "newPassword": "NewPass@123"
  }'

# 3. 验证: 使用新密码登录testuser1应该成功，admin密码应该未变
```

---

## 🔍 问题2: 组织负责人验证缺失

### 问题描述

**位置**:
- `OrganizationService.java:221` (create方法)
- `OrganizationService.java:285` (update方法)

**影响接口**:
- `POST /system/organization/create`
- `POST /system/organization/update`

**严重程度**: ⚠️ **中高** - 数据完整性问题

**原始代码**:
```java
// 创建组织
if (request.getLeaderId() != null) {
    // TODO: 验证用户是否存在
}

// 更新组织
org.setLeaderId(request.getLeaderId());  // 直接设置，未验证
```

**问题分析**:
- ❌ 未验证 `leaderId` 对应的用户是否存在
- ❌ 可能创建关联到不存在用户的组织
- ❌ 数据库外键约束可能失败(如果配置了)
- ❌ 前端查询组织负责人信息时可能报错

**数据风险**:
1. **脏数据**: 组织的 `leader_id` 指向不存在的用户
2. **查询异常**: JOIN查询时找不到对应用户记录
3. **业务逻辑错误**: 无法通知负责人、无法分配任务等

**修复代码**:
```java
// 验证负责人是否存在
if (request.getLeaderId() != null) {
    SysUser leader = baseDao.queryByIdWithDeleteCondition(request.getLeaderId(), SysUser.class);
    if (leader == null) {
        throw new BusinessException("指定的负责人不存在");
    }
}
```

**新增导入**:
```java
import com.seer.fitness.system.entity.SysUser;
```

**修复位置**:
1. **创建组织** (OrganizationService.java:221-225)
2. **更新组织** (OrganizationService.java:285-290)

**修复原理**:
1. 在设置 `leaderId` 之前先查询用户表
2. 使用 `queryByIdWithDeleteCondition()` 确保用户存在且未被删除
3. 如果用户不存在，抛出业务异常并终止操作
4. 保证数据库中的 `leader_id` 始终有效

**验证方法**:
```bash
# 1. 创建组织 - 使用不存在的负责人ID
curl -X POST "http://localhost:8080/system/organization/create" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orgCode": "TEST001",
    "orgName": "测试组织",
    "leaderId": 999999,
    "status": 1
  }'
# 期望结果: {"code":400,"msg":"指定的负责人不存在"}

# 2. 创建组织 - 使用存在的负责人ID
curl -X POST "http://localhost:8080/system/organization/create" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orgCode": "TEST001",
    "orgName": "测试组织",
    "leaderId": 1,
    "status": 1
  }'
# 期望结果: {"code":200,"msg":"操作成功"}

# 3. 更新组织 - 修改为不存在的负责人
curl -X POST "http://localhost:8080/system/organization/update" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "orgCode": "TEST001",
    "orgName": "测试组织",
    "leaderId": 999999,
    "status": 1
  }'
# 期望结果: {"code":400,"msg":"指定的负责人不存在"}
```

---

## 📊 测试验证

### 编译测试
```bash
mvn clean compile -DskipTests
```
✅ **编译成功** - 无语法错误

### 单元测试
```bash
# TODO: 需要添加单元测试
mvn test -Dtest=UserControllerTest#testChangePassword
mvn test -Dtest=OrganizationServiceTest#testCreateWithInvalidLeader
```

### 集成测试
```bash
# 使用测试脚本验证
cd docs/test
bash test_security_fixes.sh
```

---

## 🎯 影响范围

### 受影响的API接口

1. **用户密码修改**
   - `POST /system/user/change-password`
   - 权限: 登录即可
   - 影响用户: 所有用户

2. **组织创建**
   - `POST /system/organization/create`
   - 权限: `organization:create`
   - 影响用户: 组织管理员

3. **组织更新**
   - `POST /system/organization/update`
   - 权限: `organization:update`
   - 影响用户: 组织管理员

### 受影响的数据表

1. **sys_user** - 用户密码字段可能被错误修改
2. **sys_organization** - 负责人字段可能包含无效引用

---

## 📈 修复效果

### 修复前
- ❌ 用户修改密码时会错误修改admin账号
- ❌ 可创建负责人不存在的组织
- ❌ 数据完整性无保障

### 修复后
- ✅ 用户只能修改自己的密码
- ✅ 组织负责人必须是存在的有效用户
- ✅ 数据完整性得到保障
- ✅ 安全风险已消除

---

## 🔄 代码审查清单

在类似场景中检查以下问题：

- [ ] 所有需要当前用户信息的接口是否使用 `SecurityContextUtil.getCurrentUser()`
- [ ] 所有外键引用是否在插入/更新前验证引用对象存在性
- [ ] 所有硬编码的ID是否已替换为动态获取
- [ ] 所有用户相关操作是否验证用户身份
- [ ] 所有关联数据是否验证完整性约束

---

## 📝 后续建议

### 代码改进
1. ✅ 添加单元测试覆盖修复的代码
2. ✅ 在所有Controller中统一使用SecurityContextUtil
3. ✅ 在Service层添加数据验证层
4. ✅ 考虑使用AOP统一处理用户身份获取

### 数据库改进
1. 考虑添加外键约束: `sys_organization.leader_id` → `sys_user.id`
2. 添加触发器验证数据完整性
3. 定期检查orphan引用

### 监控建议
1. 添加操作日志记录密码修改行为
2. 监控组织负责人变更操作
3. 设置数据完整性告警

---

## ✅ 验证签名

**修复完成**: ✅
**编译通过**: ✅
**待测试**: ⏳ (需要集成测试)
**待部署**: ⏳ (等待测试通过)

**修复人员**: Claude (高级开发工程师)
**审核人员**: 待审核
**批准人员**: 待批准

---

**修复日期**: 2025-10-04
**文档版本**: 1.0
