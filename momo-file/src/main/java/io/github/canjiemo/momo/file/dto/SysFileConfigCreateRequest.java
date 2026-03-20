package io.github.canjiemo.momo.file.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SysFileConfigCreateRequest {
    @NotBlank(message = "配置名称不能为空")
    private String configName;
    @NotBlank(message = "存储类型不能为空")
    private String storageType;
    @NotBlank(message = "配置内容不能为空")
    private String config;
    private String remark;
}
