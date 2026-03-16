package com.seer.fitness.ai.conversation.service;

import com.seer.fitness.ai.conversation.entity.AiConversation;

import java.util.List;

public interface IAiConversationService {
    void saveUserMessage(String sessionId, Long userId, Long tenantId, String question);
    void saveAssistantMessage(String sessionId, Long userId, Long tenantId, String summary, String sql, int rows);
    List<AiConversation> getHistory(String sessionId);
    /** 获取最近 N 轮对话（每轮含 user + assistant 共 2 条），按时间升序返回 */
    List<AiConversation> getRecentHistory(String sessionId, int rounds);
}
