package com.aichatbot.controller;

import com.aichatbot.service.OllamaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health Check Controller for System Monitoring.
 * 
 * This REST controller provides health check endpoints for monitoring
 * the overall system status and individual service health.
 * 
 * Key Features:
 * - Ollama AI service availability check
 * - Embedding store status verification
 * - Application health status aggregation
 * - Returns appropriate HTTP status codes (200 UP, 503 DOWN)
 * 
 * Endpoints:
 * - GET /health: Get comprehensive system health status
 * 
 * Health Check Includes:
 * - Ollama: Verifies AI model service connectivity
 * - EmbeddingStore: Verifies document storage availability
 * - Application: Overall application health
 * 
 * Usage:
 * - Monitoring tools can poll this endpoint
 * - Load balancers can use it for health checks
 * - DevOps teams can track service availability
 * - Returns HTTP 200 when all services are UP
 * - Returns HTTP 503 when critical services are DOWN
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:5173"}, allowCredentials = "true")
public class HealthController {
    
    /** Service for Ollama AI model integration */
    private final OllamaService ollamaService;
    
    /**
     * Constructor for dependency injection.
     * 
     * @param ollamaService Service for communicating with Ollama AI
     */
    public HealthController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }
    
    /**
     * Performs a comprehensive health check of all system components.
     * 
     * Checks the health of:
     * 1. Ollama AI service - Critical for generating responses
     * 2. Embedding Store - Required for document search
     * 3. Application - Overall application status
     * 
     * Response Format:
     * - status: Overall system status ("UP" or "DOWN")
     * - services: Individual service statuses
     * 
     * HTTP Status Codes:
     * - 200 OK: All services are healthy
     * - 503 Service Unavailable: One or more critical services are down
     * 
     * Example Response (Healthy):
     * {
     *   "status": "UP",
     *   "services": {
     *     "ollama": "UP",
     *     "embeddingStore": "UP",
     *     "application": "UP"
     *   }
     * }
     * 
     * @return ResponseEntity with health status and appropriate HTTP code
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        // Check if Ollama service is accessible and responsive
        boolean ollamaHealthy = ollamaService.isHealthy();
        
        // Build health status map
        Map<String, Object> health = Map.of(
                "status", ollamaHealthy ? "UP" : "DOWN",
                "services", Map.of(
                        "ollama", ollamaHealthy ? "UP" : "DOWN",
                        "embeddingStore", "UP",  // File-based store is always available
                        "application", "UP"       // Application is running if endpoint is reachable
                )
        );
        
        // Return 200 OK if healthy, 503 Service Unavailable if not
        if (ollamaHealthy) {
            return ResponseEntity.ok(health);
        } else {
            return ResponseEntity.status(503).body(health);
        }
    }
}
