package com.vitaliq.vitaliq_platform.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.vitaliq.vitaliq_platform.config.OpenAiConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAiChatService implements AiChatService{
    private final OpenAiConfig openAiConfig;
    private OpenAIClient client;

    @PostConstruct
    public void init(){
        // fromEnv() reads OPENAI_API_KEY from system environment automatically
        this.client = OpenAIOkHttpClient.fromEnv();
        log.info("OpenAiChatService initialized — model: {}, maxTokens: {}",
                openAiConfig.getModel(), openAiConfig.getMaxTokens());
    }

    @Override
    public String generate(String prompt){
        log.debug("Calling OpenAI API — model: {}", openAiConfig.getModel());
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(openAiConfig.getModel())
                .maxCompletionTokens(openAiConfig.getMaxTokens())
                .addUserMessage(prompt)
                .build();

        ChatCompletion completion = client.chat().completions().create(params);

        return completion.choices().get(0).message().content()
                .orElseThrow(() -> new RuntimeException("OpenAI returned empty response"));
    }
}
