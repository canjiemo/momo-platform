package com.seer.fitness.system.entity;

import io.github.mocanjie.base.myjpa.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户（学校）实体类
 * 对应数据库表 public.sys_tenant
 * <p>
 * 用于多租户（多学校）隔离，基于 PostgreSQL Schema 隔离方案
 * 每个租户拥有独立的 Schema，存储隔离的业务数据
 *
 * @author seer-fitness
 */
@Data
@MyTable("public.sys_tenant")
public class SysTenant {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户编码
     * 全局唯一，例如：SCHOOL_001
     */
    private String tenantCode;

    /**
     * 租户名称（学校名称）
     * 例如：XX中学
     */
    private String tenantName;

    /**
     * PostgreSQL Schema 名称
     * 全局唯一，例如：school_001
     */
    private String schemaName;

    /**
     * 管理员用户名
     */
    private String adminUsername;

    /**
     * 管理员真实姓名
     */
    private String adminRealName;

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
     * @see com.seer.fitness.system.enums.TenantStatus
     */
    private Integer status;

    /**
     * 激活时间
     */
    private LocalDateTime activatedAt;

    /**
     * 过期时间
     * NULL表示永不过期
     */
    private LocalDateTime expiredAt;

    /**
     * 最大用户数限制
     */
    private Integer maxUsers;

    /**
     * 最大存储空间限制（GB）
     */
    private Integer maxStorageGb;

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
