package com.seer.fitness.ai.engine;

import com.seer.fitness.ai.provider.AiProviderManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SqlGenerator {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是一个 PostgreSQL 数据库查询助手。根据用户的问题，生成对应的 SELECT 查询语句。

            【可查询的数据库结构】
            %s

            【规则】
            1. 只生成 SELECT 语句，严禁 INSERT/UPDATE/DELETE/DROP/CREATE
            2. 只查询上述表，不得查询其他表
            3. 统计类查询使用 COUNT/SUM/AVG 等聚合函数
            4. 时间相关查询使用 PostgreSQL 日期函数（NOW(), DATE_TRUNC 等）
            5. 直接输出 SQL 语句，不要 Markdown 代码块，不要任何解释文字
            """;

    @Autowired
    private AiProviderManager providerManager;

    public String generate(String schemaContext, String question) {
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(schemaContext);
        String sql = providerManager.getActiveChat().chat(systemPrompt, question);
        // 清理模型可能多输出的 markdown 标记
        sql = sql.replaceAll("```sql", "").replaceAll("```", "").trim();
        log.info("AI 生成 SQL: {}", sql);
        return sql;
    }
}
