package com.seer.fitness.system.entity;

import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MyTable("sys_config")
public class SysConfig {
    private Long id;
    private String configKey;
    private String configValue;
    private String configName;
    private Integer configType;
    private String remark;
    private Long tenantId;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private Integer deleteFlag;
}
