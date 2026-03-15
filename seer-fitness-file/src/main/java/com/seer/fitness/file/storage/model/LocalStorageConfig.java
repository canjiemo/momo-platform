package com.seer.fitness.file.storage.model;

import lombok.Data;

@Data
public class LocalStorageConfig {
    private String basePath;
    private String urlPrefix;
}
