package com.seer.fitness.system.entity;

import io.github.mocanjie.base.myjpa.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户（学校）实体类
 * 对应数据库表 public.sys_tenant（平台级表）
 *
 * @author seer-fitness
 */
@Data
@MyTable("sys_tenant")
public class SysTenant {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户编码，全局唯一
     */
    private String tenantCode;

    /**
     * 管理员登录账号，创建后不可修改
     */
    private String tenantName;

    /**
     * 学校中文名称，作为管理员账号的显示名称
     */
    private String realName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系邮箱
     */
    private String contactEmail;

    /**
     * 学校地址
     */
    private String address;

    /**
     * 租户描述
     */
    private String description;

    /**
     * 状态：0-待激活 1-正常 2-已禁用 3-已过期
     */
    private Integer status;

    /**
     * 激活时间
     */
    private LocalDateTime activatedAt;

    /**
     * 过期时间，NULL表示永不过期
     */
    private LocalDateTime expiredAt;

    /**
     * 最大用户数限制
     */
    private Integer maxUsers;

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
