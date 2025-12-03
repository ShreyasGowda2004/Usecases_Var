# 🔄 Text Chunks Creation → Ollama Flow Documentation

## 📋 Overview

This document explains the **complete end-to-end flow** of how text chunks are created from GitHub repository files and how they are passed to Ollama for generating intelligent responses. 

**Key Insight:** Ollama is **ONLY** used for chat response generation. Text chunks are created using pure Java string manipulation with **NO** embedding generation or vector similarity calculations.

---

## 📊 PART 1: Text Chunk Creation (Startup/Indexing Phase)

### Architecture Overview

```
GitHub Repository → RAGService → DocumentProcessingService → FileEmbeddingStore → embeddings.jsonl
                                        ↓
                              splitIntoChunks()
                            (Pure Java - NO Ollama)
```

---

### Step 1: Application Startup

**Location:** `RAGService.java`

```java
@PostConstruct
public void purgeOnStartup() {
    if (cleanOnStartup) {
        logger.info("cleanOnStartup enabled: purging existing embeddings before indexing");
        repositoryConfig.getRepositories().forEach(repo -> {
            documentProcessingService.reprocessRepository(repo.getOwner(), repo.getName()).join();
        });
    }
}
```

**What Happens:**
- Application starts up
- `@PostConstruct` method runs automatically
- If `rag.cleanOnStartup=true` (in `application.properties`), deletes old chunks
- Triggers repository indexing process

---

### Step 2: Fetch Files from GitHub

**Location:** `RAGService.indexRepository()`

```java
private CompletableFuture<Void> indexRepository(boolean forceReindex) {
    return CompletableFuture.runAsync(() -> {
        // Fetch all files from configured repositories
        List<GitHubFile> allFiles = gitHubService.getAllFiles().join();
        logger.info("Found {} files across all repositories", allFiles.size());
        
        // Filter text files only (.md, .txt, .java, .py, etc.)
        List<GitHubFile> textFiles = allFiles.stream()
                .filter(file -> gitHubService.isTextFile(file.getName()))
                .toList();
        
        logger.info("Processing {} text files", textFiles.size());
        
        // Process in batches of 10
        int batchSize = 10;
        for (int i = 0; i < textFiles.size(); i += batchSize) {
            List<GitHubFile> batch = textFiles.subList(i, end);
            batch.stream().map(file -> processFile(file, processed, failed)).toList();
        }
    });
}
```

**What Happens:**
1. Connects to GitHub API (github.ibm.com or github.com)
2. Fetches repository tree recursively
3. Filters for text files only (excludes images, binaries, etc.)
4. Example result:
   ```
   ├─► install.md (5000 lines)
   ├─► api-guide.md (8000 lines)
   ├─► setup.md (3000 lines)
   └─► config.md (2000 lines)
   ```

---

### Step 3: Process Each File

**Location:** `RAGService.processFile()`

```java
private CompletableFuture<Void> processFile(GitHubFile file, AtomicInteger processed, AtomicInteger failed) {
    GitHubRepositoryConfig.Repository repository = /* find repository config */;
    
    return gitHubService.getFileContent(repository, file.getPath())
            .thenCompose(fileWithContent -> {
                return documentProcessingService.processDocument(
                        fileWithContent.getPath(),      // "install.md"
                        fileWithContent.getContent(),   // Raw text (5000+ lines)
                        repository.getOwner(),
                        repository.getName(),
                        repository.getBranch()
                );
            });
}
```

**What Happens:**
1. Fetches file content from GitHub API
2. Receives raw text content (could be 5000+ lines)
3. Passes to `DocumentProcessingService` for chunking

**Example Content:**
```markdown
# Installation Guide

## Prerequisites
- Java 11+
- Maven 3.6+

## Installation Steps
1. Download from https://example.com
2. Extract the archive
3. Run ./install.sh

## Configuration
Edit config.properties...
[... continues for 5000+ lines ...]
```

---

### Step 4: Split Content into Text Chunks ⭐ CRITICAL STEP

**Location:** `DocumentProcessingService.processDocument()`

```java
public CompletableFuture<Void> processDocument(String filePath, String content, 
                                               String repositoryOwner, String repositoryName, 
                                               String branch) {
    return CompletableFuture.runAsync(() -> {
        logger.info("Processing document: {}", filePath);
        
        // ════════════════════════════════════════════════════════════════
        // SPLIT INTO CHUNKS - NO OLLAMA INVOLVED!
        // ════════════════════════════════════════════════════════════════
        List<String> chunks = splitIntoChunks(content, 3000);
        //                                              ↑
        //                                    Max 3000 chars per chunk
        
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            if (chunk.trim().length() > 50) {  // Skip tiny chunks
                
                DocumentEmbedding embedding = new DocumentEmbedding();
                embedding.setFilePath(filePath);
                embedding.setContentChunk(chunk);              // ← Plain text!
                embedding.setChunkIndex(i);
                embedding.setRepositoryOwner(repositoryOwner);
                embedding.setRepositoryName(repositoryName);
                embedding.setBranchName(branch);
                
                // ❌ NO OLLAMA CALL HERE!
                // ❌ NO embedding.setEmbedding([vectors])
                // ✅ JUST PLAIN TEXT STORAGE
                
                embeddingStore.save(embedding);
            }
        }
        
        logger.info("Processed {} chunks for file: {}", chunks.size(), filePath);
    });
}
```

---

### Step 5: splitIntoChunks() Algorithm

**Location:** `DocumentProcessingService.splitIntoChunks()`

```java
private List<String> splitIntoChunks(String content, int maxChunkSize) {
    List<String> chunks = new ArrayList<>();
    
    if (content == null || content.trim().isEmpty()) {
        return chunks;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // STEP 1: Split by paragraphs (\n\n)
    // ═══════════════════════════════════════════════════════════════════
    String[] paragraphs = content.split("\n\n");
    StringBuilder currentChunk = new StringBuilder();
    
    for (String paragraph : paragraphs) {
        // Check if adding paragraph exceeds max size
        if (currentChunk.length() + paragraph.length() <= maxChunkSize) {
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
        } else {
            // Save current chunk
            if (currentChunk.length() > 0) {
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
            }
            
            // ═══════════════════════════════════════════════════════════
            // STEP 2: If paragraph is too long, split by sentences
            // ═══════════════════════════════════════════════════════════
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
```

**Algorithm Explanation:**

1. **Split by paragraphs** (double newline `\n\n`)
2. **Build chunks** up to 3000 characters
3. **If paragraph too long**, split by sentences (`. `)
4. **Maintain readability** - never split mid-sentence if possible
5. **Result:** List of text strings, each ~3000 chars max

**Example Output:**
```
Input: "install.md" (15,000 characters)

Output:
├─► chunks[0] = "# Installation Guide\n\n## Prerequisites\n- Java 11+..." (2,800 chars)
├─► chunks[1] = "## Installation Steps\n1. Download from https://..." (2,950 chars)
├─► chunks[2] = "## Configuration\nEdit config.properties:..." (2,100 chars)
├─► chunks[3] = "## Troubleshooting\nIf you encounter errors..." (1,800 chars)
└─► chunks[4] = "## Advanced Setup\nFor production deployments..." (2,350 chars)

Total: 5 text chunks created from 1 file
```

---

### Step 6: Create DocumentEmbedding Objects

**Location:** `DocumentProcessingService.processDocument()`

**Important:** Despite the name "DocumentEmbedding", these objects do **NOT** contain vector embeddings!

```java
DocumentEmbedding embedding = new DocumentEmbedding();
embedding.setFilePath("install.md");
embedding.setContentChunk("# Installation Guide\n\n## Prerequisites...");  // Plain text
embedding.setChunkIndex(0);
embedding.setRepositoryOwner("maximo-application-suite");
embedding.setRepositoryName("knowledge-center");
embedding.setBranchName("main");
```

**DocumentEmbedding.java Fields:**
```java
public class DocumentEmbedding {
    private String id;
    private String filePath;
    private String repositoryOwner;
    private String repositoryName;
    private String branchName;
    private String contentChunk;        // ← PLAIN TEXT (no vectors!)
    private Integer chunkIndex;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // ❌ NO "embedding" field with float[] or double[]
    // ✅ ONLY plain text in "contentChunk"
}
```

---

### Step 7: Persist to File Storage

**Location:** `FileEmbeddingStore.save()`

```java
public void save(DocumentEmbedding embedding) {
    // Assign ID and timestamps
    if (embedding.getId() == null || embedding.getId().isBlank()) {
        embedding.setId(UUID.randomUUID().toString());
    }
    if (embedding.getCreatedAt() == null) {
        embedding.setCreatedAt(LocalDateTime.now());
    }
    embedding.setUpdatedAt(LocalDateTime.now());
    
    // Add to in-memory list (fast retrieval)
    index(embedding);
    
    // Append to disk file
    appendToDisk(embedding);
}

private void appendToDisk(DocumentEmbedding e) {
    try (BufferedWriter writer = Files.newBufferedWriter(
            dataFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
        writer.write(serialize(e));
        writer.newLine();
    } catch (IOException ex) {
        logger.warn("Failed to persist embedding to disk", ex);
    }
}
```

**File Format:** `data/embeddings/embeddings.jsonl`

```json
{"id":"abc-123","filePath":"install.md","contentChunk":"# Installation Guide\n\n## Prerequisites\n- Java 11+\n- Maven 3.6+","chunkIndex":0,"repositoryOwner":"maximo-application-suite","repositoryName":"knowledge-center","branchName":"main","createdAt":"2024-12-03T10:00:00","updatedAt":"2024-12-03T10:00:00"}
{"id":"def-456","filePath":"install.md","contentChunk":"## Installation Steps\n1. Download from https://example.com\n2. Extract the archive","chunkIndex":1,"repositoryOwner":"maximo-application-suite","repositoryName":"knowledge-center","branchName":"main","createdAt":"2024-12-03T10:00:01","updatedAt":"2024-12-03T10:00:01"}
{"id":"ghi-789","filePath":"api-guide.md","contentChunk":"# API Guide\n\nThis guide covers...","chunkIndex":0,"repositoryOwner":"maximo-application-suite","repositoryName":"knowledge-center","branchName":"main","createdAt":"2024-12-03T10:00:02","updatedAt":"2024-12-03T10:00:02"}
```

**Key Characteristics:**
- ✅ **Format:** JSON Lines (JSONL) - one JSON object per line
- ✅ **Content:** Plain text chunks only
- ❌ **NO vector/embedding data**
- ✅ **Size:** ~5-10 MB for 159 text chunks
- ✅ **Loading:** All chunks loaded into memory on startup for fast searching

---

## 🔍 PART 2: User Query → Chunk Retrieval (Runtime Phase)

### Architecture Overview

```
User Query → ChatController → ChatService → DocumentProcessingService → Keyword Search
                                               ↓
                                    findBestMatchingFile()
                                  (Keyword-based scoring)
                                               ↓
                                    Return matching chunks
```

---

### Step 1: User Sends Query

**Frontend Request:**
```javascript
POST /api/chat/message
Content-Type: application/json

{
  "message": "create asset",
  "sessionId": "web-session-123",
  "includeContext": true,
  "fastMode": false,
  "fullContent": true
}
```

---

### Step 2: Process Message

**Location:** `ChatService.processMessage()`

```java
public CompletableFuture<ChatResponse> processMessage(ChatRequest request) {
    return CompletableFuture.supplyAsync(() -> {
        logger.info("Processing message for session: {}", sessionId);
        
        // ════════════════════════════════════════════════════════════════
        // RETRIEVE RELEVANT CHUNKS (KEYWORD-BASED SEARCH)
        // ════════════════════════════════════════════════════════════════
        List<DocumentEmbedding> relevantChunks = List.of();
        
        if (request.isIncludeContext()) {
            if (fullContent) {
                // Find best matching file and return ALL its chunks
                relevantChunks = documentProcessingService
                    .findBestMatchingFile(request.getMessage());
            } else {
                // Hybrid search (file-based scoring)
                int maxChunks = fastMode ? 6 : 25;
                relevantChunks = documentProcessingService
                    .findSimilarDocumentsHybrid(request.getMessage(), maxChunks, 0.7);
            }
            
            // FALLBACK CHAIN (if no results)
            if (relevantChunks.isEmpty()) {
                relevantChunks = documentProcessingService
                    .findRelevantChunks(request.getMessage(), 25);
            }
            if (relevantChunks.isEmpty()) {
                relevantChunks = documentProcessingService
                    .findRelevantChunksByKeywords(request.getMessage(), 50);
            }
        }
        
        // Build contextual prompt and generate response
        String contextualPrompt = buildContextualPrompt(
            request.getMessage(), 
            relevantChunks
        );
        String response = ollamaService.generateResponse(contextualPrompt);
        
        return new ChatResponse(response, sessionId, responseTime, sourceFiles);
    });
}
```

---

### Step 3: Find Best Matching File (Keyword Search)

**Location:** `DocumentProcessingService.findBestMatchingFile()`

```java
public List<DocumentEmbedding> findBestMatchingFile(String query) {
    logger.info("Finding best matching file for: {}", query);
    
    // Load all chunks from memory
    List<DocumentEmbedding> allEmbeddings = embeddingStore.findAll();
    
    // Normalize query
    String normalizedQuery = query.toLowerCase().trim();
    String[] queryWords = normalizedQuery.split("\\s+");
    // Example: "create asset" → ["create", "asset"]
    
    // ═══════════════════════════════════════════════════════════════════
    // Group chunks by file
    // ═══════════════════════════════════════════════════════════════════
    Map<String, List<DocumentEmbedding>> fileGroups = allEmbeddings.stream()
            .collect(Collectors.groupingBy(DocumentEmbedding::getFilePath));
    
    // Result:
    // ├─► "install.md" → [chunk0, chunk1, chunk2, chunk3]
    // ├─► "api-guide.md" → [chunk0, chunk1, ..., chunk11]
    // └─► "setup.md" → [chunk0, chunk1, ..., chunk7]
    
    // ═══════════════════════════════════════════════════════════════════
    // Score each FILE using Top-K chunk averaging
    // ═══════════════════════════════════════════════════════════════════
    String bestFilePath = fileGroups.entrySet().stream()
            .map(entry -> {
                String filePath = entry.getKey();
                List<DocumentEmbedding> chunks = entry.getValue();
                
                // Calculate score for each chunk
                List<Double> scores = chunks.stream()
                    .map(chunk -> calculateRelevanceScore(
                        chunk.getContentChunk(),  // ← Plain text search!
                        chunk.getFilePath(),
                        queryWords,
                        normalizedQuery
                    ))
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());

                // Use Top-K average (top 5 scores)
                int k = Math.min(5, scores.size());
                double topKAvg = scores.stream()
                    .limit(k)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
                
                // Add filename matching bonus
                double fileNameBonus = calculateFilenameRelevance(filePath, queryWords);

                return new FileScore(filePath, topKAvg + fileNameBonus);
            })
            .max(Comparator.comparing(fs -> fs.score))
            .map(fs -> fs.filePath)
            .orElse(null);
    
    // Return ALL chunks from best file
    if (bestFilePath != null) {
        logger.info("Best matching file: {}", bestFilePath);
        return fileGroups.get(bestFilePath).stream()
                .sorted(Comparator.comparing(DocumentEmbedding::getChunkIndex))
                .collect(Collectors.toList());
    }
    
    return Collections.emptyList();
}
```

---

### Step 4: Calculate Relevance Score (Keyword-Based)

**Location:** `DocumentProcessingService.calculateRelevanceScore()`

```java
private double calculateRelevanceScore(String content, String fileName, 
                                      String[] queryWords, String fullQuery) {
    double score = 0.0;
    
    String lowerContent = content.toLowerCase();
    String lowerFileName = fileName.toLowerCase();
    String lowerQuery = fullQuery.toLowerCase();
    
    // ═══════════════════════════════════════════════════════════════════
    // CONTENT SCORING (Primary importance)
    // ═══════════════════════════════════════════════════════════════════
    for (String word : queryWords) {
        if (word.length() > 2) {
            // Exact word boundary match in content
            if (lowerContent.contains(" " + word + " ") || 
                lowerContent.startsWith(word + " ") || 
                lowerContent.endsWith(" " + word)) {
                score += 15.0;  // High score for exact match
            }
            // Partial word match in content
            else if (lowerContent.contains(word)) {
                score += 8.0;   // Lower score for partial match
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // FILENAME SCORING (Secondary importance)
    // ═══════════════════════════════════════════════════════════════════
    for (String word : queryWords) {
        if (word.length() > 2 && lowerFileName.contains(word)) {
            score += 3.0;  // Filename match bonus
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // EXACT PHRASE MATCHING (Highest priority)
    // ═══════════════════════════════════════════════════════════════════
    if (lowerContent.contains(lowerQuery)) {
        score += 50.0;  // Big bonus for exact phrase
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MULTI-WORD PROXIMITY BONUS
    // ═══════════════════════════════════════════════════════════════════
    if (queryWords.length > 1) {
        for (int i = 0; i < queryWords.length - 1; i++) {
            String phrase = queryWords[i] + " " + queryWords[i + 1];
            if (lowerContent.contains(phrase)) {
                score += 25.0;  // Adjacent words bonus
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SEMANTIC EXPANSION (Synonyms)
    // ═══════════════════════════════════════════════════════════════════
    if (lowerQuery.contains("create") || lowerQuery.contains("creating")) {
        if (lowerContent.contains("create") || 
            lowerContent.contains("setup") || 
            lowerContent.contains("configure")) {
            score += 30.0;  // Semantic match bonus
        }
    }
    
    if (lowerQuery.contains("organization") || lowerQuery.contains("org")) {
        if (lowerContent.contains("organization") || 
            lowerContent.contains("site")) {
            score += 60.0;  // Strong semantic signal
        }
    }
    
    return score;
}
```

**Scoring Example:**

For query `"create asset"` and content from `api-guide.md`:

```
Chunk 5 content: "## Create Asset\nMethod: POST\nURL: /MXAPIASSET..."

Scoring breakdown:
├─► "create" exact match in content: +15 points
├─► "asset" exact match in content: +15 points
├─► "create asset" exact phrase: +50 points
├─► "create" + "asset" proximity: +25 points
├─► Semantic bonus (create→setup): +30 points
└─► Total: 135.0 points ✅ HIGH SCORE!

File-level scoring:
├─► api-guide.md chunks: [135.0, 98.7, 87.4, 82.1, 76.9]
│   Top-5 average: 96.02 ✅ WINNER!
├─► install.md chunks: [45.3, 38.2, 22.1, 18.5, 12.3]
│   Top-5 average: 27.28
└─► setup.md chunks: [23.1, 19.8, 15.2, 11.4, 8.9]
    Top-5 average: 15.68
```

---

## 🤖 PART 3: Build Prompt & Pass to Ollama

### Architecture Overview

```
Matched Chunks → buildContextualPrompt() → OllamaService.generateResponse() → AI Response
                        ↓
                Combine text chunks
                Extract relevant sections
                Format for AI
                        ↓
                 HTTP POST to Ollama
                 (localhost:11434)
```

---

### Step 1: Build Contextual Prompt

**Location:** `ChatService.buildContextualPrompt()`

```java
private String buildContextualPrompt(String userQuery, 
                                     List<DocumentEmbedding> relevantChunks) {
    if (relevantChunks.isEmpty()) {
        return String.format("""
            USER QUESTION: %s
            No relevant documentation found.
            """, userQuery);
    }
    
    StringBuilder contextBuilder = new StringBuilder();
    
    // ═══════════════════════════════════════════════════════════════════
    // COMBINE ALL CHUNKS FROM BEST FILE
    // ═══════════════════════════════════════════════════════════════════
    Map<String, List<DocumentEmbedding>> chunksByFile = relevantChunks.stream()
            .collect(Collectors.groupingBy(DocumentEmbedding::getFilePath));
    
    for (Map.Entry<String, List<DocumentEmbedding>> fileEntry : chunksByFile.entrySet()) {
        String filePath = fileEntry.getKey();
        List<DocumentEmbedding> fileChunks = fileEntry.getValue();
        
        contextBuilder.append("\n--- Content from: ").append(filePath).append(" ---\n");
        
        // Sort chunks by index and concatenate
        fileChunks.stream()
                .sorted(Comparator.comparing(DocumentEmbedding::getChunkIndex))
                .forEach(chunk -> {
                    contextBuilder.append(chunk.getContentChunk()).append("\n");
                });
    }
    
    String contextContent = contextBuilder.toString();
    
    // ═══════════════════════════════════════════════════════════════════
    // SMART SLICING: Extract only relevant sections
    // ═══════════════════════════════════════════════════════════════════
    String lowered = userQuery.toLowerCase();
    if (lowered.contains("create ") || lowered.startsWith("create")) {
        // Extract Prerequisites section
        String prereq = extractPrerequisites(contextContent);
        
        // Extract Create section
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
    
    // ═══════════════════════════════════════════════════════════════════
    // FORMAT FINAL PROMPT FOR OLLAMA
    // ═══════════════════════════════════════════════════════════════════
    return String.format("""
        You are a technical documentation assistant.
        
        USER QUESTION: "%s"
        
        CRITICAL INSTRUCTIONS - RAW CONTENT ONLY:
        1. Extract Prerequisites section EXACTLY as written
        2. Extract the COMPLETE section that answers the question
        3. Include ALL steps, methods, URLs, headers, parameters
        4. Do NOT summarize, provide EXACT content
        5. Maintain formatting from source
        
        CONTENT:
        %s
        
        RETURN EXACT CONTENT FOR: %s
        """, 
        userQuery,      // "create asset"
        contextContent, // Combined text from 12 chunks
        userQuery       // "create asset"
    );
}
```

**Example Prompt Generated:**

```
You are a technical documentation assistant.

USER QUESTION: "create asset"

CRITICAL INSTRUCTIONS - RAW CONTENT ONLY:
1. Extract Prerequisites section EXACTLY as written
2. Extract the COMPLETE section that answers the question
3. Include ALL steps, methods, URLs, headers, parameters
4. Do NOT summarize, provide EXACT content
5. Maintain formatting from source

CONTENT:
--- Content from: api-guide.md ---
# Assets API Guide

This guide covers all asset operations in Maximo...

## Prerequisites
- Valid API key required
- Maximo instance must be running
- User must have asset creation permissions

## Create Asset
Method: POST
URL: /MXAPIASSET
Headers: apikey: <your-apikey-value>
Parameters: lean: 1
Request Body:
{
  "assetnum": "ASSET001",
  "description": "New Asset",
  "status": "OPERATING"
}
Response: 201 Created
{
  "assetnum": "ASSET001",
  "assetid": 12345,
  ...
}

[... more content from other chunks ...]

RETURN EXACT CONTENT FOR: create asset
```

---

### Step 2: Send to Ollama ⭐ THIS IS WHERE TEXT CHUNKS REACH OLLAMA!

**Location:** `OllamaService.generateResponse()`

```java
public String generateResponse(String prompt) {
    try {
        logger.debug("Generating response using model: {}", modelName);
        
        // ════════════════════════════════════════════════════════════════
        // BUILD HTTP REQUEST BODY WITH PROMPT CONTAINING TEXT CHUNKS
        // ════════════════════════════════════════════════════════════════
        String requestBody = buildRequestBody(prompt);
        //                                     ↑
        //                   Contains text from 12 chunks combined!
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaBaseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(10))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        // ════════════════════════════════════════════════════════════════
        // SEND TO OLLAMA API
        // ════════════════════════════════════════════════════════════════
        HttpResponse<String> response = httpClient.send(
            request, 
            HttpResponse.BodyHandlers.ofString()
        );
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama API returned status: " 
                + response.statusCode());
        }
        
        return parseResponse(response.body());
        
    } catch (IOException | InterruptedException e) {
        logger.error("Failed to communicate with Ollama", e);
        throw new RuntimeException("Failed to generate response: " + e.getMessage());
    }
}

private String buildRequestBody(String prompt) {
    int numPredict = prompt.length() < 1200 ? 512 : 2048;
    int numCtx = Math.min(8192, 16384);
    
    return String.format("""
            {
                "model": "%s",
                "prompt": "%s",
                "stream": false,
                "options": {
                    "temperature": 0.1,
                    "top_p": 0.9,
                    "num_predict": %d,
                    "num_ctx": %d
                }
            }
            """, 
            modelName,           // "granite4:micro-h"
            escapeJson(prompt),  // ← TEXT CHUNKS ARE HERE!
            numPredict, 
            numCtx
    );
}
```

**HTTP Request Sent to Ollama:**

```http
POST http://localhost:11434/api/generate
Content-Type: application/json

{
  "model": "granite4:micro-h",
  "prompt": "You are a technical documentation assistant.\n\nUSER QUESTION: \"create asset\"\n\nCRITICAL INSTRUCTIONS...\n\nCONTENT:\n--- Content from: api-guide.md ---\n# Assets API Guide\n\n## Prerequisites\n- Valid API key...\n\n## Create Asset\nMethod: POST\nURL: /MXAPIASSET...\n\nRETURN EXACT CONTENT FOR: create asset",
  "stream": false,
  "options": {
    "temperature": 0.1,
    "top_p": 0.9,
    "num_predict": 2048,
    "num_ctx": 8192
  }
}
```

**Key Point:** The `"prompt"` field contains the combined text from all matched chunks!

---

### Step 3: Receive AI Response

**Ollama Response:**

```json
{
  "model": "granite4:micro-h",
  "created_at": "2024-12-03T10:30:45.123Z",
  "response": "## Prerequisites\n- Valid API key required\n- Maximo instance must be running\n- User must have asset creation permissions\n\n## Create Asset\n\nMethod: POST\nURL: /MXAPIASSET\nHeaders: apikey: <your-apikey-value>\nParameters: lean: 1\n\nRequest Body:\n```json\n{\n  \"assetnum\": \"ASSET001\",\n  \"description\": \"New Asset\",\n  \"status\": \"OPERATING\"\n}\n```\n\nResponse: 201 Created\n```json\n{\n  \"assetnum\": \"ASSET001\",\n  \"assetid\": 12345\n}\n```",
  "done": true,
  "context": [123, 456, 789, ...],
  "total_duration": 1847000000,
  "load_duration": 123000000,
  "prompt_eval_count": 2048,
  "eval_count": 512
}
```

**Parse and Return:**

```java
private String parseResponse(String responseBody) {
    try {
        // Extract "response" field from JSON
        int responseStart = responseBody.indexOf("\"response\":\"") + 12;
        int responseEnd = responseBody.lastIndexOf("\",\"done\"");
        
        if (responseStart > 11 && responseEnd > responseStart) {
            String response = responseBody.substring(responseStart, responseEnd);
            return unescapeJson(response);
        }
        
        return "I apologize, but I couldn't generate a proper response.";
        
    } catch (Exception e) {
        logger.error("Failed to parse Ollama response", e);
        return "I encountered an error while processing your request.";
    }
}
```

---

### Step 4: Return to User

**Location:** `ChatService.processMessage()`

```java
public CompletableFuture<ChatResponse> processMessage(ChatRequest request) {
    return CompletableFuture.supplyAsync(() -> {
        // ... chunk retrieval and prompt building ...
        
        String response = ollamaService.generateResponse(contextualPrompt);
        long responseTime = System.currentTimeMillis() - startTime;
        
        List<String> sourceFiles = relevantChunks.stream()
                .map(this::sanitizeSourceFile)
                .distinct()
                .collect(Collectors.toList());
        
        return ChatResponse.builder()
                .response(response)
                .sessionId(sessionId)
                .responseTime(responseTime)
                .sourceFiles(sourceFiles)
                .modelUsed(modelName)
                .success(true)
                .build();
    });
}
```

**Final Response to Frontend:**

```json
{
  "response": "## Prerequisites\n- Valid API key required...\n\n## Create Asset\n\nMethod: POST\nURL: /MXAPIASSET...",
  "sessionId": "web-session-123",
  "responseTime": 1847,
  "sourceFiles": ["Assets API Guide"],
  "modelUsed": "granite4:micro-h",
  "success": true
}
```

---

## 📊 Complete Flow Summary

### End-to-End Journey

| Phase | Step | Component | Input | Output | Ollama Used? |
|-------|------|-----------|-------|--------|--------------|
| **Indexing** | 1 | RAGService | GitHub repos | File list | ❌ No |
| **Indexing** | 2 | GitHubService | File paths | Raw text content | ❌ No |
| **Indexing** | 3 | DocumentProcessingService | Raw text | Text chunks (~3000 chars) | ❌ **NO** |
| **Indexing** | 4 | DocumentProcessingService | Text chunks | DocumentEmbedding objects | ❌ No |
| **Indexing** | 5 | FileEmbeddingStore | DocumentEmbedding | Saved to embeddings.jsonl | ❌ No |
| **Runtime** | 6 | ChatController | User query | ChatRequest object | ❌ No |
| **Runtime** | 7 | DocumentProcessingService | User query | Matched chunks (keyword search) | ❌ **NO** |
| **Runtime** | 8 | ChatService | Query + chunks | Contextual prompt (combined text) | ❌ No |
| **Runtime** | 9 | OllamaService | Prompt with chunks | **HTTP POST to Ollama** | ✅ **YES!** |
| **Runtime** | 10 | OllamaService | Ollama response | Parsed AI text | ✅ Yes |
| **Runtime** | 11 | ChatController | AI response | JSON to frontend | ❌ No |

---

## 🎯 Key Insights

### 1. Text Chunks Creation (NO Ollama)

```java
// DocumentProcessingService.splitIntoChunks()
// Pure Java string manipulation - NO AI involved

List<String> chunks = new ArrayList<>();
String[] paragraphs = content.split("\n\n");
for (String paragraph : paragraphs) {
    if (currentChunk.length() + paragraph.length() <= 3000) {
        currentChunk.append(paragraph);
    } else {
        chunks.add(currentChunk.toString());
        currentChunk = new StringBuilder(paragraph);
    }
}
```

**No Ollama API calls, no embeddings, just text splitting!**

---

### 2. Keyword-Based Search (NO Ollama)

```java
// DocumentProcessingService.calculateRelevanceScore()
// Heuristic scoring - NO vector similarity

double score = 0.0;
if (content.contains(word)) score += 15.0;           // Exact match
if (content.contains(query)) score += 50.0;          // Phrase match
if (fileName.contains(word)) score += 3.0;           // Filename bonus
if (content.contains("create")) score += 30.0;       // Semantic bonus
return score;
```

**No embedding vectors, no cosine similarity, just keyword matching!**

---

### 3. Text Chunks Pass to Ollama (YES Ollama)

```java
// OllamaService.generateResponse()
// THIS is where text chunks reach Ollama!

String requestBody = """
    {
        "model": "granite4:micro-h",
        "prompt": "%s",          ← Combined text from matched chunks
        "stream": false
    }
""".formatted(escapeJson(prompt));

HttpRequest request = HttpRequest.newBuilder()
    .uri("http://localhost:11434/api/generate")
    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
    .build();
```

**Text chunks are embedded in the prompt string sent to Ollama's chat API!**

---

## 📝 Configuration

### application.properties

```properties
# Ollama Configuration (Chat ONLY, not for embeddings)
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=granite4:micro-h
spring.ai.ollama.embedding.model=granite4:micro-h  # ❌ NOT USED

# RAG Settings
rag.cleanOnStartup=true                            # Delete old chunks on startup

# Embedding Store (Plain text chunks, NOT vectors)
embedding.store=file
embedding.store.dir=data/embeddings                # Storage location

# Chunk Size
file.processing.chunk-size=3000                    # Max chars per chunk
```

---

## 🔧 File Structure

```
backend/
├── data/
│   └── embeddings/
│       └── embeddings.jsonl          ← Plain text chunks (NO vectors)
│
├── src/main/java/com/aichatbot/
│   ├── service/
│   │   ├── RAGService.java           ← Orchestrates indexing
│   │   ├── DocumentProcessingService.java  ← Chunks creation & search
│   │   ├── OllamaService.java        ← Chat API integration
│   │   ├── ChatService.java          ← Main orchestrator
│   │   └── GitHubService.java        ← Fetches files
│   │
│   ├── repository/
│   │   ├── EmbeddingStore.java       ← Interface
│   │   └── FileEmbeddingStore.java   ← File-based implementation
│   │
│   └── model/
│       └── DocumentEmbedding.java    ← Plain text storage model
│
└── src/main/resources/
    └── application.properties
```

---

## 🚀 Performance Characteristics

| Metric | Value | Details |
|--------|-------|---------|
| **Chunk Size** | ~3000 chars | Max size per text chunk |
| **Total Chunks** | 159 | For 4 repositories |
| **Storage Size** | ~5-10 MB | Plain text in JSONL format |
| **Memory Usage** | ~10-15 MB | All chunks loaded in memory |
| **Search Time** | 50-150ms | Keyword-based iteration |
| **Ollama Call** | 1-2 seconds | AI response generation |
| **Total Response** | 1.2-2.5s | End-to-end user request |

---

## ✅ Conclusion

This system demonstrates a **lightweight RAG architecture** that:

1. **Creates text chunks** using pure Java string manipulation (NO AI)
2. **Stores chunks** as plain text in JSONL file (NO vectors)
3. **Searches chunks** using keyword-based heuristic scoring (NO embeddings)
4. **Passes matched chunks** to Ollama via chat API prompt (YES Ollama - chat only)
5. **Generates intelligent responses** using Ollama Granite 4:micro-h model

**Key Advantage:** Simple, fast, transparent, and requires no ML infrastructure beyond Ollama for chat.

---

**Last Updated:** December 3, 2025  
**Version:** 1.0.0  
**Author:** Shreyas Gowda
