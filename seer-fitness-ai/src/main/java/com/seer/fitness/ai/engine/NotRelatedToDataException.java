package com.seer.fitness.ai.engine;

import io.github.canjiemo.mycommon.exception.BaseException;

/** 用户问题与数据目录无关（向量相似度全部低于阈值或向量库为空），跳过 LLM 直接返回引导提示 */
public class NotRelatedToDataException extends BaseException {
    private final boolean vectorEmpty;

    public NotRelatedToDataException(boolean vectorEmpty) {
        super(0, vectorEmpty ? "向量库为空" : "问题与数据无关");
        this.vectorEmpty = vectorEmpty;
    }

    public boolean isVectorEmpty() {
        return vectorEmpty;
    }
}
