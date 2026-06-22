package io.github.canjiemo.momo.ai.session.service;

import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.momo.ai.conversation.entity.AiConversation;
import io.github.canjiemo.momo.ai.session.dto.AiSessionDTO;
import io.github.canjiemo.momo.ai.session.entity.AiSession;
import io.github.canjiemo.mycommon.exception.BusinessException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AiSessionService extends BaseServiceImpl implements IAiSessionService {

    @Override
    @Transactional
    public AiSessionDTO create(Long userId, Long tenantId) {
        AiSession session = new AiSession();
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(userId);
        session.setTenantId(tenantId);
        session.setTitle("新对话");
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        session.setDeleteFlag(0);
        baseDao.insertPO(session, true);

        AiSessionDTO dto = new AiSessionDTO();
        BeanUtils.copyProperties(session, dto);
        return dto;
    }

    @Override
    public List<AiSessionDTO> listByUser(Long userId, Long tenantId) {
        // 租户隔离：补充 tenant_id 过滤，避免不同租户下相同 user_id 串数据
        var query = lambdaQuery(AiSession.class, AiSessionDTO.class)
                .eq(AiSession::getUserId, userId);
        query = tenantId != null
                ? query.eq(AiSession::getTenantId, tenantId)
                : query.isNull(AiSession::getTenantId);
        return query.orderByDesc(AiSession::getUpdateTime).list();
    }

    @Override
    @Transactional
    public void rename(String sessionId, Long userId, String title) {
        AiSession session = getOwnedSession(sessionId, userId);
        session.setTitle(title.length() > 200 ? title.substring(0, 200) : title);
        session.setUpdateTime(LocalDateTime.now());
        baseDao.updatePO(session, true);
    }

    @Override
    @Transactional
    public void delete(String sessionId, Long userId) {
        AiSession session = getOwnedSession(sessionId, userId);
        // 物理删除该会话所有消息（ai_conversation 无 delete_flag）
        // @Transactional 保证原子性：任一步骤失败时整个事务回滚，不会出现消息半删状态
        List<AiConversation> messages = lambdaQuery(AiConversation.class)
                .eq(AiConversation::getSessionId, sessionId)
                .list();
        for (AiConversation msg : messages) {
            baseDao.delPO(msg);
        }
        baseDao.delPO(session);
    }

    @Override
    @Transactional
    public void autoTitle(String sessionId, Long userId, String question) {
        AiSession session = lambdaQuery(AiSession.class)
                .eq(AiSession::getSessionId, sessionId)
                .eq(AiSession::getUserId, userId)
                .one();
        if (session == null || !"新对话".equals(session.getTitle())) return;
        String title = question.length() > 30 ? question.substring(0, 30) + "…" : question;
        session.setTitle(title);
        session.setUpdateTime(LocalDateTime.now());
        baseDao.updatePO(session, true);
    }

    private AiSession getOwnedSession(String sessionId, Long userId) {
        AiSession session = lambdaQuery(AiSession.class)
                .eq(AiSession::getSessionId, sessionId)
                .eq(AiSession::getUserId, userId)
                .one();
        if (session == null) throw new BusinessException("会话不存在");
        return session;
    }
}
