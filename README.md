# AI Chatbot - GitHub Knowledge Assistant

A powerful, production-ready AI chatbot system that provides intelligent assistance for GitHub repositories using advanced RAG (Retrieval-Augmented Generation) technology with keyword-based search and enterprise security features.

## 📋 Table of Contents
- [Features](#-core-features)
- [Quick Start](#-quick-start)
- [System Requirements](#-system-requirements)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Usage](#-usage)
- [Architecture](#-architecture)
- [Documentation](#-documentation)

## 📚 Complete Documentation

This project includes comprehensive professional documentation:

| Document | Description | Link |
|----------|-------------|------|
| **📖 Project Documentation** | Complete guide with all diagrams, architecture, and features | [PROJECT_DOCUMENTATION.md](./PROJECT_DOCUMENTATION.md) |
| **🚀 Deployment Guide** | Production deployment strategies and configurations | [DEPLOYMENT.md](./DEPLOYMENT.md) |
| **📡 API Reference** | Complete API documentation with examples | [API_REFERENCE.md](./API_REFERENCE.md) |
| **🤝 Contributing Guide** | How to contribute to this project | [CONTRIBUTING.md](./CONTRIBUTING.md) |
| **🧪 Testing Guide** | Comprehensive testing strategies and examples | [TESTING.md](./TESTING.md) |
| **🏗️ Architecture** | System architecture and design patterns | [ARCHITECTURE.md](./ARCHITECTURE.md) |
| **📝 Backend Architecture** | Detailed backend design | [BACKEND_ARCHITECTURE.md](./BACKEND_ARCHITECTURE.md) |
| **🔧 Installation Guide** | Detailed setup instructions | [INSTALLATION.md](./INSTALLATION.md) |

### Quick Documentation Access

```bash
# View main documentation
cat PROJECT_DOCUMENTATION.md

# View specific guides
cat DEPLOYMENT.md
cat API_REFERENCE.md
cat TESTING.md
```

## 🚀 Quick Start

For new users who just cloned/downloaded this repository:

```bash
# 1. Clone the repository
git clone <your-repo-url>
cd ai-chatbot

# 2. Run setup (automatically installs all requirements)
./setup.sh

# 3. Configure GitHub token
# Edit backend/src/main/resources/application.properties
# Update: repo.github.token=your_actual_token

# 4. Start the application
./start.sh

# 5. Access the application
# Frontend: http://localhost:3000
# Backend: http://localhost:8080
```

That's it! The setup script will automatically install:
- ✅ Java 17+ (if not present)
- ✅ Node.js 18+ (if not present)
- ✅ Maven (if not present)
- ✅ Ollama (if not present)
- ✅ AI Model (granite4:micro-h)
- ✅ All project dependencies

## 💻 System Requirements

### Supported Operating Systems
- **macOS** (Intel & Apple Silicon)
- **Linux** (Ubuntu/Debian, CentOS/RHEL, Fedora)
- **Windows** (WSL2 recommended)

### Automatic Installation
The `setup.sh` script will automatically check and install:

| Requirement | Version | Auto-Install | Notes |
|-------------|---------|--------------|-------|
| Java | 17+ | ✅ Yes | OpenJDK 17 |
| Node.js | 18+ | ✅ Yes | Node 20 LTS |
| npm | Latest | ✅ Yes | Comes with Node.js |
| Maven | 3.6+ | ✅ Yes | Or uses wrapper |
| Git | Any | ✅ Yes | Version control |
| Ollama | Latest | ✅ Yes | AI model runtime |
| Homebrew | Latest | ✅ Yes | macOS only |

### Hardware Recommendations
- **RAM**: 8GB minimum, 16GB recommended
- **Storage**: 10GB free space
- **CPU**: Multi-core processor recommended for Ollama
- **Network**: Internet connection for initial setup

## 🛠️ Installation

### Method 1: Automatic Setup (Recommended)

Perfect for users who just downloaded/cloned the project:

```bash
# Make setup script executable (if needed)
chmod +x setup.sh

# Run setup - it handles everything!
./setup.sh
```

The setup script will:
1. ✅ Detect your operating system
2. ✅ Check for required software
3. ✅ Install missing dependencies automatically
4. ✅ Set up backend and frontend
5. ✅ Pull AI model
6. ✅ Validate the installation
7. ✅ Create .env configuration file

### Method 2: Manual Installation

If you prefer manual control:

#### 1. Install Java 17+
```bash
# macOS
brew install openjdk@17

# Ubuntu/Debian
sudo apt-get install openjdk-17-jdk

# CentOS/RHEL
sudo yum install java-17-openjdk-devel
```

#### 2. Install Node.js 18+
```bash
# macOS
brew install node@20

# Ubuntu/Debian
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# CentOS/RHEL
curl -fsSL https://rpm.nodesource.com/setup_20.x | sudo bash -
sudo yum install -y nodejs
```

#### 3. Install Ollama
```bash
# macOS & Linux
curl -fsSL https://ollama.ai/install.sh | sh

# Start Ollama
ollama serve &

# Pull AI model
ollama pull granite4:micro-h
```

#### 4. Build Backend
```bash
cd backend
./mvnw clean compile
cd ..
```

#### 5. Setup Frontend
```bash
cd frontend
npm install
cd ..
```

## �🚀 Latest Features (September 2025)

### 🏗️ **Keyword-Based Search Architecture** 
- **File-Based Storage**: Simple JSON Lines storage for document text chunks
- **Fast Keyword Search**: In-memory keyword matching with relevance scoring
- **Intelligent Ranking**: Multiple scoring factors (content matches, filename relevance, term frequency)
- **Lightweight Design**: No external databases required for search

### 🔍 **Enhanced Keyword Search**
- **Smart Question Understanding**: Handles different phrasings of the same question
- **Case-Insensitive Matching**: Works regardless of capitalization
- **Keyword Expansion**: Automatically expands search terms with synonyms and variations
- **Context-Aware Responses**: Understands intent regardless of exact wording
- **Relevance Scoring**: Multi-factor scoring algorithm for accurate results
- **Example**: "Create TableSpaces", "how to create tablespaces", "creating table spaces" all return the same relevant content

**Important Note**: This system uses **keyword-based search**, NOT neural embeddings or vector similarity. Ollama is used ONLY for generating chat responses.

### 🔒 **Enterprise Security**
- **Source Path Sanitization**: File paths are hidden for security
- **Generic Source References**: Shows "Database Configuration Guide" instead of actual file paths
- **No Internal Structure Exposure**: Repository organization remains private
- **User-Friendly Labels**: Meaningful descriptions instead of technical paths

### 🎨 **IBM Carbon Design System UI**
- **Modern Interface**: Complete IBM Carbon Design System implementation
- **Theme Persistence**: Dark/Light theme preferences saved
- **Chat Persistence**: Conversations persist across page reloads
- **Responsive Design**: Optimized for desktop and mobile
- **Maximo Application Suite Styling**: Enterprise-grade visual design

## 🚀 Core Features

- **File-Based Document Store**: Simple JSONL storage for document text chunks
- **Keyword-Based RAG System**: Processes GitHub repositories with intelligent keyword search for fast, accurate responses
- **Real-time Repository Access**: Direct GitHub API integration without local cloning
- **Ollama Integration**: Uses Ollama with Granite 4:micro-h model for high-quality chat responses (NOT for embeddings)
- **Fast Response Times**: Optimized with in-memory keyword search and caching
- **Production Ready**: Spring Boot backend with comprehensive error handling and monitoring

## 🔐 Login and Local Chat History

This project now includes a basic, local-only login and chat history feature in the frontend:

- On first load, you'll be asked for a username. No password or account is required.
- The username is stored in the browser's localStorage under `authUser`.
- Current conversation is saved per user using `chatCurrent:<username>`.
- Past conversations are stored in `chatHistory:<username>` and can be opened from the History button in the chat input bar.
- All data is local to the browser and not sent to the server.

## 🏗️ Architecture

### Backend (Spring Boot)
- **Framework**: Spring Boot 3.2+ with Java 17
- **AI Integration**: Ollama client for LLM interactions (chat responses only)
- **Document Storage**: 
  - **File-Based**: JSON Lines (.jsonl) for document text chunks
  - **MongoDB**: User data and chat history (optional)
- **Document Processing**: Apache Tika for text extraction
- **Search Method**: Keyword-based matching with multi-factor relevance scoring
- **No Vector Embeddings**: Pure keyword search approach

### Frontend (React + Vite + IBM Carbon)
- **Framework**: React 18 with modern hooks and IBM Carbon Design System
- **Build Tool**: Vite for fast development and optimized builds
- **UI Components**: IBM Carbon components with Maximo Application Suite styling
- **Theme System**: Persistent dark/light theme switching
- **Markdown Support**: Rich formatting for AI responses with code highlighting
- **Real-time Updates**: WebSocket-like experience with REST APIs
- **Responsive Design**: Optimized for desktop and mobile devices
- **Chat Persistence**: Conversations saved across page reloads

### 🔍 **Enhanced Search & AI**
- **Keyword Query Processing**: Understands different question formats and phrasings
- **Relevance Scoring**: Multi-factor scoring (content matches, filename relevance, term frequency)
- **Keyword Expansion Engine**: Automatically expands search terms with synonyms
- **Context-Aware Scoring**: Prioritizes content relevance over filename matches
- **Fallback Search Strategy**: Multiple search algorithms for comprehensive coverage
- **Smart Caching**: Redis-based caching for improved performance

## 📋 Prerequisites

### Required Software
1. **Java 17+** - For Spring Boot backend
2. **Node.js 18+** - For React frontend
3. **Ollama** - For AI model hosting
4. **Git** - For version control

### Ollama Setup
```bash
# Install Ollama (macOS/Linux)
curl -fsSL https://ollama.ai/install.sh | sh

# Pull the Granite model
ollama pull granite4:micro-h

# Start Ollama server (runs on http://localhost:11434)
ollama serve
```

### GitHub Token
You need a GitHub Enterprise personal access token with repository read permissions.

1. Generate a GitHub Enterprise token:
   - Go to your GitHub Enterprise instance (e.g., github.ibm.com)
   - Click on your profile picture > Settings > Developer settings > Personal access tokens
   - Click "Generate new token"
   - Give it a name like "AI Chatbot"
   - Select the `repo` scope for private repositories
   - Click "Generate token" and copy the token

2. Set the token as an environment variable:
   ```bash
   export GITHUB_TOKEN="your_github_enterprise_token"
   ```
   
   Or update the token directly in `application.properties`:
   ```properties
   repo.github.token=your_github_enterprise_token
   ```

## 🛠️ Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd ai-chatbot
```

### 2. Backend Setup
```bash
cd backend

# Update application.properties with your GitHub token
# Edit src/main/resources/application.properties
# Set: repo.github.token=your_github_token_here

# Build and run the backend
./mvnw clean install
./mvnw spring-boot:run
```

The backend will start on `http://localhost:8080`

### 3. Frontend Setup
```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will start on `http://localhost:3000`

## ⚙️ Configuration

### Application Properties
All configuration is done in `backend/src/main/resources/application.properties`:

```properties
```properties
# Ollama Configuration
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=granite4:micro-h

# GitHub Repository (Update for your repo)
repo.github.baseurl=https://github.ibm.com/api/v3
```

## 🚀 Usage

### 1. System Initialization
1. Open the application at `http://localhost:3000`
2. The system will automatically index configured GitHub repositories on startup
3. Wait for indexing to complete (may take several minutes for large repos)

### 2. Chat Interface Features
1. Switch to the **Chat** tab
2. Experience the enhanced keyword search:
   - Ask questions in any format or capitalization
   - Use different phrasings of the same question
   - Get consistent results regardless of wording
3. **Security Features**:
   - Source references show generic descriptions (e.g., "Database Configuration Guide")
   - No internal file paths exposed
   - Secure by design

### 3. Advanced UI Features
- **Theme Switching**: Toggle between dark and light themes (persisted)
- **Chat Persistence**: Conversations automatically saved and restored
- **Response Time Tracking**: See processing time for each response
- **Markdown Rendering**: Rich formatting with code syntax highlighting
- **Export Functionality**: Download chat conversations
- **Regenerate Responses**: Get alternative AI responses

## 🎯 Enhanced Example Queries

The system now understands various phrasings of the same question:

### Database Operations
```
"Create TableSpaces"                    # Exact terminology
"how to create tablespaces"            # Question format
"creating table spaces"                # Different tense
"tablespace creation"                  # Different structure
"setup database tablespaces"          # Alternative wording
"db2 tablespace configuration"        # Technical terms
```

### General Queries
```
"What is this repository about?"       # High-level overview
"How do I get started with this project?"  # Getting started
"What are the configuration options?"   # Configuration help
"Explain the database setup process"    # Specific procedures
"Show me examples of API usage"         # Code examples
"What are the security considerations?" # Security information
```

### Search Intelligence
- **Case Insensitive**: Works with any capitalization
- **Synonym Recognition**: "create" = "setup" = "configure" = "build"
- **Flexible Phrasing**: Understands questions vs. statements
- **Context Preservation**: Maintains meaning across different formats

## 🔧 Development

### Backend Development
```bash
cd backend
./mvnw spring-boot:run
```

### Frontend Development
```bash
cd frontend
npm run dev
```

### Building for Production
```bash
# Backend
cd backend
./mvnw clean package

# Frontend
cd frontend
npm run build
```

## 📊 Performance

- **Response Time**: Typically under 2-3 seconds
- **Concurrent Users**: Supports multiple simultaneous conversations
- **Repository Size**: Efficiently handles repositories with thousands of files
- **Memory Usage**: Optimized for production deployment
- **Caching**: Redis-based caching for frequently accessed content

## 🔐 Security

- GitHub token stored securely as environment variable
- CORS protection configured
- Input validation and sanitization
- Rate limiting implemented
- Error handling prevents information leakage

## 🚀 Deployment

### Docker (Optional)
```bash
# Build backend
cd backend
./mvnw clean package

# Build frontend
cd frontend
npm run build

# Create Docker images and deploy using your preferred orchestration
```

### Production Checklist
- [ ] Set up proper GitHub token management
- [ ] Configure Redis for production caching
- [ ] Set up proper logging and monitoring
- [ ] Configure HTTPS/SSL
- [ ] Set up database persistence (PostgreSQL recommended)
- [ ] Configure proper CORS origins
- [ ] Set up backup and recovery procedures

## � Security Features

### Data Protection
- **Source Path Sanitization**: All file paths are converted to generic descriptions
- **No Internal Structure Exposure**: Repository organization remains private
- **Secure Token Management**: GitHub tokens handled securely
- **CORS Protection**: Proper cross-origin resource sharing configuration

### Privacy Controls
- **Anonymous Source References**: 
  - Instead of: `devops/db2/03-Maximo-DB-Prerequisite-Configuration.md`
  - Shows: `Database Configuration Guide`
- **No File System Exposure**: Users cannot see internal directory structures
- **Sanitized Responses**: All responses filtered for security

## 🧠 Keyword Search Engine

### Intelligence Features
- **Keyword Expansion**: Automatically expands search terms with related words
- **Synonym Recognition**: Understands equivalent terms (create/setup/configure)
- **Context Understanding**: Maintains meaning across different question formats
- **Fallback Strategies**: Multiple search algorithms ensure comprehensive coverage
- **Relevance Scoring**: Multi-factor scoring for accurate results

**Important**: This is NOT a semantic/vector search engine. It uses keyword matching with intelligent scoring algorithms.

### Search Examples
| User Input | Understood As | Result |
|------------|---------------|---------|
| "Create TableSpaces" | Database table space creation | DB2 tablespace commands |
| "how to create tablespaces" | Database table space creation | Same DB2 tablespace commands |
| "tablespace setup" | Database table space creation | Same DB2 tablespace commands |

## �🔍 Troubleshooting

### Common Issues

1. **Ollama Connection Failed**
   - Ensure Ollama is running: `ollama serve`
   - Check model is available: `ollama list`
   - Verify base URL in configuration

2. **GitHub API Rate Limiting**
   - Check your GitHub token permissions
   - Ensure token has repository read access
   - Monitor rate limits in application logs

3. **Inconsistent Search Results**
   - Repository may need reindexing: Restart application to trigger automatic reindex
   - Check if repository content has changed
   - Clear browser cache and restart application

4. **Frontend Connection Issues**
   - Verify backend is running on port 8080
   - Check CORS configuration
   - Verify proxy settings in vite.config.js
   - Clear localStorage if theme/chat issues persist

## 📝 API Documentation

### Chat Endpoints
- `POST /api/chat/message` - Send chat message
- `GET /api/chat/history/{sessionId}` - Get chat history
- `DELETE /api/chat/history/{sessionId}` - Clear chat history
- `POST /api/chat/regenerate/{sessionId}` - Regenerate last response

### Health Endpoints
- `GET /api/health` - System health check

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- **Ollama** - For providing excellent local LLM hosting
- **Spring AI** - For AI integration framework
- **IBM Carbon Design System** - For enterprise-grade UI components
- **React + Vite** - For modern frontend development
- **Apache Tika** - For document processing capabilities
- **Granite 4:micro-h Model** - For high-quality AI chat responses (NOT for embeddings)

---

## 📈 Recent Updates (September 2025)

### Version 2.0 - Enhanced Keyword Search & Security
- ✅ Implemented advanced keyword search with keyword expansion
- ✅ Added case-insensitive and format-flexible question handling
- ✅ Integrated IBM Carbon Design System for enterprise UI
- ✅ Added security features with source path sanitization
- ✅ Implemented theme and chat persistence
- ✅ Enhanced responsive design for mobile devices
- ✅ Improved error handling and user experience

### Performance Improvements
- ✅ Sub-second response times with optimized search algorithms
- ✅ Enhanced caching strategies for better performance
- ✅ Reduced memory footprint with efficient data structures
- ✅ Optimized database queries for faster retrieval

This system now provides enterprise-grade AI assistance with enhanced security, improved user experience, and intelligent keyword search capabilities! 🚀
