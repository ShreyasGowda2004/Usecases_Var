PART 1: Text Chunk Creation (Startup/Indexing)
┌─────────────────────────────────────────────────────────────────────────┐
│                    STEP 1: APPLICATION STARTUP                          │
└─────────────────────────────────────────────────────────────────────────┘

Application starts → RAGService.java
    │
    ├─► @PostConstruct
    │   purgeOnStartup() {
    │       if (cleanOnStartup == true) {
    │           // Delete old chunks from embeddings.jsonl
    │           documentProcessingService.reprocessRepository(owner, name)
    │       }
    │   }
    │
    └─► initializeRepository()
         └─► indexRepository(forceReindex: false)

┌─────────────────────────────────────────────────────────────────────────┐
│              STEP 2: FETCH FILES FROM GITHUB                            │
└─────────────────────────────────────────────────────────────────────────┘

RAGService.indexRepository() {
    
    // Get all files from configured repositories
    List<GitHubFile> allFiles = gitHubService.getAllFiles()
    
    // Example result:
    // ├─► install.md (5000 lines)
    // ├─► api-guide.md (8000 lines)
    // ├─► setup.md (3000 lines)
    // └─► config.md (2000 lines)
    
    // Filter text files only
    List<GitHubFile> textFiles = allFiles.stream()
        .filter(file -> gitHubService.isTextFile(file.getName()))
        // Keeps: .md, .txt, .java, .js, .py, etc.
        // Excludes: .png, .jpg, .zip, etc.
    
    // Process in batches of 10
    for (batch in textFiles) {
        processFile(file)  ──────┐
    }                            │
}                                │
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│           STEP 3: PROCESS EACH FILE (Create Text Chunks)                │
└─────────────────────────────────────────────────────────────────────────┘

RAGService.processFile(GitHubFile file) {
    
    // Get file content from GitHub API
    GitHubFile fileWithContent = gitHubService.getFileContent(repo, file.getPath())
    
    // Example: install.md content
    String content = """
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
    """
    
    // Pass to DocumentProcessingService for chunking
    documentProcessingService.processDocument(
        filePath:   "install.md",
        content:    content,           ← Raw text (5000+ lines)
        owner:      "maximo-application-suite",
        name:       "knowledge-center",
        branch:     "main"
    )
}

                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│     STEP 4: SPLIT CONTENT INTO CHUNKS (DocumentProcessingService)       │
└─────────────────────────────────────────────────────────────────────────┘

DocumentProcessingService.processDocument(...) {
    
    logger.info("Processing document: install.md")
    
    // ═══════════════════════════════════════════════════════════════════
    // CRITICAL: splitIntoChunks() - NO OLLAMA INVOLVED HERE!
    // ═══════════════════════════════════════════════════════════════════
    
    List<String> chunks = splitIntoChunks(content, 3000)
    //                                              ↑
    //                                        Max 3000 chars per chunk
    
    // How splitIntoChunks() works:
    // ────────────────────────────────────────────────────────────────────
    
    1. Split by paragraphs (\n\n):
       String[] paragraphs = content.split("\n\n")
       
       // Result:
       // ├─► "# Installation Guide"
       // ├─► "## Prerequisites\n- Java 11+\n- Maven 3.6+"
       // ├─► "## Installation Steps\n1. Download..."
       // └─► "## Configuration\nEdit config.properties..."
    
    2. Build chunks with max 3000 chars:
       StringBuilder currentChunk = new StringBuilder()
       
       for (paragraph in paragraphs) {
           if (currentChunk.length() + paragraph.length() <= 3000) {
               currentChunk.append(paragraph)  // Add to current chunk
           } else {
               chunks.add(currentChunk)        // Save current chunk
               currentChunk = new StringBuilder(paragraph)  // Start new
           }
       }
    
    3. If paragraph is too long (>3000 chars), split by sentences:
       String[] sentences = paragraph.split("\\. ")
       // Repeat same logic with sentences
    
    // Result: List of text chunks
    // ────────────────────────────────────────────────────────────────────
    // chunks[0] = "# Installation Guide\n\n## Prerequisites\n- Java 11+..."  (2800 chars)
    // chunks[1] = "## Installation Steps\n1. Download from https://..."      (2950 chars)
    // chunks[2] = "## Configuration\nEdit config.properties:..."             (2100 chars)
    // chunks[3] = "## Troubleshooting\nIf you encounter errors..."           (1800 chars)
    
    
    // ═══════════════════════════════════════════════════════════════════
    // STEP 5: CREATE DocumentEmbedding OBJECTS (Just metadata + text)
    // ═══════════════════════════════════════════════════════════════════
    
    for (int i = 0; i < chunks.size(); i++) {
        String chunk = chunks.get(i)
        
        if (chunk.trim().length() > 50) {  // Skip tiny chunks
            
            DocumentEmbedding embedding = new DocumentEmbedding()
            embedding.setFilePath("install.md")
            embedding.setContentChunk(chunk)              ← Plain text stored!
            embedding.setChunkIndex(i)                    ← Position in file
            embedding.setRepositoryOwner("maximo-application-suite")
            embedding.setRepositoryName("knowledge-center")
            embedding.setBranchName("main")
            
            // ❌ NO OLLAMA CALL HERE!
            // ❌ NO embedding.setEmbedding([vectors]) - field doesn't exist!
            // ✅ JUST PLAIN TEXT STORAGE
            
            // Save to file: data/embeddings/embeddings.jsonl
            embeddingStore.save(embedding)
        }
    }
    
    logger.info("Processed 4 chunks for file: install.md")
}

┌─────────────────────────────────────────────────────────────────────────┐
│             STEP 6: PERSIST TO FILE (FileEmbeddingStore)                │
└─────────────────────────────────────────────────────────────────────────┘

FileEmbeddingStore.save(DocumentEmbedding embedding) {
    
    // Assign ID and timestamps
    embedding.setId(UUID.randomUUID().toString())
    embedding.setCreatedAt(LocalDateTime.now())
    embedding.setUpdatedAt(LocalDateTime.now())
    
    // Add to in-memory list (fast retrieval later)
    all.add(embedding)
    
    // Append to JSONL file
    appendToDisk(embedding)
}

appendToDisk(embedding) {
    // Open: data/embeddings/embeddings.jsonl
    // Append one line:
    
    {"id":"abc-123","filePath":"install.md","contentChunk":"# Installation Guide\n\n## Prerequisites...","chunkIndex":0,"repositoryOwner":"maximo-application-suite","repositoryName":"knowledge-center","branchName":"main","createdAt":"2024-12-03T10:00:00","updatedAt":"2024-12-03T10:00:00"}
    
    // ❌ NO "embedding" field with vectors!
    // ✅ ONLY plain text in "contentChunk"
}

📁 Result: data/embeddings/embeddings.jsonl
────────────────────────────────────────────────────────────────────────────
Line 1: {"id":"abc-123","filePath":"install.md","contentChunk":"# Installation...","chunkIndex":0,...}
Line 2: {"id":"def-456","filePath":"install.md","contentChunk":"## Installation Steps...","chunkIndex":1,...}
Line 3: {"id":"ghi-789","filePath":"install.md","contentChunk":"## Configuration...","chunkIndex":2,...}
Line 4: {"id":"jkl-012","filePath":"api-guide.md","contentChunk":"# API Guide...","chunkIndex":0,...}
...
Line 159: {"id":"xyz-999","filePath":"setup.md","contentChunk":"## Verification...","chunkIndex":8,...}

Total: 159 text chunks stored (NO vectors, just text)



PART 2: User Query → Chunk Retrieval (Runtime)
┌─────────────────────────────────────────────────────────────────────────┐
│                 USER SENDS QUERY: "create asset"                        │
└─────────────────────────────────────────────────────────────────────────┘

Frontend POST /api/chat/message
    {
        "message": "create asset",
        "sessionId": "web-123",
        "includeContext": true,
        "fullContent": true
    }

                    ↓

ChatController.sendMessage(ChatRequest)
    └─► ChatService.processMessage(request)

┌─────────────────────────────────────────────────────────────────────────┐
│            RETRIEVE RELEVANT CHUNKS (Keyword-Based Search)              │
└─────────────────────────────────────────────────────────────────────────┘

ChatService.processMessage(ChatRequest request) {
    
    String userMessage = "create asset"
    
    if (request.isIncludeContext()) {
        
        if (fullContent) {
            // ═══════════════════════════════════════════════════════════
            // Find best matching file and return ALL its chunks
            // ═══════════════════════════════════════════════════════════
            
            List<DocumentEmbedding> relevantChunks = 
                documentProcessingService.findBestMatchingFile(userMessage)
                //                         ↑
                //                    Keyword-based scoring!
            
            // Inside findBestMatchingFile():
            // ──────────────────────────────────────────────────────────
            
            1. Load all chunks from memory:
               List<DocumentEmbedding> allEmbeddings = embeddingStore.findAll()
               // Returns 159 DocumentEmbedding objects (plain text)
            
            2. Normalize query:
               String normalizedQuery = "create asset".toLowerCase()
               String[] queryWords = ["create", "asset"]
            
            3. Score each FILE (not individual chunks):
               
               Map<String, List<DocumentEmbedding>> fileGroups = 
                   allEmbeddings.groupBy(chunk -> chunk.getFilePath())
               
               // Result:
               // ├─► "install.md" → [chunk0, chunk1, chunk2, chunk3]
               // ├─► "api-guide.md" → [chunk0, chunk1, ..., chunk11]
               // └─► "setup.md" → [chunk0, chunk1, ..., chunk7]
               
               For each file:
                   For each chunk in file:
                       double score = calculateRelevanceScore(
                           chunk.getContentChunk(),  ← Plain text search!
                           chunk.getFilePath(),
                           ["create", "asset"],
                           "create asset"
                       )
                       
                       // Scoring logic (NO VECTORS):
                       // ─────────────────────────────────────────────
                       // - "create" found in text: +15 points
                       // - "asset" found in text: +15 points
                       // - "create asset" exact phrase: +50 points
                       // - "api-guide.md" filename has "asset": +3 points
                       // - Semantic bonus (create→setup→configure): +30 points
                       // ─────────────────────────────────────────────
                       
                       // Example chunk scores:
                       // api-guide.md chunk 5: 113.2 (highest!)
                       // api-guide.md chunk 6: 98.7
                       // api-guide.md chunk 3: 87.4
                       // install.md chunk 1: 45.3
                       // setup.md chunk 2: 23.1
                   
                   // Take top-5 chunk scores for this file
                   topKAverage = average([113.2, 98.7, 87.4, 82.1, 76.9])
                                = 91.66
               
               // File-level scores:
               // ├─► "api-guide.md" → 91.66 ✅ WINNER!
               // ├─► "install.md" → 38.42
               // └─► "setup.md" → 19.77
            
            4. Return ALL chunks from best file:
               return fileGroups.get("api-guide.md")
                     .sortedBy(chunk -> chunk.getChunkIndex())
               
               // Result: 12 DocumentEmbedding objects with plain text
        }
    }
    
    // Example of relevantChunks returned:
    // ────────────────────────────────────────────────────────────────────
    // relevantChunks[0].contentChunk = "# Assets API Guide\n\nThis guide..."
    // relevantChunks[1].contentChunk = "## Prerequisites\n- Valid API key..."
    // relevantChunks[2].contentChunk = "## Create Asset\nMethod: POST\nURL:..."
    // relevantChunks[3].contentChunk = "### Request Parameters\n- lean: 1..."
    // ... (8 more chunks with plain text)

 PART 3: Build Prompt & Pass to Ollama
┌─────────────────────────────────────────────────────────────────────────┐
│              BUILD CONTEXTUAL PROMPT FOR OLLAMA                         │
└─────────────────────────────────────────────────────────────────────────┘

ChatService.processMessage() {
    
    // We now have: relevantChunks = 12 DocumentEmbedding objects
    
    // ═══════════════════════════════════════════════════════════════════
    // Build prompt by combining text chunks
    // ═══════════════════════════════════════════════════════════════════
    
    String contextualPrompt = buildContextualPrompt(
        "create asset",      ← User query
        relevantChunks       ← 12 text chunks from api-guide.md
    )
}

buildContextualPrompt(String userQuery, List<DocumentEmbedding> relevantChunks) {
    
    // Combine all 12 chunks into one big text block
    StringBuilder contextBuilder = new StringBuilder()
    
    contextBuilder.append("\n--- Content from: api-guide.md ---\n")
    
    // Sort by chunkIndex and concatenate
    for (DocumentEmbedding chunk : relevantChunks.sortedBy(chunkIndex)) {
        contextBuilder.append(chunk.getContentChunk())
        contextBuilder.append("\n")
    }
    
    String contextContent = contextBuilder.toString()
    
    // contextContent now contains:
    // ────────────────────────────────────────────────────────────────────
    // --- Content from: api-guide.md ---
    // # Assets API Guide
    // 
    // This guide covers all asset operations...
    // 
    // ## Prerequisites
    // - Valid API key required
    // - Maximo instance must be running
    // 
    // ## Create Asset
    // Method: POST
    // URL: /MXAPIASSET
    // Headers: apikey: <your-apikey-value>
    // Parameters: lean: 1
    // Request Body: {
    //   "assetnum": "ASSET001",
    //   "description": "New Asset"
    // }
    // Response: 201 Created
    // [... continues with all 12 chunks combined ...]
    // ────────────────────────────────────────────────────────────────────
    
    
    // ═══════════════════════════════════════════════════════════════════
    // Smart slicing: Extract only Prerequisites + Create section
    // ═══════════════════════════════════════════════════════════════════
    
    if (userQuery.contains("create ")) {
        String prereq = extractPrerequisites(contextContent)
        // Finds: "## Prerequisites\n- Valid API key..."
        
        String createSection = extractOperationSection(contextContent, "create")
        // Finds: "## Create Asset\nMethod: POST..."
        
        // Combine only relevant parts
        contextContent = prereq + "\n\n---\n\n" + createSection
    }
    
    
    // ═══════════════════════════════════════════════════════════════════
    // Format final prompt for Ollama
    // ═══════════════════════════════════════════════════════════════════
    
    return String.format("""
        You are a technical documentation assistant.
        
        USER QUESTION: "%s"
        
        INSTRUCTIONS:
        1. Extract Prerequisites section EXACTLY as written
        2. Extract the COMPLETE section for the requested operation
        3. Include ALL steps, methods, URLs, headers, parameters
        4. Do NOT summarize, provide EXACT content
        
        CONTENT:
        %s
        
        RETURN EXACT CONTENT FOR: %s
        """, 
        userQuery,           // "create asset"
        contextContent,      // Combined text from 12 chunks
        userQuery            // "create asset"
    )
}

┌─────────────────────────────────────────────────────────────────────────┐
│                    SEND TO OLLAMA FOR RESPONSE                          │
└─────────────────────────────────────────────────────────────────────────┘

ChatService.processMessage() {
    
    // contextualPrompt is now ready with all relevant text chunks!
    
    // ═══════════════════════════════════════════════════════════════════
    // ✅ THIS IS WHERE TEXT CHUNKS ARE PASSED TO OLLAMA!
    // ═══════════════════════════════════════════════════════════════════
    
    String response = ollamaService.generateResponse(contextualPrompt)
    //                                                 ↑
    //                                   Contains text from 12 chunks
    //                                   combined into one prompt
}

OllamaService.generateResponse(String prompt) {
    
    // Build HTTP request
    String requestBody = """
        {
            "model": "granite4:micro-h",
            "prompt": "%s",               ← Full prompt with text chunks
            "stream": false,
            "options": {
                "temperature": 0.1,
                "top_p": 0.9,
                "num_predict": 512,
                "num_ctx": 8192
            }
        }
    """.formatted(escapeJson(prompt))
    
    // POST to Ollama
    HttpRequest request = HttpRequest.newBuilder()
        .uri("http://localhost:11434/api/generate")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
        .build()
    
    HttpResponse<String> response = httpClient.send(request)
    
    // Parse response
    String aiResponse = parseResponse(response.body())
    
    // Example AI response:
    // ────────────────────────────────────────────────────────────────────
    // ## Prerequisites
    // - Valid API key required
    // - Maximo instance must be running
    // 
    // ## Create Asset
    // Method: POST
    // URL: /MXAPIASSET
    // Headers: apikey: <your-apikey-value>
    // Parameters: lean: 1
    // Request Body:
    // {
    //   "assetnum": "ASSET001",
    //   "description": "New Asset"
    // }
    // Response: 201 Created
    // ────────────────────────────────────────────────────────────────────
    
    return aiResponse
}

┌─────────────────────────────────────────────────────────────────────────┐
│                   RETURN TO USER (Frontend)                             │
└─────────────────────────────────────────────────────────────────────────┘

ChatService.processMessage() {
    
    return new ChatResponse(
        response: aiResponse,              // AI-generated text
        sessionId: "web-123",
        responseTime: 1847ms,
        sourceFiles: ["api-guide.md"],     // Original source
        modelUsed: "granite4:micro-h",
        success: true
    )
}

Frontend receives and displays the response with "Execute" button! ✅

