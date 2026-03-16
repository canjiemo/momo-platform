package com.seer.fitness.ai.catalog.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.AuditFill;
import io.github.canjiemo.base.myjdbc.annotation.MyField;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MyTable("ai_field_catalog")
public class AiFieldCatalog implements MyTableEntity {
    private Long id;
    private Long tableId;
    private String tableName;
    private String fieldName;
    private String fieldType;
    private String displayName;
    private String description;
    private Integer isEnabled;
    // embedVector (pgvector) 不在此实体中声明，仅通过原生 JDBC 读写
    private Integer sortOrder;
    @MyField(fill = AuditFill.CREATE_TIME)
    private LocalDateTime createTime;
    @MyField(fill = AuditFill.UPDATE_TIME)
    private LocalDateTime updateTime;
}
