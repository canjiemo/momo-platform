package com.seer.fitness.business.dto;

import io.github.mocanjie.base.mycommon.pager.PagerParam;
import lombok.Data;

/**
 * 项目查询参数
 *
 * @author seer-fitness
 */
@Data
public class ProjectInfoQueryParam extends PagerParam {

    /**
     * 项目编号（模糊查询）
     */
    private String projectCode;

    /**
     * 项目名称（模糊查询）
     */
    private String projectName;

    /**
     * 状态：0=禁用，1=启用
     */
    private Integer status;
}
