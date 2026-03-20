package io.github.canjiemo.momo.system.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.AuditFill;
import io.github.canjiemo.base.myjdbc.annotation.MyField;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MyTable("sys_config")
public class SysConfig implements MyTableEntity {
    private Long id;
    private String configKey;
    private String configValue;
    private String configName;
    private Integer configType;
    private String remark;
    private Long tenantId;
    @MyField(fill = AuditFill.CREATE_BY)
    private String createBy;
    @MyField(fill = AuditFill.CREATE_TIME)
    private LocalDateTime createTime;
    @MyField(fill = AuditFill.UPDATE_BY)
    private String updateBy;
    @MyField(fill = AuditFill.UPDATE_TIME)
    private LocalDateTime updateTime;
    private Integer deleteFlag;
}
