package com.seer.fitness.ai.provider;

import com.seer.fitness.ai.provider.entity.AiProviderConfig;

public interface IAiEmbeddingProvider {
    String getProvider();
    void init(AiProviderConfig config);
    float[] embed(String text);
}
