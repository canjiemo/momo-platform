package com.seer.fitness.ai.conversation.service;

import com.seer.fitness.ai.conversation.entity.AiConversation;
import java.util.List;

public interface IAiConversationService {
    void saveUserMessage(String sessionId, Long userId, String question);
    void saveAssistantMessage(String sessionId, String summary, String sql, int rows);
    List<AiConversation> getHistory(String sessionId);
}
