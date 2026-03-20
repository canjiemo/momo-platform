package io.github.canjiemo.momo.file.storage.model;

import lombok.Data;

@Data
public class FileUploadResult {
    private String fileKey;
    private String accessUrl;
    private String originalName;
    private long fileSize;
    private String contentType;
}
