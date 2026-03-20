package io.github.canjiemo.momo.ai.catalog.service;

import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.momo.ai.catalog.dto.AiFieldCatalogDTO;
import io.github.canjiemo.momo.ai.catalog.dto.AiTableCatalogDTO;
import io.github.canjiemo.momo.ai.catalog.entity.AiFieldCatalog;
import io.github.canjiemo.momo.ai.catalog.entity.AiTableCatalog;
import io.github.canjiemo.momo.ai.provider.AiProviderManager;
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

    /** 扫描数据库获取所有表和字段，含表注释和字段注释（DB 注释作为默认值，已配置的优先） */
    @Override
    public List<AiTableCatalogDTO> scanDatabase() {
        // 1. 查所有表及其 DB 注释
        String tableSql = """
            SELECT pc.relname AS table_name, pgd.description AS table_comment
            FROM pg_class pc
            JOIN pg_namespace pn ON pn.oid = pc.relnamespace
            LEFT JOIN pg_description pgd ON pgd.objoid = pc.oid AND pgd.objsubid = 0
            WHERE pn.nspname = 'public'
              AND pc.relkind = 'r'
              AND pc.relname NOT LIKE 'ai_%'
            ORDER BY pc.relname
            """;
        List<Map<String, Object>> tableRows = jdbcTemplate.queryForList(tableSql, Map.of());

        Map<String, AiTableCatalogDTO> tableMap = new LinkedHashMap<>();
        for (Map<String, Object> row : tableRows) {
            String tableName = (String) row.get("table_name");
            AiTableCatalogDTO dto = new AiTableCatalogDTO();
            dto.setTableName(tableName);
            dto.setDisplayName(tableName);
            dto.setDescription((String) row.get("table_comment")); // DB 表注释
            dto.setIsEnabled(0);
            dto.setFields(new ArrayList<>());
            tableMap.put(tableName, dto);
        }

        // 2. 查所有字段及其 DB 注释
        String colSql = """
            SELECT
                pc.relname                                         AS table_name,
                a.attname                                          AS column_name,
                pg_catalog.format_type(a.atttypid, a.atttypmod)   AS data_type,
                a.attnum                                           AS ordinal_position,
                pgd.description                                    AS column_comment
            FROM pg_attribute a
            JOIN pg_class pc ON pc.oid = a.attrelid
            JOIN pg_namespace pn ON pn.oid = pc.relnamespace
            LEFT JOIN pg_description pgd ON pgd.objoid = a.attrelid AND pgd.objsubid = a.attnum
            WHERE pn.nspname = 'public'
              AND pc.relkind = 'r'
              AND pc.relname NOT LIKE 'ai_%'
              AND a.attnum > 0
              AND NOT a.attisdropped
            ORDER BY pc.relname, a.attnum
            """;
        List<Map<String, Object>> colRows = jdbcTemplate.queryForList(colSql, Map.of());

        for (Map<String, Object> row : colRows) {
            String tableName = (String) row.get("table_name");
            AiTableCatalogDTO table = tableMap.get(tableName);
            if (table == null) continue;
            AiFieldCatalogDTO field = new AiFieldCatalogDTO();
            field.setTableName(tableName);
            field.setFieldName((String) row.get("column_name"));
            field.setFieldType((String) row.get("data_type"));
            field.setDisplayName((String) row.get("column_name"));
            field.setDescription((String) row.get("column_comment")); // DB 字段注释
            field.setIsEnabled(1);
            table.getFields().add(field);
        }

        // 3. 合并已配置数据（已配置的优先，DB 注释作为兜底）
        List<AiTableCatalog> existingTables = lambdaQuery(AiTableCatalog.class).list();
        Map<String, AiTableCatalog> existingMap = new HashMap<>();
        existingTables.forEach(t -> existingMap.put(t.getTableName(), t));

        tableMap.forEach((tableName, dto) -> {
            AiTableCatalog existing = existingMap.get(tableName);
            if (existing != null) {
                dto.setId(existing.getId());
                dto.setDisplayName(existing.getDisplayName());
                dto.setIsEnabled(existing.getIsEnabled());
                // 已配置了描述则用配置的，否则保留 DB 注释
                if (existing.getDescription() != null) {
                    dto.setDescription(existing.getDescription());
                }
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

    /** 保存表配置，首次保存时自动从 information_schema 批量导入字段 */
    @Override
    @Transactional
    public void saveTable(AiTableCatalog request) {
        if (request.getId() == null) {
            // 未传 description 时自动从 pg_description 取表注释
            if (request.getDescription() == null) {
                request.setDescription(queryTableComment(request.getTableName()));
            }
            baseDao.insertPO(request, true);
            importFieldsFromSchema(request.getId(), request.getTableName());
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

    /** 从 information_schema 批量导入指定表的字段到 ai_field_catalog（含 DB 注释作为默认 description） */
    private void importFieldsFromSchema(Long tableId, String tableName) {
        List<Map<String, Object>> columns = queryColumnsWithComment(tableName);
        for (Map<String, Object> col : columns) {
            AiFieldCatalog field = new AiFieldCatalog();
            field.setTableId(tableId);
            field.setTableName(tableName);
            field.setFieldName((String) col.get("column_name"));
            field.setFieldType((String) col.get("data_type"));
            field.setDisplayName((String) col.get("column_name"));
            field.setDescription((String) col.get("column_comment")); // DB 注释，可能为 null
            field.setIsEnabled(1);
            field.setSortOrder(((Number) col.get("ordinal_position")).intValue());
            baseDao.insertPO(field, true);
        }
        log.info("字段自动导入完成: table={}, count={}", tableName, columns.size());
    }

    /**
     * 刷新字段：对比 information_schema，只新增不存在的字段，已有配置不覆盖。
     * @return 新增字段数量
     */
    @Override
    @Transactional
    public int refreshFields(Long tableId) {
        AiTableCatalog table = baseDao.queryById(tableId, AiTableCatalog.class);
        if (table == null) throw new BusinessException("表配置不存在");

        // 已有字段名集合
        Set<String> existingFields = lambdaQuery(AiFieldCatalog.class)
                .eq(AiFieldCatalog::getTableId, tableId)
                .list()
                .stream()
                .map(AiFieldCatalog::getFieldName)
                .collect(java.util.stream.Collectors.toSet());

        List<Map<String, Object>> columns = queryColumnsWithComment(table.getTableName());
        int added = 0;
        for (Map<String, Object> col : columns) {
            String fieldName = (String) col.get("column_name");
            if (existingFields.contains(fieldName)) continue; // 已有，跳过
            AiFieldCatalog field = new AiFieldCatalog();
            field.setTableId(tableId);
            field.setTableName(table.getTableName());
            field.setFieldName(fieldName);
            field.setFieldType((String) col.get("data_type"));
            field.setDisplayName(fieldName);
            field.setDescription((String) col.get("column_comment"));
            field.setIsEnabled(1);
            field.setSortOrder(((Number) col.get("ordinal_position")).intValue());
            baseDao.insertPO(field, true);
            added++;
        }
        log.info("字段刷新完成: tableId={}, 新增={}", tableId, added);
        return added;
    }

    /** 查询表的 PostgreSQL 注释（COMMENT ON TABLE） */
    private String queryTableComment(String tableName) {
        String sql = """
            SELECT pgd.description
            FROM pg_class pc
            JOIN pg_namespace pn ON pn.oid = pc.relnamespace
            LEFT JOIN pg_description pgd ON pgd.objoid = pc.oid AND pgd.objsubid = 0
            WHERE pn.nspname = 'public'
              AND pc.relkind = 'r'
              AND pc.relname = :tableName
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, Map.of("tableName", tableName));
        if (rows.isEmpty()) return null;
        return (String) rows.get(0).get("description");
    }

    /** 查询表的列信息，含 PostgreSQL 列注释（使用 regclass 精确定位当前表 OID，避免历史 OID 重复） */
    private List<Map<String, Object>> queryColumnsWithComment(String tableName) {
        String sql = """
            SELECT
                a.attname                                      AS column_name,
                pg_catalog.format_type(a.atttypid, a.atttypmod) AS data_type,
                a.attnum                                       AS ordinal_position,
                pgd.description                                AS column_comment
            FROM pg_attribute a
            LEFT JOIN pg_description pgd ON pgd.objoid = a.attrelid AND pgd.objsubid = a.attnum
            WHERE a.attrelid = (:tableName || '')::regclass
              AND a.attnum > 0
              AND NOT a.attisdropped
            ORDER BY a.attnum
            """;
        return jdbcTemplate.queryForList(sql, Map.of("tableName", tableName));
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
        // 描述有变化则重新生成向量（事务提交后执行，Ollama 不可用时不影响元数据保存）
        if (descChanged && request.getDescription() != null) {
            try {
                syncFieldVector(request.getId());
            } catch (Exception e) {
                log.warn("字段向量同步失败（可通过全量同步接口重试），fieldId={}, error={}", request.getId(), e.getMessage());
            }
        }
    }

    /** 单字段向量同步（通过 ID，用于 saveField 后触发） */
    private void syncFieldVector(Long fieldId) {
        // 用 lambdaQuery 而非 queryById，避免 myjdbc 对无 delete_flag 表自动注入条件报错
        AiFieldCatalog field = lambdaQuery(AiFieldCatalog.class)
                .eq(AiFieldCatalog::getId, fieldId)
                .one();
        if (field == null || field.getDescription() == null) return;
        doSyncVector(field);
    }

    /** 执行向量化并写入（公共逻辑，接收实体对象避免二次查询） */
    private void doSyncVector(AiFieldCatalog field) {
        // description 为 null 时退化为仅用 displayName
        String text = field.getDescription() != null
                ? field.getDisplayName() + ": " + field.getDescription()
                : field.getDisplayName();
        float[] vector = providerManager.getActiveEmbed().embed(text);
        String vectorStr = Arrays.toString(vector);
        jdbcTemplate.update(
                "UPDATE ai_field_catalog SET embed_vector = :v::vector WHERE id = :id",
                Map.of("v", vectorStr, "id", field.getId())
        );
        log.info("字段向量已同步: fieldId={}", field.getId());
    }

    /** 全量向量同步，返回 [成功数, 失败数] */
    @Override
    @Transactional
    public int[] syncAllVectors() {
        List<AiFieldCatalog> fields = lambdaQuery(AiFieldCatalog.class)
                .eq(AiFieldCatalog::getIsEnabled, 1)
                .list();
        int success = 0, failed = 0;
        for (AiFieldCatalog field : fields) {
            try {
                doSyncVector(field);
                success++;
            } catch (Exception e) {
                failed++;
                log.warn("字段向量同步失败: fieldId={}, error={}", field.getId(), e.getMessage());
            }
        }
        log.info("全量向量同步完成: 成功={} 失败={}", success, failed);
        return new int[]{success, failed};
    }
}
