package com.aichatbot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.Instant;
import java.util.List;

/**
 * Chat History Model for Conversation Persistence.
 * 
 * This model stores complete chat conversations with all messages,
 * timestamps, and metadata for retrieval and analysis.
 * 
 * NOTE: This is an optional feature. Chat history can also be stored
 * client-side in browser localStorage without MongoDB.
 * 
 * Key Features:
 * - Store complete conversation threads
 * - User-specific history tracking
 * - Automatic timestamp management
 * - Conversation title for identification
 * - Ordered message list with roles (user/assistant)
 * 
 * Database:
 * - Stored in MongoDB "chat_history" collection
 * - Compound index on (username, createdAt) for efficient queries
 * - Indexed on username for user-specific lookups
 * 
 * Data Structure:
 * - id: Unique conversation identifier
 * - username: Owner of the conversation
 * - title: User-friendly conversation title
 * - messages: Array of ChatMessage objects (user/assistant pairs)
 * - createdAt: Conversation start timestamp
 * 
 * Use Cases:
 * - View past conversations
 * - Continue previous discussions
 * - Export chat history
 * - Analytics and usage tracking
 * - Training data collection (with consent)
 * 
 * Performance:
 * - Compound index ensures fast user-specific queries
 * - Most recent conversations retrieved first
 * - Pagination support for large histories
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@Document(collection = "chat_history")
@CompoundIndex(name = "user_created_idx", def = "{'username': 1, 'createdAt': -1}")
public class ChatHistory {
    
    @Id
    private String id;
    
    @Indexed
    private String username;
    
    private String title;
    private Instant createdAt;
    private List<ChatMessage> messages;
    
    public ChatHistory() {
        this.createdAt = Instant.now();
    }
    
    public ChatHistory(String id, String username, String title, List<ChatMessage> messages) {
        this();
        this.id = id;
        this.username = username;
        this.title = title;
        this.messages = messages;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
    
    // Inner class for chat messages
    public static class ChatMessage {
        private String id;
        private String type; // "user" or "assistant"
        private String content;
    private Instant timestamp;
        private List<SourceFile> sourceFiles;
        private Boolean fullContent;
        
        public ChatMessage() {}
        
        public ChatMessage(String id, String type, String content, Instant timestamp) {
            this.id = id;
            this.type = type;
            this.content = content;
            this.timestamp = timestamp;
        }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        
        public List<SourceFile> getSourceFiles() { return sourceFiles; }
        public void setSourceFiles(List<SourceFile> sourceFiles) { this.sourceFiles = sourceFiles; }
        
        public Boolean getFullContent() { return fullContent; }
        public void setFullContent(Boolean fullContent) { this.fullContent = fullContent; }
    }
    
    public static class SourceFile {
        private String path;
        private String repository;
        
        public SourceFile() {}
        
        public SourceFile(String path, String repository) {
            this.path = path;
            this.repository = repository;
        }

        // Accept a bare string during deserialization (some clients send source files as simple paths)
        @JsonCreator
        public SourceFile(String pathOrJson) {
            // If the input looks like JSON (starts with {), leave it to default binding
            this.path = pathOrJson;
            this.repository = null;
        }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getRepository() { return repository; }
        public void setRepository(String repository) { this.repository = repository; }
    }
}
