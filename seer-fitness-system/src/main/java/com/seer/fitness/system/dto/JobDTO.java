package com.seer.fitness.system.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class JobDTO {
    private Long id;
    private String jobName;
    private String jobGroup;
    private String handlerName;
    private String cronExpression;
    private String jobParams;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
