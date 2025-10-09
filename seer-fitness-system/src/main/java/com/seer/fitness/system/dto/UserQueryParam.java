package com.seer.fitness.system.dto;

import io.github.mocanjie.base.mycommon.pager.PagerParam;
import lombok.Data;

/**
 * 用户分页查询参数
 * POST方式查询，配合独立的Pager参数使用
 *
 * @author seer-fitness
 */
@Data
public class UserQueryParam extends PagerParam {

    /**
     * 用户名（模糊查询）
     */
    private String username;

    /**
     * 真实姓名（模糊查询）
     */
    private String realName;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    /**
     * 管理员标识：1管理员 0普通用户
     */
    private Integer adminFlag;

    /**
     * 角色ID
     */
    private Long roleId;
}