package com.aichatbot.controller;

import com.aichatbot.dto.ChatRequest;
import com.aichatbot.dto.ChatResponse;
import com.aichatbot.service.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * Chat Controller for AI Conversations.
 * 
 * This REST controller handles all chat-related operations, providing
 * the main interface between the frontend and the AI chatbot backend.
 * 
 * Key Features:
 * - Asynchronous message processing for responsive UX
 * - Integration with RAG (Retrieval-Augmented Generation) system
 * - Support for fast mode and full content retrieval
 * - Session-based conversation tracking
 * - Comprehensive error handling and logging
 * 
 * Endpoints:
 * - POST /chat/message: Send a message and receive AI response
 * 
 * Architecture:
 * - Uses CompletableFuture for non-blocking async operations
 * - Delegates business logic to ChatService
 * - Validates requests using Jakarta Validation
 * - Returns structured ChatResponse DTOs
 * 
 * Note: Chat history is intentionally NOT persisted for privacy and simplicity.
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:5173", "http://localhost:3001"}, allowCredentials = "true")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    /** Service for processing chat messages and generating AI responses */
    private final ChatService chatService;
    
    /**
     * Constructor for dependency injection.
     * 
     * @param chatService Service handling chat message processing and AI generation
     */
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    /**
     * Processes a chat message and returns an AI-generated response.
     * 
     * This endpoint is the core of the chatbot functionality:
     * 1. Receives a user message with optional context settings
     * 2. Performs semantic search to find relevant documentation
     * 3. Generates an AI response using Ollama with retrieved context
     * 4. Returns the response with source file references
     * 
     * Request Processing:
     * - Validates the request using Jakarta Validation (@Valid)
     * - Extracts user message and session ID
     * - Determines fast mode vs. full content mode
     * - Performs asynchronous processing for non-blocking operation
     * 
     * Response includes:
     * - AI-generated answer based on repository content
     * - Response time in milliseconds
     * - Source file references (sanitized for security)
     * - Model name used for generation
     * 
     * Error Handling:
     * - Returns 200 OK with error details in response body
     * - Logs all errors for debugging
     * - Provides user-friendly error messages
     * 
     * @param request ChatRequest containing user message and options
     * @return CompletableFuture with ChatResponse containing AI answer
     */
    @PostMapping("/message")
    public CompletableFuture<ResponseEntity<ChatResponse>> sendMessage(@Valid @RequestBody ChatRequest request) {
        logger.info("Received chat message from session: {}", request.getSessionId());
        
        // Process message asynchronously to avoid blocking the request thread
        return chatService.processMessage(request)
                .thenApply(response -> {
                    // Return appropriate HTTP status based on processing success
                    if (response.isSuccess()) {
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.internalServerError().body(response);
                    }
                })
                .exceptionally(ex -> {
                    // Handle unexpected exceptions gracefully
                    logger.error("Failed to process chat message", ex);
                    ChatResponse errorResponse = ChatResponse.error(
                            "An unexpected error occurred. Please try again.", 
                            request.getSessionId()
                    );
                    return ResponseEntity.internalServerError().body(errorResponse);
                });
    }
    
    // Chat history endpoints intentionally removed: no session history is stored
}
