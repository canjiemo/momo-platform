package io.github.canjiemo.momo.system.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 字典类型删除请求参数
 *
 * @author canjiemo@gmail.com
 */
@Data
public class DictTypeDeleteRequest {

    /**
     * 要删除的字典类型ID列表
     * 不能为空，至少要有一个ID
     */
    @NotEmpty(message = "删除的字典类型ID列表不能为空")
    private String[] ids;
}