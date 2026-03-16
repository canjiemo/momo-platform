package com.seer.fitness.ai.provider;

import com.seer.fitness.ai.provider.entity.AiProviderConfig;

public interface IAiChatProvider {
    String getProvider();
    void init(AiProviderConfig config);
    String chat(String systemPrompt, String userMessage);
}
