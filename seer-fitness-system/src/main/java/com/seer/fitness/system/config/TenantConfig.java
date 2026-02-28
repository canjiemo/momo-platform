package com.seer.fitness.system.config;

import com.seer.fitness.system.dto.UserCacheInfo;
import com.seer.fitness.system.util.SecurityContextUtil;
import io.github.mocanjie.base.myjpa.tenant.TenantIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 多租户配置
 * 提供 TenantIdProvider Bean，myjpa 自动注入 tenant_id 查询条件
 *
 * @author seer-fitness
 */
@Configuration
public class TenantConfig {

    /**
     * 租户ID提供者
     * - 超级管理员（adminFlag=1）返回 null，可访问所有租户数据
     * - 租户用户返回自己的 tenantId
     * - 未登录用户返回 null
     */
    @Bean
    public TenantIdProvider tenantIdProvider() {
        return () -> {
            UserCacheInfo user = SecurityContextUtil.getCurrentUser();
            if (user == null) return null;
            if (user.getAdminFlag() != null && user.getAdminFlag() == 1) return null;
            return user.getTenantId();
        };
    }
}
