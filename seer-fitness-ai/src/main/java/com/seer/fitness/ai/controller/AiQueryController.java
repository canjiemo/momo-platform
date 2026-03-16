package com.seer.fitness.ai.controller;

import com.seer.fitness.ai.conversation.entity.AiConversation;
import com.seer.fitness.ai.conversation.service.IAiConversationService;
import com.seer.fitness.ai.engine.AiQueryEngine;
import com.seer.fitness.ai.engine.dto.AiQueryRequest;
import com.seer.fitness.ai.engine.dto.AiQueryResponse;
import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/system/ai")
public class AiQueryController extends MyBaseController {

    @Autowired private AiQueryEngine queryEngine;
    @Autowired private IAiConversationService conversationService;

    @PostMapping("/query")
    public MyResponseResult<AiQueryResponse> query(@RequestBody AiQueryRequest request) {
        // 保存用户问题（MVP 阶段 userId 传 null）
        conversationService.saveUserMessage(request.getSessionId(), null, request.getQuestion());

        AiQueryResponse response = queryEngine.query(request);

        // 保存 AI 回答
        int rowCount = response.getTable() != null && response.getTable().getRows() != null
                ? response.getTable().getRows().size() : 0;
        conversationService.saveAssistantMessage(
                request.getSessionId(), response.getSummary(),
                response.getGeneratedSql(), rowCount);

        return doJsonOut(response);
    }

    @GetMapping("/conversation/{sessionId}")
    public MyResponseResult<List<AiConversation>> history(@PathVariable String sessionId) {
        return doJsonOut(conversationService.getHistory(sessionId));
    }
}
