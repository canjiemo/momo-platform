package io.github.canjiemo.momo.ai.provider.impl;

import io.github.canjiemo.momo.ai.provider.IAiChatProvider;
import io.github.canjiemo.momo.ai.provider.IAiEmbeddingProvider;
import io.github.canjiemo.momo.ai.provider.entity.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component("ollama")
public class OllamaAiProvider implements IAiChatProvider, IAiEmbeddingProvider {

    /** 连接 / 读超时，防止本地 Ollama 无响应导致请求线程长期挂起 */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    private volatile OllamaChatModel chatModel;
    private volatile OllamaEmbeddingModel embeddingModel;

    @Override
    public String getProvider() {
        return "ollama";
    }

    @Override
    public void init(AiProviderConfig config) {
        OllamaApi api = OllamaApi.builder()
                .baseUrl(config.getBaseUrl())
                .restClientBuilder(timeoutRestClientBuilder())
                .build();
        this.chatModel = OllamaChatModel.builder()
                .ollamaApi(api)
                .options(OllamaChatOptions.builder().model(config.getChatModel()).build())
                .build();
        this.embeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(api)
                .options(OllamaEmbeddingOptions.builder().model(config.getEmbedModel()).build())
                .build();
        log.info("Ollama Provider 初始化完成: baseUrl={}, chatModel={}, embedModel={}",
                config.getBaseUrl(), config.getChatModel(), config.getEmbedModel());
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userMessage)
        ));
        return chatModel.call(prompt).getResult().getOutput().getText();
    }

    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    private RestClient.Builder timeoutRestClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder().requestFactory(factory);
    }
}
