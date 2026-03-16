package com.seer.fitness.ai.engine.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AiQueryResponse {
    private String sessionId;
    private String summary;           // 文字摘要
    private String generatedSql;      // 生成的 SQL
    private TableData table;          // 表格数据
    private ChartConfig chart;        // 图表配置

    @Data
    public static class TableData {
        private List<String> columns;
        private List<List<Object>> rows;
    }

    @Data
    public static class ChartConfig {
        private String type;           // bar / line / pie
        private String xField;
        private String yField;
    }
}
