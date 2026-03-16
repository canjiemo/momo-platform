package com.seer.fitness.file.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysFileConfigDTO {
    private Long id;
    private String configName;
    private String storageType;
    private Integer isActive;
    private String config;       // 敏感字段由 Service 层掩码处理后传入
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
