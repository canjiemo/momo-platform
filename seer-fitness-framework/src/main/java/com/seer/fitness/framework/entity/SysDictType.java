package com.seer.fitness.framework.entity;

import com.seer.fitness.framework.annotation.PublicSchema;
import io.github.mocanjie.base.myjpa.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典类型实体类
 * 对应数据库表 sys_dict_type
 * <p>
 * 注意：该实体标记了 @PublicSchema 注解，所有数据库操作将路由到 public schema
 * 原因：字典数据在所有租户间共享，统一存储在 public schema 中
 *
 * @author seer-fitness
 */
@PublicSchema(reason = "字典类型数据所有租户共享，统一存储在public schema")
@Data
@MyTable("sys_dict_type")
public class SysDictType {

    /**
     * 主键ID
     */
    private Long id;

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

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除标记(0:正常 1:已删除)
     */
    private Integer deleteFlag;
}
