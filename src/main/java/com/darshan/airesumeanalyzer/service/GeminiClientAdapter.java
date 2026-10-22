package com.darshan.airesumeanalyzer.service;

/**
 * Adapter interface for invoking the Google Gemini API.
 * Encapsulates the SDK calls to allow clean unit testing with mocks.
 */
public interface GeminiClientAdapter {

    /**
     * Sends a generation request to the Gemini API and returns the raw response text.
     *
     * @param apiKey         the Gemini API key
     * @param model          the model identifier (e.g., gemini-2.5-flash)
     * @param prompt         the prompt text
     * @param timeoutSeconds request timeout in seconds
     * @return the raw text response (expected JSON)
     * @throws Exception if any network, timeout, quota, or authentication error occurs
     */
    String generateContent(String apiKey, String model, String prompt, int timeoutSeconds) throws Exception;
}
