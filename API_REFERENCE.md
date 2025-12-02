# API Reference - AI Chatbot System

## 📋 Table of Contents
1. [Overview](#overview)
2. [Base URLs](#base-urls)
3. [Authentication](#authentication)
4. [Response Formats](#response-formats)
5. [Error Handling](#error-handling)
6. [Rate Limiting](#rate-limiting)
7. [Endpoints](#endpoints)
   - [User Management](#user-management)
   - [Chat Operations](#chat-operations)
   - [History Management](#history-management)
   - [GitHub Integration](#github-integration)
   - [Health & Monitoring](#health--monitoring)
   - [Proxy Service](#proxy-service)
8. [WebSocket API](#websocket-api)
9. [Code Examples](#code-examples)
10. [Postman Collection](#postman-collection)

---

## 🌐 Overview

The AI Chatbot API provides RESTful endpoints for managing users, chat interactions, conversation history, and GitHub repository integration.

### API Version
**Current Version**: v1  
**Last Updated**: November 5, 2025

### Key Features
- RESTful architecture
- JSON request/response format
- Server-Sent Events (SSE) for streaming
- CORS enabled
- Health monitoring endpoints

---

## 🔗 Base URLs

### Development
```
http://localhost:8080/api
```

### Production
```
https://your-domain.com/api
```

### Context Path
All endpoints are prefixed with `/api`

---

## 🔐 Authentication

Currently, the API uses simple username-based authentication. For production, consider implementing:
- JWT tokens
- OAuth 2.0
- API keys

### Current Authentication Flow
```http
POST /api/users/login
Content-Type: application/json

{
  "username": "your-username"
}
```

**Response**:
```json
{
  "id": "user123",
  "username": "your-username",
  "createdAt": "2025-11-05T10:30:00Z"
}
```

---

## 📝 Response Formats

### Success Response
```json
{
  "status": "success",
  "data": { },
  "message": "Operation completed successfully"
}
```

### Error Response
```json
{
  "status": "error",
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": "Additional error details"
  },
  "timestamp": "2025-11-05T10:30:00Z"
}
```

### Common HTTP Status Codes
| Code | Meaning | Description |
|------|---------|-------------|
| 200 | OK | Successful request |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Invalid request parameters |
| 401 | Unauthorized | Authentication required |
| 404 | Not Found | Resource not found |
| 500 | Internal Server Error | Server error |
| 503 | Service Unavailable | Service temporarily unavailable |

---

## ⚠️ Error Handling

### Error Codes

| Error Code | Description | HTTP Status |
|------------|-------------|-------------|
| `USER_NOT_FOUND` | User does not exist | 404 |
| `INVALID_REQUEST` | Invalid request parameters | 400 |
| `CHAT_ERROR` | Error processing chat message | 500 |
| `GITHUB_API_ERROR` | GitHub API connection error | 503 |
| `OLLAMA_ERROR` | AI model service error | 503 |
| `DATABASE_ERROR` | Database operation failed | 500 |
| `RATE_LIMIT_EXCEEDED` | Too many requests | 429 |

### Example Error Response
```json
{
  "timestamp": "2025-11-05T10:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to process chat message",
  "path": "/api/chat/message"
}
```

---

## 🚦 Rate Limiting

**Current Limits** (per user):
- Chat messages: 60 requests/minute
- History operations: 100 requests/minute
- GitHub API: 30 requests/minute

**Rate Limit Headers**:
```http
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1699185600
```

---

## 📡 Endpoints

### User Management

#### Login / Create User
Creates a new user or logs in an existing user.

**Endpoint**: `POST /api/users/login`

**Request Body**:
```json
{
  "username": "shreyas"
}
```

**Response**: `200 OK`
```json
{
  "id": "6544a1b2c3d4e5f6a7b8c9d0",
  "username": "shreyas",
  "createdAt": "2025-11-05T10:30:00Z"
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username": "shreyas"}'
```

---

### Chat Operations

#### Send Chat Message
Sends a message to the AI chatbot and receives a streaming response.

**Endpoint**: `POST /api/chat/message`

**Request Body**:
```json
{
  "username": "shreyas",
  "message": "What is Maximo Application Suite?"
}
```

**Response**: `200 OK` (Streaming)
```
data: Maximo Application Suite is

data: an integrated cloud

data: and on-premises solution

data: for asset management...

data: [DONE]
```

**Response Headers**:
```http
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

**cURL Example**:
```bash
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "username": "shreyas",
    "message": "What is Maximo?"
  }' \
  --no-buffer
```

**JavaScript Example**:
```javascript
const sendMessage = async (username, message) => {
  const response = await fetch('http://localhost:8080/api/chat/message', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ username, message }),
  });

  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    
    const chunk = decoder.decode(value);
    console.log('Received:', chunk);
  }
};
```

---

### History Management

#### Get Chat History
Retrieves all chat sessions for a user.

**Endpoint**: `GET /api/history`

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `username` | string | Yes | Username to fetch history for |
| `limit` | integer | No | Max number of sessions (default: 50) |
| `offset` | integer | No | Pagination offset (default: 0) |

**Request**:
```http
GET /api/history?username=shreyas&limit=10&offset=0
```

**Response**: `200 OK`
```json
[
  {
    "id": "6544a1b2c3d4e5f6a7b8c9d0",
    "username": "shreyas",
    "title": "Discussion about Maximo",
    "messages": [
      {
        "id": "msg1",
        "type": "user",
        "content": "What is Maximo?",
        "timestamp": "2025-11-05T10:30:00Z"
      },
      {
        "id": "msg2",
        "type": "assistant",
        "content": "Maximo is an enterprise asset management system...",
        "timestamp": "2025-11-05T10:30:02Z"
      }
    ],
    "createdAt": "2025-11-05T10:30:00Z",
    "updatedAt": "2025-11-05T10:35:00Z"
  }
]
```

**cURL Example**:
```bash
curl -X GET "http://localhost:8080/api/history?username=shreyas&limit=10" \
  -H "Content-Type: application/json"
```

#### Save Chat History
Saves or updates a chat session.

**Endpoint**: `POST /api/history`

**Request Body**:
```json
{
  "username": "shreyas",
  "title": "Maximo Discussion",
  "messages": [
    {
      "id": "msg1",
      "type": "user",
      "content": "What is Maximo?",
      "timestamp": "2025-11-05T10:30:00Z"
    },
    {
      "id": "msg2",
      "type": "assistant",
      "content": "Maximo is an enterprise asset management system...",
      "timestamp": "2025-11-05T10:30:02Z"
    }
  ]
}
```

**Response**: `201 Created`
```json
{
  "id": "6544a1b2c3d4e5f6a7b8c9d0",
  "username": "shreyas",
  "title": "Maximo Discussion",
  "messages": [...],
  "createdAt": "2025-11-05T10:30:00Z"
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:8080/api/history \
  -H "Content-Type: application/json" \
  -d '{
    "username": "shreyas",
    "title": "Maximo Discussion",
    "messages": [
      {
        "id": "msg1",
        "type": "user",
        "content": "What is Maximo?",
        "timestamp": "2025-11-05T10:30:00Z"
      }
    ]
  }'
```

#### Delete Chat History
Deletes a specific chat session.

**Endpoint**: `DELETE /api/history/{sessionId}`

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `sessionId` | string | Yes | Session ID to delete |

**Request**:
```http
DELETE /api/history/6544a1b2c3d4e5f6a7b8c9d0
```

**Response**: `204 No Content`

**cURL Example**:
```bash
curl -X DELETE http://localhost:8080/api/history/6544a1b2c3d4e5f6a7b8c9d0
```

---

### GitHub Integration

#### List Repository Files
Lists files in a GitHub repository.

**Endpoint**: `GET /api/github/files`

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `owner` | string | No | Repository owner (default: from config) |
| `repo` | string | No | Repository name (default: from config) |
| `path` | string | No | Path within repository (default: root) |

**Request**:
```http
GET /api/github/files?path=docs
```

**Response**: `200 OK`
```json
[
  {
    "name": "README.md",
    "path": "docs/README.md",
    "type": "file",
    "size": 2048,
    "url": "https://api.github.com/repos/owner/repo/contents/docs/README.md"
  },
  {
    "name": "installation",
    "path": "docs/installation",
    "type": "dir",
    "size": 0,
    "url": "https://api.github.com/repos/owner/repo/contents/docs/installation"
  }
]
```

**cURL Example**:
```bash
curl -X GET "http://localhost:8080/api/github/files?path=docs" \
  -H "Content-Type: application/json"
```

#### Get File Content
Retrieves content of a specific file.

**Endpoint**: `GET /api/github/file`

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `path` | string | Yes | File path in repository |

**Request**:
```http
GET /api/github/file?path=docs/README.md
```

**Response**: `200 OK`
```json
{
  "name": "README.md",
  "path": "docs/README.md",
  "content": "# Documentation\n\nThis is the documentation...",
  "encoding": "utf-8",
  "size": 2048
}
```

**cURL Example**:
```bash
curl -X GET "http://localhost:8080/api/github/file?path=docs/README.md" \
  -H "Content-Type: application/json"
```

#### Search Repository
Searches for content within the repository.

**Endpoint**: `GET /api/github/search`

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | string | Yes | Search query |
| `limit` | integer | No | Max results (default: 10) |

**Request**:
```http
GET /api/github/search?query=installation&limit=5
```

**Response**: `200 OK`
```json
{
  "totalCount": 15,
  "items": [
    {
      "path": "docs/installation.md",
      "score": 0.95,
      "excerpt": "...installation guide for the system..."
    }
  ]
}
```

---

### Health & Monitoring

#### Health Check
Checks the health status of all system components.

**Endpoint**: `GET /api/health`

**Response**: `200 OK`
```json
{
  "status": "UP",
  "timestamp": "2025-11-05T10:30:00Z",
  "components": {
    "mongodb": {
      "status": "UP",
      "details": {
        "database": "chatbot",
        "connection": "healthy"
      }
    },
    "ollama": {
      "status": "UP",
      "details": {
        "baseUrl": "http://localhost:11434",
        "model": "granite4:micro-h"
      }
    },
    "github": {
      "status": "UP",
      "details": {
        "apiUrl": "https://api.github.com",
        "rateLimit": {
          "remaining": 4998,
          "limit": 5000
        }
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "free": "50GB",
        "total": "100GB"
      }
    }
  }
}
```

**Service Degraded Response**: `503 Service Unavailable`
```json
{
  "status": "DOWN",
  "timestamp": "2025-11-05T10:30:00Z",
  "components": {
    "mongodb": {
      "status": "DOWN",
      "details": {
        "error": "Connection timeout"
      }
    },
    "ollama": {
      "status": "UP"
    }
  }
}
```

**cURL Example**:
```bash
curl -X GET http://localhost:8080/api/health
```

#### Metrics
Provides application metrics (if enabled).

**Endpoint**: `GET /api/metrics`

**Response**: `200 OK`
```json
{
  "uptime": 3600,
  "requestCount": 1250,
  "averageResponseTime": 180,
  "errorRate": 0.02,
  "activeUsers": 15,
  "activeSessions": 23
}
```

---

### Proxy Service

#### Proxy External API Calls
Forwards requests to external APIs.

**Endpoint**: `POST /api/proxy`

**Request Body**:
```json
{
  "url": "https://api.example.com/data",
  "method": "GET",
  "headers": {
    "Authorization": "Bearer token123"
  },
  "body": {}
}
```

**Response**: `200 OK`
```json
{
  "statusCode": 200,
  "headers": {
    "content-type": "application/json"
  },
  "body": {
    "data": "response from external API"
  }
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:8080/api/proxy \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://api.example.com/data",
    "method": "GET",
    "headers": {},
    "body": {}
  }'
```

---

## 🔌 WebSocket API

### Chat Streaming WebSocket

**Endpoint**: `ws://localhost:8080/api/ws/chat`

**Connection**:
```javascript
const socket = new WebSocket('ws://localhost:8080/api/ws/chat');

socket.onopen = () => {
  console.log('Connected to chat');
  
  // Send message
  socket.send(JSON.stringify({
    type: 'chat',
    username: 'shreyas',
    message: 'What is Maximo?'
  }));
};

socket.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('Received:', data);
};

socket.onerror = (error) => {
  console.error('WebSocket error:', error);
};

socket.onclose = () => {
  console.log('Disconnected from chat');
};
```

**Message Format**:
```json
{
  "type": "chunk",
  "content": "Maximo is",
  "done": false
}
```

**Completion Message**:
```json
{
  "type": "complete",
  "done": true
}
```

---

## 💻 Code Examples

### JavaScript/TypeScript (React)

```javascript
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

// Login
export const login = async (username) => {
  const response = await axios.post(`${API_BASE_URL}/users/login`, {
    username
  });
  return response.data;
};

// Send chat message with streaming
export const sendMessage = async (username, message, onChunk) => {
  const response = await fetch(`${API_BASE_URL}/chat/message`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ username, message }),
  });

  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    
    const chunk = decoder.decode(value);
    onChunk(chunk);
  }
};

// Get history
export const getHistory = async (username, limit = 50) => {
  const response = await axios.get(`${API_BASE_URL}/history`, {
    params: { username, limit }
  });
  return response.data;
};

// Save history
export const saveHistory = async (historyData) => {
  const response = await axios.post(`${API_BASE_URL}/history`, historyData);
  return response.data;
};
```

### Python

```python
import requests
import json

API_BASE_URL = "http://localhost:8080/api"

# Login
def login(username):
    response = requests.post(
        f"{API_BASE_URL}/users/login",
        json={"username": username}
    )
    return response.json()

# Send chat message
def send_message(username, message):
    response = requests.post(
        f"{API_BASE_URL}/chat/message",
        json={"username": username, "message": message},
        stream=True
    )
    
    for line in response.iter_lines():
        if line:
            chunk = line.decode('utf-8')
            if chunk.startswith('data: '):
                content = chunk[6:]  # Remove 'data: ' prefix
                print(content, end='', flush=True)

# Get history
def get_history(username, limit=50):
    response = requests.get(
        f"{API_BASE_URL}/history",
        params={"username": username, "limit": limit}
    )
    return response.json()

# Save history
def save_history(history_data):
    response = requests.post(
        f"{API_BASE_URL}/history",
        json=history_data
    )
    return response.json()

# Example usage
if __name__ == "__main__":
    user = login("shreyas")
    print(f"Logged in as: {user['username']}")
    
    print("\nSending message...")
    send_message("shreyas", "What is Maximo?")
    
    print("\n\nFetching history...")
    history = get_history("shreyas", limit=5)
    print(f"Found {len(history)} sessions")
```

### Java

```java
import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class ChatbotClient {
    private static final String API_BASE_URL = "http://localhost:8080/api";
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    
    // Login
    public static User login(String username) throws IOException {
        String json = mapper.writeValueAsString(
            Map.of("username", username)
        );
        
        RequestBody body = RequestBody.create(
            json, MediaType.get("application/json")
        );
        
        Request request = new Request.Builder()
            .url(API_BASE_URL + "/users/login")
            .post(body)
            .build();
            
        try (Response response = client.newCall(request).execute()) {
            return mapper.readValue(response.body().string(), User.class);
        }
    }
    
    // Send chat message
    public static void sendMessage(String username, String message) 
            throws IOException {
        String json = mapper.writeValueAsString(
            Map.of("username", username, "message", message)
        );
        
        RequestBody body = RequestBody.create(
            json, MediaType.get("application/json")
        );
        
        Request request = new Request.Builder()
            .url(API_BASE_URL + "/chat/message")
            .post(body)
            .build();
            
        try (Response response = client.newCall(request).execute()) {
            BufferedSource source = response.body().source();
            while (!source.exhausted()) {
                String line = source.readUtf8Line();
                if (line != null && line.startsWith("data: ")) {
                    System.out.print(line.substring(6));
                }
            }
        }
    }
    
    // Get history
    public static List<ChatHistory> getHistory(String username, int limit) 
            throws IOException {
        HttpUrl url = HttpUrl.parse(API_BASE_URL + "/history")
            .newBuilder()
            .addQueryParameter("username", username)
            .addQueryParameter("limit", String.valueOf(limit))
            .build();
            
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
            
        try (Response response = client.newCall(request).execute()) {
            return mapper.readValue(
                response.body().string(), 
                new TypeReference<List<ChatHistory>>() {}
            );
        }
    }
}
```

---

## 📮 Postman Collection

### Import Collection

```json
{
  "info": {
    "name": "AI Chatbot API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "User Login",
      "request": {
        "method": "POST",
        "header": [],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"username\": \"shreyas\"\n}",
          "options": {
            "raw": {
              "language": "json"
            }
          }
        },
        "url": {
          "raw": "{{baseUrl}}/users/login",
          "host": ["{{baseUrl}}"],
          "path": ["users", "login"]
        }
      }
    },
    {
      "name": "Send Chat Message",
      "request": {
        "method": "POST",
        "header": [],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"username\": \"shreyas\",\n  \"message\": \"What is Maximo?\"\n}",
          "options": {
            "raw": {
              "language": "json"
            }
          }
        },
        "url": {
          "raw": "{{baseUrl}}/chat/message",
          "host": ["{{baseUrl}}"],
          "path": ["chat", "message"]
        }
      }
    },
    {
      "name": "Get Chat History",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "{{baseUrl}}/history?username=shreyas&limit=10",
          "host": ["{{baseUrl}}"],
          "path": ["history"],
          "query": [
            {
              "key": "username",
              "value": "shreyas"
            },
            {
              "key": "limit",
              "value": "10"
            }
          ]
        }
      }
    },
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "{{baseUrl}}/health",
          "host": ["{{baseUrl}}"],
          "path": ["health"]
        }
      }
    }
  ],
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080/api",
      "type": "string"
    }
  ]
}
```

### Environment Variables

```json
{
  "name": "Local Development",
  "values": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080/api",
      "enabled": true
    },
    {
      "key": "username",
      "value": "shreyas",
      "enabled": true
    }
  ]
}
```

---

## 📞 Support

For API support:
- **Documentation**: See PROJECT_DOCUMENTATION.md
- **Issues**: https://github.com/ShreyasGowda2004/AiBot/issues
- **Email**: api-support@aichatbot.com

---

**Last Updated**: November 5, 2025  
**API Version**: 1.0.0
