package io.github.canjiemo.momo.boot.test;

import io.github.canjiemo.momo.TestEntityScanConfig;
import io.github.canjiemo.momo.boot.MomoApplication;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 集成测试基类。
 * <ul>
 *   <li>启动完整 Spring 上下文（连接本地 momo-platform-test 库）</li>
 *   <li>每个测试方法在事务中执行，结束自动回滚 → 测试间数据完全隔离</li>
 *   <li>每个测试方法开始前清空 Redis db=15，避免缓存污染</li>
 * </ul>
 * <p>
 * 注意：被测代码内部的 {@code @Transactional} 也会加入同一外层事务，
 * 因此不会真正 commit；这对验证 SQL 语义足够，但有一类场景例外 ——
 * 跨事务的可见性（如缓存预热依赖独立连接），需要在测试中显式 flush，
 * 必要时改用 {@code @Transactional(propagation = NOT_SUPPORTED)} 覆盖。
 * <p>
 * <b>关于 TableCacheManager 显式初始化：</b>
 * myjdbc 在启动时通过 {@code sun.java.command} 寻找 {@code @SpringBootApplication} 主类，
 * 进而读取其 {@code scanBasePackages} 作为实体扫描根。但 Surefire fork 的 JVM 主类是
 * {@code ForkedBooter}，导致 myjdbc 找不到 momo 主类、@MyTable 实体注册不上。
 * 在 static 块中显式调用 {@code initCache} 提前注册，确保 Spring 上下文加载 Bean 之前
 * 实体元数据已就位。
 */
@SpringBootTest(classes = MomoApplication.class)
@ActiveProfiles("test")
@Transactional
@Import(TestEntityScanConfig.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void cleanRedis() {
        try (RedisConnection conn = redisTemplate.getConnectionFactory().getConnection()) {
            conn.serverCommands().flushDb();
        }
    }
}
