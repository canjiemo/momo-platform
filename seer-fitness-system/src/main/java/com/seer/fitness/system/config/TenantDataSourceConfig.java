package com.seer.fitness.system.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.seer.fitness.system.tenant.DynamicTenantDataSource;
import com.seer.fitness.system.tenant.DynamicTenantDataSourceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * 租户数据源配置
 * 配置动态路由数据源，支持多租户隔离
 * <p>
 * 核心配置：
 * 1. 默认数据源：指向public schema，用于平台管理
 * 2. 动态路由数据源：根据TenantContext自动切换租户数据源
 * 3. 懒加载机制：首次访问租户时才创建连接池
 * <p>
 * 启用条件：
 * - 配置项 tenant.multi-tenant.enabled=true 时启用
 * - 未配置或为false时使用默认的单数据源
 * <p>
 * 工作流程：
 * 1. 应用启动时创建默认数据源（public schema）
 * 2. 请求到达时，拦截器设置TenantContext
 * 3. DynamicTenantDataSource根据上下文路由到对应数据源
 * 4. 如果是首次访问，DynamicTenantDataSourceManager创建新连接池
 * 5. 后续访问直接使用已创建的连接池
 *
 * @author seer-fitness
 */
@Configuration
@ConditionalOnProperty(name = "tenant.multi-tenant.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class TenantDataSourceConfig {

    @Autowired
    private DynamicTenantDataSourceManager dataSourceManager;

    // ==================== 默认数据源配置（public schema）====================

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.druid.initial-size:5}")
    private int initialSize;

    @Value("${spring.datasource.druid.min-idle:5}")
    private int minIdle;

    @Value("${spring.datasource.druid.max-active:20}")
    private int maxActive;

    @Value("${spring.datasource.druid.max-wait:60000}")
    private long maxWait;

    /**
     * 创建默认数据源（public schema）
     * 用于平台管理功能：租户管理、系统配置等
     */
    @Bean(name = "defaultDataSource")
    public DataSource defaultDataSource() throws SQLException {
        log.info("初始化默认数据源（public schema）");

        DruidDataSource dataSource = new DruidDataSource();

        // 基础配置
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 设置URL为public schema
        String publicUrl = buildPublicSchemaUrl();
        dataSource.setUrl(publicUrl);

        // 连接池配置
        dataSource.setInitialSize(initialSize);
        dataSource.setMinIdle(minIdle);
        dataSource.setMaxActive(maxActive);
        dataSource.setMaxWait(maxWait);

        // 检测配置
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(300000);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);

        // 其他配置
        dataSource.setPoolPreparedStatements(true);
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(20);

        // 初始化
        dataSource.init();

        log.info("默认数据源初始化成功: url={}", publicUrl);
        return dataSource;
    }

    /**
     * 创建动态路由数据源（主数据源）
     * 替换Spring Boot默认的数据源
     *
     * @Primary 标记为主数据源，Spring会优先使用此数据源
     */
    @Bean(name = "dataSource")
    @Primary
    public DataSource dataSource() throws SQLException {
        log.info("初始化动态租户路由数据源");

        DynamicTenantDataSource dynamicDataSource = new DynamicTenantDataSource(dataSourceManager);

        // 设置默认数据源（public schema）
        dynamicDataSource.setDefaultTargetDataSource(defaultDataSource());

        // 设置目标数据源Map（初始为空，租户数据源会动态添加）
        // AbstractRoutingDataSource要求必须设置此属性，即使是空Map
        dynamicDataSource.setTargetDataSources(new java.util.HashMap<>());

        // 初始化
        dynamicDataSource.afterPropertiesSet();

        log.info("动态租户路由数据源初始化成功");
        return dynamicDataSource;
    }

    /**
     * 构建public schema的JDBC URL
     */
    private String buildPublicSchemaUrl() {
        // 移除原URL中的currentSchema参数（如果有）
        String baseUrl = jdbcUrl.replaceAll("[&?]currentSchema=[^&]*", "");

        // 添加currentSchema=public参数
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "currentSchema=public&stringtype=unspecified";
    }

    /**
     * 应用关闭时的清理工作
     */
    @Bean
    public TenantDataSourceShutdownHook tenantDataSourceShutdownHook() {
        return new TenantDataSourceShutdownHook(dataSourceManager);
    }

    /**
     * 数据源关闭钩子
     * 应用关闭时自动关闭所有租户数据源
     */
    private static class TenantDataSourceShutdownHook {
        private final DynamicTenantDataSourceManager dataSourceManager;

        public TenantDataSourceShutdownHook(DynamicTenantDataSourceManager dataSourceManager) {
            this.dataSourceManager = dataSourceManager;
            // 注册JVM关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("应用关闭，开始清理租户数据源");
                dataSourceManager.closeAll();
            }));
        }
    }
}
