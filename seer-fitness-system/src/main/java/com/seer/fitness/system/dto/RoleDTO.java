package com.seer.fitness.system.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色DTO
 *
 * @author seer-fitness
 */
@Data
public class RoleDTO {

    /**
     * 角色ID
     */
    private Long id;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;


}