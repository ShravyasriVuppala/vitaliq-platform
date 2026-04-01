package com.vitaliq.vitaliq_platform.service;

public interface AiChatService {

    /**
     * Send a prompt to the configured AI provider and return the text response.
     * Provider is selected via ai.provider in application.yml.
     * NutritionService depends only on this interface — zero provider coupling.
     */
    String generate(String prompt);
}