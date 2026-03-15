package com.seer.fitness.file.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@MyTable("sys_file")
public class SysFile implements MyTableEntity {
    private Long id;
    private String fileName;
    private String fileKey;
    private String fileUrl;
    private Long fileSize;
    private String contentType;
    private String storageType;
    private String bizType;
    private String bizId;
    private Long tenantId;
    private String createBy;
    private LocalDateTime createTime;
    private Integer deleteFlag;
}
