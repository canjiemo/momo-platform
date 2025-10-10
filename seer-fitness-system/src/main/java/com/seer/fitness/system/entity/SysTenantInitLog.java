package com.seer.fitness.system.entity;

import io.github.mocanjie.base.myjpa.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户初始化日志实体类
 * 对应数据库表 public.sys_tenant_init_log
 * <p>
 * 记录租户 Schema 创建和初始化过程的每个步骤
 * 用于追踪初始化进度、调试失败原因
 *
 * @author seer-fitness
 */
@Data
@MyTable("public.sys_tenant_init_log")
public class SysTenantInitLog {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID
     * 关联 sys_tenant.id
     */
    private Long tenantId;

    /**
     * 初始化步骤名称
     * 例如：创建Schema、初始化表结构、创建管理员
     */
    private String stepName;

    /**
     * 步骤类型
     * CREATE_SCHEMA, CREATE_TABLE, INSERT_DATA, CREATE_ADMIN, ROLLBACK
     * @see com.seer.fitness.system.enums.InitStepType
     */
    private String stepType;

    /**
     * 状态：0-进行中 1-成功 2-失败
     */
    private Integer status;

    /**
     * 执行的 SQL 脚本（可选，用于调试）
     */
    private String sqlScript;

    /**
     * 错误信息（失败时记录）
     */
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    private Integer executionTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
