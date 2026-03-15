# Framework 模块增强 & System 模块瘦身 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** 将 seer-fitness-system 中的基础设施代码（JWT/Redis/密码/锁定/安全注解/工具类）迁移到 seer-fitness-framework，使 framework 成为真正可复用的基础设施层，system 只保留业务逻辑。

**Architecture:**
- 依赖方向保持不变：`framework ← system ← business ← boot`
- 迁移的 15 个类：包名从 `com.seer.fitness.system.*` → `com.seer.fitness.framework.*`
- 附带修复：`UserCacheInfo.roles` 由 `List<RoleDTO>` 改为 `List<String> roleCodes`（消除循环依赖 + 修复已失效的角色检查逻辑）

**Tech Stack:** Maven 多模块, Spring Boot 3.5.6, Java 21, JWT, Redis, BCrypt

---

## 迁移清单（15 个文件）

| 原路径（system）| 新路径（framework）| 备注 |
|---|---|---|
| `enums/OperationType.java` | `enums/OperationType.java` | @OperationLog 依赖此枚举 |
| `dto/UserCacheInfo.java` | `dto/UserCacheInfo.java` | roles 字段类型变更 |
| `annotation/OperationLog.java` | `annotation/OperationLog.java` | 引用改为 framework.enums |
| `security/RequireAuth.java` | `annotation/RequireAuth.java` | 已引用 framework.enums.AuthMode |
| `config/JwtConfig.java` | `config/JwtConfig.java` | |
| `config/RedisConfig.java` | `config/RedisConfig.java` | 需 fastjson2-extension-spring6 |
| `config/PasswordPolicyConfig.java` | `config/PasswordPolicyConfig.java` | |
| `config/AccountLockConfig.java` | `config/AccountLockConfig.java` | 引用 framework.enums.LockStrategy |
| `config/SchedulerConfig.java` | `config/SchedulerConfig.java` | |
| `utils/JwtUtil.java` | `utils/JwtUtil.java` | 引用改为 framework.config |
| `utils/RedisUtil.java` | `utils/RedisUtil.java` | |
| `utils/PasswordUtil.java` | `utils/PasswordUtil.java` | 引用改为 framework.config |
| `utils/LockTimeCalculator.java` | `utils/LockTimeCalculator.java` | 引用改为 framework.config |
| `utils/LockMessageBuilder.java` | `utils/LockMessageBuilder.java` | 引用改为 framework.config |
| `utils/SecurityContextUtil.java` | `utils/SecurityContextUtil.java` | 引用改为 framework.dto |

**保留在 system 的文件（需更新 import）：**
- `security/AuthInterceptor.java`（依赖 IAuthService → 不可移）
- `interceptor/TenantInterceptor.java`
- `config/WebConfig.java`, `AuthConfig.java`, `CaptchaConfig.java`, `TenantConfig.java`
- 所有 Controller、Service、DTO、Entity
- `aspect/OperationLogAspect.java`（依赖 OperationLogService）

---

## Task 1: 更新 framework pom.xml

**Files:**
- Modify: `seer-fitness-framework/pom.xml`

### Step 1: 在 `<dependencies>` 末尾追加新依赖

在 `seer-fitness-framework/pom.xml` 的 `</dependencies>` 前插入：

```xml
        <!-- JWT认证（版本由父pom管理）-->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Redis支持 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- 密码加密 -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>

        <!-- FastJSON2 Redis序列化 -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2-extension-spring6</artifactId>
        </dependency>

        <!-- 参数校验（PasswordPolicyConfig/AccountLockConfig 用 @Validated） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Spring Web（SecurityContextUtil 需要 RequestContextHolder）-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
```

### Step 2: 验证

```bash
mvn clean install -DskipTests -pl seer-fitness-framework --no-transfer-progress
```

期望：`BUILD SUCCESS`

### Step 3: Commit

```bash
git add seer-fitness-framework/pom.xml
git commit -m "build: framework 增加 JWT/Redis/BCrypt/Validation/Web 依赖"
```

---

## Task 2: 在 framework 创建新枚举和 DTO

**Files:**
- Create: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/enums/OperationType.java`
- Create: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/dto/UserCacheInfo.java`

### Step 1: 创建 OperationType

文件路径：`seer-fitness-framework/src/main/java/com/seer/fitness/framework/enums/OperationType.java`

```java
package com.seer.fitness.framework.enums;

public enum OperationType {

    CREATE("CREATE", "新增"),
    UPDATE("UPDATE", "修改"),
    DELETE("DELETE", "删除"),
    QUERY("QUERY", "查询"),
    LOGIN("LOGIN", "登录"),
    LOGOUT("LOGOUT", "登出"),
    IMPORT("IMPORT", "导入"),
    EXPORT("EXPORT", "导出"),
    SYNC("SYNC", "同步"),
    AUDIT("AUDIT", "审核"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String description;

    OperationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static OperationType fromCode(String code) {
        for (OperationType type : values()) {
            if (type.getCode().equals(code)) return type;
        }
        return OTHER;
    }
}
```

### Step 2: 创建 UserCacheInfo（注意：roles 字段改为 roleCodes）

文件路径：`seer-fitness-framework/src/main/java/com/seer/fitness/framework/dto/UserCacheInfo.java`

**关键变更：** 原 `List<RoleDTO> roles` → `List<String> roleCodes`，原 `getRoles()` → `getRoleCodes()`

```java
package com.seer.fitness.framework.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Redis 用户缓存信息（认证上下文，跨模块共享）
 */
@Data
public class UserCacheInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String realName;

    /**
     * 角色编码列表（如 ["TENANT_ADMIN", "TEACHER"]）
     * 注意：原字段 roles(List<RoleDTO>) 已简化为 roleCodes(List<String>)，
     * 只保留角色码，用于权限判断。
     */
    private List<String> roleCodes;

    private List<String> permissions;
    private Integer adminFlag;
    private Integer userType;
    private Long lastAccessTime;
    private String tokenId;
    private Long tenantId;
    private String tenantCode;

    public UserCacheInfo() {}

    public UserCacheInfo(Long userId, String username, String realName,
                         List<String> roleCodes, List<String> permissions,
                         Integer adminFlag, Integer userType, String tokenId) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.roleCodes = roleCodes;
        this.permissions = permissions;
        this.adminFlag = adminFlag;
        this.userType = userType;
        this.tokenId = tokenId;
        this.lastAccessTime = System.currentTimeMillis();
    }

    public UserCacheInfo(Long userId, String username, String realName,
                         List<String> roleCodes, List<String> permissions,
                         Integer adminFlag, Integer userType, String tokenId,
                         Long tenantId, String tenantCode) {
        this(userId, username, realName, roleCodes, permissions, adminFlag, userType, tokenId);
        this.tenantId = tenantId;
        this.tenantCode = tenantCode;
    }
}
```

### Step 3: 编译验证

```bash
mvn clean compile -DskipTests -pl seer-fitness-framework --no-transfer-progress
```

期望：`BUILD SUCCESS`

### Step 4: Commit

```bash
git add seer-fitness-framework/src/
git commit -m "feat(framework): 新增 OperationType 枚举、UserCacheInfo DTO"
```

---

## Task 3: 在 framework 创建注解和配置类

**Files:**
- Create: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/annotation/OperationLog.java`
- Create: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/annotation/RequireAuth.java`
- Create: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/config/JwtConfig.java`
- Create: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/config/RedisConfig.java`
- Create: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/config/PasswordPolicyConfig.java`
- Create: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/config/AccountLockConfig.java`
- Create: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/config/SchedulerConfig.java`

### Step 1: 创建 @OperationLog

文件：`seer-fitness-framework/src/main/java/com/seer/fitness/framework/annotation/OperationLog.java`

内容同 system 版本，**仅修改**：
- 第一行 package：`package com.seer.fitness.framework.annotation;`
- import 行：`import com.seer.fitness.framework.enums.OperationType;`（原为 system.enums）

```java
package com.seer.fitness.framework.annotation;

import com.seer.fitness.framework.enums.OperationType;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    OperationType type();
    String module();
    String description();
    String businessId() default "";
    String businessName() default "";
    boolean recordRequest() default true;
    boolean recordResponse() default false;
    boolean async() default true;
    String[] excludeParams() default {"password", "token", "secret", "key"};
}
```

### Step 2: 创建 @RequireAuth

文件：`seer-fitness-framework/src/main/java/com/seer/fitness/framework/annotation/RequireAuth.java`

内容同 system 版本，**仅修改 package**：

```java
package com.seer.fitness.framework.annotation;

import com.seer.fitness.framework.enums.AuthMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {
    boolean login() default true;
    String[] permissions() default {};
    String[] roles() default {};
    AuthMode mode() default AuthMode.ANY;
}
```

### Step 3: 创建 5 个 Config 类

各文件仅修改 package 为 `com.seer.fitness.framework.config`，其余内容完全照搬 system 版本。

**JwtConfig.java** - package 改为 `com.seer.fitness.framework.config`，内容不变。

**RedisConfig.java** - package 改为 `com.seer.fitness.framework.config`，内容不变。

**PasswordPolicyConfig.java** - package 改为 `com.seer.fitness.framework.config`，内容不变。

**AccountLockConfig.java** - package 改为 `com.seer.fitness.framework.config`，import 改为：
```java
import com.seer.fitness.framework.enums.LockStrategy;
```
（原为 `com.seer.fitness.framework.enums.LockStrategy` — 此枚举本已在 framework，无需变动）

**SchedulerConfig.java** - package 改为 `com.seer.fitness.framework.config`，内容不变。

> **提示：** 创建每个文件时，先用 Read 工具读取 system 版本获取完整内容，再用 Write 工具写到 framework 目录，只改 package 声明（和 AccountLockConfig 的 import）。

### Step 4: 编译验证

```bash
mvn clean compile -DskipTests -pl seer-fitness-framework --no-transfer-progress
```

期望：`BUILD SUCCESS`

### Step 5: Commit

```bash
git add seer-fitness-framework/src/
git commit -m "feat(framework): 迁入 @OperationLog/@RequireAuth 注解及 5 个 Config 配置类"
```

---

## Task 4: 在 framework 创建工具类

**Files:**
- Create: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/utils/JwtUtil.java`
- Create: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/utils/RedisUtil.java`
- Create: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/utils/PasswordUtil.java`
- Create: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/utils/LockTimeCalculator.java`
- Create: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/utils/LockMessageBuilder.java`
- Create: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/utils/SecurityContextUtil.java`

> **做法：** 先 Read system 版本，Write 到 framework 目录，修改：
> 1. package → `com.seer.fitness.framework.utils`
> 2. 所有 `import com.seer.fitness.system.config.*` → `import com.seer.fitness.framework.config.*`
> 3. 所有 `import com.seer.fitness.system.dto.*` → `import com.seer.fitness.framework.dto.*`

各文件具体 import 变更：

| 文件 | 原 import | 新 import |
|------|-----------|-----------|
| JwtUtil | `system.config.JwtConfig` | `framework.config.JwtConfig` |
| PasswordUtil | `system.config.PasswordPolicyConfig` | `framework.config.PasswordPolicyConfig` |
| LockTimeCalculator | `system.config.AccountLockConfig` | `framework.config.AccountLockConfig` |
| LockMessageBuilder | `system.config.AccountLockConfig` | `framework.config.AccountLockConfig` |
| SecurityContextUtil | `system.dto.UserCacheInfo` | `framework.dto.UserCacheInfo` |

RedisUtil 无自定义 import，仅改 package。

**SecurityContextUtil 的 hasRole() 需同步修改** — 原来调 `getRoles()`，现在改为 `getRoleCodes()`：

```java
// 原（已失效）：
currentUser.getRoles().contains(role)

// 改为：
currentUser.getRoleCodes().contains(role)
```

### Step 1: 创建 6 个工具类（按上述规则逐一 Read→Write）

### Step 2: 编译验证

```bash
mvn clean compile -DskipTests -pl seer-fitness-framework --no-transfer-progress
```

期望：`BUILD SUCCESS`

### Step 3: Commit

```bash
git add seer-fitness-framework/src/
git commit -m "feat(framework): 迁入 6 个工具类（JWT/Redis/Password/Lock/Security）"
```

---

## Task 5: 更新 system 模块的所有 import

**Files:** system 模块下所有 .java 文件（批量 sed）

此任务完成后再删除 system 中的旧文件。

### Step 1: 批量替换所有 import

在项目根目录执行（每条 sed 替换一类 import）：

```bash
SYSTEM_SRC="seer-fitness-system/src/main/java/com/seer/fitness/system"

# 枚举
find $SYSTEM_SRC -name "*.java" -exec sed -i '' \
  's|com\.seer\.fitness\.system\.enums\.OperationType|com.seer.fitness.framework.enums.OperationType|g' {} +

# DTO
find $SYSTEM_SRC -name "*.java" -exec sed -i '' \
  's|com\.seer\.fitness\.system\.dto\.UserCacheInfo|com.seer.fitness.framework.dto.UserCacheInfo|g' {} +

# 注解（OperationLog 和 RequireAuth 换新包）
find $SYSTEM_SRC -name "*.java" -exec sed -i '' \
  's|com\.seer\.fitness\.system\.annotation\.OperationLog|com.seer.fitness.framework.annotation.OperationLog|g' {} +
find $SYSTEM_SRC -name "*.java" -exec sed -i '' \
  's|com\.seer\.fitness\.system\.security\.RequireAuth|com.seer.fitness.framework.annotation.RequireAuth|g' {} +

# Config
find $SYSTEM_SRC -name "*.java" -exec sed -i '' \
  's|com\.seer\.fitness\.system\.config\.JwtConfig|com.seer.fitness.framework.config.JwtConfig|g' {} +
find $SYSTEM_SRC -name "*.java" -exec sed -i '' \
  's|com\.seer\.fitness\.system\.config\.RedisConfig|com.seer.fitness.framework.config.RedisConfig|g' {} +
find $SYSTEM_SRC -name "*.java" -exec sed -i '' \
  's|com\.seer\.fitness\.system\.config\.PasswordPolicyConfig|com.seer.fitness.framework.config.PasswordPolicyConfig|g' {} +
find $SYSTEM_SRC -name "*.java" -exec sed -i '' \
  's|com\.seer\.fitness\.system\.config\.AccountLockConfig|com.seer.fitness.framework.config.AccountLockConfig|g' {} +
find $SYSTEM_SRC -name "*.java" -exec sed -i '' \
  's|com\.seer\.fitness\.system\.config\.SchedulerConfig|com.seer.fitness.framework.config.SchedulerConfig|g' {} +

# Utils
find $SYSTEM_SRC -name "*.java" -exec sed -i '' \
  's|com\.seer\.fitness\.system\.utils\.JwtUtil|com.seer.fitness.framework.utils.JwtUtil|g' {} +
find $SYSTEM_SRC -name "*.java" -exec sed -i '' \
  's|com\.seer\.fitness\.system\.utils\.RedisUtil|com.seer.fitness.framework.utils.RedisUtil|g' {} +
find $SYSTEM_SRC -name "*.java" -exec sed -i '' \
  's|com\.seer\.fitness\.system\.utils\.PasswordUtil|com.seer.fitness.framework.utils.PasswordUtil|g' {} +
find $SYSTEM_SRC -name "*.java" -exec sed -i '' \
  's|com\.seer\.fitness\.system\.utils\.LockTimeCalculator|com.seer.fitness.framework.utils.LockTimeCalculator|g' {} +
find $SYSTEM_SRC -name "*.java" -exec sed -i '' \
  's|com\.seer\.fitness\.system\.utils\.LockMessageBuilder|com.seer.fitness.framework.utils.LockMessageBuilder|g' {} +
find $SYSTEM_SRC -name "*.java" -exec sed -i '' \
  's|com\.seer\.fitness\.system\.utils\.SecurityContextUtil|com.seer.fitness.framework.utils.SecurityContextUtil|g' {} +
```

### Step 2: 修复 AuthService 中构建 UserCacheInfo 的代码

找到 `AuthService.java` 中构建 UserCacheInfo 的位置（约第 115-140 行），将：
```java
List<RoleDTO> roles = roleService.getUserRoles(user.getId());
// ...
new UserCacheInfo(..., roles, permissions, ...)
```
改为：
```java
List<String> roleCodes = roleService.getUserRoles(user.getId())
        .stream()
        .map(RoleDTO::getRoleCode)
        .collect(java.util.stream.Collectors.toList());
// ...
new UserCacheInfo(..., roleCodes, permissions, ...)
```

### Step 3: 修复 AuthInterceptor 的 checkRoles 方法

找到 `AuthInterceptor.java` 中 `checkRoles()` 方法，将所有 `user.getRoles()` 改为 `user.getRoleCodes()`：
```java
// 原
if (user.getRoles() == null) ...
user.getRoles().containsAll(...)
user.getRoles().stream().anyMatch(...)

// 改为
if (user.getRoleCodes() == null) ...
user.getRoleCodes().containsAll(...)
user.getRoleCodes().stream().anyMatch(...)
```

同时修复 log 语句中 `user.getRoles()` → `user.getRoleCodes()`。

### Step 4: 修复 AuthService.hasRole 方法

找到 `AuthService.java` 中 `hasRole()` 方法：
```java
// 原
return user.getRoles() != null && user.getRoles().contains(role);
// 改为
return user.getRoleCodes() != null && user.getRoleCodes().contains(role);
```

### Step 5: 删除 system 中已迁移的旧文件

```bash
SYSTEM_SRC="seer-fitness-system/src/main/java/com/seer/fitness/system"

rm "$SYSTEM_SRC/enums/OperationType.java"
rm "$SYSTEM_SRC/dto/UserCacheInfo.java"
rm "$SYSTEM_SRC/annotation/OperationLog.java"
rm "$SYSTEM_SRC/security/RequireAuth.java"
rm "$SYSTEM_SRC/config/JwtConfig.java"
rm "$SYSTEM_SRC/config/RedisConfig.java"
rm "$SYSTEM_SRC/config/PasswordPolicyConfig.java"
rm "$SYSTEM_SRC/config/AccountLockConfig.java"
rm "$SYSTEM_SRC/config/SchedulerConfig.java"
rm "$SYSTEM_SRC/utils/JwtUtil.java"
rm "$SYSTEM_SRC/utils/RedisUtil.java"
rm "$SYSTEM_SRC/utils/PasswordUtil.java"
rm "$SYSTEM_SRC/utils/LockTimeCalculator.java"
rm "$SYSTEM_SRC/utils/LockMessageBuilder.java"
rm "$SYSTEM_SRC/utils/SecurityContextUtil.java"
```

### Step 6: 编译整个项目

```bash
mvn clean compile -DskipTests --no-transfer-progress
```

期望：`BUILD SUCCESS`

如果有编译错误：
- `找不到符号 getRoles()` → 还有地方未改为 `getRoleCodes()`，用 grep 找出：
  ```bash
  grep -rn "getRoles()" seer-fitness-system/src --include="*.java"
  ```
- `找不到符号 UserCacheInfo` → import 未替换，检查该文件的 import
- `找不到符号 RequireAuth` → import 还是 system.security，检查 sed 是否生效

### Step 7: Commit

```bash
git add -A
git commit -m "refactor: 将基础设施层从 system 迁移至 framework（JWT/Redis/安全注解/工具类）"
```

---

## Task 6: 清理 system pom（可选优化）

**Files:**
- Modify: `seer-fitness-system/pom.xml`

以下依赖现在由 framework 传递提供，可从 system pom 移除（如移除后编译不通过则保留）：
- `jjwt-api`、`jjwt-impl`、`jjwt-jackson`
- `spring-security-crypto`
- `fastjson2-extension-spring6`（system 中 DictCacheService 等是否直接用到需确认）

### Step 1: 逐一注释掉上述依赖，编译验证

```bash
mvn clean compile -DskipTests --no-transfer-progress
```

只移除编译通过的，有报错的恢复。

### Step 2: Commit

```bash
git add seer-fitness-system/pom.xml
git commit -m "build: system pom 移除已由 framework 传递的依赖"
```

---

## Task 7: 更新 framework 模块描述

**Files:**
- Modify: `seer-fitness-framework/pom.xml`（description）

将 `<description>` 更新为：

```xml
<description>
    框架基础设施层：认证安全（@RequireAuth/@OperationLog/UserCacheInfo）、
    JWT/Redis/密码/账号锁定工具类和配置，供所有业务模块复用。
</description>
```

```bash
git add seer-fitness-framework/pom.xml
git commit -m "docs: 更新 framework 模块描述反映新职责"
```

---

## 验证清单（完成后检查）

```bash
# 1. framework 有 15 个新类
find seer-fitness-framework/src -name "*.java" | wc -l
# 期望：≥ 22（原 7 + 新建 15）

# 2. system 中这些文件已删除
ls seer-fitness-system/src/main/java/com/seer/fitness/system/annotation/
# 期望：空目录或目录不存在

# 3. system 中没有旧包路径引用
grep -rn "system\.config\.JwtConfig\|system\.utils\.JwtUtil\|system\.dto\.UserCacheInfo\|system\.security\.RequireAuth" \
  seer-fitness-system/src --include="*.java"
# 期望：无输出

# 4. 整体编译
mvn clean compile -DskipTests --no-transfer-progress
# 期望：BUILD SUCCESS
```
