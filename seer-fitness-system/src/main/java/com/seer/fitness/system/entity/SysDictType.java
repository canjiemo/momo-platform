package com.seer.fitness.system.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.AuditFill;
import io.github.canjiemo.base.myjdbc.annotation.MyField;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典类型实体类
 * 对应数据库表 sys_dict_type
 *
 * @author seer-fitness
 */
@Data
@MyTable("sys_dict_type")
public class SysDictType implements MyTableEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID，NULL表示平台字典
     */
    private Long tenantId;

    /**
     * 字典名称
     */
    private String dictName;

    /**
     * 字典类型(唯一标识)
     */
    private String dictType;

    /**
     * 字典描述
     */
    private String dictDescription;

    /**
     * 状态(1:启用 0:禁用)
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 备注
     */
    private String remark;

    @MyField(fill = AuditFill.CREATE_BY)
    private String createBy;

    @MyField(fill = AuditFill.CREATE_TIME)
    private LocalDateTime createTime;

    @MyField(fill = AuditFill.UPDATE_BY)
    private String updateBy;

    @MyField(fill = AuditFill.UPDATE_TIME)
    private LocalDateTime updateTime;

    /**
     * 删除标记(0:正常 1:已删除)
     */
    private Integer deleteFlag;
}
