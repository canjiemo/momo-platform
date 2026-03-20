package io.github.canjiemo.momo.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典数据DTO
 *
 * @author canjiemo@gmail.com
 */
@Data
public class DictDataDTO {

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
     * 是否默认(true:是 false:否)
     */
    private Boolean isDefault;

    /**
     * 状态(true:启用 false:禁用)
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
}