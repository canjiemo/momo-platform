package io.github.canjiemo.momo.system.dto;

import io.github.canjiemo.tools.dict.MyDict;
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
    @MyDict(type = "common_status")
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
