package io.github.canjiemo.momo.system.dto;

import io.github.canjiemo.tools.dict.MyDict;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobLogDTO {
    private Long id;
    private Long jobId;
    private String jobName;
    private String handlerName;
    @MyDict(type = "job_trigger_type")
    private Integer triggerType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    @MyDict(type = "job_exec_status")
    private Integer status;
    private String errorMsg;
    private Long operatorId;
}
