package com.seer.fitness.system.dto;

import io.github.canjiemo.mycommon.pager.PagerParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysConfigQueryParam extends PagerParam {
    private String configKey;
    private String configName;
    private Integer configType;
}
