# 🔒 Seer Fitness Edu - 全面安全测试方案

## 📊 系统权限点清单 (30个)

### 1. 用户管理 (6个权限)
- `user:view` - 查看用户
- `user:create` - 创建用户
- `user:update` - 更新用户
- `user:delete` - 删除用户
- `user:init-password` - 初始化密码
- `user:reset-password` - 重置密码

### 2. 角色管理 (5个权限)
- `role:view` - 查看角色
- `role:create` - 创建角色
- `role:update` - 更新角色
- `role:delete` - 删除角色
- `role:assign` - 分配角色权限

### 3. 菜单管理 (4个权限)
- `menu:view` - 查看菜单
- `menu:create` - 创建菜单
- `menu:update` - 更新菜单
- `menu:delete` - 删除菜单

### 4. 组织管理 (4个权限)
- `organization:view` - 查看组织
- `organization:create` - 创建组织
- `organization:update` - 更新组织
- `organization:delete` - 删除组织

### 5. 操作日志 (3个权限)
- `operation_log:view` - 查看日志
- `operation_log:delete` - 删除日志
- `operation_log:export` - 导出日志

### 6. 数据字典类型 (4个权限)
- `dict:type:view` - 查看字典类型
- `dict:type:create` - 创建字典类型
- `dict:type:update` - 更新字典类型
- `dict:type:delete` - 删除字典类型

### 7. 数据字典数据 (4个权限)
- `dict:data:view` - 查看字典数据
- `dict:data:create` - 创建字典数据
- `dict:data:update` - 更新字典数据
- `dict:data:delete` - 删除字典数据

---

## 🎭 测试角色设计 (10个角色)

### 1. 超级管理员 (admin_flag=1)
- 所有权限 + admin_flag bypass
- 用户: `superadmin` / `Admin123!`

### 2. 系统管理员 (system_admin)
- 所有权限 (通过权限分配,非admin_flag)
- 用户: `sysadmin` / `Sysadmin123!`

### 3. 用户管理员 (user_manager)
- `user:*` (所有用户权限)
- `organization:view`
- 用户: `usermgr` / `Usermgr123!`

### 4. 角色管理员 (role_manager)
- `role:*` (所有角色权限)
- `menu:view`
- 用户: `rolemgr` / `Rolemgr123!`

### 5. 内容管理员 (content_manager)
- `menu:*`
- `dict:type:*`
- `dict:data:*`
- 用户: `contentmgr` / `Contentmgr123!`

### 6. 审计员 (auditor)
- `operation_log:view`
- `operation_log:export`
- `user:view`
- `role:view`
- 用户: `auditor` / `Auditor123!`

### 7. 只读用户 (read_only)
- `*.view` (所有查看权限)
- 用户: `readonly` / `Readonly123!`

### 8. 部分权限用户 (partial_user)
- `user:view`
- `user:create`
- `role:view`
- 用户: `partial` / `Partial123!`

### 9. 无权限用户 (no_perm)
- 没有任何权限(只能登录)
- 用户: `noperm` / `Noperm123!`

### 10. 禁用用户 (disabled)
- status=0
- 用户: `disabled` / `Disabled123!`

---

## 🧪 测试用例设计 (120个用例)

### A. 认证测试 (10个用例)
1. ✅ 正常登录 - 有效用户名密码
2. ❌ 错误密码登录
3. ❌ 不存在用户名登录
4. ❌ 禁用用户登录 (status=0)
5. ❌ 密码5次错误账户锁定
6. ❌ 锁定后尝试登录
7. ✅ 验证码正确登录
8. ❌ 验证码错误登录
9. ❌ 验证码过期登录
10. ✅ Token过期后访问API

### B. 授权测试 - 垂直越权 (30个用例)
测试原则: 低权限用户尝试访问高权限接口

#### B1. 无权限用户越权 (10个)
11. ❌ noperm 尝试创建用户
12. ❌ noperm 尝试删除用户
13. ❌ noperm 尝试创建角色
14. ❌ noperm 尝试删除角色
15. ❌ noperm 尝试创建菜单
16. ❌ noperm 尝试删除菜单
17. ❌ noperm 尝试删除日志
18. ❌ noperm 尝试导出日志
19. ❌ noperm 尝试创建组织
20. ❌ noperm 尝试删除组织

#### B2. 部分权限用户越权 (10个)
21. ❌ partial (只有view,create) 尝试删除用户
22. ❌ partial (只有view,create) 尝试更新用户
23. ❌ partial (只有view,create) 尝试初始化密码
24. ❌ partial (只有view,create) 尝试重置密码
25. ❌ partial 尝试创建角色 (无role:create权限)
26. ❌ partial 尝试删除角色 (无role:delete权限)
27. ❌ partial 尝试创建菜单 (无menu:create权限)
28. ❌ partial 尝试创建组织 (无org:create权限)
29. ❌ partial 尝试删除日志 (无log:delete权限)
30. ❌ partial 尝试导出日志 (无log:export权限)

#### B3. 只读用户越权 (10个)
31. ❌ readonly 尝试创建用户
32. ❌ readonly 尝试更新用户
33. ❌ readonly 尝试删除用户
34. ❌ readonly 尝试创建角色
35. ❌ readonly 尝试更新角色
36. ❌ readonly 尝试删除角色
37. ❌ readonly 尝试创建菜单
38. ❌ readonly 尝试创建组织
39. ❌ readonly 尝试删除日志
40. ❌ readonly 尝试导出日志

### C. 授权测试 - 水平越权 (20个用例)
测试原则: 用户A操作用户B的数据

#### C1. 用户数据隔离 (10个)
41. ❌ 用户A修改用户B的密码
42. ❌ 用户A删除用户B
43. ❌ 用户A查看用户B的详细信息(如需数据隔离)
44. ❌ 用户A修改用户B的角色
45. ❌ 用户A禁用用户B
46. ❌ 用户A初始化用户B的密码
47. ❌ 用户A重置用户B的密码
48. ❌ 用户A查看用户B的操作日志(如需数据隔离)
49. ❌ 用户A修改用户B的组织归属
50. ❌ 用户A删除用户B创建的数据

#### C2. 角色数据隔离 (10个)
51. ❌ 角色管理员A修改系统预置角色
52. ❌ 角色管理员A删除系统预置角色
53. ❌ 普通用户查看所有角色(如需数据隔离)
54. ❌ 普通用户修改角色权限
55. ❌ 用户A给自己分配超级管理员角色
56. ❌ 用户A给自己分配不应有的权限
57. ❌ 用户A创建admin_flag=1的用户
58. ❌ 用户A修改自己的admin_flag为1
59. ❌ 用户A修改其他用户的admin_flag
60. ❌ 非角色管理员修改角色菜单权限

### D. 业务逻辑漏洞测试 (30个用例)

#### D1. 数据一致性 (10个)
61. ❌ 创建重复用户名
62. ❌ 创建重复角色名
63. ❌ 删除已分配给用户的角色
64. ❌ 删除包含子菜单的父菜单
65. ❌ 删除包含子组织的父组织
66. ❌ 修改用户状态为非法值(非0/1)
67. ❌ 修改菜单类型为非法值(非0/1/2)
68. ❌ 设置菜单父节点为自己
69. ❌ 设置菜单父节点形成循环引用
70. ❌ 设置组织父节点形成循环引用

#### D2. 参数验证 (10个)
71. ❌ 用户名为空创建用户
72. ❌ 密码不符合强度要求
73. ❌ 用户名超长(>50字符)
74. ❌ 角色名为空创建角色
75. ❌ 菜单名为空创建菜单
76. ❌ 组织编码重复
77. ❌ 字典类型编码重复
78. ❌ SQL注入测试 (username: `admin' OR '1'='1`)
79. ❌ XSS注入测试 (realName: `<script>alert(1)</script>`)
80. ❌ 负数ID查询/删除

#### D3. 状态转换 (10个)
81. ✅ 启用用户 (0→1)
82. ✅ 禁用用户 (1→0)
83. ❌ 禁用最后一个admin_flag=1的用户
84. ❌ 删除最后一个admin_flag=1的用户
85. ❌ 修改自己的状态为禁用
86. ❌ 删除自己
87. ✅ 角色状态切换 (0↔1)
88. ✅ 菜单状态切换 (0↔1)
89. ✅ 组织状态切换 (0↔1)
90. ❌ 修改已删除数据(delete_flag=1)

### E. 权限组合测试 (20个用例)

#### E1. AND权限组合 (10个)
91. ✅ 用户同时拥有user:view和user:create执行查询
92. ✅ 用户同时拥有user:view和user:create执行创建
93. ❌ 用户只有user:view执行创建
94. ❌ 用户只有user:create执行查询
95. ✅ 用户拥有role:view和role:assign执行权限分配
96. ❌ 用户只有role:view执行权限分配
97. ❌ 用户只有role:assign执行权限分配
98. ✅ 用户拥有menu:view和menu:create执行菜单创建
99. ✅ 用户拥有dict:type:view和dict:type:create执行字典创建
100. ❌ 用户缺少任一权限时拒绝操作

#### E2. OR权限组合 (10个)
101. ✅ 用户拥有user:view或user:create访问用户列表
102. ✅ 用户拥有role:view或role:create访问角色列表
103. ✅ 用户拥有menu:view或menu:create访问菜单列表
104. ✅ admin_flag=1用户bypass所有权限检查
105. ❌ 普通用户无任何权限访问受保护接口
106. ✅ 用户拥有operation_log:view访问日志统计
107. ✅ 用户拥有operation_log:export导出日志
108. ❌ 用户无operation_log:export导出日志
109. ✅ 用户拥有多个角色时权限合并
110. ✅ 用户角色被移除后权限立即失效

### F. 并发和缓存测试 (10个用例)

111. ✅ 并发创建同名用户(只成功1个)
112. ✅ 并发创建同名角色(只成功1个)
113. ✅ 并发修改同一用户(乐观锁)
114. ✅ Token缓存失效后重新登录
115. ✅ 角色权限修改后缓存自动刷新
116. ✅ 用户被禁用后Token立即失效
117. ✅ 用户角色修改后权限立即生效
118. ✅ 字典缓存更新测试
119. ✅ 并发删除同一数据(幂等性)
120. ✅ Redis缓存故障降级测试

---

## 🔍 重点漏洞检测项

### 1. 认证漏洞
- [ ] 暴力破解防护 (账户锁定)
- [ ] JWT Token安全性 (签名、过期)
- [ ] 会话固定攻击
- [ ] 验证码绕过

### 2. 授权漏洞
- [ ] 垂直越权 (低权限访问高权限接口)
- [ ] 水平越权 (用户A操作用户B数据)
- [ ] admin_flag绕过检测
- [ ] 权限缓存一致性

### 3. 业务逻辑漏洞
- [ ] 自我提权 (用户修改自己为admin)
- [ ] 最后管理员删除
- [ ] 循环引用 (菜单/组织树)
- [ ] 数据一致性 (级联删除)

### 4. 注入漏洞
- [ ] SQL注入
- [ ] XSS注入
- [ ] LDAP注入
- [ ] 命令注入

### 5. 数据泄露
- [ ] 敏感信息泄露 (密码明文)
- [ ] 用户枚举
- [ ] 错误信息泄露
- [ ] 日志敏感数据

---

## 📈 测试优先级

### P0 (严重 - 立即测试)
- 垂直越权测试 (B组)
- admin_flag绕过测试
- 自我提权测试
- SQL注入测试

### P1 (高 - 本周完成)
- 水平越权测试 (C组)
- 业务逻辑漏洞 (D组)
- 认证测试 (A组)

### P2 (中 - 本月完成)
- 权限组合测试 (E组)
- 并发测试 (F组)
- XSS注入测试

### P3 (低 - 持续进行)
- 性能测试
- 压力测试
- 兼容性测试
