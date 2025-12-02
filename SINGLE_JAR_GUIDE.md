# Single JAR Deployment Guide

This project has been configured to build a single executable JAR file that contains both the Spring Boot backend and React frontend.

## Overview

The application uses:
- **Backend**: Spring Boot 3.2.0 with Java 17
- **Frontend**: React with Vite
- **Build Tool**: Maven with frontend-maven-plugin

## Architecture

The single JAR deployment works as follows:

1. **Frontend Build**: React app is built using Vite and output to `backend/src/main/resources/static/`
2. **Backend Packaging**: Spring Boot packages the JAR with all static resources included
3. **Runtime Routing**:
   - API requests (`/api/*`) → Spring Boot REST Controllers
   - Static resources (JS, CSS, images) → Served from classpath
   - All other routes → Forwarded to `index.html` for React Router

## Building the Single JAR

### Quick Build

Run the build script:

```bash
./build-single-jar.sh
```

This script will:
- Clean previous builds
- Install Node.js and npm (if needed)
- Install frontend dependencies
- Build the React frontend
- Compile the Spring Boot backend
- Package everything into a single JAR

### Manual Build

If you prefer to build manually:

```bash
cd backend
./mvnw clean package -DskipTests
```

### Build Output

The JAR file will be created at:
```
backend/target/ai-chatbot-backend-1.0.0.jar
```

## Running the Application

### Start the Application

```bash
java -jar backend/target/ai-chatbot-backend-1.0.0.jar
```

### Access the Application

Once running, access the application at:
- **Web UI**: http://localhost:8080
- **API**: http://localhost:8080/api/*

### Environment Variables

You can override configuration using environment variables:

```bash
# MongoDB connection
export SPRING_DATA_MONGODB_URI="mongodb://localhost:27017/chatbot"

# Server port
export SERVER_PORT=9090

# Ollama configuration
export SPRING_AI_OLLAMA_BASE_URL="http://localhost:11434"

# Run the application
java -jar backend/target/ai-chatbot-backend-1.0.0.jar
```

## Configuration Changes

The following changes were made to support single JAR deployment:

### 1. Frontend Configuration (`frontend/vite.config.js`)

```javascript
build: {
  outDir: '../backend/src/main/resources/static',
  emptyOutDir: true,
  sourcemap: false
}
```

### 2. Backend POM (`backend/pom.xml`)

Added `frontend-maven-plugin` to automatically build the frontend during Maven build:

```xml
<plugin>
  <groupId>com.github.eirslett</groupId>
  <artifactId>frontend-maven-plugin</artifactId>
  <version>1.15.0</version>
  <!-- ... configuration ... -->
</plugin>
```

### 3. SPA Routing (`SpaForwardingController.java`)

Added controller to forward all non-API routes to `index.html`:

```java
@RequestMapping(value = {
    "/",
    "/{path:[^\\.]*}",
    "/{path:^(?!api|static|actuator).*}/**"
})
public String forward() {
    return "forward:/index.html";
}
```

### 4. API Prefix

All REST controllers now use `/api` prefix:
- `/api/chat` - Chat operations
- `/api/users` - User management
- `/api/history` - Chat history
- `/api/health` - Health checks

### 5. Application Properties

Removed `server.servlet.context-path=/api` since API prefix is now handled by controllers.

## Development Mode

For development, you can still run frontend and backend separately:

### Backend
```bash
cd backend
./mvnw spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

The frontend dev server will proxy API requests to `http://localhost:8080/api`.

## Production Deployment

### Prerequisites

- Java 17 or higher
- MongoDB instance (local or remote)
- Ollama instance for AI models

### Deployment Steps

1. Build the JAR:
   ```bash
   ./build-single-jar.sh
   ```

2. Copy the JAR to your server:
   ```bash
   scp backend/target/ai-chatbot-backend-1.0.0.jar user@server:/opt/app/
   ```

3. Create a systemd service (Linux):
   ```ini
   [Unit]
   Description=AI Chatbot Application
   After=network.target

   [Service]
   Type=simple
   User=appuser
   WorkingDirectory=/opt/app
   ExecStart=/usr/bin/java -jar /opt/app/ai-chatbot-backend-1.0.0.jar
   Restart=on-failure

   [Install]
   WantedBy=multi-user.target
   ```

4. Start the service:
   ```bash
   sudo systemctl enable ai-chatbot
   sudo systemctl start ai-chatbot
   ```

## Troubleshooting

### Frontend Not Loading

If you see a blank page:
1. Check browser console for errors
2. Verify static resources exist in the JAR: `jar tf backend/target/ai-chatbot-backend-1.0.0.jar | grep static`
3. Check Spring Boot logs for static resource serving errors

### API Requests Failing

If API requests fail:
1. Verify endpoints use `/api` prefix
2. Check network tab in browser dev tools
3. Review Spring Boot logs for controller mapping

### 404 on React Routes

If you get 404 errors on React Router routes:
1. Verify `SpaForwardingController` is included in the build
2. Check that routes don't conflict with API paths
3. Ensure `forward:/index.html` mapping is working

## File Structure

```
.
├── build-single-jar.sh          # Build script
├── SINGLE_JAR_GUIDE.md          # This file
├── backend/
│   ├── pom.xml                  # Maven config with frontend-maven-plugin
│   ├── src/main/
│   │   ├── java/
│   │   │   └── com/aichatbot/
│   │   │       ├── config/
│   │   │       │   └── SpaForwardingController.java
│   │   │       └── controller/  # All use /api prefix
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/          # Frontend build output (generated)
│   └── target/
│       └── ai-chatbot-backend-1.0.0.jar  # Single JAR output
└── frontend/
    ├── vite.config.js           # Configured to build to backend/static
    └── src/
        └── utils/
            └── api.js           # Uses /api prefix
```

## Benefits of Single JAR Deployment

1. **Simplified Deployment**: Only one file to deploy
2. **Version Consistency**: Frontend and backend versions are always in sync
3. **No CORS Issues**: Frontend and backend run on the same origin
4. **Easy Scaling**: Deploy multiple instances easily
5. **Reduced Complexity**: No need for separate web server or reverse proxy

## Notes

- The single JAR includes Node.js and npm for building, but they're not included in the final JAR
- The JAR size will be larger than backend-only (includes React build artifacts)
- CORS configuration is simplified since everything runs on the same origin
- React Router works seamlessly with Spring Boot's forwarding controller
