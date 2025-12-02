package com.aichatbot.controller;

import com.aichatbot.config.GitHubRepositoryConfig;
import com.aichatbot.dto.GitHubFile;
import com.aichatbot.service.GitHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub Controller for Repository Content Access.
 * 
 * This REST controller provides endpoints for browsing and retrieving
 * content from configured GitHub repositories directly via the API.
 * 
 * Key Features:
 * - List all files across configured repositories
 * - Retrieve file content with syntax highlighting
 * - Extract API sections from documentation files
 * - Repository configuration access
 * - No local cloning required (direct API access)
 * 
 * Endpoints:
 * - GET /github/files: List all files from configured repositories
 * - GET /github/content: Get file content by path and repository
 * - GET /github/api-section: Extract specific API section from file
 * - GET /github/config: Get repository configuration
 * 
 * API Section Extraction:
 * Parses markdown files to extract specific API documentation sections
 * based on headers and code blocks. Useful for focused API exploration.
 * 
 * Use Cases:
 * - Browse repository documentation
 * - View file content with syntax highlighting
 * - Execute API examples from documentation
 * - Navigate multi-repository knowledge base
 * 
 * Performance:
 * - Uses caching to reduce GitHub API calls
 * - Async operations for non-blocking I/O
 * - Efficient Base64 decoding for file content
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/github")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:5173"}, allowCredentials = "true")
public class GitHubController {

    private static final Logger logger = LoggerFactory.getLogger(GitHubController.class);

    private final GitHubService gitHubService;
    private final GitHubRepositoryConfig repositoryConfig;

    public GitHubController(GitHubService gitHubService, GitHubRepositoryConfig repositoryConfig) {
        this.gitHubService = gitHubService;
        this.repositoryConfig = repositoryConfig;
    }

    @GetMapping("/file")
    public ResponseEntity<?> getFile(@RequestParam(required = false) String url,
                                     @RequestParam(required = false) String path,
                                     @RequestParam(required = false, name = "repo") String repoFullName,
                                     @RequestParam(required = false) String branch) {
        try {
            final String[] ownerHolder = { null };
            final String[] repoHolder = { null };
            String filePath = path;
            String ref = branch;

            if (url != null && !url.isBlank()) {
                // Support GitHub and GitHub Enterprise URL formats like:
                // https://github.ibm.com/<owner>/<repo>/blob/<branch>/<path>
                // https://github.com/<owner>/<repo>/blob/<branch>/<path>
                // Also accept raw URLs: .../<owner>/<repo>/raw/<branch>/<path>
                Pattern p = Pattern.compile("https?://[a-zA-Z0-9.-]+/([^/]+)/([^/]+)/(?:blob|raw)/([^/]+)/(.+)");
                Matcher m = p.matcher(url);
                if (m.find()) {
                    ownerHolder[0] = m.group(1);
                    repoHolder[0] = m.group(2);
                    ref = (ref == null || ref.isBlank()) ? m.group(3) : ref;
                    filePath = m.group(4);
                } else {
                    return ResponseEntity.badRequest().body(Map.of("error", "Unsupported GitHub URL format", "url", url));
                }
            } else if (repoFullName != null && filePath != null) {
                String[] parts = repoFullName.split("/");
                if (parts.length == 2) {
                    ownerHolder[0] = parts[0];
                    repoHolder[0] = parts[1];
                }
            }

            if (filePath == null || filePath.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing file path"));
            }

            // Find repository from configuration if possible
            GitHubRepositoryConfig.Repository resolvedRepo = null;
            if (ownerHolder[0] != null && repoHolder[0] != null) {
                Optional<GitHubRepositoryConfig.Repository> match = repositoryConfig.getRepositories().stream()
                        .filter(r -> ownerHolder[0].equalsIgnoreCase(r.getOwner()) && repoHolder[0].equalsIgnoreCase(r.getName()))
                        .findFirst();
                if (match.isPresent()) {
                    resolvedRepo = match.get();
                }
            }

            if (resolvedRepo == null) {
                // Fall back to the first configured repository if only one exists
                if (repositoryConfig.getRepositories() != null && !repositoryConfig.getRepositories().isEmpty()) {
                    resolvedRepo = repositoryConfig.getRepositories().get(0);
                } else {
                    return ResponseEntity.badRequest().body(Map.of("error", "No repositories configured"));
                }
            }

            // If a branch/ref is provided via URL or query param, clone the repo config with that branch
            GitHubRepositoryConfig.Repository repoForFetch = resolvedRepo;
            if (ref != null && !ref.isBlank() && !ref.equalsIgnoreCase(resolvedRepo.getBranch())) {
                GitHubRepositoryConfig.Repository temp = new GitHubRepositoryConfig.Repository();
                temp.setOwner(resolvedRepo.getOwner());
                temp.setName(resolvedRepo.getName());
                temp.setBranch(ref);
                repoForFetch = temp;
            }

            GitHubFile file = gitHubService.getFileContent(repoForFetch, filePath).join();
            if (file.getContent() == null || file.getContent().isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "File not found or empty", "path", filePath));
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("name", file.getName());
            resp.put("path", file.getPath());
            resp.put("repository", repoForFetch.getFullName());
            resp.put("branch", repoForFetch.getBranch());
            resp.put("content", file.getContent());
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            logger.error("Failed to fetch GitHub file", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch GitHub file", "details", e.getMessage()));
        }
    }
}
