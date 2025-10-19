# Flyway 自测流程指南

**版本**: 1.0
**状态**: ⚠️ **必须完成测试后才能上生产**
**测试时间**: 预计30-45分钟

---

## ⚠️ 生产环境检查清单

### 当前状态: 🟡 **未完成测试，不可上生产**

| 检查项 | 状态 | 说明 |
|--------|------|------|
| ✅ 代码完成 | 完成 | Phase 1 + Phase 2 全部完成 |
| ✅ 编译通过 | 完成 | mvn clean compile 成功 |
| ✅ 应用启动 | 完成 | 端口8070正常监听 |
| ❌ 功能测试 | **未完成** | **必须先测试！** |
| ❌ 回滚测试 | **未完成** | **必须先测试！** |
| ❌ 批量测试 | **未完成** | **必须先测试！** |
| ❌ 异常测试 | **未完成** | **必须先测试！** |
| ❌ 性能测试 | **未完成** | 可选，建议测试 |

**结论**: ⚠️ **必须完成以下自测流程后才能考虑上生产环境**

---

## 📋 自测流程概览

### Phase 1: 基础功能测试 (15分钟)
1. ✅ 创建测试租户 + Schema初始化
2. ✅ 验证Flyway baseline
3. ✅ 执行单个Schema升级
4. ✅ 验证迁移历史记录

### Phase 2: 批量升级测试 (15分钟)
5. ✅ 批量创建3-5个测试租户
6. ✅ 执行批量Schema升级
7. ✅ 监控升级任务状态
8. ✅ 验证升级日志

### Phase 3: 回滚测试 (10分钟)
9. ✅ 查询可回滚版本
10. ✅ 执行Schema回滚
11. ✅ 验证回滚结果

### Phase 4: 异常场景测试 (5分钟)
12. ✅ 测试任务取消
13. ✅ 测试无效版本回滚
14. ✅ 验证错误处理

---

## 🧪 详细测试步骤

### 准备工作

#### 1. 确保应用正常运行
```bash
# 检查应用状态
lsof -i :8070 | grep LISTEN

# 如果没有运行，启动应用
cd /Users/canjiemo/project/seer-fitness-edu
mvn spring-boot:run -pl seer-fitness-boot -DskipTests
```

#### 2. 获取测试Token
```bash
# 1. 获取验证码
CAPTCHA_RESPONSE=$(curl -s http://localhost:8070/auth/captcha)
CAPTCHA_ID=$(echo $CAPTCHA_RESPONSE | jq -r '.data.captchaId')

# 2. 从Redis获取验证码
CAPTCHA_CODE=$(redis-cli GET "captcha:$CAPTCHA_ID")

# 3. 登录获取Token
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8070/auth/login \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"admin\",
    \"password\": \"Aa123456!\",
    \"captchaId\": \"$CAPTCHA_ID\",
    \"captcha\": \"$CAPTCHA_CODE\"
  }")

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.token')
echo "Token: $TOKEN"

# 保存Token到环境变量
export AUTH_TOKEN="Bearer $TOKEN"
```

---

## Phase 1: 基础功能测试

### 测试1.1: 创建测试租户 + 初始化Schema

```bash
# 创建测试租户（会自动创建Schema并执行Flyway baseline）
curl -X POST http://localhost:8070/platform/tenant/create \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantName": "测试租户001",
    "tenantCode": "test_tenant_001",
    "contactPerson": "测试负责人",
    "contactPhone": "13800138000",
    "status": 1
  }' | jq

# 预期结果：
# {
#   "code": 200,
#   "message": "success",
#   "data": {
#     "tenantId": 101,
#     "schemaName": "test_tenant_001",
#     "baselineVersion": "1.0.0"
#   }
# }
```

**验证点**:
- ✅ 租户创建成功
- ✅ Schema自动创建（test_tenant_001）
- ✅ Flyway baseline执行成功（V1.0.0）

### 测试1.2: 验证Schema状态

```bash
# 连接数据库检查
psql -U postgres -d seer_fitness_edu -c "
  SELECT schema_name, current_version, flyway_version, baseline_version
  FROM public.sys_schema_version
  WHERE schema_name = 'test_tenant_001';
"

# 预期结果：
# schema_name     | current_version | flyway_version | baseline_version
# ----------------+-----------------+----------------+-----------------
# test_tenant_001 | 1.0.0           | 1.0.0          | 1.0.0

# 检查Flyway history表
psql -U postgres -d seer_fitness_edu -c "
  SELECT version, description, type, success
  FROM test_tenant_001.flyway_schema_history
  ORDER BY installed_rank;
"

# 预期结果：
# version | description | type     | success
# --------+-------------+----------+--------
# 1.0.0   | baseline    | BASELINE | t
```

**验证点**:
- ✅ sys_schema_version记录正确
- ✅ flyway_schema_history存在baseline记录

---

### 测试1.3: 执行单个Schema升级到V1.1.0

```bash
# 执行单个Schema升级
curl -X POST http://localhost:8070/platform/upgrade/execute \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "taskName": "测试升级tenant_001到V1.1.0",
    "upgradeType": "SINGLE",
    "targetSchemas": ["test_tenant_001"],
    "targetVersion": "1.1.0"
  }' | jq

# 预期结果：
# {
#   "code": 200,
#   "data": 1  // taskId
# }

# 保存taskId
TASK_ID=1
```

**验证点**:
- ✅ 返回任务ID
- ✅ HTTP 200 成功

### 测试1.4: 查询升级任务状态

```bash
# 查询任务状态（可能需要等待几秒）
curl -X GET "http://localhost:8070/platform/upgrade/task/$TASK_ID" \
  -H "Authorization: $AUTH_TOKEN" | jq

# 预期结果：
# {
#   "code": 200,
#   "data": {
#     "id": 1,
#     "taskName": "测试升级tenant_001到V1.1.0",
#     "status": "COMPLETED",  // 或 RUNNING
#     "totalSchemas": 1,
#     "successCount": 1,
#     "failedCount": 0,
#     "logs": [
#       {
#         "schemaName": "test_tenant_001",
#         "fromVersion": "1.0.0",
#         "toVersion": "1.1.0",
#         "migrationsExecuted": 1,
#         "status": "SUCCESS",
#         "executionTime": 1234
#       }
#     ]
#   }
# }
```

**验证点**:
- ✅ 任务状态为COMPLETED
- ✅ successCount = 1, failedCount = 0
- ✅ log中显示从1.0.0升级到1.1.0

### 测试1.5: 验证升级结果

```bash
# 1. 检查版本表
psql -U postgres -d seer_fitness_edu -c "
  SELECT schema_name, current_version, last_migration_at
  FROM public.sys_schema_version
  WHERE schema_name = 'test_tenant_001';
"

# 预期：current_version = 1.1.0

# 2. 检查Flyway history
psql -U postgres -d seer_fitness_edu -c "
  SELECT version, description, type, success
  FROM test_tenant_001.flyway_schema_history
  ORDER BY installed_rank;
"

# 预期：
# version | description           | type    | success
# --------+-----------------------+---------+--------
# 1.0.0   | baseline              | BASELINE| t
# 1.1.0   | add test column       | SQL     | t

# 3. 验证test_field列是否添加
psql -U postgres -d seer_fitness_edu -c "
  SELECT column_name, data_type
  FROM information_schema.columns
  WHERE table_schema = 'test_tenant_001'
    AND table_name = 'sys_user'
    AND column_name = 'test_field';
"

# 预期：test_field | character varying
```

**验证点**:
- ✅ current_version = 1.1.0
- ✅ flyway_schema_history有V1.1.0记录
- ✅ test_field列已添加

---

## Phase 2: 批量升级测试

### 测试2.1: 批量创建测试租户

```bash
# 创建租户002
curl -X POST http://localhost:8070/platform/tenant/create \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantName": "测试租户002",
    "tenantCode": "test_tenant_002",
    "contactPerson": "测试负责人",
    "contactPhone": "13800138002",
    "status": 1
  }' | jq

# 创建租户003
curl -X POST http://localhost:8070/platform/tenant/create \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantName": "测试租户003",
    "tenantCode": "test_tenant_003",
    "contactPerson": "测试负责人",
    "contactPhone": "13800138003",
    "status": 1
  }' | jq

# 创建租户004
curl -X POST http://localhost:8070/platform/tenant/create \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantName": "测试租户004",
    "tenantCode": "test_tenant_004",
    "contactPerson": "测试负责人",
    "contactPhone": "13800138004",
    "status": 1
  }' | jq
```

**验证点**:
- ✅ 3个租户创建成功
- ✅ 3个Schema自动创建
- ✅ 都执行了baseline（V1.0.0）

### 测试2.2: 批量升级3个Schema

```bash
# 批量升级
curl -X POST http://localhost:8070/platform/upgrade/execute \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "taskName": "批量升级3个测试租户到V1.1.0",
    "upgradeType": "BATCH",
    "targetSchemas": ["test_tenant_002", "test_tenant_003", "test_tenant_004"],
    "targetVersion": "1.1.0"
  }' | jq

# 保存taskId
BATCH_TASK_ID=2
```

### 测试2.3: 监控批量升级进度

```bash
# 每隔2秒查询一次任务状态
for i in {1..10}; do
  echo "=== 第${i}次查询 ==="
  curl -s -X GET "http://localhost:8070/platform/upgrade/task/$BATCH_TASK_ID" \
    -H "Authorization: $AUTH_TOKEN" | jq '.data | {status, successCount, failedCount}'
  sleep 2
done

# 预期：
# {
#   "status": "RUNNING",  # 初始状态
#   "successCount": 0,
#   "failedCount": 0
# }
# ...
# {
#   "status": "COMPLETED",  # 最终状态
#   "successCount": 3,
#   "failedCount": 0
# }
```

**验证点**:
- ✅ 任务状态从RUNNING变为COMPLETED
- ✅ successCount逐步增加到3
- ✅ failedCount = 0

### 测试2.4: 查询升级历史

```bash
# 查询所有升级历史
curl -X POST http://localhost:8070/platform/upgrade/history \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "pageNum": 1,
    "pageSize": 10
  }' | jq

# 预期：看到2个任务记录
# - taskId=1: SINGLE升级
# - taskId=2: BATCH升级
```

**验证点**:
- ✅ 历史记录包含所有任务
- ✅ 分页功能正常

---

## Phase 3: 回滚测试

### 测试3.1: 查询可回滚版本

```bash
# 查询test_tenant_001的可回滚版本
curl -X GET "http://localhost:8070/platform/upgrade/versions/test_tenant_001" \
  -H "Authorization: $AUTH_TOKEN" | jq

# 预期结果：
# {
#   "code": 200,
#   "data": ["1.1.0", "1.0.0"]  // 当前版本 + 历史版本
# }
```

**验证点**:
- ✅ 返回版本列表
- ✅ 包含1.0.0和1.1.0

### 测试3.2: 执行Schema回滚到V1.0.0

```bash
# 回滚test_tenant_001到V1.0.0
curl -X POST http://localhost:8070/platform/upgrade/rollback \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "schemaName": "test_tenant_001",
    "targetVersion": "1.0.0"
  }' | jq

# 预期结果：
# {
#   "code": 200,
#   "data": {
#     "success": true,
#     "schemaName": "test_tenant_001",
#     "fromVersion": "1.1.0",
#     "toVersion": "1.0.0",
#     "executionTime": 567
#   }
# }
```

**验证点**:
- ✅ success = true
- ✅ fromVersion = 1.1.0, toVersion = 1.0.0

### 测试3.3: 验证回滚结果

```bash
# 检查版本表
psql -U postgres -d seer_fitness_edu -c "
  SELECT schema_name, current_version
  FROM public.sys_schema_version
  WHERE schema_name = 'test_tenant_001';
"

# 预期：current_version = 1.0.0

# 检查回滚历史
psql -U postgres -d seer_fitness_edu -c "
  SELECT schema_name, from_version, to_version, rollback_flag
  FROM public.sys_schema_version_history
  WHERE schema_name = 'test_tenant_001'
  ORDER BY created_at DESC
  LIMIT 1;
"

# 预期：
# schema_name     | from_version | to_version | rollback_flag
# ----------------+--------------+------------+--------------
# test_tenant_001 | 1.1.0        | 1.0.0      | 1
```

**验证点**:
- ✅ current_version已回滚到1.0.0
- ✅ version_history记录了回滚操作（rollback_flag=1）

**⚠️ 重要提醒**:
- Flyway Community Edition不支持自动undo
- 回滚只更新了版本记录，**test_field列仍然存在**
- 生产环境回滚需要DBA手动执行回滚脚本

---

## Phase 4: 异常场景测试

### 测试4.1: 测试任务取消

```bash
# 1. 启动一个批量升级任务
curl -X POST http://localhost:8070/platform/upgrade/execute \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "taskName": "测试取消的任务",
    "upgradeType": "BATCH",
    "targetSchemas": ["test_tenant_002", "test_tenant_003"],
    "targetVersion": "1.1.0"
  }' | jq

CANCEL_TASK_ID=3

# 2. 立即取消任务
curl -X POST "http://localhost:8070/platform/upgrade/cancel/$CANCEL_TASK_ID" \
  -H "Authorization: $AUTH_TOKEN" | jq

# 3. 查询任务状态
curl -X GET "http://localhost:8070/platform/upgrade/task/$CANCEL_TASK_ID" \
  -H "Authorization: $AUTH_TOKEN" | jq '.data.status'

# 预期：status = "CANCELLED"
```

**验证点**:
- ✅ 任务可以取消
- ✅ 状态变为CANCELLED

### 测试4.2: 测试无效版本回滚

```bash
# 尝试回滚到不存在的版本
curl -X POST http://localhost:8070/platform/upgrade/rollback \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "schemaName": "test_tenant_001",
    "targetVersion": "9.9.9"
  }' | jq

# 预期结果：
# {
#   "code": 500,  // 或其他错误码
#   "data": {
#     "success": false,
#     "errorMessage": "目标版本不存在"
#   }
# }
```

**验证点**:
- ✅ 返回错误信息
- ✅ success = false

### 测试4.3: 测试重复回滚

```bash
# 再次回滚到当前版本
curl -X POST http://localhost:8070/platform/upgrade/rollback \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "schemaName": "test_tenant_001",
    "targetVersion": "1.0.0"
  }' | jq

# 预期：
# {
#   "data": {
#     "success": false,
#     "errorMessage": "目标版本与当前版本相同"
#   }
# }
```

**验证点**:
- ✅ 拒绝无意义的回滚
- ✅ 返回友好错误信息

---

## 📊 测试结果检查清单

### 必须通过的测试 (12项)

- [ ] 1.1 创建租户 + Schema初始化成功
- [ ] 1.2 Flyway baseline记录正确
- [ ] 1.3 单个Schema升级成功
- [ ] 1.4 升级任务状态正确
- [ ] 1.5 test_field列已添加
- [ ] 2.1 批量创建租户成功
- [ ] 2.2 批量升级任务创建成功
- [ ] 2.3 升级进度监控正常
- [ ] 2.4 升级历史查询正常
- [ ] 3.1 可回滚版本查询正常
- [ ] 3.2 Schema回滚成功
- [ ] 3.3 回滚历史记录正确

### 异常处理测试 (3项)

- [ ] 4.1 任务取消功能正常
- [ ] 4.2 无效版本回滚被拒绝
- [ ] 4.3 重复回滚被拒绝

---

## 🚀 生产环境上线检查

### 上线前必须完成

✅ **所有15项测试必须通过**

### 生产环境配置检查

- [ ] 数据库备份策略已配置
- [ ] Flyway迁移脚本已review
- [ ] 权限配置已设置（upgrade:execute, upgrade:view, upgrade:rollback）
- [ ] 监控告警已配置
- [ ] 回滚方案已准备

### 建议的上线步骤

1. **灰度发布** - 先在1-2个测试租户上验证
2. **小批量** - 先升级5-10个租户
3. **监控观察** - 观察24小时无异常
4. **逐步扩大** - 分批升级所有租户
5. **保留回滚窗口** - 升级后48小时内保持可快速回滚

---

## 📝 测试报告模板

```markdown
# Flyway功能测试报告

**测试人员**: [你的名字]
**测试时间**: [日期时间]
**测试环境**: [环境名称]

## 测试结果

### Phase 1: 基础功能 (5/5)
- [✅] 1.1 创建租户
- [✅] 1.2 Baseline验证
- [✅] 1.3 单个升级
- [✅] 1.4 任务状态
- [✅] 1.5 升级验证

### Phase 2: 批量升级 (4/4)
- [✅] 2.1 批量创建
- [✅] 2.2 批量升级
- [✅] 2.3 进度监控
- [✅] 2.4 历史查询

### Phase 3: 回滚测试 (3/3)
- [✅] 3.1 版本查询
- [✅] 3.2 执行回滚
- [✅] 3.3 回滚验证

### Phase 4: 异常测试 (3/3)
- [✅] 4.1 任务取消
- [✅] 4.2 无效版本
- [✅] 4.3 重复回滚

## 测试总结

通过率: 15/15 (100%)

## 发现的问题

[列出测试中发现的问题]

## 上线建议

[✅ 可以上线 / ❌ 需要修复后再测试]
```

---

## 🔧 常见问题排查

### 问题1: 升级任务一直PENDING

**可能原因**:
- @Async线程池未配置
- executeUpgradeAsync方法未被调用

**排查**:
```bash
# 查看应用日志
tail -f /tmp/flyway_phase2_app.log | grep "异步执行升级"
```

### 问题2: Flyway迁移失败

**可能原因**:
- 迁移脚本语法错误
- Schema权限不足

**排查**:
```bash
# 检查Flyway日志
psql -U postgres -d seer_fitness_edu -c "
  SELECT * FROM test_tenant_001.flyway_schema_history
  WHERE success = false;
"
```

### 问题3: 回滚后test_field列仍存在

**这是正常现象！**
- Flyway Community Edition不支持自动undo
- 回滚只更新版本记录
- 需要DBA手动执行 `ALTER TABLE sys_user DROP COLUMN test_field;`

---

**测试完成后，请填写测试报告并保存到 `docs/test/FLYWAY_TEST_REPORT.md`**
