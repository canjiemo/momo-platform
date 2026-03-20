package io.github.canjiemo.momo.file.storage.model;

import lombok.Data;

@Data
public class LocalStorageConfig {
    private String basePath;
    private String urlPrefix;
}
