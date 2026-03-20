package io.github.canjiemo.momo.ai.config;

import io.github.canjiemo.momo.framework.dto.UserCacheInfo;
import io.github.canjiemo.momo.framework.utils.AsyncTenantHolder;
import io.github.canjiemo.momo.framework.utils.SecurityContextUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * AI 异步任务线程池配置。
 *
 * TaskDecorator 在 HTTP 线程提交任务时，快照捕获当前用户的 tenantId，
 * 在异步线程启动前写入 AsyncTenantHolder，结束后统一清理。
 * 这样 TenantIdProvider 在异步线程中能正常获取租户信息，无需业务代码手动管理。
 */
@EnableAsync
@Configuration
public class AiAsyncConfig {

    @Bean("aiQueryExecutor")
    public Executor aiQueryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-query-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setTaskDecorator(runnable -> {
            // 在 HTTP 线程（任务提交时）捕获快照，避免 Undertow 回收 Request 后丢失
            UserCacheInfo user = SecurityContextUtil.getCurrentUser();
            Long tenantId = user != null ? user.getTenantId() : null;
            return () -> {
                try {
                    AsyncTenantHolder.set(tenantId);
                    runnable.run();
                } finally {
                    AsyncTenantHolder.clear();
                }
            };
        });
        executor.initialize();
        return executor;
    }
}
