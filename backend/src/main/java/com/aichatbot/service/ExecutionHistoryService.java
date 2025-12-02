package com.aichatbot.service;

import com.aichatbot.model.ExecutionHistory;
import com.aichatbot.repository.ExecutionHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Execution History Service for API Request/Response Tracking.
 * 
 * This service manages the storage and retrieval of API execution records,
 * capturing every API request made through the system for auditing and replay.
 * 
 * Key Features:
 * - Store complete API requests with headers, params, and body
 * - Store complete API responses with status and body
 * - Automatic timestamp management
 * - User-specific history tracking
 * - Support for multiple sources (Console, Chat, GitHub)
 * 
 * Execution Sources:
 * - Execution Console: Manual API testing
 * - Chat Play Button: Auto-extracted requests from AI responses
 * - GitHub File View: Section execute from documentation
 * 
 * Stored Information:
 * - Request: Method, URL, headers, parameters, body
 * - Response: Status code, headers, body
 * - Metadata: Username, timestamp, source
 * 
 * Use Cases:
 * - API execution history viewing
 * - Request replay/re-execution
 * - Debugging API interactions
 * - Usage analytics and auditing
 * - Error pattern analysis
 * 
 * Dependencies:
 * - Requires MongoDB connection
 * - Uses ExecutionHistoryRepository for data access
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@Service
public class ExecutionHistoryService {

    @Autowired
    private ExecutionHistoryRepository executionHistoryRepository;

    public ExecutionHistory save(ExecutionHistory history) {
        if (history.getTimestamp() == null) {
            history.setTimestamp(Instant.now());
        }
        // Normalize lists (avoid null for persistence/UI)
        if (history.getRequestHeaders() == null) history.setRequestHeaders(new java.util.ArrayList<>());
        if (history.getRequestParams() == null) history.setRequestParams(new java.util.ArrayList<>());
        if (history.getResponseHeaders() == null) history.setResponseHeaders(new java.util.ArrayList<>());
        return executionHistoryRepository.save(history);
    }

    public List<ExecutionHistory> listByUsername(String username) {
        return executionHistoryRepository.findByUsernameOrderByTimestampDesc(username);
    }

    public long countByUsername(String username) {
        return executionHistoryRepository.countByUsername(username);
    }

    public Optional<ExecutionHistory> getById(String id) {
        return executionHistoryRepository.findById(id);
    }

    public void deleteById(String id) {
        executionHistoryRepository.deleteById(id);
    }

    public void deleteByUsername(String username) {
        executionHistoryRepository.deleteByUsername(username);
    }
}
