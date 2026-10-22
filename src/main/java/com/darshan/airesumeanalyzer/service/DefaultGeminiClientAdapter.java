package com.darshan.airesumeanalyzer.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.springframework.stereotype.Component;

/**
 * Production implementation of {@link GeminiClientAdapter} using the official
 * Google Gen AI Java SDK (com.google.genai:google-genai).
 */
@Component
public class DefaultGeminiClientAdapter implements GeminiClientAdapter {

    @Override
    public String generateContent(String apiKey, String model, String prompt, int timeoutSeconds) throws Exception {
        int timeoutMillis = Math.max(1, timeoutSeconds) * 1000;
        HttpRetryOptions retryOptions = HttpRetryOptions.builder()
                .attempts(1)
                .build();

        HttpOptions httpOptions = HttpOptions.builder()
                .timeout(timeoutMillis)
                .retryOptions(retryOptions)
                .build();

        Client client = Client.builder()
                .apiKey(apiKey)
                .httpOptions(httpOptions)
                .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .build();

        GenerateContentResponse response = client.models.generateContent(model, prompt, config);
        return response.text();
    }
}
