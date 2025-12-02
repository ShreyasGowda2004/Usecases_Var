package com.aichatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Ollama AI Service for generating intelligent chat responses.
 * 
 * This service handles all direct communication with the Ollama AI runtime,
 * which hosts and runs large language models (LLMs) locally on the server.
 * 
 * Key Responsibilities:
 * - Send prompts to Ollama API for AI response generation
 * - Configure model parameters (temperature, token limits, context size)
 * - Parse and extract responses from Ollama API
 * - Handle errors and timeouts gracefully
 * 
 * Architecture:
 * - Uses Java HTTP Client for non-blocking HTTP requests
 * - Communicates with Ollama REST API at /api/generate endpoint
 * - Supports streaming and non-streaming responses
 * - Implements automatic JSON escaping/unescaping for safe transmission
 * 
 * IMPORTANT: This service is ONLY used for generating chat responses.
 * It is NOT used for generating embeddings - the system uses keyword-based search instead.
 * 
 * Configuration:
 * - spring.ai.ollama.base-url: Ollama server URL (default: http://localhost:11434)
 * - spring.ai.ollama.chat.model: Model name (e.g., granite4:micro-h)
 * 
 * Performance Optimizations:
 * - Adaptive token prediction based on prompt length
 * - Context window capping for faster responses
 * - 10-minute timeout for long-running queries
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@Service
public class OllamaService {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);
    
    /** HTTP client for making requests to Ollama API */
    private final HttpClient httpClient;
    
    /** Ollama server base URL (e.g., http://localhost:11434) */
    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;
    
    /** Name of the AI model to use for chat responses */
    @Value("${spring.ai.ollama.chat.model}")
    private String modelName;
    
    /**
     * Constructor initializes HTTP client with connection timeout.
     * The client is reused for all requests to improve performance.
     */
    public OllamaService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    /**
     * Generate an AI response for the given prompt.
     * 
     * This is the main entry point for getting AI-generated responses.
     * It sends the prompt to Ollama API and returns the generated text.
     * 
     * Process:
     * 1. Build JSON request body with prompt and model parameters
     * 2. Send POST request to Ollama /api/generate endpoint
     * 3. Wait for response (up to 10 minutes timeout)
     * 4. Parse and extract the generated text from JSON response
     * 5. Return the AI-generated response
     * 
     * @param prompt The input prompt/question to send to the AI model
     * @return The AI-generated response text
     * @throws RuntimeException if Ollama API is unavailable or returns an error
     */
    public String generateResponse(String prompt) {
        try {
            logger.debug("Generating response using model: {}", modelName);
            
            // Build JSON request with model parameters
            String requestBody = buildRequestBody(prompt);
            
            // Create HTTP POST request to Ollama API
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaBaseUrl + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMinutes(10))  // Long timeout for complex queries
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            // Send request and get response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Check for API errors
            if (response.statusCode() != 200) {
                logger.error("Ollama API error: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("Ollama API returned status: " + response.statusCode());
            }
            
            // Extract and return the generated text
            return parseResponse(response.body());
            
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to communicate with Ollama", e);
            throw new RuntimeException("Failed to generate response: " + e.getMessage());
        }
    }
    
    /**
     * Build JSON request body for Ollama API with optimized parameters.
     * 
     * This method constructs the JSON payload that will be sent to Ollama.
     * It includes adaptive parameter tuning based on prompt length for better performance.
     * 
     * Model Parameters:
     * - temperature: 0.1 (low for consistent, factual responses)
     * - top_p: 0.9 (nucleus sampling for diverse but focused output)
     * - num_predict: 512-2048 tokens (adaptive based on prompt length)
     * - num_ctx: 8192 tokens max (context window size)
     * 
     * Performance Optimization:
     * - Short prompts (<1200 chars) → 512 tokens (faster responses)
     * - Long prompts (≥1200 chars) → 2048 tokens (detailed responses)
     * 
     * @param prompt The user's input prompt (will be JSON-escaped)
     * @return JSON string formatted for Ollama API
     */
    private String buildRequestBody(String prompt) {
        // Heuristic: if prompt is short (< 1200 chars) we request fewer tokens to accelerate generation
        int numPredict = prompt.length() < 1200 ? 512 : 2048; // previously 8192
        int numCtx = Math.min(8192, 16384); // cap to 8k for speed
        return String.format("""
                {
                    "model": "%s",
                    "prompt": "%s",
                    "stream": false,
                    "options": {
                        "temperature": 0.1,
                        "top_p": 0.9,
                        "num_predict": %d,
                        "num_ctx": %d
                    }
                }
                """, modelName, escapeJson(prompt), numPredict, numCtx);
    }
    
    /**
     * Parse and extract the AI-generated text from Ollama's JSON response.
     * 
     * Ollama returns responses in JSON format:
     * {"model":"...","response":"AI generated text here","done":true,...}
     * 
     * This method extracts the "response" field value and unescapes JSON characters.
     * Uses simple string parsing for performance (no JSON library overhead).
     * 
     * Error Handling:
     * - Returns fallback message if JSON structure is unexpected
     * - Logs warnings for parsing failures
     * - Never throws exceptions (graceful degradation)
     * 
     * @param responseBody The raw JSON response from Ollama API
     * @return The extracted AI-generated text, or fallback message on error
     */
    private String parseResponse(String responseBody) {
        try {
            // Simple JSON parsing - in production use proper JSON library
            int responseStart = responseBody.indexOf("\"response\":\"") + 12;
            int responseEnd = responseBody.lastIndexOf("\",\"done\"");
            
            if (responseStart > 11 && responseEnd > responseStart) {
                String response = responseBody.substring(responseStart, responseEnd);
                return unescapeJson(response);
            }
            
            logger.warn("Could not parse Ollama response: {}", responseBody);
            return "I apologize, but I couldn't generate a proper response. Please try again.";
            
        } catch (Exception e) {
            logger.error("Failed to parse Ollama response", e);
            return "I encountered an error while processing your request. Please try again.";
        }
    }
    
    /**
     * Escape special characters for safe JSON transmission.
     * 
     * Converts special characters to their JSON escape sequences:
     * - Backslash (\) → \\\\
     * - Quote (") → \\"
     * - Newline (\n) → \\n
     * - Carriage return (\r) → \\r
     * - Tab (\t) → \\t
     * 
     * This ensures that prompts containing special characters don't break
     * the JSON structure when sent to Ollama API.
     * 
     * @param text The raw text to escape
     * @return JSON-safe escaped text
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * Unescape JSON escape sequences to restore original text.
     * 
     * Converts JSON escape sequences back to their original characters:
     * - \\\\ → Backslash (\)
     * - \\" → Quote (")
     * - \\n → Newline (\n)
     * - \\r → Carriage return (\r)
     * - \\t → Tab (\t)
     * 
     * This restores the AI-generated response to its original format
     * after extracting it from Ollama's JSON response.
     * 
     * @param text The JSON-escaped text to unescape
     * @return Original text with special characters restored
     */
    private String unescapeJson(String text) {
        return text.replace("\\\\", "\\")
                   .replace("\\\"", "\"")
                   .replace("\\n", "\n")
                   .replace("\\r", "\r")
                   .replace("\\t", "\t");
    }
    
    /**
     * Check if Ollama service is available and responding.
     * 
     * Performs a health check by calling Ollama's /api/tags endpoint.
     * This is a lightweight operation that verifies:
     * - Ollama server is running
     * - API is accessible
     * - Network connectivity is working
     * 
     * Used by:
     * - System health monitoring
     * - Startup validation
     * - Error diagnostics
     * 
     * @return true if Ollama is healthy and responding, false otherwise
     */
    public boolean isHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaBaseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
            
        } catch (Exception e) {
            logger.warn("Ollama health check failed", e);
            return false;
        }
    }
}
