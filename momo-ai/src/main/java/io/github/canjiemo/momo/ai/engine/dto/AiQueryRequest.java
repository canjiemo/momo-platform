package io.github.canjiemo.momo.ai.engine.dto;

import lombok.Data;

@Data
public class AiQueryRequest {
    private String sessionId;
    private String question;
    // 由 Controller 在 HTTP 线程填充，异步线程不可直接调用 SecurityContextUtil
    private Long userId;
    private Long tenantId;
}
