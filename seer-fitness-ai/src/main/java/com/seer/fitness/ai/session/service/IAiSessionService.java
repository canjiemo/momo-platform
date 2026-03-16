package com.seer.fitness.ai.session.service;

import com.seer.fitness.ai.session.dto.AiSessionDTO;

import java.util.List;

public interface IAiSessionService {
    /** 新建会话，返回 sessionId */
    AiSessionDTO create(Long userId, Long tenantId);

    /** 当前用户的会话列表，按最近更新倒序 */
    List<AiSessionDTO> listByUser(Long userId, Long tenantId);

    /** 重命名 */
    void rename(String sessionId, Long userId, String title);

    /** 删除会话及其所有消息 */
    void delete(String sessionId, Long userId);

    /** 首条提问时自动设置标题（取前30字） */
    void autoTitle(String sessionId, Long userId, String question);
}
