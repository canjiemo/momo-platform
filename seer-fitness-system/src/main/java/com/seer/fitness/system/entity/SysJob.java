package com.seer.fitness.system.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MyTable("sys_job")
public class SysJob implements MyTableEntity {
    private Long id;
    private String jobName;
    private String jobGroup;
    private String handlerName;
    private String cronExpression;
    private String jobParams;
    /** 0=停用 1=启用 */
    private Integer status;
    private String remark;
    private Integer deleteFlag;
    private Long createdBy;
    private LocalDateTime createTime;
    private Long updatedBy;
    private LocalDateTime updateTime;
}
