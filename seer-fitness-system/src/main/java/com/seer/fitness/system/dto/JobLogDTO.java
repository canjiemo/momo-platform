package com.seer.fitness.system.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class JobLogDTO {
    private Long id;
    private Long jobId;
    private String jobName;
    private String handlerName;
    private Integer triggerType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private Integer status;
    private String errorMsg;
    private Long operatorId;
}
