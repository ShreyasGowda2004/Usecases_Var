#!/bin/bash

# Backup and restore script for UI themes
# Usage: ./theme-switcher.sh [carbon|original]

FRONTEND_DIR="/Users/shreyasgowda/Desktop/ai chatbot/frontend/src"

backup_original() {
    echo "Backing up original interface..."
    cp "$FRONTEND_DIR/App.jsx" "$FRONTEND_DIR/App_original.jsx" 2>/dev/null || true
    cp "$FRONTEND_DIR/App.css" "$FRONTEND_DIR/App_original.css" 2>/dev/null || true
    echo "Original interface backed up."
}

switch_to_carbon() {
    echo "Switching to IBM Carbon Design System..."
    backup_original
    
    # Update main.jsx to use Carbon
    sed -i '' 's/import App from .\/App.jsx/import CarbonApp from .\/CarbonApp.jsx/' "$FRONTEND_DIR/main.jsx"
    sed -i '' 's/<App \/>/<CarbonApp \/>/' "$FRONTEND_DIR/main.jsx"
    
    echo "✅ Switched to IBM Carbon Design System"
    echo "🌐 Features available:"
    echo "   • 4 IBM theme options (White, Gray 10, Gray 90, Gray 100)"
    echo "   • Official IBM Carbon components"
    echo "   • Maximo Application Suite styling"
    echo "   • Improved accessibility"
    echo "   • Responsive design"
    echo ""
    echo "🎨 Theme selector available in header (colored dots)"
}

switch_to_original() {
    echo "Switching to original interface..."
    
    # Restore original main.jsx
    sed -i '' 's/import CarbonApp from .\/CarbonApp.jsx/import App from .\/App.jsx/' "$FRONTEND_DIR/main.jsx"
    sed -i '' 's/<CarbonApp \/>/<App \/>/' "$FRONTEND_DIR/main.jsx"
    
    echo "✅ Switched back to original interface"
}

show_help() {
    echo "IBM Maximo AI Chatbot - Theme Switcher"
    echo ""
    echo "Usage: $0 [option]"
    echo ""
    echo "Options:"
    echo "  carbon     Switch to IBM Carbon Design System"
    echo "  original   Switch back to original interface"
    echo "  help       Show this help message"
    echo ""
    echo "Current interface files:"
    echo "  Carbon:    CarbonApp.jsx, CarbonChatInterface.jsx, CarbonAdminPanel.jsx"
    echo "  Original:  App.jsx, ChatInterface.jsx, AdminPanel.jsx"
}

case "$1" in
    "carbon")
        switch_to_carbon
        ;;
    "original")
        switch_to_original
        ;;
    "help"|"--help"|"-h")
        show_help
        ;;
    *)
        echo "❓ Current: IBM Carbon Design System active"
        echo ""
        show_help
        ;;
esac
