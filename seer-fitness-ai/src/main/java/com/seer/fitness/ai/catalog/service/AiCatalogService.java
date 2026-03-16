package com.seer.fitness.ai.catalog.service;

import com.seer.fitness.ai.catalog.dto.AiFieldCatalogDTO;
import com.seer.fitness.ai.catalog.dto.AiTableCatalogDTO;
import com.seer.fitness.ai.catalog.entity.AiFieldCatalog;
import com.seer.fitness.ai.catalog.entity.AiTableCatalog;
import com.seer.fitness.ai.provider.AiProviderManager;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.mycommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Slf4j
@Service
public class AiCatalogService extends BaseServiceImpl implements IAiCatalogService {

    @Autowired
    private AiProviderManager providerManager;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    /** 扫描数据库获取所有表和字段（来自 information_schema） */
    @Override
    public List<AiTableCatalogDTO> scanDatabase() {
        String sql = """
            SELECT c.table_name, c.column_name, c.data_type, c.ordinal_position
            FROM information_schema.columns c
            WHERE c.table_schema = 'public'
              AND c.table_name NOT LIKE 'ai_%'
            ORDER BY c.table_name, c.ordinal_position
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, Map.of());

        Map<String, AiTableCatalogDTO> tableMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String tableName = (String) row.get("table_name");
            AiTableCatalogDTO table = tableMap.computeIfAbsent(tableName, k -> {
                AiTableCatalogDTO dto = new AiTableCatalogDTO();
                dto.setTableName(k);
                dto.setDisplayName(k);
                dto.setIsEnabled(0);
                dto.setFields(new ArrayList<>());
                return dto;
            });
            AiFieldCatalogDTO field = new AiFieldCatalogDTO();
            field.setTableName(tableName);
            field.setFieldName((String) row.get("column_name"));
            field.setFieldType((String) row.get("data_type"));
            field.setDisplayName((String) row.get("column_name"));
            field.setIsEnabled(1);
            table.getFields().add(field);
        }

        // 合并已配置的中文名和描述
        List<AiTableCatalog> existingTables = lambdaQuery(AiTableCatalog.class).list();
        Map<String, AiTableCatalog> existingMap = new HashMap<>();
        existingTables.forEach(t -> existingMap.put(t.getTableName(), t));

        tableMap.forEach((tableName, dto) -> {
            AiTableCatalog existing = existingMap.get(tableName);
            if (existing != null) {
                dto.setId(existing.getId());
                dto.setDisplayName(existing.getDisplayName());
                dto.setDescription(existing.getDescription());
                dto.setIsEnabled(existing.getIsEnabled());
            }
        });

        return new ArrayList<>(tableMap.values());
    }

    @Override
    public List<AiTableCatalogDTO> listTables() {
        return lambdaQuery(AiTableCatalog.class, AiTableCatalogDTO.class)
                .orderByAsc(AiTableCatalog::getSortOrder)
                .list();
    }

    @Override
    public List<AiFieldCatalogDTO> listFields(Long tableId) {
        return lambdaQuery(AiFieldCatalog.class, AiFieldCatalogDTO.class)
                .eq(AiFieldCatalog::getTableId, tableId)
                .orderByAsc(AiFieldCatalog::getSortOrder)
                .list();
    }

    /** 保存表配置 */
    @Override
    @Transactional
    public void saveTable(AiTableCatalog request) {
        if (request.getId() == null) {
            baseDao.insertPO(request, true);
        } else {
            AiTableCatalog existing = baseDao.queryById(request.getId(), AiTableCatalog.class);
            if (existing == null) throw new BusinessException("表配置不存在");
            if (request.getDisplayName() != null) existing.setDisplayName(request.getDisplayName());
            if (request.getDescription() != null) existing.setDescription(request.getDescription());
            if (request.getIsEnabled()   != null) existing.setIsEnabled(request.getIsEnabled());
            if (request.getSortOrder()   != null) existing.setSortOrder(request.getSortOrder());
            existing.setUpdateTime(null);
            baseDao.updatePO(existing);
        }
    }

    /** 保存字段配置，保存后触发向量同步 */
    @Override
    @Transactional
    public void saveField(AiFieldCatalog request) {
        boolean descChanged = false;
        if (request.getId() == null) {
            baseDao.insertPO(request, true);
            descChanged = true;
        } else {
            AiFieldCatalog existing = baseDao.queryById(request.getId(), AiFieldCatalog.class);
            if (existing == null) throw new BusinessException("字段配置不存在");
            if (request.getDisplayName() != null) existing.setDisplayName(request.getDisplayName());
            if (request.getIsEnabled()   != null) existing.setIsEnabled(request.getIsEnabled());
            if (request.getSortOrder()   != null) existing.setSortOrder(request.getSortOrder());
            if (request.getDescription() != null) {
                existing.setDescription(request.getDescription());
                descChanged = true;
            }
            existing.setUpdateTime(null);
            baseDao.updatePO(existing);
            request.setId(existing.getId());
        }
        // 描述有变化则重新生成向量
        if (descChanged && request.getDescription() != null) {
            syncFieldVector(request.getId());
        }
    }

    /** 单字段向量同步 */
    public void syncFieldVector(Long fieldId) {
        AiFieldCatalog field = baseDao.queryById(fieldId, AiFieldCatalog.class);
        if (field == null || field.getDescription() == null) return;
        String text = field.getDisplayName() + ": " + field.getDescription();
        float[] vector = providerManager.getActiveEmbed().embed(text);
        String vectorStr = Arrays.toString(vector);
        jdbcTemplate.update(
                "UPDATE ai_field_catalog SET embed_vector = :v::vector WHERE id = :id",
                Map.of("v", vectorStr, "id", fieldId)
        );
        log.info("字段向量已同步: fieldId={}", fieldId);
    }

    /** 全量向量同步 */
    @Override
    public void syncAllVectors() {
        List<AiFieldCatalog> fields = lambdaQuery(AiFieldCatalog.class)
                .eq(AiFieldCatalog::getIsEnabled, 1)
                .list();
        int count = 0;
        for (AiFieldCatalog field : fields) {
            if (field.getDescription() != null) {
                syncFieldVector(field.getId());
                count++;
            }
        }
        log.info("全量向量同步完成，共 {} 个字段", count);
    }
}
