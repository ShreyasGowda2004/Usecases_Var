# AI Chatbot - Command Reference Guide

Complete reference of all important commands for the AI Chatbot project.

---

## 📚 Table of Contents
- [Setup & Installation](#setup--installation)
- [Start/Stop Application](#startstop-application)
- [Backend Commands](#backend-commands)
- [Frontend Commands](#frontend-commands)
- [Ollama Commands](#ollama-commands)
- [Git Commands](#git-commands)
- [Testing & Debugging](#testing--debugging)
- [Database & Data](#database--data)
- [Configuration](#configuration)
- [Monitoring](#monitoring)
- [Quick Access URLs](#quick-access-urls)
- [Advanced Commands](#advanced-commands)
- [Troubleshooting](#troubleshooting)

---

## Setup & Installation

### Initial Setup
```bash
# Auto-installs all dependencies (Java, Node.js, Maven, Ollama, etc.)
./setup.sh
```

### Build Standalone JAR
```bash
# Creates a single executable JAR file
./build-single-jar.sh
```

---

## Start/Stop Application

### Basic Operations
```bash
# Start both backend and frontend
./start.sh

# Stop all services
./stop.sh

# Restart all services (clean stop + start)
./restart.sh
```

### Advanced Start Options
```bash
# Start with custom GitHub token
./start-with-token.sh

# Start with embeddings reset
RESET_EMBEDDINGS=true ./restart.sh

# Start in background
nohup ./restart.sh &
```

---

## Backend Commands

### Project Directory
```bash
cd backend
```

### Build & Run
```bash
# Clean and build project
./mvnw clean install

# Run backend server
./mvnw spring-boot:run

# Build without running tests
./mvnw clean install -DskipTests

# Package as JAR
./mvnw package

# Clean build artifacts
./mvnw clean
```

### Testing
```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=ChatServiceTest

# Run with coverage
./mvnw clean test jacoco:report
```

### Maven Wrapper
```bash
# Update Maven wrapper
./mvnw -N io.takari:maven:wrapper

# Download dependencies
./mvnw dependency:resolve
```

---

## Frontend Commands

### Project Directory
```bash
cd frontend
```

### Development
```bash
# Install dependencies
npm install

# Start development server (http://localhost:3000)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Package Management
```bash
# Update dependencies
npm update

# Audit for vulnerabilities
npm audit

# Fix vulnerabilities
npm audit fix

# Clean install
rm -rf node_modules package-lock.json
npm install
```

---

## Ollama Commands

### Service Management
```bash
# Start Ollama service
ollama serve

# Start Ollama in background
nohup ollama serve &
```

### Model Management
```bash
# List installed models
ollama list

# Pull Granite model
ollama pull granite4:micro-h

# Remove a model
ollama rm granite4:micro-h

# Show model info
ollama show granite4:micro-h
```

### Testing
```bash
# Test Ollama API
curl http://localhost:11434/api/tags

# Test model generation
curl http://localhost:11434/api/generate -d '{
  "model": "granite4:micro-h",
  "prompt": "Hello, how are you?"
}'
```

---

## Git Commands

### Basic Operations
```bash
# Check status
git status

# View changes
git diff

# Add all changes
git add .

# Commit changes
git commit -m "Your commit message"

# Push to remote
git push origin main

# Pull latest changes
git pull origin main
```

### Branch Management
```bash
# List branches
git branch -a

# Create new branch
git checkout -b feature-name

# Switch branch
git checkout main

# Merge branch
git merge feature-name
```

### History & Logs
```bash
# View commit history
git log --oneline

# View detailed log
git log --graph --all --decorate

# Show specific commit
git show <commit-hash>
```

---

## Testing & Debugging

### Connection Testing
```bash
# Test GitHub connection
./test-github-connection.sh

# Test backend health
curl http://localhost:8080/api/health

# Test Ollama health
curl http://localhost:11434/api/tags
```

### Port Checking
```bash
# Check if port 8080 is in use
lsof -i :8080

# Check if port 3000 is in use
lsof -i :3000

# Check if port 11434 is in use (Ollama)
lsof -i :11434

# Kill process on specific port
kill -9 $(lsof -t -i:8080)
```

### Log Viewing
```bash
# View backend logs (if log file exists)
tail -f backend/logs/application.log

# View backend console output
# (Check terminal where backend is running)

# View Maven output
./mvnw spring-boot:run | tee backend.log

# View frontend logs
# (Check terminal where frontend is running)
```

### API Testing
```bash
# Test chat endpoint
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello",
    "sessionId": "test-session"
  }'

# Test GitHub API
curl http://localhost:8080/api/github/repos

# Test user authentication
curl http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

---

## Database & Data

### Embeddings Management
```bash
# View embeddings file
cat backend/data/embeddings/embeddings.jsonl

# View embeddings count
wc -l backend/data/embeddings/embeddings.jsonl

# View first 10 embeddings
head -n 10 backend/data/embeddings/embeddings.jsonl

# Search embeddings
grep "search-term" backend/data/embeddings/embeddings.jsonl
```

### Backup & Restore
```bash
# Backup embeddings
cp backend/data/embeddings/embeddings.jsonl embeddings_backup_$(date +%Y%m%d).jsonl

# Restore embeddings
cp embeddings_backup_20251203.jsonl backend/data/embeddings/embeddings.jsonl

# Reset embeddings (will reindex on restart)
rm backend/data/embeddings/embeddings.jsonl
./restart.sh
```

### Data Directory
```bash
# Create data directory structure
mkdir -p backend/data/embeddings

# Check data directory size
du -sh backend/data/

# List all data files
find backend/data -type f
```

---

## Configuration

### Backend Configuration
```bash
# Edit application properties
nano backend/src/main/resources/application.properties

# View current configuration
cat backend/src/main/resources/application.properties

# Key configurations:
# - server.port=8080
# - repo.github.token=your_token
# - ollama.base-url=http://localhost:11434
# - ollama.model=granite4:micro-h
```

### Frontend Configuration
```bash
# Edit Vite configuration
nano frontend/vite.config.js

# Edit package.json
nano frontend/package.json

# View current config
cat frontend/vite.config.js
```

### Environment Variables
```bash
# Set GitHub token
export GITHUB_TOKEN="your_token_here"

# Set environment
export NODE_ENV=production
export SPRING_PROFILES_ACTIVE=production

# View environment variables
env | grep -E "(GITHUB|NODE|SPRING)"
```

---

## Monitoring

### Process Monitoring
```bash
# Watch backend process
ps aux | grep java

# Watch frontend process
ps aux | grep vite

# Watch Ollama process
ps aux | grep ollama

# Watch all related processes
ps aux | grep -E "(java|vite|ollama)"
```

### Resource Monitoring
```bash
# Monitor system resources
top

# Monitor memory usage
free -h

# Monitor disk space
df -h

# Monitor disk usage by directory
du -sh */

# Real-time monitoring
htop  # if installed
```

### Service Status
```bash
# Check if services are running
curl -s http://localhost:8080/api/health && echo "✅ Backend is running"
curl -s http://localhost:3000 && echo "✅ Frontend is running"
curl -s http://localhost:11434/api/tags && echo "✅ Ollama is running"
```

---

## Quick Access URLs

### Application URLs
```bash
# Frontend (Development)
http://localhost:3000

# Backend API
http://localhost:8080

# Backend Health Check
http://localhost:8080/api/health

# Ollama API
http://localhost:11434

# Ollama Health
http://localhost:11434/api/tags
```

### API Endpoints
```bash
# Chat
POST http://localhost:8080/api/chat

# GitHub Repositories
GET http://localhost:8080/api/github/repos

# User Authentication
POST http://localhost:8080/api/auth/login

# Chat History
GET http://localhost:8080/api/history

# Execution History
GET http://localhost:8080/api/execution-history
```

---

## Advanced Commands

### Performance Tuning
```bash
# Run backend with custom JVM options
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx2g -Xms1g"

# Build with parallel processing
./mvnw clean install -T 4

# Run frontend with custom port
npm run dev -- --port 3001
```

### Process Management
```bash
# Kill all related processes
pkill -f "spring-boot:run"
pkill -f "vite"
pkill -f "ollama"

# Forcefully kill process by PID
kill -9 <PID>

# Find process by port
lsof -ti:8080 | xargs kill -9
```

### Debugging
```bash
# Run backend in debug mode
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Enable verbose logging
./mvnw spring-boot:run -Dlogging.level.root=DEBUG

# Run frontend with debug
npm run dev -- --debug
```

### Batch Operations
```bash
# Full clean rebuild
./mvnw clean && rm -rf target && ./mvnw install

# Update all dependencies
cd backend && ./mvnw versions:use-latest-releases && cd ../frontend && npm update && cd ..

# Complete reset
./stop.sh
rm -rf backend/target
rm -rf frontend/node_modules
rm -f backend/data/embeddings/embeddings.jsonl
./setup.sh
```

---

## Troubleshooting

### Version Checks
```bash
# Check Java version
java -version

# Check Node.js version
node -v

# Check npm version
npm -v

# Check Maven version
mvn -v

# Check Ollama version
ollama --version

# Check Git version
git --version
```

### Service Status Check
```bash
# Verify all services
ps aux | grep -E "(java|vite|ollama)"

# Check which services are running
systemctl status ollama  # if using systemd

# Check network connections
netstat -an | grep -E "(8080|3000|11434)"
```

### Common Issues

#### Port Already in Use
```bash
# Find and kill process on port 8080
lsof -ti:8080 | xargs kill -9

# Or use the restart script
./restart.sh
```

#### Ollama Not Running
```bash
# Start Ollama
ollama serve

# Or in background
nohup ollama serve > /dev/null 2>&1 &
```

#### Build Failures
```bash
# Clean and rebuild
./mvnw clean install -U

# Clear Maven cache
rm -rf ~/.m2/repository
./mvnw clean install
```

#### Frontend Issues
```bash
# Clear cache and reinstall
cd frontend
rm -rf node_modules package-lock.json
npm cache clean --force
npm install
```

#### Permission Issues
```bash
# Make scripts executable
chmod +x *.sh

# Fix ownership
sudo chown -R $USER:$USER .
```

### Log Analysis
```bash
# Search for errors in logs
grep -i error backend/logs/application.log

# Search for exceptions
grep -i exception backend/logs/application.log

# View last 100 lines of logs
tail -n 100 backend/logs/application.log
```

### Network Debugging
```bash
# Test network connectivity
ping localhost

# Check DNS resolution
nslookup github.com

# Test HTTP connectivity
curl -I http://localhost:8080
```

---

## Utility Scripts

### Theme Switcher
```bash
# Switch UI theme
./theme-switcher.sh
```

### View Documentation
```bash
# Project overview
cat PROJECT_OVERVIEW.md

# API reference
cat API_REFERENCE.md

# This command reference
cat COMMANDS.md

# Flowcharts
cat BACKEND_FLOWCHART.md
cat FRONTEND_FLOWCHART.md
```

---

## Quick Reference Card

```bash
# Setup
./setup.sh              # First-time setup

# Daily Operations
./start.sh              # Start services
./stop.sh               # Stop services
./restart.sh            # Restart services

# Development
cd backend && ./mvnw spring-boot:run    # Run backend
cd frontend && npm run dev              # Run frontend

# Testing
curl http://localhost:8080/api/health   # Test backend
curl http://localhost:11434/api/tags    # Test Ollama

# Monitoring
ps aux | grep -E "(java|vite|ollama)"   # Check processes
lsof -i :8080                           # Check port

# Troubleshooting
./restart.sh            # Fix most issues
pkill -f "spring-boot"  # Kill backend
pkill -f "vite"         # Kill frontend
```

---

## Tips & Best Practices

1. **Always use the provided scripts** (`./start.sh`, `./stop.sh`, `./restart.sh`) for consistent service management
2. **Check logs** when encountering issues - they provide detailed error information
3. **Keep Ollama running** in the background for faster AI response times
4. **Backup embeddings** periodically if you have custom repository data
5. **Use `./restart.sh`** instead of manually stopping/starting services
6. **Monitor resource usage** when running all services locally
7. **Update dependencies** regularly for security patches
8. **Use version control** for any configuration changes
9. **Test after changes** using the health check endpoints
10. **Read documentation** in the project root for detailed information

---

**Last Updated:** December 3, 2025  
**Project:** AI Chatbot - GitHub Knowledge Assistant  
**Repository:** https://github.com/ShreyasGowda2004/Usecases_Var
