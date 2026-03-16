package com.seer.fitness.ai.session.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiSessionDTO {
    private Long id;
    private String sessionId;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
