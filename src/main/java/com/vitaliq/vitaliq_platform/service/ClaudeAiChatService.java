package com.vitaliq.vitaliq_platform.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.vitaliq.vitaliq_platform.config.AnthropicConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider", havingValue = "claude")
public class ClaudeAiChatService implements AiChatService {

    private final AnthropicConfig anthropicConfig;
    private AnthropicClient client;

    // Build client once on startup — not on every request
    @PostConstruct
    public void init() {
        // fromEnv() reads ANTHROPIC_API_KEY from system environment automatically
        this.client = AnthropicOkHttpClient.fromEnv();
        log.info("ClaudeAiChatService initialized — model: {}, maxTokens: {}",
                anthropicConfig.getModel(), anthropicConfig.getMaxTokens());

        // Temporary — verify key is loading from environment
//        String key = System.getenv("ANTHROPIC_API_KEY");
//        log.debug("ANTHROPIC_API_KEY loaded: {}",
//                key != null ? key.substring(0, 10) + "..." : "NULL — key not found in environment");
    }

    @Override
    public String generate(String prompt) {
        log.debug("Calling Claude API — model: {}", anthropicConfig.getModel());

        MessageCreateParams params = MessageCreateParams.builder()
                .model(anthropicConfig.getModel())
                .maxTokens(anthropicConfig.getMaxTokens())
                .addUserMessage(prompt)
                .build();

        Message message = client.messages().create(params);

        return message.content().stream()
                .filter(block -> block.isText())
                .map(block -> block.asText().text())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Claude returned empty response"));
    }
}