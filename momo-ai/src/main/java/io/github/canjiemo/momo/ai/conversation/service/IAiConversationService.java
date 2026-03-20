package io.github.canjiemo.momo.ai.conversation.service;

import io.github.canjiemo.momo.ai.conversation.dto.ConversationCursorResult;
import io.github.canjiemo.momo.ai.conversation.entity.AiConversation;

import java.util.List;

public interface IAiConversationService {
    void saveUserMessage(String sessionId, Long userId, Long tenantId, String question);
    void saveAssistantMessage(String sessionId, Long userId, Long tenantId, String summary, String sql, int rows);

    /**
     * 游标分页查询对话历史，结果按时间升序返回（旧→新）。
     *
     * @param sessionId 会话 ID
     * @param cursor    上一页最老消息的 id，null 表示首次加载（取最新一页）
     * @param size      每页条数
     */
    ConversationCursorResult getHistory(String sessionId, Long cursor, int size);

    /** 获取最近 N 轮对话（每轮含 user + assistant 共 2 条），按时间升序返回 */
    List<AiConversation> getRecentHistory(String sessionId, int rounds);
}
