package com.seer.fitness.system.dto;

import io.github.canjiemo.tools.dict.MyDict;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典类型DTO
 *
 * @author seer-fitness
 */
@Data
public class DictTypeDTO {

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
     * 状态(true:启用 false:禁用)
     */
    @MyDict(type = "common_status")
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
}