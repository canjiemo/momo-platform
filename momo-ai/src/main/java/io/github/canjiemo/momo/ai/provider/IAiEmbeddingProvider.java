package io.github.canjiemo.momo.ai.provider;

import io.github.canjiemo.momo.ai.provider.entity.AiProviderConfig;

public interface IAiEmbeddingProvider {
    String getProvider();
    void init(AiProviderConfig config);
    float[] embed(String text);
}
