package com.seer.fitness.system.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MyTable("sys_job_log")
public class SysJobLog implements MyTableEntity {
    private Long id;
    private Long jobId;
    private String jobName;
    private String handlerName;
    /** 0=定时触发 1=手动触发 */
    private Integer triggerType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    /** 0=失败 1=成功 */
    private Integer status;
    private String errorMsg;
    private Long operatorId;
}
