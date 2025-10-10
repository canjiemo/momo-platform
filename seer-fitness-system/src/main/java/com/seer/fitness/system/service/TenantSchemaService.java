package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.entity.SysTenantInitLog;
import com.seer.fitness.system.enums.InitStepType;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
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
 * 3. INSERT DATA - 插入基础数据（字典、角色、菜单）
 * 4. CREATE ADMIN - 创建租户管理员账号
 * 5. LOG STEP - 记录每个步骤的执行日志
 * 6. ROLLBACK - 失败时自动回滚（删除Schema）
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class TenantSchemaService extends BaseServiceImpl implements ITenantSchemaService {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * SQL脚本路径（在resources目录下）
     */
    private static final String SCHEMA_TEMPLATE_SQL = "sql/tenant/tenant_schema_template.sql";
    private static final String INIT_DATA_SQL = "sql/tenant/tenant_init_data.sql";

    /**
     * 创建Schema并初始化表结构和数据
     * 完整流程包含7个步骤，每个步骤都会记录日志
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

            // 步骤3：插入基础数据
            logStep(tenantId, InitStepType.INSERT_DATA, "开始插入基础数据", 0);
            executeInitDataScript(schemaName);
            logStep(tenantId, InitStepType.INSERT_DATA, "基础数据插入成功", 1);

            // 步骤4：创建管理员账号
            logStep(tenantId, InitStepType.CREATE_ADMIN, "开始创建管理员账号", 0);
            createAdminUser(schemaName, adminUsername, adminRealName, adminPassword);
            logStep(tenantId, InitStepType.CREATE_ADMIN, "管理员账号创建成功", 1);

            log.info("租户Schema创建成功: schemaName={}", schemaName);

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
            throw new BusinessException("Schema初始化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 创建Schema
     */
    private void createSchema(String schemaName) {
        String sql = "CREATE SCHEMA " + schemaName;
        baseDao.executeSQL(sql);
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
     */
    private void executeInitDataScript(String schemaName) throws Exception {
        // 1. 加载SQL脚本
        String sqlScript = loadSqlScript(INIT_DATA_SQL);

        // 2. 切换到目标Schema
        setSearchPath(schemaName);

        // 3. 执行SQL脚本
        executeSqlScript(sqlScript);

        // 4. 切换回public schema
        resetSearchPath();

        log.info("基础数据插入成功: schemaName={}", schemaName);
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

            baseDao.executeSQLWithParam(insertUserSql, params);

            // 切换回public schema
            resetSearchPath();

            log.info("管理员账号创建成功: schemaName={}, username={}", schemaName, username);

        } catch (Exception e) {
            resetSearchPath(); // 确保切换回public
            throw new BusinessException("创建管理员账号失败：" + e.getMessage(), e);
        }
    }

    /**
     * 切换到指定Schema
     */
    private void setSearchPath(String schemaName) {
        String sql = "SET search_path TO " + schemaName;
        baseDao.executeSQL(sql);
    }

    /**
     * 切换回public schema
     */
    private void resetSearchPath() {
        String sql = "SET search_path TO public";
        baseDao.executeSQL(sql);
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
            baseDao.executeSQL(trimmedSql);
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
            log.setMessage(message);
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
        baseDao.executeSQL(sql);
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
}
