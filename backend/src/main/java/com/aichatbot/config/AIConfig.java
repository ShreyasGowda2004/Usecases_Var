package com.aichatbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI Configuration for Ollama Integration.
 * 
 * This configuration class manages all AI-related settings for the chatbot system,
 * specifically for integration with Ollama - a local AI model runtime.
 * 
 * Responsibilities:
 * - Configure Ollama base URL for API communication
 * - Define chat model for conversational responses
 * - Define embedding model for semantic search (future use)
 * 
 * Configuration is loaded from application.properties:
 * - spring.ai.ollama.base-url: Ollama server endpoint (default: http://localhost:11434)
 * - spring.ai.ollama.chat.model: Model name for chat (e.g., granite3.3:8b)
 * - spring.ai.ollama.embedding.model: Model for embeddings (reserved for future use)
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@Configuration
public class AIConfig {

    /** Ollama server base URL for API requests */
    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    /** Chat model name for conversational AI responses */
    @Value("${spring.ai.ollama.chat.model}")
    private String chatModel;

    /** Embedding model name for semantic vector generation (future use) */
    @Value("${spring.ai.ollama.embedding.model}")
    private String embeddingModel;

    /**
     * Provides the Ollama server base URL as a Spring bean.
     * This URL is used by OllamaService to communicate with the local Ollama instance.
     * 
     * @return The configured Ollama base URL (e.g., http://localhost:11434)
     */
    @Bean
    public String ollamaBaseUrl() {
        return ollamaBaseUrl;
    }

    /**
     * Provides the chat model name as a Spring bean.
     * This model is used for generating intelligent conversational responses.
     * 
     * @return The configured chat model name (e.g., granite3.3:8b)
     */
    @Bean 
    public String chatModelName() {
        return chatModel;
    }
    
    /**
     * Provides the embedding model name as a Spring bean.
     * Reserved for future implementation of advanced semantic embeddings.
     * Currently using keyword-based search instead of neural embeddings.
     * 
     * @return The configured embedding model name
     */
    @Bean
    public String embeddingModelName() {
        return embeddingModel;
    }
}
