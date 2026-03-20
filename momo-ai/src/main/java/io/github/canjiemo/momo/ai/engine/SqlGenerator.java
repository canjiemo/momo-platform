package io.github.canjiemo.momo.ai.engine;

import io.github.canjiemo.momo.ai.conversation.entity.AiConversation;
import io.github.canjiemo.momo.ai.provider.AiProviderManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SqlGenerator {

    static final String NOT_A_QUERY = "NOT_A_QUERY";

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
            6. 若用户问题与数据查询无关（如打招呼、闲聊、无法对应到上述表结构），直接输出 NOT_A_QUERY，不要输出任何其他内容
            9. 若用户问题是对上一轮的追问（如"那他叫什么名字""是什么角色""有几个"等缺少主语的短句），必须先结合【对话历史】中的上下文推断完整意图，再生成对应 SQL；只有确实无法关联到任何数据查询时才输出 NOT_A_QUERY
            7. 禁止在同一 SELECT 中混用聚合函数（COUNT/SUM/AVG 等）和非聚合列（除非有 GROUP BY）；若用户同时想要"数量"和"明细列表"，优先返回明细列表（行数即数量），不要写无效的聚合+明细混合语句
            8. 需要同时展示总数和明细时，使用窗口函数：COUNT(*) OVER() AS total
            """;

    @Autowired
    private AiProviderManager providerManager;

    public String generate(String schemaContext, String question, List<AiConversation> history) {
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(schemaContext);
        if (history != null && !history.isEmpty()) {
            StringBuilder historyText = new StringBuilder("\n【对话历史】（供理解上下文，无需重复回答）\n");
            for (AiConversation msg : history) {
                if ("user".equals(msg.getRole())) {
                    historyText.append("[用户] ").append(msg.getContent()).append("\n");
                } else {
                    historyText.append("[助手] ").append(msg.getContent());
                    if (msg.getGeneratedSql() != null) {
                        historyText.append("（上轮SQL: ").append(msg.getGeneratedSql()).append("）");
                    }
                    if (msg.getExecRows() != null) {
                        historyText.append("（返回").append(msg.getExecRows()).append("行）");
                    }
                    historyText.append("\n");
                }
            }
            systemPrompt = systemPrompt + historyText;
            log.info("[SqlGen] 携带历史轮数={}", history.size() / 2);
        }
        log.debug("[SqlGen] System Prompt:\n{}", systemPrompt);
        log.debug("[SqlGen] User Question: {}", question);
        String sql = providerManager.getActiveChat().chat(systemPrompt, question);
        // 清理模型可能多输出的 markdown 标记
        sql = sql.replaceAll("```sql", "").replaceAll("```", "").trim();
        log.info("[SqlGen] 生成SQL: {}", sql);
        return sql;
    }
}
