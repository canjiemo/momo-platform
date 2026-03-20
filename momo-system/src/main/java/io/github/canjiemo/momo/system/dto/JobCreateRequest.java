package io.github.canjiemo.momo.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobCreateRequest {
    @NotBlank(message = "任务名称不能为空")
    private String jobName;

    private String jobGroup;

    @NotBlank(message = "处理器名称不能为空")
    private String handlerName;

    @NotBlank(message = "Cron表达式不能为空")
    private String cronExpression;

    private String jobParams;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String remark;
}
