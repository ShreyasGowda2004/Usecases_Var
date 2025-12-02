
package com.aichatbot.service;

import com.aichatbot.dto.ChatRequest;
import com.aichatbot.dto.ChatResponse;
import com.aichatbot.model.DocumentEmbedding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Chat Service for Processing User Messages and Generating AI Responses.
 * 
 * This service is the core orchestrator of the RAG (Retrieval-Augmented Generation) system.
 * It coordinates document retrieval, context building, and AI response generation.
 * 
 * Architecture Flow:
 * 1. RETRIEVAL: Search for relevant document chunks using keyword-based matching
 * 2. AUGMENTATION: Build contextual prompt with retrieved documents
 * 3. GENERATION: Send prompt to Ollama AI for intelligent response
 * 
 * Key Features:
 * - Hybrid search with multiple fallback strategies
 * - Fast mode for quick responses (fewer chunks, lower threshold)
 * - Full content mode for comprehensive answers (entire file content)
 * - Smart LLM bypass for direct content queries
 * - Source sanitization for security (hides actual file paths)
 * - Session-based conversation tracking
 * 
 * Search Strategy (Multiple Fallbacks):
 * 1. Hybrid search: File-level scoring with keyword matching
 * 2. Best file approach: Find single best matching file, return all chunks
 * 3. Standard search: Basic keyword matching across all chunks
 * 4. Keyword fallback: Last resort with lower threshold
 * 
 * Performance Optimizations:
 * - Async processing with CompletableFuture
 * - Configurable chunk limits (6 for fast mode, 25 for normal)
 * - Relevance threshold tuning (0.6 for fast, 0.7 for normal)
 * - Direct response bypass for simple queries
 * 
 * IMPORTANT: This system uses KEYWORD-BASED search, NOT neural embeddings.
 * The term "embeddings" refers to text chunks, not vector embeddings.
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@Service
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private final DocumentProcessingService documentProcessingService;
    private final OllamaService ollamaService;
    
    @Value("${spring.ai.ollama.chat.model}")
    private String modelName;
    
    public ChatService(DocumentProcessingService documentProcessingService,
                      OllamaService ollamaService) {
        this.documentProcessingService = documentProcessingService;
        this.ollamaService = ollamaService;
    }
    
    public CompletableFuture<ChatResponse> processMessage(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : generateSessionId();
		final boolean fastMode = request.isFastMode();
		final boolean fullContent = request.isFullContent();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Processing message for session: {}", sessionId);
                
                // Get relevant context if requested - USE HYBRID SEARCH APPROACH
                List<DocumentEmbedding> relevantChunks = List.of();
                if (request.isIncludeContext()) {
                    if (fullContent) {
                        // Directly load ALL chunks from best matching file for fullContent mode (fast retrieval path)
                        relevantChunks = documentProcessingService.findBestMatchingFile(request.getMessage());
                    } else {
                        // Hybrid search placeholder (file-based scoring)
                        int maxChunks = fastMode ? 6 : 25;
                        relevantChunks = documentProcessingService.findSimilarDocumentsHybrid(request.getMessage(), maxChunks, fastMode ? 0.6 : 0.7);
                    }
                    
                    // FALLBACK 1: If hybrid search fails or returns no results, try best file approach
                    if (relevantChunks.isEmpty() && (!fastMode || fullContent)) { // allow deeper search / fallback
                        logger.info("Hybrid search returned no results, trying best file approach for: {}", request.getMessage());
                        relevantChunks = documentProcessingService.findBestMatchingFile(request.getMessage());
                    }
                    
                    // FALLBACK 2: If no best file found, try standard search
                    if (relevantChunks.isEmpty() && (!fastMode || fullContent)) {
                        logger.info("No best file found, trying standard search for: {}", request.getMessage());
                        relevantChunks = documentProcessingService.findRelevantChunks(request.getMessage(), 25);
                    }
                    
                    // FALLBACK 3: Last resort - keyword-based search
                    if (relevantChunks.isEmpty() && (!fastMode || fullContent)) {
                        logger.info("No results with standard search, trying keyword fallback for: {}", request.getMessage());
                        relevantChunks = documentProcessingService.findRelevantChunksByKeywords(request.getMessage(), 50);
                    }
                }
                
                // Determine if we should bypass the LLM for a faster response
                boolean bypassLLM = shouldBypassLLM(request.getMessage(), relevantChunks);

                String response;
                String responseModel = modelName;

                if (bypassLLM) {
                    logger.info("Bypassing LLM for fast response. Query: '{}'", request.getMessage());
                    response = formatDirectResponse(relevantChunks);
                    responseModel = "Direct Content";
                } else {
                    logger.info("Using LLM for intelligent response. Query: '{}'", request.getMessage());
                    // Always use LLM for intelligent analysis and extraction
                    // Build context-aware prompt with complete file data and let LLM analyze what to return
                    String contextualPrompt = buildContextualPrompt(request.getMessage(), relevantChunks);
                    response = ollamaService.generateResponse(contextualPrompt);
                }
                
                long responseTime = System.currentTimeMillis() - startTime;
                
                // Return sanitized source file references for security (no actual file paths)
                List<String> sourceFiles = relevantChunks.stream()
                        .map(this::sanitizeSourceFile)
                        .distinct()
                        .limit(1) // Return only the most relevant source file
                        .toList();
                
                logger.info("Successfully processed message in {}ms", responseTime);
                
                return ChatResponse.success(response, sessionId, responseTime, sourceFiles, responseModel);
                
            } catch (Exception e) {
                logger.error("Failed to process message for session: {}", sessionId, e);
                return ChatResponse.error("I apologize, but I encountered an error while processing your request. Please try again.", sessionId);
            }
        });
    }

    /**
     * Decide whether to bypass the LLM for a faster response.
     * This is true if the user asks for a complete guide, raw content, or a specific file.
     */
    private boolean shouldBypassLLM(String message, List<DocumentEmbedding> relevantChunks) {
        String lowerMessage = message.toLowerCase().trim();
        
        // Bypass if user asks for a complete guide or raw/unprocessed content
        boolean wantsCompleteGuide = lowerMessage.matches(".*\\b(setup|install|guide|complete|full|entire|all steps|walkthrough|show|give|get|display)\\b.*");
        boolean wantsRawContent = lowerMessage.matches(".*\\b(raw|exact|direct|unprocessed|just|only|file|document)\\b.*");
        
        // Bypass if the query is a direct file name
        boolean isFileRequest = lowerMessage.contains(".md") || lowerMessage.contains(".txt");

        return wantsCompleteGuide || wantsRawContent || isFileRequest;
    }

    /**
     * Format the response directly from the document chunks without LLM processing.
     */
    private String formatDirectResponse(List<DocumentEmbedding> chunks) {
        if (chunks.isEmpty()) {
            return "No matching file found for your request.";
        }
        
        StringBuilder fullContent = new StringBuilder();
        final String[] fileName = {null};
        
        chunks.stream()
            .sorted((a, b) -> Integer.compare(
                a.getChunkIndex() != null ? a.getChunkIndex() : 0,
                b.getChunkIndex() != null ? b.getChunkIndex() : 0))
            .forEach(chunk -> {
                if (fileName[0] == null) {
                    fileName[0] = chunk.getFilePath();
                }
                fullContent.append(chunk.getContentChunk()).append("\n");
            });
        
        return String.format("**File: %s**\n\n%s", fileName[0], fullContent.toString());
    }

    private String buildContextualPrompt(String userQuery, List<DocumentEmbedding> relevantChunks) {
        if (relevantChunks.isEmpty()) {
            return String.format("""
                USER QUESTION: %s
                
                No relevant documentation found for this query. Please try different keywords or check if the topic is covered under different terminology in the available documentation.
                """, userQuery);
        }
        
    // Check if user wants raw/exact content (keywords: "exact", "raw", "only", "just", "direct") 
    // OR if they want complete guides/setups
        boolean wantsRawContent = userQuery.toLowerCase().matches(".*\\b(only|just|exact|raw|direct|exactly)\\b.*");
        boolean wantsCompleteGuide = userQuery.toLowerCase().matches(".*\\b(setup|install|guide|complete|full|entire|all steps|walkthrough)\\b.*");
        
        StringBuilder contextBuilder = new StringBuilder();
        
        // Group chunks by file and include ALL chunks from the best matching file(s)
        Map<String, List<DocumentEmbedding>> chunksByFile = relevantChunks.stream()
                .collect(Collectors.groupingBy(DocumentEmbedding::getFilePath));
        
        // Process all files (since findBestMatchingFile already returns content from single best file)
        for (Map.Entry<String, List<DocumentEmbedding>> fileEntry : chunksByFile.entrySet()) {
            String filePath = fileEntry.getKey();
            List<DocumentEmbedding> fileChunks = fileEntry.getValue();
            
            contextBuilder.append("\n--- Content from: ").append(filePath).append(" ---\n");
            
            // Sort chunks by index to maintain original order and include ALL chunks
            fileChunks.stream()
                    .filter(chunk -> chunk.getContentChunk() != null && chunk.getContentChunk().length() > 20)
                    .sorted((a, b) -> Integer.compare(
                        a.getChunkIndex() != null ? a.getChunkIndex() : 0,
                        b.getChunkIndex() != null ? b.getChunkIndex() : 0))
                    .forEach(chunk -> {
                        contextBuilder.append(chunk.getContentChunk()).append("\n");
                    });
        }
        
        String contextContent = contextBuilder.toString();
        
        // If the user asks to "create" something, deterministically slice content
        // to only include Prerequisites and the specific Create section.
        // This prevents returning the entire guide for create-only questions.
        String lowered = userQuery.toLowerCase();
        if (lowered.contains("create ") || lowered.startsWith("create")) {
            String prereq = extractPrerequisites(contextContent);
            String createSection = extractOperationSection(contextContent, "create", userQuery);
            if (createSection != null && !createSection.isBlank()) {
                StringBuilder sliced = new StringBuilder();
                if (prereq != null && !prereq.isBlank()) {
                    sliced.append(prereq.trim()).append("\n\n---\n\n");
                }
                sliced.append(createSection.trim());
                contextContent = sliced.toString();
            }
        }
        
        // Check if prerequisites exist in the content - ALWAYS check this regardless of query type
        boolean hasPrerequisites = contextContent.toLowerCase().contains("prerequisite") || 
                                   contextContent.toLowerCase().contains("requirements") ||
                                   contextContent.toLowerCase().contains("before you begin") ||
                                   contextContent.toLowerCase().contains("before starting") ||
                                   contextContent.toLowerCase().contains("prereq");
        
    if (wantsRawContent || wantsCompleteGuide) {
            // Ultra-direct mode for raw content requests and complete setup guides
            return String.format("""
                Return the COMPLETE content related to: "%s"
                
                CRITICAL: Provide ALL steps, commands, and procedures from the document.
                Do NOT summarize, truncate, or skip any details.
                Include ALL download links, installation steps, configuration details, and verification commands.
                Maintain exact formatting, commands, file paths, and structure from the original documentation.
                When the document contains numbered steps, include ALL steps in order.
                
                CONTENT:
                %s
                
                RETURN COMPLETE CONTENT:
                """, userQuery, contextContent);
        } else if (hasPrerequisites) {
            // ANY query with prerequisites - show prerequisites first, then answer the question
            return String.format("""
                You are a technical documentation assistant. Return the EXACT, UNMODIFIED content from the repository.
                
                USER QUESTION: "%s"
                
                CRITICAL INSTRUCTIONS - RAW CONTENT ONLY:
                1. FIRST: Extract Prerequisites/Requirements section EXACTLY as written in the source
                2. SECOND: Extract the COMPLETE section that answers the user's question EXACTLY as written
                3. Include ALL steps, methods, URLs, headers, parameters, and request/response bodies for the requested operation
                4. Do NOT add explanations, extra text, formatting changes, or modifications
                5. Do NOT rewrite, paraphrase, or enhance the content
                6. Do NOT add introductions like "To create..." or "Follow these steps..."
                7. Return ONLY the raw content from the repository file
                8. Preserve exact formatting: headings, lists, code blocks, spacing
                9. Do NOT add markdown formatting that isn't in the original
                10. If they ask "create asset", show Prerequisites FIRST, then ONLY the "Create Asset" section with ALL details (method, URL, headers, params, body, response)
                
                COMPLETE DOCUMENTATION:
                %s
                
                RETURN RAW CONTENT FOR: %s (Prerequisites FIRST, then COMPLETE section for this specific operation)
                """, userQuery, contextContent, userQuery);
        } else {
            // Standard mode with emphasis on intelligent extraction based on user question
            return String.format("""
                You are an expert technical assistant. Analyze the user's question and extract ONLY the relevant information from the provided documentation.
                
                USER QUESTION: "%s"
                
                CRITICAL INSTRUCTIONS - BE SELECTIVE:
                1. Read and understand what the user is specifically asking for
                2. From the complete documentation below, extract ONLY the section(s) that directly answer their question
                3. Do NOT return the entire document or complete file content
                4. If they ask "how to create X", provide ONLY the creation steps, not query/update/delete operations
                5. If they ask "how to query X", provide ONLY the query examples, not creation/update operations  
                6. If they ask "how to update X", provide ONLY the update steps, not creation/query operations
                7. If they ask "how to delete X", provide ONLY the deletion steps, not creation/update operations
                8. Maintain the exact formatting, commands, and structure from the original documentation
                9. Include ALL necessary details for the specific operation they're asking about
                10. Do NOT include unrelated operations, sections, or topics from the same file
                11. Be focused and targeted - users want specific answers, not entire documents
                
                COMPLETE DOCUMENTATION:
                %s
                
                EXTRACT AND PROVIDE ONLY THE SPECIFIC SECTION THAT ANSWERS: %s
                """, userQuery, contextContent, userQuery);
        }
    }
    
    /**
     * Extract the Prerequisites section from a markdown document.
     * Matches a heading containing "Prerequisite" (any level) and captures until the next heading or EOF.
     */
    private String extractPrerequisites(String content) {
        // Use a multiline, dotall regex to capture from a heading containing "Prerequisite"
        // until the next heading (lines starting with #) or end of file.
        String pattern = "(?ms)^(#+\\s*Prerequisite[s]?\\b.*?)(?=^#+\\s|\\\\z)";
        return extractFirstMatch(content, pattern);
    }
    
    /**
     * Extract a specific operation section (e.g., Create/Query/Update/Delete) from a markdown guide.
     * When multiple "Create" sections exist, the heading that best matches the user query is returned.
     */
    private String extractOperationSection(String content, String operation, String userQuery) {
        if (content == null || content.isBlank() || operation == null || operation.isBlank()) {
            return null;
        }

        String op = operation.trim();
        List<String> candidates = new ArrayList<>();
        String opPattern = java.util.regex.Pattern.quote(op);

        addOperationMatches(candidates, content,
                String.format("(?ms)(?i)^(##\\s*\\d+\\.\\s*[^\\n]*\\b%s\\b[^\\n]*\n[\\s\\S]*?)(?=^##\\s|\\z)", opPattern));
        addOperationMatches(candidates, content,
                String.format("(?ms)(?i)^(##\\s*[^\\n]*\\b%s\\b[^\\n]*\n[\\s\\S]*?)(?=^##\\s|\\z)", opPattern));
        addOperationMatches(candidates, content,
                String.format("(?ms)(?i)^(#+\\s*[^\\n]*\\b%s\\b[^\\n]*\n[\\s\\S]*?)(?=^#+\\s|\\z)", opPattern));

        if (candidates.isEmpty()) {
            return null;
        }

        if (userQuery != null && !userQuery.isBlank()) {
            String normalizedQuery = normalizeForComparison(userQuery);
            if (!normalizedQuery.isEmpty()) {
                String bestCandidate = null;
                int bestScore = Integer.MIN_VALUE;

                for (String section : candidates) {
                    String heading = section.split("\\n", 2)[0];
                    String normalizedHeading = normalizeForComparison(heading);

                    if (!normalizedQuery.isEmpty() && normalizedHeading.contains(normalizedQuery)) {
                        return section;
                    }

                    int score = computeKeywordOverlapScore(normalizedHeading, normalizedQuery);
                    if (score > bestScore) {
                        bestScore = score;
                        bestCandidate = section;
                    }
                }

                if (bestCandidate != null) {
                    return bestCandidate;
                }
            }
        }

        return candidates.get(0);
    }
    
    private void addOperationMatches(List<String> target, String content, String regex) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String match = matcher.group(1);
            if (match != null && !match.isBlank()) {
                target.add(match);
            }
        }
    }

    private String normalizeForComparison(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private int computeKeywordOverlapScore(String heading, String query) {
        if (heading == null || heading.isEmpty() || query == null || query.isEmpty()) {
            return 0;
        }

        Set<String> headingTokens = new HashSet<>(Arrays.asList(heading.split(" ")));
        int score = 0;

        for (String token : query.split(" ")) {
            String trimmed = token.trim();
            if (trimmed.length() < 3) {
                continue;
            }
            if (headingTokens.contains(trimmed)) {
                score += 2;
            } else if (heading.contains(trimmed)) {
                score += 1;
            }
        }

        return score;
    }

    private String extractFirstMatch(String content, String regex) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher m = p.matcher(content);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
    
    // No persistence of chat history, so no need to extract file names for storage
    
    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }
    
    // Chat history is intentionally not persisted
    
    /**
     * Sanitize source file paths for security - returns generic descriptions instead of actual paths
     */
    private String sanitizeSourceFile(DocumentEmbedding embedding) {
        String filePath = embedding.getFilePath();
        
        // Extract meaningful category from file path without revealing internal structure
        if (filePath.contains("db2")) {
            return "Database Configuration Guide";
        } else if (filePath.contains("maximo") && filePath.contains("install")) {
            return "Maximo Installation Guide";
        } else if (filePath.contains("maximo") && filePath.contains("setup")) {
            return "Maximo Setup Guide";
        } else if (filePath.contains("liberty")) {
            return "WebSphere Liberty Configuration";
        } else if (filePath.contains("mongo")) {
            return "MongoDB Configuration Guide";
        } else if (filePath.contains("java")) {
            return "Java Configuration Guide";
        } else if (filePath.contains("openshift")) {
            return "OpenShift Deployment Guide";
        } else if (filePath.contains("system")) {
            return "System Configuration Guide";
        } else if (filePath.contains("restapi")) {
            return "REST API Documentation";
        } else if (filePath.contains("manage")) {
            return "Maximo Manage Configuration";
        } else if (filePath.contains("mas-suite")) {
            return "MAS Suite Installation Guide";
        } else {
            return "Technical Documentation";
        }
    }
}
