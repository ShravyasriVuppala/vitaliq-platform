package com.vitaliq.vitaliq_platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "anthropic")
public class AnthropicConfig {

    // api-key intentionally omitted — SDK reads ANTHROPIC_API_KEY from
    // environment automatically via AnthropicOkHttpClient.fromEnv()
    private String model;
    private Integer maxTokens;
}