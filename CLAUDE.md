# CLAUDE.md — Seer Fitness Edu

> 本文件记录项目的架构决策、开发规范和踩坑记录，供 AI 辅助开发时参考。
> 代码可以自己读，这里只写**不读代码无法推断**的内容。

---

## 项目概览

**先知智慧体育-校园** — 校园健身教育多租户管理系统。

- Spring Boot 3.5.6 + Java 21 + Maven 多模块
- PostgreSQL + Redis
- MyJPA 1.0-jdk21（自定义 ORM）+ MyMVC 1.0-jdk21
- JWT 认证 + BCrypt 密码 + Druid 连接池 + Undertow 服务器

### 模块结构

```
seer-fitness-edu/
├── seer-fitness-framework/   基础设施层（实体、注解、工具类、配置 Bean）
├── seer-fitness-system/      系统层（用户/角色/菜单/组织/字典/配置管理）
├── seer-fitness-business/    业务层（体育业务功能）
└── seer-fitness-boot/        启动层（main、yml、SQL 脚本）
```

framework 包含：`UserCacheInfo`、`SecurityContextUtil`、`RedisUtil`、`JwtUtil`、`PasswordUtil`、
`LockTimeCalculator`、`LockMessageBuilder`、`@RequireAuth`、`@OperationLog`、各 Config Bean。

---

## 多租户架构

**模式**：tenant_id 单 schema（已废弃 Schema 隔离方案，勿再提）。

| tenant_id | 含义 |
|---|---|
| NULL | 平台级数据（超管用户、平台菜单、平台角色） |
| 具体值 | 租户数据 |

**有 tenant_id 的表**：`sys_user`、`sys_role`、`sys_menu`、`sys_role_menu`、`sys_user_role`、
`sys_organization`、`sys_operation_log`、`sys_dict_type`、`sys_dict_data`、`sys_config`

**无 tenant_id 的表**：`sys_tenant`（平台元数据，不参与租户隔离）

### TenantIdProvider

```java
// TenantConfig.java
@Bean
public TenantIdProvider tenantIdProvider() {
    return () -> {
        UserCacheInfo user = SecurityContextUtil.getCurrentUser();
        if (user == null) return null;
        // 平台超管 adminFlag=1 且 tenantId=null → 返回 null → 看全部数据
        if (user.getAdminFlag() == 1 && user.getTenantId() == null) return null;
        return user.getTenantId();
    };
}
```

**关键推论**：平台管理员的 TenantIdProvider 返回 null，myjpa 不注入任何 tenant_id 条件。
因此平台管理员查询特定租户数据时，直接 `.eq(Entity::getTenantId, tenantId)` 即可，
**不需要 `TenantContext.withoutTenant()`**。

---

## MyJPA — 查询 API

### 优先级规则

**单表查询 → 必须用 `lambdaQuery`**，禁止手写 SQL。
**多表 JOIN / 复杂聚合 → 用 raw SQL**（`queryListForSql` 等）。

框架自动注入 `delete_flag` 和 `tenant_id` 条件，SQL 中不要手写这两个条件。

### lambdaQuery API

```java
// 两种入口
lambdaQuery(SysUser.class)                 // 返回 List<SysUser>
lambdaQuery(SysUser.class, UserDTO.class)  // 返回 List<UserDTO>

// 条件方法（全部 AND）
.eq(fn, val)         // =
.ne(fn, val)         // !=
.gt(fn, val)         // >
.ge(fn, val)         // >=（可选下界，null 自动跳过）
.lt(fn, val)         // <
.le(fn, val)         // <=（可选上界，null 自动跳过）
.like(fn, val)       // LIKE '%val%'
.likeLeft(fn, val)   // LIKE '%val'
.likeRight(fn, val)  // LIKE 'val%'
.in(fn, collection)
.notIn(fn, collection)
.between(fn, v1, v2) // 两端同时非 null 才追加，否则整体跳过
.isNull(fn)          // 始终追加，不受跳过规则影响
.isNotNull(fn)       // 始终追加，不受跳过规则影响
.orderByAsc(fn)
.orderByDesc(fn)

// 终结方法
.list()    // List<R>
.one()     // R（取第一条，不存在返回 null）
.count()   // long
.page(pager)   // Pager<R>
.exists()  // boolean，替代 COUNT(*) > 0
```

**自动跳过规则**：val 为 `null`、空白字符串、空集合时该条件跳过。
整数 `0`、`false` 不跳过。`isNull`/`isNotNull` 不受此规则影响，始终追加。

**时间范围查询**：两端各自可选时，用 `ge` + `le` 分开，不要用 `between`：

```java
// ✅ 正确：startTime 和 endTime 各自独立可选
.ge(SysLog::getCreateTime, param.getStartTime())
.le(SysLog::getCreateTime, param.getEndTime())

// ❌ 错误：任一端为 null 则整体条件被忽略
.between(SysLog::getCreateTime, param.getStartTime(), param.getEndTime())
```

### 常用 lambdaQuery 模式

```java
// 分页搜索（条件自动跳过 null，无需 if 判断）
return lambdaQuery(SysUser.class, UserDTO.class)
        .like(SysUser::getUsername, param.getUsername())
        .eq(SysUser::getStatus, param.getStatus())   // status=0 不跳过，null 才跳过
        .orderByDesc(SysUser::getCreateTime)
        .page(pager);

// 平台数据查询（加 isNull）
return lambdaQuery(SysRole.class, RoleDTO.class)
        .isNull(SysRole::getTenantId)
        .eq(SysRole::getStatus, 1)
        .list();

// 唯一性校验（含排除 ID）
var q = lambdaQuery(SysRole.class).eq(SysRole::getRoleCode, roleCode).isNull(SysRole::getTenantId);
if (excludeId != null) q.ne(SysRole::getId, excludeId);
return q.exists();

// 平台管理员查指定租户数据（直接 eq，无需 TenantContext）
var q = lambdaQuery(SysRole.class, RoleDTO.class)
        .like(SysRole::getRoleName, param.getRoleName());
if (SecurityContextUtil.isPlatformAdmin() && param.getTenantId() != null) {
    q.eq(SysRole::getTenantId, param.getTenantId());
}
return q.page(pager);
```

### raw SQL（多表场景）

```java
// 命名参数用 :key 形式，不支持 ?
Map<String, Object> params = new HashMap<>();
params.put("userId", userId);
String sql = "SELECT r.* FROM sys_role r INNER JOIN sys_user_role ur ON r.id = ur.role_id WHERE ur.user_id = :userId";

List<RoleDTO> list = baseDao.queryListForSql(sql, params, RoleDTO.class);
SysUser user = baseDao.querySingleForSql(sql, params, SysUser.class);
Pager<UserDTO> page = baseDao.queryPageForSql(sql, params, pager, UserDTO.class);
```

### 增删改

```java
baseDao.insertPO(entity, true);          // true = 用数据库序列自动生成 ID
baseDao.updatePO(entity);               // 更新全部字段
baseDao.updatePO(entity, true);         // ignoreNull=true 忽略 null 字段
baseDao.delByIds(SysUser.class, id);    // 逻辑删除（有 deleteFlag）/ 物理删除（无）
baseDao.delPO(entity);                  // 同上，单条版
```

---

## 认证与授权

### 获取当前用户

```java
// framework 包路径
import com.seer.fitness.framework.utils.SecurityContextUtil;
import com.seer.fitness.framework.model.UserCacheInfo;

UserCacheInfo user = SecurityContextUtil.getCurrentUser();
Long userId   = user.getUserId();
Long tenantId = user.getTenantId();   // 平台管理员为 null
boolean isPlatformAdmin = SecurityContextUtil.isPlatformAdmin();

// 永远不要硬编码用户 ID
```

### 权限注解

```java
@RequireAuth(login = false)                         // 公开接口
@RequireAuth(login = true)                          // 仅需登录
@RequireAuth(permissions = {"user:create"})         // 需要特定权限
// 超管 adminFlag=1 自动绕过权限检查
```

### 关键 Redis Key

```
user:token:{tokenId}    用户会话缓存（UserCacheInfo）
captcha:{captchaId}     验证码
config:{configKey}      系统配置缓存（无 TTL）
dict:{type}:{value}     字典描述缓存
```

---

## 系统配置管理（sys_config）

所有动态配置存储在 `sys_config` 表，通过 Redis 缓存（无 TTL），启动时全量预热。

### ConfigUtil 读取配置

```java
import com.seer.fitness.system.utils.ConfigUtil;
import com.seer.fitness.system.constants.ConfigKeys;

ConfigUtil.getString(ConfigKeys.PASSWORD_INITIAL, "Aa123456!")
ConfigUtil.getInt(ConfigKeys.LOCK_MAX_FAIL_COUNT, 5)
ConfigUtil.getBoolean(ConfigKeys.LOCK_ENABLED, true)
ConfigUtil.getList(ConfigKeys.LOCK_WHITELIST_USERS)  // 逗号分隔 → List<String>
```

### ConfigKeys 常量（完整列表）

```java
// 验证码
CAPTCHA_ENABLED / CAPTCHA_EXPIRE_SECONDS / CAPTCHA_LENGTH / CAPTCHA_TYPE

// 密码策略
PASSWORD_INITIAL / PASSWORD_MIN_LENGTH / PASSWORD_MAX_LENGTH
PASSWORD_REQUIRE_LOWERCASE / PASSWORD_REQUIRE_UPPERCASE
PASSWORD_REQUIRE_DIGIT / PASSWORD_REQUIRE_SPECIAL

// 账户锁定
LOCK_ENABLED / LOCK_MAX_FAIL_COUNT / LOCK_AUTO_RESET_HOURS / LOCK_BASE_MINUTES
LOCK_IP_ENABLED / LOCK_IP_MAX_ATTEMPTS / LOCK_IP_LOCK_MINUTES / LOCK_IP_RECORD_HOURS
LOCK_RESET_ON_SUCCESS / LOCK_WHITELIST_USERS / LOCK_WHITELIST_IPS

// 其他
SCHEDULER_POOL_SIZE
```

**新增配置项**：同时在 `ConfigKeys`、`sys_config` 表（SQL 脚本）、调用处三处添加。

---

## 数据字典（@MyDict）

### 用法

```java
import io.github.canjiemo.tools.dict.MyDict;  // 注意包路径

@Data
public class UserDTO {
    @MyDict(type = "user_status")   // 属性名是 type（不是 name）
    private Integer status;         // 编译期自动生成 statusDesc 字段
}
```

### 严禁：字典 DTO 本身不能加 @MyDict

`DictDataDTO` 和 `DictTypeDTO` 不能在自身字段上加 `@MyDict(type = "common_status")`。

原因：序列化时触发 `getStatusDesc()` → 查字典 → 缓存 miss → 重新缓存 → 再次序列化 → 无限循环。

### 已有字典类型

| dict_type | 值域 |
|---|---|
| `user_status` | 0=禁用 1=启用 |
| `common_status` | 0=禁用 1=启用 |
| `tenant_status` | 0=待激活 1=正常 2=禁用 3=过期 |
| `menu_type` | 0=目录 1=菜单 2=按钮 |
| `menu_display_type` | 1=平台菜单 2=租户模板菜单 |
| `admin_flag` | 0=普通用户 1=管理员 |
| `user_type` | 0=运维 1=教师 2=学生 |
| `operation_result` | 0=失败 1=成功 |

---

## 实体类规范

```java
@Data
@MyTable("sys_user")           // 使用 @MyTable，不是 @Table
public class SysUser {
    private Long id;
    private Long tenantId;     // 有 tenant_id 的表必须声明此字段
    private Integer status;    // Integer，不用 boolean/Boolean
    private Integer deleteFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- PostgreSQL `SMALLINT` → Java `Integer`
- 自增主键用 `BIGSERIAL`，Java 用 `Long`
- 布尔语义字段用 `Integer`（0/1），不用 `boolean`

---

## 编码规范

### Service 必须先定义接口

```java
// 接口（extends BaseServiceImpl 的子类）
public interface IUserService extends IBaseService { ... }

// 实现
@Service
public class UserService extends BaseServiceImpl implements IUserService { ... }

// 注入（用接口类型）
@Autowired
private IUserService userService;   // ✅
// private UserService userService;  // ❌
```

### 实体转 DTO

```java
UserDTO dto = new UserDTO();
BeanUtils.copyProperties(user, dto);
// 需要覆盖特定字段时在 copyProperties 之后单独 set
```

不写独立的 `convertToDTO()` 方法，不逐字段手动 set。

### 分页参数必须带泛型

```java
Pager<UserDTO> search(UserQueryParam param, Pager<UserDTO> pager);  // ✅
Pager<UserDTO> search(UserQueryParam param, Pager pager);           // ❌
```

### 验证外键存在性

```java
if (request.getOrgId() != null) {
    if (baseDao.queryById(request.getOrgId(), SysOrganization.class) == null)
        throw new BusinessException("组织不存在");
}
```

### 操作日志 + 事务

```java
@OperationLog(type = OperationType.CREATE, module = "用户管理", description = "创建用户")
@Transactional
public void create(UserCreateRequest request) { ... }
```

---

## 常见陷阱

| 场景 | 错误做法 | 正确做法 |
|---|---|---|
| 查询当前用户 | `Long userId = 1L` | `SecurityContextUtil.getCurrentUser()` |
| 实体注解 | `@Table("sys_user")` | `@MyTable("sys_user")` |
| 逻辑删除查询 | SQL 手写 `delete_flag = 0` | 框架自动注入，不要写 |
| 旧 API | `queryByIdWithDeleteCondition()` | 已删除，直接用 `queryById()` |
| 平台管理员查指定租户 | `TenantContext.withoutTenant(() -> ...)` | 直接 `.eq(Entity::getTenantId, id)` |
| 可选时间范围 | `.between(startTime, endTime)` | `.ge(startTime).le(endTime)` |
| 字典描述字段 | Service 手动 `setStatusDesc(...)` | 用 `@MyDict`，框架生成 |
| 字典 DTO 自身 | `DictDataDTO` 加 `@MyDict` | 禁止，会循环递归 |
| Druid 配置 | 不写 type | 必须写 `type: com.alibaba.druid.pool.DruidDataSource` |
| 默认密码 | 各处硬编码 | 从 `ConfigUtil.getString(ConfigKeys.PASSWORD_INITIAL, "Aa123456!")` 读取 |

---

## 数据库初始化

```bash
createdb -U postgres seer_fitness_edu
# 按序号顺序执行 seer-fitness-boot/src/main/resources/db/pgsql/ 下的所有 SQL 脚本
psql -U postgres -d seer_fitness_edu -f 001_create_tables.sql
psql -U postgres -d seer_fitness_edu -f 002_init_data.sql
# ... 以此类推
```

默认账号：`admin / Aa123456!`

## 构建

```bash
mvn clean package -DskipTests
mvn spring-boot:run -pl seer-fitness-boot   # 开发环境
```

依赖解析问题：若出现 `.lastUpdated` 文件导致构建失败：
```bash
find ~/.m2/repository/io/github/mocanjie -name "*.lastUpdated" -delete
```
