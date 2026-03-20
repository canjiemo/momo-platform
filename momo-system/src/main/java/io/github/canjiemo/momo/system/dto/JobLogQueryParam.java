package io.github.canjiemo.momo.system.dto;

import io.github.canjiemo.mycommon.pager.PagerParam;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobLogQueryParam extends PagerParam {
    private Long jobId;
    private Integer triggerType;
    private Integer status;
    private LocalDateTime startTimeBegin;
    private LocalDateTime startTimeEnd;
}
