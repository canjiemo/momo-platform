package com.seer.fitness.ai.conversation.service;

import com.seer.fitness.ai.conversation.entity.AiConversation;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiConversationService extends BaseServiceImpl implements IAiConversationService {

    @Override
    @Transactional
    public void saveUserMessage(String sessionId, Long userId, String question) {
        AiConversation conv = new AiConversation();
        conv.setSessionId(sessionId);
        conv.setUserId(userId);
        conv.setRole("user");
        conv.setContent(question);
        conv.setCreateTime(LocalDateTime.now());
        baseDao.insertPO(conv, true);
    }

    @Override
    @Transactional
    public void saveAssistantMessage(String sessionId, String summary,
                                      String sql, int rows) {
        AiConversation conv = new AiConversation();
        conv.setSessionId(sessionId);
        conv.setRole("assistant");
        conv.setContent(summary != null ? summary : "");
        conv.setGeneratedSql(sql);
        conv.setExecRows(rows);
        conv.setCreateTime(LocalDateTime.now());
        baseDao.insertPO(conv, true);
    }

    @Override
    public List<AiConversation> getHistory(String sessionId) {
        return lambdaQuery(AiConversation.class)
                .eq(AiConversation::getSessionId, sessionId)
                .orderByAsc(AiConversation::getCreateTime)
                .list();
    }
}
