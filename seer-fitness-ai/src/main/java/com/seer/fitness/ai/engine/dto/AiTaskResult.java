package com.seer.fitness.ai.engine.dto;

import lombok.Data;

/**
 * AI 异步任务结果，存储于 Redis（key: ai:task:{taskId}，TTL 10分钟）
 */
@Data
public class AiTaskResult {

    public enum Status { PENDING, DONE, FAILED }

    private Status status = Status.PENDING;
    private AiQueryResponse result;
    /** 失败时的错误提示（展示给用户） */
    private String errorMsg;
}
