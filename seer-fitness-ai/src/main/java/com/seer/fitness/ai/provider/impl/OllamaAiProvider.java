package com.seer.fitness.ai.provider.impl;

import com.seer.fitness.ai.provider.IAiChatProvider;
import com.seer.fitness.ai.provider.IAiEmbeddingProvider;
import com.seer.fitness.ai.provider.entity.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component("ollama")
public class OllamaAiProvider implements IAiChatProvider, IAiEmbeddingProvider {

    private OllamaChatModel chatModel;
    private OllamaEmbeddingModel embeddingModel;

    @Override
    public String getProvider() {
        return "ollama";
    }

    @Override
    public void init(AiProviderConfig config) {
        OllamaApi api = new OllamaApi(config.getBaseUrl());
        this.chatModel = OllamaChatModel.builder()
                .ollamaApi(api)
                .defaultOptions(OllamaOptions.builder().model(config.getChatModel()).build())
                .build();
        this.embeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(api)
                .defaultOptions(OllamaOptions.builder().model(config.getEmbedModel()).build())
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
}
