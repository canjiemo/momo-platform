package io.github.canjiemo.momo.system.dto;

import io.github.canjiemo.tools.dict.MyDict;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色DTO
 *
 * @author canjiemo@gmail.com
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
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 状态：1启用 0禁用
     */
    @MyDict(type = "common_status")
    private Integer status;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新人ID
     */
    private Long updatedBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}