package io.github.canjiemo.momo.ai.engine;

import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.momo.ai.provider.AiProviderManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class SchemaContextBuilder extends BaseServiceImpl {

    private static final int TOP_K = 10;
    /** 余弦距离阈值（<=> 越小越相似），超过此值视为不相关，不注入 Prompt。
     *  nomic-embed-text 对中文语义匹配的距离偏高，0.5 是较合适的起点 */
    private static final double DISTANCE_THRESHOLD = 0.5;

    @Autowired
    private AiProviderManager providerManager;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * 将用户问题向量化，检索最相关字段，构建 Schema 上下文文本
     */
    public String build(String question) {
        // 1. 向量化用户问题
        long t1 = System.currentTimeMillis();
        float[] queryVector = providerManager.getActiveEmbed().embed(question);
        log.info("[Schema] 问题向量化完成 | 耗时={}ms dims={}", System.currentTimeMillis() - t1, queryVector.length);
        String vectorStr = Arrays.toString(queryVector);

        // 2. pgvector 余弦相似度检索 Top-K 字段（过滤低相关结果）
        String sql = """
            SELECT fc.table_name, fc.field_name, fc.display_name, fc.description,
                   tc.display_name AS table_display_name, tc.description AS table_description,
                   (fc.embed_vector <=> :vec::vector) AS distance
            FROM ai_field_catalog fc
            JOIN ai_table_catalog tc ON tc.table_name = fc.table_name AND tc.is_enabled = 1
            WHERE fc.is_enabled = 1
              AND fc.embed_vector IS NOT NULL
              AND (fc.embed_vector <=> :vec::vector) < :threshold
            ORDER BY fc.embed_vector <=> :vec::vector
            LIMIT :topK
            """;
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                sql, Map.of("vec", vectorStr, "topK", TOP_K, "threshold", DISTANCE_THRESHOLD));

        log.info("[Schema] 向量检索完成 | 命中字段数={}", results.size());
        if (results.isEmpty()) {
            // 区分两种情况：向量库为空（数据未同步）vs 问题与数据无关（相似度不足）
            boolean hasVectors = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ai_field_catalog WHERE is_enabled = 1 AND embed_vector IS NOT NULL",
                    Map.of(), Long.class) > 0;
            if (hasVectors) {
                log.warn("[Schema] 向量全部低相关（distance >= {}），问题与数据无关", DISTANCE_THRESHOLD);
            } else {
                log.warn("[Schema] 向量库为空，尚未完成向量同步");
            }
            throw new NotRelatedToDataException(!hasVectors);
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

        log.info("[Schema] 上下文构建完成 | 涉及表={}", byTable.keySet());
        log.debug("[Schema] 上下文内容:\n{}", sb);
        return sb.toString();
    }

}
