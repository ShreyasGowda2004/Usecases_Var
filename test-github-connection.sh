#!/bin/bash

# Test GitHub API connectivity

echo "🔍 Testing GitHub Enterprise API connection..."

# Get the token - either from command line or environment variable
if [ "$1" ]; then
    TOKEN="$1"
elif [ "$GITHUB_TOKEN" ]; then
    TOKEN="$GITHUB_TOKEN"
else
    echo "❌ Error: No GitHub token provided!"
    echo "Usage: ./test-github-connection.sh <your_token>"
    echo "   or: export GITHUB_TOKEN=<your_token> && ./test-github-connection.sh"
    exit 1
fi

# Test API connection
BASEURL="https://github.ibm.com/api/v3"
REPO_OWNER="maximo-application-suite"
REPO_NAME="knowledge-center"

echo "📝 Testing connection to $BASEURL/repos/$REPO_OWNER/$REPO_NAME"

# Make the API request
response=$(curl -s -o response.txt -w "%{http_code}" \
  -H "Authorization: token $TOKEN" \
  -H "Accept: application/vnd.github+json" \
  "$BASEURL/repos/$REPO_OWNER/$REPO_NAME")

# Check status code
if [ "$response" -eq 200 ]; then
    echo "✅ GitHub API connection successful!"
    echo "Repository info:"
    cat response.txt | grep -E '"name"|"description"|"default_branch"'
    rm response.txt
else
    echo "❌ GitHub API connection failed with status code: $response"
    echo "Error response:"
    cat response.txt
    rm response.txt
    echo ""
    echo "Please check your token and GitHub Enterprise URL."
fi
