package io.github.canjiemo.momo.system.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 字典数据批量更新排序请求参数
 *
 * @author canjiemo@gmail.com
 */
@Data
public class DictDataBatchUpdateSortRequest {

    /**
     * 字典数据ID列表
     * 不能为空，至少要有一个ID
     */
    @NotEmpty(message = "字典数据ID列表不能为空")
    private List<String> ids;

    /**
     * 对应的排序值列表
     * 不能为空，且数量必须与ID列表一致
     */
    @NotEmpty(message = "排序值列表不能为空")
    @Size(min = 1, message = "排序值列表至少要有一个元素")
    private List<Integer> sortOrders;
}