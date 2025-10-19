#!/bin/bash

# Flyway自动化测试脚本
set -e

echo "========================================="
echo "Flyway功能测试开始"
echo "========================================="

# 1. 获取验证码
echo "1. 获取验证码..."
CAPTCHA_RESPONSE=$(curl -s http://localhost:8070/auth/captcha)
CAPTCHA_ID=$(echo "$CAPTCHA_RESPONSE" | jq -r '.data.captchaId')
echo "Captcha ID: $CAPTCHA_ID"

# 2. 从Redis获取验证码
echo "2. 从Redis获取验证码..."
CAPTCHA_CODE=$(redis-cli GET "captcha:$CAPTCHA_ID")
echo "Captcha Code: $CAPTCHA_CODE"

# 3. 登录获取Token
echo "3. 登录获取Token..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8070/auth/login \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"admin\",
    \"password\": \"Aa123456!\",
    \"captchaId\": \"$CAPTCHA_ID\",
    \"captcha\": \"$CAPTCHA_CODE\"
  }")

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.token')
echo "Token: $TOKEN"
AUTH_TOKEN="Bearer $TOKEN"

echo ""
echo "========================================="
echo "Phase 1: 基础功能测试"
echo "========================================="

# 测试1.1: 创建测试租户001
echo ""
echo "测试1.1: 创建测试租户001..."
TENANT_001_RESPONSE=$(curl -s -X POST http://localhost:8070/platform/tenant/create \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantName": "测试租户001",
    "tenantCode": "test_tenant_001",
    "contactPerson": "测试负责人",
    "contactPhone": "13800138000",
    "status": 1
  }')

echo "响应: $TENANT_001_RESPONSE"
TENANT_001_ID=$(echo "$TENANT_001_RESPONSE" | jq -r '.data.id')
echo "租户ID: $TENANT_001_ID"

# 测试1.2: 验证Schema状态
echo ""
echo "测试1.2: 验证Schema状态..."
psql -U postgres -d seer_fitness_edu -c "
  SELECT schema_name, current_version, flyway_version, baseline_version
  FROM public.sys_schema_version
  WHERE schema_name = 'test_tenant_001';"

psql -U postgres -d seer_fitness_edu -c "
  SELECT version, description, type, success
  FROM test_tenant_001.flyway_schema_history
  ORDER BY installed_rank;"

# 测试1.3: 执行单个Schema升级到V1.1.0
echo ""
echo "测试1.3: 执行单个Schema升级到V1.1.0..."
UPGRADE_RESPONSE=$(curl -s -X POST http://localhost:8070/platform/upgrade/execute \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "taskName": "测试升级tenant_001到V1.1.0",
    "upgradeType": "SINGLE",
    "targetSchemas": ["test_tenant_001"],
    "targetVersion": "1.1.0"
  }')

echo "响应: $UPGRADE_RESPONSE"
TASK_ID=$(echo "$UPGRADE_RESPONSE" | jq -r '.data')
echo "任务ID: $TASK_ID"

# 等待升级完成
echo "等待升级完成..."
sleep 5

# 测试1.4: 查询升级任务状态
echo ""
echo "测试1.4: 查询升级任务状态..."
TASK_STATUS=$(curl -s -X GET "http://localhost:8070/platform/upgrade/task/$TASK_ID" \
  -H "Authorization: $AUTH_TOKEN")
echo "任务状态: $TASK_STATUS"

# 测试1.5: 验证升级结果
echo ""
echo "测试1.5: 验证升级结果..."
psql -U postgres -d seer_fitness_edu -c "
  SELECT schema_name, current_version, last_migration_at
  FROM public.sys_schema_version
  WHERE schema_name = 'test_tenant_001';"

psql -U postgres -d seer_fitness_edu -c "
  SELECT version, description, type, success
  FROM test_tenant_001.flyway_schema_history
  ORDER BY installed_rank;"

psql -U postgres -d seer_fitness_edu -c "
  SELECT column_name, data_type
  FROM information_schema.columns
  WHERE table_schema = 'test_tenant_001'
    AND table_name = 'sys_user'
    AND column_name = 'test_field';"

echo ""
echo "========================================="
echo "Phase 2: 批量升级测试"
echo "========================================="

# 测试2.1: 批量创建测试租户
echo ""
echo "测试2.1: 批量创建测试租户002-004..."

for i in {2..4}; do
  echo "创建测试租户00$i..."
  TENANT_CODE=$(printf "test_tenant_%03d" $i)
  PHONE=$(printf "1380013800%d" $i)

  curl -s -X POST http://localhost:8070/platform/tenant/create \
    -H "Authorization: $AUTH_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"tenantName\": \"测试租户00$i\",
      \"tenantCode\": \"$TENANT_CODE\",
      \"contactPerson\": \"测试负责人\",
      \"contactPhone\": \"$PHONE\",
      \"status\": 1
    }" | jq

  sleep 2
done

# 测试2.2: 批量升级Schema
echo ""
echo "测试2.2: 批量升级3个Schema..."
BATCH_UPGRADE_RESPONSE=$(curl -s -X POST http://localhost:8070/platform/upgrade/execute \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "taskName": "批量升级3个测试租户到V1.1.0",
    "upgradeType": "BATCH",
    "targetSchemas": ["test_tenant_002", "test_tenant_003", "test_tenant_004"],
    "targetVersion": "1.1.0"
  }')

echo "响应: $BATCH_UPGRADE_RESPONSE"
BATCH_TASK_ID=$(echo "$BATCH_UPGRADE_RESPONSE" | jq -r '.data')
echo "批量任务ID: $BATCH_TASK_ID"

# 测试2.3: 监控批量升级进度
echo ""
echo "测试2.3: 监控批量升级进度..."
for i in {1..10}; do
  echo "=== 第${i}次查询 ==="
  curl -s -X GET "http://localhost:8070/platform/upgrade/task/$BATCH_TASK_ID" \
    -H "Authorization: $AUTH_TOKEN" | jq '.data | {status, successCount, failedCount}'
  sleep 2
done

# 测试2.4: 查询升级历史
echo ""
echo "测试2.4: 查询升级历史..."
curl -s -X POST http://localhost:8070/platform/upgrade/history \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "pageNum": 1,
    "pageSize": 10
  }' | jq

echo ""
echo "========================================="
echo "Phase 3: 回滚测试"
echo "========================================="

# 测试3.1: 查询可回滚版本
echo ""
echo "测试3.1: 查询可回滚版本..."
curl -s -X GET "http://localhost:8070/platform/upgrade/versions/test_tenant_001" \
  -H "Authorization: $AUTH_TOKEN" | jq

# 测试3.2: 执行Schema回滚到V1.0.0
echo ""
echo "测试3.2: 执行Schema回滚到V1.0.0..."
ROLLBACK_RESPONSE=$(curl -s -X POST http://localhost:8070/platform/upgrade/rollback \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "schemaName": "test_tenant_001",
    "targetVersion": "1.0.0"
  }')

echo "响应: $ROLLBACK_RESPONSE"

# 测试3.3: 验证回滚结果
echo ""
echo "测试3.3: 验证回滚结果..."
psql -U postgres -d seer_fitness_edu -c "
  SELECT schema_name, current_version
  FROM public.sys_schema_version
  WHERE schema_name = 'test_tenant_001';"

psql -U postgres -d seer_fitness_edu -c "
  SELECT schema_name, from_version, to_version, rollback_flag
  FROM public.sys_schema_version_history
  WHERE schema_name = 'test_tenant_001'
  ORDER BY created_at DESC
  LIMIT 1;"

echo ""
echo "========================================="
echo "Phase 4: 异常场景测试"
echo "========================================="

# 测试4.1: 测试任务取消
echo ""
echo "测试4.1: 测试任务取消..."
CANCEL_TASK_RESPONSE=$(curl -s -X POST http://localhost:8070/platform/upgrade/execute \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "taskName": "测试取消的任务",
    "upgradeType": "BATCH",
    "targetSchemas": ["test_tenant_002", "test_tenant_003"],
    "targetVersion": "1.1.0"
  }')

CANCEL_TASK_ID=$(echo "$CANCEL_TASK_RESPONSE" | jq -r '.data')
echo "取消任务ID: $CANCEL_TASK_ID"

# 立即取消任务
curl -s -X POST "http://localhost:8070/platform/upgrade/cancel/$CANCEL_TASK_ID" \
  -H "Authorization: $AUTH_TOKEN" | jq

# 查询任务状态
curl -s -X GET "http://localhost:8070/platform/upgrade/task/$CANCEL_TASK_ID" \
  -H "Authorization: $AUTH_TOKEN" | jq '.data.status'

# 测试4.2: 测试无效版本回滚
echo ""
echo "测试4.2: 测试无效版本回滚..."
curl -s -X POST http://localhost:8070/platform/upgrade/rollback \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "schemaName": "test_tenant_001",
    "targetVersion": "9.9.9"
  }' | jq

# 测试4.3: 测试重复回滚
echo ""
echo "测试4.3: 测试重复回滚..."
curl -s -X POST http://localhost:8070/platform/upgrade/rollback \
  -H "Authorization: $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "schemaName": "test_tenant_001",
    "targetVersion": "1.0.0"
  }' | jq

echo ""
echo "========================================="
echo "Flyway功能测试完成!"
echo "========================================="
