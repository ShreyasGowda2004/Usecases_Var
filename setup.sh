#!/bin/bash

# AI Chatbot Setup Script
# This script automatically checks and installs all requirements for the AI Chatbot project
# Supports: macOS, Linux (Ubuntu/Debian, CentOS/RHEL/Fedora)

set -e

echo "🛠️  AI Chatbot Project Setup"
echo "============================"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}➜ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

# Detect OS
detect_os() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        OS="macos"
        print_info "Detected OS: macOS"
    elif [[ -f /etc/os-release ]]; then
        . /etc/os-release
        if [[ "$ID" == "ubuntu" ]] || [[ "$ID" == "debian" ]]; then
            OS="debian"
            print_info "Detected OS: $PRETTY_NAME"
        elif [[ "$ID" == "centos" ]] || [[ "$ID" == "rhel" ]] || [[ "$ID" == "fedora" ]]; then
            OS="redhat"
            print_info "Detected OS: $PRETTY_NAME"
        else
            OS="linux"
            print_info "Detected OS: $PRETTY_NAME"
        fi
    else
        OS="unknown"
        print_warning "Unknown OS detected"
    fi
}

# Check and install Homebrew (macOS only)
check_install_homebrew() {
    if [[ "$OS" == "macos" ]]; then
        if ! command -v brew &> /dev/null; then
            print_warning "Homebrew not found. Installing..."
            /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
            
            # Add Homebrew to PATH for Apple Silicon
            if [[ $(uname -m) == "arm64" ]]; then
                echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
                eval "$(/opt/homebrew/bin/brew shellenv)"
            fi
            
            print_success "Homebrew installed"
        else
            print_success "Homebrew already installed"
        fi
    fi
}

# Check and install Java 17+
check_install_java() {
    print_status "Checking Java..."
    
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [[ "$JAVA_VERSION" -ge 17 ]]; then
            print_success "Java $JAVA_VERSION found"
            return 0
        else
            print_warning "Java $JAVA_VERSION found, but version 17+ is required"
        fi
    else
        print_warning "Java not found"
    fi
    
    # Install Java based on OS
    print_status "Installing Java 17..."
    if [[ "$OS" == "macos" ]]; then
        brew install openjdk@17
        
        # Link Java for macOS (handle both Intel and Apple Silicon)
        if [[ $(uname -m) == "arm64" ]]; then
            # Apple Silicon
            sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk 2>/dev/null || true
            echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
            export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
        else
            # Intel
            sudo ln -sfn /usr/local/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk 2>/dev/null || true
            echo 'export PATH="/usr/local/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
            export PATH="/usr/local/opt/openjdk@17/bin:$PATH"
        fi
    elif [[ "$OS" == "debian" ]]; then
        sudo apt-get update
        sudo apt-get install -y openjdk-17-jdk
    elif [[ "$OS" == "redhat" ]]; then
        sudo yum install -y java-17-openjdk-devel
    else
        print_error "Please install Java 17+ manually from https://adoptium.net/"
        exit 1
    fi
    
    print_success "Java 17 installed"
}

# Check and install Node.js 18+
check_install_node() {
    print_status "Checking Node.js..."
    
    if command -v node &> /dev/null; then
        NODE_VERSION=$(node --version | cut -d'v' -f2 | cut -d'.' -f1)
        if [[ "$NODE_VERSION" -ge 18 ]]; then
            print_success "Node.js v$(node --version | cut -d'v' -f2) found"
            return 0
        else
            print_warning "Node.js v$(node --version | cut -d'v' -f2) found, but version 18+ is required"
        fi
    else
        print_warning "Node.js not found"
    fi
    
    # Install Node.js based on OS
    print_status "Installing Node.js 20 LTS..."
    if [[ "$OS" == "macos" ]]; then
        brew install node@20
        
        # Handle path for both Intel and Apple Silicon
        if [[ $(uname -m) == "arm64" ]]; then
            # Apple Silicon
            echo 'export PATH="/opt/homebrew/opt/node@20/bin:$PATH"' >> ~/.zshrc
            export PATH="/opt/homebrew/opt/node@20/bin:$PATH"
        else
            # Intel
            echo 'export PATH="/usr/local/opt/node@20/bin:$PATH"' >> ~/.zshrc
            export PATH="/usr/local/opt/node@20/bin:$PATH"
        fi
    elif [[ "$OS" == "debian" ]]; then
        curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
        sudo apt-get install -y nodejs
    elif [[ "$OS" == "redhat" ]]; then
        curl -fsSL https://rpm.nodesource.com/setup_20.x | sudo bash -
        sudo yum install -y nodejs
    else
        print_error "Please install Node.js 18+ manually from https://nodejs.org/"
        exit 1
    fi
    
    print_success "Node.js installed"
}

# Check and install Maven
check_install_maven() {
    print_status "Checking Maven..."
    
    if command -v mvn &> /dev/null; then
        MAVEN_VERSION=$(mvn -version | grep "Apache Maven" | awk '{print $3}' | cut -d'.' -f1)
        print_success "Maven $(mvn -version | grep 'Apache Maven' | awk '{print $3}') found"
        return 0
    else
        print_warning "Maven not found"
    fi
    
    # Install Maven based on OS
    print_status "Installing Maven..."
    if [[ "$OS" == "macos" ]]; then
        brew install maven
    elif [[ "$OS" == "debian" ]]; then
        sudo apt-get install -y maven
    elif [[ "$OS" == "redhat" ]]; then
        sudo yum install -y maven
    else
        print_warning "Maven not installed. Will use Maven wrapper (mvnw)"
        return 0
    fi
    
    print_success "Maven installed"
}

# Check and install Git
check_install_git() {
    print_status "Checking Git..."
    
    if command -v git &> /dev/null; then
        print_success "Git $(git --version | awk '{print $3}') found"
        return 0
    else
        print_warning "Git not found"
    fi
    
    # Install Git based on OS
    print_status "Installing Git..."
    if [[ "$OS" == "macos" ]]; then
        brew install git
    elif [[ "$OS" == "debian" ]]; then
        sudo apt-get install -y git
    elif [[ "$OS" == "redhat" ]]; then
        sudo yum install -y git
    else
        print_error "Please install Git manually"
        exit 1
    fi
    
    print_success "Git installed"
}

# Check and install Ollama
check_install_ollama() {
    print_status "Checking Ollama..."
    
    if command -v ollama &> /dev/null; then
        print_success "Ollama CLI found"
        
        # Check if ollama service is running
        if curl -s http://localhost:11434/api/tags &> /dev/null; then
            print_success "Ollama service is running"
        else
            print_warning "Ollama is installed but not running"
            print_info "Starting Ollama service..."
            if [[ "$OS" == "macos" ]]; then
                open -a Ollama 2>/dev/null || print_warning "Please start Ollama manually"
            else
                print_warning "Please start Ollama manually: ollama serve &"
            fi
        fi
        return 0
    else
        print_warning "Ollama not found"
    fi
    
    # Install Ollama based on OS
    print_status "Installing Ollama..."
    if [[ "$OS" == "macos" ]]; then
        print_info "Installing Ollama for macOS..."
        
        # Try Homebrew first (most reliable)
        if command -v brew &> /dev/null; then
            print_info "Using Homebrew to install Ollama..."
            brew install ollama
            print_success "Ollama installed via Homebrew"
        else
            # Fallback to direct download
            print_info "Downloading Ollama app..."
            if ! command -v curl &> /dev/null; then
                print_error "curl is required but not found"
                exit 1
            fi
            
            # Download the macOS app
            OLLAMA_URL="https://ollama.com/download/Ollama-darwin.zip"
            if curl -fsSL "$OLLAMA_URL" -o /tmp/Ollama.zip; then
                if unzip -q /tmp/Ollama.zip -d /tmp/ 2>/dev/null; then
                    sudo cp -R /tmp/Ollama.app /Applications/ 2>/dev/null || {
                        print_error "Failed to copy Ollama to /Applications. Need sudo access."
                        rm -rf /tmp/Ollama.zip /tmp/Ollama.app
                        exit 1
                    }
                    rm -rf /tmp/Ollama.zip /tmp/Ollama.app
                    print_success "Ollama installed to /Applications"
                else
                    print_error "Failed to extract Ollama. Please install manually from https://ollama.com/download"
                    rm -rf /tmp/Ollama.zip
                    exit 1
                fi
            else
                print_error "Failed to download Ollama. Please install manually from https://ollama.com/download"
                exit 1
            fi
        fi
    elif [[ "$OS" == "linux" ]] || [[ "$OS" == "debian" ]] || [[ "$OS" == "redhat" ]]; then
        print_info "Installing Ollama for Linux..."
        curl -fsSL https://ollama.ai/install.sh | sh
    else
        print_error "Please install Ollama manually from https://ollama.ai"
        exit 1
    fi
    
    print_success "Ollama installed"
    print_info "Starting Ollama service..."
    if [[ "$OS" == "macos" ]]; then
        # Start Ollama app on macOS
        open -a Ollama 2>/dev/null &
        sleep 2
        # If app didn't start, try CLI
        if ! curl -s http://localhost:11434/api/tags &> /dev/null; then
            nohup ollama serve &> /dev/null &
        fi
    else
        # Start Ollama on Linux
        nohup ollama serve &> /dev/null &
    fi
    sleep 3
}

# Pull AI model
pull_ai_model() {
    print_status "Checking AI model..."
    
    if ollama list 2>/dev/null | grep -q "granite4:micro-h"; then
        print_success "AI model 'granite4:micro-h' already exists"
        return 0
    fi
    
    print_status "Pulling AI model 'granite4:micro-h' (this may take a few minutes)..."
    if ollama pull granite4:micro-h; then
        print_success "AI model downloaded successfully"
    else
        print_warning "Failed to pull model. You can do it later with: ollama pull granite4:micro-h"
    fi
}

# Check and install curl
check_install_curl() {
    if ! command -v curl &> /dev/null; then
        print_status "Installing curl..."
        if [[ "$OS" == "macos" ]]; then
            brew install curl
        elif [[ "$OS" == "debian" ]]; then
            sudo apt-get install -y curl
        elif [[ "$OS" == "redhat" ]]; then
            sudo yum install -y curl
        fi
        print_success "curl installed"
    fi
}

# Check prerequisites
check_prerequisites() {
    print_status "📋 Checking and installing prerequisites..."
    echo ""
    
    detect_os
    echo ""
    
    check_install_curl
    check_install_homebrew
    check_install_git
    check_install_java
    check_install_node
    check_install_maven
    check_install_ollama
    
    echo ""
    print_success "All prerequisites checked/installed"
    echo ""
}

# Setup backend
setup_backend() {
    print_status "🔧 Setting up backend..."
    
    if [ ! -d "backend" ]; then
        print_error "Backend directory not found!"
        exit 1
    fi
    
    cd backend
    
    # Make Maven wrapper executable
    if [ -f "mvnw" ]; then
        chmod +x mvnw
    else
        print_warning "Maven wrapper not found, generating..."
        if command -v mvn &> /dev/null; then
            mvn -N io.takari:maven:wrapper
            chmod +x mvnw
        else
            print_error "Cannot generate Maven wrapper. Please install Maven."
            exit 1
        fi
    fi
    
    # Build backend
    print_status "📦 Building backend (this may take a few minutes)..."
    if ./mvnw clean compile; then
        print_success "Backend compiled successfully"
    else
        print_error "Backend compilation failed"
        cd ..
        exit 1
    fi
    
    cd ..
}

# Setup frontend
setup_frontend() {
    print_status "🎨 Setting up frontend..."
    
    if [ ! -d "frontend" ]; then
        print_error "Frontend directory not found!"
        exit 1
    fi
    
    cd frontend
    
    # Check if package.json exists
    if [ ! -f "package.json" ]; then
        print_error "package.json not found in frontend directory!"
        cd ..
        exit 1
    fi
    
    # Install dependencies
    print_status "📦 Installing frontend dependencies..."
    if npm install; then
        print_success "Frontend dependencies installed"
    else
        print_error "Failed to install frontend dependencies"
        cd ..
        exit 1
    fi
    
    cd ..
}

# Validate setup
validate_setup() {
    print_status "🔍 Validating setup..."
    
    # Check if backend compiles
    cd backend
    print_status "Testing backend compilation..."
    if ./mvnw compile -q; then
        print_success "Backend compilation test passed"
    else
        print_error "Backend compilation test failed"
        cd ..
        exit 1
    fi
    cd ..
    
    # Check if frontend has all dependencies
    cd frontend
    print_status "Testing frontend setup..."
    if [ -d "node_modules" ] && [ -f "package.json" ]; then
        print_success "Frontend setup validated"
    else
        print_error "Frontend setup validation failed"
        cd ..
        exit 1
    fi
    cd ..
    
    print_success "All validations passed"
}

# Display system information
display_system_info() {
    echo ""
    print_status "📊 System Information:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    if command -v java &> /dev/null; then
        JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
        echo "Java:    $JAVA_VER"
    fi
    
    if command -v node &> /dev/null; then
        echo "Node.js: $(node --version)"
    fi
    
    if command -v npm &> /dev/null; then
        echo "npm:     v$(npm --version)"
    fi
    
    if command -v mvn &> /dev/null; then
        MVN_VER=$(mvn -version 2>/dev/null | grep "Apache Maven" | awk '{print $3}')
        echo "Maven:   $MVN_VER"
    fi
    
    if command -v ollama &> /dev/null; then
        echo "Ollama:  ✓ Installed"
        if curl -s http://localhost:11434/api/tags &> /dev/null; then
            echo "         ✓ Service running"
        else
            echo "         ✗ Service not running"
        fi
    fi
    
    if command -v git &> /dev/null; then
        echo "Git:     $(git --version | awk '{print $3}')"
    fi
    
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

# Display next steps
show_next_steps() {
    echo ""
    echo "╔════════════════════════════════════════╗"
    echo "║   🎉 Setup Completed Successfully!    ║"
    echo "╚════════════════════════════════════════╝"
    echo ""
    
    display_system_info
    
    print_status "📝 Next Steps:"
    echo ""
    echo "  1. 🔑 Configure GitHub Token:"
    echo "     • Edit backend/src/main/resources/application.properties"
    echo "     • Update: repo.github.token=your_token_here"
    echo "     • Get token from: https://github.ibm.com/settings/tokens"
    echo ""
    echo "  2. 🤖 Ensure Ollama is running:"
    echo "     • Check: curl http://localhost:11434/api/tags"
    echo "     • Start if needed: ollama serve"
    echo ""
    echo "  3. 🚀 Start the application:"
    echo "     • Run: ./start.sh"
    echo "     • Or run backend and frontend separately"
    echo ""
    echo "  4. 🌐 Access the application:"
    echo "     • Frontend:    http://localhost:3000"
    echo "     • Backend API: http://localhost:8080"
    echo "     • Health Check: http://localhost:8080/api/health"
    echo ""
    
    print_status "🛠️  Useful Commands:"
    echo "  • ./start.sh          - Start both backend and frontend"
    echo "  • ./restart.sh        - Restart the application"
    echo "  • ./stop.sh           - Stop all services"
    echo ""
    
    print_status "📖 Documentation:"
    echo "  • See README.md for detailed instructions"
    echo "  • See BACKEND_ARCHITECTURE.md for backend details"
    echo "  • See RAG_System_Documentation.md for RAG system info"
    echo ""
    
    if ! curl -s http://localhost:11434/api/tags &> /dev/null; then
        print_warning "⚠️  Ollama service is not running!"
        echo "  Please start it with: ollama serve"
        echo "  Then pull the model: ollama pull granite4:micro-h"
        echo ""
    fi
    
    # Check if token is not set or is a placeholder
    if grep -q "your_token_here\|your_github_token\|YOUR_TOKEN" backend/src/main/resources/application.properties 2>/dev/null; then
        print_warning "⚠️  GitHub token not configured!"
        echo "  Please edit backend/src/main/resources/application.properties"
        echo "  Update: repo.github.token=your_actual_token"
        echo ""
    fi
}

# Main execution
main() {
    echo ""
    print_info "This script will automatically install all required dependencies"
    print_info "Press Ctrl+C to cancel at any time"
    echo ""
    sleep 2
    
    check_prerequisites
    pull_ai_model
    setup_backend
    setup_frontend
    validate_setup
    show_next_steps
}

# Run main function
main "$@"
