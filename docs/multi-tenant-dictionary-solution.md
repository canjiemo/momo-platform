# 多租户架构 - 数据字典解决方案

## 📌 问题说明

### 当前架构存在的问题

您发现了一个重要的架构不完整点：

**Public Schema 的字典**：
```sql
-- 位置：public.sys_dict_type / public.sys_dict_data
-- 用途：平台管理（租户状态、初始化步骤等）
-- 访问：仅平台管理员可访问
```

**租户 Schema 的字典**：
```sql
-- 位置：school_001.sys_dict_type / school_001.sys_dict_data
-- 状态：表结构已创建，但数据为空！❌
-- 问题：租户无法使用字典功能（用户性别、学员状态等）
```

---

## 🎯 设计原则

### 多租户字典的两种类型

#### 1. **平台级字典**（Public Schema）
- **存储位置**：`public.sys_dict_*`
- **用途**：平台管理功能
- **示例**：租户状态、初始化步骤、订阅类型
- **访问权限**：仅平台管理员
- **是否同步到租户**：❌ 否

#### 2. **租户级字典**（Tenant Schema）
- **存储位置**：`school_xxx.sys_dict_*`
- **用途**：业务功能
- **示例**：用户性别、学员状态、课程类型、证书等级
- **访问权限**：租户用户
- **是否隔离**：✅ 完全隔离

---

## ✅ 解决方案

### 方案一：预置通用字典（推荐）

**适用场景**：所有租户使用相同的业务字典

在 `tenant_init_data.sql` 中添加通用字典数据：

```sql
-- ========================================
-- 第六部分: 通用字典数据
-- 说明：所有租户通用的业务字典
-- ========================================

-- 1. 用户性别字典
INSERT INTO sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (5001, '用户性别', 'user_gender', '用户性别：男、女、未知', 1, 1, 'system');

INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, status, sort_order, create_by)
VALUES
  (50001, 'user_gender', '男', '1', '男性', 1, 1, 'system'),
  (50002, 'user_gender', '女', '2', '女性', 1, 2, 'system'),
  (50003, 'user_gender', '未知', '0', '未知性别', 1, 3, 'system');

-- 2. 用户状态字典
INSERT INTO sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (5002, '用户状态', 'user_status', '用户账号状态', 1, 2, 'system');

INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (50011, 'user_status', '启用', '1', '账号正常使用', '', 'success', 1, 1, 'system'),
  (50012, 'user_status', '禁用', '0', '账号被禁用', '', 'danger', 1, 2, 'system');

-- 3. 学员状态字典（教育行业）
INSERT INTO sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (5003, '学员状态', 'student_status', '学员当前学习状态', 1, 3, 'system');

INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (50021, 'student_status', '在读', '1', '正在学习中', '', 'success', 1, 1, 'system'),
  (50022, 'student_status', '休学', '2', '暂停学习', '', 'warning', 1, 2, 'system'),
  (50023, 'student_status', '毕业', '3', '已完成学业', '', 'info', 1, 3, 'system'),
  (50024, 'student_status', '退学', '4', '已退学', '', 'danger', 1, 4, 'system');

-- 4. 课程状态字典
INSERT INTO sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (5004, '课程状态', 'course_status', '课程发布状态', 1, 4, 'system');

INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (50031, 'course_status', '草稿', '0', '课程编辑中', '', 'info', 1, 1, 'system'),
  (50032, 'course_status', '已发布', '1', '课程已上线', '', 'success', 1, 2, 'system'),
  (50033, 'course_status', '已下架', '2', '课程已下线', '', 'warning', 1, 3, 'system');

-- 5. 缴费状态字典
INSERT INTO sys_dict_type (id, dict_name, dict_type, dict_description, status, sort_order, create_by)
VALUES (5005, '缴费状态', 'payment_status', '学员缴费状态', 1, 5, 'system');

INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by)
VALUES
  (50041, 'payment_status', '待缴费', '0', '尚未缴费', '', 'warning', 1, 1, 'system'),
  (50042, 'payment_status', '已缴费', '1', '已全额缴费', '', 'success', 1, 2, 'system'),
  (50043, 'payment_status', '部分缴费', '2', '已缴纳部分费用', '', 'info', 1, 3, 'system'),
  (50044, 'payment_status', '已退费', '3', '已办理退费', '', 'danger', 1, 4, 'system');
```

**优点**：
- ✅ 新租户自动拥有通用字典
- ✅ 所有租户规范统一
- ✅ 开发者无需额外操作

**缺点**：
- ❌ 租户无法完全自定义
- ❌ 字典修改需要重新初始化

---

### 方案二：字典同步服务（灵活）

**适用场景**：租户需要自定义字典，但希望有基础模板

创建一个字典同步服务：

```java
/**
 * 字典同步服务
 * 用于将 Public Schema 的通用字典同步到租户 Schema
 */
@Service
@Slf4j
public class DictionarySyncService {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * 同步通用字典到租户 Schema
     * @param schemaName 租户Schema名称
     */
    public void syncCommonDictionaries(String schemaName) {
        log.info("开始同步通用字典到租户Schema: {}", schemaName);

        try {
            // 1. 切换到租户Schema
            String switchSql = "SET search_path TO " + schemaName;
            jdbcTemplate.getJdbcTemplate().execute(switchSql);

            // 2. 从Public Schema查询通用字典类型
            String queryTypeSql = "SELECT * FROM public.sys_dict_type " +
                                "WHERE dict_type IN ('user_gender', 'user_status', 'student_status', 'course_status', 'payment_status')";

            List<Map<String, Object>> dictTypes = jdbcTemplate.getJdbcTemplate().queryForList(queryTypeSql);

            // 3. 插入字典类型
            for (Map<String, Object> type : dictTypes) {
                String insertTypeSql = "INSERT INTO sys_dict_type " +
                    "(id, dict_name, dict_type, dict_description, status, sort_order, create_by) " +
                    "VALUES (:id, :dict_name, :dict_type, :dict_description, :status, :sort_order, :create_by) " +
                    "ON CONFLICT (dict_type) DO NOTHING";

                jdbcTemplate.update(insertTypeSql, type);
            }

            // 4. 查询并插入字典数据
            String queryDataSql = "SELECT * FROM public.sys_dict_data " +
                                "WHERE dict_type IN ('user_gender', 'user_status', 'student_status', 'course_status', 'payment_status')";

            List<Map<String, Object>> dictData = jdbcTemplate.getJdbcTemplate().queryForList(queryDataSql);

            for (Map<String, Object> data : dictData) {
                String insertDataSql = "INSERT INTO sys_dict_data " +
                    "(id, dict_type, dict_label, dict_value, dict_description, css_class, list_class, status, sort_order, create_by) " +
                    "VALUES (:id, :dict_type, :dict_label, :dict_value, :dict_description, :css_class, :list_class, :status, :sort_order, :create_by) " +
                    "ON CONFLICT (id) DO NOTHING";

                jdbcTemplate.update(insertDataSql, data);
            }

            // 5. 切换回Public Schema
            jdbcTemplate.getJdbcTemplate().execute("SET search_path TO public");

            log.info("通用字典同步成功: schemaName={}", schemaName);

        } catch (Exception e) {
            log.error("通用字典同步失败: schemaName={}", schemaName, e);
            throw new BusinessException("字典同步失败: " + e.getMessage());
        }
    }
}
```

然后在租户创建时调用：

```java
// TenantSchemaService.java 中添加
@Override
public void createSchemaAndInitTables(...) {
    // ... 现有代码 ...

    // 步骤5：同步通用字典（新增）
    logStep(tenantId, InitStepType.SYNC_DICT, "开始同步通用字典", 0);
    dictionarySyncService.syncCommonDictionaries(schemaName);
    logStep(tenantId, InitStepType.SYNC_DICT, "通用字典同步成功", 1);
}
```

**优点**：
- ✅ 租户可以自定义修改字典
- ✅ 支持从 Public 同步更新
- ✅ 灵活性高

**缺点**：
- ❌ 实现复杂
- ❌ 需要维护同步逻辑

---

## 📝 开发者最佳实践

### 1. **查询字典数据**

开发者访问字典时，**自动路由到租户Schema**，无需特殊处理：

```java
@Service
public class DictDataService {

    // 查询字典数据 - 自动路由到租户Schema
    public List<DictDataDTO> getByType(String dictType) {
        String sql = "SELECT * FROM sys_dict_data WHERE dict_type = :dictType";
        Map<String, Object> params = Maps.newHashMap();
        params.put("dictType", dictType);

        // 由于 TenantInterceptor 已经设置了 TenantContext
        // 数据库查询会自动路由到租户的 Schema
        return baseDao.queryListForSql(sql, params, DictDataDTO.class);
    }
}
```

### 2. **创建自定义字典**

租户可以通过字典管理界面创建自己的字典：

```java
// 租户用户通过 /dict/type/create 接口创建字典
POST /dict/type/create
{
  "dictType": "school_badge_level",  // 学校自定义：徽章等级
  "dictName": "徽章等级",
  "dictDescription": "学员徽章等级"
}

// 数据会自动插入到租户的 Schema 中
// INSERT INTO school_001.sys_dict_type ...
```

### 3. **跨Schema查询（不推荐）**

如果确实需要查询 Public Schema 的字典：

```java
// 明确指定 schema
String sql = "SELECT * FROM public.sys_dict_data WHERE dict_type = :dictType";
```

---

## 🚀 推荐实施步骤

### 立即实施：添加通用字典到租户初始化

1. **编辑** `tenant_init_data.sql`
2. **在文件末尾添加**通用字典数据（参考上面的SQL）
3. **新建租户**会自动包含这些字典
4. **已有租户**需要手动执行同步脚本

### 可选实施：字典同步服务

如果需要灵活的字典管理，可以实施方案二的同步服务。

---

## 📋 通用字典清单（建议）

### 基础字典（必须）
- ✅ 用户性别 (`user_gender`)
- ✅ 用户状态 (`user_status`)
- ✅ 是否标识 (`yes_no`)
- ✅ 启用状态 (`enable_status`)

### 教育行业字典
- ✅ 学员状态 (`student_status`)
- ✅ 课程状态 (`course_status`)
- ✅ 缴费状态 (`payment_status`)
- ✅ 证书类型 (`certificate_type`)
- ✅ 班级类型 (`class_type`)

### 健身行业字典（Seer Fitness Edu）
- ⬜ 会员类型 (`member_type`)
- ⬜ 课程难度 (`course_difficulty`)
- ⬜ 训练目标 (`training_goal`)
- ⬜ 体测指标 (`fitness_metric`)

---

## ⚡ 快速修复指南

### 为现有租户添加字典数据

如果已经创建了租户，可以手动执行：

```sql
-- 1. 切换到租户 Schema
SET search_path TO school_001;

-- 2. 执行字典初始化SQL（参考上面的示例）
INSERT INTO sys_dict_type (...);
INSERT INTO sys_dict_data (...);

-- 3. 重复步骤1-2为其他租户执行
```

或者创建一个批量同步脚本：

```java
// 创建一个临时的Controller方法
@PostMapping("/admin/sync-all-dictionaries")
@RequireAuth(adminFlag = true)
public MyResponseResult syncAllDictionaries() {
    List<TenantDTO> tenants = tenantService.getAllActiveTenants();

    for (TenantDTO tenant : tenants) {
        try {
            dictionarySyncService.syncCommonDictionaries(tenant.getSchemaName());
            log.info("字典同步成功: {}", tenant.getTenantCode());
        } catch (Exception e) {
            log.error("字典同步失败: {}", tenant.getTenantCode(), e);
        }
    }

    return doJsonMsg("批量同步完成");
}
```

---

## 🎓 总结

**当前问题**：
- 租户Schema有字典表结构，但数据为空

**推荐方案**：
- **方案一（简单）**：在 `tenant_init_data.sql` 中添加通用字典
- **方案二（灵活）**：实现字典同步服务

**开发者注意**：
- 租户字典会自动路由到租户Schema，无需特殊处理
- 可以通过字典管理界面创建租户自定义字典
- Public Schema的字典仅用于平台管理，租户无法访问

---

**下一步建议**：
1. ✅ 确定需要哪些通用字典
2. ✅ 编辑 `tenant_init_data.sql` 添加字典数据
3. ✅ 测试新建租户是否包含字典
4. ✅ 为现有租户执行字典同步脚本
