package com.seer.fitness.ai.catalog.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.AuditFill;
import io.github.canjiemo.base.myjdbc.annotation.MyField;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MyTable("ai_table_catalog")
public class AiTableCatalog implements MyTableEntity {
    private Long id;
    private String tableName;
    private String displayName;
    private String description;
    private Integer isEnabled;
    private Integer sortOrder;
    @MyField(fill = AuditFill.CREATE_TIME)
    private LocalDateTime createTime;
    @MyField(fill = AuditFill.UPDATE_TIME)
    private LocalDateTime updateTime;
}
