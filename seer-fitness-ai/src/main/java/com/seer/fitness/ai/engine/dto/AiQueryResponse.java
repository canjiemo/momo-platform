package com.seer.fitness.ai.engine.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

@Data
public class AiQueryResponse {
    private String sessionId;
    private String summary;           // 文字摘要
    @JsonIgnore
    private String generatedSql;      // 生成的 SQL（仅内部使用，不返回前端）
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
