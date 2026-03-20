package io.github.canjiemo.momo.ai.provider;

import io.github.canjiemo.momo.ai.provider.entity.AiProviderConfig;

public interface IAiChatProvider {
    String getProvider();
    void init(AiProviderConfig config);
    String chat(String systemPrompt, String userMessage);
}
