package com.seer.fitness.ai.catalog.dto;

import lombok.Data;

@Data
public class AiFieldCatalogDTO {
    private Long id;
    private Long tableId;
    private String tableName;
    private String fieldName;
    private String fieldType;
    private String displayName;
    private String description;
    private Integer isEnabled;
    private Integer sortOrder;
}
