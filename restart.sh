#!/bin/bash

# AI Chatbot Restart Script
# This script stops any running instances and starts fresh backend and frontend services

set -e

# Ensure we always run from the script directory regardless of caller CWD
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$BASE_DIR"

# Ensure embeddings directory exists early; allow optional reset via env var
EMB_DIR="backend/data/embeddings"
mkdir -p "$EMB_DIR"
if [ "${RESET_EMBEDDINGS:-false}" = "true" ]; then
    echo "♻️  RESET_EMBEDDINGS=true → removing existing embeddings file"
    rm -f "$EMB_DIR/embeddings.jsonl" || true
fi

echo "🔄 AI Chatbot Restart Script"
echo "============================="

# Function to stop a service by PID
stop_service() {
    local service_name=$1
    local pid_file=$2
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if kill -0 "$pid" 2>/dev/null; then
            echo "🔴 Stopping $service_name (PID: $pid)..."
            kill "$pid"
            # Wait for process to stop
            local count=0
            while kill -0 "$pid" 2>/dev/null && [ $count -lt 10 ]; do
                sleep 1
                count=$((count + 1))
            done
            if kill -0 "$pid" 2>/dev/null; then
                echo "⚠️  Force stopping $service_name..."
                kill -9 "$pid" 2>/dev/null || true
            fi
            echo "✅ $service_name stopped"
        else
            echo "⚠️  $service_name was not running"
        fi
        rm -f "$pid_file"
    else
        echo "ℹ️  No PID file found for $service_name"
    fi
}

# Stop existing services
stop_existing_services() {
    echo "🛑 Stopping any existing services..."
    stop_service "Backend" ".backend.pid"
    stop_service "Frontend" ".frontend.pid"
    
    # Also kill any remaining processes (backup cleanup)
    echo "🧹 Cleaning up any remaining processes..."
    pkill -f "spring-boot:run" 2>/dev/null || true
    pkill -f "vite" 2>/dev/null || true
    
    # Small delay to ensure cleanup
    sleep 2
    echo "✅ Cleanup completed"
    echo ""
}

# Check if Ollama is running
check_ollama() {
    echo "📡 Checking Ollama service..."
    if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
        echo "✅ Ollama is running"
    else
        echo "❌ Ollama is not running. Please start Ollama first:"
        echo "   ollama serve"
        exit 1
    fi
}

# Check if required model is available
check_model() {
    echo "🤖 Checking for Granite model..."
    if ollama list | grep -q "granite4:micro-h"; then
        echo "✅ Granite 4:micro-h model found"
    else
        echo "📥 Downloading Granite 4:micro-h model..."
        ollama pull granite4:micro-h
    fi
}

# Start backend
start_backend() {
    echo "🔧 Starting backend service..."
    cd backend
    
    # Build and start backend
    echo "📦 Building backend..."
    ./mvnw clean install -q
    
    echo "🚀 Starting backend server..."
    ./mvnw spring-boot:run &
    BACKEND_PID=$!
    echo "Backend started with PID: $BACKEND_PID"
    
    # Wait for backend to be ready
    echo "⏳ Waiting for backend to be ready..."
    while ! curl -s http://localhost:8080/api/health > /dev/null 2>&1; do
        sleep 2
        echo "   Still waiting..."
    done
    echo "✅ Backend is ready!"
    
    # Optionally trigger repository initialization to (re)build embeddings
    if [ "${INIT_AFTER_START:-true}" = "true" ]; then
        echo "📚 Triggering repository initialization (/api/admin/initialize)"
        curl -s -X POST http://localhost:8080/api/admin/initialize >/dev/null 2>&1 || true
    fi
    
    cd ..
}

# Start frontend
start_frontend() {
    echo "🎨 Starting frontend service..."
    cd frontend
    
    # Install dependencies if needed
    if [ ! -d "node_modules" ]; then
        echo "📦 Installing frontend dependencies..."
        npm install
    fi
    
    echo "🚀 Starting frontend server..."
    npm run dev &
    FRONTEND_PID=$!
    echo "Frontend started with PID: $FRONTEND_PID"
    
    cd ..
}

# Main execution
main() {
    stop_existing_services
    check_ollama
    check_model
    start_backend
    start_frontend
    
    # Save PIDs to files for later cleanup
    echo $BACKEND_PID > .backend.pid
    echo $FRONTEND_PID > .frontend.pid
    
    echo ""
    echo "🎉 AI Chatbot is now running!"
    echo "================================"
    echo "Frontend: http://localhost:3000"
    echo "Backend:  http://localhost:8080"
    echo "Health:   http://localhost:8080/api/health"
    echo ""
    echo "Services are running in the background."
    echo "📋 Useful commands:"
    echo "   ./restart.sh        - Restart all services"
    echo "   ./stop.sh           - Stop all services"
    echo "   tail -f .backend.pid - Monitor backend"
    echo "   tail -f .frontend.pid - Monitor frontend"
    echo ""
}

# Handle command line arguments
case "${1:-}" in
    stop)
        echo "🛑 Stopping AI Chatbot Application"
        echo "================================="
        stop_service "Backend" ".backend.pid"
        stop_service "Frontend" ".frontend.pid"
        pkill -f "spring-boot:run" 2>/dev/null || true
        pkill -f "vite" 2>/dev/null || true
        echo ""
        echo "✅ All services stopped successfully!"
        echo "👋 Goodbye!"
        ;;
    *)
        # Default: restart services
        main "$@"
        ;;
esac
