package com.seer.fitness.ai.conversation.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MyTable("ai_conversation")
public class AiConversation implements MyTableEntity {
    private Long id;
    private Long tenantId;
    private String sessionId;
    private Long userId;
    private String role;           // user / assistant
    private String content;
    private String generatedSql;
    private Integer execRows;
    private LocalDateTime createTime;
}
