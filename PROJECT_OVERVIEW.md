# AI Chatbot - Complete Project Overview

## 📋 Table of Contents
1. [Project Summary](#project-summary)
2. [Architecture Overview](#architecture-overview)
3. [Technology Stack](#technology-stack)
4. [Project Structure](#project-structure)
5. [Backend Deep Dive](#backend-deep-dive)
6. [Frontend Deep Dive](#frontend-deep-dive)
7. [Data Flow](#data-flow)
8. [Database Architecture](#database-architecture)
9. [API Documentation](#api-documentation)
10. [Deployment](#deployment)
11. [Configuration Guide](#configuration-guide)

---

## 🎯 Project Summary

**AI Chatbot with RAG (Retrieval-Augmented Generation)** is an intelligent chatbot application that uses AI to answer questions based on documentation from multiple GitHub repositories. The application combines:
- Real-time AI chat powered by Ollama
- Keyword-based document search (no neural embeddings)
- GitHub repository integration
- User authentication and chat history
- MongoDB for data persistence
- Single JAR deployment (Spring Boot serving React)

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         User Browser                            │
│                    (http://localhost:8080)                      │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                      │
│                  (Single JAR - Port 8080)                       │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Static Resources (React Frontend)                       │   │
│  │  - index.html, JS, CSS, Assets                           │   │
│  │  - Served from /static                                   │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  REST API Controllers (/api/*)                           │   │
│  │  - ChatController                                        │   │
│  │  - UserController                                        │   │
│  │  - HistoryController                                     │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Business Logic Services                                 │   │
│  │  - ChatService (AI Processing)                           │   │
│  │  - RAGService (Document Retrieval)                       │   │
│  │  - GitHubService (Repository Integration)                │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────┬────────────────┬────────────────┬──────────────────┘
             │                │                │
             ▼                ▼                ▼
    ┌────────────────┐ ┌────────────┐ ┌──────────────────┐
    │    MongoDB     │ │   Ollama   │ │ GitHub API       │
    │   (Cloud)      │ │  (Local)   │ │ (github.ibm.com) │
    │                │ │            │ │                  │
    │ - Users        │ │ - Chat AI  │ │ - Repositories   │
    │ - Chat History │ │            │ │ - Documentation  │
    │ - Executions   │ │            │ │                  │
    └────────────────┘ └────────────┘ └──────────────────┘
```

---

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Build Tool**: Maven
- **Database**: MongoDB (Cloud - MongoDB Atlas)
- **AI Engine**: Ollama (Local)
  - Model: granite4:micro-h (chat responses only)
- **Documentation**: Apache Tika (Text extraction)
- **Document Storage**: File-based (JSONL) - keyword search, no neural embeddings

### Frontend
- **Framework**: React 18.2.0
- **Build Tool**: Vite 5.0
- **UI Library**: Carbon Design System (@carbon/react)
- **HTTP Client**: Axios
- **Styling**: CSS Modules
- **Markdown**: react-markdown with remark-gfm

### Integration
- **GitHub API**: Enterprise GitHub (github.ibm.com)
- **Caching**: Spring Cache (Simple)
- **Validation**: Jakarta Validation

---

## 📁 Project Structure

```
Usecase_jar/
├── backend/                          # Spring Boot Backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/aichatbot/
│   │   │   │   ├── AiChatbotApplication.java    # Main entry point
│   │   │   │   ├── config/                      # Configuration classes
│   │   │   │   │   ├── AIConfig.java           # Ollama AI configuration
│   │   │   │   │   ├── EmbeddingStoreConfig.java  # Embedding storage setup
│   │   │   │   │   ├── GitHubRepositoryConfig.java # GitHub repos config
│   │   │   │   │   ├── MongoConfig.java        # MongoDB configuration
│   │   │   │   │   ├── SpaForwardingController.java # React SPA routing
│   │   │   │   │   └── WebConfig.java          # CORS & web settings
│   │   │   │   ├── controller/                  # REST API Endpoints
│   │   │   │   │   ├── ChatController.java     # /api/chat - AI conversations
│   │   │   │   │   ├── UserController.java     # /api/users - Authentication
│   │   │   │   │   ├── ChatHistoryController.java # /api/history - Chat logs
│   │   │   │   │   ├── ExecutionHistoryController.java # /api/execution-history
│   │   │   │   │   ├── GitHubController.java   # /api/github - Repo files
│   │   │   │   │   ├── HealthController.java   # /api/health - Health check
│   │   │   │   │   └── ProxyController.java    # /api/proxy - HTTP proxy
│   │   │   │   ├── dto/                         # Data Transfer Objects
│   │   │   │   │   ├── ChatRequest.java        # Chat request payload
│   │   │   │   │   ├── ChatResponse.java       # Chat response payload
│   │   │   │   │   └── ...
│   │   │   │   ├── model/                       # Domain Models (MongoDB entities)
│   │   │   │   │   ├── User.java              # User entity
│   │   │   │   │   ├── ChatHistory.java       # Chat conversation entity
│   │   │   │   │   ├── DocumentEmbedding.java # Document embedding entity
│   │   │   │   │   └── ExecutionHistory.java  # Pipeline execution logs
│   │   │   │   ├── repository/                  # Data Access Layer
│   │   │   │   │   ├── UserRepository.java    # MongoDB - User operations
│   │   │   │   │   ├── ChatHistoryRepository.java # MongoDB - Chat history
│   │   │   │   │   ├── ExecutionHistoryRepository.java # MongoDB - Executions
│   │   │   │   │   ├── EmbeddingStore.java    # Interface for embeddings
│   │   │   │   │   └── FileEmbeddingStore.java # File-based embedding storage
│   │   │   │   └── service/                     # Business Logic
│   │   │   │       ├── ChatService.java       # AI chat orchestration
│   │   │   │       ├── RAGService.java        # Retrieval-Augmented Generation
│   │   │   │       ├── OllamaService.java     # Ollama AI integration
│   │   │   │       ├── GitHubService.java     # GitHub API integration
│   │   │   │       ├── DocumentProcessingService.java # Doc parsing
│   │   │   │       ├── UserService.java       # User management
│   │   │   │       ├── ChatHistoryService.java # Chat history management
│   │   │   │       ├── ExecutionHistoryService.java # Execution tracking
│   │   │   │       └── PipelineService.java   # Processing pipelines
│   │   │   └── resources/
│   │   │       ├── application.properties     # App configuration
│   │   │       └── static/                    # React frontend (built)
│   │   │           ├── index.html
│   │   │           └── assets/
│   │   └── test/                              # Unit tests
│   ├── pom.xml                               # Maven dependencies
│   ├── mvnw                                  # Maven wrapper (Unix)
│   └── mvnw.cmd                              # Maven wrapper (Windows)
├── frontend/                                  # React Frontend
│   ├── src/
│   │   ├── main.jsx                          # React entry point
│   │   ├── App.jsx                           # Main app component
│   │   ├── CarbonApp.jsx                     # Carbon Design version
│   │   ├── components/                        # React components
│   │   │   ├── Login.jsx                     # Login/Register UI
│   │   │   ├── ChatInterface.jsx             # Chat UI
│   │   │   ├── ExecutionHistory.jsx          # Execution logs UI
│   │   │   ├── ExecutionConsole.jsx          # Pipeline console
│   │   │   ├── PipelineManager.jsx           # Pipeline management
│   │   │   └── Carbon*.jsx                   # Carbon Design versions
│   │   ├── utils/
│   │   │   └── api.js                        # API client functions
│   │   └── assets/                            # Images, icons
│   ├── index.html                            # HTML template
│   ├── package.json                          # npm dependencies
│   └── vite.config.js                        # Vite build config
├── data/
│   └── embeddings/
│       └── embeddings.jsonl                  # Document embeddings storage
├── build-single-jar.sh                       # Build script
├── start.sh                                  # Start script
├── stop.sh                                   # Stop script
├── README.md                                 # Project readme
├── SINGLE_JAR_GUIDE.md                       # Deployment guide
└── PROJECT_OVERVIEW.md                       # This file
```

---

## 🔧 Backend Deep Dive

### 1. Entry Point: `AiChatbotApplication.java`

**Location**: `backend/src/main/java/com/aichatbot/AiChatbotApplication.java`

**Purpose**: Main Spring Boot application class that bootstraps the entire backend.

```java
@SpringBootApplication
public class AiChatbotApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiChatbotApplication.class, args);
    }
}
```

**What it does**:
1. Initializes Spring Boot context
2. Scans and loads all components (@Controller, @Service, @Repository)
3. Configures embedded Tomcat server on port 8080
4. Loads application.properties configuration
5. Connects to MongoDB
6. Initializes AI services (Ollama)
7. Loads GitHub repository configurations
8. Starts embedding store and RAG system

---

### 2. Configuration Layer (`config/`)

#### `AIConfig.java`
**Purpose**: Configures Ollama AI integration

**Key Components**:
- `ChatClient`: Ollama chat model client
- Base URL: `http://localhost:11434`
- Model: `granite4:micro-h`

**What it does**:
- Creates Spring beans for AI communication
- Configures chat
- Enables async AI processing

---

#### `MongoConfig.java`
**Purpose**: MongoDB database configuration

**Key Components**:
- Connection URI: MongoDB Atlas cloud
- Database name: `Chatbot`
- Collections: `users`, `chatHistory`, `executionHistory`

**What it does**:
- Establishes connection to MongoDB Atlas
- Configures document converters
- Enables Spring Data MongoDB repositories

---

#### `EmbeddingStoreConfig.java`
**Purpose**: Document storage configuration (keyword-based search)

**Key Components**:
- Storage type: File-based (JSONL)
- Storage path: `data/embeddings/embeddings.jsonl`
- Clean on startup: Configurable

**What it does**:
- Initializes document chunk storage (file-based)
- Loads existing document chunks from disk
- Provides document CRUD operations
- **Note**: No neural embeddings - uses keyword/relevance scoring

---

#### `GitHubRepositoryConfig.java`
**Purpose**: GitHub repository configuration

**Key Components**:
- Base URL: `https://github.ibm.com/api/v3`
- Token: Personal Access Token
- Repositories: List of repos to index

**What it does**:
- Loads GitHub repository list from properties
- Configures authentication
- Enables repository file access

---

#### `WebConfig.java`
**Purpose**: Web and CORS configuration

**Key Components**:
- CORS origins: `http://localhost:3000`, etc.
- Allowed methods: GET, POST, PUT, DELETE
- Allowed headers: All

**What it does**:
- Configures CORS for development
- Enables cross-origin requests
- Sets up security policies

---

#### `SpaForwardingController.java`
**Purpose**: React SPA routing support

**Key Components**:
- Forward non-API routes to `index.html`
- Handle `favicon.ico` requests
- Support React Router

**What it does**:
1. Intercepts requests to non-API paths
2. Returns `index.html` as ClassPathResource
3. Allows React Router to handle client-side routing
4. Prevents 404 errors on page refresh

---

### 3. Controller Layer (REST API)

#### `ChatController.java` - `/api/chat`
**Purpose**: AI chat operations

**Endpoints**:
- `POST /api/chat/message` - Send message and get AI response

**Request Flow**:
1. Receives `ChatRequest` (username, message, conversationId)
2. Validates request using Jakarta Validation
3. Calls `ChatService.processMessage()`
4. Returns `ChatResponse` (reply, sources, conversationId)

**Example Request**:
```json
{
  "username": "john",
  "message": "How to install MAS?",
  "conversationId": "uuid-123"
}
```

**Example Response**:
```json
{
  "reply": "To install MAS, follow these steps...",
  "sources": ["knowledge-center/install.md"],
  "conversationId": "uuid-123",
  "timestamp": "2025-12-02T12:00:00Z"
}
```

---

#### `UserController.java` - `/api/users`
**Purpose**: User authentication and management

**Endpoints**:
- `POST /api/users/login` - User login
- `POST /api/users/register` - Create new user
- `GET /api/users/{username}` - Get user details

**Request Flow (Login)**:
1. Receives username
2. Checks if user exists in MongoDB
3. Returns user object or error
4. No password authentication (simple system)

---

#### `ChatHistoryController.java` - `/api/history`
**Purpose**: Chat conversation history

**Endpoints**:
- `GET /api/history/{username}` - Get all conversations for user
- `GET /api/history/session/{id}` - Get specific conversation
- `GET /api/history/count/{username}` - Count user's conversations

**Request Flow**:
1. Receives username
2. Queries MongoDB `chatHistory` collection
3. Returns list of chat sessions
4. Includes messages, timestamps, metadata

---

#### `HealthController.java` - `/api/health`
**Purpose**: System health checks

**Endpoints**:
- `GET /api/health` - Get system status

**Returns**:
- MongoDB status
- Ollama status
- Embedding store status
- GitHub API status

---

### 4. Service Layer (Business Logic)

#### `ChatService.java`
**Purpose**: Orchestrates AI chat processing

**Key Methods**:
- `processMessage()`: Main chat handler

**Process Flow**:
```
User Message
    ↓
1. Extract question
    ↓
2. Call RAGService.retrieveContext()
    ↓
3. Build prompt with context
    ↓
4. Call OllamaService.chat()
    ↓
5. Save to ChatHistory
    ↓
6. Return response
```

**Code Example**:
```java
public ChatResponse processMessage(ChatRequest request) {
    // 1. Retrieve relevant documents
    List<Document> context = ragService.retrieveContext(request.getMessage());
    
    // 2. Build prompt
    String prompt = buildPrompt(request.getMessage(), context);
    
    // 3. Get AI response
    String reply = ollamaService.chat(prompt);
    
    // 4. Save history
    chatHistoryService.saveMessage(request, reply);
    
    // 5. Return response
    return new ChatResponse(reply, extractSources(context));
}
```

---

#### `RAGService.java`
**Purpose**: Retrieval-Augmented Generation (Keyword-Based)

**Key Methods**:
- `initializeRepository()`: Index GitHub repositories
- `reindexRepository()`: Force re-indexing
- `scheduledReindex()`: Automatic periodic re-indexing

**Process Flow**:
```
User Query
    ↓
1. Tokenize query into keywords
    ↓
2. Search document store
    ↓
3. Calculate relevance score (keyword matching)
    ↓
4. Rank results by score
    ↓
5. Return top N documents
```

**How it works**:
1. Splits user question into keywords
2. Searches document chunks using keyword matching
3. Scores based on:
   - Exact word matches
   - Filename relevance
   - Content frequency
   - Special term bonuses
4. Returns most relevant documents
5. **No neural embeddings or vector similarity used**

---

#### `OllamaService.java`
**Purpose**: Direct integration with Ollama AI

**Key Methods**:
- `generateResponse()`: Send prompt, get chat response

**Configuration**:
- URL: `http://localhost:11434`
- Model: `granite4:micro-h`
- Timeout: 5 minutes
- **Usage**: Chat responses only (NOT used for embeddings)

---

#### `GitHubService.java`
**Purpose**: GitHub API integration

**Key Methods**:
- `fetchRepositoryFiles()`: Get all files from repo
- `fetchFileContent()`: Get specific file content
- `listRepositories()`: Get configured repos

**Process Flow**:
```
Repository Configuration
    ↓
1. Authenticate with token
    ↓
2. List repository tree
    ↓
3. Filter documentation files
    ↓
4. Download file contents
    ↓
5. Return file data
```

---

#### `DocumentProcessingService.java`
**Purpose**: Parse and process documents (keyword-based search)

**Key Methods**:
- `processDocument()`: Extract text from file and store chunks
- `findBestMatchingFile()`: Find best matching file using keyword scoring
- `findRelevantChunks()`: Find relevant chunks using keyword matching
- `findRelevantChunksByKeywords()`: Fallback keyword search
- `calculateRelevanceScore()`: Score documents based on keyword matches

**Supported Formats**:
- Markdown (.md)
- Text (.txt)
- PDF (.pdf)
- HTML (.html)

**Process Flow**:
```
Document File
    ↓
1. Detect file type
    ↓
2. Extract text (Apache Tika)
    ↓
3. Clean and normalize
    ↓
4. Split into chunks (2000 chars)
    ↓
5. Store chunks with metadata (NO embeddings generated)
    ↓
6. Store in document store (JSONL file)
```

**Search Algorithm**:
- **Keyword Matching**: Exact and partial word matches
- **Filename Scoring**: Boost for relevant filenames
- **Content Frequency**: Higher score for repeated terms
- **No Vector Similarity**: Pure keyword-based approach

---

#### `UserService.java`
**Purpose**: User management

**Key Methods**:
- `login()`: Authenticate user
- `register()`: Create new user
- `getUser()`: Retrieve user data

**MongoDB Operations**:
- Collection: `users`
- Document structure:
```json
{
  "_id": "ObjectId",
  "username": "john",
  "email": "john@example.com",
  "createdAt": "2025-12-01T10:00:00Z",
  "lastLogin": "2025-12-02T12:00:00Z"
}
```

---

#### `ChatHistoryService.java`
**Purpose**: Manage chat conversations

**Key Methods**:
- `saveMessage()`: Store chat message
- `getHistory()`: Retrieve conversation history
- `deleteHistory()`: Remove conversations

**MongoDB Operations**:
- Collection: `chatHistory`
- Document structure:
```json
{
  "_id": "ObjectId",
  "conversationId": "uuid-123",
  "username": "john",
  "messages": [
    {
      "role": "user",
      "content": "How to install MAS?",
      "timestamp": "2025-12-02T12:00:00Z"
    },
    {
      "role": "assistant",
      "content": "To install MAS...",
      "sources": ["doc1.md"],
      "timestamp": "2025-12-02T12:00:05Z"
    }
  ],
  "createdAt": "2025-12-02T12:00:00Z",
  "updatedAt": "2025-12-02T12:05:00Z"
}
```

---

### 5. Repository Layer (Data Access)

#### `UserRepository.java`
**Interface**: `MongoRepository<User, String>`

**Purpose**: MongoDB operations for users

**Methods**:
- `findByUsername()`: Find user by username
- `existsByUsername()`: Check if user exists
- `save()`: Create or update user

---

#### `ChatHistoryRepository.java`
**Interface**: `MongoRepository<ChatHistory, String>`

**Purpose**: MongoDB operations for chat history

**Methods**:
- `findByUsername()`: Get all conversations for user
- `findByConversationId()`: Get specific conversation
- `countByUsername()`: Count user's conversations

---

#### `FileEmbeddingStore.java`
**Purpose**: File-based document chunk storage

**Storage Format**: JSON Lines (.jsonl)
```json
{"id":"doc1","contentChunk":"...","filePath":"...","metadata":{...}}
{"id":"doc2","contentChunk":"...","filePath":"...","metadata":{...}}
```

**Operations**:
- `add()`: Store new document chunk
- `findAll()`: Load all document chunks
- `delete()`: Remove document chunk
- `loadFromFile()`: Load from disk
- `saveToFile()`: Persist to disk

**Note**: Stores document text chunks only - NO vector embeddings

---

### 6. Model Layer (Domain Entities)

#### `User.java`
```java
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String username;
    private String email;
    private Date createdAt;
    private Date lastLogin;
}
```

---

#### `ChatHistory.java`
```java
@Document(collection = "chatHistory")
public class ChatHistory {
    @Id
    private String id;
    private String conversationId;
    private String username;
    private List<Message> messages;
    private Date createdAt;
    private Date updatedAt;
}
```

---

#### `DocumentEmbedding.java`
```java
public class DocumentEmbedding {
    private String id;
    private String content;
    private float[] embedding;
    private Map<String, Object> metadata;
}
```

---

## 🎨 Frontend Deep Dive

### 1. Entry Point: `main.jsx`

**Location**: `frontend/src/main.jsx`

**Purpose**: React application bootstrap

```javascript
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
```

**What it does**:
1. Creates React root
2. Renders App component
3. Attaches to `#root` div in index.html

---

### 2. Main App: `App.jsx` / `CarbonApp.jsx`

**Purpose**: Main application container

**Components**:
- `Login`: Authentication screen
- `ChatInterface`: Main chat UI
- `ExecutionHistory`: Pipeline logs

**State Management**:
```javascript
const [user, setUser] = useState(null);
const [currentView, setCurrentView] = useState('login');
```

**Navigation Flow**:
```
Login Screen
    ↓
(After authentication)
    ↓
Chat Interface
```

---

### 3. Components

#### `Login.jsx`
**Purpose**: User authentication

**Features**:
- Username input
- Login button
- Register button
- Form validation

**API Calls**:
```javascript
// Login
const response = await userAPI.login(username);

// Register
const response = await userAPI.register(username, email);
```

**Success Flow**:
1. User enters username
2. Calls `/api/users/login`
3. Receives user object
4. Stores in state
5. Navigates to ChatInterface

---

#### `ChatInterface.jsx`
**Purpose**: Main chat UI

**Features**:
- Message input
- Chat history display
- Source citations
- Typing indicator
- Conversation management

**UI Structure**:
```
┌─────────────────────────────────────┐
│  AI Chatbot              [Logout]   │
├─────────────────────────────────────┤
│  User: Hello                        │
│  AI: Hi! How can I help?            │
│  Sources: [doc1.md]                 │
│                                     │
│  User: [typing...]                  │
├─────────────────────────────────────┤
│  [Type your message...] [Send]      │
└─────────────────────────────────────┘
```

**API Calls**:
```javascript
// Send message
const response = await chatAPI.sendMessage({
  username: user.username,
  message: inputText,
  conversationId: currentConversationId
});

// Load history
const history = await chatHistoryAPI.getHistory(user.username);
```

---

#### `ExecutionHistory.jsx`
**Purpose**: View pipeline execution logs

**Features**:
- Execution list
- Status indicators
- Error messages
- Timestamp display

**API Calls**:
```javascript
// Get executions
const executions = await executionAPI.getHistory(username);
```

---

### 4. API Client: `api.js`

**Location**: `frontend/src/utils/api.js`

**Purpose**: Centralized API communication

**Structure**:
```javascript
const API_BASE = '/api';

export const userAPI = {
  login: async (username) => { /* ... */ },
  register: async (username, email) => { /* ... */ }
};

export const chatAPI = {
  sendMessage: async (request) => { /* ... */ }
};

export const chatHistoryAPI = {
  getHistory: async (username) => { /* ... */ },
  getSession: async (sessionId) => { /* ... */ }
};

export const healthAPI = {
  check: async () => { /* ... */ }
};
```

**Features**:
- Centralized error handling
- Automatic JSON parsing
- Response validation
- Retry logic (future)

---

## 🔄 Data Flow

### Complete Request-Response Cycle

#### 1. User Sends Chat Message

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (React)                         │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                   User types: "How to install MAS?"
                               │
                               ▼
                    ChatInterface.handleSend()
                               │
                               ▼
                    chatAPI.sendMessage({
                      username: "john",
                      message: "How to install MAS?",
                      conversationId: "uuid-123"
                    })
                               │
                               ▼
                    POST /api/chat/message
                               │
┌──────────────────────────────┴──────────────────────────────────┐
│                      Backend (Spring Boot)                       │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. ChatController.sendMessage()                                 │
│     ↓                                                            │
│  2. Validate request (Jakarta Validation)                        │
│     ↓                                                            │
│  3. ChatService.processMessage()                                 │
│     ↓                                                            │
│  4. RAGService.retrieveContext("How to install MAS?")            │
│     │                                                            │
│     ├─→ OllamaService.generateEmbedding(query)                   │
│     │   ↓                                                        │
│     │   Ollama API: POST /api/embeddings                         │
│     │   ↓                                                        │
│     │   Returns: [0.1, 0.2, 0.3, ...]                            │
│     │                                                            │
│     ├─→ FileEmbeddingStore.search(queryEmbedding)                │
│     │   ↓                                                        │
│     │   Loads: data/embeddings/embeddings.jsonl                  │
│     │   ↓                                                        │
│     │   Calculate cosine similarity                              │
│     │   ↓                                                        │
│     │   Returns top 5 similar documents                          │
│     │                                                            │
│     └─→ Returns: [                                               │
│           {content: "MAS installation...", source: "doc1.md"},   │
│           {content: "Prerequisites...", source: "doc2.md"}       │
│         ]                                                        │
│     ↓                                                            │
│  5. Build prompt:                                                │
│     "Context: [retrieved documents]                              │
│      Question: How to install MAS?                               │
│      Answer based on context."                                   │
│     ↓                                                            │
│  6. OllamaService.chat(prompt)                                   │
│     ↓                                                            │
│     Ollama API: POST /api/generate                               │
│     ↓                                                            │
│     Returns: "To install MAS, follow these steps..."             │
│     ↓                                                            │
│  7. ChatHistoryService.saveMessage()                             │
│     ↓                                                            │
│     MongoDB: chatHistory.insertOne({                             │
│       conversationId: "uuid-123",                                │
│       username: "john",                                          │
│       messages: [                                                │
│         {role: "user", content: "How to install MAS?"},          │
│         {role: "assistant", content: "To install MAS..."}        │
│       ]                                                          │
│     })                                                           │
│     ↓                                                            │
│  8. Return ChatResponse:                                         │
│     {                                                            │
│       reply: "To install MAS...",                                │
│       sources: ["doc1.md", "doc2.md"],                           │
│       conversationId: "uuid-123"                                 │
│     }                                                            │
│                                                                  │
└──────────────────────────────┬───────────────────────────────────┘
                               │
                               ▼
                    Response: 200 OK
                               │
┌──────────────────────────────┴───────────────────────────────────┐
│                         Frontend (React)                         │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Receive response                                             │
│     ↓                                                            │
│  2. Update messages state                                        │
│     ↓                                                            │
│  3. Render AI response                                           │
│     ↓                                                            │
│  4. Display source citations                                     │
│     ↓                                                            │
│  5. Clear input field                                            │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 💾 Database Architecture

### MongoDB Collections

#### 1. `users` Collection
**Purpose**: Store user accounts

**Document Schema**:
```json
{
  "_id": ObjectId("507f1f77bcf86cd799439011"),
  "username": "john_doe",
  "email": "john@example.com",
  "createdAt": ISODate("2025-12-01T10:00:00Z"),
  "lastLogin": ISODate("2025-12-02T12:00:00Z"),
  "role": "user",
  "preferences": {
    "theme": "dark",
    "language": "en"
  }
}
```

**Indexes**:
- `username` (unique)
- `email` (unique)

---

#### 2. `chatHistory` Collection
**Purpose**: Store conversation history

**Document Schema**:
```json
{
  "_id": ObjectId("507f1f77bcf86cd799439012"),
  "conversationId": "uuid-550e8400-e29b-41d4-a716-446655440000",
  "username": "john_doe",
  "title": "MAS Installation Questions",
  "messages": [
    {
      "role": "user",
      "content": "How to install MAS?",
      "timestamp": ISODate("2025-12-02T12:00:00Z")
    },
    {
      "role": "assistant",
      "content": "To install MAS, follow these steps:\n1. ...",
      "sources": [
        {
          "file": "knowledge-center/installation.md",
          "repository": "maximo-application-suite/knowledge-center",
          "relevance": 0.92
        }
      ],
      "timestamp": ISODate("2025-12-02T12:00:05Z")
    },
    {
      "role": "user",
      "content": "What are the prerequisites?",
      "timestamp": ISODate("2025-12-02T12:01:00Z")
    },
    {
      "role": "assistant",
      "content": "The prerequisites for MAS installation are:\n1. ...",
      "sources": [
        {
          "file": "knowledge-center/prerequisites.md",
          "repository": "maximo-application-suite/knowledge-center",
          "relevance": 0.88
        }
      ],
      "timestamp": ISODate("2025-12-02T12:01:05Z")
    }
  ],
  "createdAt": ISODate("2025-12-02T12:00:00Z"),
  "updatedAt": ISODate("2025-12-02T12:01:05Z"),
  "messageCount": 4,
  "status": "active"
}
```

**Indexes**:
- `conversationId` (unique)
- `username` + `createdAt` (compound)
- `username` + `updatedAt` (compound)

---

#### 3. `executionHistory` Collection
**Purpose**: Store pipeline execution logs

**Document Schema**:
```json
{
  "_id": ObjectId("507f1f77bcf86cd799439013"),
  "executionId": "exec-uuid-123",
  "type": "REPOSITORY_INDEXING",
  "status": "SUCCESS",
  "startTime": ISODate("2025-12-02T11:00:00Z"),
  "endTime": ISODate("2025-12-02T11:15:00Z"),
  "duration": 900000,
  "repository": {
    "owner": "maximo-application-suite",
    "name": "knowledge-center",
    "branch": "main"
  },
  "statistics": {
    "filesProcessed": 150,
    "filesSkipped": 5,
    "chunksCreated": 600,
    "embeddingsGenerated": 600,
    "errors": 0
  },
  "errors": [],
  "metadata": {
    "ollamaModel": "granite4:micro-h",
    "chunkSize": 2000,
    "cleanOnStartup": true
  }
}
```

**Indexes**:
- `executionId` (unique)
- `type` + `startTime` (compound)
- `status` + `startTime` (compound)

---

### File-Based Storage

#### `data/embeddings/embeddings.jsonl`
**Purpose**: Store document text chunks (keyword search)

**Format**: JSON Lines (one JSON object per line)

**Example**:
```json
{"id":"doc-001","contentChunk":"MAS installation requires OpenShift 4.8 or higher...","filePath":"installation.md","chunkIndex":0,"repositoryOwner":"maximo-application-suite","repositoryName":"knowledge-center","branch":"main"}
{"id":"doc-002","contentChunk":"The prerequisites include: 1. OpenShift cluster 2. Storage...","filePath":"prerequisites.md","chunkIndex":0,"repositoryOwner":"maximo-application-suite","repositoryName":"knowledge-center","branch":"main"}
```

**No Vector Data**: This file stores plain text chunks only, NOT embeddings

**Operations**:
- **Add**: Append new document chunk
- **Search**: Load all, calculate keyword relevance score, sort
- **Delete**: Rewrite file without specific chunk
- **Update**: Delete + Add

**Search Method**:
- Keyword matching (exact and partial)
- Filename relevance scoring
- Content frequency analysis
- **No vector similarity or neural embeddings**

**Performance Considerations**:
- File loaded into memory on startup
- In-memory keyword search is fast
- Suitable for datasets up to 100K chunks

---

## 📡 API Documentation

### Base URL
- **Development**: `http://localhost:8080/api`
- **Production**: Same origin as frontend

---

### Authentication
Currently using simple username-based authentication (no passwords).

---

### Endpoints

#### User Management

**POST `/api/users/login`**
```json
Request:
{
  "username": "john_doe"
}

Response: 200 OK
{
  "id": "507f1f77bcf86cd799439011",
  "username": "john_doe",
  "email": "john@example.com",
  "createdAt": "2025-12-01T10:00:00Z"
}

Error: 404 Not Found
{
  "error": "User not found"
}
```

---

**POST `/api/users/register`**
```json
Request:
{
  "username": "jane_doe",
  "email": "jane@example.com"
}

Response: 201 Created
{
  "id": "507f1f77bcf86cd799439014",
  "username": "jane_doe",
  "email": "jane@example.com",
  "createdAt": "2025-12-02T12:00:00Z"
}

Error: 409 Conflict
{
  "error": "Username already exists"
}
```

---

**GET `/api/users/{username}`**
```json
Response: 200 OK
{
  "id": "507f1f77bcf86cd799439011",
  "username": "john_doe",
  "email": "john@example.com",
  "createdAt": "2025-12-01T10:00:00Z",
  "lastLogin": "2025-12-02T12:00:00Z"
}
```

---

#### Chat Operations

**POST `/api/chat/message`**
```json
Request:
{
  "username": "john_doe",
  "message": "How to install MAS?",
  "conversationId": "uuid-123"
}

Response: 200 OK
{
  "reply": "To install MAS, follow these steps:\n1. Ensure OpenShift 4.8+\n2. Run install script...",
  "sources": [
    {
      "file": "installation.md",
      "repository": "knowledge-center",
      "relevance": 0.92
    }
  ],
  "conversationId": "uuid-123",
  "timestamp": "2025-12-02T12:00:05Z"
}

Error: 500 Internal Server Error
{
  "error": "AI service unavailable"
}
```

---

#### Chat History

**GET `/api/history/{username}`**
```json
Response: 200 OK
[
  {
    "conversationId": "uuid-123",
    "title": "MAS Installation",
    "messageCount": 4,
    "createdAt": "2025-12-02T12:00:00Z",
    "updatedAt": "2025-12-02T12:05:00Z"
  },
  {
    "conversationId": "uuid-124",
    "title": "Configuration Questions",
    "messageCount": 2,
    "createdAt": "2025-12-01T14:00:00Z",
    "updatedAt": "2025-12-01T14:10:00Z"
  }
]
```

---

**GET `/api/history/session/{conversationId}`**
```json
Response: 200 OK
{
  "conversationId": "uuid-123",
  "username": "john_doe",
  "title": "MAS Installation",
  "messages": [
    {
      "role": "user",
      "content": "How to install MAS?",
      "timestamp": "2025-12-02T12:00:00Z"
    },
    {
      "role": "assistant",
      "content": "To install MAS...",
      "sources": ["installation.md"],
      "timestamp": "2025-12-02T12:00:05Z"
    }
  ],
  "createdAt": "2025-12-02T12:00:00Z"
}
```

---

**GET `/api/history/count/{username}`**
```json
Response: 200 OK
{
  "count": 15
}
```

---

#### Health Check

**GET `/api/health`**
```json
Response: 200 OK
{
  "status": "UP",
  "components": {
    "mongodb": {
      "status": "UP",
      "details": {
        "version": "7.0.0",
        "connection": "connected"
      }
    },
    "ollama": {
      "status": "UP",
      "details": {
        "url": "http://localhost:11434",
        "model": "granite4:micro-h"
      }
    },
    "embeddingStore": {
      "status": "UP",
      "details": {
        "type": "file",
        "embeddingsCount": 2400
      }
    },
    "github": {
      "status": "UP",
      "details": {
        "repositoriesConfigured": 4
      }
    }
  }
}
```

---

## 🚀 Deployment

### Single JAR Deployment

The application is configured to build as a **single executable JAR** containing both frontend and backend.

#### Build Process

```bash
# Build single JAR
./build-single-jar.sh

# Or manually
cd backend
./mvnw clean package -DskipTests
```

**Build Steps**:
1. **Clean**: Remove old build artifacts
2. **Frontend Build**:
   - Install Node.js and npm (via frontend-maven-plugin)
   - Run `npm install` in frontend directory
   - Run `npm run build` (Vite)
   - Output to `backend/src/main/resources/static/`
3. **Backend Compile**:
   - Compile Java classes
   - Copy resources (including static files)
4. **Package**:
   - Create JAR with Spring Boot repackager
   - Include all dependencies
   - Output: `backend/target/ai-chatbot-backend-1.0.0.jar`

**Output**:
- JAR file: ~75MB
- Contains: Backend classes + Frontend assets + Dependencies

---

#### Running the Application

```bash
# Run JAR
java -jar backend/target/ai-chatbot-backend-1.0.0.jar

# With environment variables
export SPRING_DATA_MONGODB_URI="mongodb://localhost:27017/chatbot"
export SPRING_AI_OLLAMA_BASE_URL="http://localhost:11434"
java -jar backend/target/ai-chatbot-backend-1.0.0.jar
```

**Access**:
- Frontend: `http://localhost:8080`
- API: `http://localhost:8080/api/*`

---

#### Prerequisites

1. **Java 17+**
```bash
java -version
# Should show: java version "17" or higher
```

2. **MongoDB** (Cloud or Local)
```bash
# Using MongoDB Atlas (cloud) - already configured
# Or local:
mongod --dbpath /path/to/data
```

3. **Ollama** (Local AI)
```bash
# Install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Download model
ollama pull granite4:micro-h

# Verify
ollama list
```

4. **GitHub Token**
- Create Personal Access Token
- Update `application.properties`:
```properties
repo.github.token=your_token_here
```

---

## ⚙️ Configuration Guide

### Application Properties

**Location**: `backend/src/main/resources/application.properties`

#### Server Configuration
```properties
# Port
server.port=8080

# Static resources (React frontend)
spring.web.resources.static-locations=classpath:/static/
spring.web.resources.add-mappings=true
```

---

#### MongoDB Configuration
```properties
# Connection URI (MongoDB Atlas)
spring.data.mongodb.uri=mongodb+srv://user:pass@cluster.mongodb.net/Chatbot?retryWrites=true&w=majority

# Database name
spring.data.mongodb.database=Chatbot
```

**Environment Variable Override**:
```bash
export SPRING_DATA_MONGODB_URI="mongodb://localhost:27017/chatbot"
```

---

#### Ollama AI Configuration
```properties
# Ollama URL
spring.ai.ollama.base-url=http://localhost:11434

# Chat model (for generating responses)
spring.ai.ollama.chat.model=granite4:micro-h

# Embedding model (reserved, not currently used)
spring.ai.ollama.embedding.model=granite4:micro-h
```

**Important Notes**:
- **Chat Model**: Used for generating AI responses
- **Embedding Model**: Configuration exists but NOT used
- **Search Method**: Keyword-based (not neural embeddings)

**Supported Chat Models**:
- `granite4:micro-h` (recommended)
- `llama2`
- `mistral`
- `codellama`

---

#### GitHub Configuration
```properties
# GitHub API base URL
repo.github.baseurl=https://github.ibm.com/api/v3

# GitHub token
repo.github.token=github_pat_xxxxx

# Repository 1
repo.github.repositories[0].owner=maximo-application-suite
repo.github.repositories[0].name=knowledge-center
repo.github.repositories[0].branch=main

# Repository 2
repo.github.repositories[1].owner=maximo-application-suite
repo.github.repositories[1].name=mas-suite-install
repo.github.repositories[1].branch=main
```

**Adding More Repositories**:
```properties
repo.github.repositories[4].owner=your-org
repo.github.repositories[4].name=your-repo
repo.github.repositories[4].branch=main
```

---

#### Document Store Configuration
```properties
# Storage type (file-based document chunks)
embedding.store=file

# Storage directory
embedding.store.dir=data/embeddings

# Clean document chunks on startup
rag.cleanOnStartup=true

# Note: No vector embeddings are generated
# System uses keyword-based search only
```

---

#### CORS Configuration
```properties
# Allowed origins (for development)
cors.allowed-origins=http://localhost:3000,http://localhost:5173

# Allowed methods
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS

# Allowed headers
cors.allowed-headers=*

# Allow credentials
cors.allow-credentials=true
```

**Note**: CORS is not needed in production single JAR deployment since frontend and backend share the same origin.

---

#### Performance Configuration
```properties
# Document processing
file.processing.chunk-size=2000
file.processing.batch-size=50

# Rate limiting
rate.limiting.requests-per-minute=60

# Async timeout (5 minutes)
spring.mvc.async.request-timeout=300000

# Thread pool
spring.task.execution.pool.core-size=10
spring.task.execution.pool.max-size=50
spring.task.execution.pool.queue-capacity=1000
```

---

#### Logging Configuration
```properties
# Application logging
logging.level.com.aichatbot=INFO

# Spring AI logging
logging.level.org.springframework.ai=DEBUG

# Root logging
logging.level.root=WARN

# Log pattern
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

---

### Environment Variables

Override properties using environment variables:

```bash
# MongoDB
export SPRING_DATA_MONGODB_URI="mongodb://localhost:27017/chatbot"

# Ollama
export SPRING_AI_OLLAMA_BASE_URL="http://localhost:11434"
export SPRING_AI_OLLAMA_CHAT_MODEL="llama2"

# GitHub
export REPO_GITHUB_TOKEN="github_pat_xxxxx"

# Server
export SERVER_PORT=9090
```

---

## 🔍 Key Features Explained

### 1. Retrieval-Augmented Generation (RAG) - Keyword-Based

**What is RAG?**
- Combines document retrieval with AI generation
- Provides context to AI for accurate answers
- Reduces hallucinations

**How it works in this project**:
1. **Indexing Phase**:
   - Fetch documents from GitHub
   - Split into chunks (2000 chars)
   - Store chunks with metadata (NO embeddings generated)
   - Save to JSONL file

2. **Query Phase**:
   - User asks question
   - Tokenize query into keywords
   - Search document chunks using keyword matching
   - Score chunks based on:
     - Exact word matches
     - Filename relevance
     - Content frequency
   - Rank and return top chunks
   - Provide context to AI
   - Generate answer

**Benefits**:
- Answers based on actual documentation
- Source citations for verification
- Up-to-date information from repositories
- Fast keyword-based search (no vector computation)
- Simpler architecture (no neural embedding models)

**Important Note**:
- This system uses **keyword-based search**, NOT neural embeddings
- Ollama is used ONLY for generating chat responses
- No vector similarity or embedding generation occurs

---

### 2. Single JAR Deployment

**Architecture**:
```
ai-chatbot-backend-1.0.0.jar
├── BOOT-INF/
│   ├── classes/
│   │   ├── com/aichatbot/          # Backend classes
│   │   ├── application.properties
│   │   └── static/                 # Frontend build
│   │       ├── index.html
│   │       └── assets/
│   │           ├── index-xxx.js
│   │           └── index-xxx.css
│   └── lib/                        # Dependencies
└── META-INF/
```

**How it works**:
1. Spring Boot serves static files from `/static`
2. `SpaForwardingController` handles React Router
3. Non-API routes forward to `index.html`
4. React Router handles client-side routing
5. API calls go to `/api/*` endpoints

**Benefits**:
- Single file deployment
- No CORS issues
- Version consistency
- Easy scaling

---

### 3. File-Based Document Store (Keyword Search)

**Why file-based?**
- Simple setup
- No additional database
- Easy to backup
- Portable
- Fast keyword search

**Format**: JSON Lines (.jsonl)
```json
{"id":"doc1","contentChunk":"...","filePath":"...","metadata":{}}
{"id":"doc2","contentChunk":"...","filePath":"...","metadata":{}}
```

**Important**: This file stores **plain text chunks only**, NOT embeddings

**Operations**:
- **Load**: Read entire file into memory
- **Search**: Keyword matching and relevance scoring
- **Add**: Append new document chunk
- **Persist**: Write back to file

**Search Method**:
- Tokenize query into keywords
- Match keywords against document content
- Score based on:
  - Exact matches (high score)
  - Partial matches (medium score)
  - Filename matches (bonus)
- No vector similarity calculation

**Performance**:
- Fast for < 100K document chunks
- In-memory keyword search is very fast
- No expensive vector operations

---

### 4. MongoDB Integration

**Collections**:
- `users`: User accounts
- `chatHistory`: Conversation history
- `executionHistory`: Pipeline logs

**Benefits**:
- Flexible schema
- Easy scaling
- Cloud-ready (MongoDB Atlas)
- Fast queries with indexes

---

## 📊 Architecture Diagrams

### Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Browser                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  React Application                                      │    │
│  │  - Login UI                                             │    │
│  │  - Chat Interface                                       │    │
│  │  - Admin Panel                                          │    │
│  └─────────────────────────────────────────────────────────┘    │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP/REST
                             ▼
┌────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                     │
│  ┌────────────────────────────────────────────────────────┐    │
│  │  Controllers (REST API)                                │    │
│  │  - Chat, User, History, Admin                          │    │
│  └────────────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────────────┐    │
│  │  Services (Business Logic)                             │    │
│  │  - ChatService, RAGService, GitHubService              │    │
│  └────────────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────────────┐    │
│  │  Repositories (Data Access)                            │    │
│  │  - MongoDB, FileEmbeddingStore                         │    │
│  └────────────────────────────────────────────────────────┘    │
└──────┬──────────────┬──────────────┬───────────────────────────┘
       │              │              │
       ▼              ▼              ▼
  ┌─────────┐   ┌─────────┐   ┌──────────┐
  │ MongoDB │   │ Ollama  │   │  GitHub  │
  │  Atlas  │   │  Local  │   │   API    │
  └─────────┘   └─────────┘   └──────────┘
```

---

### Sequence Diagram: Chat Flow

```
User → React → Controller → Service → RAG → Ollama → MongoDB
 │       │         │          │       │       │         │
 │ Type  │         │          │       │       │         │
 ├──────>│         │          │       │       │         │
 │       │ POST    │          │       │       │         │
 │       ├────────>│          │       │       │         │
 │       │         │ process  │       │       │         │
 │       │         ├─────────>│       │       │         │
 │       │         │          │retrieve│      │         │
 │       │         │          ├──────>│       │         │
 │       │         │          │       │embed  │         │
 │       │         │          │       ├──────>│         │
 │       │         │          │       │<──────┤         │
 │       │         │          │<──────┤       │         │
 │       │         │          │ chat  │       │         │
 │       │         │          ├──────────────>│         │
 │       │         │          │<──────────────┤         │
 │       │         │          │ save  │       │         │
 │       │         │          ├──────────────────────>  │
 │       │         │<─────────┤       │       │         │
 │       │<────────┤          │       │       │         │
 │<──────┤         │          │       │       │         │
 │       │         │          │       │       │         │
```

---

## 🎯 Summary

This AI Chatbot application is a **production-ready, full-stack solution** that combines:

1. **Modern Tech Stack**: Spring Boot 3 + React 18 + MongoDB + Ollama AI
2. **Single JAR Deployment**: Easy to deploy and scale
3. **RAG System**: Accurate answers based on real documentation
4. **GitHub Integration**: Automatic indexing of repositories
5. **Chat History**: Persistent conversation storage

**Key Files**:
- **Backend Entry**: `AiChatbotApplication.java`
- **Frontend Entry**: `main.jsx`
- **Configuration**: `application.properties`
- **Build**: `pom.xml` + `vite.config.js`
- **Deployment**: `build-single-jar.sh`

**Data Flow**:
- **Frontend → Backend**: REST API calls
- **Backend → MongoDB**: Data persistence
- **Backend → Ollama**: AI processing
- **Backend → GitHub**: Document retrieval

This documentation provides a complete understanding of the project architecture, implementation, and deployment.
