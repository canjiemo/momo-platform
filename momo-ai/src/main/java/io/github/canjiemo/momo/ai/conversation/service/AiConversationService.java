package io.github.canjiemo.momo.ai.conversation.service;

import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.momo.ai.conversation.dto.ConversationCursorResult;
import io.github.canjiemo.momo.ai.conversation.entity.AiConversation;
import io.github.canjiemo.momo.framework.dto.UserCacheInfo;
import io.github.canjiemo.momo.framework.utils.SecurityContextUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiConversationService extends BaseServiceImpl implements IAiConversationService {

    @Override
    @Transactional
    public void saveUserMessage(String sessionId, Long userId, Long tenantId, String question) {
        AiConversation conv = new AiConversation();
        conv.setTenantId(tenantId);
        conv.setUserId(userId);
        conv.setSessionId(sessionId);
        conv.setRole("user");
        conv.setContent(question);
        conv.setCreateTime(LocalDateTime.now());
        baseDao.insertPO(conv, true);
    }

    @Override
    @Transactional
    public void saveAssistantMessage(String sessionId, Long userId, Long tenantId,
                                      String summary, String sql, int rows) {
        AiConversation conv = new AiConversation();
        conv.setTenantId(tenantId);
        conv.setUserId(userId);
        conv.setSessionId(sessionId);
        conv.setRole("assistant");
        conv.setContent(summary != null ? summary : "");
        conv.setGeneratedSql(sql);
        conv.setExecRows(rows);
        conv.setCreateTime(LocalDateTime.now());
        baseDao.insertPO(conv, true);
    }

    private static final int DEFAULT_PAGE_SIZE = 20;

    @Override
    public ConversationCursorResult getHistory(String sessionId, Long cursor, int size) {
        if (size <= 0 || size > 100) size = DEFAULT_PAGE_SIZE;

        UserCacheInfo user = SecurityContextUtil.getCurrentUser();
        Long userId   = user != null ? user.getUserId()   : null;
        Long tenantId = user != null ? user.getTenantId() : null;

        // 多查一条，用来判断是否还有更多历史
        Map<String, Object> params = new HashMap<>();
        params.put("sessionId", sessionId);
        params.put("userId", userId);
        params.put("limit", size + 1);

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM ai_conversation WHERE session_id = :sessionId AND user_id = :userId"
        );
        if (tenantId != null) {
            sql.append(" AND tenant_id = :tenantId");
            params.put("tenantId", tenantId);
        } else {
            sql.append(" AND tenant_id IS NULL");
        }
        if (cursor != null) {
            // 取比 cursor 更老的消息
            sql.append(" AND id < :cursor");
            params.put("cursor", cursor);
        }
        sql.append(" ORDER BY id DESC LIMIT :limit");

        List<AiConversation> rows = baseDao.queryListForSql(sql.toString(), params, AiConversation.class);

        boolean hasMore = rows.size() > size;
        if (hasMore) {
            // 去掉多查的那条（最老的那条，仅用于探测）
            rows = rows.subList(0, size);
        }

        // DESC → ASC，前端展示时从旧到新
        Collections.reverse(rows);

        // 下一页游标 = 本页最老那条消息的 id（反转后的第一个元素）
        Long nextCursor = hasMore ? rows.get(0).getId() : null;

        return new ConversationCursorResult(rows, nextCursor, hasMore);
    }

    @Override
    public List<AiConversation> getRecentHistory(String sessionId, int rounds) {
        UserCacheInfo user = SecurityContextUtil.getCurrentUser();
        Long userId   = user != null ? user.getUserId()   : null;
        Long tenantId = user != null ? user.getTenantId() : null;

        // 只取有 SQL 的轮次（每轮 user + assistant 各 1 条），直接 LIMIT，不再全量加载
        int limit = rounds * 2;
        Map<String, Object> params = new HashMap<>();
        params.put("sessionId", sessionId);
        params.put("userId", userId);
        params.put("limit", limit);

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM ai_conversation"
                + " WHERE session_id = :sessionId AND user_id = :userId AND generated_sql IS NOT NULL"
        );
        if (tenantId != null) {
            sql.append(" AND tenant_id = :tenantId");
            params.put("tenantId", tenantId);
        } else {
            sql.append(" AND tenant_id IS NULL");
        }
        sql.append(" ORDER BY id DESC LIMIT :limit");

        List<AiConversation> rows = baseDao.queryListForSql(sql.toString(), params, AiConversation.class);
        // DESC → ASC，LLM 需要按时间顺序理解上下文
        Collections.reverse(rows);
        return rows;
    }
}
