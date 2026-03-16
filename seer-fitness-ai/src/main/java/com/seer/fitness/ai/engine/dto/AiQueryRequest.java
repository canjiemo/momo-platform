package com.seer.fitness.ai.engine.dto;

import lombok.Data;

@Data
public class AiQueryRequest {
    private String sessionId;
    private String question;
}
