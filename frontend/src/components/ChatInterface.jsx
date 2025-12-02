import React, { useState, useEffect, useRef } from 'react';
import { Send, Loader2, RefreshCw, Trash2, File, Clock } from 'lucide-react';
import { v4 as uuidv4 } from 'uuid';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import './ChatInterface.css';

const ChatInterface = () => {
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [sessionId, setSessionId] = useState(() => uuidv4());
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    // Load chat history when component mounts
    loadChatHistory();
  }, [sessionId]);

  const loadChatHistory = async () => {
    try {
      const response = await fetch(`/api/chat/history/${sessionId}`);
      if (response.ok) {
        const history = await response.json();
        const formattedMessages = [];
        
        history.forEach(msg => {
          formattedMessages.push({
            id: `user-${msg.id}`,
            type: 'user',
            content: msg.userMessage,
            timestamp: new Date(msg.createdAt)
          });
          formattedMessages.push({
            id: `bot-${msg.id}`,
            type: 'bot',
            content: msg.botResponse,
            timestamp: new Date(msg.createdAt),
            responseTime: msg.responseTimeMs,
            sourceFiles: msg.contextFiles ? msg.contextFiles.split(',').filter(f => f) : [],
            modelUsed: msg.modelUsed
          });
        });
        
        setMessages(formattedMessages);
      }
    } catch (error) {
      console.error('Failed to load chat history:', error);
    }
  };

  const sendMessage = async () => {
    if (!inputMessage.trim() || isLoading) return;

    const userMessage = {
      id: uuidv4(),
      type: 'user',
      content: inputMessage,
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMessage]);
    setInputMessage('');
    setIsLoading(true);

    try {
      // Create AbortController with 5-minute timeout
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 300000); // 5 minutes

      const response = await fetch('/api/chat/message', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        signal: controller.signal,
        body: JSON.stringify({
          message: inputMessage,
          sessionId: sessionId,
          includeContext: true
        }),
      });

      clearTimeout(timeoutId);

      const data = await response.json();

      if (data.success) {
        const botMessage = {
          id: uuidv4(),
          type: 'bot',
          content: data.response,
          timestamp: new Date(),
          responseTime: data.responseTimeMs,
          sourceFiles: data.sourceFiles || [],
          modelUsed: data.modelUsed
        };
        setMessages(prev => [...prev, botMessage]);
      } else {
        throw new Error(data.errorMessage || 'Failed to get response');
      }
    } catch (error) {
      console.error('Failed to send message:', error);
      
      let errorContent = 'I apologize, but I encountered an error while processing your request. Please try again.';
      
      if (error.name === 'AbortError') {
        errorContent = 'The request timed out due to lengthy processing. The response might still be processing in the background. Please try asking for shorter segments or wait a moment before retrying.';
      }
      
      const errorMessage = {
        id: uuidv4(),
        type: 'bot',
        content: errorContent,
        timestamp: new Date(),
        isError: true
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  const regenerateResponse = async () => {
    if (isLoading) return;

    setIsLoading(true);
    try {
      const response = await fetch(`/api/chat/regenerate/${sessionId}`, {
        method: 'POST'
      });

      const data = await response.json();

      if (data.success) {
        // Remove the last bot message and add the new one
        setMessages(prev => {
          const newMessages = [...prev];
          if (newMessages.length > 0 && newMessages[newMessages.length - 1].type === 'bot') {
            newMessages.pop();
          }
          
          return [...newMessages, {
            id: uuidv4(),
            type: 'bot',
            content: data.response,
            timestamp: new Date(),
            responseTime: data.responseTimeMs,
            sourceFiles: data.sourceFiles || [],
            modelUsed: data.modelUsed
          }];
        });
      } else {
        throw new Error(data.errorMessage || 'Failed to regenerate response');
      }
    } catch (error) {
      console.error('Failed to regenerate response:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const clearChat = async () => {
    try {
      await fetch(`/api/chat/history/${sessionId}`, {
        method: 'DELETE'
      });
      setMessages([]);
      setSessionId(uuidv4());
    } catch (error) {
      console.error('Failed to clear chat:', error);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const formatTimestamp = (timestamp) => {
    return timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  // Helper function to detect internal file links
  const isInternalFileLink = (href) => {
    if (!href) return false;
    
    // Check for GitHub internal repository links
    if (href.includes('github.ibm.com/maximo-application-suite/knowledge-center') ||
        href.includes('/blob/main/') ||
        href.includes('/tree/main/')) {
      return true;
    }
    
    // Check for common internal file patterns
    const internalPatterns = [
      /\.md$/i,           // Markdown files
      /\.java$/i,         // Java files
      /\.xml$/i,          // XML files
      /\.yaml$/i,         // YAML files
      /\.yml$/i,          // YML files
      /\.properties$/i,   // Properties files
      /\.sh$/i,           // Shell scripts
      /\.json$/i,         // JSON files
      /^[A-Z][a-zA-Z0-9-]*$/,  // CamelCase file names (like Java Home, Maven Home)
      /^[a-z-]+$/,             // kebab-case file names
    ];
    
    // Check if it's not a full external URL
    if (href.startsWith('http://') || href.startsWith('https://')) {
      // But if it's a GitHub repository link to our repos, treat as internal
      return href.includes('github.ibm.com/maximo-application-suite/knowledge-center') ||
             href.includes('github.ibm.com/maximo-application-suite/mas-suite-install') ||
             href.includes('github.ibm.com/maximo-application-suite/mas-manage-install') ||
             href.includes('MaxRenewAutomate');
    }
    
    // Check against patterns
    return internalPatterns.some(pattern => pattern.test(href)) || 
           href.includes('/') || // Path-like structure
           !href.includes('.'); // No extension but could be a file reference
  };

  // Handle internal file link clicks
  const handleInternalLinkClick = async (href, linkText) => {
    if (isLoading) return;
    
    // Create a query to fetch the linked file content
    let query = '';
    let displayName = '';
    
    // Handle GitHub repository URLs
    if (href.includes('github.ibm.com') && href.includes('/blob/main/')) {
      // Extract file path from GitHub URL
      // Example: https://github.ibm.com/maximo-application-suite/knowledge-center/blob/main/devops/db2/01-Setup.md
      const pathMatch = href.match(/\/blob\/main\/(.+)$/);
      if (pathMatch) {
        const filePath = pathMatch[1];
        // Use just the filename for both display and query
        const filename = filePath.split('/').pop();
        displayName = filename; // Keep full filename with extension for display
        query = filename; // Keep full filename with extension for search too
      }
    } else if (href.includes('/')) {
      // If it's a path, use the filename
      const filename = href.split('/').pop();
      displayName = filename || href;
      query = filename || href;
    } else {
      // Use the link text or href as the query
      const rawQuery = (Array.isArray(linkText) ? linkText.join('') : linkText) || href;
      displayName = rawQuery;
      query = rawQuery;
    }
    
    // Clean up the query for searching but preserve dots for file extensions
    query = query.replace(/[^\w\s\-\.]/g, ' ').trim();
    
    // Add user message for the link click
    const userMessage = {
      id: uuidv4(),
      type: 'user',
      content: `📎 Show content from: ${displayName}`,
      timestamp: new Date(),
      isLinkClick: true
    };
    
    setMessages(prev => [...prev, userMessage]);
    setIsLoading(true);
    
    try {
      const response = await fetch('/api/chat/message', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          message: query,
          conversationId: sessionId
        }),
      });

      const data = await response.json();

      if (data.success) {
        const botMessage = {
          id: uuidv4(),
          type: 'bot',
          content: data.response,
          timestamp: new Date(),
          responseTime: data.responseTimeMs,
          sourceFiles: data.sourceFiles || [],
          modelUsed: data.modelUsed,
          isLinkResponse: true
        };
        setMessages(prev => [...prev, botMessage]);
      } else {
        throw new Error(data.errorMessage || 'Failed to get linked content');
      }
    } catch (error) {
      console.error('Failed to fetch linked content:', error);
      const errorMessage = {
        id: uuidv4(),
        type: 'bot',
        content: `Sorry, I couldn't fetch the content from "${query}". Please try searching for it directly.`,
        timestamp: new Date(),
        isError: true
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="chat-interface">
      <div className="chat-header">
        <h2>Chat with AI Assistant</h2>
        <div className="chat-controls">
          <button
            onClick={clearChat}
            className="control-button"
            title="Clear Chat"
          >
            <Trash2 size={18} />
          </button>
        </div>
      </div>

      <div className="chat-messages">
        {messages.length === 0 ? (
          <div className="welcome-message">
            <h3>Welcome to the AI GitHub Knowledge Assistant!</h3>
            <p>I can help you understand and work with the repository content. Ask me anything about the codebase, documentation, or specific files.</p>
            <div className="example-questions">
              <h4>Example questions:</h4>
              <ul>
                <li>"What is this repository about?"</li>
                <li>"How do I get started with this project?"</li>
                <li>"Explain the main components of the system"</li>
                <li>"What are the configuration options?"</li>
              </ul>
            </div>
          </div>
        ) : (
          messages.map((message) => (
            <div key={message.id} className={`message ${message.type}`}>
              <div className="message-content">
                {message.type === 'user' ? (
                  <div className="user-message">
                    {message.content}
                  </div>
                ) : (
                  <div className="bot-message">
                    <ReactMarkdown 
                      remarkPlugins={[remarkGfm]}
                      components={{
                        table: ({ children }) => (
                          <table className="markdown-table">{children}</table>
                        ),
                        thead: ({ children }) => (
                          <thead className="markdown-thead">{children}</thead>
                        ),
                        tbody: ({ children }) => (
                          <tbody className="markdown-tbody">{children}</tbody>
                        ),
                        tr: ({ children }) => (
                          <tr className="markdown-tr">{children}</tr>
                        ),
                        th: ({ children }) => (
                          <th className="markdown-th">{children}</th>
                        ),
                        td: ({ children }) => (
                          <td className="markdown-td">{children}</td>
                        ),
                        p: ({ children }) => {
                          // If the paragraph contains only code block elements, render as div instead
                          const hasOnlyCodeBlock = React.Children.toArray(children).every(child => 
                            (React.isValidElement(child) && child.type === 'pre') ||
                            (typeof child === 'string' && child.trim() === '')
                          );
                          
                          if (hasOnlyCodeBlock) {
                            return <div className="markdown-container">{children}</div>;
                          }
                          return <p>{children}</p>;
                        },
                        pre: ({ children }) => {
                          // Get the text content from the code block
                          const textContent = React.Children.toArray(children)
                            .map(child => {
                              if (React.isValidElement(child) && child.props && child.props.children) {
                                return child.props.children;
                              }
                              return child;
                            })
                            .join('');

                          if (typeof textContent === 'string') {
                            const lines = textContent.split('\n');
                            
                            // Improved command detection - look for shell commands at start of lines
                            const commandPatterns = [
                              /^\s*mkdir\s/i,
                              /^\s*cd\s/i,
                              /^\s*wget\s/i,
                              /^\s*curl\s/i,
                              /^\s*tar\s/i,
                              /^\s*extract\s/i,
                              /^\s*mv\s/i,
                              /^\s*cp\s/i,
                              /^\s*rm\s/i,
                              /^\s*ls\s/i,
                              /^\s*cat\s/i,
                              /^\s*echo\s/i,
                              /^\s*export\s/i,
                              /^\s*source\s/i,
                              /^\s*sudo\s/i,
                              /^\s*crc\s/i,
                              /^\s*oc\s/i,
                              /^\s*java\s/i,
                              /^\s*mvn\s/i,
                              /^\s*npm\s/i,
                              /^\s*yarn\s/i,
                              /^\s*docker\s/i,
                              /^\s*kubectl\s/i,
                              /^\s*git\s/i,
                              /^\s*chmod\s/i,
                              /^\s*chown\s/i,
                              /^\s*systemctl\s/i,
                              /^\s*service\s/i,
                              /^\s*\.\//i
                            ];
                            
                            const isCommand = (line) => {
                              const trimmed = line.trim();
                              return commandPatterns.some(pattern => pattern.test(trimmed));
                            };
                            
                            const commandLines = lines.filter(line => 
                              line.trim() && isCommand(line)
                            );
                            
                            // If we have multiple command lines, split them
                            if (commandLines.length > 1) {
                              return (
                                <div className="multi-code-blocks">
                                  {lines.map((line, index) => {
                                    const trimmedLine = line.trim();
                                    if (trimmedLine) {
                                      // Check if this line is a command
                                      if (isCommand(line)) {
                                        return (
                                          <pre key={index} className="code-block">
                                            <code>{trimmedLine}</code>
                                          </pre>
                                        );
                                      } else {
                                        // Non-command lines (comments, output, etc.)
                                        return (
                                          <div key={index} className="command-comment">
                                            {trimmedLine}
                                          </div>
                                        );
                                      }
                                    }
                                    return null;
                                  })}
                                </div>
                              );
                            }
                          }
                          
                          return <pre className="code-block">{children}</pre>;
                        },
                        code: ({ node, inline, className, children, ...props }) => {
                          const match = /language-(\w+)/.exec(className || '');
                          return inline ? (
                            <code className="inline-code" {...props}>
                              {children}
                            </code>
                          ) : (
                            <code className={className} {...props}>
                              {children}
                            </code>
                          );
                        },
                        a: ({ href, children, ...props }) => {
                          // Handle missing href
                          if (!href) {
                            return <span {...props}>{children}</span>;
                          }
                          
                          // Check if this is an internal repository link first
                          if (isInternalFileLink(href)) {
                            return (
                              <span 
                                className="internal-file-link"
                                onClick={(e) => {
                                  e.preventDefault();
                                  e.stopPropagation();
                                  handleInternalLinkClick(href, children);
                                }}
                                title={`Click to view content from: ${href}`}
                                {...props}
                              >
                                {children}
                              </span>
                            );
                          }
                          
                          // Check if this is an external link (http/https) but NOT our repositories
                          if (href.startsWith('http://') || href.startsWith('https://')) {
                            // Fix common URL formatting issues
                            const cleanHref = href.replace(/\[|\]/g, ''); // Remove brackets like github.ibm.[com
                            
                            return (
                              <a 
                                href={cleanHref} 
                                target="_blank" 
                                rel="noopener noreferrer" 
                                onClick={(e) => {
                                  e.preventDefault();
                                  try {
                                    window.open(cleanHref, '_blank', 'noopener,noreferrer');
                                  } catch (error) {
                                    console.error('Failed to open URL:', cleanHref, error);
                                    // Fallback: try to navigate directly
                                    window.open(cleanHref, '_blank');
                                  }
                                }}
                                {...props}
                              >
                                {children}
                              </a>
                            );
                          }
                          
                          // For any other links, treat as external and make them absolute
                          const absoluteHref = href.startsWith('//') ? `https:${href}` : 
                                               href.startsWith('/') ? `https://github.com${href}` : 
                                               href.includes('://') ? href : `https://${href}`;
                          
                          // Clean the URL to fix formatting issues
                          const cleanAbsoluteHref = absoluteHref.replace(/\[|\]/g, '');
                          
                          return (
                            <a 
                              href={cleanAbsoluteHref} 
                              target="_blank" 
                              rel="noopener noreferrer"
                              onClick={(e) => {
                                e.preventDefault();
                                try {
                                  window.open(cleanAbsoluteHref, '_blank', 'noopener,noreferrer');
                                } catch (error) {
                                  console.error('Failed to open URL:', cleanAbsoluteHref, error);
                                }
                              }}
                              {...props}
                            >
                              {children}
                            </a>
                          );
                        }
                      }}
                    >
                      {message.content}
                    </ReactMarkdown>
                    
                    <div className="message-meta">
                      <span className="timestamp">
                        <Clock size={12} />
                        {formatTimestamp(message.timestamp)}
                      </span>
                      {message.responseTime && (
                        <span className="response-time">
                          {message.responseTime}ms
                        </span>
                      )}
                      {message.modelUsed && (
                        <span className="model-used">
                          {message.modelUsed}
                        </span>
                      )}
                    </div>
                  </div>
                )}
              </div>
            </div>
          ))
        )}
        
        {isLoading && (
          <div className="message bot">
            <div className="message-content">
              <div className="bot-message loading">
                <Loader2 size={20} className="spinning" />
                Thinking<span className="loading-dots"></span>
              </div>
            </div>
          </div>
        )}
        
        <div ref={messagesEndRef} />
      </div>

      <div className="chat-input">
        <div className="input-container">
          <textarea
            ref={inputRef}
            value={inputMessage}
            onChange={(e) => setInputMessage(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Ask me anything about the repository..."
            className="message-input"
            rows="1"
          />
          <div className="input-actions">
            {messages.length > 0 && messages[messages.length - 1]?.type === 'bot' && (
              <button
                onClick={regenerateResponse}
                className="action-button"
                disabled={isLoading}
                title="Regenerate Response"
              >
                <RefreshCw size={18} />
              </button>
            )}
            <button
              onClick={sendMessage}
              className="send-button"
              disabled={!inputMessage.trim() || isLoading}
            >
              <Send size={18} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ChatInterface;
