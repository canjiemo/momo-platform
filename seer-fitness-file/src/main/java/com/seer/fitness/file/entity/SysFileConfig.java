package com.seer.fitness.file.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@MyTable("sys_file_config")
public class SysFileConfig implements MyTableEntity {
    private Long id;
    private String configName;
    private String storageType;
    private Integer isActive;
    private String config;        // JSONB 列，Java 侧用 String 接收
    private String remark;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private Integer deleteFlag;
}
