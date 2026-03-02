package com.seer.fitness.system.entity;

import io.github.mocanjie.base.myjpa.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户角色关联实体类
 * 对应数据库表 sys_user_role
 *
 * @author seer-fitness
 */
@Data
@MyTable("sys_user_role")
public class SysUserRole {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID(关联sys_user.id)
     */
    private Long userId;

    /**
     * 角色ID(关联sys_role.id)
     */
    private Long roleId;

    /**
     * 分配人ID（谁分配的角色）
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}