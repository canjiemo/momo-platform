package com.seer.fitness.framework.entity;

import io.github.mocanjie.base.myjpa.MyTableEntity;
import io.github.mocanjie.base.myjpa.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典数据实体类
 * 对应数据库表 sys_dict_data
 *
 * @author seer-fitness
 */
@Data
@MyTable("sys_dict_data")
public class SysDictData implements MyTableEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID，NULL表示平台字典数据
     */
    private Long tenantId;

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
