package com.seer.fitness.system.tenant;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态租户数据源管理器
 * 负责管理每个租户的独立数据库连接池
 * <p>
 * 核心功能：
 * - 懒加载：首次访问租户时才创建连接池
 * - 连接池隔离：每个租户使用独立的Druid连接池
 * - 动态Schema：每个连接池指向不同的PostgreSQL Schema
 * - 线程安全：使用ConcurrentHashMap + double-check
 * - 资源管理：支持移除租户并关闭连接池
 * - 健康检查：定期检查连接池状态，自动清理失效数据源
 * <p>
 * 技术方案：
 * - 使用 currentSchema 参数指定默认Schema
 * - 每个租户的连接池配置独立（initial-size, max-active等）
 * - 支持热加载新租户，无需重启应用
 * <p>
 * 注意事项：
 * - 连接池数量 = 租户数量，需要控制租户规模（建议 <= 100）
 * - 每个连接池占用一定内存（约10-20MB）
 * - 数据库max_connections需要足够大（建议 >= 600）
 * <p>
 * 安全加固（2024-10-18）：
 * - 添加租户数量限制，防止资源耗尽
 * - 添加连接池健康检查
 * - 改进异常处理和日志记录
 *
 * @author seer-fitness
 * @since 2024-10-18 安全加固
 */
@Component
@Slf4j
public class DynamicTenantDataSourceManager {

    /**
     * 最大租户数量限制
     * 防止无限创建连接池导致资源耗尽
     */
    private static final int MAX_TENANT_COUNT = 100;

    /**
     * 租户数据源缓存
     * Key: schemaName (租户Schema名称)
     * Value: DruidDataSource (租户专属连接池)
     */
    private final Map<String, DruidDataSource> tenantDataSources = new ConcurrentHashMap<>();

    // ==================== 数据库配置（从配置文件注入）====================

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    // ==================== Druid连接池配置 ====================

    @Value("${spring.datasource.druid.initial-size:5}")
    private int initialSize;

    @Value("${spring.datasource.druid.min-idle:5}")
    private int minIdle;

    @Value("${spring.datasource.druid.max-active:20}")
    private int maxActive;

    @Value("${spring.datasource.druid.max-wait:60000}")
    private long maxWait;

    @Value("${spring.datasource.druid.time-between-eviction-runs-millis:60000}")
    private long timeBetweenEvictionRunsMillis;

    @Value("${spring.datasource.druid.min-evictable-idle-time-millis:300000}")
    private long minEvictableIdleTimeMillis;

    @Value("${spring.datasource.druid.validation-query:SELECT 1}")
    private String validationQuery;

    @Value("${spring.datasource.druid.test-while-idle:true}")
    private boolean testWhileIdle;

    @Value("${spring.datasource.druid.test-on-borrow:false}")
    private boolean testOnBorrow;

    @Value("${spring.datasource.druid.test-on-return:false}")
    private boolean testOnReturn;

    /**
     * 获取租户数据源（懒加载）
     * 首次访问时自动创建，后续直接从缓存获取
     * 修复并发安全问题 - 2024-10-18
     *
     * @param schemaName 租户Schema名称
     * @return 租户专属的数据源
     * @throws IllegalArgumentException 当schema名称无效时
     * @throws RuntimeException 当租户数量超限或创建失败时
     */
    public DataSource getDataSource(String schemaName) {
        // 1. 参数验证
        if (schemaName == null || schemaName.isEmpty()) {
            throw new IllegalArgumentException("Schema名称不能为空");
        }

        // 2. 安全验证：schema名称格式
        if (!isValidSchemaName(schemaName)) {
            log.error("非法的schema名称: {}", schemaName);
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }

        // 3. 先从缓存获取
        DruidDataSource dataSource = tenantDataSources.get(schemaName);
        if (dataSource != null) {
            // 验证数据源健康状态
            if (isDataSourceHealthy(dataSource)) {
                return dataSource;
            } else {
                log.warn("数据源不健康，将重新创建: schemaName={}", schemaName);
                removeTenant(schemaName);
                // 继续下面的创建流程
            }
        }

        // 4. Double-check锁定，防止重复创建
        synchronized (this) {
            // 再次检查（可能其他线程已创建）
            dataSource = tenantDataSources.get(schemaName);
            if (dataSource != null && isDataSourceHealthy(dataSource)) {
                return dataSource;
            }

            // 5. 检查租户数量限制
            if (tenantDataSources.size() >= MAX_TENANT_COUNT) {
                log.error("租户数量已达上限: current={}, max={}",
                        tenantDataSources.size(), MAX_TENANT_COUNT);
                throw new RuntimeException("Maximum tenant count exceeded: " + MAX_TENANT_COUNT);
            }

            // 6. 创建新的租户数据源
            log.info("首次访问租户，创建数据源: schemaName={}", schemaName);
            try {
                dataSource = createTenantDataSource(schemaName);
                tenantDataSources.put(schemaName, dataSource);

                log.info("租户数据源创建成功: schemaName={}, 当前租户数={}/{}",
                        schemaName, tenantDataSources.size(), MAX_TENANT_COUNT);
                return dataSource;
            } catch (Exception e) {
                log.error("创建租户数据源失败: schemaName={}", schemaName, e);
                // 确保失败时清理
                tenantDataSources.remove(schemaName);
                throw new RuntimeException("Failed to create datasource for tenant: " + schemaName, e);
            }
        }
    }

    /**
     * 验证schema名称格式
     * 防止SQL注入和非法字符
     *
     * @param schemaName schema名称
     * @return 是否有效
     */
    private boolean isValidSchemaName(String schemaName) {
        // PostgreSQL schema命名规范
        return schemaName.matches("^[a-z][a-z0-9_]{0,62}$");
    }

    /**
     * 检查数据源健康状态
     *
     * @param dataSource 数据源
     * @return 是否健康
     */
    private boolean isDataSourceHealthy(DruidDataSource dataSource) {
        if (dataSource == null) {
            return false;
        }

        // 检查是否已关闭
        if (dataSource.isClosed()) {
            return false;
        }

        // 检查是否启用
        if (!dataSource.isEnable()) {
            return false;
        }

        // 尝试获取连接验证
        try (Connection conn = dataSource.getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            log.warn("数据源健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建租户专属的Druid数据源
     *
     * @param schemaName 租户Schema名称
     * @return DruidDataSource
     */
    private DruidDataSource createTenantDataSource(String schemaName) {
        DruidDataSource dataSource = new DruidDataSource();

        // 基础配置
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 设置JDBC URL，指定currentSchema参数
        String tenantJdbcUrl = buildTenantJdbcUrl(schemaName);
        dataSource.setUrl(tenantJdbcUrl);

        // 连接池配置
        dataSource.setInitialSize(initialSize);
        dataSource.setMinIdle(minIdle);
        dataSource.setMaxActive(maxActive);
        dataSource.setMaxWait(maxWait);

        // 检测配置
        dataSource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        dataSource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        dataSource.setValidationQuery(validationQuery);
        dataSource.setTestWhileIdle(testWhileIdle);
        dataSource.setTestOnBorrow(testOnBorrow);
        dataSource.setTestOnReturn(testOnReturn);

        // 其他配置
        dataSource.setPoolPreparedStatements(true);
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(20);

        // 初始化连接池
        try {
            dataSource.init();
            log.info("Druid连接池初始化成功: schemaName={}, url={}", schemaName, tenantJdbcUrl);
        } catch (SQLException e) {
            log.error("Druid连接池初始化失败: schemaName={}", schemaName, e);
            throw new RuntimeException("创建租户数据源失败：" + schemaName, e);
        }

        return dataSource;
    }

    /**
     * 构建租户专属的JDBC URL
     * 在原URL基础上添加或替换currentSchema参数
     *
     * @param schemaName 租户Schema名称
     * @return 租户专属的JDBC URL
     */
    private String buildTenantJdbcUrl(String schemaName) {
        // 移除原URL中的currentSchema参数（如果有）
        String baseUrl = jdbcUrl.replaceAll("[&?]currentSchema=[^&]*", "");

        // 添加新的currentSchema参数
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "currentSchema=" + schemaName + "&stringtype=unspecified";
    }

    /**
     * 移除租户数据源并关闭连接池
     * 用于租户删除或下线场景
     *
     * @param schemaName 租户Schema名称
     */
    public void removeTenant(String schemaName) {
        DruidDataSource dataSource = tenantDataSources.remove(schemaName);
        if (dataSource != null) {
            try {
                dataSource.close();
                log.info("租户数据源已关闭: schemaName={}, 剩余租户数={}",
                        schemaName, tenantDataSources.size());
            } catch (Exception e) {
                log.error("关闭租户数据源失败: schemaName={}", schemaName, e);
            }
        } else {
            log.warn("租户数据源不存在，无需关闭: schemaName={}", schemaName);
        }
    }

    /**
     * 获取当前已加载的租户数量
     */
    public int getTenantCount() {
        return tenantDataSources.size();
    }

    /**
     * 获取所有已加载的租户Schema名称
     */
    public String[] getLoadedTenants() {
        return tenantDataSources.keySet().toArray(new String[0]);
    }

    /**
     * 预热指定租户的连接池
     * 提前创建连接，避免首次访问时的延迟
     *
     * @param schemaName 租户Schema名称
     */
    public void warmUp(String schemaName) {
        log.info("预热租户连接池: schemaName={}", schemaName);
        getDataSource(schemaName);
    }

    /**
     * 定期健康检查
     * 每5分钟检查一次所有租户数据源的健康状态
     * 自动清理不健康的数据源
     * 修复并发安全问题 - 2024-10-18
     */
    @Scheduled(fixedDelay = 300000) // 5分钟
    public void healthCheck() {
        if (tenantDataSources.isEmpty()) {
            return;
        }

        log.debug("开始租户数据源健康检查，当前租户数: {}", tenantDataSources.size());
        int unhealthyCount = 0;

        // 遍历所有数据源进行健康检查
        for (Map.Entry<String, DruidDataSource> entry : tenantDataSources.entrySet()) {
            String schemaName = entry.getKey();
            DruidDataSource dataSource = entry.getValue();

            if (!isDataSourceHealthy(dataSource)) {
                log.warn("发现不健康的数据源，准备清理: schemaName={}", schemaName);
                removeTenant(schemaName);
                unhealthyCount++;
            } else {
                // 记录健康数据源的统计信息
                log.trace("数据源健康: schemaName={}, activeCount={}, poolingCount={}",
                        schemaName,
                        dataSource.getActiveCount(),
                        dataSource.getPoolingCount());
            }
        }

        if (unhealthyCount > 0) {
            log.warn("健康检查完成，清理了{}个不健康的数据源，剩余租户数: {}",
                    unhealthyCount, tenantDataSources.size());
        } else {
            log.debug("健康检查完成，所有数据源正常，租户数: {}", tenantDataSources.size());
        }
    }

    /**
     * 获取数据源统计信息
     * 用于监控和调试
     *
     * @return 统计信息字符串
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("租户数据源统计:\n");
        stats.append(String.format("总租户数: %d/%d\n", tenantDataSources.size(), MAX_TENANT_COUNT));

        tenantDataSources.forEach((schemaName, dataSource) -> {
            stats.append(String.format("  - %s: active=%d, pooling=%d, total=%d, healthy=%s\n",
                    schemaName,
                    dataSource.getActiveCount(),
                    dataSource.getPoolingCount(),
                    dataSource.getActiveCount() + dataSource.getPoolingCount(),
                    isDataSourceHealthy(dataSource) ? "✓" : "✗"));
        });

        return stats.toString();
    }

    /**
     * 关闭所有租户数据源
     * 应用关闭时调用
     */
    public void closeAll() {
        log.info("开始关闭所有租户数据源，当前租户数: {}", tenantDataSources.size());
        tenantDataSources.forEach((schemaName, dataSource) -> {
            try {
                dataSource.close();
                log.info("租户数据源已关闭: {}", schemaName);
            } catch (Exception e) {
                log.error("关闭租户数据源失败: {}", schemaName, e);
            }
        });
        tenantDataSources.clear();
        log.info("所有租户数据源已关闭");
    }
}
