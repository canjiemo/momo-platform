package io.github.canjiemo.momo.file.storage.adapter;

import com.alibaba.fastjson2.JSON;
import io.github.canjiemo.momo.file.storage.IFileStorageAdapter;
import io.github.canjiemo.momo.file.storage.model.FileUploadResult;
import io.github.canjiemo.momo.file.storage.model.MinioStorageConfig;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Component("minio")
public class MinioStorageAdapter implements IFileStorageAdapter {

    private MinioStorageConfig config;
    private MinioClient minioClient;

    @Override
    public String getType() { return "minio"; }

    @Override
    public void init(String configJson) {
        this.config = JSON.parseObject(configJson, MinioStorageConfig.class);
        this.minioClient = MinioClient.builder()
                .endpoint(config.getEndpoint())
                .credentials(config.getAccessKey(), config.getSecretKey())
                .build();
        ensureBucketExists();
        log.info("MinIO 存储初始化: endpoint={}, bucket={}", config.getEndpoint(), config.getBucket());
    }

    private void ensureBucketExists() {
        String bucket = config.getBucket();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket 已自动创建: {}", bucket);
            }
        } catch (Exception e) {
            log.error("MinIO bucket 检查/创建失败: bucket={}", bucket, e);
            throw new BusinessException("MinIO bucket 初始化失败: " + bucket);
        }
    }

    @Override
    public FileUploadResult upload(MultipartFile file, String directory) throws Exception {
        String originalName = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(originalName);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileKey = directory + "/" + uuid + (ext != null ? "." + ext : "");

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(config.getBucket())
                .object(fileKey)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());

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
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(config.getBucket())
                .object(fileKey)
                .build());
        log.info("MinIO 文件已删除: bucket={}, key={}", config.getBucket(), fileKey);
    }

    @Override
    public String getAccessUrl(String fileKey) {
        if (config.isPublicBucket()) {
            return config.getEndpoint() + "/" + config.getBucket() + "/" + fileKey;
        }
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(config.getBucket())
                    .object(fileKey)
                    .expiry(config.getPresignedExpireSeconds())
                    .build());
        } catch (Exception e) {
            log.error("获取 MinIO 预签名 URL 失败: {}", fileKey, e);
            return "";
        }
    }
}
