# 配置管理模块 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现 `sys_config` 平台配置管理模块，将 yml 中的业务可调参数迁移到数据库，通过 Redis 缓存提供高性能读取，并更新所有消费方代码改为从 ConfigUtil 读取。

**Architecture:** `sys_config` 表存储 key-value 配置，Redis 无 TTL 持久缓存（key: `config:{key}`），启动时全量预热。ConfigUtil（system 模块）提供 getString/getInt/getBoolean/getList 四个工具方法。framework 中的 PasswordUtil / LockTimeCalculator / LockMessageBuilder 各新增接受原始参数的重载方法，system 服务层读 ConfigUtil 后调用重载方法，与 yml Bean 解耦。

**Tech Stack:** Spring Boot 3.5.6, Java 21, PostgreSQL, Redis, MyJPA 1.0-jdk21

---

## 迁移配置清单（18 项）

| config_key | 类型 | 当前 yml 值 | 所属 yml 节 |
|---|---|---|---|
| `security.captcha.enabled` | boolean | true | security.captcha |
| `security.captcha.expire-seconds` | int | 300 | security.captcha |
| `security.captcha.length` | int | 4 | security.captcha |
| `security.password.initial-password` | string | Aa123456! | security.password |
| `security.password.policy.min-length` | int | 8 | security.password.policy |
| `security.password.policy.max-length` | int | 15 | security.password.policy |
| `security.password.policy.require-lowercase` | boolean | true | security.password.policy |
| `security.password.policy.require-uppercase` | boolean | true | security.password.policy |
| `security.password.policy.require-digit` | boolean | true | security.password.policy |
| `security.password.policy.require-special` | boolean | true | security.password.policy |
| `security.account-lock.enabled` | boolean | true | security.account-lock |
| `security.account-lock.attempts.max-fail-count` | int | 5 | security.account-lock |
| `security.account-lock.attempts.auto-reset-hours` | int | 24 | security.account-lock |
| `security.account-lock.lock-time.base-minutes` | int | 30 | security.account-lock |
| `security.account-lock.ip-lock.enabled` | boolean | true | security.account-lock |
| `security.account-lock.ip-lock.max-attempts` | int | 20 | security.account-lock |
| `security.account-lock.ip-lock.lock-minutes` | int | 60 | security.account-lock |
| `security.account-lock.whitelist.users` | list | admin,system | security.account-lock |
| `security.account-lock.whitelist.ips` | list | 127.0.0.1,192.168.1.0/24 | security.account-lock |
| `scheduler.pool-size` | int | 5 | scheduler |

---

## Task 1: SQL 脚本（建表 + 初始化数据）

**Files:**
- Create: `seer-fitness-boot/src/main/resources/db/pgsql/005_sys_config_table.sql`
- Create: `seer-fitness-boot/src/main/resources/db/pgsql/006_sys_config_data.sql`

### Step 1: 创建建表脚本

文件：`005_sys_config_table.sql`

```sql
-- 系统配置表
CREATE TABLE sys_config (
    id          BIGSERIAL PRIMARY KEY,
    config_key  VARCHAR(100) NOT NULL,
    config_value TEXT,
    config_name VARCHAR(200) NOT NULL,
    config_type SMALLINT NOT NULL DEFAULT 1,  -- 1=系统内置 2=用户自定义
    remark      VARCHAR(500),
    tenant_id   BIGINT,                        -- 预留多租户，当前只用 NULL
    create_by   VARCHAR(50),
    create_time TIMESTAMP,
    update_by   VARCHAR(50),
    update_time TIMESTAMP,
    delete_flag SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_sys_config_key UNIQUE (config_key)
);

COMMENT ON TABLE sys_config IS '系统配置表';
COMMENT ON COLUMN sys_config.config_key IS '配置键（全局唯一）';
COMMENT ON COLUMN sys_config.config_value IS '配置值（列表用英文逗号分隔）';
COMMENT ON COLUMN sys_config.config_name IS '配置名称（可读描述）';
COMMENT ON COLUMN sys_config.config_type IS '1=系统内置（不可删）2=用户自定义';
COMMENT ON COLUMN sys_config.tenant_id IS '租户ID，预留字段，当前仅使用 NULL（平台级）';
```

### Step 2: 创建初始化数据脚本

文件：`006_sys_config_data.sql`

```sql
INSERT INTO sys_config (config_key, config_value, config_name, config_type, remark, create_by, create_time, update_by, update_time, delete_flag) VALUES

-- 验证码配置
('security.captcha.enabled',       'true',        '验证码开关',           1, '是否启用图形验证码', 'system', NOW(), 'system', NOW(), 0),
('security.captcha.expire-seconds','300',          '验证码过期时间(秒)',    1, '验证码Redis缓存过期秒数', 'system', NOW(), 'system', NOW(), 0),
('security.captcha.length',        '4',            '验证码位数',           1, '验证码字符个数（4-8）', 'system', NOW(), 'system', NOW(), 0),

-- 密码策略
('security.password.initial-password',        'Aa123456!', '用户初始密码',             1, '管理员重置/初始化密码时使用的默认值', 'system', NOW(), 'system', NOW(), 0),
('security.password.policy.min-length',       '8',         '密码最小长度',             1, '用户密码最少字符数', 'system', NOW(), 'system', NOW(), 0),
('security.password.policy.max-length',       '15',        '密码最大长度',             1, '用户密码最多字符数', 'system', NOW(), 'system', NOW(), 0),
('security.password.policy.require-lowercase','true',      '密码需含小写字母',         1, 'true=必须包含 a-z', 'system', NOW(), 'system', NOW(), 0),
('security.password.policy.require-uppercase','true',      '密码需含大写字母',         1, 'true=必须包含 A-Z', 'system', NOW(), 'system', NOW(), 0),
('security.password.policy.require-digit',    'true',      '密码需含数字',             1, 'true=必须包含 0-9', 'system', NOW(), 'system', NOW(), 0),
('security.password.policy.require-special',  'true',      '密码需含特殊字符',         1, 'true=必须包含特殊字符', 'system', NOW(), 'system', NOW(), 0),

-- 账户锁定
('security.account-lock.enabled',                 'true',              '账户锁定开关',           1, '登录失败超限后是否锁定账户', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.attempts.max-fail-count', '5',                 '最大登录失败次数',       1, '超过此次数账户被锁定', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.attempts.auto-reset-hours','24',               '失败次数自动重置(小时)', 1, 'N小时内无失败则重置计数', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.lock-time.base-minutes',  '30',                '基础锁定时长(分钟)',     1, '渐进式锁定的初始锁定分钟数', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.ip-lock.enabled',         'true',              'IP锁定开关',             1, '同一IP失败过多时锁定', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.ip-lock.max-attempts',    '20',                'IP最大失败次数',         1, '超过后锁定该IP', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.ip-lock.lock-minutes',    '60',                'IP锁定时长(分钟)',       1, 'IP被锁定的持续时间', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.whitelist.users',         'admin,system',      '账户白名单',             1, '不受锁定限制的用户名，英文逗号分隔', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.whitelist.ips',           '127.0.0.1',         'IP白名单',               1, '不受锁定限制的IP，英文逗号分隔', 'system', NOW(), 'system', NOW(), 0),

-- 定时任务
('scheduler.pool-size', '5', '定时任务线程池大小', 1, '修改后重启生效', 'system', NOW(), 'system', NOW(), 0);
```

### Step 3: Commit

```bash
git add seer-fitness-boot/src/main/resources/db/pgsql/
git commit -m "feat(config): 新增 sys_config 建表和初始化数据 SQL 脚本"
```

---

## Task 2: Entity + Constants + DTO

**Files:**
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/entity/SysConfig.java`
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/constants/ConfigKeys.java`
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/dto/SysConfigDTO.java`
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/dto/SysConfigQueryParam.java`
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/dto/SysConfigCreateRequest.java`
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/dto/SysConfigUpdateRequest.java`

### Step 1: 创建 Entity

```java
package com.seer.fitness.system.entity;

import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MyTable("sys_config")
public class SysConfig {
    private Long id;
    private String configKey;
    private String configValue;
    private String configName;
    private Integer configType;   // 1=系统内置 2=用户自定义
    private String remark;
    private Long tenantId;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private Integer deleteFlag;
}
```

### Step 2: 创建 ConfigKeys 常量类

```java
package com.seer.fitness.system.constants;

public final class ConfigKeys {
    private ConfigKeys() {}

    // Redis 缓存前缀
    public static final String CACHE_PREFIX = "config:";

    // 验证码
    public static final String CAPTCHA_ENABLED        = "security.captcha.enabled";
    public static final String CAPTCHA_EXPIRE_SECONDS = "security.captcha.expire-seconds";
    public static final String CAPTCHA_LENGTH         = "security.captcha.length";

    // 密码策略
    public static final String PASSWORD_INITIAL           = "security.password.initial-password";
    public static final String PASSWORD_MIN_LENGTH        = "security.password.policy.min-length";
    public static final String PASSWORD_MAX_LENGTH        = "security.password.policy.max-length";
    public static final String PASSWORD_REQUIRE_LOWERCASE = "security.password.policy.require-lowercase";
    public static final String PASSWORD_REQUIRE_UPPERCASE = "security.password.policy.require-uppercase";
    public static final String PASSWORD_REQUIRE_DIGIT     = "security.password.policy.require-digit";
    public static final String PASSWORD_REQUIRE_SPECIAL   = "security.password.policy.require-special";

    // 账户锁定
    public static final String LOCK_ENABLED          = "security.account-lock.enabled";
    public static final String LOCK_MAX_FAIL_COUNT   = "security.account-lock.attempts.max-fail-count";
    public static final String LOCK_AUTO_RESET_HOURS = "security.account-lock.attempts.auto-reset-hours";
    public static final String LOCK_BASE_MINUTES     = "security.account-lock.lock-time.base-minutes";
    public static final String LOCK_IP_ENABLED       = "security.account-lock.ip-lock.enabled";
    public static final String LOCK_IP_MAX_ATTEMPTS  = "security.account-lock.ip-lock.max-attempts";
    public static final String LOCK_IP_LOCK_MINUTES  = "security.account-lock.ip-lock.lock-minutes";
    public static final String LOCK_WHITELIST_USERS  = "security.account-lock.whitelist.users";
    public static final String LOCK_WHITELIST_IPS    = "security.account-lock.whitelist.ips";

    // 定时任务
    public static final String SCHEDULER_POOL_SIZE = "scheduler.pool-size";
}
```

### Step 3: 创建 SysConfigDTO

```java
package com.seer.fitness.system.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysConfigDTO {
    private Long id;
    private String configKey;
    private String configValue;
    private String configName;
    private Integer configType;
    private String remark;
    private String updateBy;
    private LocalDateTime updateTime;
}
```

### Step 4: 创建 SysConfigQueryParam

```java
package com.seer.fitness.system.dto;

import lombok.Data;

@Data
public class SysConfigQueryParam {
    private String configKey;
    private String configName;
    private Integer configType;
}
```

### Step 5: 创建 SysConfigCreateRequest

```java
package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SysConfigCreateRequest {
    @NotBlank(message = "配置键不能为空")
    private String configKey;

    private String configValue;

    @NotBlank(message = "配置名称不能为空")
    private String configName;

    private String remark;
}
```

### Step 6: 创建 SysConfigUpdateRequest

```java
package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SysConfigUpdateRequest {
    @NotNull(message = "配置ID不能为空")
    private Long id;

    private String configValue;

    private String configName;

    private String remark;
}
```

### Step 7: 编译验证

```bash
cd /Users/canjiemo/project/seer-fitness-edu && mvn clean compile -DskipTests -pl seer-fitness-system --no-transfer-progress -q && echo "BUILD SUCCESS"
```

### Step 8: Commit

```bash
git add seer-fitness-system/src/main/java/com/seer/fitness/system/entity/SysConfig.java \
        seer-fitness-system/src/main/java/com/seer/fitness/system/constants/ConfigKeys.java \
        seer-fitness-system/src/main/java/com/seer/fitness/system/dto/SysConfig*.java
git commit -m "feat(config): 新增 SysConfig 实体、ConfigKeys 常量、相关 DTO"
```

---

## Task 3: Service（CRUD + Redis 缓存）

**Files:**
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/service/ISysConfigService.java`
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/service/SysConfigService.java`

### Step 1: 创建 ISysConfigService

```java
package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.SysConfigCreateRequest;
import com.seer.fitness.system.dto.SysConfigDTO;
import com.seer.fitness.system.dto.SysConfigQueryParam;
import com.seer.fitness.system.dto.SysConfigUpdateRequest;
import io.github.canjiemo.mycommon.pager.Pager;

import java.util.List;

public interface ISysConfigService {
    Pager<SysConfigDTO> search(SysConfigQueryParam param, Pager<SysConfigDTO> pager);
    SysConfigDTO getByKey(String configKey);
    String getValue(String configKey);
    void create(SysConfigCreateRequest request);
    void update(SysConfigUpdateRequest request);
    void delete(Long id);
    void refreshCache();
    void refreshCache(String configKey);
}
```

### Step 2: 创建 SysConfigService

```java
package com.seer.fitness.system.service;

import com.seer.fitness.system.constants.ConfigKeys;
import com.seer.fitness.system.dto.SysConfigCreateRequest;
import com.seer.fitness.system.dto.SysConfigDTO;
import com.seer.fitness.system.dto.SysConfigQueryParam;
import com.seer.fitness.system.dto.SysConfigUpdateRequest;
import com.seer.fitness.system.entity.SysConfig;
import com.seer.fitness.framework.utils.RedisUtil;
import com.seer.fitness.framework.utils.SecurityContextUtil;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class SysConfigService extends BaseServiceImpl implements ISysConfigService {

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public Pager<SysConfigDTO> search(SysConfigQueryParam param, Pager<SysConfigDTO> pager) {
        return lambdaQuery(SysConfig.class, SysConfigDTO.class)
                .like(SysConfig::getConfigKey, param.getConfigKey())
                .like(SysConfig::getConfigName, param.getConfigName())
                .eq(SysConfig::getConfigType, param.getConfigType())
                .isNull(SysConfig::getTenantId)
                .orderByAsc(SysConfig::getConfigKey)
                .page(pager);
    }

    @Override
    public SysConfigDTO getByKey(String configKey) {
        SysConfigDTO dto = lambdaQuery(SysConfig.class, SysConfigDTO.class)
                .eq(SysConfig::getConfigKey, configKey)
                .isNull(SysConfig::getTenantId)
                .one();
        if (dto == null) throw new BusinessException("配置项不存在: " + configKey);
        return dto;
    }

    @Override
    public String getValue(String configKey) {
        // 1. 读 Redis
        String cacheKey = ConfigKeys.CACHE_PREFIX + configKey;
        String cached = redisUtil.get(cacheKey, String.class);
        if (cached != null) return cached;

        // 2. 查 DB
        SysConfig config = lambdaQuery(SysConfig.class)
                .eq(SysConfig::getConfigKey, configKey)
                .isNull(SysConfig::getTenantId)
                .one();
        if (config == null || config.getConfigValue() == null) return null;

        // 3. 写入 Redis（无 TTL）
        redisUtil.set(cacheKey, config.getConfigValue());
        return config.getConfigValue();
    }

    @Override
    @Transactional(readOnly = false)
    public void create(SysConfigCreateRequest request) {
        boolean exists = lambdaQuery(SysConfig.class)
                .eq(SysConfig::getConfigKey, request.getConfigKey())
                .isNull(SysConfig::getTenantId)
                .exists();
        if (exists) throw new BusinessException("配置键已存在: " + request.getConfigKey());

        String currentUser = SecurityContextUtil.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

        SysConfig config = new SysConfig();
        config.setConfigKey(request.getConfigKey());
        config.setConfigValue(request.getConfigValue());
        config.setConfigName(request.getConfigName());
        config.setConfigType(2); // 用户自定义
        config.setRemark(request.getRemark());
        config.setCreateBy(currentUser);
        config.setCreateTime(now);
        config.setUpdateBy(currentUser);
        config.setUpdateTime(now);
        config.setDeleteFlag(0);

        baseDao.insertPO(config, true);
        log.info("创建配置项: key={}", request.getConfigKey());
    }

    @Override
    @Transactional(readOnly = false)
    public void update(SysConfigUpdateRequest request) {
        SysConfig config = baseDao.queryById(request.getId(), SysConfig.class);
        if (config == null) throw new BusinessException("配置项不存在");

        String currentUser = SecurityContextUtil.getCurrentUsername();

        if (request.getConfigValue() != null) config.setConfigValue(request.getConfigValue());
        if (request.getConfigName() != null) config.setConfigName(request.getConfigName());
        if (request.getRemark() != null) config.setRemark(request.getRemark());
        config.setUpdateBy(currentUser);
        config.setUpdateTime(LocalDateTime.now());

        baseDao.updatePO(config);

        // 删除缓存，下次读自动回填
        redisUtil.delete(ConfigKeys.CACHE_PREFIX + config.getConfigKey());
        log.info("更新配置项: key={}, value={}", config.getConfigKey(), config.getConfigValue());
    }

    @Override
    @Transactional(readOnly = false)
    public void delete(Long id) {
        SysConfig config = baseDao.queryById(id, SysConfig.class);
        if (config == null) throw new BusinessException("配置项不存在");
        if (config.getConfigType() == 1) throw new BusinessException("系统内置配置不允许删除");

        baseDao.delByIds(SysConfig.class, String.valueOf(id));
        redisUtil.delete(ConfigKeys.CACHE_PREFIX + config.getConfigKey());
        log.info("删除配置项: key={}", config.getConfigKey());
    }

    @Override
    public void refreshCache() {
        // 删除所有 config:* key，逐项回填
        redisUtil.deleteByPattern(ConfigKeys.CACHE_PREFIX + "*");
        List<SysConfig> all = lambdaQuery(SysConfig.class).isNull(SysConfig::getTenantId).list();
        for (SysConfig c : all) {
            if (c.getConfigValue() != null) {
                redisUtil.set(ConfigKeys.CACHE_PREFIX + c.getConfigKey(), c.getConfigValue());
            }
        }
        log.info("刷新配置缓存完成，共 {} 项", all.size());
    }

    @Override
    public void refreshCache(String configKey) {
        redisUtil.delete(ConfigKeys.CACHE_PREFIX + configKey);
        getValue(configKey); // 触发 DB 读取并回填
        log.info("刷新配置缓存: key={}", configKey);
    }
}
```

### Step 3: 编译验证

```bash
cd /Users/canjiemo/project/seer-fitness-edu && mvn clean compile -DskipTests -pl seer-fitness-system --no-transfer-progress -q && echo "BUILD SUCCESS"
```

### Step 4: Commit

```bash
git add seer-fitness-system/src/main/java/com/seer/fitness/system/service/ISysConfigService.java \
        seer-fitness-system/src/main/java/com/seer/fitness/system/service/SysConfigService.java
git commit -m "feat(config): 新增 SysConfigService（CRUD + Redis 缓存）"
```

---

## Task 4: ConfigUtil + ConfigCacheInitializer

**Files:**
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/utils/ConfigUtil.java`
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/service/ConfigCacheInitializer.java`

### Step 1: 创建 ConfigUtil

```java
package com.seer.fitness.system.utils;

import com.seer.fitness.system.service.ISysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class ConfigUtil {

    private static ISysConfigService configService;

    @Autowired
    public void setConfigService(ISysConfigService configService) {
        ConfigUtil.configService = configService;
    }

    public static String getString(String key, String defaultValue) {
        try {
            String val = configService.getValue(key);
            return StringUtils.hasText(val) ? val : defaultValue;
        } catch (Exception e) {
            log.warn("读取配置失败: key={}, 使用默认值: {}", key, defaultValue, e);
            return defaultValue;
        }
    }

    public static int getInt(String key, int defaultValue) {
        try {
            String val = configService.getValue(key);
            return StringUtils.hasText(val) ? Integer.parseInt(val.trim()) : defaultValue;
        } catch (Exception e) {
            log.warn("读取配置失败: key={}, 使用默认值: {}", key, defaultValue, e);
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        try {
            String val = configService.getValue(key);
            return StringUtils.hasText(val) ? Boolean.parseBoolean(val.trim()) : defaultValue;
        } catch (Exception e) {
            log.warn("读取配置失败: key={}, 使用默认值: {}", key, defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * 读取逗号分隔的字符串配置，返回 List<String>（trim 每一项）
     */
    public static List<String> getList(String key) {
        try {
            String val = configService.getValue(key);
            if (!StringUtils.hasText(val)) return Collections.emptyList();
            return Arrays.stream(val.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        } catch (Exception e) {
            log.warn("读取配置失败: key={}", key, e);
            return Collections.emptyList();
        }
    }
}
```

### Step 2: 创建 ConfigCacheInitializer

```java
package com.seer.fitness.system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConfigCacheInitializer {

    @Autowired
    private ISysConfigService sysConfigService;

    @EventListener(ApplicationReadyEvent.class)
    public void initConfigCache() {
        log.info("系统启动，开始初始化配置缓存...");
        try {
            sysConfigService.refreshCache();
            log.info("配置缓存初始化完成");
        } catch (Exception e) {
            log.error("配置缓存初始化失败，将按需懒加载", e);
        }
    }
}
```

### Step 3: 编译验证

```bash
cd /Users/canjiemo/project/seer-fitness-edu && mvn clean compile -DskipTests -pl seer-fitness-system --no-transfer-progress -q && echo "BUILD SUCCESS"
```

### Step 4: Commit

```bash
git add seer-fitness-system/src/main/java/com/seer/fitness/system/utils/ConfigUtil.java \
        seer-fitness-system/src/main/java/com/seer/fitness/system/service/ConfigCacheInitializer.java
git commit -m "feat(config): 新增 ConfigUtil 工具类和 ConfigCacheInitializer 启动预热"
```

---

## Task 5: PlatformConfigController

**Files:**
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/controller/PlatformConfigController.java`

### Step 1: 创建 Controller

```java
package com.seer.fitness.system.controller;

import com.seer.fitness.framework.annotation.OperationLog;
import com.seer.fitness.framework.annotation.RequireAuth;
import com.seer.fitness.framework.enums.OperationType;
import com.seer.fitness.system.dto.SysConfigCreateRequest;
import com.seer.fitness.system.dto.SysConfigDTO;
import com.seer.fitness.system.dto.SysConfigQueryParam;
import com.seer.fitness.system.dto.SysConfigUpdateRequest;
import com.seer.fitness.system.service.ISysConfigService;
import io.github.canjiemo.mycommon.pager.Pager;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/config")
public class PlatformConfigController {

    @Autowired
    private ISysConfigService sysConfigService;

    @PostMapping("/search")
    @RequireAuth(permissions = {"config:view"})
    public Pager<SysConfigDTO> search(@RequestBody SysConfigQueryParam param, Pager<SysConfigDTO> pager) {
        return sysConfigService.search(param, pager);
    }

    @GetMapping("/{key}")
    @RequireAuth(permissions = {"config:view"})
    public SysConfigDTO getByKey(@PathVariable String key) {
        return sysConfigService.getByKey(key);
    }

    @PostMapping("/create")
    @RequireAuth(permissions = {"config:create"})
    @OperationLog(type = OperationType.CREATE, module = "配置管理", description = "新增配置项")
    public void create(@RequestBody @Valid SysConfigCreateRequest request) {
        sysConfigService.create(request);
    }

    @PostMapping("/update")
    @RequireAuth(permissions = {"config:update"})
    @OperationLog(type = OperationType.UPDATE, module = "配置管理", description = "修改配置项")
    public void update(@RequestBody @Valid SysConfigUpdateRequest request) {
        sysConfigService.update(request);
    }

    @PostMapping("/delete/{id}")
    @RequireAuth(permissions = {"config:delete"})
    @OperationLog(type = OperationType.DELETE, module = "配置管理", description = "删除配置项")
    public void delete(@PathVariable Long id) {
        sysConfigService.delete(id);
    }

    @PostMapping("/refresh")
    @RequireAuth(permissions = {"config:update"})
    @OperationLog(type = OperationType.OTHER, module = "配置管理", description = "刷新配置缓存")
    public void refresh(@RequestParam(required = false) String key) {
        if (key != null) {
            sysConfigService.refreshCache(key);
        } else {
            sysConfigService.refreshCache();
        }
    }
}
```

### Step 2: 编译验证

```bash
cd /Users/canjiemo/project/seer-fitness-edu && mvn clean compile -DskipTests --no-transfer-progress -q && echo "BUILD SUCCESS"
```

### Step 3: Commit

```bash
git add seer-fitness-system/src/main/java/com/seer/fitness/system/controller/PlatformConfigController.java
git commit -m "feat(config): 新增 PlatformConfigController（/platform/config/*）"
```

---

## Task 6: 迁移 CaptchaService（删除 CaptchaConfig）

**Files:**
- Modify: `seer-fitness-system/src/main/java/com/seer/fitness/system/service/CaptchaService.java`
- Delete: `seer-fitness-system/src/main/java/com/seer/fitness/system/config/CaptchaConfig.java`

### Step 1: 更新 CaptchaService

将所有 `captchaConfig.*` 调用替换为 ConfigUtil 调用。关键替换点：

| 原调用 | 替换为 |
|---|---|
| `captchaConfig.isEnabled()` | `ConfigUtil.getBoolean(ConfigKeys.CAPTCHA_ENABLED, true)` |
| `captchaConfig.getExpireSeconds()` | `ConfigUtil.getInt(ConfigKeys.CAPTCHA_EXPIRE_SECONDS, 300)` |
| `captchaConfig.getLength()` | `ConfigUtil.getInt(ConfigKeys.CAPTCHA_LENGTH, 4)` |
| `captchaConfig.getType().getCharacters()` | 根据 `ConfigUtil.getString("security.captcha.type", "DIGIT")` 解析到 CaptchaType 枚举 |
| `captchaConfig.getWidth()` | 硬编码 `120`（宽高不在 sys_config 中，保持固定） |
| `captchaConfig.getHeight()` | 硬编码 `40` |
| `captchaConfig.getLineCount()` | 硬编码 `0` |

完整更新后的 CaptchaService（关键方法）：

```java
// 删除 @Autowired CaptchaConfig captchaConfig 字段
// 在类头部加 import

public CaptchaResponse generateCaptcha() {
    if (!ConfigUtil.getBoolean(ConfigKeys.CAPTCHA_ENABLED, true)) {
        return null;
    }
    String code = generateRandomCode();
    BufferedImage image = createCaptchaImage(code);
    String base64Image = imageToBase64(image);
    String captchaId = UUID.randomUUID().toString().replace("-", "");
    String redisKey = "captcha:" + captchaId;
    int expireSeconds = ConfigUtil.getInt(ConfigKeys.CAPTCHA_EXPIRE_SECONDS, 300);
    redisUtil.set(redisKey, code.toUpperCase(), expireSeconds, TimeUnit.SECONDS);
    log.info("生成验证码: id={}, code={}", captchaId, code);
    return new CaptchaResponse(captchaId, "data:image/png;base64," + base64Image, expireSeconds);
}

public boolean verifyCaptcha(String captchaId, String userInput) {
    if (!ConfigUtil.getBoolean(ConfigKeys.CAPTCHA_ENABLED, true)) {
        return true;
    }
    // ... 其余逻辑不变
}

private String generateRandomCode() {
    // 根据 ConfigUtil 读取 type，解析为字符集
    String typeStr = ConfigUtil.getString("security.captcha.type", "DIGIT");
    CaptchaConfig.CaptchaType type;
    try {
        type = CaptchaConfig.CaptchaType.valueOf(typeStr.toUpperCase());
    } catch (Exception e) {
        type = CaptchaConfig.CaptchaType.DIGIT;
    }
    String characters = type.getCharacters();
    int length = ConfigUtil.getInt(ConfigKeys.CAPTCHA_LENGTH, 4);
    StringBuilder code = new StringBuilder();
    for (int i = 0; i < length; i++) {
        code.append(characters.charAt(random.nextInt(characters.length())));
    }
    return code.toString();
}

private BufferedImage createCaptchaImage(String code) {
    int width = 120;   // 固定值
    int height = 40;   // 固定值
    // ... 其余不变，去掉 captchaConfig.getWidth/Height/LineCount 调用
    // lineCount 改为 0（固定）
}

public CaptchaConfigResponse getCaptchaConfig() {
    return new CaptchaConfigResponse(
            ConfigUtil.getBoolean(ConfigKeys.CAPTCHA_ENABLED, true),
            ConfigUtil.getString("security.captcha.type", "DIGIT"),
            ConfigUtil.getInt(ConfigKeys.CAPTCHA_LENGTH, 4),
            ConfigUtil.getInt(ConfigKeys.CAPTCHA_EXPIRE_SECONDS, 300)
    );
}
```

> **提示**：`CaptchaConfig.CaptchaType` 枚举暂时保留（generateRandomCode 仍用它解析字符集），但 `CaptchaConfig` 的 `@Configuration/@ConfigurationProperties` 部分删除——或整个文件删除，把 `CaptchaType` 枚举内联到 CaptchaService。推荐整个文件删除、枚举内联到 CaptchaService，更干净。

### Step 2: 删除 CaptchaConfig.java

```bash
rm seer-fitness-system/src/main/java/com/seer/fitness/system/config/CaptchaConfig.java
```

### Step 3: 编译验证（全量）

```bash
cd /Users/canjiemo/project/seer-fitness-edu && mvn clean compile -DskipTests --no-transfer-progress -q && echo "BUILD SUCCESS"
```

### Step 4: Commit

```bash
git add -A
git commit -m "refactor(config): CaptchaService 改用 ConfigUtil，删除 CaptchaConfig Bean"
```

---

## Task 7: 迁移密码策略（PasswordUtil 新增重载方法）

**Files:**
- Modify: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/utils/PasswordUtil.java`
- Modify: `seer-fitness-system/src/main/java/com/seer/fitness/system/service/UserService.java`（及其他调用 passwordUtil.encryptPassword 的地方）

### Step 1: PasswordUtil 新增接受原始参数的重载方法

在 `PasswordUtil.java` 末尾追加以下方法（保留原有注入 Config Bean 的方法不变，作为兼容）：

```java
/**
 * 验证密码强度（接受原始参数，不依赖注入的 Config Bean）
 */
public void validatePasswordStrength(String password,
                                     int minLen, int maxLen,
                                     boolean requireLower, boolean requireUpper,
                                     boolean requireDigit, boolean requireSpecial,
                                     String specialChars) {
    if (password == null || password.length() < minLen) {
        throw new BusinessException(String.format("密码长度不能少于%d位", minLen));
    }
    if (password.length() > maxLen) {
        throw new BusinessException(String.format("密码长度不能超过%d位", maxLen));
    }
    if (requireLower && !password.matches(".*[a-z].*")) {
        throw new BusinessException("密码必须包含小写字母");
    }
    if (requireUpper && !password.matches(".*[A-Z].*")) {
        throw new BusinessException("密码必须包含大写字母");
    }
    if (requireDigit && !password.matches(".*\\d.*")) {
        throw new BusinessException("密码必须包含数字");
    }
    if (requireSpecial && specialChars != null) {
        String pattern = ".*[" + Pattern.quote(specialChars) + "].*";
        if (!password.matches(pattern)) {
            throw new BusinessException("密码必须包含特殊字符：" + specialChars);
        }
    }
}

/**
 * 加密密码（接受原始参数，不依赖注入的 Config Bean）
 */
public String encryptPassword(String plainPassword,
                               int minLen, int maxLen,
                               boolean requireLower, boolean requireUpper,
                               boolean requireDigit, boolean requireSpecial,
                               String specialChars, int bcryptStrength) {
    validatePassword(plainPassword);
    validatePasswordStrength(plainPassword, minLen, maxLen,
            requireLower, requireUpper, requireDigit, requireSpecial, specialChars);
    return BCrypt.hashpw(plainPassword, BCrypt.gensalt(bcryptStrength));
}
```

### Step 2: 找到所有调用 passwordUtil.encryptPassword() 的地方

```bash
grep -rn "passwordUtil\.\|encryptPassword\|validatePasswordStrength" \
  seer-fitness-system/src --include="*.java"
```

### Step 3: 更新 UserService（及所有调用处）

将每处 `passwordUtil.encryptPassword(password)` 改为：

```java
passwordUtil.encryptPassword(
    password,
    ConfigUtil.getInt(ConfigKeys.PASSWORD_MIN_LENGTH, 8),
    ConfigUtil.getInt(ConfigKeys.PASSWORD_MAX_LENGTH, 15),
    ConfigUtil.getBoolean(ConfigKeys.PASSWORD_REQUIRE_LOWERCASE, true),
    ConfigUtil.getBoolean(ConfigKeys.PASSWORD_REQUIRE_UPPERCASE, true),
    ConfigUtil.getBoolean(ConfigKeys.PASSWORD_REQUIRE_DIGIT, true),
    ConfigUtil.getBoolean(ConfigKeys.PASSWORD_REQUIRE_SPECIAL, true),
    "!@#$%^&*()_+-=[]{}|;:,.<>?",
    12   // bcryptStrength 固定不动（不应运行时改）
)
```

初始密码读取处（init-password/reset-password）：
```java
String initialPassword = ConfigUtil.getString(ConfigKeys.PASSWORD_INITIAL, "Aa123456!");
```

### Step 4: 编译验证

```bash
cd /Users/canjiemo/project/seer-fitness-edu && mvn clean compile -DskipTests --no-transfer-progress -q && echo "BUILD SUCCESS"
```

### Step 5: Commit

```bash
git add -A
git commit -m "refactor(config): 密码策略改从 ConfigUtil 读取，PasswordUtil 新增参数化重载方法"
```

---

## Task 8: 迁移账户锁定策略（AccountLockService + LockTimeCalculator + LockMessageBuilder）

**Files:**
- Modify: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/utils/LockTimeCalculator.java`
- Modify: `seer-fitness-framework/src/main/java/com/seer/fitness/framework/utils/LockMessageBuilder.java`
- Modify: `seer-fitness-system/src/main/java/com/seer/fitness/system/service/AccountLockService.java`

### Step 1: LockTimeCalculator 新增参数化重载方法

在 `LockTimeCalculator.java` 追加：

```java
/**
 * 渐进式锁定时间计算（参数化，不依赖注入的 Config Bean）
 */
public long calculateProgressiveLockMinutes(int failAttempts, int maxFailCount,
                                             int baseMinutes, double multiplier, int maxMinutes) {
    int overAttempts = failAttempts - maxFailCount;
    long lockMinutes = (long) (baseMinutes * Math.pow(multiplier, overAttempts));
    return Math.min(lockMinutes, maxMinutes);
}
```

### Step 2: Read LockMessageBuilder，追加重载方法

先 Read `LockMessageBuilder.java`，然后追加：

```java
/**
 * 构造失败提示（参数化，接受 maxFailCount）
 */
public String buildFailMessage(int attempts, int maxFailCount) {
    int remaining = maxFailCount - attempts;
    return config.getMessages().getFailTemplate()
            .replace("{remaining}", String.valueOf(remaining));
}
```

> **注意**：`buildFailMessage(int attempts, int maxFailCount)` 使用注入的 `config.getMessages().getFailTemplate()` 模板（模板本身不迁移到 DB），只是 maxFailCount 从外部传入。

### Step 3: 更新 AccountLockService

将所有 `lockConfig.*` 调用替换为 ConfigUtil + 新重载方法：

```java
// 删除 @Autowired AccountLockConfig lockConfig 字段注入
// 改为注入 ConfigUtil 所需的 ISysConfigService（ConfigUtil 是静态工具，无需注入）

// 替换映射：
// lockConfig.isEnabled()  →  ConfigUtil.getBoolean(ConfigKeys.LOCK_ENABLED, true)
// lockConfig.getAttempts().getMaxFailCount()  →  ConfigUtil.getInt(ConfigKeys.LOCK_MAX_FAIL_COUNT, 5)
// lockConfig.getReset().getAutoResetHours()  →  ConfigUtil.getInt(ConfigKeys.LOCK_AUTO_RESET_HOURS, 24)
// lockConfig.getReset().isOnSuccess()  →  ConfigUtil.getBoolean("security.account-lock.reset.on-success", true)
// lockConfig.getIpLock().isEnabled()  →  ConfigUtil.getBoolean(ConfigKeys.LOCK_IP_ENABLED, true)
// lockConfig.getIpLock().getMaxAttempts()  →  ConfigUtil.getInt(ConfigKeys.LOCK_IP_MAX_ATTEMPTS, 20)
// lockConfig.getIpLock().getLockMinutes()  →  ConfigUtil.getInt(ConfigKeys.LOCK_IP_LOCK_MINUTES, 60)
// lockConfig.getIpLock().getRecordHours()  →  ConfigUtil.getInt("security.account-lock.ip-lock.record-hours", 2)
// lockConfig.getWhitelist().getUsers().contains(username)  →  ConfigUtil.getList(ConfigKeys.LOCK_WHITELIST_USERS).contains(username)
// lockConfig.getWhitelist().getIps().contains(ip)  →  ConfigUtil.getList(ConfigKeys.LOCK_WHITELIST_IPS).contains(ip)
// messageBuilder.buildFailMessage(attempts)  →  messageBuilder.buildFailMessage(attempts, ConfigUtil.getInt(ConfigKeys.LOCK_MAX_FAIL_COUNT, 5))
// lockTimeCalculator.calculateLockMinutes(failAttempts)  →  lockTimeCalculator.calculateProgressiveLockMinutes(
//     failAttempts,
//     ConfigUtil.getInt(ConfigKeys.LOCK_MAX_FAIL_COUNT, 5),
//     ConfigUtil.getInt(ConfigKeys.LOCK_BASE_MINUTES, 30),
//     2.0,   // multiplier 固定（可以后续加 ConfigKey）
//     1440   // maxMinutes 固定（可以后续加 ConfigKey）
// )
```

### Step 4: 编译验证

```bash
cd /Users/canjiemo/project/seer-fitness-edu && mvn clean compile -DskipTests --no-transfer-progress -q && echo "BUILD SUCCESS"
```

### Step 5: Commit

```bash
git add -A
git commit -m "refactor(config): 账户锁定策略改从 ConfigUtil 读取，更新 LockTimeCalculator/LockMessageBuilder 重载方法"
```

---

## Task 9: 删除 yml 中已迁移的配置

**Files:**
- Modify: `seer-fitness-boot/src/main/resources/application.yml`
- Check & confirm: `application-local.yml`、`application-dev.yml` 无迁移项

### Step 1: 确认 local/dev yml 没有需要删除的迁移项

```bash
grep -n "captcha\|account-lock\|password\|scheduler" \
  seer-fitness-boot/src/main/resources/application-local.yml \
  seer-fitness-boot/src/main/resources/application-dev.yml
```

如有匹配，一并删除。

### Step 2: 删除 application.yml 中的迁移节

从 `application.yml` 中删除以下整个块：

```yaml
# 删除整个 security: 块（captcha + password + account-lock）
security:
  captcha: ...
  password: ...
  account-lock: ...

# 删除 scheduler 块
scheduler:
  pool-size: 5
```

保留 `jwt:` 节（secret/expiration 不迁移）。

### Step 3: 编译验证（全量）

```bash
cd /Users/canjiemo/project/seer-fitness-edu && mvn clean compile -DskipTests --no-transfer-progress -q && echo "BUILD SUCCESS"
```

如报 `ConfigurationProperties binding failed`：找到仍绑定 yml 的 Config Bean（AccountLockConfig/PasswordPolicyConfig），去掉 `@ConfigurationProperties` 或改为不绑定（类仍保留供 LockMessageBuilder 的 message template 使用，但不再从 yml 读取）。

### Step 4: Commit

```bash
git add seer-fitness-boot/src/main/resources/application.yml
git add seer-fitness-boot/src/main/resources/application-local.yml
git add seer-fitness-boot/src/main/resources/application-dev.yml
git commit -m "refactor(config): 删除 yml 中已迁移至 sys_config 的配置项"
```

---

## 验证清单

```bash
# 1. 整体编译
mvn clean compile -DskipTests --no-transfer-progress
# 期望：BUILD SUCCESS

# 2. 确认 yml 没有残留迁移项
grep -rn "security.captcha\|security.password\|security.account-lock\|scheduler.pool-size" \
  seer-fitness-boot/src/main/resources/ --include="*.yml"
# 期望：无输出

# 3. 确认 CaptchaConfig 已删除
ls seer-fitness-system/src/main/java/com/seer/fitness/system/config/
# 期望：无 CaptchaConfig.java

# 4. 查看最近提交
git log --oneline -10
```
