package io.github.canjiemo.momo.ai.provider.impl;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import io.github.canjiemo.momo.ai.provider.IAiChatProvider;
import io.github.canjiemo.momo.ai.provider.IAiEmbeddingProvider;
import io.github.canjiemo.momo.ai.provider.entity.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 阿里百炼（DashScope）AI Provider
 * <p>
 * 使用 DashScope 的 OpenAI 兼容接口。Spring AI 2.0.0 起 OpenAI 模型底层改用官方
 * OpenAI Java SDK（{@link com.openai.client.OpenAIClient}），baseUrl 需包含 /v1，
 * 本类自动补全（数据库可仍配置为 https://dashscope.aliyuncs.com/compatible-mode）。
 * <p>
 * 推荐模型：
 * - chat:  qwen-plus / qwen-turbo / qwen-max
 * - embed: text-embedding-v3
 * <p>
 * 向量维度：text-embedding-v3 默认 1024，此处固定请求 768 维以匹配 pgvector 列定义。
 * 若切换到此 Provider，需在管理页面执行「全量同步向量」重建向量索引。
 */
@Slf4j
@Component("aliyun")
public class AliyunProvider implements IAiChatProvider, IAiEmbeddingProvider {

    /** LLM 调用读超时，防止 Provider 无响应导致请求线程长期挂起 */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    /** text-embedding-v3 请求维度，需与 ai_field_catalog.embed_vector vector(768) 一致 */
    private static final int EMBED_DIMENSIONS = 768;

    private volatile OpenAiChatModel chatModel;
    private volatile OpenAiEmbeddingModel embeddingModel;

    @Override
    public String getProvider() {
        return "aliyun";
    }

    @Override
    public void init(AiProviderConfig config) {
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl(normalizeBaseUrl(config.getBaseUrl()))
                .apiKey(config.getApiKey())
                .timeout(REQUEST_TIMEOUT)
                .build();
        this.chatModel = OpenAiChatModel.builder()
                .openAiClient(client)
                .options(OpenAiChatOptions.builder()
                        .model(config.getChatModel())
                        .build())
                .build();
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .openAiClient(client)
                .options(OpenAiEmbeddingOptions.builder()
                        .model(config.getEmbedModel())
                        .dimensions(EMBED_DIMENSIONS)
                        .build())
                .metadataMode(MetadataMode.EMBED)
                .build();
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

    /** 官方 OpenAI SDK 会在 baseUrl 后拼接 /chat/completions 等路径，需保证 baseUrl 以 /v1 结尾 */
    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return baseUrl;
        }
        String b = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return b.endsWith("/v1") ? b : b + "/v1";
    }
}
