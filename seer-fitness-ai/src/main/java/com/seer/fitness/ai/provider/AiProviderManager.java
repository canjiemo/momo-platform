package com.seer.fitness.ai.provider;

import com.seer.fitness.ai.provider.entity.AiProviderConfig;
import io.github.canjiemo.base.myjdbc.dao.IBaseDao;
import io.github.canjiemo.mycommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AiProviderManager {

    private final Map<String, IAiChatProvider> chatProviders;
    private final Map<String, IAiEmbeddingProvider> embedProviders;

    @Autowired
    private IBaseDao baseDao;

    private volatile IAiChatProvider activeChat;
    private volatile IAiEmbeddingProvider activeEmbed;

    public AiProviderManager(List<IAiChatProvider> chatList,
                             List<IAiEmbeddingProvider> embedList) {
        this.chatProviders = chatList.stream()
                .collect(Collectors.toMap(IAiChatProvider::getProvider, Function.identity()));
        this.embedProviders = embedList.stream()
                .collect(Collectors.toMap(IAiEmbeddingProvider::getProvider, Function.identity()));
    }

    public IAiChatProvider getActiveChat() {
        if (activeChat == null) {
            synchronized (this) {
                if (activeChat == null) refresh();
            }
        }
        return activeChat;
    }

    public IAiEmbeddingProvider getActiveEmbed() {
        if (activeEmbed == null) {
            synchronized (this) {
                if (activeEmbed == null) refresh();
            }
        }
        return activeEmbed;
    }

    public void invalidate() {
        this.activeChat = null;
        this.activeEmbed = null;
        log.info("AI Provider 缓存已失效，下次调用重新加载");
    }

    private void refresh() {
        AiProviderConfig config = baseDao.querySingleForSql(
                "SELECT * FROM ai_provider_config WHERE is_active = 1 AND delete_flag = 0 LIMIT 1",
                java.util.Collections.emptyMap(), AiProviderConfig.class);
        if (config == null) {
            throw new BusinessException("未配置 AI 模型，请先在平台配置中激活一个 AI Provider");
        }

        IAiChatProvider chat = chatProviders.get(config.getProvider());
        IAiEmbeddingProvider embed = embedProviders.get(config.getProvider());
        if (chat == null || embed == null) {
            throw new BusinessException("不支持的 AI Provider: " + config.getProvider());
        }

        chat.init(config);
        embed.init(config);
        this.activeChat = chat;
        this.activeEmbed = embed;
        log.info("AI Provider 已加载: provider={}, chatModel={}", config.getProvider(), config.getChatModel());
    }
}
