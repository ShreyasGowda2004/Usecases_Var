import React, { useState, useEffect } from 'react';
import ChatInterface from './components/ChatInterface';
import { MessageCircle } from 'lucide-react';
import './App.css';

function App() {
  const [systemStatus, setSystemStatus] = useState({ status: 'unknown' });

  useEffect(() => {
    // Check system health on startup
    fetch('/api/health')
      .then(res => res.json())
      .then(data => setSystemStatus(data))
      .catch(err => console.error('Health check failed:', err));
  }, []);

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-content">
          <h1 className="app-title">
            <MessageCircle size={24} />
            AI Chatbot - GitHub Knowledge Assistant
          </h1>
          <div className="header-controls">
            <div className={`status-indicator ${systemStatus.status?.toLowerCase()}`}>
              <span className="status-dot"></span>
              {systemStatus.status || 'Unknown'}
            </div>
          </div>
        </div>
      </header>

      <main className="app-main">
        <ChatInterface />
      </main>
    </div>
  );
}

export default App;
