package io.github.canjiemo.momo.system.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 字典数据删除请求参数
 *
 * @author canjiemo@gmail.com
 */
@Data
public class DictDataDeleteRequest {

    /**
     * 要删除的字典数据ID列表
     * 不能为空，至少要有一个ID
     */
    @NotEmpty(message = "删除的字典数据ID列表不能为空")
    private String[] ids;
}