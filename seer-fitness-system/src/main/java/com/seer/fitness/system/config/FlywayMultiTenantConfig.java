package com.seer.fitness.system.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Flyway多租户配置类
 * 为每个租户Schema创建独立的Flyway实例，管理数据库版本迁移
 * <p>
 * 功能：
 * 1. 为指定Schema创建Flyway实例
 * 2. 执行基线初始化（baseline）
 * 3. 执行版本迁移（migrate）
 * 4. 校验迁移状态（validate）
 * <p>
 * 使用场景：
 * - 新建租户时：初始化Schema并建立基线
 * - 批量升级时：对多个租户Schema执行迁移
 * - 版本校验时：检查Schema与迁移脚本是否一致
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Slf4j
@Component
public class FlywayMultiTenantConfig {

    private final DataSource dataSource;

    @Value("${spring.flyway.baseline-version:1.0.0}")
    private String baselineVersion;

    @Value("${spring.flyway.encoding:UTF-8}")
    private String encoding;

    @Value("${spring.flyway.validate-on-migrate:true}")
    private boolean validateOnMigrate;

    public FlywayMultiTenantConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 为指定Schema创建Flyway实例（租户模式）
     * 使用tenant目录下的迁移脚本
     *
     * @param schemaName Schema名称
     * @return 配置好的Flyway实例
     */
    public Flyway createFlywayForTenantSchema(String schemaName) {
        log.info("创建租户Schema的Flyway实例: {}", schemaName);

        return Flyway.configure()
                .dataSource(dataSource)
                // 迁移脚本位置：仅使用tenant目录
                .locations("classpath:db/migration/tenant")
                // 指定Schema
                .schemas(schemaName)
                // 设置默认Schema（SQL执行上下文）
                .defaultSchema(schemaName)
                // 允许Flyway创建schema（schema由Flyway管理）
                .createSchemas(true)
                // 基线配置
                .baselineOnMigrate(true)
                .baselineVersion(baselineVersion)
                .baselineDescription("租户初始基线版本")
                // Flyway历史表名称
                .table("flyway_schema_history")
                // 编码
                .encoding(encoding)
                // 迁移前验证
                .validateOnMigrate(validateOnMigrate)
                // 禁止乱序执行
                .outOfOrder(false)
                // 禁用占位符替换
                .placeholderReplacement(false)
                // 构建Flyway实例
                .load();
    }

    /**
     * 为public Schema创建Flyway实例（平台模式）
     * 使用public目录下的迁移脚本
     *
     * @return 配置好的Flyway实例
     */
    public Flyway createFlywayForPublicSchema() {
        log.info("创建public Schema的Flyway实例");

        return Flyway.configure()
                .dataSource(dataSource)
                // 迁移脚本位置：仅使用public目录
                .locations("classpath:db/migration/public")
                // 指定Schema
                .schemas("public")
                // 设置默认Schema（SQL执行上下文）
                .defaultSchema("public")
                // public schema已存在，禁止Flyway自动创建
                .createSchemas(false)
                // 基线配置
                .baselineOnMigrate(true)
                .baselineVersion(baselineVersion)
                .baselineDescription("平台初始基线版本")
                // Flyway历史表名称
                .table("flyway_schema_history")
                // 编码
                .encoding(encoding)
                // 迁移前验证
                .validateOnMigrate(validateOnMigrate)
                // 禁止乱序执行
                .outOfOrder(false)
                // 禁用占位符替换
                .placeholderReplacement(false)
                // 构建Flyway实例
                .load();
    }

    /**
     * 为指定Schema执行基线初始化
     * <p>
     * 适用场景：
     * - 现有Schema需要纳入Flyway管理
     * - 手动建立基线版本
     *
     * @param schemaName Schema名称
     * @return 基线版本号
     */
    public String baselineSchema(String schemaName) {
        try {
            log.info("为Schema执行基线初始化: {}", schemaName);
            Flyway flyway = createFlywayForTenantSchema(schemaName);
            flyway.baseline();
            log.info("Schema基线初始化成功: {}, 基线版本: {}", schemaName, baselineVersion);
            return baselineVersion;
        } catch (Exception e) {
            log.error("Schema基线初始化失败: {}", schemaName, e);
            throw new RuntimeException("基线初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 为指定Schema执行数据库迁移
     *
     * @param schemaName Schema名称
     * @return 成功执行的迁移数量
     */
    public int migrateSchema(String schemaName) {
        try {
            log.info("开始执行Schema迁移: {}", schemaName);
            Flyway flyway = createFlywayForTenantSchema(schemaName);
            int migrationsExecuted = flyway.migrate().migrationsExecuted;
            log.info("Schema迁移完成: {}, 执行了{}个迁移", schemaName, migrationsExecuted);
            return migrationsExecuted;
        } catch (Exception e) {
            log.error("Schema迁移失败: {}", schemaName, e);
            throw new RuntimeException("Schema迁移失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证Schema的迁移状态
     * 检查已应用的迁移是否与迁移脚本一致
     *
     * @param schemaName Schema名称
     * @return true表示验证通过
     */
    public boolean validateSchema(String schemaName) {
        try {
            log.info("验证Schema迁移状态: {}", schemaName);
            Flyway flyway = createFlywayForTenantSchema(schemaName);
            flyway.validate();
            log.info("Schema迁移状态验证通过: {}", schemaName);
            return true;
        } catch (Exception e) {
            log.error("Schema迁移状态验证失败: {}", schemaName, e);
            return false;
        }
    }

    /**
     * 获取Schema的当前迁移版本
     *
     * @param schemaName Schema名称
     * @return 当前版本号，如果未初始化返回null
     */
    public String getCurrentVersion(String schemaName) {
        try {
            Flyway flyway = createFlywayForTenantSchema(schemaName);
            var info = flyway.info();
            var current = info.current();
            if (current != null) {
                return current.getVersion().getVersion();
            }
            return null;
        } catch (Exception e) {
            log.error("获取Schema版本失败: {}", schemaName, e);
            return null;
        }
    }

    /**
     * 清理Schema的Flyway历史记录（慎用！）
     * 仅用于开发测试环境
     *
     * @param schemaName Schema名称
     */
    public void cleanSchema(String schemaName) {
        log.warn("清理Schema的Flyway历史记录（危险操作）: {}", schemaName);
        try {
            Flyway flyway = createFlywayForTenantSchema(schemaName);
            flyway.clean();
            log.info("Schema清理完成: {}", schemaName);
        } catch (Exception e) {
            log.error("Schema清理失败: {}", schemaName, e);
            throw new RuntimeException("Schema清理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 修复Schema的迁移状态
     * 用于解决迁移失败后的问题
     *
     * @param schemaName Schema名称
     * @return 修复的迁移数量
     */
    public int repairSchema(String schemaName) {
        try {
            log.info("修复Schema迁移状态: {}", schemaName);
            Flyway flyway = createFlywayForTenantSchema(schemaName);
            flyway.repair();
            log.info("Schema修复完成: {}", schemaName);
            return 1;
        } catch (Exception e) {
            log.error("Schema修复失败: {}", schemaName, e);
            throw new RuntimeException("Schema修复失败: " + e.getMessage(), e);
        }
    }
}
