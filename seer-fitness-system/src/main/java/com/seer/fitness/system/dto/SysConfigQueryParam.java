package com.seer.fitness.system.dto;

import lombok.Data;

@Data
public class SysConfigQueryParam {
    private String configKey;
    private String configName;
    private Integer configType;
}
