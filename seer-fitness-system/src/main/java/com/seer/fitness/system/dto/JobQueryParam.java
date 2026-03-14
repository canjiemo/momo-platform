package com.seer.fitness.system.dto;

import io.github.canjiemo.mycommon.pager.PagerParam;
import lombok.Data;

@Data
public class JobQueryParam extends PagerParam {
    private String jobName;
    private String jobGroup;
    private String handlerName;
    private Integer status;
}
