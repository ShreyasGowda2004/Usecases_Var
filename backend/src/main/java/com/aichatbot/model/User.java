package com.aichatbot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * User Model for Authentication and Multi-Instance Management.
 * 
 * This model represents a user in the system with support for multiple
 * environment instances (dev, test, prod) and authentication tracking.
 * 
 * NOTE: This model is optional. The system works without MongoDB.
 * If MongoDB is not configured, users are tracked client-side only.
 * 
 * Key Features:
 * - Unique username for identification
 * - Email for contact and notifications
 * - Multiple instance support (dev, test, prod)
 * - Default instance selection
 * - Login tracking for analytics
 * - Timestamp auditing
 * 
 * Database:
 * - Stored in MongoDB "users" collection
 * - Indexed on username for fast lookups
 * - Auto-managed by Spring Data MongoDB
 * 
 * Multi-Instance Support:
 * Each user can have multiple instances configured:
 * - Development environment
 * - Testing environment
 * - Production environment
 * - Custom environments
 * 
 * Use Cases:
 * - User authentication and authorization
 * - Instance switching for different environments
 * - Usage analytics and tracking
 * - User preference storage
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String username;
    
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private List<UserInstance> instances;
    private String defaultInstanceId;
    
    public User() {
        this.createdAt = LocalDateTime.now();
        this.lastLoginAt = LocalDateTime.now();
        this.instances = new ArrayList<>();
    }
    
    public User(String username, String email) {
        this();
        this.username = username;
        this.email = email;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    
    public List<UserInstance> getInstances() { return instances; }
    public void setInstances(List<UserInstance> instances) { this.instances = instances; }
    
    public String getDefaultInstanceId() { return defaultInstanceId; }
    public void setDefaultInstanceId(String defaultInstanceId) { this.defaultInstanceId = defaultInstanceId; }
    
    // Inner class for user instances
    public static class UserInstance {
        private String id;
        private String name;
        private String url;
        private String apiKey;
        
        public UserInstance() {}
        
        public UserInstance(String id, String name, String url, String apiKey) {
            this.id = id;
            this.name = name;
            this.url = url;
            this.apiKey = apiKey;
        }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }
}
