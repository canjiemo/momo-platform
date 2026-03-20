package io.github.canjiemo.momo.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SysConfigUpdateRequest {
    @NotNull(message = "配置ID不能为空")
    private Long id;

    private String configValue;
    private String configName;
    private String remark;
}
