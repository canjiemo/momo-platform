# 📡 API接口路径清单

## 认证接口 (AuthController)

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/auth/login` | 公开 | 用户登录 |
| GET | `/auth/captcha` | 公开 | 获取验证码 |

## 用户管理 (UserController)

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/system/user/search` | `user:view` | 分页搜索用户 |
| GET | `/system/user/{id}` | `user:view` | 获取用户详情 |
| POST | `/system/user/create` | `user:create` | 创建用户 |
| POST | `/system/user/update` | `user:update` | 更新用户 |
| POST | `/system/user/delete` | `user:delete` | 删除用户 |
| POST | `/system/user/change-password` | 登录即可 | 修改自己密码 |
| POST | `/system/user/init-password` | `user:init` | 初始化密码 |
| POST | `/system/user/reset-password-admin` | `user:reset` | 管理员重置密码 |
| GET | `/system/user/profile` | 登录即可 | 获取个人信息 |

## 角色管理 (RoleController)

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/system/role/search` | `role:view` | 分页搜索角色 |
| GET | `/system/role/list` | `role:view` | 获取角色列表 |
| GET | `/system/role/{id}` | `role:view` | 获取角色详情 |
| POST | `/system/role/create` | `role:create` | 创建角色 |
| POST | `/system/role/update` | `role:update` | 更新角色 |
| POST | `/system/role/delete` | `role:delete` | 删除角色 |
| POST | `/system/role/assign-menus` | `role:assign` | 分配权限 |
| GET | `/system/role/menus/{id}` | `role:view` | 获取角色权限 |

## 菜单管理 (MenuController)

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/system/menu/tree` | `menu:view` | 获取菜单树 |
| GET | `/system/menu/user-menus` | 登录即可 | 获取当前用户菜单 |
| GET | `/system/menu/list` | `menu:view` | 获取菜单列表 |
| GET | `/system/menu/{id}` | `menu:view` | 获取菜单详情 |
| POST | `/system/menu/create` | `menu:create` | 创建菜单 |
| POST | `/system/menu/update` | `menu:update` | 更新菜单 |
| POST | `/system/menu/delete` | `menu:delete` | 删除菜单 |

## 组织管理 (OrganizationController)

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/system/organization/search` | `organization:view` | 分页搜索组织 |
| GET | `/system/organization/tree` | `organization:view` | 获取组织树 |
| GET | `/system/organization/tree/{parentId}` | `organization:view` | 获取子组织树 |
| GET | `/system/organization/list` | `organization:view` | 获取组织列表 |
| GET | `/system/organization/{id}` | `organization:view` | 获取组织详情 |
| POST | `/system/organization/create` | `organization:create` | 创建组织 |
| POST | `/system/organization/update` | `organization:update` | 更新组织 |
| POST | `/system/organization/delete` | `organization:delete` | 删除组织 |
| POST | `/system/organization/move/{orgId}/{newParentId}` | `organization:update` | 移动组织 |
| GET | `/system/organization/children/{orgId}` | `organization:view` | 获取子组织 |
| GET | `/system/organization/path/{orgId}` | `organization:view` | 获取组织路径 |
| GET | `/system/organization/check-code` | `organization:view` | 检查组织编码 |

## 操作日志 (OperationLogController)

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/system/operation-log/search` | `log:view` | 分页搜索日志 |
| GET | `/system/operation-log/{id}` | `log:view` | 获取日志详情 |
| POST | `/system/operation-log/delete` | `log:delete` | 删除日志 |
| POST | `/system/operation-log/clean/{days}` | `log:delete` | 清理历史日志 |
| GET | `/system/operation-log/stats/operation-type` | `log:view` | 操作类型统计 |
| GET | `/system/operation-log/stats/module` | `log:view` | 模块统计 |
| GET | `/system/operation-log/stats/user-activity` | `log:view` | 用户活跃度统计 |
| GET | `/system/operation-log/stats/failure` | `log:view` | 失败操作统计 |
| GET | `/system/operation-log/stats/trend` | `log:view` | 操作趋势统计 |
| POST | `/system/operation-log/export` | `log:export` | 导出日志 |
| GET | `/system/operation-log/operation-types` | `log:view` | 获取操作类型列表 |

## 数据字典-类型 (DictTypeController)

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/system/dict-type/search` | `dict:view` | 分页搜索字典类型 |
| GET | `/system/dict-type/list` | `dict:view` | 获取字典类型列表 |
| GET | `/system/dict-type/{id}` | `dict:view` | 获取字典类型详情 |
| GET | `/system/dict-type/type/{dictType}` | `dict:view` | 按类型获取字典 |
| POST | `/system/dict-type/create` | `dict:create` | 创建字典类型 |
| POST | `/system/dict-type/update` | `dict:update` | 更新字典类型 |
| POST | `/system/dict-type/delete` | `dict:delete` | 删除字典类型 |
| POST | `/system/dict-type/refresh` | `dict:update` | 刷新字典缓存 |

## 数据字典-数据 (DictDataController)

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/system/dict-data/search` | `dict:view` | 分页搜索字典数据 |
| GET | `/system/dict-data/list/{dictType}` | `dict:view` | 按类型获取字典数据 |
| GET | `/system/dict-data/{id}` | `dict:view` | 获取字典数据详情 |
| GET | `/system/dict-data/label/{dictType}/{dictValue}` | `dict:view` | 获取字典标签 |
| POST | `/system/dict-data/create` | `dict:create` | 创建字典数据 |
| POST | `/system/dict-data/update` | `dict:update` | 更新字典数据 |
| POST | `/system/dict-data/delete` | `dict:delete` | 删除字典数据 |
| POST | `/system/dict-data/batch-update-sort` | `dict:update` | 批量更新排序 |

## 测试用例中常用的API路径

### ✅ 正确路径

```bash
# 用户管理
POST /system/user/search         # 搜索用户
POST /system/user/create         # 创建用户
POST /system/user/update         # 更新用户
POST /system/user/delete         # 删除用户
POST /system/user/init-password  # 初始化密码
POST /system/user/reset-password-admin  # 重置密码

# 角色管理
POST /system/role/search         # 搜索角色
POST /system/role/create         # 创建角色
POST /system/role/update         # 更新角色
POST /system/role/delete         # 删除角色
POST /system/role/assign-menus   # 分配权限

# 菜单管理
GET  /system/menu/tree           # 获取菜单树
POST /system/menu/create         # 创建菜单
POST /system/menu/update         # 更新菜单
POST /system/menu/delete         # 删除菜单

# 组织管理
POST /system/organization/search  # 搜索组织
POST /system/organization/create  # 创建组织
POST /system/organization/update  # 更新组织
POST /system/organization/delete  # 删除组织

# 操作日志
POST /system/operation-log/search  # 搜索日志
POST /system/operation-log/delete  # 删除日志
POST /system/operation-log/export  # 导出日志

# 数据字典
POST /system/dict-type/search     # 搜索字典类型
POST /system/dict-data/search     # 搜索字典数据
```

### ❌ 错误路径（测试脚本中可能使用的）

```bash
# 这些路径不存在，需要修正：
GET  /system/users               ❌ 应该是 POST /system/user/search
GET  /system/roles               ❌ 应该是 GET  /system/role/list
GET  /system/menus               ❌ 应该是 GET  /system/menu/list
DELETE /system/user/{id}         ❌ 应该是 POST /system/user/delete
DELETE /system/role/{id}         ❌ 应该是 POST /system/role/delete
```

## 测试脚本需要修改的地方

测试脚本中应该使用以下格式：

```bash
# 创建用户
api_request "POST" "/system/user/create" "$TOKEN" '{"username":"test",...}'

# 删除用户
api_request "POST" "/system/user/delete" "$TOKEN" '{"ids":[1,2,3]}'

# 搜索用户
api_request "POST" "/system/user/search" "$TOKEN" '{"pageNum":1,"pageSize":10}'

# 初始化密码
api_request "POST" "/system/user/init-password" "$TOKEN" '{"userId":10}'

# 分配角色权限
api_request "POST" "/system/role/assign-menus" "$TOKEN" '{"roleId":1,"menuIds":[1,2,3]}'
```

## 注意事项

1. **所有修改操作都使用POST**，不使用PUT/DELETE
2. **删除操作传递ID数组**: `{"ids":[1,2,3]}`
3. **分页查询使用POST** `/search` 端点
4. **列表查询使用GET** `/list` 端点
5. **详情查询使用GET** `/{id}` 端点

---

**最后更新**: 2025-10-04
**版本**: 1.0
