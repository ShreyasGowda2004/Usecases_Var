package com.aichatbot.config;

import com.aichatbot.repository.EmbeddingStore;
import com.aichatbot.repository.FileEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * Embedding Store Configuration.
 * 
 * This configuration class sets up the storage mechanism for document embeddings,
 * which are used for semantic search and retrieval-augmented generation (RAG).
 * 
 * Storage Strategy:
 * - Uses file-based JSONL storage for simplicity and portability
 * - No external database required (MongoDB, Redis, etc.)
 * - Fast in-memory index with persistent disk storage
 * - Suitable for production with thousands of documents
 * 
 * The embedding directory is configurable via:
 * - embedding.store.dir property in application.properties
 * - Defaults to ~/.ai-chatbot/embeddings if not specified
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@Configuration
public class EmbeddingStoreConfig {

    /**
     * Directory path for storing embeddings.
     * Defaults to user's home directory under .ai-chatbot/embeddings
     */
    @Value("${embedding.store.dir:#{systemProperties['user.home']}/.ai-chatbot/embeddings}")
    private String embeddingDir;

    /**
     * Creates and configures the embedding store bean.
     * 
     * Provides a file-based implementation that:
     * - Stores embeddings in JSONL format for easy debugging
     * - Maintains in-memory index for fast queries
     * - Persists to disk for durability across restarts
     * - Supports concurrent read/write operations
     * 
     * @return FileEmbeddingStore instance configured with the specified directory
     */
    @Bean
    public EmbeddingStore embeddingStore() {
        return new FileEmbeddingStore(Path.of(embeddingDir));
    }
}
