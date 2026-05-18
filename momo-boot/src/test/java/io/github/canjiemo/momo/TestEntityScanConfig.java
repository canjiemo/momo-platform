package io.github.canjiemo.momo;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

/**
 * 测试上下文专用 —— 把 {@code io.github.canjiemo.momo}（本配置类所在包）注册到
 * Spring Boot 的 AutoConfigurationPackages。
 * <p>
 * 背景：myjdbc 启动时通过 {@code sun.java.command} 找主类，在生产/dev 启动中
 * 主类是 MomoApplication（@SpringBootApplication(scanBasePackages = "io.github.canjiemo.momo")），
 * 因此能扫到所有 @MyTable 实体。但 Surefire fork 的 JVM 主类是 ForkedBooter，
 * myjdbc 找不到主类，fallback 用 AutoConfigurationPackages.get(beanFactory)，
 * 而默认值是 MomoApplication 所在的 {@code io.github.canjiemo.momo.boot}，
 * 看不到 entity 包，于是 lambdaQuery / insertPO 都报"未找到表名"或 [MYJDBC-1001]。
 * <p>
 * 让本配置类位于父包 {@code io.github.canjiemo.momo}，并标 {@link AutoConfigurationPackage}，
 * 会把该父包注册进 AutoConfigurationPackages —— myjdbc 的 fallback 路径就能扫到 entity 子包。
 * 通过 AbstractIntegrationTest 上的 {@code @Import} 引入。
 */
@Configuration
@AutoConfigurationPackage
public class TestEntityScanConfig {
}
