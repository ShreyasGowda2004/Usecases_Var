#!/bin/bash

# Load environment variables
export GITHUB_TOKEN="your_new_github_enterprise_token_here"

# Stop any running instances
./stop.sh

# Start backend
cd backend
./mvnw spring-boot:run &
cd ..

# Start frontend
cd frontend
npm run dev &
cd ..

echo "Application started!"
echo "Backend: http://localhost:8080/api"
echo "Frontend: http://localhost:3000"
