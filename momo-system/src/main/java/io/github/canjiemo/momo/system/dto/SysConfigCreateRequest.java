package io.github.canjiemo.momo.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SysConfigCreateRequest {
    @NotBlank(message = "配置键不能为空")
    private String configKey;

    private String configValue;

    @NotBlank(message = "配置名称不能为空")
    private String configName;

    private String remark;
}
