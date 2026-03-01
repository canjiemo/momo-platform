package com.seer.fitness.system.dto;

import io.github.mocanjie.base.mycommon.pager.PagerParam;
import lombok.Data;

/**
 * 角色查询参数DTO
 * 用于复杂条件查询
 *
 * @author seer-fitness
 */
@Data
public class RoleQueryParam extends PagerParam {

    /**
     * 角色名称（模糊查询）
     */
    private String roleName;

    /**
     * 角色编码（精确查询）
     */
    private String roleCode;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;
}