package com.seer.fitness.ai.engine;

import com.seer.fitness.ai.engine.dto.AiQueryRequest;
import com.seer.fitness.ai.engine.dto.AiQueryResponse;
import io.github.canjiemo.mycommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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

    public AiQueryResponse query(AiQueryRequest request) {
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new BusinessException("请输入查询问题");
        }
        if (request.getQuestion().length() > 500) {
            throw new BusinessException("问题描述不能超过 500 个字符");
        }
        String question = request.getQuestion();
        log.info("AI 查询开始: sessionId={}, question={}", request.getSessionId(), question);

        // 1. 向量检索相关 Schema
        String schemaContext = schemaContextBuilder.build(question);

        // 2. LLM 生成 SQL
        String sql = sqlGenerator.generate(schemaContext, question);

        // 3. 安全校验
        sqlValidator.validate(sql);

        // 4. 执行（myjdbc 自动注入租户隔离 + 逻辑删除）
        List<Map<String, Object>> rows = sqlExecutor.execute(sql);

        // 5. 格式化输出
        AiQueryResponse response = resultFormatter.format(question, sql, rows);
        response.setSessionId(request.getSessionId());

        log.info("AI 查询完成: rows={}", rows.size());
        return response;
    }
}
