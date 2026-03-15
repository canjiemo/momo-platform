package com.seer.fitness.system.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.AuditFill;
import io.github.canjiemo.base.myjdbc.annotation.MyField;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统组织架构实体类
 * 对应数据库表 sys_organization
 * 支持树形结构的组织管理，包括集团、公司、部门、科室、班组等层级
 *
 * @author seer-fitness
 */
@Data
@MyTable("sys_organization")
public class SysOrganization implements MyTableEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID，必填
     */
    private Long tenantId;

    /**
     * 组织编码，唯一标识
     * 用于系统内部标识，支持编码规则
     */
    private String orgCode;

    /**
     * 组织名称
     */
    private String orgName;


    /**
     * 父组织ID，0为顶级组织
     */
    private Long parentId;

    /**
     * 排序字段，数值越小越靠前
     */
    private Integer sortOrder;

    /**
     * 负责人用户ID
     */
    private Long leaderId;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 办公地址
     */
    private String address;

    /**
     * 组织描述
     */
    private String description;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    /**
     * 逻辑删除：0正常 1删除
     */
    private Integer deleteFlag;

    @MyField(fill = AuditFill.CREATE_BY)
    private Long createdBy;

    @MyField(fill = AuditFill.CREATE_TIME)
    private LocalDateTime createTime;

    @MyField(fill = AuditFill.UPDATE_BY)
    private Long updatedBy;

    @MyField(fill = AuditFill.UPDATE_TIME)
    private LocalDateTime updateTime;
}