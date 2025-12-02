package com.aichatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * GitHub Repository Configuration.
 * 
 * This configuration class manages all GitHub repository settings for the chatbot,
 * including authentication, repository targets, and multi-repository support.
 * 
 * Key Features:
 * - Supports multiple GitHub repositories for knowledge aggregation
 * - Works with both GitHub.com and GitHub Enterprise
 * - Configurable branch tracking per repository
 * - Secure token-based authentication
 * 
 * Configuration is loaded from application.properties with prefix "repo.github":
 * - repo.github.baseurl: GitHub API base URL
 * - repo.github.token: Personal access token or OAuth token
 * - repo.github.repositories: List of repository configurations
 * 
 * Example configuration:
 * <pre>
 * repo.github.baseurl=https://github.ibm.com/api/v3
 * repo.github.token=${GITHUB_TOKEN}
 * repo.github.repositories[0].owner=maximo-application-suite
 * repo.github.repositories[0].name=knowledge-center
 * repo.github.repositories[0].branch=main
 * </pre>
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "repo.github")
public class GitHubRepositoryConfig {
    
    /** GitHub API base URL (supports GitHub Enterprise) */
    private String baseurl;
    
    /** GitHub authentication token (PAT or OAuth) */
    private String token;
    
    /** List of repositories to index and search */
    private List<Repository> repositories;
    
    /**
     * Gets the GitHub API base URL.
     * 
     * @return GitHub API base URL (e.g., https://api.github.com or https://github.ibm.com/api/v3)
     */
    public String getBaseurl() {
        return baseurl;
    }
    
    /**
     * Sets the GitHub API base URL.
     * 
     * @param baseurl GitHub API endpoint
     */
    public void setBaseurl(String baseurl) {
        this.baseurl = baseurl;
    }
    
    /**
     * Gets the GitHub authentication token.
     * 
     * @return GitHub personal access token or OAuth token
     */
    public String getToken() {
        return token;
    }
    
    /**
     * Sets the GitHub authentication token.
     * 
     * @param token GitHub authentication token
     */
    public void setToken(String token) {
        this.token = token;
    }
    
    /**
     * Gets the list of configured repositories.
     * 
     * @return List of Repository configurations
     */
    public List<Repository> getRepositories() {
        return repositories;
    }
    
    /**
     * Sets the list of repositories to index.
     * 
     * @param repositories List of repository configurations
     */
    public void setRepositories(List<Repository> repositories) {
        this.repositories = repositories;
    }
    
    /**
     * Represents a single GitHub repository configuration.
     * 
     * Each repository includes:
     * - owner: Repository owner (user or organization)
     * - name: Repository name
     * - branch: Branch to track (e.g., main, master, develop)
     * 
     * This allows the chatbot to aggregate knowledge from multiple repositories
     * and provide comprehensive answers across different documentation sources.
     */
    public static class Repository {
        /** Repository owner (organization or user) */
        private String owner;
        
        /** Repository name */
        private String name;
        
        /** Branch to index (e.g., main, master) */
        private String branch;
        
        /**
         * Gets the repository owner.
         * 
         * @return Owner name (organization or username)
         */
        public String getOwner() {
            return owner;
        }
        
        /**
         * Sets the repository owner.
         * 
         * @param owner Owner name (organization or username)
         */
        public void setOwner(String owner) {
            this.owner = owner;
        }
        
        /**
         * Gets the repository name.
         * 
         * @return Repository name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Sets the repository name.
         * 
         * @param name Repository name
         */
        public void setName(String name) {
            this.name = name;
        }
        
        /**
         * Gets the branch to track.
         * 
         * @return Branch name (e.g., main, master, develop)
         */
        public String getBranch() {
            return branch;
        }
        
        /**
         * Sets the branch to track.
         * 
         * @param branch Branch name to index
         */
        public void setBranch(String branch) {
            this.branch = branch;
        }
        
        /**
         * Gets the full repository name in owner/name format.
         * Used for GitHub API calls and identification.
         * 
         * @return Full repository identifier (e.g., "microsoft/vscode")
         */
        public String getFullName() {
            return owner + "/" + name;
        }
    }
}
