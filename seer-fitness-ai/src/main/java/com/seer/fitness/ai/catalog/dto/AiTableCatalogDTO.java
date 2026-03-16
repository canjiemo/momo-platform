package com.seer.fitness.ai.catalog.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiTableCatalogDTO {
    private Long id;
    private String tableName;
    private String displayName;
    private String description;
    private Integer isEnabled;
    private Integer sortOrder;
    // 扫描时用：从 information_schema 获取的字段列表
    private List<AiFieldCatalogDTO> fields;
}
