package io.github.canjiemo.momo.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 字典数据创建请求
 *
 * @author canjiemo@gmail.com
 */
@Data
public class DictDataCreateRequest {

    /**
     * 字典类型
     */
    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100, message = "字典类型长度不能超过100个字符")
    private String dictType;

    /**
     * 字典标签(显示值)
     */
    @NotBlank(message = "字典标签不能为空")
    @Size(max = 100, message = "字典标签长度不能超过100个字符")
    private String dictLabel;

    /**
     * 字典值(实际值)
     */
    @NotBlank(message = "字典值不能为空")
    @Size(max = 100, message = "字典值长度不能超过100个字符")
    private String dictValue;

    /**
     * 字典项描述
     */
    @Size(max = 500, message = "字典项描述长度不能超过500个字符")
    private String dictDescription;

    /**
     * 样式属性(CSS类名)
     */
    @Size(max = 100, message = "样式属性长度不能超过100个字符")
    private String cssClass;

    /**
     * 表格样式
     */
    @Size(max = 100, message = "表格样式长度不能超过100个字符")
    private String listClass;

    /**
     * 是否默认(true:是 false:否)
     */
    private Integer isDefault = 0;

    /**
     * 状态(true:启用 false:禁用)
     */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 排序
     */
    private Integer sortOrder = 0;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}