package com.seer.fitness.business.dto;

import lombok.Data;

/**
 * 删除项目请求
 *
 * @author seer-fitness
 */
@Data
public class ProjectInfoDeleteRequest {

    /**
     * 项目ID列表
     */
    private String[] ids;
}
