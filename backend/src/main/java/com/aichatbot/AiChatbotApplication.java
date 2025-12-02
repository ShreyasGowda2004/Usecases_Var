package com.aichatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot Application for AI Chatbot System.
 * 
 * This is the entry point for the production-ready AI chatbot application that provides:
 * - Advanced RAG (Retrieval-Augmented Generation) capabilities for GitHub repositories
 * - Direct integration with Ollama AI models for intelligent responses
 * - Real-time document processing and semantic search
 * - File-based embedding storage for fast retrieval without external databases
 * 
 * Architecture Features:
 * - Excludes DataSource and JPA auto-configuration (uses file-based storage)
 * - Enables caching for improved performance on repeated queries
 * - Enables async processing for non-blocking operations
 * - Enables scheduled tasks for periodic repository indexing
 * 
 * The application integrates with:
 * - GitHub API for repository content access
 * - Ollama for AI model inference
 * - File-based JSONL storage for embeddings
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,  // Exclude JDBC - using file-based storage
    HibernateJpaAutoConfiguration.class // Exclude JPA - using file-based storage
})
@EnableCaching      // Enable Spring caching for performance optimization
@EnableAsync        // Enable asynchronous method execution for non-blocking operations
@EnableScheduling   // Enable scheduled tasks for periodic repository re-indexing
public class AiChatbotApplication {

    /**
     * Main application entry point.
     * Bootstraps the Spring Boot application and starts the embedded web server.
     * 
     * @param args Command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(AiChatbotApplication.class, args);
    }
}
