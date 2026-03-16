package com.seer.fitness.ai.engine;

import com.seer.fitness.ai.catalog.entity.AiFieldCatalog;
import com.seer.fitness.ai.catalog.entity.AiTableCatalog;
import com.seer.fitness.ai.provider.AiProviderManager;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SchemaContextBuilder extends BaseServiceImpl {

    private static final int TOP_K = 10;

    @Autowired
    private AiProviderManager providerManager;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * 将用户问题向量化，检索最相关字段，构建 Schema 上下文文本
     */
    public String build(String question) {
        // 1. 向量化用户问题
        float[] queryVector = providerManager.getActiveEmbed().embed(question);
        String vectorStr = Arrays.toString(queryVector);

        // 2. pgvector 余弦相似度检索 Top-K 字段
        String sql = """
            SELECT fc.table_name, fc.field_name, fc.display_name, fc.description,
                   tc.display_name AS table_display_name, tc.description AS table_description
            FROM ai_field_catalog fc
            JOIN ai_table_catalog tc ON tc.table_name = fc.table_name AND tc.is_enabled = 1
            WHERE fc.is_enabled = 1
              AND fc.embed_vector IS NOT NULL
            ORDER BY fc.embed_vector <=> :vec::vector
            LIMIT :topK
            """;
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                sql, Map.of("vec", vectorStr, "topK", TOP_K));

        if (results.isEmpty()) {
            // fallback: 返回所有开放表的全量 Schema
            return buildFullSchema();
        }

        // 3. 按表分组，构建 Schema 文本
        Map<String, List<Map<String, Object>>> byTable = new LinkedHashMap<>();
        for (Map<String, Object> row : results) {
            byTable.computeIfAbsent((String) row.get("table_name"), k -> new ArrayList<>()).add(row);
        }

        StringBuilder sb = new StringBuilder();
        byTable.forEach((tableName, fields) -> {
            Map<String, Object> first = fields.get(0);
            sb.append("表名: ").append(tableName)
              .append("（").append(first.get("table_display_name")).append("）\n");
            if (first.get("table_description") != null) {
                sb.append("  说明: ").append(first.get("table_description")).append("\n");
            }
            for (Map<String, Object> f : fields) {
                sb.append("  - ").append(f.get("field_name"))
                  .append("（").append(f.get("display_name")).append("）");
                if (f.get("description") != null) {
                    sb.append(": ").append(f.get("description"));
                }
                sb.append("\n");
            }
            sb.append("\n");
        });

        log.debug("Schema 上下文构建完成，涉及 {} 张表", byTable.size());
        return sb.toString();
    }

    /** 全量 Schema（无向量时的 fallback） */
    private String buildFullSchema() {
        List<AiTableCatalog> tables = lambdaQuery(AiTableCatalog.class)
                .eq(AiTableCatalog::getIsEnabled, 1).list();
        if (tables.isEmpty()) return "";

        // 一次性查询所有启用字段，避免 N+1
        List<AiFieldCatalog> allFields = lambdaQuery(AiFieldCatalog.class)
                .eq(AiFieldCatalog::getIsEnabled, 1).list();
        Map<Long, List<AiFieldCatalog>> fieldsByTable = allFields.stream()
                .collect(Collectors.groupingBy(AiFieldCatalog::getTableId));

        StringBuilder sb = new StringBuilder();
        for (AiTableCatalog t : tables) {
            sb.append("表名: ").append(t.getTableName())
              .append("（").append(t.getDisplayName()).append("）\n");
            List<AiFieldCatalog> fields = fieldsByTable.getOrDefault(t.getId(), List.of());
            for (AiFieldCatalog f : fields) {
                sb.append("  - ").append(f.getFieldName())
                  .append("（").append(f.getDisplayName()).append("）");
                if (f.getDescription() != null) sb.append(": ").append(f.getDescription());
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
