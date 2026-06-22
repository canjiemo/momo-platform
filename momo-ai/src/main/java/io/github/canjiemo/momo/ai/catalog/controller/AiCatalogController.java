package io.github.canjiemo.momo.ai.catalog.controller;

import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.momo.ai.catalog.dto.AiFieldCatalogDTO;
import io.github.canjiemo.momo.ai.catalog.dto.AiTableCatalogDTO;
import io.github.canjiemo.momo.ai.catalog.entity.AiFieldCatalog;
import io.github.canjiemo.momo.ai.catalog.entity.AiTableCatalog;
import io.github.canjiemo.momo.ai.catalog.service.IAiCatalogService;
import io.github.canjiemo.momo.framework.annotation.RequireAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/platform/ai/catalog")
public class AiCatalogController extends MyBaseController {

    @Autowired
    private IAiCatalogService catalogService;

    @RequireAuth(permissions = {"ai:catalog:scan"})
    @GetMapping("/scan")
    public MyResponseResult<List<AiTableCatalogDTO>> scan() {
        return doJsonOut(catalogService.scanDatabase());
    }

    @RequireAuth(permissions = {"ai:catalog:scan"})
    @GetMapping("/tables")
    public MyResponseResult<List<AiTableCatalogDTO>> tables() {
        return doJsonOut(catalogService.listTables());
    }

    @RequireAuth(permissions = {"ai:catalog:edit"})
    @PostMapping("/table/save")
    public MyResponseResult saveTable(@RequestBody AiTableCatalog request) {
        catalogService.saveTable(request);
        return doJsonDefaultMsg();
    }

    @RequireAuth(permissions = {"ai:catalog:scan"})
    @GetMapping("/fields/{tableId}")
    public MyResponseResult<List<AiFieldCatalogDTO>> fields(@PathVariable Long tableId) {
        return doJsonOut(catalogService.listFields(tableId));
    }

    @RequireAuth(permissions = {"ai:catalog:edit"})
    @PostMapping("/field/save")
    public MyResponseResult saveField(@RequestBody AiFieldCatalog request) {
        catalogService.saveField(request);
        return doJsonDefaultMsg();
    }

    @RequireAuth(permissions = {"ai:catalog:edit"})
    @PostMapping("/fields/refresh")
    public MyResponseResult<Map<String, Object>> refreshFields(@RequestBody List<Long> tableIds) {
        int total = 0;
        for (Long tableId : tableIds) {
            total += catalogService.refreshFields(tableId);
        }
        return doJsonOut(Map.of("added", total));
    }

    @RequireAuth(permissions = {"ai:catalog:sync"})
    @PostMapping("/embed/sync")
    public MyResponseResult<Map<String, Object>> syncVectors() {
        int[] result = catalogService.syncAllVectors();
        return doJsonOut(Map.of("success", result[0], "failed", result[1]));
    }
}
