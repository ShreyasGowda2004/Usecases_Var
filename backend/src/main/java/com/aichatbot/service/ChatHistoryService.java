package com.aichatbot.service;

import com.aichatbot.model.ChatHistory;
import com.aichatbot.repository.ChatHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * Chat History Service for Conversation Persistence.
 * 
 * This service manages the storage and retrieval of chat conversation history.
 * It provides a simple CRUD interface for chat history operations.
 * 
 * NOTE: This service is optional. Chat history can also be stored
 * client-side in browser localStorage without MongoDB.
 * 
 * Key Features:
 * - Save complete conversations with all messages
 * - Retrieve user-specific history sorted by date
 * - Get individual conversations by ID
 * - Delete conversations
 * 
 * Operations:
 * - saveHistory(): Persist a complete conversation
 * - getUserHistory(): Get all conversations for a user (newest first)
 * - getHistoryById(): Retrieve a specific conversation
 * - deleteHistory(): Remove a conversation permanently
 * 
 * Use Cases:
 * - View past conversations
 * - Continue previous discussions
 * - Export chat history
 * - Delete unwanted conversations
 * 
 * Dependencies:
 * - Requires MongoDB connection
 * - Uses ChatHistoryRepository for data access
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@Service
public class ChatHistoryService {
    
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;
    
    public ChatHistory saveHistory(ChatHistory chatHistory) {
        return chatHistoryRepository.save(chatHistory);
    }
    
    public List<ChatHistory> getUserHistory(String username) {
        return chatHistoryRepository.findByUsernameOrderByCreatedAtDesc(username);
    }
    
    public Optional<ChatHistory> getHistoryById(String id) {
        return chatHistoryRepository.findById(id);
    }
    
    public void deleteHistory(String id) {
        chatHistoryRepository.deleteById(id);
    }
    
    public void deleteAllUserHistory(String username) {
        chatHistoryRepository.deleteByUsername(username);
    }
    
    public long countUserHistory(String username) {
        return chatHistoryRepository.countByUsername(username);
    }
}
