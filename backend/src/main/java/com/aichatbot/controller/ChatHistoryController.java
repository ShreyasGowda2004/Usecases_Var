package com.aichatbot.controller;

import com.aichatbot.model.ChatHistory;
import com.aichatbot.service.ChatHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Chat History Controller for Conversation Management.
 * 
 * This REST controller provides endpoints for managing chat conversation history,
 * allowing users to save, retrieve, and delete their chat sessions.
 * 
 * NOTE: This controller is optional. Chat history can also be stored
 * client-side in browser localStorage without MongoDB.
 * 
 * Key Features:
 * - Save complete conversations with all messages
 * - Retrieve user-specific conversation history
 * - Get individual conversations by ID
 * - Delete conversations
 * - CORS enabled for frontend access
 * 
 * Endpoints:
 * - POST /history: Save a new or updated conversation
 * - GET /history/{username}: Get all conversations for a user
 * - GET /history/detail/{id}: Get a specific conversation
 * - DELETE /history/{id}: Delete a conversation
 * 
 * Response Format:
 * - Success: Returns ChatHistory object or list
 * - Error: Returns {"error": "message"} with appropriate HTTP status
 * 
 * Security:
 * - CORS enabled for all origins (configure for production)
 * - No authentication required (add for production)
 * - Username-based access control
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
public class ChatHistoryController {
    
    @Autowired
    private ChatHistoryService chatHistoryService;
    
    @PostMapping
    public ResponseEntity<?> saveHistory(@RequestBody ChatHistory chatHistory) {
        try {
            ChatHistory saved = chatHistoryService.saveHistory(chatHistory);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/{username}")
    public ResponseEntity<?> getUserHistory(@PathVariable String username) {
        try {
            List<ChatHistory> history = chatHistoryService.getUserHistory(username);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/session/{id}")
    public ResponseEntity<?> getHistoryById(@PathVariable String id) {
        try {
            return chatHistoryService.getHistoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHistory(@PathVariable String id) {
        try {
            chatHistoryService.deleteHistory(id);
            return ResponseEntity.ok(Map.of("message", "History deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/user/{username}")
    public ResponseEntity<?> deleteAllUserHistory(@PathVariable String username) {
        try {
            chatHistoryService.deleteAllUserHistory(username);
            return ResponseEntity.ok(Map.of("message", "All user history deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/count/{username}")
    public ResponseEntity<?> countUserHistory(@PathVariable String username) {
        try {
            long count = chatHistoryService.countUserHistory(username);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
