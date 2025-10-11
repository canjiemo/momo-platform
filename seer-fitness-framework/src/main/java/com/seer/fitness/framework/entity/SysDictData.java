package com.seer.fitness.framework.entity;

import com.seer.fitness.framework.annotation.PublicSchema;
import io.github.mocanjie.base.myjpa.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典数据实体类
 * 对应数据库表 sys_dict_data
 * <p>
 * 注意：该实体标记了 @PublicSchema 注解，所有数据库操作将路由到 public schema
 * 原因：字典数据在所有租户间共享，统一存储在 public schema 中
 *
 * @author seer-fitness
 */
@PublicSchema(reason = "字典数据所有租户共享，统一存储在public schema")
@Data
@MyTable("sys_dict_data")
public class SysDictData {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 字典标签(显示值)
     */
    private String dictLabel;

    /**
     * 字典值(实际值)
     */
    private String dictValue;

    /**
     * 字典项描述
     */
    private String dictDescription;

    /**
     * 样式属性(CSS类名)
     */
    private String cssClass;

    /**
     * 表格样式
     */
    private String listClass;

    /**
     * 是否默认(1:是 0:否)
     */
    private Integer isDefault;

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
