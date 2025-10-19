package com.seer.fitness.system.entity;

import io.github.mocanjie.base.myjpa.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Schema升级任务日志实体类
 * 对应数据库表 sys_schema_upgrade_detail
 * 用于记录每个Schema的升级执行详情
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Data
@MyTable("sys_schema_upgrade_detail")
public class SysUpgradeTaskLog {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 升级任务ID
     */
    private Long taskId;

    /**
     * Schema名称
     */
    private String schemaName;

    /**
     * 升级前版本
     */
    private String fromVersion;

    /**
     * 升级后版本
     */
    private String toVersion;

    /**
     * 执行的迁移数量
     */
    private Integer migrationsExecuted;

    /**
     * 执行状态：PENDING-待执行 RUNNING-执行中 SUCCESS-成功 FAILED-失败 ROLLED_BACK-已回滚
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 完成时间
     */
    private LocalDateTime endTime;

    /**
     * 执行耗时（毫秒）
     */
    private Integer executionTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 逻辑删除：0-正常 1-已删除
     */
    private Integer deleteFlag;
}
