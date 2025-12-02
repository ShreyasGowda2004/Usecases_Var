package com.aichatbot.service;

import com.aichatbot.model.User;
import com.aichatbot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * User Service for User Management and Authentication.
 * 
 * This service handles all user-related operations including:
 * - User registration and creation
 * - Authentication and login tracking
 * - User profile retrieval
 * - Instance management (dev, test, prod)
 * 
 * NOTE: This service is optional. The system can run without MongoDB.
 * If MongoDB is not configured, user management happens client-side.
 * 
 * Key Features:
 * - Username uniqueness validation
 * - Login timestamp tracking
 * - Multi-instance support per user
 * - Default instance management
 * - Automatic user creation on first login
 * 
 * Use Cases:
 * - User registration flow
 * - Login/logout tracking
 * - Instance switching for different environments
 * - User preference storage
 * 
 * Dependencies:
 * - Requires MongoDB connection
 * - Uses UserRepository for data access
 * - Throws IllegalArgumentException for validation errors
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public User createUser(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        User user = new User(username, email);
        return userRepository.save(user);
    }
    
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public User loginUser(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLoginAt(LocalDateTime.now());
            return userRepository.save(user);
        } else {
            // Auto-create user if doesn't exist (for backwards compatibility)
            User newUser = new User(username, "");
            return userRepository.save(newUser);
        }
    }
    
    public User updateUserInstances(String username, User.UserInstance[] instances, String defaultInstanceId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setInstances(instances != null ? java.util.Arrays.asList(instances) : new java.util.ArrayList<>());
        user.setDefaultInstanceId(defaultInstanceId);
        return userRepository.save(user);
    }
    
    public void deleteUser(String username) {
        userRepository.findByUsername(username).ifPresent(user -> userRepository.delete(user));
    }
}
