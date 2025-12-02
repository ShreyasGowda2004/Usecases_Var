package com.aichatbot.controller;

import com.aichatbot.model.User;
import com.aichatbot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * User Controller for User Management.
 * 
 * This REST controller handles user-related operations including
 * authentication, registration, and profile management.
 * 
 * Key Features:
 * - Simple username-based login (auto-creates users)
 * - User registration with optional email
 * - User profile retrieval
 * - User instance configuration management
 * - User deletion
 * 
 * Endpoints:
 * - POST /users/login: Authenticate or auto-create user
 * - POST /users/register: Register a new user
 * - GET /users/{username}: Get user by username
 * - PUT /users/{username}/config: Update user instances
 * - DELETE /users/{username}: Delete user account
 * 
 * Security Note:
 * Currently uses simple username-based auth for MVP.
 * In production, implement proper authentication with:
 * - Password hashing (BCrypt)
 * - JWT tokens
 * - OAuth2/OIDC
 * - Spring Security
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    /** Service for user management operations */
    @Autowired
    private UserService userService;
    
    /**
     * Authenticates a user or creates a new account if not found.
     * 
     * This endpoint provides simple username-based authentication:
     * - If user exists: Updates last login time and returns user
     * - If user doesn't exist: Auto-creates user for convenience
     * 
     * Request Body:
     * {
     *   "username": "john_doe"
     * }
     * 
     * Response: User object with profile information
     * 
     * Validation:
     * - Username is required and cannot be empty
     * 
     * @param loginRequest Map containing username
     * @return ResponseEntity with User object or error message
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }
            
            User user = userService.loginUser(username);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Registers a new user account.
     * 
     * Creates a new user with username and optional email.
     * Throws error if username already exists.
     * 
     * Request Body:
     * {
     *   "username": "john_doe",
     *   "email": "john@example.com" // optional
     * }
     * 
     * Response: Created User object
     * 
     * Validation:
     * - Username is required
     * - Username must be unique
     * - Email is optional
     * 
     * @param registerRequest Map containing username and optional email
     * @return ResponseEntity with created User or error message
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> registerRequest) {
        try {
            String username = registerRequest.get("username");
            String email = registerRequest.getOrDefault("email", "");
            
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }
            
            User user = userService.createUser(username, email);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Retrieves a user by username.
     * 
     * Returns complete user profile information if found.
     * 
     * @param username Username to search for
     * @return ResponseEntity with User object or 404 Not Found
     */
    @GetMapping("/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            return userService.getUserByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Updates user instance configuration.
     * 
     * Allows users to configure multiple Maximo instances they can access,
     * including URLs and API keys for each instance.
     * 
     * Request Body:
     * {
     *   "instances": [
     *     {
     *       "id": "instance1",
     *       "name": "Production",
     *       "url": "https://maximo-prod.example.com",
     *       "apiKey": "xxx"
     *     }
     *   ],
     *   "defaultInstanceId": "instance1"
     * }
     * 
     * @param username Username to update
     * @param config Configuration map with instances and default selection
     * @return ResponseEntity with updated User object
     */
    @PutMapping("/{username}/config")
    public ResponseEntity<?> updateUserConfig(@PathVariable String username, @RequestBody Map<String, Object> config) {
        try {
            User.UserInstance[] instances = null;
            if (config.containsKey("instances")) {
                // Convert list of maps to UserInstance array
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, String>> instanceList = (java.util.List<Map<String, String>>) config.get("instances");
                instances = instanceList.stream()
                    .map(inst -> new User.UserInstance(
                        inst.get("id"),
                        inst.get("name"),
                        inst.get("url"),
                        inst.get("apiKey")
                    ))
                    .toArray(User.UserInstance[]::new);
            }
            
            String defaultInstanceId = (String) config.get("defaultInstanceId");
            User user = userService.updateUserInstances(username, instances, defaultInstanceId);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Deletes a user account.
     * 
     * Permanently removes the user from the system.
     * 
     * @param username Username to delete
     * @return ResponseEntity with success message or error
     */
    @DeleteMapping("/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        try {
            userService.deleteUser(username);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
