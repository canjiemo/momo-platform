package io.github.canjiemo.momo.ai.catalog.service;

import io.github.canjiemo.momo.ai.catalog.dto.AiFieldCatalogDTO;
import io.github.canjiemo.momo.ai.catalog.dto.AiTableCatalogDTO;
import io.github.canjiemo.momo.ai.catalog.entity.AiFieldCatalog;
import io.github.canjiemo.momo.ai.catalog.entity.AiTableCatalog;

import java.util.List;

public interface IAiCatalogService {
    List<AiTableCatalogDTO> scanDatabase();
    List<AiTableCatalogDTO> listTables();
    List<AiFieldCatalogDTO> listFields(Long tableId);
    void saveTable(AiTableCatalog request);
    void saveField(AiFieldCatalog request);
    int[] syncAllVectors();
    int refreshFields(Long tableId);
}
