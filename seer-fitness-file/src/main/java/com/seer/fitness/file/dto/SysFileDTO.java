package com.seer.fitness.file.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysFileDTO {
    private Long id;
    private String fileName;
    private String fileKey;
    private String fileUrl;
    private Long fileSize;
    private String contentType;
    private String storageType;
    private String bizType;
    private String bizId;
    private LocalDateTime createTime;
}
