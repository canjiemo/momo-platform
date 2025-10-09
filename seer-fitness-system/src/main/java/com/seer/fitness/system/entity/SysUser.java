package com.seer.fitness.system.entity;

import io.github.mocanjie.base.myjpa.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体类
 * 对应数据库表 sys_user
 *
 * @author seer-fitness
 */
@Data
@MyTable("sys_user")
public class SysUser {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 加密密码
     */
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 所属组织ID
     */
    private Long orgId;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    /**
     * 管理员标识：1管理员 0普通用户
     */
    private Integer adminFlag;

    /**
     * 用户类型：0-运维人员 1-教师 2-学生
     * @see com.seer.fitness.framework.enums.UserType
     */
    private Integer userType;

    /**
     * 逻辑删除：0正常 1删除
     */
    private Integer deleteFlag;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新人ID
     */
    private Long updatedBy;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}