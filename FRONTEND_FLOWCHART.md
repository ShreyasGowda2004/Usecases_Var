# 🎨 AI Chatbot Frontend Architecture & Flow Documentation

## 📋 Table of Contents
1. [System Overview](#system-overview)
2. [Complete Architecture Diagram](#complete-architecture-diagram)
3. [Component Details](#component-details)
4. [Data Flow Diagrams](#data-flow-diagrams)
5. [User Interaction Flows](#user-interaction-flows)
6. [State Management](#state-management)
7. [API Integration](#api-integration)

---

## 🎯 System Overview

The AI Chatbot Frontend is a modern React application built with **Vite** and **IBM Carbon Design System**, providing an intelligent chat interface for IBM Maximo Application Suite with integrated API execution capabilities.

### Key Technologies
- **React 18.3.1** - UI framework
- **Vite 5.4.2** - Build tool and dev server
- **IBM Carbon Design System** - UI components
- **React Router** - Navigation
- **React Markdown** - Content rendering
- **UUID** - Session management

---

## 🔄 Complete Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           FRONTEND APPLICATION LAYER                         │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐     │
│  │                      BROWSER (Port 3000)                            │     │
│  ├─────────────────────────────────────────────────────────────────────┤     │
│  │                                                                     │     │
│  │  ┌────────────────────────────────────────────────────────────┐     │     │
│  │  │                    App.jsx (Root)                          │     │     │
│  │  │  - Theme Management (Light/Dark)                           │     │     │
│  │  │  - Tab Navigation (Chat/Admin)                             │     │     │
│  │  │  - Health Status Display                                   │     │     │
│  │  └────────────┬────────────────────────────┬──────────────────┘     │     │
│  │               │                            │                        │     │
│  │               ▼                            ▼                        │     │
│  │  ┌────────────────────────┐  ┌──────────────────────────────┐       │     │
│  │  │  CarbonChatInterface   │  │     AdminPanel               │       │     │
│  │  │  (Main Chat UI)        │  │  - Repository Management     │       │     │
│  │  └────────────┬───────────┘  │  - System Configuration      │       │     │
│  │               │              │  - Instance Management       │       │     │
│  │               │              └──────────────────────────────┘       │     │
│  │               │                                                     │     │
│  │               ├──────────────────┬──────────────────┬───────        ┤     │
│  │               │                  │                  │               │     │
│  │               ▼                  ▼                  ▼               │     │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐       │     │
│  │  │ ExecutionConsole │  │ ExecutionHistory │  │ Login Modal  │       │     │
│  │  │ (API Tester)     │  │ (Past Executions)│  │ (Auth)       │       │     │
│  │  └──────────────────┘  └──────────────────┘  └──────────────┘       │     │
│  │                                                                     │     │
│  └─────────────────────────────────────────────────────────────────────┘     │
│                                                                              │
└───────────────────────────────┬──────────────────────────────────────────────┘
                                │
                                │ HTTP/REST API
                                ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                          API INTEGRATION LAYER                               │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐     │
│  │                      utils/api.js (API Helpers)                     │     │
│  ├─────────────────────────────────────────────────────────────────────┤     │
│  │                                                                     │     │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────────────────┐     │     │
│  │  │  userAPI     │  │  historyAPI  │  │  executionHistoryAPI   │     │     │
│  │  │              │  │              │  │                        │     │     │
│  │  │ • login()    │  │ • save()     │  │ • saveExecution()      │     │     │
│  │  │ • register() │  │ • get()      │  │ • getUserExecutions()  │     │     │
│  │  │ • getUser()  │  │ • delete()   │  │ • deleteExecution()    │     │     │
│  │  │ • updateCfg()│  │ • count()    │  │ • deleteAll()          │     │     │
│  │  └──────────────┘  └──────────────┘  └────────────────────────┘     │     │
│  │                                                                     │     │
│  └─────────────────────────────────────────────────────────────────────┘     │
│                                                                              │
└───────────────────────────────┬──────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                    BACKEND API ENDPOINTS (Port 8080)                         │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  • POST   /api/chat/message           → Send chat message                    │
│  • POST   /api/users/login            → User authentication                  │
│  • POST   /api/users/register         → User registration                    │
│  • GET    /api/users/{username}       → Get user details                     │
│  • POST   /api/history                → Save chat history                    │
│  • GET    /api/history/{username}     → Get user history                     │
│  • DELETE /api/history/{id}           → Delete history                       │
│  • POST   /api/execution-history      → Save execution                       │
│  • GET    /api/execution-history/{user} → Get executions                     │
│  • POST   /api/proxy                  → Execute HTTP request                 │
│  • GET    /api/health                 → System health                        │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Component Details

### 1. **App.jsx** (Root Component)

```
Purpose: Application container and theme management
Responsibilities:
  - Theme switching (Light/Dark mode)
  - Global state initialization
  - Health status monitoring
  - User authentication state
```

**Key Features:**
- **Theme Management**: Carbon theme switching with persistence
- **Status Indicator**: Real-time backend health status
- **Responsive Layout**: Adapts to desktop and mobile

---

### 2. **CarbonChatInterface.jsx** ⭐ CORE CHAT COMPONENT

**Purpose:** Main chat interface with intelligent response parsing and execution capabilities

**Key Features:**

#### A. Welcome Screen
```jsx
/**
 * Initial landing view with sample questions
 * 
 * Features:
 * - Product-specific branding (Maximo)
 * - Sample question cards (4 quick-start options)
 * - One-click question submission
 * - Smooth transition to chat view
 */
```

#### B. Message Streaming
```jsx
/**
 * Real-time streaming of AI responses
 * 
 * Algorithm:
 * 1. Fetch API with streaming enabled
 * 2. Read response chunks using ReadableStream
 * 3. Parse Server-Sent Events (SSE) format
 * 4. Update UI incrementally as data arrives
 * 5. Detect executability during streaming
 * 
 * Performance:
 * - Sub-second first-token display
 * - Smooth character-by-character rendering
 * - No UI blocking during stream
 */
```

#### C. Executable Request Detection
```jsx
/**
 * Intelligent parsing of API requests from assistant responses
 * 
 * Detection Algorithm:
 * 1. Scan message content for HTTP method keywords
 *    - GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS
 * 
 * 2. Extract structured data:
 *    - Method: Detect from "Method:", "method:", or inline text
 *    - URL: Parse from "URL:", "Endpoint:", or "url:" patterns
 *    - Headers: Extract "apikey:", "Content-Type:", etc.
 *    - Parameters: Find "lean:", "oslc.select:", query params
 *    - Body: Detect JSON/XML request bodies
 * 
 * 3. Validate completeness:
 *    - Must have method + URL
 *    - Optional: headers, params, body
 * 
 * 4. Build request object:
 *    {
 *      url: '/MXAPIASSET',
 *      method: 'POST',
 *      headers: [{key: 'apikey', value: '<your-apikey-value>'}],
 *      params: [{key: 'lean', value: '1'}],
 *      body: '{"assetnum": "12345"}'
 *    }
 * 
 * 5. Trigger execution UI:
 *    - Show Manual/Automatic choice modal
 *    - Enable "Execute" button with prefilled data
 * 
 * Supported Formats:
 * - Markdown tables
 * - Inline key-value pairs
 * - Code blocks (JSON/XML)
 * - Plain text with keywords
 */
```

#### D. Manual/Automatic Execution Choice
```jsx
/**
 * User prompt for execution preference
 * 
 * Triggered When:
 * - Assistant response contains executable API request
 * - Method and URL successfully extracted
 * - User hasn't made a previous choice (or choice expired)
 * 
 * Options:
 * 1. Manual Mode:
 *    - Opens ExecutionConsole with prefilled data
 *    - User can modify before sending
 *    - Full control over headers/params/body
 * 
 * 2. Automatic Mode:
 *    - Executes request immediately
 *    - Uses extracted values as-is
 *    - Shows result inline in chat
 * 
 * Persistence:
 * - Choice stored in session state
 * - Applies to all future executable requests
 * - Can be changed via settings
 */
```

#### E. Message Rendering
```jsx
/**
 * Markdown rendering with custom components
 * 
 * Libraries:
 * - ReactMarkdown: Core markdown parsing
 * - remark-gfm: GitHub Flavored Markdown (tables, strikethrough)
 * 
 * Custom Components:
 * - h1, h2, h3: Styled headings with Carbon typography
 * - p: Paragraph with code block detection
 * - code: Inline code vs CodeSnippet blocks
 * - pre: Wrapper for code blocks
 * - ul/ol: Styled lists
 * - table: Carbon DataTable-style rendering
 * - a: External link handling
 * 
 * Memoization Strategy:
 * - Memoize markdown components to prevent re-renders
 * - Prevents CodeSnippet flicker on streaming
 * - Stable references across renders
 * 
 * Streaming Optimization:
 * - Simple <pre> blocks during streaming
 * - Full CodeSnippet after stream complete
 * - Avoids component re-mounting glitches
 */
```

#### F. Expand/Collapse Long Messages
```jsx
/**
 * Show More/Less functionality for lengthy responses
 * 
 * Algorithm:
 * 1. Check message length (characters)
 * 2. If > 4000 chars, mark as collapsible
 * 3. Initially show first ~3000 characters
 * 4. Display "Show more" button with chevron icon
 * 5. On click, reveal full content
 * 6. Change to "Show less" with reversed chevron
 * 
 * UI Features:
 * - Smooth expand/collapse animation
 * - Rotate arrow icon (down → up)
 * - Preserve scroll position
 * - Visible button styling (IBM blue)
 */
```

#### G. Section-Based Execution
```jsx
/**
 * Execute specific sections within long responses
 * 
 * Use Case:
 * - Response contains multiple API examples
 * - User wants to execute one specific example
 * 
 * Detection:
 * - Parse response into logical sections (by heading)
 * - Find executable requests within each section
 * - Associate "Execute" button with section context
 * 
 * Execution:
 * - Opens ExecutionConsole with section-specific data
 * - Title reflects the section (e.g., "Create Asset")
 * - Isolated from other sections in same response
 */
```

---

### 3. **ExecutionConsole.jsx** ⭐ API EXECUTION ENGINE

**Purpose:** Lightweight Postman-like HTTP request runner

**Key Features:**

#### A. Request Builder
```jsx
/**
 * Visual request composer
 * 
 * Components:
 * 1. Method Selector (Dropdown)
 *    - GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS
 * 
 * 2. URL Input (TextInput)
 *    - Supports absolute URLs (https://...)
 *    - Supports relative paths (/MXAPIASSET)
 *    - Auto-completes with instance URL
 * 
 * 3. Instance Selector (Dropdown)
 *    - List of configured Maximo instances
 *    - Auto-applies hostname + API key
 *    - Replaces placeholders in URL/headers
 * 
 * 4. Headers Tab
 *    - Key-value pair editor
 *    - Add/remove rows dynamically
 *    - Common headers: apikey, Content-Type, Accept
 * 
 * 5. Parameters Tab (Query Params)
 *    - Key-value pair editor
 *    - Auto-appends to URL
 *    - Example: lean=1, oslc.select=assetnum
 * 
 * 6. Body Tab
 *    - Raw JSON editor (TextArea)
 *    - Auto-formatting on paste
 *    - Syntax validation
 */
```

#### B. Instance Management
```jsx
/**
 * Maximo instance configuration
 * 
 * Instance Object:
 * {
 *   id: 'prod-maximo-1',
 *   name: 'Production Maximo',
 *   url: 'https://maximo.example.com',
 *   apiKey: 'abc123xyz',
 *   description: 'Production environment'
 * }
 * 
 * Placeholder Replacement:
 * - hostname → maximo.example.com
 * - https://hostname → https://maximo.example.com
 * - <your-apikey-value> → abc123xyz
 * 
 * Applied To:
 * - URL field
 * - Header values
 * - Parameter values
 * - Request body
 */
```

#### C. Request Execution
```jsx
/**
 * HTTP request proxy execution
 * 
 * Flow:
 * 1. Validate inputs (method, URL, headers)
 * 2. Build request object:
 *    {
 *      method: 'POST',
 *      url: 'https://maximo.example.com/MXAPIASSET',
 *      headers: { 'apikey': 'abc123', 'Content-Type': 'application/json' },
 *      body: '{"assetnum": "12345"}'
 *    }
 * 
 * 3. Send to backend proxy:
 *    POST /api/proxy
 *    Body: { method, url, headers, body }
 * 
 * 4. Backend proxy makes actual HTTP call to Maximo
 * 
 * 5. Return response envelope:
 *    {
 *      status: 201,
 *      statusText: 'Created',
 *      headers: { 'Content-Type': 'application/json', ... },
 *      body: '{"_rowstamp": "123456", ...}'
 *    }
 * 
 * 6. Display response in UI:
 *    - Status code badge (200-299: success, 400-599: error)
 *    - Response time (ms)
 *    - Response size (bytes)
 *    - Headers table
 *    - Body (formatted JSON or raw text)
 * 
 * Error Handling:
 * - Proxy errors (our backend failed)
 * - Target server errors (Maximo returned error)
 * - Network errors (timeout, connection refused)
 * - Invalid JSON in request body
 * - Missing required headers
 * 
 * Special Cases:
 * - 204 No Content: Show empty body (no placeholder)
 * - 205 Reset Content: Show empty body
 * - Non-JSON responses: Display as plain text
 * - Large responses: Auto-collapse body
 */
```

#### D. Response Display
```jsx
/**
 * Response viewer with tabs
 * 
 * Tabs:
 * 1. Body (CodeSnippet)
 *    - Auto-formatted JSON
 *    - Plain text for non-JSON
 *    - Empty for 204/205 responses
 *    - Copy button for easy sharing
 * 
 * 2. Headers (Table)
 *    - Key-value list of response headers
 *    - Content-Type, Content-Length, etc.
 * 
 * 3. Meta (Info)
 *    - Status: 200 OK
 *    - Time: 245ms
 *    - Size: 1.2 KB
 */
```

#### E. Execution History Integration
```jsx
/**
 * Save all executions to history
 * 
 * Saved Data:
 * - username: Current user
 * - timestamp: ISO 8601 timestamp
 * - source: 'console' | 'automatic' | 'section'
 * - instanceId: Selected instance ID
 * - actionTitle: "Create Asset" (extracted from context)
 * - method: 'POST'
 * - url: '/MXAPIASSET'
 * - requestHeaders: [{key: 'apikey', value: 'xxx'}]
 * - requestParams: [{key: 'lean', value: '1'}]
 * - requestBody: '{"assetnum": "12345"}'
 * - statusCode: 201
 * - durationMs: 245
 * - responseHeaders: [{key: 'Content-Type', value: 'application/json'}]
 * - responseBody: '{"_rowstamp": "123456"}'
 * - status: 'success' | 'error'
 * 
 * Storage: MongoDB via backend API
 */
```

---

### 4. **ExecutionHistory.jsx** 📜 PAST EXECUTIONS VIEWER

**Purpose:** View and replay past API executions

**Key Features:**

#### A. History List
```jsx
/**
 * Timeline view of past executions
 * 
 * Display:
 * - Action title (e.g., "Create Asset")
 * - Method badge (POST, GET, etc.)
 * - URL endpoint
 * - Timestamp (relative: "2 hours ago")
 * - Status badge (Success ✓ / Error ✗)
 * - Duration (ms)
 * 
 * Sorting:
 * - Newest first (default)
 * - Oldest first
 * - By status (errors first)
 * 
 * Filtering:
 * - By method (GET, POST, etc.)
 * - By status (success/error)
 * - By date range
 * - By search term (URL/title)
 */
```

#### B. Execution Details
```jsx
/**
 * Expandable execution detail view
 * 
 * Sections:
 * 1. Request Details
 *    - Method, URL, Headers, Params, Body
 *    - Instance used
 *    - Timestamp
 * 
 * 2. Response Details
 *    - Status code, Headers, Body
 *    - Duration, Size
 * 
 * 3. Actions
 *    - Replay: Open ExecutionConsole with same data
 *    - Delete: Remove from history
 *    - Copy: Copy request as cURL command
 */
```

---

### 5. **Login.jsx** 🔐 AUTHENTICATION

**Purpose:** User authentication and registration

**Key Features:**
- Username-based login (no password for demo)
- New user registration
- Session persistence (localStorage)
- Auto-login on return visit

---

## 🔄 Data Flow Diagrams

### Complete User Interaction Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                     CHAT MESSAGE PROCESSING FLOW                         │
└──────────────────────────────────────────────────────────────────────────┘

1. USER TYPES MESSAGE
   │
   ├─► Input: "create asset"
   │   • TextInput component (Carbon)
   │   • Enter key or Send button
   │
   ▼

2. MESSAGE SUBMISSION
   │
   ├─► CarbonChatInterface.sendMessage()
   │   • Generate unique message ID
   │   • Add user message to state
   │   • Clear input field
   │   • Scroll to bottom
   │
   ▼

3. BACKEND API CALL
   │
   ├─► POST /api/chat/message
   │   Headers: Content-Type: application/json
   │   Body: {
   │     message: "create asset",
   │     sessionId: "web-session-123",
   │     includeContext: true,
   │     fastMode: true,
   │     fullContent: true
   │   }
   │
   ▼

4. STREAMING RESPONSE
   │
   ├─► streamResponseFromFetch()
   │   │
   │   ├─► Read response.body (ReadableStream)
   │   │   • Get reader: stream.getReader()
   │   │   • Read chunks: reader.read()
   │   │
   │   ├─► Parse SSE format
   │   │   • Lines starting with "data: "
   │   │   • JSON decode each line
   │   │
   │   ├─► Update UI incrementally
   │   │   • Append text to assistant message
   │   │   • Re-render message component
   │   │   • Smooth character-by-character display
   │   │
   │   └─► Detect executability during stream
   │       • Scan for HTTP methods
   │       • Mark as executable when found
   │
   ▼

5. EXECUTABLE DETECTION
   │
   ├─► isExecutableApiResponse(content)
   │   │
   │   ├─► Regex patterns:
   │   │   • Method: /\b(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\b/i
   │   │   • URL: /URL:\s*([^\s\n]+)|url:\s*([^\s\n]+)|Endpoint:\s*([^\s\n]+)/i
   │   │
   │   ├─► Validation:
   │   │   if (hasMethod && hasUrl) → executable = true
   │   │
   │   └─► Result: true (message contains API request)
   │
   ▼

6. MANUAL/AUTOMATIC CHOICE PROMPT
   │
   ├─► Show modal dialog
   │   • "Would you like to execute this automatically?"
   │   • Buttons: "Manual" | "Automatic"
   │   • Checkbox: "Remember my choice"
   │
   ├─► User selects "Manual"
   │   • Store preference: executionPreference = 'manual'
   │   • Hide modal
   │   • Enable "Execute" button on message
   │
   └─► User selects "Automatic"
       • Store preference: executionPreference = 'automatic'
       • Hide modal
       • Auto-execute request immediately
       • Go to step 7
   │
   ▼

7. EXTRACT REQUEST DETAILS
   │
   ├─► extractRequestFromText(content)
   │   │
   │   ├─► Extract Method:
   │   │   Pattern: /Method:\s*(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)/i
   │   │   Result: "POST"
   │   │
   │   ├─► Extract URL:
   │   │   Pattern: /URL:\s*([^\s\n]+)/i
   │   │   Result: "/MXAPIASSET"
   │   │
   │   ├─► Extract Headers:
   │   │   Pattern: /apikey:\s*([^\n]+)|Content-Type:\s*([^\n]+)/i
   │   │   Result: [{key: 'apikey', value: '<your-apikey-value>'}]
   │   │
   │   ├─► Extract Parameters:
   │   │   Pattern: /lean:\s*(\d+)|oslc\.select:\s*([^\n]+)/i
   │   │   Result: [{key: 'lean', value: '1'}]
   │   │
   │   └─► Extract Body:
   │       • Find JSON block: ```json ... ```
   │       • Parse and validate
   │       • Result: '{"assetnum": "12345"}'
   │
   ▼

8. OPEN EXECUTION CONSOLE (Manual Mode)
   │
   ├─► setExecutionConsoleOpen(true)
   │   • Pass extracted request data as props
   │   • initialMethod: "POST"
   │   • initialUrl: "/MXAPIASSET"
   │   • initialHeaders: [{key: 'apikey', value: '<your-apikey-value>'}]
   │   • initialParams: [{key: 'lean', value: '1'}]
   │   • initialBody: '{"assetnum": "12345"}'
   │   • actionTitle: "Create Asset" (extracted from message)
   │
   ▼

9. USER MODIFIES REQUEST (Optional)
   │
   ├─► ExecutionConsole UI:
   │   • Change method: POST → PUT
   │   • Edit URL: /MXAPIASSET → /MXAPIASSET/12345
   │   • Add header: Accept: application/json
   │   • Update body: Add more fields
   │   • Select instance: "Production Maximo"
   │
   ▼

10. INSTANCE SELECTION & PLACEHOLDER REPLACEMENT
    │
    ├─► Selected Instance:
    │   {
    │     id: 'prod-1',
    │     name: 'Production Maximo',
    │     url: 'https://maximo.example.com',
    │     apiKey: 'abc123xyz'
    │   }
    │
    ├─► Placeholder Replacement Algorithm:
    │   │
    │   ├─► URL:
    │   │   Before: "/MXAPIASSET"
    │   │   After: "https://maximo.example.com/MXAPIASSET"
    │   │
    │   ├─► Headers:
    │   │   Before: {key: 'apikey', value: '<your-apikey-value>'}
    │   │   After: {key: 'apikey', value: 'abc123xyz'}
    │   │
    │   └─► Body:
    │       Before: '{"url": "https://hostname/path"}'
    │       After: '{"url": "https://maximo.example.com/path"}'
    │
    ▼

11. EXECUTE REQUEST
    │
    ├─► ExecutionConsole.sendRequest()
    │   │
    │   ├─► Validate inputs:
    │   │   • Method required
    │   │   • URL required and valid
    │   │   • Headers valid (no empty keys)
    │   │
    │   ├─► Build request payload:
    │   │   {
    │   │     method: 'POST',
    │   │     url: 'https://maximo.example.com/MXAPIASSET',
    │   │     headers: {
    │   │       'apikey': 'abc123xyz',
    │   │       'Content-Type': 'application/json'
    │   │     },
    │   │     body: '{"assetnum": "12345", "description": "New Asset"}'
    │   │   }
    │   │
    │   ├─► Send to proxy:
    │   │   POST /api/proxy
    │   │   Body: [request payload]
    │   │
    │   └─► Measure time: startTime = performance.now()
    │
    ▼

12. BACKEND PROXY EXECUTION
    │
    ├─► ProxyController.proxy()
    │   │
    │   ├─► Extract target URL and method
    │   │
    │   ├─► Build HTTP client request:
    │   │   • RestTemplate (Spring)
    │   │   • Set headers from request
    │   │   • Set body from request
    │   │
    │   ├─► Execute actual HTTP call to Maximo:
    │   │   POST https://maximo.example.com/MXAPIASSET
    │   │   Headers: apikey: abc123xyz, Content-Type: application/json
    │   │   Body: {"assetnum": "12345", "description": "New Asset"}
    │   │
    │   └─► Return response envelope:
    │       {
    │         status: 201,
    │         statusText: 'Created',
    │         headers: {
    │           'Content-Type': 'application/json',
    │           'Content-Length': '456'
    │         },
    │         body: '{"_rowstamp": "123456", "assetnum": "12345", ...}'
    │       }
    │
    ▼

13. RESPONSE PROCESSING
    │
    ├─► ExecutionConsole receives proxy response
    │   • endTime = performance.now()
    │   • duration = endTime - startTime (245ms)
    │
    ├─► Parse response envelope:
    │   • Check if empty body (204/205) → show blank
    │   • Try JSON parse → format with indentation
    │   • Fall back to plain text if not JSON
    │
    ├─► Update UI state:
    │   • setResponseMeta({
    │       status: '201 Created',
    │       timeMs: 245,
    │       size: 456,
    │       ok: true
    │     })
    │   • setResponseHeaders([{key: 'Content-Type', value: 'application/json'}])
    │   • setResponseBody('[formatted JSON]')
    │
    ▼

14. SAVE TO EXECUTION HISTORY
    │
    ├─► executionHistoryAPI.saveExecution()
    │   │
    │   ├─► POST /api/execution-history
    │   │   Body: {
    │   │     username: 'john.doe',
    │   │     timestamp: '2025-11-25T10:30:00.000Z',
    │   │     source: 'console',
    │   │     instanceId: 'prod-1',
    │   │     actionTitle: 'Create Asset',
    │   │     method: 'POST',
    │   │     url: '/MXAPIASSET',
    │   │     requestHeaders: [...],
    │   │     requestParams: [...],
    │   │     requestBody: '...',
    │   │     statusCode: 201,
    │   │     durationMs: 245,
    │   │     responseHeaders: [...],
    │   │     responseBody: '...',
    │   │     status: 'success'
    │   │   }
    │   │
    │   └─► Stored in MongoDB executionHistory collection
    │
    ▼

15. DISPLAY RESPONSE
    │
    ├─► Response Panel (ExecutionConsole):
    │   │
    │   ├─► Status Badge:
    │   │   • Color: Green (201 = success)
    │   │   • Text: "201 Created"
    │   │
    │   ├─► Metrics:
    │   │   • Time: 245ms
    │   │   • Size: 456 bytes
    │   │
    │   ├─► Tabs:
    │   │   │
    │   │   ├─► Body Tab:
    │   │   │   • CodeSnippet (Carbon)
    │   │   │   • Language: json
    │   │   │   • Content: [formatted response]
    │   │   │   • Copy button enabled
    │   │   │
    │   │   ├─► Headers Tab:
    │   │   │   • Table view
    │   │   │   • Key: Content-Type → Value: application/json
    │   │   │   • Key: Content-Length → Value: 456
    │   │   │
    │   │   └─► Meta Tab:
    │   │       • Status: 201 Created ✓
    │   │       • Time: 245ms
    │   │       • Size: 456 bytes
    │   │
    │   └─► Action Buttons:
    │       • Save to History (already done)
    │       • Copy as cURL
    │       • Close Console
    │
    └─► User can now:
        • View complete response
        • Copy data for use elsewhere
        • Close console and continue chatting
        • Replay request with modifications
```

---

## 🧠 State Management

### Component State Architecture

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        STATE MANAGEMENT DIAGRAM                          │
└──────────────────────────────────────────────────────────────────────────┘

App.jsx (Root)
│
├─► Global State:
│   • theme: 'light' | 'dark'
│   • systemStatus: { status: 'UP', ollama: {...}, mongodb: {...} }
│   • currentUser: { username: 'john.doe', email: '...' }
│
└─► Pass to children via props


CarbonChatInterface.jsx (Chat)
│
├─► Local State:
│   • messages: Array<Message>
│   │   Message: {
│   │     id: string,
│   │     type: 'user' | 'assistant',
│   │     content: string,
│   │     timestamp: Date,
│   │     sourceFiles: string[],
│   │     isError: boolean,
│   │     canExecute: boolean,
│   │     requestData: { method, url, headers, params, body }
│   │   }
│   │
│   • isLoading: boolean
│   • isStreaming: boolean
│   • inputValue: string
│   • sessionId: string (UUID)
│   • showWelcome: boolean
│   │
│   • executionPreference: 'manual' | 'automatic' | null
│   • showExecutionChoice: boolean
│   • pendingExecutionData: { ... }
│   │
│   • executionConsoleOpen: boolean
│   • executionHistoryOpen: boolean
│   │
│   • collapsedMessages: Set<string> (message IDs)
│   • hiddenMessages: Set<string> (message IDs for hidden sections)
│
├─► Derived State:
│   • hasMessages: messages.length > 0
│   • lastMessage: messages[messages.length - 1]
│   • executableMessages: messages.filter(m => m.canExecute)
│
└─► Effects:
    • Auto-scroll on new message
    • Persist session to localStorage
    • Load history on mount


ExecutionConsole.jsx (API Tester)
│
├─► Local State:
│   • method: string ('GET', 'POST', etc.)
│   • url: string
│   • headers: Array<{ key, value }>
│   • params: Array<{ key, value }>
│   • body: string (JSON)
│   • bodyMode: 'raw-json'
│   │
│   • selectedInstanceId: string
│   • activeTab: 'headers' | 'params' | 'body'
│   │
│   • isSending: boolean
│   • error: string | null
│   │
│   • responseMeta: { status, timeMs, size, ok }
│   • responseHeaders: Array<{ key, value }>
│   • responseBody: string
│   • responseTab: 'body' | 'headers' | 'meta'
│   │
│   • autoFormatJson: boolean
│
├─► Derived State:
│   • canSendBody: ['POST', 'PUT', 'PATCH'].includes(method)
│   • hasResponse: responseMeta !== null
│   • isSuccess: responseMeta?.ok === true
│
└─► Effects:
    • Apply instance placeholders on instance change
    • Validate URL on change
    • Auto-format JSON on paste


ExecutionHistory.jsx (Past Executions)
│
├─► Local State:
│   • executions: Array<ExecutionRecord>
│   • isLoading: boolean
│   • filter: { method: string[], status: string[], search: string }
│   • sortBy: 'newest' | 'oldest' | 'status'
│   • expandedExecutionId: string | null
│
├─► Derived State:
│   • filteredExecutions: executions filtered by filter + sortBy
│   • successCount: executions.filter(e => e.status === 'success').length
│   • errorCount: executions.filter(e => e.status === 'error').length
│
└─► Effects:
    • Load history on mount
    • Refresh on new execution saved
    • Poll for updates (optional)
```

---

## 🔌 API Integration

### API Helper Functions (utils/api.js)

```javascript
// User Management
userAPI.login(username)
  → POST /api/users/login
  → Returns: { id, username, email, role, createdAt }

userAPI.register(username, email)
  → POST /api/users/register
  → Returns: { id, username, email, role }

userAPI.getUser(username)
  → GET /api/users/{username}
  → Returns: { id, username, email, role, config }

userAPI.updateConfig(username, config)
  → PUT /api/users/{username}/config
  → Body: { instances: [...], preferences: {...} }
  → Returns: { success: true }


// Chat History
historyAPI.saveHistory(historyData)
  → POST /api/history
  → Body: {
      username: 'john.doe',
      title: 'Create Asset Discussion',
      messages: [
        { id, type: 'user', content, timestamp },
        { id, type: 'assistant', content, sourceFiles, timestamp }
      ]
    }
  → Returns: { id, createdAt }

historyAPI.getUserHistory(username)
  → GET /api/history/{username}
  → Returns: [{ id, username, title, createdAt, messages }]

historyAPI.getHistoryById(id)
  → GET /api/history/session/{id}
  → Returns: { id, username, title, createdAt, messages }

historyAPI.deleteHistory(id)
  → DELETE /api/history/{id}
  → Returns: { success: true, deleted: 1 }

historyAPI.deleteAllUserHistory(username)
  → DELETE /api/history/user/{username}
  → Returns: { success: true, deleted: count }


// Execution History
executionHistoryAPI.saveExecution(executionData)
  → POST /api/execution-history
  → Body: {
      username, timestamp, source, instanceId,
      actionTitle, method, url,
      requestHeaders, requestParams, requestBody,
      statusCode, durationMs,
      responseHeaders, responseBody,
      status: 'success' | 'error'
    }
  → Returns: { id, createdAt }

executionHistoryAPI.getUserExecutions(username, limit)
  → GET /api/execution-history/{username}?limit={limit}
  → Returns: [{ id, username, timestamp, actionTitle, method, url, statusCode, durationMs, status }]

executionHistoryAPI.getExecutionById(id)
  → GET /api/execution-history/execution/{id}
  → Returns: { id, username, ..., requestHeaders, requestBody, responseBody }

executionHistoryAPI.deleteExecution(id)
  → DELETE /api/execution-history/{id}
  → Returns: { success: true, deleted: 1 }

executionHistoryAPI.deleteAllUserExecutions(username)
  → DELETE /api/execution-history/user/{username}
  → Returns: { success: true, deleted: count }
```

---

## 🎨 UI Components & Styling

### Carbon Design System Integration

```
┌──────────────────────────────────────────────────────────────────────────┐
│                     CARBON COMPONENTS USAGE                              │
└──────────────────────────────────────────────────────────────────────────┘

From @carbon/react:

1. Button
   - Primary: Send message, Execute request
   - Secondary: Close modal, Cancel
   - Ghost: Show more, Expand section
   - Icon buttons: Send, Clear, Settings

2. TextInput
   - Message input (chat)
   - URL input (execution console)
   - Key/value inputs (headers/params)

3. TextArea
   - Request body editor
   - Multi-line text input

4. Select / SelectItem
   - Method selector (GET, POST, etc.)
   - Instance selector
   - Filter dropdowns

5. CodeSnippet
   - Inline code: `assetnum`
   - Multi-line code blocks: JSON/XML responses
   - Copy button included

6. Tile
   - Message bubbles (user/assistant)
   - Sample question cards
   - Execution result panels

7. Modal
   - Manual/Automatic choice
   - Login/Register
   - Confirmation dialogs

8. InlineNotification
   - Error messages
   - Success confirmations
   - Warning alerts

9. Tabs / Tab / TabList / TabPanels / TabPanel
   - Execution Console: Headers/Params/Body
   - Response viewer: Body/Headers/Meta

10. Loading
    - Spinner during API calls
    - Skeleton loaders for history

11. Tag
    - Status badges: Success/Error
    - Method badges: GET/POST/PUT
    - Filter tags

12. Grid / Column
    - Responsive layout
    - Multi-column layouts

13. Checkbox
    - Remember choice (execution preference)
    - Auto-format JSON toggle
    - Filter options

14. Link
    - External documentation links
    - Source file references


Icons (@carbon/icons-react):
- Send: Submit message
- User: User messages
- Watsonx: AI assistant messages
- Erase: Clear chat
- StopFilledAlt: Stop streaming
- PlayFilledAlt: Execute request
- ChevronDown/ChevronUp: Expand/collapse
- Close: Close modal/console
- Play: Execute button
- Add: Add header/param
- Subtract: Remove header/param
```

---

## 🎯 Key Design Decisions

### Why React + Vite?
- **Performance**: Lightning-fast hot module replacement (HMR)
- **Modern**: ES6+ support, optimized builds
- **Simple**: Minimal configuration compared to CRA
- **Fast**: Instant server start, optimized production builds

### Why Carbon Design System?
- **Enterprise-Ready**: IBM's official design system
- **Consistent**: Unified look and feel across all components
- **Accessible**: WCAG 2.1 AA compliant
- **Themeable**: Light/dark mode built-in
- **Comprehensive**: 50+ components, icons, patterns

### Why Streaming Responses?
- **User Experience**: Users see responses appear in real-time
- **Perceived Speed**: Feels faster than waiting for complete response
- **Engagement**: Keeps user engaged during processing
- **Transparency**: Shows AI is "thinking" and working

### Why Manual/Automatic Choice?
- **Control**: Users decide execution behavior
- **Safety**: Prevents unintended API calls
- **Flexibility**: Power users can automate, beginners can review
- **Trust**: Transparent about what will be executed

### Why Execution Console?
- **Debugging**: Inspect and modify requests before sending
- **Learning**: See exact API structure and format
- **Flexibility**: Test variations without re-asking AI
- **History**: Track all executions for reference

### Why Memoization for Markdown?
- **Performance**: Prevents re-rendering of code snippets
- **Stability**: Eliminates flicker during streaming
- **UX**: Smooth, professional appearance
- **Efficiency**: Reduces React reconciliation work

---

## 📊 Performance Metrics

| Operation | Time | Details |
|-----------|------|---------|
| Initial Page Load | 500-800ms | React bundle + Carbon CSS |
| Message Send | 50-100ms | Frontend processing |
| Streaming Start | 200-400ms | Backend connection + first token |
| Stream Complete | 1-2s | Full response received |
| Executable Detection | 10-30ms | Regex parsing |
| Console Open | 50-100ms | Component mount + render |
| Request Execution | 200-500ms | Proxy + target server |
| Response Display | 50-100ms | JSON formatting + render |
| **Total User Flow** | **2-3s** | Ask → See → Execute → Result |

| Resource | Size | Details |
|----------|------|---------|
| React Bundle (Dev) | ~2 MB | Unminified with source maps |
| React Bundle (Prod) | ~200 KB | Minified + gzipped |
| Carbon CSS | ~150 KB | Complete design system |
| Total JS (Prod) | ~350 KB | All dependencies |
| Initial Load (Prod) | ~500 KB | HTML + CSS + JS |

---

## 🔒 Security Considerations

1. **API Keys**: Never stored in localStorage (only in-memory during session)
2. **Proxy Pattern**: All external requests go through backend proxy
3. **Input Validation**: URL, headers, body validated before execution
4. **CORS**: Restricted to localhost:3000 in dev, configured for production
5. **XSS Prevention**: ReactMarkdown sanitizes user input
6. **Session Management**: UUID-based sessions, no JWT exposure

---

## 🚀 Deployment Architecture

```
Production Environment:

┌──────────────────────────────────────────────────────────────┐
│                        CDN / Static Hosting                  │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Frontend Assets (React Build):                              │
│  • HTML: index.html                                          │
│  • JS: main.[hash].js (~200 KB gzipped)                      │
│  • CSS: main.[hash].css (~150 KB)                            │
│  • Icons: Carbon icons embedded                              │
│                                                              │
│  Served via:                                                 │
│  • Nginx / Apache                                            │
│  • AWS CloudFront                                            │
│  • Azure CDN                                                 │
│  • Vercel / Netlify                                          │
│                                                              │
│  URL: https://chatbot.example.com                            │
│                                                              │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        │ API Proxy
                        │ /api/* → Backend
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│                        Backend API Server                    │
│                     (Spring Boot @ 8080)                     │
│                                                              │
│  URL: https://api.chatbot.example.com                        │
│  CORS: https://chatbot.example.com                           │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 📚 File Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── CarbonChatInterface.jsx      (Main chat UI)
│   │   ├── CarbonChatInterface.css      (Chat styles)
│   │   ├── ExecutionConsole.jsx         (API tester)
│   │   ├── ExecutionConsole.css         (Console styles)
│   │   ├── ExecutionHistory.jsx         (Past executions)
│   │   ├── ExecutionHistory.css         (History styles)
│   │   ├── Login.jsx                    (Auth UI)
│   │   └── Login.css                    (Auth styles)
│   │
│   ├── utils/
│   │   └── api.js                       (API helpers)
│   │
│   ├── App.jsx                          (Root component)
│   ├── App.css                          (Global styles)
│   ├── CarbonApp.jsx                    (Carbon theme wrapper)
│   ├── CarbonApp.css                    (Theme overrides)
│   ├── main.jsx                         (Entry point)
│   └── index.css                        (Reset styles)
│
├── index.html                           (HTML template)
├── package.json                         (Dependencies)
└── vite.config.js                       (Build config)
```

---

## 🎉 Key Features Summary

1. ✅ **Real-time Streaming**: AI responses appear character-by-character
2. ✅ **Intelligent Request Detection**: Auto-detects executable API requests
3. ✅ **Manual/Automatic Choice**: User controls execution behavior
4. ✅ **API Execution Console**: Full-featured HTTP request runner
5. ✅ **Execution History**: Track all past API calls
6. ✅ **Instance Management**: Configure multiple Maximo environments
7. ✅ **Placeholder Replacement**: Auto-fill instance URLs and API keys
8. ✅ **Response Formatting**: Auto-format JSON, handle all response types
9. ✅ **Dark Theme Support**: Carbon dark theme with persistence
10. ✅ **Markdown Rendering**: Rich formatting with code snippets
11. ✅ **Expand/Collapse**: Handle long responses gracefully
12. ✅ **Session Persistence**: Remember conversations across reloads
13. ✅ **Error Handling**: Comprehensive error messages and recovery
14. ✅ **Performance Optimized**: Memoization, lazy loading, efficient rendering

---

## 📖 Additional Documentation

- **API Reference:** `/API_REFERENCE.md`
- **Backend Architecture:** `/BACKEND_FLOWCHART.md`
- **Deployment Guide:** `/DEPLOYMENT.md`
- **Testing Guide:** `/TESTING.md`

---

**Last Updated:** November 25, 2025  
**Version:** 1.0.0  
**Maintainer:** Shreyas Gowda
