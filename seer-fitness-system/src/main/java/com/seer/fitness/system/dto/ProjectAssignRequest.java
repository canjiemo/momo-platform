package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 项目分配请求
 * 租户从平台分配项目
 *
 * @author seer-fitness
 */
@Data
public class ProjectAssignRequest {

    /**
     * 平台项目ID列表
     */
    @NotEmpty(message = "项目ID列表不能为空")
    private List<Long> projectIds;
}
