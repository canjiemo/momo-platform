# 🚀 快速开始 - 安全测试

## 一键运行测试

```bash
# 1. 启动应用
mvn spring-boot:run -pl seer-fitness-boot

# 2. 导入测试数据
psql -U postgres -d seer_fitness_edu -f docs/test/test_roles_menus.sql

# 3. 运行测试
bash docs/test/security_test.sh
```

## 📋 测试前检查清单

### ✅ 必须完成

- [ ] 应用运行在 `http://localhost:8080`
- [ ] PostgreSQL 数据库运行正常
- [ ] Redis 服务运行正常
- [ ] 测试数据已导入

### ⚙️ 配置验证码（二选一）

**选项A: 禁用验证码（推荐）**

编辑 `application.yml`:
```yaml
captcha:
  enabled: false
```

**选项B: 手动处理验证码**

测试时从Redis获取验证码：
```bash
redis-cli GET "captcha:{captchaId}"
```

## 🔑 测试账号

**统一密码**: `Aa123456!`

| 用户名 | 说明 |
|--------|------|
| `superadmin` | 超级管理员（admin_flag=1，绕过所有权限检查） |
| `sysadmin` | 系统管理员（全部30个权限） |
| `readonly` | 只读用户（所有view权限） |
| `noperm` | 无权限用户（测试拒绝访问） |

## 🧪 测试命令

### 运行示例测试（10个）
```bash
bash docs/test/security_test.sh
```

### 运行完整测试（120个）
```bash
bash docs/test/complete_120_tests.sh
```

### 查看测试报告
```bash
cat docs/test/security_test_report.md
```

## 🔧 常见问题

### 1. 验证码错误

**现象**: `{"code":400,"msg":"验证码错误或已过期"}`

**解决**:
```yaml
# 在 application.yml 中禁用验证码
captcha:
  enabled: false
```

### 2. 账户锁定

**现象**: `{"code":400,"msg":"账户已被锁定29分钟..."}`

**解决**:
```bash
redis-cli DEL "account:lock:superadmin"
```

### 3. Token获取失败

**现象**: `Token获取失败`

**解决**:
1. 检查用户名密码是否正确（`Aa123456!`）
2. 确认账户未被锁定
3. 查看应用日志排查问题

## 📊 测试覆盖

- **120个测试用例** 全覆盖
- **30个权限点** 全覆盖
- **6个功能模块** 全覆盖
- **自动化率** 100%

## 📖 详细文档

- [完整使用文档](README.md)
- [120个测试用例设计](comprehensive_test_plan.md)
- [测试执行总结](FINAL_TEST_SUMMARY.md)

## 🎯 测试流程

```
1. 获取验证码
   ↓
2. 登录获取Token
   ↓
3. 使用Token访问API
   ↓
4. 验证响应码和数据
   ↓
5. 生成测试报告
```

## 💡 提示

- 首次测试建议**禁用验证码**
- 测试前**清理Redis缓存**避免干扰
- 账户锁定后可**手动解锁**
- 定期**重置测试数据**保证环境一致

---

**需要帮助?** 查看 [README.md](README.md) 获取详细说明
