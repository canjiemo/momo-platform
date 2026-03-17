package com.seer.fitness.ai.controller;

import com.seer.fitness.ai.conversation.dto.ConversationCursorResult;
import com.seer.fitness.ai.conversation.entity.AiConversation;
import com.seer.fitness.ai.conversation.service.IAiConversationService;
import com.seer.fitness.ai.engine.AiQueryAsyncService;
import com.seer.fitness.ai.engine.dto.AiQueryRequest;
import com.seer.fitness.ai.engine.dto.AiTaskResult;
import com.seer.fitness.framework.annotation.RequireAuth;
import com.seer.fitness.framework.dto.UserCacheInfo;
import com.seer.fitness.framework.utils.SecurityContextUtil;
import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.mycommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/system/ai")
@RequireAuth(login = true)
public class AiQueryController extends MyBaseController {

    @Autowired private AiQueryAsyncService asyncService;
    @Autowired private IAiConversationService conversationService;

    /**
     * 提交 AI 查询任务，立即返回 taskId，前端凭 taskId 轮询结果
     */
    @PostMapping("/query")
    public MyResponseResult<Map<String, String>> query(@RequestBody AiQueryRequest request) {
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            throw new BusinessException("sessionId 不能为空");
        }
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new BusinessException("请输入查询问题");
        }
        if (request.getQuestion().length() > 500) {
            throw new BusinessException("问题描述不能超过 500 个字符");
        }

        // 在 HTTP 线程取用户信息和历史（异步线程中 SecurityContextUtil 不可用）
        // userId/tenantId 由 SecurityContextUtil 强制覆盖，不信任客户端传值
        UserCacheInfo user = SecurityContextUtil.getCurrentUser();
        Long userId = user != null ? user.getUserId() : null;
        request.setUserId(userId);
        request.setTenantId(user != null ? user.getTenantId() : null);
        List<AiConversation> history = conversationService.getRecentHistory(request.getSessionId(), 5);

        String taskId = UUID.randomUUID().toString().replace("-", "");
        asyncService.initTask(taskId, userId);
        asyncService.executeAsync(taskId, request, history);

        log.info("[AI查询] 任务已提交 taskId={} sessionId={}", taskId, request.getSessionId());
        return doJsonOut(Map.of("taskId", taskId));
    }

    /**
     * 轮询任务结果
     * 返回：{ status: "PENDING" } 或 { status: "DONE", result: {...} } 或 { status: "FAILED", errorMsg: "..." }
     */
    @GetMapping("/query/result/{taskId}")
    public MyResponseResult<AiTaskResult> result(@PathVariable String taskId) {
        AiTaskResult taskResult = asyncService.getTaskResult(taskId);
        if (taskResult == null) {
            throw new BusinessException("任务不存在或已过期（10分钟内有效）");
        }
        // 验证任务归属，防止跨用户查询他人任务结果
        UserCacheInfo user = SecurityContextUtil.getCurrentUser();
        Long currentUserId = user != null ? user.getUserId() : null;
        if (taskResult.getOwnerUserId() != null && !taskResult.getOwnerUserId().equals(currentUserId)) {
            throw new BusinessException("无权访问该任务");
        }
        return doJsonOut(taskResult);
    }

    /**
     * 游标分页查询对话历史
     *
     * @param sessionId 会话 ID
     * @param cursor    上一页最老消息的 id，首次加载不传
     * @param size      每页条数，默认 20，最大 100
     */
    @GetMapping("/conversation/{sessionId}")
    public MyResponseResult<ConversationCursorResult> history(
            @PathVariable String sessionId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return doJsonOut(conversationService.getHistory(sessionId, cursor, size));
    }
}
