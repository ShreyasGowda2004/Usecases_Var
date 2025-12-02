#!/bin/bash

# Build Single JAR - Spring Boot Serving React Frontend
# This script builds the frontend and backend into a single executable JAR file

set -e  # Exit on error

echo "=================================================="
echo "Building Single JAR (Spring Boot + React)"
echo "=================================================="
echo ""

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}Step 1: Cleaning previous builds...${NC}"
cd backend
./mvnw clean
cd ..
echo -e "${GREEN}✓ Clean completed${NC}"
echo ""

echo -e "${BLUE}Step 2: Building React frontend and Spring Boot backend...${NC}"
echo "This will:"
echo "  - Install Node.js and npm"
echo "  - Install frontend dependencies"
echo "  - Build React app to backend/src/main/resources/static"
echo "  - Compile Spring Boot application"
echo "  - Package everything into a single JAR"
echo ""
cd backend
./mvnw package -DskipTests
cd ..
echo -e "${GREEN}✓ Build completed${NC}"
echo ""

# Find the JAR file
JAR_FILE=$(find backend/target -name "ai-chatbot-backend-*.jar" ! -name "*.original" | head -n 1)

if [ -f "$JAR_FILE" ]; then
    echo "=================================================="
    echo -e "${GREEN}SUCCESS! Single JAR created:${NC}"
    echo "  $(basename $JAR_FILE)"
    echo ""
    echo "Location:"
    echo "  $JAR_FILE"
    echo ""
    echo "File size: $(du -h "$JAR_FILE" | cut -f1)"
    echo ""
    echo "=================================================="
    echo "To run the application:"
    echo "  java -jar $JAR_FILE"
    echo ""
    echo "The application will be available at:"
    echo "  http://localhost:8080"
    echo ""
    echo "API endpoints will be at:"
    echo "  http://localhost:8080/api/*"
    echo "=================================================="
else
    echo -e "${RED}ERROR: JAR file not found!${NC}"
    exit 1
fi
