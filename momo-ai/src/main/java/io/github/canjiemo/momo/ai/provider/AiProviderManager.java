package io.github.canjiemo.momo.ai.provider;

import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.momo.ai.config.AiSecretCipher;
import io.github.canjiemo.momo.ai.provider.entity.AiProviderConfig;
import io.github.canjiemo.mycommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AiProviderManager extends BaseServiceImpl {

    private final Map<String, IAiChatProvider> chatProviders;
    private final Map<String, IAiEmbeddingProvider> embedProviders;
    private final AiSecretCipher secretCipher;

    private volatile IAiChatProvider activeChat;
    private volatile IAiEmbeddingProvider activeEmbed;

    public AiProviderManager(List<IAiChatProvider> chatList,
                             List<IAiEmbeddingProvider> embedList,
                             AiSecretCipher secretCipher) {
        this.chatProviders = chatList.stream()
                .collect(Collectors.toMap(IAiChatProvider::getProvider, Function.identity()));
        this.embedProviders = embedList.stream()
                .collect(Collectors.toMap(IAiEmbeddingProvider::getProvider, Function.identity()));
        this.secretCipher = secretCipher;
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

    public synchronized void invalidate() {
        this.activeChat = null;
        this.activeEmbed = null;
        log.info("AI Provider 缓存已失效，下次调用重新加载");
    }

    private void refresh() {
        AiProviderConfig config = lambdaQuery(AiProviderConfig.class)
                .eq(AiProviderConfig::getIsActive, 1)
                .one();
        if (config == null) {
            throw new BusinessException("未配置 AI 模型，请先在平台配置中激活一个 AI Provider");
        }

        IAiChatProvider chat = chatProviders.get(config.getProvider());
        IAiEmbeddingProvider embed = embedProviders.get(config.getProvider());
        if (chat == null || embed == null) {
            throw new BusinessException("不支持的 AI Provider: " + config.getProvider());
        }

        // 解密 API Key 后再交给 Provider 初始化（DB 中为密文）
        config.setApiKey(secretCipher.decrypt(config.getApiKey()));
        chat.init(config);
        if (chat != embed) embed.init(config);
        this.activeChat = chat;
        this.activeEmbed = embed;
        log.info("AI Provider 已加载: provider={}, chatModel={}", config.getProvider(), config.getChatModel());
    }
}
