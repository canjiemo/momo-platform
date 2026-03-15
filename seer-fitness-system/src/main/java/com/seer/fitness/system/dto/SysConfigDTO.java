package com.seer.fitness.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysConfigDTO {
    private Long id;
    private String configKey;
    private String configValue;
    private String configName;
    private Integer configType;
    private String remark;
    private String updateBy;
    private LocalDateTime updateTime;
}
