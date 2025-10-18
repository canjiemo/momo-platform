package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.entity.SysTenantInitLog;
import com.seer.fitness.system.enums.InitStepType;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 租户Schema管理服务实现
 * 负责租户Schema的自动创建、初始化和回滚
 * <p>
 * 核心流程：
 * 1. CREATE SCHEMA - 创建独立的Schema
 * 2. CREATE TABLE - 执行DDL脚本创建所有表
 * 3. CREATE ADMIN - 创建租户管理员账号
 * 4. LOG STEP - 记录每个步骤的执行日志
 * 5. ROLLBACK - 失败时自动回滚（删除Schema）
 *
 * 注意 (2025-10-17 更新):
 * - 菜单数据由平台分配，不在此处插入
 * - 角色由租户管理员自行创建
 * - 仅创建超级管理员账号
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class TenantSchemaService extends BaseServiceImpl implements ITenantSchemaService {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private TenantTemplateAutoSyncService templateAutoSyncService;

    @Autowired
    private com.seer.fitness.system.config.FlywayMultiTenantConfig flywayMultiTenantConfig;

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * SQL脚本路径（在resources目录下）
     */
    private static final String SCHEMA_TEMPLATE_SQL = "sql/tenant/tenant_schema_template.sql";

    /**
     * @deprecated 已废弃 (2025-10-17) - 新架构不再使用SQL脚本初始化数据
     */
    @Deprecated
    private static final String INIT_DATA_SQL = "sql/tenant/tenant_init_data.sql";

    /**
     * 创建Schema并初始化表结构和超级管理员
     * 完整流程包含4个步骤，每个步骤都会记录日志
     *
     * 注意：菜单和角色不在此处初始化
     * - 菜单：通过平台分配接口分配
     * - 角色：租户管理员自行创建
     */
    @Override
    @Transactional(readOnly = false)
    public void createSchemaAndInitTables(Long tenantId, String schemaName,
                                         String adminUsername, String adminRealName, String adminPassword) throws Exception {
        log.info("开始创建租户Schema: tenantId={}, schemaName={}, adminUsername={}",
                tenantId, schemaName, adminUsername);

        // 检查Schema是否已存在
        if (schemaExists(schemaName)) {
            throw new BusinessException("Schema已存在：" + schemaName);
        }

        try {
            // 步骤1：创建Schema
            logStep(tenantId, InitStepType.CREATE_SCHEMA, "开始创建Schema", 0);
            createSchema(schemaName);
            logStep(tenantId, InitStepType.CREATE_SCHEMA, "Schema创建成功", 1);

            // 步骤2：执行DDL脚本（创建表）
            logStep(tenantId, InitStepType.CREATE_TABLE, "开始执行DDL脚本", 0);
            executeDdlScript(schemaName);
            logStep(tenantId, InitStepType.CREATE_TABLE, "DDL脚本执行成功", 1);

            // 步骤2.5：初始化Flyway基线（2025-10-18新增）
            logStep(tenantId, InitStepType.INIT_FLYWAY, "开始初始化Flyway版本管理基线", 0);
            initFlywayBaseline(tenantId, schemaName);
            logStep(tenantId, InitStepType.INIT_FLYWAY, "Flyway版本管理基线初始化成功", 1);

            // 步骤3：创建管理员账号
            logStep(tenantId, InitStepType.CREATE_ADMIN, "开始创建管理员账号", 0);
            createAdminUser(schemaName, adminUsername, adminRealName, adminPassword);
            logStep(tenantId, InitStepType.CREATE_ADMIN, "管理员账号创建成功", 1);

            // 步骤4：自动同步菜单和角色模板（2025-10-18新增）
            logStep(tenantId, InitStepType.SYNC_TEMPLATES, "开始自动同步菜单和角色模板", 0);
            autoSyncTemplates(tenantId);
            logStep(tenantId, InitStepType.SYNC_TEMPLATES, "菜单和角色模板同步成功", 1);

            log.info("租户Schema创建成功（含模板自动同步）: schemaName={}", schemaName);

        } catch (Exception e) {
            // 记录失败日志
            logStep(tenantId, InitStepType.ROLLBACK, "初始化失败，开始回滚: " + e.getMessage(), 0);

            // 自动回滚：删除Schema
            try {
                dropSchema(schemaName);
                logStep(tenantId, InitStepType.ROLLBACK, "回滚成功，Schema已删除", 1);
            } catch (Exception rollbackError) {
                logStep(tenantId, InitStepType.ROLLBACK, "回滚失败: " + rollbackError.getMessage(), 2);
                log.error("回滚失败: schemaName={}", schemaName, rollbackError);
            }

            log.error("租户Schema创建失败: schemaName={}", schemaName, e);
            throw new BusinessException("Schema初始化失败：" + e.getMessage());
        }
    }

    /**
     * 创建Schema
     */
    private void createSchema(String schemaName) {
        String sql = "CREATE SCHEMA " + schemaName;
        jdbcTemplate.getJdbcTemplate().execute(sql);
        log.info("Schema创建成功: {}", schemaName);
    }

    /**
     * 执行DDL脚本（创建表结构）
     */
    private void executeDdlScript(String schemaName) throws Exception {
        // 1. 加载SQL脚本
        String sqlScript = loadSqlScript(SCHEMA_TEMPLATE_SQL);

        // 2. 切换到目标Schema
        setSearchPath(schemaName);

        // 3. 执行SQL脚本
        executeSqlScript(sqlScript);

        // 4. 切换回public schema
        resetSearchPath();

        log.info("DDL脚本执行成功: schemaName={}", schemaName);
    }

    /**
     * 执行基础数据脚本（插入角色、菜单等）
     *
     * @deprecated 已废弃 (2025-10-17)
     * 原因：新架构不再通过SQL脚本初始化菜单和角色数据
     * - 菜单：通过平台分配接口分配
     * - 角色：租户管理员自行创建
     *
     * 保留此方法仅用于向后兼容，实际已不再调用
     */
    @Deprecated
    private void executeInitDataScript(String schemaName) throws Exception {
        // 此方法已废弃，不再使用
        log.warn("executeInitDataScript 方法已废弃，新架构不再通过SQL初始化数据");

        // 原实现已注释
        // String sqlScript = loadSqlScript(INIT_DATA_SQL);
        // setSearchPath(schemaName);
        // executeSqlScript(sqlScript);
        // resetSearchPath();
    }

    /**
     * 创建管理员账号
     */
    private void createAdminUser(String schemaName, String username, String realName, String password) {
        try {
            // 加密密码
            String encodedPassword = PASSWORD_ENCODER.encode(password);

            // 切换到目标Schema
            setSearchPath(schemaName);

            // 生成用户ID（使用时间戳 + 随机数）
            long userId = System.currentTimeMillis();

            // 插入管理员用户
            String insertUserSql = "INSERT INTO sys_user (id, username, password, real_name, admin_flag, status, delete_flag, created_at, updated_at) " +
                    "VALUES (:id, :username, :password, :realName, 1, 1, 0, :now, :now)";

            Map<String, Object> params = Maps.newHashMap();
            params.put("id", userId);
            params.put("username", username);
            params.put("password", encodedPassword);
            params.put("realName", realName);
            params.put("now", LocalDateTime.now());

            jdbcTemplate.update(insertUserSql, params);

            // 切换回public schema
            resetSearchPath();

            log.info("管理员账号创建成功: schemaName={}, username={}", schemaName, username);

        } catch (Exception e) {
            resetSearchPath(); // 确保切换回public
            throw new BusinessException("创建管理员账号失败：" + e.getMessage());
        }
    }

    /**
     * 切换到指定Schema
     */
    private void setSearchPath(String schemaName) {
        String sql = "SET search_path TO " + schemaName;
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    /**
     * 切换回public schema
     */
    private void resetSearchPath() {
        String sql = "SET search_path TO public";
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    /**
     * 执行SQL脚本（支持多条SQL语句）
     */
    private void executeSqlScript(String sqlScript) {
        // 按分号分割SQL语句
        String[] sqlStatements = sqlScript.split(";");

        for (String sql : sqlStatements) {
            String trimmedSql = sql.trim();
            // 跳过空语句和注释
            if (trimmedSql.isEmpty() || trimmedSql.startsWith("--")) {
                continue;
            }
            // 执行SQL
            jdbcTemplate.getJdbcTemplate().execute(trimmedSql);
        }
    }

    /**
     * 从resources目录加载SQL脚本
     */
    private String loadSqlScript(String resourcePath) throws Exception {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            byte[] bdata = FileCopyUtils.copyToByteArray(resource.getInputStream());
            return new String(bdata, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("加载SQL脚本失败: {}", resourcePath, e);
            throw new Exception("加载SQL脚本失败：" + resourcePath, e);
        }
    }

    /**
     * 记录初始化步骤日志
     *
     * @param tenantId 租户ID
     * @param stepType 步骤类型
     * @param message  日志消息
     * @param status   状态：0-处理中 1-成功 2-失败
     */
    private void logStep(Long tenantId, InitStepType stepType, String message, Integer status) {
        try {
            SysTenantInitLog log = new SysTenantInitLog();
            log.setTenantId(tenantId);
            log.setStepName(stepType.getDescription());
            log.setStepType(stepType.getCode());
            log.setStatus(status);
            log.setErrorMessage(message);
            log.setCreatedAt(LocalDateTime.now());

            baseDao.insertPO(log, true);

        } catch (Exception e) {
            // 日志记录失败不影响主流程
            log.error("记录初始化日志失败: tenantId={}, stepType={}", tenantId, stepType, e);
        }
    }

    /**
     * 删除Schema（慎用！）
     */
    @Override
    @Transactional(readOnly = false)
    public void dropSchema(String schemaName) throws Exception {
        if (!schemaExists(schemaName)) {
            log.warn("Schema不存在，无需删除: {}", schemaName);
            return;
        }

        String sql = "DROP SCHEMA " + schemaName + " CASCADE";
        jdbcTemplate.getJdbcTemplate().execute(sql);
        log.warn("Schema已删除: {}", schemaName);
    }

    /**
     * 检查Schema是否存在
     */
    @Override
    public boolean schemaExists(String schemaName) {
        String sql = "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = :schemaName";
        Map<String, Object> params = Maps.newHashMap();
        params.put("schemaName", schemaName);

        Long count = baseDao.querySingleForSql(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * 验证Schema完整性
     * 检查关键表是否存在
     */
    @Override
    public boolean validateSchema(String schemaName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = :schemaName AND table_name = 'sys_user'";
            Map<String, Object> params = Maps.newHashMap();
            params.put("schemaName", schemaName);

            Long count = baseDao.querySingleForSql(sql, params, Long.class);
            return count != null && count > 0;

        } catch (Exception e) {
            log.error("验证Schema失败: {}", schemaName, e);
            return false;
        }
    }

    /**
     * 自动同步菜单和角色模板到新租户
     *
     * @param tenantId 租户ID
     */
    private void autoSyncTemplates(Long tenantId) {
        try {
            // 查询租户的 feature_level（如果没有则使用默认值1=基础版）
            String sql = "SELECT feature_level FROM sys_tenant WHERE id = :id";
            Map<String, Object> params = Maps.newHashMap();
            params.put("id", tenantId);

            Integer featureLevel = baseDao.querySingleForSql(sql, params, Integer.class);
            if (featureLevel == null || featureLevel < 1 || featureLevel > 3) {
                log.warn("租户功能级别无效，使用默认基础版: tenantId={}, featureLevel={}", tenantId, featureLevel);
                featureLevel = 1; // 默认基础版
            }

            log.info("开始自动同步模板到租户: tenantId={}, featureLevel={}", tenantId, featureLevel);

            // 调用自动同步服务（使用系统管理员ID=1作为操作人）
            Long systemAdminId = 1L;
            templateAutoSyncService.autoSyncTemplates(tenantId, featureLevel, systemAdminId);

            log.info("自动同步模板完成: tenantId={}", tenantId);

        } catch (Exception e) {
            log.error("自动同步模板失败: tenantId={}", tenantId, e);
            throw new BusinessException("自动同步菜单和角色模板失败：" + e.getMessage());
        }
    }

    /**
     * 初始化Flyway版本管理基线
     * <p>
     * 功能：
     * 1. 使用FlywayMultiTenantConfig为租户Schema建立基线
     * 2. 更新public.sys_schema_version表记录版本信息
     * <p>
     * 注意：
     * - 基线版本号为1.0.0
     * - 基线建立后，后续的迁移将从1.0.0版本开始
     * - 此方法在表结构创建之后调用
     *
     * @param tenantId   租户ID
     * @param schemaName Schema名称
     */
    private void initFlywayBaseline(Long tenantId, String schemaName) {
        try {
            log.info("开始为Schema初始化Flyway基线: tenantId={}, schemaName={}", tenantId, schemaName);

            // 1. 使用FlywayMultiTenantConfig执行基线
            String baselineVersion = flywayMultiTenantConfig.baselineSchema(schemaName);

            // 2. 更新public.sys_schema_version表
            String updateVersionSql = "INSERT INTO public.sys_schema_version " +
                    "(tenant_id, schema_name, current_version, flyway_version, " +
                    "is_baseline, baseline_version, baseline_description, " +
                    "last_upgraded_at, last_upgraded_by, created_at, updated_at, delete_flag) " +
                    "VALUES (:tenantId, :schemaName, :currentVersion, :flywayVersion, " +
                    ":isBaseline, :baselineVersion, :baselineDescription, " +
                    ":lastUpgradedAt, :lastUpgradedBy, :createdAt, :updatedAt, 0) " +
                    "ON CONFLICT (schema_name, delete_flag) DO UPDATE SET " +
                    "current_version = :currentVersion, " +
                    "flyway_version = :flywayVersion, " +
                    "last_upgraded_at = :lastUpgradedAt, " +
                    "updated_at = :updatedAt";

            Map<String, Object> params = Maps.newHashMap();
            params.put("tenantId", tenantId);
            params.put("schemaName", schemaName);
            params.put("currentVersion", baselineVersion);
            params.put("flywayVersion", baselineVersion);
            params.put("isBaseline", true);
            params.put("baselineVersion", baselineVersion);
            params.put("baselineDescription", "租户初始基线版本");
            params.put("lastUpgradedAt", LocalDateTime.now());
            params.put("lastUpgradedBy", "SYSTEM");
            params.put("createdAt", LocalDateTime.now());
            params.put("updatedAt", LocalDateTime.now());

            jdbcTemplate.update(updateVersionSql, params);

            log.info("Flyway基线初始化成功: schemaName={}, version={}", schemaName, baselineVersion);

        } catch (Exception e) {
            log.error("Flyway基线初始化失败: schemaName={}", schemaName, e);
            throw new BusinessException("Flyway基线初始化失败：" + e.getMessage());
        }
    }
}
