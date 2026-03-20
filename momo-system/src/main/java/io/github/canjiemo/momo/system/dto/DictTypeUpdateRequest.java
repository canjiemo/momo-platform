package io.github.canjiemo.momo.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 字典类型更新请求
 *
 * @author canjiemo@gmail.com
 */
@Data
public class DictTypeUpdateRequest {

    /**
     * 主键ID
     */
    @NotBlank(message = "ID不能为空")
    private Long id;

    /**
     * 字典名称
     */
    @NotBlank(message = "字典名称不能为空")
    @Size(max = 100, message = "字典名称长度不能超过100个字符")
    private String dictName;

    /**
     * 字典类型(唯一标识)
     */
    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100, message = "字典类型长度不能超过100个字符")
    private String dictType;

    /**
     * 字典描述
     */
    @Size(max = 500, message = "字典描述长度不能超过500个字符")
    private String dictDescription;

    /**
     * 状态(true:启用 false:禁用)
     */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}