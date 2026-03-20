package io.github.canjiemo.momo.ai.engine;

import io.github.canjiemo.momo.ai.conversation.entity.AiConversation;
import io.github.canjiemo.momo.ai.engine.dto.AiQueryRequest;
import io.github.canjiemo.momo.ai.engine.dto.AiQueryResponse;
import io.github.canjiemo.mycommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AiQueryEngine {

    @Autowired private SchemaContextBuilder schemaContextBuilder;
    @Autowired private SqlGenerator sqlGenerator;
    @Autowired private SqlValidator sqlValidator;
    @Autowired private SqlExecutor sqlExecutor;
    @Autowired private ResultFormatter resultFormatter;

    public AiQueryResponse query(AiQueryRequest request, List<AiConversation> history) {
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new BusinessException("请输入查询问题");
        }
        if (request.getQuestion().length() > 500) {
            throw new BusinessException("问题描述不能超过 500 个字符");
        }
        String question = request.getQuestion();
        int historyRounds = history != null ? history.size() / 2 : 0;
        log.info("[AI查询] ① 开始 | sessionId={} historyRounds={} question={}",
                request.getSessionId(), historyRounds, question);

        // 1. 向量检索相关 Schema
        long t1 = System.currentTimeMillis();
        String schemaContext;
        try {
            schemaContext = schemaContextBuilder.build(question);
        } catch (NotRelatedToDataException e) {
            log.info("[AI查询] ② 无相关Schema，跳过LLM | 耗时={}ms", System.currentTimeMillis() - t1);
            AiQueryResponse notQuery = new AiQueryResponse();
            notQuery.setSessionId(request.getSessionId());
            notQuery.setSummary(e.isVectorEmpty()
                    ? "暂无可查询的数据，请联系管理员完成数据目录向量同步后再试。"
                    : "未找到与您问题相关的数据，请描述您想查询的内容，例如：\"查询本月新增用户数\" 或 \"统计各租户的用户数量\"。");
            return notQuery;
        }
        log.info("[AI查询] ② Schema检索完成 | 耗时={}ms", System.currentTimeMillis() - t1);

        // 2. LLM 生成 SQL（携带对话历史实现上下文记忆）
        long t2 = System.currentTimeMillis();
        String sql = sqlGenerator.generate(schemaContext, question,
                history != null ? history : Collections.emptyList());
        log.info("[AI查询] ③ SQL生成完成 | 耗时={}ms sql={}", System.currentTimeMillis() - t2, sql);

        // NOT_A_QUERY：与数据查询无关，直接返回友好提示
        if (SqlGenerator.NOT_A_QUERY.equals(sql)) {
            log.info("[AI查询] 非数据查询问题，跳过SQL执行");
            AiQueryResponse notQuery = new AiQueryResponse();
            notQuery.setSessionId(request.getSessionId());
            notQuery.setSummary("我是数据查询助手，只能帮您查询系统数据。请描述您想查询的内容，例如：\"查询本月新增用户数\" 或 \"统计各租户的用户数量\"。");
            return notQuery;
        }

        // 3. 安全校验
        sqlValidator.validate(sql);

        // 4. 执行（myjdbc 自动注入租户隔离 + 逻辑删除）
        long t3 = System.currentTimeMillis();
        List<Map<String, Object>> rows = sqlExecutor.execute(sql);
        log.info("[AI查询] ④ SQL执行完成 | 耗时={}ms rows={}", System.currentTimeMillis() - t3, rows.size());

        // 5. 格式化输出
        long t4 = System.currentTimeMillis();
        AiQueryResponse response = resultFormatter.format(question, sql, rows);
        response.setSessionId(request.getSessionId());
        log.info("[AI查询] ⑤ 摘要生成完成 | 耗时={}ms", System.currentTimeMillis() - t4);

        log.info("[AI查询] ✓ 全流程完成 | 总耗时={}ms rows={}",
                System.currentTimeMillis() - t1, rows.size());
        return response;
    }
}
