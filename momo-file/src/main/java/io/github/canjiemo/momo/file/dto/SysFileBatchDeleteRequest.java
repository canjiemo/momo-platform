package io.github.canjiemo.momo.file.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class SysFileBatchDeleteRequest {

    /**
     * 要删除的文件 ID 列表，至少包含一个
     */
    @NotEmpty(message = "删除的文件ID不能为空")
    private Long[] ids;
}
