package com.seer.fitness.ai.catalog.service;

import com.seer.fitness.ai.catalog.dto.AiFieldCatalogDTO;
import com.seer.fitness.ai.catalog.dto.AiTableCatalogDTO;
import com.seer.fitness.ai.catalog.entity.AiFieldCatalog;
import com.seer.fitness.ai.catalog.entity.AiTableCatalog;
import java.util.List;

public interface IAiCatalogService {
    List<AiTableCatalogDTO> scanDatabase();
    List<AiTableCatalogDTO> listTables();
    List<AiFieldCatalogDTO> listFields(Long tableId);
    void saveTable(AiTableCatalog request);
    void saveField(AiFieldCatalog request);
    void syncAllVectors();
}
