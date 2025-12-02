package com.aichatbot.service;

import com.aichatbot.dto.GitHubFile;
import com.aichatbot.config.GitHubRepositoryConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * GitHub Service for Repository Content Access.
 * 
 * This service handles all interactions with GitHub API to fetch repository content.
 * It supports both GitHub.com and GitHub Enterprise for documentation access.
 * 
 * Key Features:
 * - Fetch file lists from multiple repositories
 * - Retrieve file content with Base64 decoding
 * - Recursive directory traversal
 * - Token-based authentication
 * - In-memory caching for performance
 * - Async operations for non-blocking I/O
 * 
 * Architecture:
 * - Uses Java HttpClient for HTTP/2 support
 * - JSON parsing with Jackson ObjectMapper
 * - CompletableFuture for async operations
 * - Repository caching to reduce API calls
 * 
 * Supported Operations:
 * - List all files across configured repositories
 * - Get file content by path
 * - Detect text files for processing
 * - Handle large files and binary content
 * 
 * Configuration:
 * - repo.github.baseurl: GitHub API base URL
 * - repo.github.token: Personal access token or OAuth token
 * 
 * API Rate Limiting:
 * - GitHub limits: 5000 requests/hour (authenticated)
 * - Caching reduces API calls significantly
 * - Batch operations for efficiency
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@Service
public class GitHubService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);

    @Value("${repo.github.baseurl}")
    private String githubBaseUrl;

    @Value("${repo.github.token}")
    private String githubToken;

    private final ObjectMapper objectMapper;
    private final GitHubRepositoryConfig repositoryConfig;
    private final HttpClient httpClient;

    private final Map<String, List<GitHubFile>> repositoryCache = new HashMap<>();

    public GitHubService(ObjectMapper objectMapper, GitHubRepositoryConfig repositoryConfig) {
        this.objectMapper = objectMapper;
        this.repositoryConfig = repositoryConfig;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public CompletableFuture<List<GitHubFile>> getAllFiles() {
        return CompletableFuture.supplyAsync(() -> {
            List<GitHubFile> allFiles = new ArrayList<>();
            
            for (GitHubRepositoryConfig.Repository repo : repositoryConfig.getRepositories()) {
                try {
                    List<GitHubFile> repoFiles = getRepositoryContents(repo, "").join();
                    allFiles.addAll(repoFiles);
                } catch (Exception e) {
                    logger.error("Failed to get files from repository: {}", repo.getFullName(), e);
                }
            }
            
            return allFiles;
        });
    }

    public CompletableFuture<List<GitHubFile>> getRepositoryContents(GitHubRepositoryConfig.Repository repository, String path) {
        String cacheKey = repository.getFullName() + ":" + path;
        
        if (repositoryCache.containsKey(cacheKey)) {
            logger.debug("Cache hit for: {}", cacheKey);
            return CompletableFuture.completedFuture(repositoryCache.get(cacheKey));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s",
                        githubBaseUrl, repository.getOwner(), repository.getName(), path, repository.getBranch());
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "token " + githubToken)
                        .header("Accept", "application/vnd.github+json")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    logger.error("GitHub API error for {}: {} - {}", url, response.statusCode(), response.body());
                    return new ArrayList<>();
                }

                JsonNode jsonArray = objectMapper.readTree(response.body());
                List<GitHubFile> files = new ArrayList<>();

                if (jsonArray.isArray()) {
                    for (JsonNode item : jsonArray) {
                        GitHubFile file = new GitHubFile();
                        file.setName(item.get("name").asText());
                        file.setPath(item.get("path").asText());
                        file.setType(item.get("type").asText());
                        file.setSize(item.has("size") ? item.get("size").asLong() : 0);
                        file.setRepositoryName(repository.getFullName());

                        if ("dir".equals(file.getType())) {
                            List<GitHubFile> subFiles = getRepositoryContents(repository, file.getPath()).join();
                            files.addAll(subFiles);
                        } else {
                            files.add(file);
                        }
                    }
                }

                repositoryCache.put(cacheKey, files);
                return files;

            } catch (Exception e) {
                logger.error("Failed to fetch repository contents for {}: {}", repository.getFullName(), e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    public CompletableFuture<GitHubFile> getFileContent(GitHubRepositoryConfig.Repository repository, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s",
                        githubBaseUrl, repository.getOwner(), repository.getName(), filePath, repository.getBranch());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "token " + githubToken)
                        .header("Accept", "application/vnd.github+json")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    logger.error("GitHub API error for file {}: {} - {}", filePath, response.statusCode(), response.body());
                    return new GitHubFile();
                }

                JsonNode jsonResponse = objectMapper.readTree(response.body());
                
                GitHubFile file = new GitHubFile();
                file.setName(jsonResponse.get("name").asText());
                file.setPath(jsonResponse.get("path").asText());
                file.setType(jsonResponse.get("type").asText());
                file.setSize(jsonResponse.has("size") ? jsonResponse.get("size").asLong() : 0);
                file.setRepositoryName(repository.getFullName());

                if (jsonResponse.has("content")) {
                    String encodedContent = jsonResponse.get("content").asText();
                    String decodedContent = new String(Base64.getDecoder().decode(encodedContent.replaceAll("\\s", "")));
                    file.setContent(decodedContent);
                }

                return file;

            } catch (Exception e) {
                logger.error("Failed to fetch file content for {}: {}", filePath, e.getMessage());
                return new GitHubFile();
            }
        });
    }

    public boolean isTextFile(String fileName) {
        if (fileName == null) return false;
        
        String lowerName = fileName.toLowerCase();
        
        // Ignore README.md files from all repositories
        if (lowerName.equals("readme.md") || lowerName.equals("readme")) {
            return false;
        }
        
        Set<String> textExtensions = Set.of(
            ".md", ".txt", ".json", ".xml", ".yml", ".yaml", ".properties", ".conf", ".config",
            ".java", ".js", ".ts", ".jsx", ".tsx", ".py", ".rb", ".go", ".rs", ".c", ".cpp", ".h", ".hpp",
            ".css", ".scss", ".sass", ".less", ".html", ".htm", ".php", ".sql", ".sh", ".bash", ".zsh",
            ".ps1", ".bat", ".cmd", ".dockerfile", ".makefile", ".cmake", ".gradle", ".maven", ".pom",
            ".log", ".ini", ".toml", ".cfg", ".env", ".gitignore", ".gitattributes", ".editorconfig",
            ".eslintrc", ".prettierrc", ".babelrc", ".webpack", ".rollup", ".vite",
            // Certificate and security files
            ".crt", ".cert", ".pem", ".key", ".pub", ".cer", ".der", ".p7b", ".p7c", ".p12", ".pfx",
            // Configuration and data files
            ".rsp", ".response", ".template", ".tpl", ".mustache", ".hbs", ".jinja", ".j2",
            // Documentation and text formats
            ".rst", ".adoc", ".asciidoc", ".tex", ".latex", ".org", ".wiki",
            // Data formats
            ".csv", ".tsv", ".jsonl", ".ndjson", ".geojson", ".topojson",
            // Infrastructure and deployment
            ".tf", ".terraform", ".hcl", ".nomad", ".consul", ".vault", ".k8s", ".kube",
            ".helm", ".chart", ".ansible", ".playbook", ".role", ".handler",
            // Build and CI/CD
            ".jenkins", ".travis", ".circleci", ".github", ".gitlab", ".azure", ".bitbucket"
        );
        
        return textExtensions.stream().anyMatch(lowerName::endsWith) ||
               lowerName.equals("dockerfile") || lowerName.equals("makefile") ||
               lowerName.equals("license") || lowerName.equals("changelog") || lowerName.equals("contributing");
    }

    public void clearCache() {
        repositoryCache.clear();
        logger.info("Repository cache cleared");
    }

    public int getCacheSize() {
        return repositoryCache.size();
    }
}
