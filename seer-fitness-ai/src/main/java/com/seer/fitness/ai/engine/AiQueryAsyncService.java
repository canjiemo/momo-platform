package com.seer.fitness.ai.engine;

import com.seer.fitness.ai.conversation.entity.AiConversation;
import com.seer.fitness.ai.conversation.service.IAiConversationService;
import com.seer.fitness.ai.engine.dto.AiQueryRequest;
import com.seer.fitness.ai.engine.dto.AiQueryResponse;
import com.seer.fitness.ai.engine.dto.AiTaskResult;
import com.seer.fitness.framework.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AiQueryAsyncService {

    private static final String TASK_KEY_PREFIX = "ai:task:";
    private static final long TASK_TTL_MINUTES = 10;

    @Autowired private AiQueryEngine queryEngine;
    @Autowired private IAiConversationService conversationService;
    @Autowired private com.seer.fitness.ai.session.service.IAiSessionService sessionService;
    @Autowired private RedisUtil redisUtil;

    /**
     * 初始化任务状态为 PENDING，供 Controller 在提交前调用（同步，确保轮询时 key 已存在）
     */
    public void initTask(String taskId) {
        AiTaskResult pending = new AiTaskResult();
        pending.setStatus(AiTaskResult.Status.PENDING);
        redisUtil.set(TASK_KEY_PREFIX + taskId, pending, TASK_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 异步执行 AI 查询，完成后将结果写入 Redis
     */
    @Async("aiQueryExecutor")
    public void executeAsync(String taskId, AiQueryRequest request, List<AiConversation> history) {
        AiTaskResult taskResult = new AiTaskResult();
        try {
            AiQueryResponse response = queryEngine.query(request, history);

            // 所有对话均记录历史（含 NOT_A_QUERY 的提示性回复）
            int rowCount = response.getTable() != null && response.getTable().getRows() != null
                    ? response.getTable().getRows().size() : 0;
            conversationService.saveUserMessage(request.getSessionId(),
                    request.getUserId(), request.getTenantId(), request.getQuestion());
            conversationService.saveAssistantMessage(request.getSessionId(),
                    request.getUserId(), request.getTenantId(),
                    response.getSummary(), response.getGeneratedSql(), rowCount);
            // 首条问题自动设置会话标题
            sessionService.autoTitle(request.getSessionId(),
                    request.getUserId(), request.getQuestion());

            taskResult.setStatus(AiTaskResult.Status.DONE);
            taskResult.setResult(response);
        } catch (Exception e) {
            log.error("[AI异步任务] 执行失败 taskId={}", taskId, e);
            taskResult.setStatus(AiTaskResult.Status.FAILED);
            taskResult.setErrorMsg("查询执行失败，请稍后重试");
        } finally {
            redisUtil.set(TASK_KEY_PREFIX + taskId, taskResult, TASK_TTL_MINUTES, TimeUnit.MINUTES);
        }
    }

    /**
     * 轮询任务结果
     */
    public AiTaskResult getTaskResult(String taskId) {
        return redisUtil.get(TASK_KEY_PREFIX + taskId, AiTaskResult.class);
    }
}
