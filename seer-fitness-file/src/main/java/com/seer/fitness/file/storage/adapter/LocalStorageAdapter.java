package com.seer.fitness.file.storage.adapter;

import com.alibaba.fastjson2.JSON;
import com.seer.fitness.file.storage.IFileStorageAdapter;
import com.seer.fitness.file.storage.model.FileUploadResult;
import com.seer.fitness.file.storage.model.LocalStorageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Component("local")
public class LocalStorageAdapter implements IFileStorageAdapter {

    private LocalStorageConfig config;

    @Override
    public String getType() { return "local"; }

    @Override
    public void init(String configJson) {
        this.config = JSON.parseObject(configJson, LocalStorageConfig.class);
        log.info("本地存储初始化: basePath={}", config.getBasePath());
    }

    @Override
    public FileUploadResult upload(MultipartFile file, String directory) throws Exception {
        String originalName = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(originalName);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileKey = directory + "/" + uuid + (ext != null ? "." + ext : "");

        Path target = Paths.get(config.getBasePath(), fileKey);
        Files.createDirectories(target.getParent());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        FileUploadResult result = new FileUploadResult();
        result.setFileKey(fileKey);
        result.setAccessUrl(getAccessUrl(fileKey));
        result.setOriginalName(originalName);
        result.setFileSize(file.getSize());
        result.setContentType(file.getContentType());
        return result;
    }

    @Override
    public void delete(String fileKey) throws Exception {
        Path path = Paths.get(config.getBasePath(), fileKey);
        Files.deleteIfExists(path);
        log.info("本地文件已删除: {}", fileKey);
    }

    @Override
    public String getAccessUrl(String fileKey) {
        return config.getUrlPrefix() + "/" + fileKey;
    }

    /** 供 FileController 本地文件服务时使用 */
    public Path resolvePath(String fileKey) {
        return Paths.get(config.getBasePath(), fileKey);
    }
}
