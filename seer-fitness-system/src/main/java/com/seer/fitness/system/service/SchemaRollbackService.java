package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.config.FlywayMultiTenantConfig;
import com.seer.fitness.system.dto.RollbackResult;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Schema回滚服务实现
 * <p>
 * 注意：Flyway Community Edition不支持自动回滚（undo）
 * 该实现提供基于repair的状态修复和手动回滚记录
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Service
@Slf4j
public class SchemaRollbackService extends BaseServiceImpl implements ISchemaRollbackService {

    @Autowired
    private FlywayMultiTenantConfig flywayMultiTenantConfig;

    /**
     * 回滚Schema到指定版本
     * <p>
     * 实现策略：
     * 1. Flyway repair修复状态
     * 2. 记录回滚操作到version_history
     * 3. 需要DBA手动执行回滚脚本
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RollbackResult rollbackSchema(String schemaName, String targetVersion) {
        LocalDateTime startTime = LocalDateTime.now();
        String fromVersion = null;

        try {
            log.info("开始回滚Schema: schema={}, targetVersion={}", schemaName, targetVersion);

            // 1. 验证回滚安全性
            if (!validateRollback(schemaName, targetVersion)) {
                throw new BusinessException("回滚验证失败：目标版本不存在或不安全");
            }

            // 2. 获取当前版本
            fromVersion = flywayMultiTenantConfig.getCurrentVersion(schemaName);
            log.info("Schema当前版本: schema={}, version={}", schemaName, fromVersion);

            // 3. 执行Flyway repair（修复迁移状态）
            log.info("执行Flyway repair: schema={}", schemaName);
            flywayMultiTenantConfig.repairSchema(schemaName);

            // 4. 更新Schema版本记录
            updateSchemaVersionForRollback(schemaName, fromVersion, targetVersion);

            // 5. 记录回滚历史
            recordRollbackHistory(schemaName, fromVersion, targetVersion);

            int executionTime = (int) java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();

            log.warn("Schema回滚完成（需要DBA手动执行回滚脚本）: schema={}, from={}, to={}",
                    schemaName, fromVersion, targetVersion);

            return RollbackResult.builder()
                    .success(true)
                    .schemaName(schemaName)
                    .fromVersion(fromVersion)
                    .toVersion(targetVersion)
                    .rolledBackAt(LocalDateTime.now())
                    .executionTime(executionTime)
                    .build();

        } catch (Exception e) {
            log.error("Schema回滚失败: schema={}, error={}", schemaName, e.getMessage(), e);

            int executionTime = (int) java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();

            return RollbackResult.builder()
                    .success(false)
                    .schemaName(schemaName)
                    .fromVersion(fromVersion)
                    .toVersion(targetVersion)
                    .errorMessage(e.getMessage())
                    .rolledBackAt(LocalDateTime.now())
                    .executionTime(executionTime)
                    .build();
        }
    }

    /**
     * 验证回滚安全性
     */
    @Override
    public boolean validateRollback(String schemaName, String targetVersion) {
        try {
            // 1. 检查Schema是否存在 - 使用COUNT查询
            String checkSchemaSql = "SELECT COUNT(*) FROM public.sys_schema_version WHERE schema_name = :schemaName";
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("schemaName", schemaName);

            Integer count = namedParameterJdbcTemplate.queryForObject(checkSchemaSql, params, Integer.class);
            if (count == null || count == 0) {
                log.error("Schema不存在: {}", schemaName);
                return false;
            }

            // 2. 检查目标版本是否在历史版本中
            List<String> availableVersions = getAvailableVersions(schemaName);
            if (!availableVersions.contains(targetVersion)) {
                log.error("目标版本不存在: schema={}, targetVersion={}", schemaName, targetVersion);
                return false;
            }

            // 3. 获取当前版本
            String currentVersion = flywayMultiTenantConfig.getCurrentVersion(schemaName);

            // 4. 检查是否是向后回滚（版本号降级）
            if (currentVersion != null && currentVersion.equals(targetVersion)) {
                log.warn("目标版本与当前版本相同: schema={}, version={}", schemaName, currentVersion);
                return false;
            }

            log.info("回滚验证通过: schema={}, from={}, to={}", schemaName, currentVersion, targetVersion);
            return true;

        } catch (Exception e) {
            log.error("回滚验证异常: schema={}, targetVersion={}", schemaName, targetVersion, e);
            return false;
        }
    }

    /**
     * 获取可回滚的版本列表
     */
    @Override
    public List<String> getAvailableVersions(String schemaName) {
        try {
            // 从version_history表查询历史版本 - 使用queryForList直接获取String列表
            String sql = "SELECT DISTINCT to_version FROM public.sys_schema_version_history " +
                        "WHERE schema_name = :schemaName AND delete_flag = 0 " +
                        "ORDER BY to_version DESC";

            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("schemaName", schemaName);

            List<String> versions = namedParameterJdbcTemplate.queryForList(sql, params, String.class);

            // 添加当前版本
            String currentVersion = flywayMultiTenantConfig.getCurrentVersion(schemaName);
            if (currentVersion != null && !versions.contains(currentVersion)) {
                versions.add(0, currentVersion);
            }

            log.info("查询到{}个可用版本: schema={}", versions.size(), schemaName);
            return versions;

        } catch (Exception e) {
            log.error("查询可用版本失败: schema={}", schemaName, e);
            return new ArrayList<>();
        }
    }

    /**
     * 更新Schema版本记录（回滚）
     */
    @Transactional(rollbackFor = Exception.class)
    protected void updateSchemaVersionForRollback(String schemaName, String fromVersion, String toVersion) {
        try {
            String updateSql = "UPDATE public.sys_schema_version " +
                              "SET current_version = :toVersion, " +
                              "    flyway_version = :toVersion, " +
                              "    updated_at = :now " +
                              "WHERE schema_name = :schemaName";

            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("toVersion", toVersion);
            params.addValue("schemaName", schemaName);
            params.addValue("now", LocalDateTime.now());

            int updated = namedParameterJdbcTemplate.update(updateSql, params);
            log.info("更新Schema版本记录（回滚）: schema={}, from={}, to={}, updated={}",
                    schemaName, fromVersion, toVersion, updated);

        } catch (Exception e) {
            log.error("更新Schema版本记录失败: schema={}", schemaName, e);
            throw new BusinessException("更新版本记录失败: " + e.getMessage());
        }
    }

    /**
     * 记录回滚历史
     */
    @Transactional(rollbackFor = Exception.class)
    protected void recordRollbackHistory(String schemaName, String fromVersion, String toVersion) {
        try {
            String historySql = "INSERT INTO public.sys_schema_version_history " +
                               "(id, schema_name, from_version, to_version, migrations_executed, " +
                               " migrated_at, migrated_by, rollback_flag, created_at, delete_flag) " +
                               "VALUES (nextval('public.sys_schema_version_history_id_seq'), " +
                               " :schemaName, :fromVersion, :toVersion, 0, " +
                               " :now, :userId, 1, :now, 0)";

            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("schemaName", schemaName);
            params.addValue("fromVersion", fromVersion);
            params.addValue("toVersion", toVersion);
            params.addValue("now", LocalDateTime.now());
            params.addValue("userId", 0L); // 系统自动

            namedParameterJdbcTemplate.update(historySql, params);
            log.info("记录回滚历史: schema={}, from={}, to={}", schemaName, fromVersion, toVersion);

        } catch (Exception e) {
            log.error("记录回滚历史失败: schema={}", schemaName, e);
            // 不抛出异常，允许回滚继续
        }
    }
}
