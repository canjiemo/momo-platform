package io.github.canjiemo.momo.file.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SysFileConfigUpdateRequest {
    @NotNull(message = "ID不能为空")
    private Long id;
    private String configName;
    private String config;
    private String remark;
}
