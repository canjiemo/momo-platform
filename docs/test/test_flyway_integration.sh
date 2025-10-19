#!/bin/bash

# ==============================================================================
# Flyway集成测试脚本
# 目的：验证新租户创建时Flyway自动初始化是否正常工作
# ==============================================================================

set -e  # 遇到错误立即退出

BASE_URL="http://localhost:8070"
TIMESTAMP=$(date +%s)
TENANT_CODE="FLYWAY_TEST_${TIMESTAMP}"
TENANT_NAME="Flyway测试租户_${TIMESTAMP}"
SCHEMA_NAME="flyway_test_${TIMESTAMP}"

echo "========================================="
echo "Flyway集成测试"
echo "========================================="
echo "租户代码: $TENANT_CODE"
echo "租户名称: $TENANT_NAME"
echo "Schema名称: $SCHEMA_NAME"
echo ""

# 步骤1: 获取验证码
echo "[步骤1] 获取验证码..."
CAPTCHA_RESPONSE=$(curl -s "${BASE_URL}/auth/captcha")
CAPTCHA_ID=$(echo $CAPTCHA_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['data']['captchaId'])" 2>/dev/null)

if [ -z "$CAPTCHA_ID" ]; then
    echo "❌ 获取验证码失败"
    exit 1
fi

echo "✅ 验证码ID: $CAPTCHA_ID"

# 步骤2: 从Redis获取验证码
echo "[步骤2] 从Redis读取验证码..."
CAPTCHA_CODE=$(redis-cli GET "captcha:$CAPTCHA_ID" 2>/dev/null)

if [ -z "$CAPTCHA_CODE" ]; then
    echo "❌ Redis中未找到验证码"
    exit 1
fi

echo "✅ 验证码: $CAPTCHA_CODE"

# 步骤3: 登录获取Token
echo "[步骤3] 使用superadmin登录..."
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"superadmin\",\"password\":\"Aa123456!\",\"captchaId\":\"$CAPTCHA_ID\",\"captcha\":\"$CAPTCHA_CODE\"}")

TOKEN=$(echo $LOGIN_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin).get('data', {}).get('token', ''))" 2>/dev/null)

if [ -z "$TOKEN" ]; then
    echo "❌ 登录失败"
    echo "响应: $LOGIN_RESPONSE"
    exit 1
fi

echo "✅ 登录成功，Token: ${TOKEN:0:20}..."

# 步骤4: 创建租户
echo "[步骤4] 创建新租户..."
CREATE_TENANT_RESPONSE=$(curl -s -X POST "${BASE_URL}/system/tenant/create" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"tenantCode\":\"$TENANT_CODE\",
    \"tenantName\":\"$TENANT_NAME\",
    \"schemaName\":\"$SCHEMA_NAME\",
    \"adminUsername\":\"admin_${TIMESTAMP}\",
    \"adminRealName\":\"测试管理员\",
    \"adminPassword\":\"Aa123456!\",
    \"contactPhone\":\"13800000001\",
    \"description\":\"Flyway集成测试租户\"
  }")

echo "响应: $CREATE_TENANT_RESPONSE"

TENANT_ID=$(echo $CREATE_TENANT_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin).get('data', {}).get('id', ''))" 2>/dev/null)

if [ -z "$TENANT_ID" ]; then
    echo "❌ 创建租户失败"
    exit 1
fi

echo "✅ 租户创建成功，ID: $TENANT_ID"
echo ""

# 等待租户初始化完成
echo "[等待] 租户初始化中，等待10秒..."
sleep 10

# 步骤5: 验证flyway_schema_history表
echo "[步骤5] 验证flyway_schema_history表..."
FLYWAY_TABLE_CHECK=$(psql -U postgres -d fitness-edu -t -c "
SELECT tablename
FROM pg_tables
WHERE schemaname = '$SCHEMA_NAME'
  AND tablename = 'flyway_schema_history';
" 2>/dev/null | xargs)

if [ "$FLYWAY_TABLE_CHECK" = "flyway_schema_history" ]; then
    echo "✅ flyway_schema_history表已创建"

    # 查询Flyway历史记录
    FLYWAY_RECORDS=$(psql -U postgres -d fitness-edu -t -c "
    SELECT version, description, success
    FROM $SCHEMA_NAME.flyway_schema_history
    ORDER BY installed_rank;
    " 2>/dev/null)

    echo "Flyway迁移记录:"
    echo "$FLYWAY_RECORDS"
else
    echo "❌ flyway_schema_history表未创建"
fi

# 步骤6: 验证sys_schema_version记录
echo ""
echo "[步骤6] 验证sys_schema_version记录..."
VERSION_RECORD=$(psql -U postgres -d fitness-edu -t -c "
SELECT
    schema_name,
    current_version,
    flyway_version,
    is_baseline,
    baseline_version
FROM public.sys_schema_version
WHERE schema_name = '$SCHEMA_NAME'
  AND delete_flag = 0;
" 2>/dev/null)

if [ -n "$VERSION_RECORD" ]; then
    echo "✅ sys_schema_version记录已创建"
    echo "版本信息: $VERSION_RECORD"
else
    echo "❌ sys_schema_version记录未找到"
fi

# 步骤7: 验证租户Schema中的表
echo ""
echo "[步骤7] 验证租户Schema中的表..."
TENANT_TABLES=$(psql -U postgres -d fitness-edu -t -c "
SELECT tablename
FROM pg_tables
WHERE schemaname = '$SCHEMA_NAME'
ORDER BY tablename;
" 2>/dev/null)

TABLE_COUNT=$(echo "$TENANT_TABLES" | wc -l | xargs)

echo "✅ 租户Schema中共有 $TABLE_COUNT 张表:"
echo "$TENANT_TABLES"

# 步骤8: 验证租户初始化日志
echo ""
echo "[步骤8] 验证租户初始化日志..."
INIT_LOGS=$(psql -U postgres -d fitness-edu -t -c "
SELECT
    step_type,
    step_desc,
    status,
    created_at
FROM public.sys_tenant_init_log
WHERE tenant_id = $TENANT_ID
ORDER BY created_at;
" 2>/dev/null)

echo "初始化日志:"
echo "$INIT_LOGS"

# 检查是否有INIT_FLYWAY步骤
if echo "$INIT_LOGS" | grep -q "INIT_FLYWAY"; then
    echo "✅ INIT_FLYWAY步骤已执行"
else
    echo "❌ INIT_FLYWAY步骤未找到"
fi

# 总结
echo ""
echo "========================================="
echo "测试总结"
echo "========================================="
echo "租户ID: $TENANT_ID"
echo "Schema名称: $SCHEMA_NAME"
echo "表数量: $TABLE_COUNT"
echo ""
echo "✅ Flyway集成测试完成！"
echo ""
echo "清理命令（如需清理测试数据）:"
echo "  DROP SCHEMA $SCHEMA_NAME CASCADE;"
echo "  DELETE FROM public.sys_tenant WHERE id = $TENANT_ID;"
echo "  DELETE FROM public.sys_schema_version WHERE schema_name = '$SCHEMA_NAME';"
echo "  DELETE FROM public.sys_tenant_init_log WHERE tenant_id = $TENANT_ID;"
