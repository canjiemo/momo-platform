# Flyway Phase 2 实施计划

**开始时间**: 2025-10-18 15:15 CST
**目标**: 实现多租户Schema批量升级服务
**依赖**: Phase 1 已完成 ✅

---

## 📋 Phase 2 功能清单

### 核心功能

1. **SchemaUpgradeService** - 批量升级服务
   - 升级单个schema
   - 批量升级多个schema
   - 升级前验证
   - 升级后验证
   - 失败自动回滚

2. **SchemaRollbackService** - 回滚服务
   - 回滚到指定版本
   - 验证回滚安全性
   - 记录回滚操作

3. **UpgradeController** - 升级管理API
   - 执行升级任务
   - 查询升级状态
   - 查看升级历史
   - 手动回滚

4. **UpgradeTaskService** - 任务管理
   - 创建升级任务
   - 更新任务状态
   - 记录升级日志

---

## 🏗️ 数据结构设计

### 1. sys_upgrade_task (升级任务表)

已在Phase 1创建，字段说明：

```sql
CREATE TABLE public.sys_upgrade_task (
  id BIGINT PRIMARY KEY,                    -- 任务ID
  task_name VARCHAR(100) NOT NULL,          -- 任务名称
  target_version VARCHAR(20) NOT NULL,      -- 目标版本
  from_version VARCHAR(20),                 -- 起始版本
  upgrade_type VARCHAR(20),                 -- 升级类型: SINGLE/BATCH/ALL
  target_schemas TEXT,                      -- 目标schema列表(JSON数组)
  total_schemas INT DEFAULT 0,              -- 总schema数
  success_count INT DEFAULT 0,              -- 成功数量
  failed_count INT DEFAULT 0,               -- 失败数量
  status VARCHAR(20) DEFAULT 'PENDING',     -- 状态: PENDING/RUNNING/COMPLETED/FAILED/CANCELLED
  started_at TIMESTAMP,                     -- 开始时间
  completed_at TIMESTAMP,                   -- 完成时间
  created_by BIGINT,                        -- 创建人
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  delete_flag SMALLINT DEFAULT 0
);
```

### 2. sys_upgrade_task_log (升级日志表)

```sql
CREATE TABLE public.sys_upgrade_task_log (
  id BIGINT PRIMARY KEY,                    -- 日志ID
  task_id BIGINT NOT NULL,                  -- 任务ID
  schema_name VARCHAR(100) NOT NULL,        -- Schema名称
  from_version VARCHAR(20),                 -- 升级前版本
  to_version VARCHAR(20),                   -- 升级后版本
  migrations_executed INT DEFAULT 0,        -- 执行的迁移数
  status VARCHAR(20) DEFAULT 'PENDING',     -- 状态: PENDING/RUNNING/SUCCESS/FAILED/ROLLED_BACK
  error_message TEXT,                       -- 错误信息
  started_at TIMESTAMP,                     -- 开始时间
  completed_at TIMESTAMP,                   -- 完成时间
  execution_time INT DEFAULT 0,             -- 执行耗时(毫秒)
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  delete_flag SMALLINT DEFAULT 0
);
```

---

## 🔧 核心类设计

### 1. SchemaUpgradeService

```java
public interface SchemaUpgradeService {

    /**
     * 升级单个schema到指定版本
     * @param schemaName Schema名称
     * @param targetVersion 目标版本(null表示最新)
     * @return 升级结果
     */
    UpgradeResult upgradeSchema(String schemaName, String targetVersion);

    /**
     * 批量升级多个schema
     * @param schemaNames Schema名称列表
     * @param targetVersion 目标版本
     * @return 升级任务ID
     */
    Long batchUpgradeSchemas(List<String> schemaNames, String targetVersion);

    /**
     * 升级所有租户schema
     * @param targetVersion 目标版本
     * @return 升级任务ID
     */
    Long upgradeAllSchemas(String targetVersion);

    /**
     * 查询升级任务状态
     * @param taskId 任务ID
     * @return 任务状态
     */
    UpgradeTaskStatus getTaskStatus(Long taskId);

    /**
     * 取消升级任务
     * @param taskId 任务ID
     */
    void cancelTask(Long taskId);
}
```

### 2. SchemaRollbackService

```java
public interface SchemaRollbackService {

    /**
     * 回滚schema到指定版本
     * @param schemaName Schema名称
     * @param targetVersion 目标版本
     * @return 回滚结果
     */
    RollbackResult rollbackSchema(String schemaName, String targetVersion);

    /**
     * 验证回滚安全性
     * @param schemaName Schema名称
     * @param targetVersion 目标版本
     * @return 是否安全
     */
    boolean validateRollback(String schemaName, String targetVersion);

    /**
     * 获取可回滚的版本列表
     * @param schemaName Schema名称
     * @return 版本列表
     */
    List<String> getAvailableVersions(String schemaName);
}
```

### 3. UpgradeTaskService

```java
public interface UpgradeTaskService {

    /**
     * 创建升级任务
     * @param request 任务创建请求
     * @return 任务ID
     */
    Long createTask(UpgradeTaskCreateRequest request);

    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param status 新状态
     */
    void updateTaskStatus(Long taskId, String status);

    /**
     * 记录schema升级日志
     * @param log 日志对象
     */
    void logSchemaUpgrade(SysUpgradeTaskLog log);

    /**
     * 查询任务详情
     * @param taskId 任务ID
     * @return 任务详情
     */
    UpgradeTaskDetail getTaskDetail(Long taskId);
}
```

---

## 📡 API设计

### UpgradeController

```java
@RestController
@RequestMapping("/platform/upgrade")
public class UpgradeController {

    /**
     * 执行批量升级
     * POST /platform/upgrade/execute
     */
    @PostMapping("/execute")
    @RequireAuth(permissions = {"upgrade:execute"})
    public Result<Long> executeUpgrade(@RequestBody UpgradeRequest request);

    /**
     * 查询升级任务状态
     * GET /platform/upgrade/task/{taskId}
     */
    @GetMapping("/task/{taskId}")
    @RequireAuth(permissions = {"upgrade:view"})
    public Result<UpgradeTaskDetail> getTaskStatus(@PathVariable Long taskId);

    /**
     * 查询升级历史
     * POST /platform/upgrade/history
     */
    @PostMapping("/history")
    @RequireAuth(permissions = {"upgrade:view"})
    public Result<PageResult<UpgradeTaskDTO>> getUpgradeHistory(@RequestBody QueryParam param);

    /**
     * 取消升级任务
     * POST /platform/upgrade/cancel/{taskId}
     */
    @PostMapping("/cancel/{taskId}")
    @RequireAuth(permissions = {"upgrade:execute"})
    public Result<Void> cancelTask(@PathVariable Long taskId);

    /**
     * 回滚schema
     * POST /platform/upgrade/rollback
     */
    @PostMapping("/rollback")
    @RequireAuth(permissions = {"upgrade:rollback"})
    public Result<RollbackResult> rollbackSchema(@RequestBody RollbackRequest request);
}
```

---

## 🔄 升级流程设计

### 批量升级流程

```
1. 创建升级任务
   ├─ 验证目标版本存在
   ├─ 查询目标schema列表
   ├─ 创建sys_upgrade_task记录
   └─ 返回任务ID

2. 执行升级(异步)
   ├─ 更新任务状态: RUNNING
   ├─ 遍历每个schema
   │  ├─ 记录开始时间
   │  ├─ 验证当前版本
   │  ├─ 执行Flyway migrate
   │  ├─ 验证升级结果
   │  ├─ 更新sys_schema_version
   │  └─ 记录升级日志
   ├─ 统计成功/失败数量
   └─ 更新任务状态: COMPLETED/FAILED

3. 失败处理
   ├─ 记录错误信息
   ├─ 标记schema状态: FAILED
   └─ 可选：自动回滚
```

### 回滚流程

```
1. 验证回滚安全性
   ├─ 检查目标版本是否存在
   ├─ 检查是否有不可逆的迁移
   └─ 警告数据丢失风险

2. 执行回滚
   ├─ Flyway repair (修复状态)
   ├─ Flyway undo (如果支持)
   └─ 手动执行回滚脚本

3. 更新版本记录
   ├─ 更新sys_schema_version
   └─ 记录回滚日志
```

---

## 📝 实现步骤

### Step 1: 实体类和DTO (30分钟)

1. 创建实体类
   - `SysUpgradeTask.java`
   - `SysUpgradeTaskLog.java`

2. 创建DTO
   - `UpgradeRequest.java`
   - `UpgradeTaskDTO.java`
   - `UpgradeTaskDetail.java`
   - `RollbackRequest.java`
   - `RollbackResult.java`
   - `UpgradeResult.java`

### Step 2: Service实现 (60分钟)

1. `UpgradeTaskService` - 任务管理
2. `SchemaUpgradeService` - 升级逻辑
3. `SchemaRollbackService` - 回滚逻辑

### Step 3: Controller实现 (30分钟)

1. `UpgradeController` - API端点

### Step 4: 测试脚本 (30分钟)

1. 创建测试迁移脚本: `V1.1.0__add_test_column.sql`
2. 编写升级测试脚本
3. 验证批量升级
4. 验证回滚功能

### Step 5: 文档和总结 (20分钟)

1. 更新CLAUDE.md
2. 编写Phase 2测试报告
3. 提交代码

**预计总时间**: 2.5-3小时

---

## 🎯 验收标准

### 功能验收

- [ ] 单个schema升级成功
- [ ] 批量schema升级成功
- [ ] 升级任务状态正确记录
- [ ] 升级日志详细记录
- [ ] 失败schema正确标记
- [ ] 回滚功能正常工作
- [ ] API端点全部可用
- [ ] 权限控制正确

### 数据验收

- [ ] sys_upgrade_task记录正确
- [ ] sys_upgrade_task_log记录完整
- [ ] sys_schema_version正确更新
- [ ] flyway_schema_history记录正确

### 性能要求

- [ ] 单个schema升级 < 1秒
- [ ] 10个schema批量升级 < 10秒
- [ ] 升级任务异步执行不阻塞

---

## 🚀 开始实施

准备开始Step 1: 创建实体类和DTO

是否开始实施？
