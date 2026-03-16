package com.seer.fitness.ai.catalog.controller;

import com.seer.fitness.ai.catalog.dto.AiFieldCatalogDTO;
import com.seer.fitness.ai.catalog.dto.AiTableCatalogDTO;
import com.seer.fitness.ai.catalog.entity.AiFieldCatalog;
import com.seer.fitness.ai.catalog.entity.AiTableCatalog;
import com.seer.fitness.ai.catalog.service.IAiCatalogService;
import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/platform/ai/catalog")
public class AiCatalogController extends MyBaseController {

    @Autowired
    private IAiCatalogService catalogService;

    @GetMapping("/scan")
    public MyResponseResult<List<AiTableCatalogDTO>> scan() {
        return doJsonOut(catalogService.scanDatabase());
    }

    @GetMapping("/tables")
    public MyResponseResult<List<AiTableCatalogDTO>> tables() {
        return doJsonOut(catalogService.listTables());
    }

    @PostMapping("/table/save")
    public MyResponseResult saveTable(@RequestBody AiTableCatalog request) {
        catalogService.saveTable(request);
        return doJsonDefaultMsg();
    }

    @GetMapping("/fields/{tableId}")
    public MyResponseResult<List<AiFieldCatalogDTO>> fields(@PathVariable Long tableId) {
        return doJsonOut(catalogService.listFields(tableId));
    }

    @PostMapping("/field/save")
    public MyResponseResult saveField(@RequestBody AiFieldCatalog request) {
        catalogService.saveField(request);
        return doJsonDefaultMsg();
    }

    @PostMapping("/embed/sync")
    public MyResponseResult syncVectors() {
        catalogService.syncAllVectors();
        return doJsonDefaultMsg();
    }
}
