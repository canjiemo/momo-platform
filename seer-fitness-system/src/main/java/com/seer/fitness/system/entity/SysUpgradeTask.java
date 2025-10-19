package com.seer.fitness.system.entity;

import io.github.mocanjie.base.myjpa.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Schema升级任务实体类
 * 对应数据库表 sys_schema_upgrade_task
 * 用于记录租户Schema批量升级任务的执行状态
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@Data
@MyTable("sys_schema_upgrade_task")
public class SysUpgradeTask {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 目标版本号
     */
    private String targetVersion;

    /**
     * 起始版本号
     */
    private String fromVersion;

    /**
     * 升级类型：SINGLE-单个 BATCH-批量 ALL-全部
     */
    private String upgradeType;

    /**
     * 目标Schema列表（JSON数组格式）
     */
    private String targetSchemas;

    /**
     * 总Schema数量
     */
    private Integer totalSchemas;

    /**
     * 成功数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failedCount;

    /**
     * 任务状态：PENDING-待执行 RUNNING-执行中 COMPLETED-已完成 FAILED-失败 CANCELLED-已取消
     */
    private String status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 完成时间
     */
    private LocalDateTime endTime;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除：0-正常 1-已删除
     */
    private Integer deleteFlag;
}
