package io.github.canjiemo.momo.ai.session.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MyTable("ai_session")
public class AiSession implements MyTableEntity {
    private Long id;
    private String sessionId;
    private Long tenantId;
    private Long userId;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleteFlag;
}
