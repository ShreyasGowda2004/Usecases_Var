package com.aichatbot.service;

import com.aichatbot.model.DocumentEmbedding;
import com.aichatbot.repository.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Document Processing Service for Keyword-Based Search and Retrieval.
 * 
 * This service handles all document processing operations including:
 * - Text chunking and storage
 * - Keyword-based search with relevance scoring
 * - File-level ranking and scoring
 * - Repository management and cleanup
 * 
 * Search Algorithm (Keyword-Based):
 * The service implements a sophisticated keyword matching algorithm with multiple scoring factors:
 * 
 * 1. Content Matching:
 *    - Exact word matches (15 points per match)
 *    - Partial matches and variations
 *    - Term frequency analysis
 * 
 * 2. Filename Relevance:
 *    - Filename keyword matches (20-120 points)
 *    - Plural/singular variations
 *    - Special domain term bonuses
 *    - Irrelevant file penalties
 * 
 * 3. File-Level Scoring:
 *    - Top-K chunk averaging (uses best 5 chunks from each file)
 *    - Prevents dilution from long files with few relevant chunks
 *    - Filename bonus aggregation
 * 
 * Key Features:
 * - Caching for frequently accessed queries
 * - Async document processing
 * - Repository-level chunk management
 * - Hybrid search strategies
 * - Fallback mechanisms for edge cases
 * 
 * Performance:
 * - In-memory search for fast retrieval
 * - Suitable for 100K+ document chunks
 * - No external database required
 * - Spring caching for repeated queries
 * 
 * IMPORTANT: Despite the method names containing "embedding",
 * this service does NOT use neural embeddings or vector similarity.
 * It uses pure keyword-based search with intelligent scoring.
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@Service
public class DocumentProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);
    
    private final EmbeddingStore embeddingStore;
    
    // Inner class for scoring
    private static class ScoredEmbedding {
        final DocumentEmbedding embedding;
        final double score;
        
        ScoredEmbedding(DocumentEmbedding embedding, double score) {
            this.embedding = embedding;
            this.score = score;
        }
    }
    
    public DocumentProcessingService(EmbeddingStore embeddingStore) {
        this.embeddingStore = embeddingStore;
    }
    
    /**
     * Find the single best matching file and return ALL content from that file
     */
    public List<DocumentEmbedding> findBestMatchingFile(String query) {
        logger.info("Finding best matching file for: {}", query);
        
    List<DocumentEmbedding> allEmbeddings = embeddingStore.findAll();
        String normalizedQuery = query.toLowerCase().trim();
        String[] queryWords = normalizedQuery.split("\\s+");
        
        // Group all chunks by file path and calculate file-level scores
        Map<String, List<DocumentEmbedding>> fileGroups = allEmbeddings.stream()
                .collect(Collectors.groupingBy(DocumentEmbedding::getFilePath));
        
        // Calculate file-level score using Top-K chunk scores to avoid dilution in long files
        String bestFilePath = fileGroups.entrySet().stream()
                .map(entry -> {
                    String filePath = entry.getKey();
                    List<DocumentEmbedding> chunks = entry.getValue();
                    
                    // Compute individual chunk scores
                    List<Double> scores = chunks.stream()
                        .map(chunk -> calculateRelevanceScore(
                            chunk.getContentChunk(),
                            chunk.getFilePath(),
                            queryWords,
                            normalizedQuery
                        ))
                        .sorted(Comparator.reverseOrder())
                        .collect(Collectors.toList());

                    // Use Top-K average (e.g., top 5) to represent file strength
                    int k = Math.min(5, scores.size());
                    double topKAvg = scores.stream().limit(k).mapToDouble(Double::doubleValue).average().orElse(0.0);
                    
                    // Bonus for file name matching
                    double fileNameBonus = calculateFilenameRelevance(filePath, queryWords);

                    return new FileScore(filePath, topKAvg + fileNameBonus);
                })
                .max(Comparator.comparing(fs -> fs.score))
                .map(fs -> fs.filePath)
                .orElse(null);
        
        if (bestFilePath != null) {
            logger.info("Best matching file: {} ", bestFilePath);
            // Return ALL chunks from the best matching file, ordered by chunk index
            return fileGroups.get(bestFilePath).stream()
                    .sorted(Comparator.comparing(DocumentEmbedding::getChunkIndex))
                    .collect(Collectors.toList());
        }
        
        logger.warn("No matching file found for query: {}", query);
        return Collections.emptyList();
    }
    
    /**
     * Calculate filename relevance score with enhanced plural/singular matching
     */
    private double calculateFilenameRelevance(String filePath, String[] queryWords) {
        String fileName = filePath.toLowerCase();
        double score = 0.0;
        
        // Expand queryWords to include plural/singular variations
        Set<String> expandedWords = new HashSet<>();
        for (String word : queryWords) {
            expandedWords.add(word.toLowerCase());
            // Add plural/singular variations
            if (word.endsWith("s") && word.length() > 3) {
                expandedWords.add(word.substring(0, word.length() - 1)); // Remove 's'
            } else {
                expandedWords.add(word + "s"); // Add 's'
            }
            // Special cases
            if (word.equalsIgnoreCase("commodity")) {
                expandedWords.add("commodities");
            }
            if (word.equalsIgnoreCase("commodities")) {
                expandedWords.add("commodity");
            }
            if (word.equalsIgnoreCase("organization") || word.equalsIgnoreCase("organisation")) {
                expandedWords.add("organization");
                expandedWords.add("org");
                expandedWords.add("site");
            }
        }
        
        for (String word : expandedWords) {
            if (word.length() > 2 && fileName.contains(word)) {
                score += 20.0; // High bonus for filename matches
            }
        }
        
        // Special bonuses for specific terms to ensure exact file matching
        if (fileName.contains("commodity") || fileName.contains("commodities")) {
            if (containsAnyOf(String.join(" ", queryWords), Arrays.asList("commodity", "commodities", "get", "item"))) {
                score += 100.0; // Very high bonus for commodity-related queries
            }
        }
        
        if (fileName.contains("liberty") && containsAnyOf(String.join(" ", queryWords), Arrays.asList("liberty", "setup", "install", "maximo"))) {
            score += 100.0; // Very high bonus for liberty setup files
        }
        
        if (fileName.contains("setup") && containsAnyOf(String.join(" ", queryWords), Arrays.asList("setup", "install", "configure"))) {
            score += 50.0;
        }
        
        if (fileName.contains("maximo") && containsAnyOf(String.join(" ", queryWords), Arrays.asList("maximo", "liberty", "setup"))) {
            score += 50.0;
        }

        // Strongly prefer Organization/Site docs for organization-related queries
        String queryJoined = String.join(" ", queryWords).toLowerCase();
        boolean asksOrganization = containsAnyOf(queryJoined, Arrays.asList("organization", "organisation", "org", "create organization", "how to create organization", "site"));
        if (asksOrganization) {
            if (fileName.contains("organization") || fileName.contains("organisation") || fileName.contains("org") || fileName.contains("site")) {
                score += 120.0; // Strong boost to pick the intended guide
            }
            // Penalize GL Components/COA when asking for organization creation
            if (fileName.contains("glcomponents") || fileName.contains("gl-components") || fileName.contains("coa")) {
                score -= 80.0;
            }
        }
        
        // Penalize irrelevant files
        if (fileName.contains("java") && !containsAnyOf(String.join(" ", queryWords), Arrays.asList("java", "class", "code"))) {
            score -= 30.0;
        }
        
        return score;
    }
    
    /**
     * Helper class for file scoring
     */
    private static class FileScore {
        final String filePath;
        final double score;
        
        FileScore(String filePath, double score) {
            this.filePath = filePath;
            this.score = score;
        }
    }
    
    /**
     * Main search method with enhanced content-focused scoring
     */
    @Cacheable(value = "relevant-chunks-cache", key = "#query + '_' + #maxResults")
    public List<DocumentEmbedding> findRelevantChunks(String query, int maxResults) {
        logger.info("Searching for relevant chunks for query: {}", query);
        
    List<DocumentEmbedding> allEmbeddings = embeddingStore.findAll();
        if (allEmbeddings.isEmpty()) {
            logger.warn("No embeddings found in repository");
            return new ArrayList<>();
        }
        
        String normalizedQuery = query.toLowerCase().trim();
        String[] queryWords = normalizedQuery.split("\\s+");
        
        return allEmbeddings.stream()
                .map(embedding -> {
                    double score = calculateRelevanceScore(
                        embedding.getContentChunk(), 
                        embedding.getFilePath(), 
                        queryWords, 
                        normalizedQuery
                    );
                    return new ScoredEmbedding(embedding, score);
                })
                .filter(scored -> scored.score > 0.1) // Filter very low scores
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(maxResults)
                .map(scored -> scored.embedding)
                .collect(Collectors.toList());
    }
    
    /**
     * Relaxed threshold search for broader results
     */
    public List<DocumentEmbedding> findRelevantChunksWithLowerThreshold(String query, int maxResults) {
        logger.info("Performing relaxed threshold search for: {}", query);
        
    List<DocumentEmbedding> allEmbeddings = embeddingStore.findAll();
        String queryLower = query.toLowerCase();
        String[] queryWords = queryLower.split("\\s+");
        
        return allEmbeddings.stream()
                .map(embedding -> {
                    double score = calculateRelaxedScore(embedding, queryWords, queryLower);
                    return new ScoredEmbedding(embedding, score);
                })
                .filter(scored -> scored.score > 0.001) // Very low threshold for maximum coverage
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(maxResults)
                .map(scored -> scored.embedding)
                .collect(Collectors.toList());
    }
    
    /**
     * Keyword-based fallback search
     */
    @Cacheable(value = "keyword-search-cache", key = "#query + '_' + #maxResults")
    public List<DocumentEmbedding> findRelevantChunksByKeywords(String query, int maxResults) {
        logger.info("Performing keyword-based fallback search for: {}", query);
        
    List<DocumentEmbedding> allEmbeddings = embeddingStore.findAll();
        String[] keywords = query.toLowerCase().split("\\s+");
        
        return allEmbeddings.stream()
                .map(embedding -> {
                    double score = calculateKeywordScore(embedding, keywords);
                    return new ScoredEmbedding(embedding, score);
                })
                .filter(scored -> scored.score > 0.5)
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(maxResults)
                .map(scored -> scored.embedding)
                .collect(Collectors.toList());
    }
    
    /**
     * Content-focused relevance scoring (reduced filename bias)
     */
    private double calculateRelevanceScore(String content, String fileName, String[] queryWords, String fullQuery) {
        double score = 0.0;
        
        String lowerContent = content.toLowerCase();
        String lowerFileName = fileName.toLowerCase();
        String lowerQuery = fullQuery.toLowerCase();
        
        String[] normalizedQueryWords = Arrays.stream(queryWords)
                .map(String::toLowerCase)
                .map(String::trim)
                .filter(word -> !word.isEmpty() && word.length() > 1)
                .toArray(String[]::new);
        
        // CONTENT SCORING (Primary importance)
        int wordsInContent = 0;
        double contentScore = 0.0;
        
        for (String word : normalizedQueryWords) {
            if (word.length() > 2) {
                // Exact word boundary match in content
                if (lowerContent.contains(" " + word + " ") || 
                    lowerContent.startsWith(word + " ") || 
                    lowerContent.endsWith(" " + word)) {
                    contentScore += 15.0;
                    wordsInContent++;
                }
                // Partial word match in content
                else if (lowerContent.contains(word)) {
                    contentScore += 8.0;
                    wordsInContent++;
                }
                
                // Try normalized form if no match
                if (!lowerContent.contains(word)) {
                    String normalized = normalizeWord(word);
                    if (lowerContent.contains(normalized)) {
                        contentScore += 5.0;
                        wordsInContent++;
                    }
                }
            }
        }
        
        // FILENAME SCORING (Secondary importance - much reduced)
        int wordsInFileName = 0;
        double filenameScore = 0.0;
        
        for (String word : normalizedQueryWords) {
            if (word.length() > 2) {
                if (lowerFileName.contains(word)) {
                    filenameScore += 3.0; // Drastically reduced from 5000+
                    wordsInFileName++;
                }
                
                // Try normalized form
                String normalized = normalizeWord(word);
                if (lowerFileName.contains(normalized)) {
                    filenameScore += 2.0;
                }
            }
        }
        
        // EXACT PHRASE MATCHING (Highest priority)
        if (lowerContent.contains(lowerQuery)) {
            score += 50.0;
        }
        
        // MULTI-WORD PROXIMITY BONUS
        if (normalizedQueryWords.length > 1) {
            for (int i = 0; i < normalizedQueryWords.length - 1; i++) {
                String word1 = normalizedQueryWords[i];
                String word2 = normalizedQueryWords[i + 1];
                String phrase = word1 + " " + word2;
                if (lowerContent.contains(phrase)) {
                    score += 25.0;
                }
            }
        }
        
        // DOMAIN-SPECIFIC BONUSES AND SEMANTIC SCORING
        double semanticScore = 0.0;
        
        // Enhanced semantic patterns with keyword expansion
        Set<String> expandedWords = expandKeywords(normalizedQueryWords);
        String[] allWords = expandedWords.toArray(new String[0]);
        
        // Use expanded keywords for enhanced content scoring
        for (String word : allWords) {
            if (word.length() > 2 && !Arrays.asList(normalizedQueryWords).contains(word)) {
                // Score additional expanded keywords
                if (lowerContent.contains(word)) {
                    contentScore += 5.0; // Lower score for expanded keywords
                    wordsInContent++;
                }
            }
        }
        
        if (containsAnyOf(lowerQuery, Arrays.asList("how to create", "create", "creating", "setup", "configure"))) {
            if (containsAnyOf(lowerContent, Arrays.asList("create", "setup", "configure", "build", "make", "generate"))) {
                semanticScore += 30.0; // High weight for creation semantics
            }
        }

        // Strong semantic boost for Organization creation queries
        boolean queryOrgCreate = containsAnyOf(lowerQuery, Arrays.asList(
            "how to create organization",
            "create organization",
            "creating organization",
            "create org",
            "creating org"
        ));
        if (queryOrgCreate) {
            if (containsAnyOf(lowerContent, Arrays.asList("how to create organization", "create organization", "creating organization", "create site", "organization", "site"))) {
                semanticScore += 60.0; // Very strong signal
            }
        }
        
        if (containsAnyOf(lowerQuery, Arrays.asList("tablespace", "table space", "tablespaces"))) {
            if (containsAnyOf(lowerContent, Arrays.asList("tablespace", "table space", "maxindex", "maxdata", "db2 create"))) {
                semanticScore += 40.0; // Very high weight for tablespace content
            }
        }
        
        if (lowerQuery.contains("tablespace") || lowerQuery.contains("database") || lowerQuery.contains("db2")) {
            if (lowerContent.contains("tablespace") || lowerContent.contains("database") || lowerContent.contains("db2")) {
                semanticScore += 25.0;
            }
        }
        
        if (lowerQuery.contains("config") || lowerQuery.contains("prerequisite")) {
            if (lowerContent.contains("configuration") || lowerContent.contains("prerequisite") || lowerContent.contains("setup")) {
                semanticScore += 20.0;
            }
        }
        
        if (containsAnyOf(lowerQuery, Arrays.asList("maximo", "mas"))) {
            if (containsAnyOf(lowerContent, Arrays.asList("maximo", "mas", "manage"))) {
                semanticScore += 15.0;
            }
        }
        
        // Combine scores with semantic matching having highest priority
        score += semanticScore * 4.0; // Semantic gets highest weight (increased from 3.0)
        score += contentScore * 3.0; // Content is 3x more important (increased from 2.0)
        score += filenameScore; // Filename gets standard weight
        
        // Small bonus for having words in both filename and content
        if (wordsInFileName > 0 && wordsInContent > 0) {
            score += 5.0; // Reduced bonus to prevent over-weighting
        }
        
        // Enhanced bonus for high content match ratio
        double contentMatchRatio = (double) wordsInContent / Math.max(normalizedQueryWords.length, 1);
        if (contentMatchRatio >= 0.7) {
            score += 40.0; // Increased from 30.0
        } else if (contentMatchRatio >= 0.5) {
            score += 20.0; // Increased from 15.0
        }
        
        return score;
    }
    
    /**
     * Relaxed scoring for broader search with semantic expansion
     */
    private double calculateRelaxedScore(DocumentEmbedding embedding, String[] queryWords, String queryLower) {
        String content = embedding.getContentChunk().toLowerCase();
        String filePath = embedding.getFilePath().toLowerCase();
        
        double score = 0.0;
        
        // Use keyword expansion for broader coverage
        Set<String> expandedKeywords = expandKeywords(queryWords);
        
        // Simple word matching - more forgiving with expanded keywords
        for (String word : expandedKeywords) {
            if (word.length() > 2) {
                // Exact word match in content
                if (content.contains(" " + word + " ") || content.startsWith(word + " ") || content.endsWith(" " + word)) {
                    score += 2.0;
                }
                // Partial word match in content
                else if (content.contains(word)) {
                    score += 1.0;
                }
                
                // Word in filename gets bonus but not overwhelming
                if (filePath.contains(word)) {
                    score += 1.5;
                }
                
                // Add fuzzy matching for partial words
                if (content.contains(word.substring(0, Math.min(word.length(), 4)))) {
                    score += 0.5; // Small bonus for partial matches
                }
            }
        }
        
        // Special scoring for common DB/Maximo terms
        if (content.contains("tablespace") || content.contains("db2") || content.contains("database")) {
            if (queryLower.contains("tablespace") || queryLower.contains("db2") || queryLower.contains("database")) {
                score += 3.0;
            }
        }
        
        // Special scoring for configuration terms
        if (content.contains("configuration") || content.contains("config") || content.contains("prerequisite")) {
            if (queryLower.contains("config") || queryLower.contains("prerequisite")) {
                score += 2.0;
            }
        }
        
        // Boost for exact phrase matches
        if (content.contains(queryLower)) {
            score += 3.0;
        }
        
        return score;
    }
    
    /**
     * Keyword-based scoring with semantic expansion
     */
    private double calculateKeywordScore(DocumentEmbedding embedding, String[] keywords) {
        String content = embedding.getContentChunk().toLowerCase();
        String filePath = embedding.getFilePath().toLowerCase();
        
        double score = 0.0;
        
        // Expand keywords with semantic variations
        Set<String> expandedKeywords = expandKeywords(keywords);
        
        for (String keyword : expandedKeywords) {
            if (keyword.length() > 2) {
                long contentMatches = countOccurrences(content, keyword);
                long pathMatches = countOccurrences(filePath, keyword);
                
                score += contentMatches * 2.0; // Content matches weighted higher
                score += pathMatches * 1.0; // Path matches get lower weight
                
                // Add fuzzy matching for partial words
                if (contentMatches == 0 && content.contains(keyword.substring(0, Math.min(keyword.length(), 4)))) {
                    score += 0.5; // Small bonus for partial matches
                }
            }
        }
        
        return score;
    }
    
    /**
     * Expand keywords with semantic variations and synonyms
     */
    private Set<String> expandKeywords(String[] keywords) {
        Set<String> expanded = new HashSet<>();
        
        for (String keyword : keywords) {
            String lower = keyword.toLowerCase();
            expanded.add(lower);
            
            // Add common variations and synonyms
            switch (lower) {
                case "create":
                    expanded.addAll(Arrays.asList("creating", "creation", "setup", "configure", "build", "make", "generate"));
                    break;
                case "organization":
                case "organisation":
                case "org":
                    expanded.addAll(Arrays.asList("organization", "organisation", "org", "site", "sites"));
                    break;
                case "commodity":
                case "commodities":
                    expanded.addAll(Arrays.asList("commodity", "commodities", "item", "items", "product", "products"));
                    break;
                case "get":
                    expanded.addAll(Arrays.asList("get", "retrieve", "fetch", "obtain", "access", "find"));
                    break;
                case "tablespace":
                case "tablespaces":
                    expanded.addAll(Arrays.asList("tablespace", "tablespaces", "table space", "table spaces", "database space", "db space"));
                    break;
                case "how":
                    expanded.addAll(Arrays.asList("how", "steps", "procedure", "process", "method", "way", "guide"));
                    break;
                case "to":
                    // Skip common words
                    break;
                case "install":
                    expanded.addAll(Arrays.asList("install", "installation", "installing", "deploy", "deployment", "setup"));
                    break;
                case "configure":
                case "configuration":
                    expanded.addAll(Arrays.asList("configure", "configuration", "config", "setup", "setting", "settings"));
                    break;
                case "maximo":
                    expanded.addAll(Arrays.asList("maximo", "mas", "manage"));
                    break;
                case "db2":
                    expanded.addAll(Arrays.asList("db2", "database", "db"));
                    break;
                case "prerequisite":
                case "prerequisites":
                    expanded.addAll(Arrays.asList("prerequisite", "prerequisites", "requirement", "requirements", "prereq"));
                    break;
                default:
                    // Add plural/singular variations
                    if (lower.endsWith("s") && lower.length() > 3) {
                        expanded.add(lower.substring(0, lower.length() - 1)); // Remove 's'
                    } else {
                        expanded.add(lower + "s"); // Add 's'
                    }
                    break;
            }
        }
        
        return expanded;
    }
    
    /**
     * Helper method to check if text contains any of the given phrases
     */
    private boolean containsAnyOf(String text, java.util.List<String> phrases) {
        return phrases.stream().anyMatch(text::contains);
    }
    
    /**
     * Word normalization
     */
    private String normalizeWord(String word) {
        if (word == null || word.length() < 3) return word;
        
        String lower = word.toLowerCase();
        
        // Handle common plural patterns
        if (lower.endsWith("ies") && lower.length() > 4) {
            return lower.substring(0, lower.length() - 3) + "y";
        }
        if (lower.endsWith("s") && !lower.endsWith("ss") && lower.length() > 3) {
            return lower.substring(0, lower.length() - 1);
        }
        
        return lower;
    }
    
    /**
     * Count occurrences of a keyword in text
     */
    private long countOccurrences(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty()) {
            return 0;
        }
        return Arrays.stream(text.split("\\W+"))
                .filter(word -> word.equalsIgnoreCase(keyword))
                .count();
    }
    
    /**
     * Process a document and store its embeddings
     */
    public CompletableFuture<Void> processDocument(String filePath, String content, String repositoryOwner, String repositoryName, String branch) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Processing document: {}", filePath);
            
            // Split content into chunks
            List<String> chunks = splitIntoChunks(content, 3000); // 3000 char chunks for longer documentation like OpenShift guides
            
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                if (chunk.trim().length() > 50) { // Skip very short chunks
                    DocumentEmbedding embedding = new DocumentEmbedding();
                    embedding.setFilePath(filePath);
                    embedding.setContentChunk(chunk);
                    embedding.setChunkIndex(i);
                    embedding.setRepositoryOwner(repositoryOwner);
                    embedding.setRepositoryName(repositoryName);
                    embedding.setBranchName(branch);
                    
                    // Save to the configured store (file by default)
                    embeddingStore.save(embedding);
                }
            }
            
            logger.info("Processed {} chunks for file: {}", chunks.size(), filePath);
        });
    }
    
    /**
     * Split content into manageable chunks
     */
    private List<String> splitIntoChunks(String content, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        
        if (content == null || content.trim().isEmpty()) {
            return chunks;
        }
        
        // Split by paragraphs first
        String[] paragraphs = content.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() <= maxChunkSize) {
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            } else {
                // Add current chunk if it has content
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }
                
                // If paragraph is too long, split by sentences
                if (paragraph.length() > maxChunkSize) {
                    String[] sentences = paragraph.split("\\. ");
                    for (String sentence : sentences) {
                        if (currentChunk.length() + sentence.length() <= maxChunkSize) {
                            if (currentChunk.length() > 0) {
                                currentChunk.append(". ");
                            }
                            currentChunk.append(sentence);
                        } else {
                            if (currentChunk.length() > 0) {
                                chunks.add(currentChunk.toString());
                                currentChunk = new StringBuilder();
                            }
                            currentChunk.append(sentence);
                        }
                    }
                } else {
                    currentChunk.append(paragraph);
                }
            }
        }
        
        // Add final chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }

    /**
     * Search for similar documents using MongoDB text search
     */
    public List<DocumentEmbedding> findSimilarDocumentsHybrid(String query, int limit, double threshold) {
        logger.info("Searching for documents related to query: {}", query);
        return findBestMatchingFile(query);
    }
    
    /**
     * Repository reprocessing
     */
    public CompletableFuture<Void> reprocessRepository(String repositoryOwner, String repositoryName) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Starting repository reprocessing for {}/{}", repositoryOwner, repositoryName);
            try {
                // Delete all existing embeddings for this repository (MongoDB operation)
                embeddingStore.deleteByRepositoryOwnerAndRepositoryName(repositoryOwner, repositoryName);
                logger.info("Deleted existing embeddings for {}/{}", repositoryOwner, repositoryName);
            } catch (Exception e) {
                logger.warn("Failed to delete existing embeddings for {}/{}", repositoryOwner, repositoryName, e);
            }
            logger.info("Repository reprocessing completed for {}/{}", repositoryOwner, repositoryName);
        });
    }
}
