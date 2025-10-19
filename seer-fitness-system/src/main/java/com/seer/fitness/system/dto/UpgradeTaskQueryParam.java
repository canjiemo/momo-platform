package com.seer.fitness.system.dto;

import io.github.mocanjie.base.mycommon.pager.PagerParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 升级任务查询参数
 *
 * @author seer-fitness
 * @since 2025-10-18
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UpgradeTaskQueryParam extends PagerParam {

    /**
     * 任务名称（模糊查询）
     */
    private String taskName;

    /**
     * 升级类型：SINGLE/BATCH/ALL
     */
    private String upgradeType;

    /**
     * 任务状态：PENDING/RUNNING/COMPLETED/FAILED/CANCELLED
     */
    private String status;
}
