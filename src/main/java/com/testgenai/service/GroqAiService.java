package com.testgenai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Wraps Spring AI's ChatClient, which is auto-configured to talk to Groq via
 * Spring AI's OpenAI-compatible client (see spring.ai.openai.* in application.yml).
 *
 * Because Spring AI abstracts the provider, switching to OpenAI, OpenRouter, a local
 * Ollama model, or any other OpenAI-compatible endpoint is a config change only -
 * no code in this class needs to change. Get a free Groq key at
 * https://console.groq.com/keys
 */
@Service
public class GroqAiService {

    /** Placeholder value used when no real API key is supplied, so the context still starts. */
    private static final String UNCONFIGURED = "not-configured";

    private final ChatClient chatClient;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    public GroqAiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !UNCONFIGURED.equals(apiKey);
    }

    /**
     * Sends a system + user prompt pair to the model and returns the raw text response.
     */
    public String complete(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Groq API key is not configured. Set the GROQ_API_KEY environment variable " +
                    "(get a free key at https://console.groq.com/keys) and restart the app.");
        }

        try {
            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            if (content == null || content.isBlank()) {
                throw new IllegalStateException("Empty response from AI provider.");
            }
            return content;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("AI provider request failed: " + e.getMessage(), e);
        }
    }
}
