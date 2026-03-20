package io.github.canjiemo.momo.file.storage.model;

import lombok.Data;

@Data
public class MinioStorageConfig {
    private String endpoint;
    private String bucket;
    private String accessKey;
    private String secretKey;
    private boolean publicBucket = true;
    private int presignedExpireSeconds = 3600;
}
