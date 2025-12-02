package com.aichatbot.dto;

import java.util.List;

/**
 * Chat Response Data Transfer Object.
 * 
 * This DTO encapsulates the AI-generated response along with metadata
 * about the response generation process.
 * 
 * Key Information:
 * - AI-generated response text
 * - Session identifier for tracking
 * - Response time in milliseconds
 * - Source file references
 * - Model used for generation
 * - Success/error status
 * 
 * Response includes:
 * - Intelligent answer based on repository documentation
 * - Performance metrics (response time)
 * - Attribution (source files used)
 * - Model information for transparency
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
public class ChatResponse {
    
    /** AI-generated response text */
    private String response;
    
    /** Session identifier for conversation tracking */
    private String sessionId;
    
    /** Response generation time in milliseconds */
    private long responseTimeMs;
    
    /** List of source files used for generating the response */
    private List<String> sourceFiles;
    
    /** AI model used for generation (e.g., granite3.3:8b) */
    private String modelUsed;
    
    /** Whether the response was generated successfully */
    private boolean success;
    
    /** Error message if response generation failed */
    private String errorMessage;
    
    /**
     * Default constructor.
     */
    public ChatResponse() {}
    
    /**
     * Constructor for successful responses.
     * 
     * @param response AI-generated response text
     * @param sessionId Session identifier
     * @param responseTimeMs Response generation time in milliseconds
     */
    public ChatResponse(String response, String sessionId, long responseTimeMs) {
        this.response = response;
        this.sessionId = sessionId;
        this.responseTimeMs = responseTimeMs;
        this.success = true;
    }
    
    /**
     * Creates an error response.
     * 
     * @param errorMessage Error description
     * @param sessionId Session identifier
     * @return ChatResponse with error information
     */
    public static ChatResponse error(String errorMessage, String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setErrorMessage(errorMessage);
        response.setSessionId(sessionId);
        response.setSuccess(false);
        return response;
    }
    
    /**
     * Creates a successful response with full details.
     * 
     * @param message AI-generated response text
     * @param sessionId Session identifier
     * @param responseTime Response generation time in milliseconds
     * @param sourceFiles List of source files used
     * @param model AI model name used for generation
     * @return ChatResponse with complete information
     */
    public static ChatResponse success(String message, String sessionId, long responseTime, List<String> sourceFiles, String model) {
        ChatResponse response = new ChatResponse(message, sessionId, responseTime);
        response.setSourceFiles(sourceFiles);
        response.setModelUsed(model);
        return response;
    }
    
    // Getters and Setters with JavaDoc
    
    /**
     * Gets the AI-generated response text.
     * @return Response text
     */
    public String getResponse() { return response; }
    
    /**
     * Sets the AI-generated response text.
     * @param response Response text
     */
    public void setResponse(String response) { this.response = response; }
    
    /**
     * Gets the session identifier.
     * @return Session ID
     */
    public String getSessionId() { return sessionId; }
    
    /**
     * Sets the session identifier.
     * @param sessionId Session ID
     */
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    /**
     * Gets the response generation time.
     * @return Response time in milliseconds
     */
    public long getResponseTimeMs() { return responseTimeMs; }
    
    /**
     * Sets the response generation time.
     * @param responseTimeMs Response time in milliseconds
     */
    public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    
    /**
     * Gets the list of source files used.
     * @return Source file references
     */
    public List<String> getSourceFiles() { return sourceFiles; }
    
    /**
     * Sets the list of source files used.
     * @param sourceFiles Source file references
     */
    public void setSourceFiles(List<String> sourceFiles) { this.sourceFiles = sourceFiles; }
    
    /**
     * Gets the AI model name used for generation.
     * @return Model name (e.g., granite3.3:8b)
     */
    public String getModelUsed() { return modelUsed; }
    
    /**
     * Sets the AI model name.
     * @param modelUsed Model name
     */
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }
    
    /**
     * Checks if the response was generated successfully.
     * @return true if successful, false if error occurred
     */
    public boolean isSuccess() { return success; }
    
    /**
     * Sets the success status.
     * @param success true if successful, false otherwise
     */
    public void setSuccess(boolean success) { this.success = success; }
    
    /**
     * Gets the error message if response generation failed.
     * @return Error message or null if successful
     */
    public String getErrorMessage() { return errorMessage; }
    
    /**
     * Sets the error message.
     * @param errorMessage Error description
     */
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
