package io.github.canjiemo.momo.file.storage;

import io.github.canjiemo.momo.file.storage.model.FileUploadResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储适配器接口。
 * 新增存储类型时实现此接口并注册为 Spring Bean 即可，无需修改现有代码。
 */
public interface IFileStorageAdapter {

    /** 适配器类型标识，对应 sys_file_config.storage_type */
    String getType();

    /**
     * 用 sys_file_config.config（JSONB）初始化适配器。
     * 每次切换激活配置后调用。
     */
    void init(String configJson);

    /**
     * 上传文件
     * @param file      上传的文件
     * @param directory 存储目录（如 "avatar"、"document"），不含前缀斜杠
     */
    FileUploadResult upload(MultipartFile file, String directory) throws Exception;

    /** 删除文件 */
    void delete(String fileKey) throws Exception;

    /** 获取文件访问 URL（本地拼接，MinIO 按 publicBucket 决定是否预签名） */
    String getAccessUrl(String fileKey);
}
