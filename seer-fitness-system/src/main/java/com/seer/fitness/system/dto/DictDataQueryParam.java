package com.seer.fitness.system.dto;

import io.github.canjiemo.mycommon.pager.PagerParam;
import lombok.Data;

/**
 * 字典数据分页查询参数
 * POST方式查询，配合独立的Pager参数使用
 *
 * @author seer-fitness
 */
@Data
public class DictDataQueryParam extends PagerParam {

    /**
     * 字典类型 (支持精确查询)
     */
    private String dictType;

    /**
     * 字典标签 (支持模糊查询)
     */
    private String dictLabel;

    /**
     * 字典值 (支持模糊查询)
     */
    private String dictValue;

    /**
     * 状态(true:启用 false:禁用)
     */
    private Integer status;
}