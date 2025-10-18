package com.seer.fitness.system.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway配置类
 * 禁用Spring Boot的自动Flyway迁移，改为手动控制
 * <p>
 * 说明：
 * - 在多租户架构中，每个租户Schema需要独立管理版本
 * - 禁用自动迁移，通过FlywayMultiTenantConfig统一管理
 * - 新租户创建时，在TenantSchemaService中手动初始化Flyway基线
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Slf4j
@Configuration
public class FlywayConfig {

    /**
     * 禁用自动迁移策略
     * 返回一个空策略，防止Spring Boot在启动时自动执行Flyway迁移
     * <p>
     * 原因：
     * 1. 多租户系统需要对每个Schema单独执行迁移
     * 2. 迁移时机由业务逻辑控制（新建租户、批量升级等）
     * 3. 避免启动时对public schema执行不必要的迁移
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // 不执行任何迁移操作
            log.info("Flyway自动迁移已禁用，将通过FlywayMultiTenantConfig手动控制迁移");
        };
    }
}
