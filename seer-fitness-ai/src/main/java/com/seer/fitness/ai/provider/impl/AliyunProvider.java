package com.seer.fitness.ai.provider.impl;

import com.seer.fitness.ai.provider.IAiChatProvider;
import com.seer.fitness.ai.provider.IAiEmbeddingProvider;
import com.seer.fitness.ai.provider.entity.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 阿里百炼（DashScope）AI Provider
 * <p>
 * 使用 DashScope 的 OpenAI 兼容接口，baseUrl 配置为：
 * https://dashscope.aliyuncs.com/compatible-mode
 * （不含 /v1，Spring AI 会自动拼接 /v1/chat/completions 和 /v1/embeddings）
 * <p>
 * 推荐模型：
 * - chat:  qwen-plus / qwen-turbo / qwen-max
 * - embed: text-embedding-v3（需在数据库配置 embed_model 字段）
 * <p>
 * 向量维度：text-embedding-v3 默认 1024，此处固定请求 768 维以匹配 pgvector 列定义。
 * 若切换到此 Provider，需在管理页面执行「全量同步向量」重建向量索引。
 */
@Slf4j
@Component("aliyun")
public class AliyunProvider implements IAiChatProvider, IAiEmbeddingProvider {

    private volatile OpenAiChatModel chatModel;
    private volatile OpenAiEmbeddingModel embeddingModel;

    @Override
    public String getProvider() {
        return "aliyun";
    }

    @Override
    public void init(AiProviderConfig config) {
        OpenAiApi api = new OpenAiApi(config.getBaseUrl(), config.getApiKey());
        this.chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(config.getChatModel())
                        .build())
                .build();
        this.embeddingModel = new OpenAiEmbeddingModel(api, MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(config.getEmbedModel())
                        .dimensions(768)
                        .build());
        log.info("阿里百炼 Provider 初始化完成: chatModel={}, embedModel={}",
                config.getChatModel(), config.getEmbedModel());
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
