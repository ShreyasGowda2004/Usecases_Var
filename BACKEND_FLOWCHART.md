# 🏗️ AI Chatbot Backend Architecture & Flow Documentation

## 📋 Table of Contents
1. [System Overview](#system-overview)
2. [Complete Architecture Diagram](#complete-architecture-diagram)
3. [Component Details](#component-details)
4. [Data Flow Diagrams](#data-flow-diagrams)
5. [RAG Pipeline](#rag-pipeline)
6. [API Endpoints](#api-endpoints)
7. [Database Schema](#database-schema)

---

## 🎯 System Overview

The AI Chatbot Backend is a Spring Boot application that provides intelligent assistance for IBM Maximo Application Suite using **RAG (Retrieval-Augmented Generation)** technology.

### Key Technologies
- **Spring Boot 3.2.0** - Application framework
- **Ollama (Granite 4:micro-h)** - AI model for response generation ONLY (no embeddings)
- **MongoDB** - Chat history and execution logs
- **File-based Text Chunk Store** - Document text chunks (NOT vector embeddings)
- **Keyword-Based Semantic Search** - Heuristic scoring with synonym expansion
- **GitHub API** - Knowledge base repository access

---

## 🔄 Complete Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           USER INTERACTION LAYER                             │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐     HTTP (Port 3000)     ┌────────────────────┐         │
│  │  React Frontend │ ◄──────────────────────► │  Chat Interface    │         │
│  │  (Vite + Carbon)│                          │ Execution Console  │         │
│  └─────────────────┘                          └────────────────────┘         │
│           │                                              │                   │
│           │ REST API (JSON)                              │                   │
│           ▼                                              ▼                   │
└───────────┼──────────────────────────────────────────────┼───────────────────┘
            │                                              │
            │                        HTTP (Port 8080)      │
            │                                              │
┌───────────▼──────────────────────────────────────────────▼───────────────────┐
│                          BACKEND APPLICATION LAYER                           │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐     │
│  │                        CONTROLLER LAYER                             │     │
│  ├─────────────────────────────────────────────────────────────────────┤     │
│  │                                                                     │     │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  ┌──────────┐   │     │
│  │  │ChatController│  │AdminControll │  │GitHubCtrl  │  │HealthCtrl│   │     │
│  │  │POST /message │  │GET  /repos   │  │GET /file   │  │GET /     │   │     │
│  │  └──────┬───────┘  └──────┬───────┘  └─────┬─┬─────┘ └────┬─────┘   │     │
│  │         │                  │                 │              │       │     │
│  └─────────┼──────────────────┼─────────────────┼──────────────┼───────┘     │
│            │                  │                 │              │             │
│            ▼                  ▼                 ▼              ▼             │
│  ┌─────────────────────────────────────────────────────────────────────┐     │
│  │                         SERVICE LAYER                               │     │
│  ├─────────────────────────────────────────────────────────────────────┤     │
│  │                                                                     │     │ 
│  │  ┌───────────────┐        ┌──────────────────┐      ┌────────────┐  │     │
│  │  │  ChatService  │ ◄──────┤DocumentProcessing├────► │RAGService  │  │     │
│  │  │   (Orchestrator)       │     Service      │      │            │  │     │
│  │  └───────┬───────┘        └──────────────────┘      └────────────┘  │     │
│  │          │                                                          │     │
│  │          │                                                          │     │
│  │  ┌───────▼────────┐      ┌──────────────────┐      ┌────────────┐   │     │
│  │  │ OllamaService  │      │  GitHubService   │      │UserService │   │     │
│  │  │ (AI Generation)│      │  (Repo Access)   │      │(Auth)      │   │     │
│  │  └────────────────┘      └──────────────────┘      └────────────┘   │     │
│  │                                                                     │     │
│  └──────────────────────────┬───────────────────────┬──────────────────┘     │
│                             │                       │                        │
└─────────────────────────────┼───────────────────────┼────────────────────────┘
                              │                       │
                              ▼                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          DATA PERSISTENCE LAYER                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────────┐              ┌──────────────────────┐             │
│  │  MongoDB (Port 27017)│              │  File-Based Storage  │             │
│  ├──────────────────────┤              ├──────────────────────┤             │
│  │ • ChatHistory        │              │ • embeddings.jsonl   │             │
│  │ • ExecutionHistory   │              │ • Plain Text Chunks  │             │
│  │ • Users              │              │ • NO Vector Data     │             │
│  │ • Pipelines          │              │ • Metadata Only      │             │
│  └──────────────────────┘              └──────────────────────┘             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       EXTERNAL SERVICES LAYER                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────────┐              ┌──────────────────────┐             │
│  │ Ollama (Port 11434)  │              │  GitHub API          │             │
│  ├──────────────────────┤              ├──────────────────────┤             │
│  │ • Granite 4:micro-h  │              │ • github.ibm.com     │             │
│  │ • Text Generation    │              │ • Repository Access  │             │
│  │ • Chat Responses ONLY│              │ • File Retrieval     │             │
│  └──────────────────────┘              └──────────────────────┘             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Component Details

### 1. **Controller Layer** (Entry Points)

#### `ChatController.java`
```
Purpose: Handle chat API requests
Endpoint: POST /api/chat/message
Flow:
  1. Receive ChatRequest (message, sessionId, includeContext, fastMode)
  2. Validate request
  3. Call ChatService.processMessage()
  4. Return ChatResponse (response, sources, timing, model)
```

#### `GitHubController.java`
```
Purpose: Fetch raw file content from GitHub
Endpoint: GET /api/github/file?url={githubUrl}
Flow:
  1. Parse GitHub URL
  2. Extract repo, branch, path
  3. Call GitHub API
  4. Return file content
```

#### `HealthController.java`
```
Purpose: System health monitoring
Endpoint: GET /api/health
Returns:
  - Application status
  - Ollama connectivity
  - MongoDB connectivity
  - Embedding count
```

---

### 2. **Service Layer** (Business Logic)

#### `ChatService.java` ⭐ CORE ORCHESTRATOR

**Purpose:** Orchestrates the entire chat processing workflow

**Key Methods:**

##### `processMessage(ChatRequest)`
```java
/**
 * Main entry point for chat processing
 * 
 * Flow:
 * 1. Check if direct file request → handleDirectFileRequest()
 * 2. Retrieve context using DocumentProcessingService:
 *    - fullContent=true  → findBestMatchingFile() (entire file)
 *    - fullContent=false → findSimilarDocumentsHybrid() (specific chunks)
 * 3. Apply fallback chain:
 *    - Hybrid search
 *    - Best file approach
 *    - Standard search
 *    - Keyword search
 * 4. Build contextual prompt → buildContextualPrompt()
 * 5. Generate AI response → OllamaService.generateResponse()
 * 6. Return ChatResponse with metadata
 * 
 * @param request ChatRequest containing user message and options
 * @return CompletableFuture<ChatResponse> Async response with AI answer
 */
```

##### `buildContextualPrompt(String query, List<DocumentEmbedding> chunks)`
```java
/**
 * Constructs intelligent prompt for AI based on query intent
 * 
 * Smart Features:
 * 1. Detect "create X" queries → Extract only Prerequisites + Create section
 * 2. Detect "raw/exact" requests → Return complete unmodified content
 * 3. Detect prerequisites → Show prerequisites first, then answer
 * 4. Standard queries → Extract only relevant sections
 * 
 * Slicing Logic:
 * - extractPrerequisites() → Finds "Prerequisites" heading
 * - extractOperationSection("create") → Finds "Create" section
 * - Combines both for targeted response
 * 
 * @param query User's question
 * @param chunks Retrieved document chunks
 * @return Formatted prompt for AI model
 */
```

##### `extractOperationSection(String content, String operation)`
```java
/**
 * Extracts specific operation section from markdown documentation
 * 
 * Search Order:
 * 1. Numbered headings: "## 1. Create Asset"
 * 2. Unnumbered headings: "## Create Asset"
 * 3. Any level heading: "### Create Asset"
 * 
 * Uses regex to capture from heading until next heading or EOF
 * 
 * @param content Full document content
 * @param operation Operation name (create/query/update/delete)
 * @return Extracted section or null
 */
```

---

#### `DocumentProcessingService.java` ⭐ CORE RETRIEVAL ENGINE

**Purpose:** Manage document text chunks and keyword-based semantic search

**Key Methods:**

##### `findRelevantChunks(String query, int maxResults)`
```java
/**
 * Keyword-based search with semantic expansion and heuristic scoring
 * 
 * Algorithm:
 * 1. Calculate relevance score for all text chunks using:
 *    - Exact word matching (highest weight)
 *    - Partial word matching
 *    - Phrase proximity scoring
 *    - Filename matching bonus
 *    - Semantic keyword expansion (synonyms)
 * 2. Filter by minimum score threshold (0.1)
 * 3. Sort by relevance score (descending)
 * 4. Return top N results
 * 
 * NO VECTOR EMBEDDINGS - Pure keyword/heuristic approach
 * 
 * @param query User's search query
 * @param maxResults Maximum chunks to return
 * @return List of relevant document chunks
 */
```

##### `findBestMatchingFile(String query)`
```java
/**
 * Returns ALL chunks from the single best matching file
 * 
 * Use Case: Complete guides, installation steps, full documentation
 * 
 * Algorithm:
 * 1. Score all files using Top-K chunk averaging:
 *    - Calculate relevance score for each chunk in file
 *    - Take top 5 chunk scores per file
 *    - Average top-5 scores as file-level score
 * 2. Add filename matching bonus
 * 3. Select file with highest total score
 * 4. Return ALL chunks from that file (sorted by index)
 * 
 * NO EMBEDDINGS - Uses keyword-based scoring
 * 
 * @param query User's search query
 * @return All chunks from best matching file
 */
```

##### `processDocument(String filePath, String content, String owner, String name, String branch)`
```java
/**
 * Processes GitHub document to create text chunks (NOT embeddings)
 * 
 * Flow:
 * 1. Split content into chunks (~3000 chars max)
 *    - Splits by paragraphs (\n\n)
 *    - If too long, splits by sentences
 *    - Maintains readability
 * 2. Create DocumentEmbedding objects (misnomer - just text storage)
 * 3. Save to file: data/embeddings/embeddings.jsonl
 * 4. Cache in memory for fast retrieval
 * 
 * NO OLLAMA INVOLVEMENT - Just text chunking and storage
 * 
 * @param filePath Source file path
 * @param content Raw text content
 * @param owner Repository owner
 * @param name Repository name
 * @param branch Branch name
 */
```

---

#### `OllamaService.java` ⭐ AI INTEGRATION

**Purpose:** Interface with Ollama AI model

**Key Methods:**

##### `generateResponse(String prompt)`
```java
/**
 * Generates AI response using Ollama Granite 4:micro-h model
 * 
 * Configuration:
 * - Model: granite4:micro-h (fast, accurate)
 * - Temperature: 0.7 (balanced creativity)
 * - Max Tokens: 4096
 * 
 * Connection:
 * - URL: http://localhost:11434
 * - Protocol: HTTP POST
 * - Format: JSON
 * 
 * @param prompt Contextual prompt with user query + retrieved docs
 * @return AI-generated response text
 */
```

##### `isHealthy()`
```java
/**
 * Checks if Ollama service is accessible
 * 
 * Calls GET /api/tags endpoint to verify connectivity
 * 
 * NOTE: Ollama is ONLY used for chat response generation,
 * NOT for embeddings or semantic search.
 * 
 * @return boolean indicating service health
 */
```

---

#### `GitHubService.java` 📚 KNOWLEDGE BASE ACCESS

**Purpose:** Fetch documentation from GitHub repositories

**Key Methods:**

##### `fetchFileContent(String owner, String repo, String path, String branch)`
```java
/**
 * Retrieves file content from GitHub
 * 
 * Supports:
 * - github.com (public)
 * - github.ibm.com (enterprise)
 * 
 * Authentication: Personal Access Token (PAT)
 * 
 * @param owner Repository owner
 * @param repo Repository name
 * @param path File path
 * @param branch Branch name
 * @return File content as string
 */
```

##### `listMarkdownFiles(String owner, String repo, String branch)`
```java
/**
 * Recursively lists all markdown files in repository
 * 
 * Filters:
 * - Only .md files
 * - Excludes: README, LICENSE, CHANGELOG
 * 
 * @return List of file paths
 */
```

---

### 3. **Model Layer** (Data Structures)

#### `DocumentEmbedding.java`
```java
/**
 * Represents a document text chunk (NOT actual embeddings!)
 * 
 * IMPORTANT: Despite the name, this class does NOT store vector embeddings.
 * It only stores plain text chunks with metadata.
 * 
 * Fields:
 * - id: Unique identifier
 * - filePath: Source file path
 * - contentChunk: Raw text content (~3000 chars)
 * - chunkIndex: Position in original file
 * - repositoryOwner: Repository owner
 * - repositoryName: Repository name
 * - branchName: Branch name
 * - createdAt: Timestamp
 * 
 * NO VECTOR FIELD EXISTS - Search uses keyword matching on contentChunk
 * 
 * Stored in: backend/data/embeddings/embeddings.jsonl
 */
```

#### `ChatMessage.java`
```java
/**
 * Represents a chat message
 * 
 * Fields:
 * - id: Message ID
 * - sessionId: Conversation session
 * - userMessage: User's question
 * - assistantResponse: AI's answer
 * - sourceFiles: Referenced documents
 * - timestamp: Message time
 * - modelUsed: AI model name
 * 
 * Stored in: MongoDB chatHistory collection
 */
```

---

## 🔄 Data Flow Diagrams

### Complete Request Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                     CHAT REQUEST PROCESSING FLOW                         │
└──────────────────────────────────────────────────────────────────────────┘

1. USER SENDS MESSAGE
   │
   ├─► Frontend: React Chat Interface
   │   • User types: "create asset"
   │   • Click Send button
   │
   ▼

2. HTTP REQUEST TO BACKEND
   │
   ├─► POST /api/chat/message
   │   Content-Type: application/json
   │   {
   │     "message": "create asset",
   │     "sessionId": "web-session-123",
   │     "includeContext": true,
   │     "fastMode": true,
   │     "fullContent": true
   │   }
   │
   ▼

3. CONTROLLER LAYER
   │
   ├─► ChatController.sendMessage()
   │   • Validate request
   │   • Log request
   │   • Call service layer
   │
   ▼

4. SERVICE ORCHESTRATION
   │
   ├─► ChatService.processMessage()
   │   │
   │   ├─► Step 1: Check if direct file request
   │   │   • Pattern match: ".md", "raw", "exact"
   │   │   • If yes → Skip RAG, return raw file
   │   │   • If no → Continue to RAG pipeline
   │   │
   │   ├─► Step 2: Retrieve context (RAG Pipeline)
   │   │   │
   │   │   ├─► DocumentProcessingService.findBestMatchingFile()
   │   │   │   │
   │   │   │   ├─► Calculate similarity scores for all files
   │   │   │   │   • Query embedding: [0.234, 0.567, ...]
   │   │   │   │   • Compare with file embeddings
   │   │   │   │   • Cosine similarity calculation
   │   │   │   │
   │   │   │   ├─► Select file with highest average score
   │   │   │   │   • Assets_API_Guide.md: 0.85
   │   │   │   │   • DB2_Setup.md: 0.42
   │   │   │   │   • Winner: Assets_API_Guide.md
   │   │   │   │
   │   │   │   └─► Return ALL chunks from best file
   │   │   │       • Chunk 0: "# Assets API..."
   │   │   │       • Chunk 1: "## Prerequisites..."
   │   │   │       • Chunk 2: "## Create Asset..."
   │   │   │       • Total: 15 chunks
   │   │   │
   │   │   └─► Result: 15 DocumentEmbedding objects
   │   │
   │   ├─► Step 3: Build contextual prompt
   │   │   │
   │   │   ├─► Detect query intent: "create"
   │   │   │
   │   │   ├─► Extract prerequisites section
   │   │   │   • Regex: "## Prerequisite..."
   │   │   │   • Result: "Valid API key required..."
   │   │   │
   │   │   ├─► Extract create section
   │   │   │   • Regex: "## Create Asset"
   │   │   │   • Result: "Method: POST, URL: /MXAPIASSET..."
   │   │   │
   │   │   └─► Combine into prompt:
   │   │       "Return EXACT content for: create asset
   │   │        CONTENT:
   │   │        ## Prerequisites
   │   │        ...
   │   │        ## Create Asset
   │   │        Method: POST
   │   │        URL: /MXAPIASSET
   │   │        Headers: apikey: <your-apikey-value>
   │   │        Parameters: lean: 1
   │   │        ..."
   │   │
   │   ├─► Step 4: Generate AI response
   │   │   │
   │   │   ├─► OllamaService.generateResponse(prompt)
   │   │   │   │
   │   │   │   ├─► HTTP POST to Ollama
   │   │   │   │   URL: http://localhost:11434/api/generate
   │   │   │   │   Model: granite4:micro-h
   │   │   │   │   Prompt: [contextual prompt from above]
   │   │   │   │
   │   │   │   └─► AI processes and returns:
   │   │   │       "## Prerequisites
   │   │   │        - Valid API key
   │   │   │        
   │   │   │        ## Create Asset
   │   │   │        Method: POST
   │   │   │        URL: /MXAPIASSET
   │   │   │        Headers: apikey: <your-apikey-value>
   │   │   │        Parameters: lean: 1
   │   │   │        Request Body: {...}"
   │   │   │
   │   │   └─► Response received in <2 seconds
   │   │
   │   └─► Step 5: Package response
   │       • response: AI-generated text
   │       • sessionId: "web-session-123"
   │       • responseTime: 1847ms
   │       • sourceFiles: ["Assets API Guide"]
   │       • modelUsed: "granite4:micro-h"
   │
   ▼

5. RETURN TO FRONTEND
   │
   ├─► ChatResponse JSON:
   │   {
   │     "response": "## Prerequisites...",
   │     "sessionId": "web-session-123",
   │     "responseTime": 1847,
   │     "sourceFiles": ["Assets API Guide"],
   │     "modelUsed": "granite4:micro-h",
   │     "success": true
   │   }
   │
   ▼

6. FRONTEND RENDERS
   │
   ├─► Parse markdown
   ├─► Extract execution details:
   │   • Method: POST
   │   • URL: /MXAPIASSET
   │   • Headers: apikey: <your-apikey-value>
   │   • Parameters: lean: 1
   │   • Body: {...}
   │
   └─► Display with "Execute" button
       User can click to open Execution Console
```

---

## 🧠 RAG Pipeline Detailed Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│               RETRIEVAL-AUGMENTED GENERATION (RAG) PIPELINE              │
└──────────────────────────────────────────────────────────────────────────┘

PHASE 1: DOCUMENT INDEXING (Startup / Scheduled)
═══════════════════════════════════════════════════

┌─────────────────┐
│ System Startup  │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ DocumentProcessingService.init()    │
│ @PostConstruct                      │
└────────┬────────────────────────────┘
         │
         ├─► Load repositories from config
         │   • Repo 1: maximo-application-suite/knowledge-center
         │   • Repo 2: maximo-application-suite/api-docs
         │
         ▼
┌─────────────────────────────────────┐
│ For each repository:                │
│ processRepository(repo)             │
└────────┬────────────────────────────┘
         │
         ├─► Step 1: Fetch files from GitHub
         │   │
         │   ├─► GitHubService.listMarkdownFiles()
         │   │   • GET /repos/{owner}/{repo}/git/trees/{branch}?recursive=1
         │   │   • Filter: *.md files only
         │   │   • Result: [install.md, api.md, setup.md, ...]
         │   │
         │   └─► GitHubService.fetchFileContent(file)
         │       • GET /repos/{owner}/{repo}/contents/{path}?ref={branch}
         │       • Decode base64 content
         │       • Result: Raw markdown text
         │
         ├─► Step 2: Split into chunks
         │   │
         │   ├─► For each file:
         │   │   • Max chunk size: 5000 characters
         │   │   • Preserve paragraph boundaries
         │   │   • Maintain context overlap: 200 chars
         │   │
         │   └─► Result: Multiple chunks per file
         │       • install.md → 8 chunks
         │       • api.md → 12 chunks
         │
         ├─► Step 3: Create text chunk objects (NO EMBEDDING GENERATION)
         │   │
         │   ├─► For each chunk:
         │   │   new DocumentEmbedding()
         │   │   │
         │   │   ├─► setFilePath("install.md")
         │   │   ├─► setContentChunk("Prerequisites for...")
         │   │   ├─► setChunkIndex(0)
         │   │   ├─► setRepositoryOwner("maximo-application-suite")
         │   │   ├─► setRepositoryName("knowledge-center")
         │   │   └─► setCreatedAt(LocalDateTime.now())
         │   │
         │   │   ❌ NO OLLAMA API CALL
         │   │   ❌ NO VECTOR GENERATION
         │   │   ✅ JUST PLAIN TEXT STORAGE
         │   │
         │   └─► Store DocumentEmbedding:
         │       {
         │         "id": "uuid-123",
         │         "filePath": "install.md",
         │         "contentChunk": "Prerequisites for...",
         │         "chunkIndex": 0,
         │         "repositoryOwner": "maximo-application-suite",
         │         "repositoryName": "knowledge-center",
         │         "branchName": "main"
         │       }
         │       
         │       ❌ NO "embedding" field - just text!
         │
         └─► Step 4: Persist text chunks
             │
             ├─► Save to file: backend/data/embeddings/embeddings.jsonl
             │   • One JSON object per line (JSONL format)
             │   • Contains plain text chunks + metadata
             │   • NO vector data stored
             │   • Total size: ~5-10 MB for 159 chunks
             │
             └─► Cache in memory: CopyOnWriteArrayList
                 • All chunks loaded into memory on startup
                 • Fast iteration for keyword search
                 • FileEmbeddingStore.findAll() returns all chunks


PHASE 2: QUERY PROCESSING (User Request)
════════════════════════════════════════

┌─────────────────┐
│ User Query:     │
│ "create asset"  │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ ChatService.processMessage()        │
└────────┬────────────────────────────┘
         │
         ├─► Prepare query (NO EMBEDDING GENERATION)
         │   │
         │   ├─► Normalize query: "create asset" → ["create", "asset"]
         │   │
         │   └─► Expand keywords with synonyms:
         │       • "create" → ["create", "creating", "setup", "configure", "build"]
         │       • "asset" → ["asset", "assets"]
         │
         ├─► Find similar documents using KEYWORD SEARCH
         │   │
         │   ├─► DocumentProcessingService.findBestMatchingFile()
         │   │   │
         │   │   ├─► Algorithm: Keyword-based file-level scoring
         │   │   │   │
         │   │   │   ├─► For each unique file:
         │   │   │   │   • Get all chunks from file
         │   │   │   │   • Calculate relevance score for each chunk:
         │   │   │   │     score = calculateRelevanceScore(
         │   │   │   │       contentChunk, filePath, queryWords, fullQuery
         │   │   │   │     )
         │   │   │   │     Scoring factors:
         │   │   │   │     - Exact word matches: +15 points
         │   │   │   │     - Partial word matches: +8 points
         │   │   │   │     - Exact phrase match: +50 points
         │   │   │   │     - Word proximity bonus: +25 points
         │   │   │   │     - Filename matches: +3 points
         │   │   │   │     - Semantic bonuses: +30-60 points
         │   │   │   │   • Sort chunk scores (descending)
         │   │   │   │   • Take top-5 chunk scores
         │   │   │   │   • fileScore = average(top-5 scores)
         │   │   │   │
         │   │   │   ├─► Sort files by score (descending)
         │   │   │   │   • Assets_API_Guide.md: 85.4
         │   │   │   │   • DB2_Setup.md: 12.3
         │   │   │   │   • Install_Guide.md: 8.7
         │   │   │   │
         │   │   │   └─► Select best file: Assets_API_Guide.md
         │   │   │
         │   │   └─► Return ALL chunks from best file (sorted by index)
         │   │       • Total: 15 chunks
         │   │       • Content: Complete API guide
         │   │
         │   └─► Result: List<DocumentEmbedding> (15 items)
         │       
         │       ❌ NO VECTOR SIMILARITY
         │       ✅ PURE KEYWORD MATCHING + HEURISTICS
         │
         └─► Build contextual prompt
             │
             ├─► Combine chunks into context
             │   • Join all chunk content
             │   • Detect prerequisites section
             │   • Detect create section
             │   • Slice to relevant parts only
             │
             └─► Format for AI:
                 "USER QUESTION: create asset
                  CONTENT:
                  ## Prerequisites
                  - Valid API key required
                  
                  ## Create Asset
                  Method: POST
                  URL: /MXAPIASSET
                  Headers: apikey: <your-apikey-value>
                  Parameters: lean: 1
                  Request Body: {...}
                  
                  RETURN EXACT CONTENT FOR: create asset"


PHASE 3: AI GENERATION (Ollama)
═══════════════════════════════

┌─────────────────────────────────────┐
│ OllamaService.generateResponse()    │
└────────┬────────────────────────────┘
         │
         ├─► HTTP POST to Ollama
         │   │
         │   ├─► URL: http://localhost:11434/api/generate
         │   │
         │   ├─► Request Body:
         │   │   {
         │   │     "model": "granite4:micro-h",
         │   │     "prompt": "[contextual prompt]",
         │   │     "stream": false,
         │   │     "options": {
         │   │       "temperature": 0.7,
         │   │       "num_predict": 4096
         │   │     }
         │   │   }
         │   │
         │   └─► Ollama Processing:
         │       • Load model: granite4:micro-h
         │       • Tokenize prompt
         │       • Generate response tokens
         │       • Decode to text
         │       • Time: ~1-2 seconds
         │
         └─► Response:
             {
               "model": "granite4:micro-h",
               "created_at": "2024-11-20T10:30:00Z",
               "response": "## Prerequisites\n- Valid API key...\n\n## Create Asset\nMethod: POST...",
               "done": true
             }


PHASE 4: RESPONSE ASSEMBLY
══════════════════════════

┌─────────────────────────────────────┐
│ ChatService assembles final response│
└────────┬────────────────────────────┘
         │
         ├─► Extract metadata
         │   • Response time: 1847ms
         │   • Source files: ["Assets API Guide"]
         │   • Model used: "granite4:micro-h"
         │
         └─► Return ChatResponse
             {
               "response": "[AI-generated text]",
               "sessionId": "web-session-123",
               "responseTime": 1847,
               "sourceFiles": ["Assets API Guide"],
               "modelUsed": "granite4:micro-h",
               "success": true
             }
```

---

## 🔌 API Endpoints Reference

### Chat Endpoints

#### `POST /api/chat/message`
**Purpose:** Send a message and get AI response

**Request:**
```json
{
  "message": "create asset",
  "sessionId": "web-session-123",
  "includeContext": true,
  "fastMode": true,
  "fullContent": true
}
```

**Response:**
```json
{
  "response": "## Prerequisites\n...\n## Create Asset\n...",
  "sessionId": "web-session-123",
  "responseTime": 1847,
  "sourceFiles": ["Assets API Guide"],
  "modelUsed": "granite4:micro-h",
  "success": true
}
```

---

### Health Endpoints

#### `GET /api/health`
**Purpose:** Check system health

**Response:**
```json
{
  "status": "UP",
  "ollama": {
    "status": "UP",
    "model": "granite4:micro-h",
    "url": "http://localhost:11434"
  },
  "mongodb": {
    "status": "UP",
    "url": "mongodb://localhost:27017"
  },
  "embeddings": {
    "count": 159,
    "lastIndexed": "2024-11-20T09:15:00Z"
  }
}
```

---

## 🗄️ Database Schema

### MongoDB Collections

#### `chatHistory`
```javascript
{
  _id: ObjectId("..."),
  id: "uuid-123",
  username: "john.doe",
  title: "Create Asset Discussion",
  createdAt: ISODate("2024-11-20T10:30:00Z"),
  messages: [
    {
      id: "msg-1",
      type: "user",
      content: "create asset",
      timestamp: ISODate("2024-11-20T10:30:00Z")
    },
    {
      id: "msg-2",
      type: "assistant",
      content: "## Prerequisites...",
      sourceFiles: ["Assets API Guide"],
      timestamp: ISODate("2024-11-20T10:30:02Z")
    }
  ]
}
```

#### `executionHistory`
```javascript
{
  _id: ObjectId("..."),
  id: "exec-456",
  username: "john.doe",
  actionTitle: "Create Asset",
  method: "POST",
  url: "/MXAPIASSET",
  requestHeaders: [
    { key: "apikey", value: "xxx" },
    { key: "Content-Type", value: "application/json" }
  ],
  requestParams: [
    { key: "lean", value: "1" }
  ],
  requestBody: "{...}",
  responseStatus: 201,
  responseBody: "{...}",
  timestamp: ISODate("2024-11-20T10:35:00Z"),
  duration: 245
}
```

#### `users`
```javascript
{
  _id: ObjectId("..."),
  username: "john.doe",
  email: "john@example.com",
  role: "user",
  createdAt: ISODate("2024-11-01T00:00:00Z"),
  lastLogin: ISODate("2024-11-20T10:00:00Z")
}
```

---

### File-Based Storage

#### `backend/data/embeddings/embeddings.jsonl`
```json
{"id":"uuid-1","filePath":"Assets_API_Guide.md","contentChunk":"# Assets API...\n\nThis guide covers...","chunkIndex":0,"repositoryOwner":"maximo-application-suite","repositoryName":"knowledge-center","branchName":"main","createdAt":"2024-11-20T09:15:00Z","updatedAt":"2024-11-20T09:15:00Z"}
{"id":"uuid-2","filePath":"Assets_API_Guide.md","contentChunk":"## Prerequisites\n\n- Valid API key\n- Maximo instance...","chunkIndex":1,"repositoryOwner":"maximo-application-suite","repositoryName":"knowledge-center","branchName":"main","createdAt":"2024-11-20T09:15:00Z","updatedAt":"2024-11-20T09:15:00Z"}
```

**Format:** JSON Lines (one object per line)
**Content:** Plain text chunks ONLY (no vector data)
**Size:** ~5-10 MB (159 text chunks)
**Loading:** On startup into CopyOnWriteArrayList via FileEmbeddingStore
**Search:** Keyword-based iteration through all chunks (no vector similarity)

---

## 🎯 Key Design Decisions

### Why Keyword-Based Search (No Vector Embeddings)?
- **Simplicity:** No ML model dependencies or vector database
- **Performance:** Fast in-memory keyword matching with heuristic scoring
- **Transparency:** Scoring logic is explainable and debuggable
- **Efficiency:** No embedding generation overhead (Ollama only for chat)
- **Accuracy:** Semantic expansion with synonyms provides intelligent matching
- **Cost:** Zero additional infrastructure or API calls

### Why File-Based Text Chunk Storage?
- **Simplicity:** No vector database setup required
- **Performance:** In-memory array provides fast iteration
- **Portability:** Easy to backup, version, and deploy (JSONL format)
- **Lightweight:** ~5-10 MB vs 50+ MB for vector embeddings

### Why Heuristic Scoring (Instead of Vector Similarity)?
- **Speed:** No cosine similarity calculations needed
- **Control:** Fine-tune scoring weights for domain-specific needs
- **Reliability:** Consistent results without model dependencies
- **Flexibility:** Easy to add custom bonuses for specific query patterns

### Why Smart Slicing?
- **Precision:** Returns only relevant sections (not entire files)
- **Speed:** Smaller prompts = faster AI responses
- **User Experience:** Users get exactly what they asked for

### Why Multiple Fallbacks?
- **Reliability:** System works even if one method fails
- **Comprehensive Coverage:** Different queries need different approaches
- **Zero Empty Responses:** Always finds something relevant

---

## 📊 Performance Metrics

| Operation | Time | Details |
|-----------|------|---------||
| Query Normalization | <1ms | String processing |
| Keyword Search | 50-150ms | In-memory iteration with scoring |
| File Matching | 100-200ms | Multi-file Top-K scoring |
| AI Response Generation | 1-2s | Ollama Granite 4:micro-h (chat only) |
| **Total Response Time** | **1.2-2.5s** | End-to-end |

| Resource | Usage | Limits |
|----------|-------|--------|
| Text Chunks in Memory | ~10-15 MB | 159 plain text chunks |
| MongoDB Storage | ~10 MB | Chat + execution history |
| Ollama Memory | ~4 GB | Model loaded (chat only) |
| Backend Memory | ~256 MB | Spring Boot + text cache |

---

## 🔒 Security Considerations

1. **GitHub Token**: Stored in environment variable, never exposed to frontend
2. **MongoDB**: No authentication in development (add in production)
3. **CORS**: Configured for localhost:3000 (update for production)
4. **File Paths**: Sanitized before returning to frontend
5. **API Keys**: Placeholder values in responses

---

## 🚀 Deployment Architecture

```
Production Environment:

┌─────────────────────────────────────────────────────────────┐
│                        Cloud Platform                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐     │
│  │  Frontend    │   │  Backend     │   │  Ollama      │     │
│  │  (React)     │   │  (Spring)    │   │  (AI Model)  │     │
│  │  Port 80/443 │   │  Port 8080   │   │  Port 11434  │     │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘     │
│         │                  │                  │             │
│         └──────────────────┼──────────────────┘             │
│                            │                                │
│  ┌─────────────────────────▼────────────────────────────┐   │
│  │              MongoDB Atlas (Managed)                 │   │
│  │              - Chat History                          │   │
│  │              - Execution Logs                        │   │
│  │              - User Data                             │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---



---

**Last Updated:** November 20, 2024  
**Version:** 1.0.0  
**Maintainer:** Shreyas Gowda
