package com.seer.fitness.ai.conversation.service;

import com.seer.fitness.ai.conversation.entity.AiConversation;
import com.seer.fitness.framework.dto.UserCacheInfo;
import com.seer.fitness.framework.utils.SecurityContextUtil;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    @Override
    public List<AiConversation> getHistory(String sessionId) {
        UserCacheInfo user = SecurityContextUtil.getCurrentUser();
        Long userId = user != null ? user.getUserId() : null;
        return lambdaQuery(AiConversation.class)
                .eq(AiConversation::getSessionId, sessionId)
                .eq(AiConversation::getUserId, userId)
                .orderByAsc(AiConversation::getCreateTime)
                .list();
    }

    @Override
    public List<AiConversation> getRecentHistory(String sessionId, int rounds) {
        UserCacheInfo user = SecurityContextUtil.getCurrentUser();
        Long userId = user != null ? user.getUserId() : null;
        // lambdaQuery 自动注入 tenant_id 条件
        // 只取有 SQL 的轮次注入 LLM 上下文，NOT_A_QUERY 的拒绝回复不传给模型
        List<AiConversation> all = lambdaQuery(AiConversation.class)
                .eq(AiConversation::getSessionId, sessionId)
                .eq(AiConversation::getUserId, userId)
                .isNotNull(AiConversation::getGeneratedSql)
                .orderByAsc(AiConversation::getCreateTime)
                .list();
        int limit = rounds * 2;
        if (all.size() <= limit) return all;
        return all.subList(all.size() - limit, all.size());
    }
}
