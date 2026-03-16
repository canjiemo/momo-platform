package com.seer.fitness.ai.engine;

import com.seer.fitness.ai.engine.dto.AiQueryResponse;
import com.seer.fitness.ai.provider.AiProviderManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;

@Slf4j
@Component
public class ResultFormatter {

    private static final String SUMMARY_PROMPT = """
            根据用户的问题和查询结果，生成简洁的中文摘要（2-3句话），
            并在最后一行输出推荐的图表类型（只输出一个单词: bar/line/pie/none）。
            格式：
            [摘要内容]
            CHART_TYPE: [bar|line|pie|none]
            """;

    @Autowired
    private AiProviderManager providerManager;

    public AiQueryResponse format(String question, String sql,
                                   List<Map<String, Object>> rows) {
        AiQueryResponse response = new AiQueryResponse();
        response.setGeneratedSql(sql);

        // 构建表格数据
        if (!rows.isEmpty()) {
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
            response.setTable(tableData);
        }

        // 生成摘要 + 图表类型
        String resultPreview = rows.size() > 5
                ? rows.subList(0, 5).toString() + "...（共 " + rows.size() + " 条）"
                : rows.toString();
        String userMsg = "问题: " + question + "\n查询结果: " + resultPreview;
        String aiResponse = providerManager.getActiveChat().chat(SUMMARY_PROMPT, userMsg);

        // 解析摘要和图表类型
        String summary = aiResponse;
        String chartType = "none";
        if (aiResponse.contains("CHART_TYPE:")) {
            int idx = aiResponse.lastIndexOf("CHART_TYPE:");
            summary = aiResponse.substring(0, idx).trim();
            chartType = aiResponse.substring(idx + "CHART_TYPE:".length()).trim().toLowerCase();
        }
        response.setSummary(summary);

        // 图表配置（简单推断 X/Y 轴）
        if (!"none".equals(chartType) && response.getTable() != null) {
            List<String> cols = response.getTable().getColumns();
            AiQueryResponse.ChartConfig chart = new AiQueryResponse.ChartConfig();
            chart.setType(chartType);
            chart.setXField(cols.size() > 0 ? cols.get(0) : "");
            chart.setYField(cols.size() > 1 ? cols.get(1) : "");
            response.setChart(chart);
        }

        return response;
    }
}
