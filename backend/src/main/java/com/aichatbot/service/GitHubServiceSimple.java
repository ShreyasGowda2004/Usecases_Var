package com.aichatbot.service;

import com.aichatbot.dto.GitHubFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

//@Service
public class GitHubServiceSimple {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubServiceSimple.class);
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    @Value("${repo.github.owner}")
    private String repositoryOwner;
    
    @Value("${repo.github.name}")
    private String repositoryName;
    
    @Value("${repo.github.branch}")
    private String branchName;
    
    @Value("${repo.github.token}")
    private String githubToken;
    
    @Value("${repo.github.baseurl}")
    private String baseUrl;
    
    public GitHubServiceSimple(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    @Cacheable(value = "github-tree", key = "#path")
    public CompletableFuture<List<GitHubFile>> getRepositoryContents(String path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s",
                        baseUrl, repositoryOwner, repositoryName, path != null ? path : "", branchName);
                
                logger.info("Fetching repository contents from: {}", url);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "token " + githubToken)
                        .header("Accept", "application/vnd.github.v3+json")
                        .timeout(Duration.ofMinutes(2))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    logger.error("GitHub API error: {} - {}", response.statusCode(), response.body());
                    throw new RuntimeException("Failed to fetch repository contents: " + response.statusCode());
                }
                
                return parseGitHubFiles(response.body());
                
            } catch (Exception e) {
                logger.error("Failed to fetch repository contents", e);
                throw new RuntimeException("Failed to fetch repository contents: " + e.getMessage());
            }
        });
    }
    
    @Cacheable(value = "github-file", key = "#filePath")
    public CompletableFuture<GitHubFile> getFileContent(String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s",
                        baseUrl, repositoryOwner, repositoryName, filePath, branchName);
                
                logger.info("Fetching file content from: {}", url);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "token " + githubToken)
                        .header("Accept", "application/vnd.github.v3+json")
                        .timeout(Duration.ofMinutes(2))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 404) {
                    logger.warn("File not found: {}", filePath);
                    throw new RuntimeException("File not found: " + filePath);
                }
                
                if (response.statusCode() != 200) {
                    logger.error("GitHub API error for file {}: {} - {}", filePath, response.statusCode(), response.body());
                    throw new RuntimeException("Failed to fetch file content: " + response.statusCode());
                }
                
                GitHubFile file = parseGitHubFile(response.body());
                return decodeFileContent(file);
                
            } catch (Exception e) {
                logger.error("Failed to fetch file content for: {}", filePath, e);
                throw new RuntimeException("Failed to fetch file content: " + e.getMessage());
            }
        });
    }
    
    public CompletableFuture<List<GitHubFile>> getAllFiles() {
        return getAllFilesRecursive("");
    }
    
    private CompletableFuture<List<GitHubFile>> getAllFilesRecursive(String path) {
        return getRepositoryContents(path)
                .thenCompose(files -> {
                    List<GitHubFile> allFiles = new ArrayList<>();
                    List<CompletableFuture<Void>> futures = new ArrayList<>();
                    
                    for (GitHubFile file : files) {
                        if ("file".equals(file.getType())) {
                            allFiles.add(file);
                        } else if ("dir".equals(file.getType())) {
                            CompletableFuture<Void> dirFuture = getAllFilesRecursive(file.getPath())
                                    .thenAccept(allFiles::addAll);
                            futures.add(dirFuture);
                        }
                    }
                    
                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> allFiles);
                });
    }
    
    private List<GitHubFile> parseGitHubFiles(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<GitHubFile>>() {});
        } catch (Exception e) {
            logger.error("Failed to parse GitHub files response", e);
            throw new RuntimeException("Failed to parse GitHub response", e);
        }
    }
    
    private GitHubFile parseGitHubFile(String json) {
        try {
            return objectMapper.readValue(json, GitHubFile.class);
        } catch (Exception e) {
            logger.error("Failed to parse GitHub file response", e);
            throw new RuntimeException("Failed to parse GitHub file response", e);
        }
    }
    
    private GitHubFile decodeFileContent(GitHubFile file) {
        if (file.getContent() != null && "base64".equals(file.getEncoding())) {
            try {
                String decodedContent = new String(
                        Base64.getDecoder().decode(file.getContent().replace("\n", "")),
                        StandardCharsets.UTF_8
                );
                file.setContent(decodedContent);
            } catch (Exception e) {
                logger.error("Failed to decode file content for: {}", file.getPath(), e);
                file.setContent(""); // Set empty content if decoding fails
            }
        }
        return file;
    }
    
    public boolean isTextFile(String filename) {
        if (filename == null) return false;
        
        String lowerName = filename.toLowerCase();
        
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
}
