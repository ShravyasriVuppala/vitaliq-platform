package com.vitaliq.vitaliq_platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAiConfig {

    // api-key intentionally omitted — SDK reads OPENAI_API_KEY from
    // environment automatically via OpenAIOkHttpClient.fromEnv()
    private String model;  // read from application.yml
    private Integer maxTokens;
}