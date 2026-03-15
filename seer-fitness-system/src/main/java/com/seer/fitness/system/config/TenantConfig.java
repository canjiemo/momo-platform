package com.seer.fitness.system.config;

import com.seer.fitness.framework.dto.UserCacheInfo;
import com.seer.fitness.framework.utils.SecurityContextUtil;
import io.github.canjiemo.base.myjdbc.audit.AuditFieldProvider;
import io.github.canjiemo.base.myjdbc.tenant.TenantIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 多租户 + 审计字段配置
 *
 * @author seer-fitness
 */
@Configuration
public class TenantConfig {

    /**
     * 租户ID提供者
     * - 平台超级管理员（adminFlag=1 且 tenantId=null）返回 null，可访问所有数据
     * - 租户管理员（adminFlag=1 且 tenantId≠null）返回自己的 tenantId
     * - 普通租户用户返回自己的 tenantId
     * - 未登录用户返回 null
     */
    @Bean
    public TenantIdProvider tenantIdProvider() {
        return () -> {
            UserCacheInfo user = SecurityContextUtil.getCurrentUser();
            if (user == null) return null;
            // 只有平台超管（tenant_id=NULL 且 admin_flag=1）才绕过租户过滤
            if (user.getAdminFlag() != null && user.getAdminFlag() == 1 && user.getTenantId() == null) return null;
            return user.getTenantId();
        };
    }

    /**
     * 审计字段操作人提供者
     * myjdbc 在 INSERT/UPDATE 前调用，自动填充 createBy/updateBy 字段。
     * 返回 null 时跳过操作人填充（匿名/系统操作）。
     */
    @Bean
    public AuditFieldProvider auditFieldProvider() {
        return () -> {
            UserCacheInfo user = SecurityContextUtil.getCurrentUser();
            return user != null ? user.getUserId() : null;
        };
    }
}
