package com.aichatbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Web Configuration for CORS (Cross-Origin Resource Sharing).
 * 
 * This configuration enables the frontend React application to communicate
 * with the Spring Boot backend by properly configuring CORS policies.
 * 
 * Security Features:
 * - Configurable allowed origins for frontend access
 * - Supports multiple HTTP methods (GET, POST, PUT, DELETE)
 * - Allows credentials for session management
 * - Configurable headers for API requests
 * - Preflight request caching for performance
 * 
 * Configuration is loaded from application.properties:
 * - cors.allowed-origins: Frontend URLs (e.g., http://localhost:3000)
 * - cors.allowed-methods: HTTP methods allowed (GET, POST, etc.)
 * - cors.allowed-headers: HTTP headers permitted
 * - cors.allow-credentials: Whether to allow cookies/credentials
 * 
 * This configuration is essential for development and production deployments
 * where the frontend and backend are hosted on different domains or ports.
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@Configuration
public class WebConfig {

    /** Allowed origin patterns for CORS (frontend URLs) */
    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    /** Allowed HTTP methods for CORS */
    @Value("${cors.allowed-methods}")
    private String[] allowedMethods;

    /** Allowed HTTP headers for CORS */
    @Value("${cors.allowed-headers}")
    private String allowedHeaders;

    /** Whether to allow credentials (cookies, authorization headers) */
    @Value("${cors.allow-credentials}")
    private boolean allowCredentials;

    /**
     * Configures CORS (Cross-Origin Resource Sharing) for the application.
     * 
     * This bean enables the React frontend to make API calls to the Spring Boot backend
     * even when they're hosted on different domains or ports.
     * 
     * CORS Configuration:
     * - Allowed Origins: Frontend URLs that can access the API
     * - Allowed Methods: HTTP verbs permitted (GET, POST, PUT, DELETE)
     * - Allowed Headers: HTTP headers the frontend can send
     * - Allow Credentials: Enables sending cookies and auth headers
     * - Max Age: How long browsers cache preflight responses (1 hour)
     * 
     * Security Note:
     * In production, restrict allowed origins to specific domains instead of wildcards.
     * 
     * @return CorsConfigurationSource configured for the application
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origin patterns (supports wildcards for development)
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
        
        // Set allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(allowedMethods));
        
        // Set allowed headers
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(allowCredentials);
        
        // Cache preflight responses for 1 hour to improve performance
        configuration.setMaxAge(3600L);

        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
