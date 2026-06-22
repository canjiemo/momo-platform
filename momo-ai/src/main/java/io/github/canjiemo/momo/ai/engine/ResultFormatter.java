package io.github.canjiemo.momo.ai.engine;

import io.github.canjiemo.momo.ai.engine.dto.AiQueryResponse;
import io.github.canjiemo.momo.ai.provider.AiProviderManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 结果格式化：
 *   - 0 条结果 → 规则文本，跳过 LLM
 *   - 有结果    → LLM 润色摘要 + 规则推断图表类型
 *
 * 图表推断规则（基于列名关键字 + 数据特征）：
 *   - 含时间列（date/time/month/year/日期/月/年）+ 数值列 → line
 *   - 含分组列（name/type/status/角色/用户/租户 等）+ 数值列 → bar
 *   - 仅 2 列且第二列为数值、行数 ≤ 10 → pie（占比场景）
 *   - 其余 → none
 */
@Slf4j
@Component
public class ResultFormatter {

    private static final String SUMMARY_PROMPT = """
            根据用户的问题和查询结果，用简洁自然的中文描述查询结论（2-3句话），突出关键数据和结论，不要重复问题本身。
            """;

    @Autowired
    private AiProviderManager providerManager;

    private static final Set<String> TIME_KEYWORDS =
            Set.of("date", "time", "month", "year", "day", "日期", "月", "年", "日", "时间");
    private static final Set<String> GROUP_KEYWORDS =
            Set.of("name", "type", "status", "role", "user", "tenant",
                    "名称", "类型", "状态", "角色", "用户", "租户", "分类");

    public AiQueryResponse format(String question, String sql, List<Map<String, Object>> rows) {
        AiQueryResponse response = new AiQueryResponse();
        response.setGeneratedSql(sql);

        if (rows.isEmpty()) {
            response.setSummary("未查到符合条件的数据。");
            log.info("[Formatter] 空结果，跳过图表");
            return response;
        }

        // 构建表格数据
        List<String> columns = new ArrayList<>(rows.get(0).keySet());
        List<List<Object>> tableRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<Object> tableRow = new ArrayList<>();
            columns.forEach(col -> tableRow.add(row.get(col)));
            tableRows.add(tableRow);
        }
        AiQueryResponse.TableData tableData = new AiQueryResponse.TableData();
        tableData.setColumns(columns);
        tableData.setRows(tableRows);
        // 命中行数上限，结果可能被截断，提示前端
        tableData.setTruncated(rows.size() >= SqlExecutor.MAX_ROWS);
        response.setTable(tableData);

        // 有结果：LLM 润色摘要
        response.setSummary(generateSummary(question, rows));

        // 规则推断图表类型
        String chartType = inferChartType(columns, rows);
        log.info("[Formatter] 摘要={} chartType={}", response.getSummary(), chartType);

        if (!"none".equals(chartType)) {
            AiQueryResponse.ChartConfig chart = new AiQueryResponse.ChartConfig();
            chart.setType(chartType);
            chart.setXField(columns.get(0));
            chart.setYField(columns.size() > 1 ? columns.get(1) : columns.get(0));
            response.setChart(chart);
            log.info("[Formatter] 图表配置: type={} xField={} yField={}",
                    chartType, chart.getXField(), chart.getYField());
        }

        return response;
    }

    private String generateSummary(String question, List<Map<String, Object>> rows) {
        String preview = rows.size() > 5
                ? rows.subList(0, 5).toString() + "...（共 " + rows.size() + " 条）"
                : rows.toString();
        String userMsg = "问题: " + question + "\n查询结果: " + preview;
        try {
            String summary = providerManager.getActiveChat().chat(SUMMARY_PROMPT, userMsg);
            log.info("[Formatter] LLM摘要生成完成");
            return summary;
        } catch (Exception e) {
            log.warn("[Formatter] LLM摘要生成失败，降级规则摘要: {}", e.getMessage());
            return "共查询到 " + rows.size() + " 条数据。";
        }
    }

    private String inferChartType(List<String> columns, List<Map<String, Object>> rows) {
        if (columns.size() < 2) return "none";

        String firstCol = columns.get(0).toLowerCase();
        boolean firstIsTime = TIME_KEYWORDS.stream().anyMatch(firstCol::contains);
        boolean firstIsGroup = GROUP_KEYWORDS.stream().anyMatch(firstCol::contains);
        boolean firstIsNumeric = isNumericColumn(rows, columns.get(0));
        boolean secondIsNumeric = isNumericColumn(rows, columns.get(1));

        // 次列非数值，无法绘制数值图形
        if (!secondIsNumeric) return "none";

        if (firstIsTime) return "line";                              // 时间序列 → 折线
        if (firstIsGroup) return "bar";                              // 命中分组关键字 → 柱状
        if (columns.size() == 2 && rows.size() <= 10) return "pie";  // 两列小数据集 → 占比饼图
        // 兜底：类别型（非数值）首列 + 数值次列 → 柱状图
        // （此前关键字未命中时会误判为 none，导致"各部门人数"等明显可绘图的结果不出图）
        if (!firstIsNumeric) return "bar";

        return "none";                                               // 数值-数值等无明确图形语义
    }

    private boolean isNumericColumn(List<Map<String, Object>> rows, String col) {
        return rows.stream()
                .map(r -> r.get(col))
                .filter(Objects::nonNull)
                .findFirst()
                .map(v -> v instanceof Number)
                .orElse(false);
    }
}
