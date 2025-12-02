package com.aichatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Chat Request Data Transfer Object.
 * 
 * This DTO encapsulates all information needed for processing a chat message,
 * including the user's question, session tracking, and retrieval options.
 * 
 * Key Features:
 * - Message validation (required, max 5000 characters)
 * - Session ID for conversation tracking
 * - Context inclusion toggle for RAG system
 * - Fast mode for quick responses with reduced context
 * - Full content mode for complete file retrieval
 * 
 * Validation Rules:
 * - Message must not be blank
 * - Message maximum length: 5000 characters
 * - Session ID is optional (auto-generated if not provided)
 * 
 * Usage Modes:
 * 1. Fast Mode (default): Quick responses with top 6 chunks
 * 2. Full Content Mode: Complete file content retrieval
 * 3. Context Mode: Include/exclude RAG context
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
public class ChatRequest {
    
    /** User's chat message (question or request) */
    @NotBlank(message = "Message cannot be empty")
    @Size(max = 5000, message = "Message too long")
    private String message;
    
    /** Session identifier for conversation tracking */
    private String sessionId;
    
    /** Whether to include RAG context in the response */
    private boolean includeContext = true;
    
    /** Enable fast mode for quicker responses (reduced context, smaller generation) */
    private boolean fastMode = true; // default to fast for quicker UX if caller omits field
    
    /** Return complete content from best matching file (no truncation) */
    private boolean fullContent = false;
    
    /**
     * Default constructor.
     */
    public ChatRequest() {}
    
    /**
     * Constructor with message and session ID.
     * 
     * @param message User's chat message
     * @param sessionId Session identifier
     */
    public ChatRequest(String message, String sessionId) {
        this.message = message;
        this.sessionId = sessionId;
    }
    
    /**
     * Gets the user's chat message.
     * 
     * @return The chat message
     */
    public String getMessage() { return message; }
    
    /**
     * Sets the user's chat message.
     * 
     * @param message The chat message
     */
    public void setMessage(String message) { this.message = message; }
    
    /**
     * Gets the session identifier.
     * 
     * @return Session ID for conversation tracking
     */
    public String getSessionId() { return sessionId; }
    
    /**
     * Sets the session identifier.
     * 
     * @param sessionId Session ID for conversation tracking
     */
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    /**
     * Checks if RAG context should be included.
     * 
     * @return true if context should be included, false otherwise
     */
    public boolean isIncludeContext() { return includeContext; }
    
    /**
     * Sets whether to include RAG context.
     * 
     * @param includeContext true to include context, false otherwise
     */
    public void setIncludeContext(boolean includeContext) { this.includeContext = includeContext; }
    
    /**
     * Checks if fast mode is enabled.
     * Fast mode uses lightweight processing for quicker responses.
     * 
     * @return true if fast mode is enabled
     */
    public boolean isFastMode() { return fastMode; }
    
    /**
     * Sets fast mode flag.
     * 
     * @param fastMode true to enable fast mode
     */
    public void setFastMode(boolean fastMode) { this.fastMode = fastMode; }
    
    /**
     * Checks if full content retrieval is requested.
     * Full content mode returns complete file contents without truncation.
     * 
     * @return true if full content is requested
     */
    public boolean isFullContent() { return fullContent; }
    
    /**
     * Sets full content retrieval flag.
     * 
     * @param fullContent true to enable full content retrieval
     */
    public void setFullContent(boolean fullContent) { this.fullContent = fullContent; }
}
