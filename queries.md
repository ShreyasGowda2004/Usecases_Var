# Architecture Explanation - Frontend Build & Static Resources

Understanding why and how the React frontend is served from Spring Boot's `static` folder.

---

## 📚 Table of Contents
- [Why `resources/static` Instead of `webapp`?](#why-resourcesstatic-instead-of-webapp)
- [Traditional vs Modern Approach](#traditional-vs-modern-approach)
- [How Your Setup Works](#how-your-setup-works)
- [Complete Build Flow](#complete-build-flow)
- [Request Routing](#request-routing)
- [Key Advantages](#key-advantages)
- [When to Use `webapp`](#when-to-use-webapp)
- [Architecture Diagram](#architecture-diagram)

---

## Why `resources/static` Instead of `webapp`?

### Spring Boot's Static Content Serving

Spring Boot has **built-in static resource handling** that automatically serves files from these locations:
- `/META-INF/resources/`
- `/resources/`
- **`/static/`** ← Your project uses this
- `/public/`

When you place files in `src/main/resources/static/`, Spring Boot:
- ✅ Automatically serves them at the root path (`/`)
- ✅ Includes them in the JAR file during packaging
- ✅ No additional configuration needed
- ✅ Works perfectly for Single Page Applications (SPA)
- ✅ Embedded in the executable JAR

### Why This Location?

1. **JAR Packaging**: Resources in `src/main/resources/` are included in the JAR's classpath
2. **Auto-Detection**: Spring Boot automatically detects and serves from `static/` folder
3. **No Configuration**: Zero XML or Java config needed for basic serving
4. **Modern Pattern**: Follows microservices and cloud-native principles
5. **Single Artifact**: Everything in one deployable file

---

## Traditional vs Modern Approach

### Comparison Table

| Aspect | `src/main/webapp` (Traditional) | `src/main/resources/static` (Modern) |
|--------|-------------------------------|-------------------------------------|
| **Used For** | Traditional WAR deployments | Spring Boot JAR deployments |
| **Packaging Type** | WAR (Web Archive) | JAR (Java Archive) |
| **Server Type** | External (Tomcat, JBoss, WebLogic) | Embedded Tomcat/Jetty |
| **Configuration** | Requires web.xml, context.xml | Auto-configured by Spring Boot |
| **Build Output** | Separate webapp directory | Inside JAR resources |
| **Deployment** | Deploy to application server | Run with `java -jar` |
| **Distribution** | WAR file + server | Single JAR file |
| **Best For** | Legacy enterprise apps | Microservices, Cloud, Docker |
| **Startup Time** | Slower (external container) | Faster (optimized embedded) |
| **DevOps** | Complex (multiple components) | Simple (single artifact) |
| **Cloud Native** | Requires adaptation | Ready out of the box |
| **Container Support** | Additional layers needed | Direct container support |

### Why webapp is Legacy

The `src/main/webapp` approach comes from the **Java EE era** (pre-2013):
- Applications were packaged as WAR files
- Deployed to external application servers (Tomcat, WebSphere, etc.)
- Required separate web.xml configuration
- Frontend and backend were tightly coupled
- JSP/JSF were common for UI

### Why resources/static is Modern

The `src/main/resources/static` approach follows **Spring Boot principles** (2013+):
- Applications are packaged as executable JAR files
- Contains embedded web server (Tomcat/Jetty)
- Configuration through application.properties
- Frontend and backend can be separate (REST API)
- Modern frameworks (React, Vue, Angular) are common

---

## How Your Setup Works

### 1. Vite Configuration

**File**: `frontend/vite.config.js`

```javascript
export default defineConfig({
  plugins: [react()],
  base: '/',
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    outDir: '../backend/src/main/resources/static',  // ← Key: Builds directly to Spring Boot static folder
    emptyOutDir: true,                               // Clears folder before build
    sourcemap: false                                 // No source maps in production
  }
})
```

**What happens:**
- During development (`npm run dev`): Vite dev server runs on port 3000, proxies API calls to port 8080
- During build (`npm run build`): Vite compiles React app and outputs directly to backend's static folder
- Result: Production files are immediately in the correct location for Spring Boot

### 2. Maven Frontend Plugin

**File**: `backend/pom.xml`

```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>1.15.0</version>
    <configuration>
        <workingDirectory>../frontend</workingDirectory>
        <installDirectory>../frontend</installDirectory>
    </configuration>
    <executions>
        <!-- Install Node.js and npm -->
        <execution>
            <id>install node and npm</id>
            <goals>
                <goal>install-node-and-npm</goal>
            </goals>
            <configuration>
                <nodeVersion>v18.18.0</nodeVersion>
                <npmVersion>9.8.1</npmVersion>
            </configuration>
        </execution>
        
        <!-- Install dependencies -->
        <execution>
            <id>npm install</id>
            <goals>
                <goal>npm</goal>
            </goals>
            <configuration>
                <arguments>install</arguments>
            </configuration>
        </execution>
        
        <!-- Build frontend -->
        <execution>
            <id>npm run build</id>
            <goals>
                <goal>npm</goal>
            </goals>
            <configuration>
                <arguments>run build</arguments>  <!-- Triggers Vite build -->
            </configuration>
        </execution>
    </executions>
</plugin>
```

**What happens:**
- `mvn package` automatically:
  1. Installs Node.js and npm (if not present)
  2. Runs `npm install` (installs React dependencies)
  3. Runs `npm run build` (builds frontend via Vite)
  4. Packages everything into a JAR

### 3. Spring Boot Auto-Configuration

**File**: `backend/src/main/java/com/aichatbot/config/WebConfig.java`

```java
@Configuration
public class WebConfig {
    // CORS configuration for API access
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Allows frontend (during dev) to access backend API
    }
}
```

**What Spring Boot does automatically:**
- Detects `static/` folder in classpath resources
- Serves `index.html` at root URL (`http://localhost:8080/`)
- Serves assets from `/assets/*` path
- Routes API requests to controllers (e.g., `/api/chat`)
- Handles SPA routing (all unmatched routes → index.html)

### 4. Directory Structure After Build

```
backend/src/main/resources/
├── application.properties          ← Backend configuration
└── static/                         ← Frontend files (React app)
    ├── index.html                  ← Entry point
    └── assets/
        ├── index-BwG0W1mR.js      ← Bundled JavaScript
        ├── index-BwG0W1mR.css     ← Bundled CSS
        └── [images, fonts, etc.]
```

---

## Complete Build Flow

### Visual Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    DEVELOPMENT PHASE                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  npm run dev    │  ← React dev server (port 3000)
                    │  (Vite)         │
                    └────────┬────────┘
                             │
                             │ Proxies API calls to
                             ▼
                    ┌─────────────────┐
                    │  Backend Java   │  ← Spring Boot (port 8080)
                    │  API Server     │
                    └─────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    BUILD PHASE                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
            ┌──────────────────────────────────┐
            │  ./mvnw package                  │
            │  (Maven build command)           │
            └────────────┬─────────────────────┘
                         │
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
┌─────────────────┐           ┌─────────────────┐
│ Maven triggers  │           │  Maven compiles │
│ frontend plugin │           │  Java code      │
└────────┬────────┘           └────────┬────────┘
         │                              │
         ▼                              │
┌─────────────────┐                    │
│ npm install     │                    │
│ (Dependencies)  │                    │
└────────┬────────┘                    │
         │                              │
         ▼                              │
┌─────────────────┐                    │
│ npm run build   │                    │
│ (Vite build)    │                    │
└────────┬────────┘                    │
         │                              │
         ▼                              │
┌─────────────────────────────┐        │
│  frontend/dist/ created     │        │
│  ├── index.html             │        │
│  └── assets/                │        │
│      ├── index-xxx.js       │        │
│      └── index-xxx.css      │        │
└────────┬────────────────────┘        │
         │                              │
         │ (Vite config copies to)     │
         ▼                              │
┌─────────────────────────────┐        │
│  backend/src/main/resources/│        │
│  static/                    │        │
│    ├── index.html           │        │
│    └── assets/              │        │
└────────┬────────────────────┘        │
         │                              │
         └──────────┬───────────────────┘
                    │
                    ▼
         ┌──────────────────────┐
         │  Maven packages      │
         │  everything into JAR │
         └──────────┬───────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────┐
│  backend/target/                                │
│  ai-chatbot-backend-1.0.0.jar                   │
│    ├── BOOT-INF/                                │
│    │   ├── classes/                             │
│    │   │   ├── com/aichatbot/...               │ ← Java classes
│    │   │   ├── application.properties           │ ← Config
│    │   │   └── static/                          │ ← Frontend here!
│    │   │       ├── index.html                   │
│    │   │       └── assets/                      │
│    │   └── lib/                                 │ ← Dependencies
│    └── META-INF/                                │
└─────────────────────────────────────────────────┘
                    │
                    ▼
         ┌──────────────────────┐
         │  java -jar app.jar   │
         │  (Run application)   │
         └──────────┬───────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────┐
│  http://localhost:8080/                         │
│    ├── /              → index.html (React)      │
│    ├── /assets/*      → JS/CSS/images           │
│    ├── /api/chat      → ChatController          │
│    ├── /api/github    → GitHubController        │
│    └── [Any route]    → index.html (SPA)        │
└─────────────────────────────────────────────────┘
```

### Step-by-Step Breakdown

#### Development Mode
```bash
# Terminal 1: Start backend
cd backend
./mvnw spring-boot:run
# Runs at http://localhost:8080

# Terminal 2: Start frontend
cd frontend
npm run dev
# Runs at http://localhost:3000
# Proxies /api/* requests to :8080
```

#### Production Build
```bash
# Single command builds everything
./build-single-jar.sh

# Or manually:
cd backend
./mvnw clean package

# This triggers:
# 1. Clean previous builds
# 2. Install Node.js/npm (if needed)
# 3. npm install (frontend dependencies)
# 4. npm run build (Vite → static/)
# 5. Compile Java code
# 6. Package everything into JAR
```

#### Deployment
```bash
# Run the JAR
java -jar backend/target/ai-chatbot-backend-1.0.0.jar

# Access at http://localhost:8080
# - Frontend served at root (/)
# - API available at /api/*
```

---

## Request Routing

### How Spring Boot Routes Requests

```
┌─────────────────────────────────────────┐
│  User Request                           │
│  http://localhost:8080/some-path        │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│  Spring Boot DispatcherServlet          │
│  (Analyzes request path)                │
└────────────────┬────────────────────────┘
                 │
    ┌────────────┴────────────┐
    │                         │
    ▼                         ▼
┌─────────────────┐   ┌──────────────────┐
│ Path: /api/*    │   │ Any other path   │
│                 │   │                  │
│ ✓ /api/chat     │   │ ✓ /              │
│ ✓ /api/github   │   │ ✓ /dashboard     │
│ ✓ /api/health   │   │ ✓ /login         │
└────────┬────────┘   └────────┬─────────┘
         │                     │
         ▼                     ▼
┌─────────────────┐   ┌──────────────────┐
│ REST Controller │   │ Static Resource  │
│ @RestController │   │ Handler          │
│                 │   │                  │
│ - ChatCtrl      │   │ Checks:          │
│ - GitHubCtrl    │   │ static/          │
│ - UserCtrl      │   │ └── matches path?│
│                 │   │     ├─ YES →     │
│ Returns JSON    │   │     │   serve it │
│ response        │   │     └─ NO →      │
└─────────────────┘   │       serve      │
                      │       index.html │
                      │       (SPA)      │
                      └──────────────────┘
                               │
                               ▼
                      ┌──────────────────┐
                      │ React Router     │
                      │ handles client-  │
                      │ side routing     │
                      └──────────────────┘
```

### Example Request Flows

#### API Request (Backend Processing)
```
GET http://localhost:8080/api/chat
    ↓
Spring Boot sees "/api/*"
    ↓
Routes to ChatController
    ↓
Processes request
    ↓
Returns JSON: {"response": "Hello!", ...}
```

#### Static Asset Request (File Serving)
```
GET http://localhost:8080/assets/index-BwG0W1mR.js
    ↓
Spring Boot sees "/assets/*"
    ↓
Checks static/ folder
    ↓
Finds assets/index-BwG0W1mR.js
    ↓
Serves JavaScript file
```

#### SPA Route Request (React Router)
```
GET http://localhost:8080/dashboard
    ↓
Spring Boot: Not /api/*, not in static/
    ↓
Serves static/index.html
    ↓
Browser loads React app
    ↓
React Router sees "/dashboard"
    ↓
Renders Dashboard component
```

---

## Key Advantages

### 1. Single JAR Deployment
```bash
# One file contains everything
java -jar ai-chatbot-backend-1.0.0.jar

# No need for:
# - Separate frontend server (nginx, Apache)
# - Application server installation
# - Complex deployment scripts
# - Multiple deployment artifacts
```

### 2. No CORS Issues in Production
```
Same Origin:
http://localhost:8080/           ← Frontend
http://localhost:8080/api/chat   ← Backend API

✓ Same protocol (http)
✓ Same domain (localhost)
✓ Same port (8080)
= No CORS configuration needed!
```

### 3. Simplified DevOps

**Traditional Setup (Multiple Services):**
```
┌──────────┐    ┌──────────┐    ┌──────────┐
│  Nginx   │────│  Tomcat  │────│ Database │
│  :80     │    │  :8080   │    │  :27017  │
└──────────┘    └──────────┘    └──────────┘
Frontend WAR    Backend WAR      Separate

Deploy 3 components, configure 2 proxies
```

**Your Setup (Single Service):**
```
┌─────────────────┐    ┌──────────┐
│  Spring Boot    │────│ Database │
│  :8080          │    │  :27017  │
│  (with React)   │    └──────────┘
└─────────────────┘
Single JAR          Separate

Deploy 1 JAR, configure 0 proxies
```

### 4. Cloud-Ready & Container-Friendly

**Dockerfile Example:**
```dockerfile
FROM openjdk:17-slim
COPY backend/target/ai-chatbot-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]

# That's it! No multi-stage builds needed
```

**Kubernetes Deployment:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-chatbot
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: app
        image: ai-chatbot:1.0.0
        ports:
        - containerPort: 8080
        
# Single container, simple scaling
```

### 5. Faster Startup & Lower Memory

| Metric | Traditional (WAR + External Tomcat) | Modern (Embedded JAR) |
|--------|-------------------------------------|----------------------|
| Startup Time | 30-60 seconds | 10-20 seconds |
| Memory Usage | 512MB - 1GB | 256MB - 512MB |
| Deployment Size | WAR (100MB) + Server (50MB) | JAR (75MB) |
| Cold Start | Slow (container warmup) | Fast (optimized) |

### 6. Development Benefits

**Advantages:**
- ✅ Single build command (`./mvnw package`)
- ✅ Consistent dev/prod experience
- ✅ Easy to test production builds locally
- ✅ No build artifact management complexity
- ✅ Frontend/backend version always in sync
- ✅ Simpler CI/CD pipelines

---

## When to Use `webapp`

You should ONLY use `src/main/webapp` if you have these specific requirements:

### 1. Legacy Requirements
- Maintaining existing WAR-based application
- Corporate mandate for WAR deployment
- Existing infrastructure expects WAR files
- Integration with legacy Java EE systems

### 2. JSP/JSF Applications
```java
// If you're using JSP pages
src/main/webapp/
├── WEB-INF/
│   ├── web.xml
│   └── views/
│       └── home.jsp    ← JSP files must be in webapp
└── index.jsp
```

### 3. External Application Servers
- Deploying to shared WebSphere/WebLogic
- Organization-wide Tomcat cluster
- Server-level security/monitoring requirements
- Multi-application server environment

### 4. Specific Java EE Features
- Servlet 3.0 annotations (some scenarios)
- Web fragments
- Container-managed security realms
- Shared libraries across applications

### 5. Compliance/Governance
- Regulatory requirements for WAR packaging
- Audit requirements for separate static assets
- Security policies requiring external server
- Organizational standards mandating WAR

### Example: When webapp Makes Sense
```
Enterprise Scenario:
├── Multiple teams share one Tomcat cluster
├── Security team controls server configuration
├── Operations deploys multiple WARs
├── JSP-based admin interface required
└── Compliance requires separation of concerns

→ Use webapp + WAR packaging
```

### Modern Alternative
```
Modern Equivalent:
├── Each service is a JAR (microservice)
├── Container orchestration (Kubernetes)
├── API Gateway for security
├── React/Vue/Angular for UI
└── Observability with APM tools

→ Use resources/static + JAR packaging
```

---

## Architecture Diagram

### Your Project Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                    AI CHATBOT APPLICATION                      │
│                  (Single JAR Deployment)                       │
└────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┴─────────────────────┐
        │                                           │
        ▼                                           ▼
┌──────────────────────┐                ┌──────────────────────┐
│   FRONTEND (React)   │                │  BACKEND (Spring)    │
│                      │                │                      │
│  Development:        │                │  Development:        │
│  - Vite dev server   │                │  - mvnw spring-boot  │
│  - Port 3000         │                │  - Port 8080         │
│  - Hot reload        │                │  - Auto reload       │
│                      │                │                      │
│  Production:         │                │  Production:         │
│  - Built to static/  │◄───────────────│  - Serves static/    │
│  - Served by Spring  │                │  - Handles API       │
└──────────────────────┘                └──────────┬───────────┘
                                                   │
                                                   ▼
                                        ┌──────────────────────┐
                                        │  EMBEDDED TOMCAT     │
                                        │  (Port 8080)         │
                                        └──────────┬───────────┘
                                                   │
                    ┌──────────────────────────────┴───────┐
                    │                                      │
                    ▼                                      ▼
         ┌─────────────────────┐            ┌─────────────────────┐
         │  Static Resources   │            │   REST APIs         │
         │                     │            │                     │
         │  /                  │            │  /api/chat          │
         │  /assets/*          │            │  /api/github        │
         │  /dashboard         │            │  /api/history       │
         │  (React Router)     │            │  /api/health        │
         └─────────────────────┘            └──────────┬──────────┘
                                                       │
                                                       ▼
                                            ┌──────────────────────┐
                                            │  Services            │
                                            │  - ChatService       │
                                            │  - RAGService        │
                                            │  - OllamaService     │
                                            │  - GitHubService     │
                                            └──────────┬───────────┘
                                                       │
                    ┌──────────────────────────────────┴───────┐
                    │                                          │
                    ▼                                          ▼
         ┌─────────────────────┐                  ┌─────────────────────┐
         │  MongoDB            │                  │  Ollama AI          │
         │  (Chat History)     │                  │  (LLM Service)      │
         │  Port: 27017        │                  │  Port: 11434        │
         └─────────────────────┘                  └─────────────────────┘
```

### Package Structure Inside JAR

```
ai-chatbot-backend-1.0.0.jar
│
├── META-INF/
│   ├── MANIFEST.MF                      ← JAR metadata
│   └── maven/...                        ← Maven info
│
├── BOOT-INF/
│   │
│   ├── classes/                         ← Compiled application
│   │   │
│   │   ├── com/
│   │   │   └── aichatbot/
│   │   │       ├── AiChatbotApplication.class
│   │   │       ├── config/
│   │   │       │   ├── WebConfig.class
│   │   │       │   ├── AIConfig.class
│   │   │       │   └── ...
│   │   │       ├── controller/
│   │   │       │   ├── ChatController.class
│   │   │       │   ├── GitHubController.class
│   │   │       │   └── ...
│   │   │       ├── service/
│   │   │       └── model/
│   │   │
│   │   ├── application.properties       ← Configuration
│   │   │
│   │   └── static/                      ← 🎯 FRONTEND LIVES HERE
│   │       ├── index.html               ← React entry point
│   │       └── assets/
│   │           ├── index-BwG0W1mR.js    ← Bundled React app
│   │           ├── index-BwG0W1mR.css   ← Bundled styles
│   │           └── [other assets]
│   │
│   └── lib/                             ← All dependencies
│       ├── spring-boot-*.jar
│       ├── spring-web-*.jar
│       ├── jackson-*.jar
│       └── [all other JARs]
│
└── org/
    └── springframework/
        └── boot/
            └── loader/                   ← Spring Boot loader classes
```

---

## Build Command Comparison

### Traditional WAR Build
```bash
# Old way - separate builds
cd frontend
npm run build    # → Builds to frontend/dist/
cp -r dist/* ../backend/src/main/webapp/

cd ../backend
mvn clean package    # → Creates WAR file
mv target/app.war /path/to/tomcat/webapps/

# Start external Tomcat
/path/to/tomcat/bin/startup.sh

# Access at http://localhost:8080/app/
```

### Modern JAR Build (Your Project)
```bash
# Modern way - single build
./build-single-jar.sh
# OR
cd backend && ./mvnw clean package

# Maven automatically:
# 1. Installs Node.js/npm
# 2. Runs npm install
# 3. Runs npm run build → static/
# 4. Compiles Java code
# 5. Packages everything into JAR

# Run it
java -jar backend/target/ai-chatbot-backend-1.0.0.jar

# Access at http://localhost:8080/
```

---

## Configuration Files

### 1. Vite Config - Frontend Build

**File**: `frontend/vite.config.js`
```javascript
export default defineConfig({
  build: {
    outDir: '../backend/src/main/resources/static',  // Output location
    emptyOutDir: true,                               // Clean before build
    assetsDir: 'assets',                             // Asset folder name
    sourcemap: false                                 // No source maps
  }
})
```

### 2. Maven POM - Backend Build

**File**: `backend/pom.xml`
```xml
<build>
    <plugins>
        <!-- Spring Boot Plugin -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
        
        <!-- Frontend Plugin -->
        <plugin>
            <groupId>com.github.eirslett</groupId>
            <artifactId>frontend-maven-plugin</artifactId>
            <executions>
                <execution>
                    <id>npm run build</id>
                    <goals>
                        <goal>npm</goal>
                    </goals>
                    <configuration>
                        <arguments>run build</arguments>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 3. Application Properties

**File**: `backend/src/main/resources/application.properties`
```properties
# Server configuration
server.port=8080

# Static resource handling (auto-configured by Spring Boot)
# spring.web.resources.static-locations=classpath:/static/
# spring.mvc.static-path-pattern=/**

# These are defaults and don't need to be specified!
# Spring Boot automatically serves from static/ folder
```

---

## Summary

### ✅ Your Setup (Modern & Correct)

```
Single JAR Approach
├── Spring Boot JAR packaging
├── Frontend in src/main/resources/static/
├── Embedded Tomcat serves everything
├── Single file deployment
└── Perfect for: Microservices, Cloud, Docker, Kubernetes
```

**Benefits:**
- 🚀 Fast deployment
- 📦 Single artifact
- ☁️ Cloud-native
- 🐳 Container-friendly
- 🔧 Easy maintenance
- ⚡ Quick startup

### ❌ Old Way (Legacy)

```
WAR Approach
├── Java EE WAR packaging
├── Frontend in src/main/webapp/
├── External application server required
├── Multiple deployment artifacts
└── Used for: Legacy systems, JSP apps, enterprise mandates
```

**Drawbacks:**
- 🐌 Slower deployment
- 📦 Multiple artifacts
- 🏢 Server dependency
- 🐘 Heavy footprint
- 🔧 Complex maintenance
- ⏰ Slow startup

---

## Quick Reference

### Development Commands

```bash
# Frontend (Development)
cd frontend
npm install
npm run dev          # http://localhost:3000

# Backend (Development)
cd backend
./mvnw spring-boot:run    # http://localhost:8080
```

### Build Commands

```bash
# Build everything
./build-single-jar.sh

# OR manually
cd backend
./mvnw clean package
```

### Run Production Build

```bash
# Run the JAR
java -jar backend/target/ai-chatbot-backend-1.0.0.jar

# With custom port
java -jar backend/target/ai-chatbot-backend-1.0.0.jar --server.port=9090

# With profile
java -jar backend/target/ai-chatbot-backend-1.0.0.jar --spring.profiles.active=production
```

### Verify Build

```bash
# Check JAR contents
jar -tf backend/target/ai-chatbot-backend-1.0.0.jar | grep static/

# Should show:
# BOOT-INF/classes/static/index.html
# BOOT-INF/classes/static/assets/index-xxx.js
# BOOT-INF/classes/static/assets/index-xxx.css
```

---

**Project**: AI Chatbot - GitHub Knowledge Assistant  
**Repository**: https://github.com/ShreyasGowda2004/Usecases_Var  
**Last Updated**: December 3, 2025
