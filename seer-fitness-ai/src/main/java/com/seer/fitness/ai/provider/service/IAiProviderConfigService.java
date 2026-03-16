package com.seer.fitness.ai.provider.service;

import com.seer.fitness.ai.provider.entity.AiProviderConfig;

import java.util.List;

public interface IAiProviderConfigService {
    List<AiProviderConfig> list();
    void create(AiProviderConfig config);
    void update(AiProviderConfig request);
    void activate(Long id);
    void delete(Long id);
}
