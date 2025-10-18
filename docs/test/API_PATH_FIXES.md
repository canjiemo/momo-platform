# 🔧 测试脚本API路径修正清单

## 需要修正的API路径

根据实际Controller扫描，以下是测试脚本中需要修正的API路径：

### ❌ 错误 → ✅ 正确

| 测试脚本中的路径 | 正确路径 | 说明 |
|------------------|----------|------|
| `/system/user/reset-password` | `/system/user/reset-password-admin` | 管理员重置密码 |
| `GET /system/user/profile` | `GET /system/user/profile` | ✅ 正确 |
| `POST /system/user/create` | `POST /system/user/create` | ✅ 正确 |
| `POST /system/user/delete` | `POST /system/user/delete` | ✅ 正确 |
| `POST /system/user/update` | `POST /system/user/update` | ✅ 正确 |
| `POST /system/user/init-password` | `POST /system/user/init-password` | ✅ 正确 |
| `POST /system/role/create` | `POST /system/role/create` | ✅ 正确 |
| `POST /system/role/delete` | `POST /system/role/delete` | ✅ 正确 |
| `POST /system/role/update` | `POST /system/role/update` | ✅ 正确 |
| `POST /system/menu/create` | `POST /system/menu/create` | ✅ 正确 |
| `POST /system/menu/delete` | `POST /system/menu/delete` | ✅ 正确 |
| `POST /system/menu/update` | `POST /system/menu/update` | ✅ 正确 |
| `POST /system/organization/create` | `POST /system/organization/create` | ✅ 正确 |
| `POST /system/organization/delete` | `POST /system/organization/delete` | ✅ 正确 |
| `POST /system/organization/update` | `POST /system/organization/update` | ✅ 正确 |
| `POST /system/operation-log/delete` | `POST /system/operation-log/delete` | ✅ 正确 |
| `POST /system/operation-log/export` | `POST /system/operation-log/export` | ✅ 正确 |

## 修正后的测试脚本片段

### B14: 部分权限用户重置密码

```bash
# ❌ 错误
RESP=$(api_request "POST" "/system/user/reset-password" "$TOKEN_PARTIAL" '{"id":10,"newPassword":"New123!"}')

# ✅ 正确
RESP=$(api_request "POST" "/system/user/reset-password-admin" "$TOKEN_PARTIAL" '{"userId":10,"newPassword":"New123!"}')
```

## 请求参数格式

### 用户管理

```bash
# 创建用户
POST /system/user/create
{
  "username": "test",
  "password": "Test@123",
  "realName": "测试用户",
  "status": 1
}

# 删除用户（注意：使用ids数组）
POST /system/user/delete
{
  "ids": [10, 20, 30]
}

# 更新用户
POST /system/user/update
{
  "id": 10,
  "realName": "新名字",
  "status": 1
}

# 初始化密码
POST /system/user/init-password
{
  "userId": 10
}

# 管理员重置密码
POST /system/user/reset-password-admin
{
  "userId": 10,
  "newPassword": "NewPass@123"
}

# 修改自己密码
POST /system/user/change-password
{
  "oldPassword": "Old@123",
  "newPassword": "New@123"
}
```

### 角色管理

```bash
# 创建角色
POST /system/role/create
{
  "roleName": "测试角色",
  "description": "描述",
  "status": 1
}

# 删除角色（注意：使用ids数组）
POST /system/role/delete
{
  "ids": [100, 200]
}

# 更新角色
POST /system/role/update
{
  "id": 100,
  "roleName": "新名称",
  "status": 1
}

# 分配权限
POST /system/role/assign-menus
{
  "roleId": 100,
  "menuIds": [1, 2, 3, 4, 5]
}
```

### 菜单管理

```bash
# 创建菜单
POST /system/menu/create
{
  "menuName": "测试菜单",
  "parentId": 0,
  "type": 0,
  "path": "/test",
  "component": "Test",
  "permission": "test:view",
  "status": 1
}

# 删除菜单（注意：使用id，不是ids数组）
POST /system/menu/delete
{
  "id": 1000
}

# 更新菜单
POST /system/menu/update
{
  "id": 1000,
  "menuName": "新名称",
  "status": 1
}
```

### 组织管理

```bash
# 创建组织
POST /system/organization/create
{
  "orgName": "测试组织",
  "orgCode": "TEST",
  "parentId": 0,
  "status": 1
}

# 删除组织（注意：使用id，不是ids数组）
POST /system/organization/delete
{
  "id": 100
}

# 更新组织
POST /system/organization/update
{
  "id": 100,
  "orgName": "新名称",
  "status": 1
}
```

### 操作日志

```bash
# 删除日志（注意：使用ids数组）
POST /system/operation-log/delete
{
  "ids": [1, 2, 3]
}

# 导出日志
POST /system/operation-log/export
{
  "startTime": "2025-01-01",
  "endTime": "2025-12-31"
}
```

## 查询接口

### 分页搜索（使用POST）

```bash
# 搜索用户
POST /system/user/search
{
  "pageNum": 1,
  "pageSize": 10,
  "username": "admin",  # 可选过滤条件
  "status": 1           # 可选过滤条件
}

# 搜索角色
POST /system/role/search
{
  "pageNum": 1,
  "pageSize": 10
}

# 搜索组织
POST /system/organization/search
{
  "pageNum": 1,
  "pageSize": 10
}

# 搜索日志
POST /system/operation-log/search
{
  "pageNum": 1,
  "pageSize": 10,
  "module": "user",     # 可选
  "operationType": "CREATE"  # 可选
}
```

### 列表查询（使用GET）

```bash
# 角色列表（不分页）
GET /system/role/list

# 菜单列表
GET /system/menu/list

# 菜单树
GET /system/menu/tree

# 组织列表
GET /system/organization/list

# 组织树
GET /system/organization/tree

# 字典类型列表
GET /system/dict-type/list

# 字典数据列表
GET /system/dict-data/list/{dictType}
```

### 详情查询（使用GET）

```bash
# 用户详情
GET /system/user/{id}

# 角色详情
GET /system/role/{id}

# 菜单详情
GET /system/menu/{id}

# 组织详情
GET /system/organization/{id}

# 日志详情
GET /system/operation-log/{id}
```

## 特别注意

### 1. 删除操作的参数格式

不同的Controller使用不同的删除参数格式：

- **用户删除**: `{"ids": [10, 20]}` - 数组
- **角色删除**: `{"ids": [100, 200]}` - 数组
- **菜单删除**: `{"id": 1000}` - 单个ID
- **组织删除**: `{"id": 100}` - 单个ID
- **日志删除**: `{"ids": [1, 2, 3]}` - 数组

### 2. 密码相关接口

- **修改自己密码**: `/system/user/change-password`
  - 需要提供 `oldPassword` 和 `newPassword`

- **初始化密码**: `/system/user/init-password`
  - 参数: `{"userId": 10}`
  - 权限: `user:init`

- **管理员重置密码**: `/system/user/reset-password-admin`
  - 参数: `{"userId": 10, "newPassword": "New@123"}`
  - 权限: `user:reset`

### 3. 权限分配

```bash
# 分配角色权限
POST /system/role/assign-menus
{
  "roleId": 100,
  "menuIds": [1, 2, 3, 4, 5]
}

# 获取角色权限
GET /system/role/menus/{roleId}
```

## 测试脚本修改建议

在 `complete_120_tests.sh` 中查找并替换：

```bash
# 查找
"/system/user/reset-password"

# 替换为
"/system/user/reset-password-admin"
```

同时确认参数格式：

```bash
# 旧参数
'{"id":10,"newPassword":"New123!"}'

# 新参数
'{"userId":10,"newPassword":"New123!"}'
```

---

**更新日期**: 2025-10-04
**版本**: 1.0
