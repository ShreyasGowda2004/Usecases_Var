import React, { useState, useRef, useEffect, useCallback, useImperativeHandle, useMemo } from 'react';
import {
  Button,
  Tile,
  Grid,
  Column,
  Loading,
  InlineNotification,
  SkeletonText,
  CodeSnippet,
  Link,
  Modal,
  Checkbox
} from '@carbon/react';
import {
  Send,
  User,
  Watsonx,
  Erase,
  StopFilledAlt,
  PlayFilledAlt,
  ChevronDown,
  ChevronUp
} from '@carbon/icons-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { v4 as uuidv4 } from 'uuid';
import './CarbonChatInterface.css';
import ExecutionConsole from './ExecutionConsole';
import { historyAPI } from '../utils/api';

// Helper to extract plain text from a react-markdown node's children
function getNodeText(node) {
  if (!node) return '';
  if (typeof node === 'string') return node;
  if (Array.isArray(node)) return node.map(getNodeText).join('');
  if (node.props && node.props.children) return getNodeText(node.props.children);
  if (node.children) return node.children.map(getNodeText).join('');
  return '';
}

// Welcome Screen Component
const WelcomeScreen = ({ onSampleClick, product = 'Maximo' }) => {
  const sampleQuestions = [
    "Give DB2 setup",
    "Create Company Master", 
    "Create Organization and Site",
    "Install OpenShift in local"
  ];

  return (
    <div className="welcome-screen">
      <div className="welcome-header">
        <div className="welcome-icon">
          <Watsonx size={32} />
        </div>
        <h1 className="welcome-title">Welcome to IBM {product} AI Assistant</h1>
        <p className="welcome-subtitle">
          Your intelligent assistant for IBM Maximo Application Suite. Get help with installation, configuration, database setup, and organizational management. Ask questions about DB2, OpenShift, company sets, and more.
        </p>
      </div>
      
      <div className="welcome-illustration">
        <svg viewBox="0 0 400 200" className="watson-illustration">
          {/* Central brain/AI node */}
          <circle cx="200" cy="100" r="40" fill="#4589ff" opacity="0.2" />
          <circle cx="200" cy="100" r="25" fill="#4589ff" />
          
          {/* Connected nodes */}
          <circle cx="120" cy="60" r="15" fill="#be95ff" />
          <circle cx="280" cy="60" r="15" fill="#33b1ff" />
          <circle cx="120" cy="140" r="15" fill="#ff8389" />
          <circle cx="280" cy="140" r="15" fill="#42be65" />
          <circle cx="160" cy="40" r="8" fill="#82cfff" />
          <circle cx="240" cy="40" r="8" fill="#d4bbff" />
          <circle cx="160" cy="160" r="8" fill="#ffb3ba" />
          <circle cx="240" cy="160" r="8" fill="#8dd3c7" />
          
          {/* Connection lines */}
          <line x1="200" y1="100" x2="120" y2="60" stroke="#4589ff" strokeWidth="2" opacity="0.6" />
          <line x1="200" y1="100" x2="280" y2="60" stroke="#4589ff" strokeWidth="2" opacity="0.6" />
          <line x1="200" y1="100" x2="120" y2="140" stroke="#4589ff" strokeWidth="2" opacity="0.6" />
          <line x1="200" y1="100" x2="280" y2="140" stroke="#4589ff" strokeWidth="2" opacity="0.6" />
          <line x1="120" y1="60" x2="160" y2="40" stroke="#be95ff" strokeWidth="1" opacity="0.5" />
          <line x1="280" y1="60" x2="240" y2="40" stroke="#33b1ff" strokeWidth="1" opacity="0.5" />
          <line x1="120" y1="140" x2="160" y2="160" stroke="#ff8389" strokeWidth="1" opacity="0.5" />
          <line x1="280" y1="140" x2="240" y2="160" stroke="#42be65" strokeWidth="1" opacity="0.5" />
        </svg>
      </div>

      <div className="sample-questions">
        <h3 className="sample-title">Sample questions</h3>
        <div className="question-grid">
          {sampleQuestions.map((question, index) => (
            <button
              key={index}
              className="sample-question-btn"
              onClick={() => onSampleClick(question)}
            >
              <span>{question}</span>
              <Send size={16} className="question-arrow" />
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

// Helper: reformat single-line Kubernetes/IBM YAML manifest into multiline fenced block
function reformatYamlIfNeeded(raw) {
  if (!raw) return raw;
  const trimmed = raw.trim();
  // If already fenced or has multiple lines, leave (unless it's a single compressed line)
  const hasNewlines = /\n/.test(trimmed);
  const hasFence = /^```/.test(trimmed);
  const looksLikeYaml = /apiVersion:\s*[^\s]+/i.test(trimmed) && /kind:\s*[^\s]+/i.test(trimmed);
  if (!looksLikeYaml) return raw;
  if (hasFence) return raw; // already formatted
  // If it already has proper newlines, just wrap in fence for code styling
  if (hasNewlines && /metadata:/.test(trimmed)) {
    return '```yaml\n' + trimmed + '\n```';
  }
  // Compressed single line -> insert newlines before known YAML keys
  const keys = ['apiVersion','kind','metadata','spec','features','license','installIBMCatalogSource','isDisconnected','deployment','meterDefinitionCatalogServer','registration','name','namespace','accept'];
  let reformatted = trimmed;
  // Ensure a space after colons already; then add newlines
  keys.forEach(k => {
    const re = new RegExp('(?<!^)\\s+' + k + ':(?=\\s|$)','g');
    reformatted = reformatted.replace(re, '\n' + k + ':');
  });
  // Indent nested structure heuristically
  // Basic indentation for blocks after metadata:, spec:, features:, license:
  reformatted = reformatted
    .replace(/metadata:\n([^\n]+)/,'metadata:\n  $1')
    .replace(/spec:\n([^\n]+)/,'spec:\n  $1')
    .replace(/features:\n([^\n]+)/,'features:\n    $1')
    .replace(/license:\n([^\n]+)/,'license:\n    $1');
  return '```yaml\n' + reformatted + '\n```';
}

// Helper: reformat compressed multi-command shell snippets into a proper bash code block
function reformatShellIfNeeded(raw) {
  if (!raw) return raw;
  const trimmed = raw.trim();
  const alreadyFenced = /^```/.test(trimmed);
  // Heuristic: treat as shell script if it has shebang or typical shell keywords
  const isShell = /#!\//.test(trimmed) || /(export\s+\w+=|\boc\s+|\bkubectl\s+|\bhelm\s+|chmod\s+\+x|^bash\s)/m.test(trimmed);
  if (!isShell) return raw;
  if (alreadyFenced) return raw; // don't double wrap

  // Normalize: break chained commands on ' && ' and ';'
  let script = trimmed
    .replace(/\s*&&\s*/g, ' && ') // standard spacing
    .replace(/;\s*/g, ';')
    .replace(/;(?=[^\n])/g, '\n')
    .replace(/ && /g, ' &&\n');

  // If the entire script is on one line but has multiple exports / oc / kubectl, split
  script = script
    .replace(/\s+(?=export\s+\w+=)/g, '\n')
    .replace(/\s+(?=oc\s+)/g, '\n')
    .replace(/\s+(?=kubectl\s+)/g, '\n');

  // Collapse more than two blank lines
  script = script.replace(/\n{3,}/g, '\n\n');

  // Trim each line
  script = script.split('\n').map(l => l.trimEnd()).join('\n');

  return '```bash\n' + script + '\n```';
}

// Helper: detect Java source and wrap in fenced code block if not already fenced
function reformatJavaIfNeeded(raw) {
  if (!raw) return raw;
  if (/```/.test(raw)) return raw; // already has fenced block
  // Quick signal that this is likely Java
  if (!/(public\s+class|class\s+\w+|package\s+[\w\.]+;)/.test(raw)) return raw;
  const lines = raw.split(/\r?\n/);
  const startIdx = lines.findIndex(l => /^(package\s+|import\s+|public\s+|class\s+)/.test(l.trim()));
  if (startIdx === -1) return raw;
  const before = lines.slice(0, startIdx).join('\n').trim();
  const code = lines.slice(startIdx).join('\n').trim();
  if (!code) return raw;
  if (before) return `${before}\n\n\`\`\`java\n${code}\n\`\`\``.replace(/`{3}/g,'```');
  return '```java\n' + code + '\n```';
}

const stripOrderedListPrefix = (value = '') => value.replace(/^\s*(?:\d+[\.\)]\s+|\(\d+\)\s+|\d+\s*[-–—]\s+|\d+:\s+)/, '');

const sanitizeTitleText = (value = '') => stripOrderedListPrefix(value
  .replace(/\*\*|__/g, '')
  .replace(/`/g, '')
  .replace(/\s+/g, ' ')
  .trim());

function deriveActionTitle(content = '', fallback = '') {
  const headings = [];
  const headingRe = /(^|\n)\s*(?:#{1,6}\s*|\d+[\.\)]\s+|\(\d+\)\s+|\d+\s*[-–—]\s+)([^\n]+)/g;
  let match;
  while ((match = headingRe.exec(content)) !== null) {
    const candidate = sanitizeTitleText(match[2]);
    if (candidate) headings.push(candidate);
  }

  const preferred = headings.find(title => /\b(create|add|update|delete|install|configure|set(?:\s|-)?up|setup|deploy|run|execute)\b/i.test(title));
  const fallbackClean = sanitizeTitleText(fallback);
  const picked = preferred || headings[0] || fallbackClean;
  if (picked) {
    const trimmed = picked.replace(/[:\-–—]+\s*$/, '').trim();
    if (trimmed) {
      return trimmed.length > 120 ? `${trimmed.slice(0, 117).trim()}...` : trimmed;
    }
  }

  const firstLine = sanitizeTitleText((content || '').split('\n').find(line => line.trim()) || '');
  if (firstLine) {
    return firstLine.length > 120 ? `${firstLine.slice(0, 117).trim()}...` : firstLine;
  }

  return 'Manual Request';
}

function buildActionTitleFromRecord(record) {
  if (!record) return 'Manual Request';
  if (record.actionTitle && record.actionTitle.trim()) {
    const cleaned = stripOrderedListPrefix(record.actionTitle.trim());
    return cleaned || 'Manual Request';
  }
  const method = ((record.method || '') + '').trim().toUpperCase();
  const rawUrl = record.url || '';
  if (rawUrl) {
    try {
      const parsed = new URL(rawUrl);
      const path = parsed.pathname.replace(/\/+$/, '') || '/';
      return method ? `${method} ${path}`.trim() : `${path}`;
    } catch {
      return method ? `${method} ${rawUrl}`.trim() : `${rawUrl}`;
    }
  }
  return method || 'Manual Request';
}

const EMPTY_REQUEST_PREFILL = { url: '', method: '', headers: [], params: [], body: '' };

function normalizeRequestPrefill(prefill = {}) {
  return {
    url: prefill.url || '',
    method: prefill.method || '',
    headers: Array.isArray(prefill.headers) ? prefill.headers.map(h => ({ ...h })) : [],
    params: Array.isArray(prefill.params) ? prefill.params.map(p => ({ ...p })) : [],
    body: prefill.body || ''
  };
}

function applyInstanceToRequest(prefill, instance) {
  const normalized = normalizeRequestPrefill(prefill);
  if (!instance) {
    return normalized;
  }

  const instanceUrl = instance.url || '';
  let origin = '';
  let host = '';
  if (instanceUrl) {
    try {
      const parsed = new URL(instanceUrl);
      origin = parsed.origin;
      host = parsed.host;
    } catch (err) {
      origin = instanceUrl;
      host = instanceUrl.replace(/^https?:\/\//i, '').split('/')[0];
    }
  }
  const hostLower = host ? host.toLowerCase() : '';
  const apiKey = instance.apiKey || '';

  const replacePlaceholders = (value, { isUrl = false } = {}) => {
    if (!value) return value;
    let result = value;
    if (origin) {
      result = result.replace(/https?:\/\/hostname/gi, origin);
    }
    if (host) {
      result = result.replace(/hostname/gi, (match, offset, str) => {
        // Avoid replacing the hostname portion we already handled with scheme replacement
        // If the substring already contains the host with scheme, skip.
        return host;
      });
    }
    if (apiKey) {
      result = result.replace(/<your-apikey-value>/gi, apiKey);
    }
    if (isUrl && origin) {
      if (!/^https?:\/\//i.test(result)) {
        if (hostLower && result.toLowerCase().startsWith(hostLower)) {
          const suffix = result.slice(host.length);
          const trimmedSuffix = suffix.startsWith('/') ? suffix : `/${suffix}`;
          result = origin.replace(/\/$/, '') + trimmedSuffix;
        } else {
          const trimmed = result.replace(/^\/+/, '');
          result = origin.replace(/\/$/, '') + (trimmed ? `/${trimmed}` : '');
        }
      }
    }
    return result;
  };

  const updatedHeaders = normalized.headers.map(header => ({
    ...header,
    key: replacePlaceholders(header.key),
    value: replacePlaceholders(header.value)
  }));

  const updatedParams = normalized.params.map(param => ({
    ...param,
    key: replacePlaceholders(param.key),
    value: replacePlaceholders(param.value)
  }));

  return {
    ...normalized,
    url: replacePlaceholders(normalized.url, { isUrl: true }),
    headers: updatedHeaders,
    params: updatedParams,
    body: replacePlaceholders(normalized.body)
  };
}

function requestsEqual(a, b) {
  if (a === b) return true;
  if (!a || !b) return false;
  if (a.url !== b.url) return false;
  if (a.method !== b.method) return false;
  if (a.body !== b.body) return false;
  const headersEqual = JSON.stringify(a.headers || []) === JSON.stringify(b.headers || []);
  const paramsEqual = JSON.stringify(a.params || []) === JSON.stringify(b.params || []);
  return headersEqual && paramsEqual;
}

// Memoized message component to avoid re-rendering all previous messages on each keystroke
const MessageComponent = React.memo(function MessageComponent({ message, isStreaming, onOpenExec, onOpenExecSection, onOpenGitFile, hidden, collapsed, onToggleCollapse, onRevealManual, prereqText, canExecute = false, assistantName = 'Maximo AI Assistant' }) {
  const isUser = message.type === 'user';
  const isError = message.isError;
  
  // Memoize markdown components to prevent CodeSnippet flickering
  const markdownComponents = useMemo(() => ({
    // Heading renderers (no inline execute buttons)
    h1: ({children}) => (
      <h3 style={{marginTop: '1.5rem', marginBottom: '0.5rem', fontWeight: 600}}>{children}</h3>
    ),
    h2: ({children}) => (
      <h4 style={{marginTop: '1rem', marginBottom: '0.5rem', fontWeight: 600}}>{children}</h4>
    ),
    h3: ({children}) => (
      <h5 style={{marginTop: '0.75rem', marginBottom: '0.25rem', fontWeight: 600}}>{children}</h5>
    ),
    // Paragraph renderer (no inline execute buttons)
    p({children, node, ...props}) {
      const hasCodeBlock = node && node.children && node.children.some(child =>
        child.type === 'element' && child.tagName === 'code' && !child.properties?.className?.includes('inline')
      );
      if (hasCodeBlock) return <div {...props}>{children}</div>;
      return <p {...props}>{children}</p>;
    },
    code({node, inline, className, children, ...props}) {
      const language = className ? className.replace('language-', '') : '';
      if (!inline) {
        // While streaming, use a simple <pre> block to prevent the CodeSnippet component
        // from re-rendering and glitching on every new word.
        if (isStreaming) {
          return (
            <pre className="streaming-code-block">
              <code>{String(children)}</code>
            </pre>
          );
        }
        // Once streaming is done, render the full Carbon component.
        return (
          <CodeSnippet
            type="multi"
            language={language}
            style={{ margin: '8px 0' }}
            {...props}
          >
            {String(children).replace(/\n$/, '')}
          </CodeSnippet>
        );
      }
      return (
        <code className={`inline-code ${className || ''}`.trim()} {...props}>
          {children}
        </code>
      );
    },
    pre({children}) { return <>{children}</>; },
    ul: ({children}) => <ul style={{paddingLeft: '1.5rem', marginBottom: '1rem'}}>{children}</ul>,
    ol: ({children}) => <ol style={{paddingLeft: '1.5rem', marginBottom: '1rem'}}>{children}</ol>,
    li: ({children}) => <li style={{marginBottom: '0.25rem'}}>{children}</li>,
    a: ({ href, children }) => {
      const url = href || '';
      const isGitHub = /^https?:\/\/(?:github\.com|github\.ibm\.com)\//i.test(url);
      const isBlobOrRaw = /\/(blob|raw)\//.test(url);
      if (isGitHub && isBlobOrRaw && onOpenGitFile) {
        return (
          <Link
            href={url}
            onClick={(e) => {
              e.preventDefault();
              onOpenGitFile(url);
            }}
          >
            {children}
          </Link>
        );
      }
      return (
        <Link href={url} target="_blank" rel="noopener noreferrer">{children}</Link>
      );
    }
  }), [isStreaming, onOpenGitFile]);
  
  
  // Check if this is the special loading placeholder message
  if (message.isLoading) {
    return (
      <div className="message-container assistant-message">
        <div className="message-header">
          <div className="message-avatar">
            <Watsonx size={20} />
          </div>
          <div className="message-info">
            <span className="message-sender">{assistantName}</span>
            <span className="message-timestamp">{message.timestamp.toLocaleTimeString()}</span>
          </div>
        </div>
        <div className="loading-inline-spinner" aria-live="polite" aria-busy>
          <div className="ring-spinner" role="status" aria-label="Loading" />
        </div>
      </div>
    );
  }

  return (
    <div className={`message-container ${isUser ? 'user-message' : 'assistant-message'} ${isStreaming ? 'streaming' : ''}`}>
      <div className="message-header">
        <div className="message-avatar">
          {isUser ? <User size={20} /> : <Watsonx size={20} />}
        </div>
        <div className="message-info">
          <span className="message-sender">{isUser ? 'You' : assistantName}</span>
          <span className="message-timestamp">{message.timestamp.toLocaleTimeString()}</span>
        </div>
      </div>
      <Tile className={`message-content ${isError ? 'error-message' : ''} ${isStreaming ? 'streaming-content' : ''}`}>
        {!isUser && (
          <div className="message-tools-bar">
            <button className="collapse-btn tools-icon-btn" title={collapsed ? 'Expand' : 'Collapse'} onClick={onToggleCollapse} aria-label={collapsed ? 'Expand response' : 'Collapse response'}>
              {collapsed ? <ChevronDown size={16} /> : <ChevronUp size={16} />}
            </button>
          </div>
        )}
        {isUser ? (
          <p>{message.content}</p>
        ) : (
          <div className="assistant-response">
            {hidden ? (
              <div>
                {prereqText && prereqText.trim() && (
                  <div className="inline-prereq" style={{ marginBottom: '0.5rem' }}>
                    <ReactMarkdown
                      remarkPlugins={[remarkGfm]}
                      components={{
                        a: ({ href, children }) => {
                          const url = href || '';
                          const isGitHub = /^https?:\/\/(?:github\.com|github\.ibm\.com)\//i.test(url);
                          const isBlobOrRaw = /\/(blob|raw)\//.test(url);
                          if (isGitHub && isBlobOrRaw && onOpenGitFile) {
                            return (
                              <Link
                                href={url}
                                onClick={(e) => {
                                  e.preventDefault();
                                  onOpenGitFile(url);
                                }}
                              >
                                {children}
                              </Link>
                            );
                          }
                          return (
                            <Link href={url} target="_blank" rel="noopener noreferrer">{children}</Link>
                          );
                        }
                      }}
                    >
                      {prereqText}
                    </ReactMarkdown>
                  </div>
                )}
                {canExecute && (
                  <div className="inline-choice" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                    <div style={{ color: 'var(--cds-text-secondary, #6f6f6f)' }}>
                      Choose how to proceed: Manual (show) or Automatic (execute)
                    </div>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <Button size="sm" kind="secondary" onClick={onRevealManual}>Manual</Button>
                      <Button size="sm" kind="primary" onClick={() => onOpenExec(message)} title={'Execute automatically'}>
                        Automatic
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            ) : collapsed ? (
              <div className="placeholder small" style={{ color: 'var(--cds-text-secondary, #6f6f6f)' }}>
                Response collapsed. Click ▸ to expand.
              </div>
            ) : (
            <div className="markdown-content">
              {canExecute && (
                <div className="inline-exec-toolbar" style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '0.25rem' }}>
                  <button
                    className="section-play-btn tools-icon-btn"
                    title="Execute this request"
                    aria-label="Execute this request"
                    onClick={() => onOpenExec && onOpenExec(message)}
                  >
                    <PlayFilledAlt size={16} />
                  </button>
                </div>
              )}
              <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                components={markdownComponents}
              >
                {message.content.replace(/\\u003c/g, '<').replace(/\\u003e/g, '>')}
              </ReactMarkdown>
            </div>
            )}
            {isStreaming && <div className="typing-cursor">▊</div>}
          </div>
        )}
      </Tile>
    </div>
  );
});

const CarbonChatInterface = React.forwardRef(function CarbonChatInterface({ authUser, instances = [], activeInstanceId = '', onActiveInstanceChange, product = 'Maximo' }, ref) {
  const storageUser = authUser?.username || 'guest';
  const storageKey = `chatCurrent:${storageUser}`;
  const historyKey = `chatHistory:${storageUser}`;
  const [messages, setMessages] = useState(() => {
    try {
      const raw = localStorage.getItem(storageKey);
      if (raw) {
        const parsed = JSON.parse(raw).map(m => ({...m, timestamp: new Date(m.timestamp)}));
        if (parsed.length) return parsed;
      }
    } catch (e) { /* ignore */ }
    return [];
  });
  
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamingMessageId, setStreamingMessageId] = useState(null);
  const [error, setError] = useState(null);
  const [showExec, setShowExec] = useState(false);
  const [execBaseRequest, setExecBaseRequest] = useState(EMPTY_REQUEST_PREFILL);
  const [execResolvedRequest, setExecResolvedRequest] = useState(EMPTY_REQUEST_PREFILL);
  const [consoleInstanceId, setConsoleInstanceId] = useState(activeInstanceId || '');
  const [execContext, setExecContext] = useState({
    actionTitle: 'Manual Request',
    source: 'console',
    username: storageUser,
    instanceId: activeInstanceId || ''
  });
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);
  const cancelStreamRef = useRef(false);
  const abortControllerRef = useRef(null);
  const [showClearConfirm, setShowClearConfirm] = useState(false);
  const [skipClearConfirm, setSkipClearConfirm] = useState(() => {
    try { return localStorage.getItem('clearConfirmSkip') === 'true'; } catch { return false; }
  });
  // Show welcome screen only if there are no persisted messages; this avoids resetting to welcome
  // when theme toggles or settings close cause a re-render/remount.
  const [showWelcome, setShowWelcome] = useState(() => messages.length === 0);

  // GitHub file preview modal state
  const [gitModal, setGitModal] = useState({
    open: false,
    loading: false,
    error: null,
    title: '',
    content: '',
    meta: null,
  });

  const assistantName = `${product} AI Assistant`;

  // No automatic execution flow; manual console only

  // Hide/Collapse state per message and post-exec prompt
  const [hiddenMessageIds, setHiddenMessageIds] = useState({});
  const [collapsedMessageIds, setCollapsedMessageIds] = useState({});
  const [prereqById, setPrereqById] = useState({});

  useEffect(() => {
    if (!instances.length) {
      setConsoleInstanceId('');
      return;
    }
    if (activeInstanceId && instances.some(inst => inst.id === activeInstanceId)) {
      setConsoleInstanceId(activeInstanceId);
      return;
    }
    setConsoleInstanceId(prev => {
      if (prev === '' || instances.some(inst => inst.id === prev)) {
        return prev;
      }
      return instances[0].id;
    });
  }, [instances, activeInstanceId]);

  useEffect(() => {
    if (!showExec) return;
    const instance = instances.find(inst => inst.id === consoleInstanceId);
    const nextResolved = applyInstanceToRequest(execBaseRequest, instance);
    setExecResolvedRequest(prev => (requestsEqual(prev, nextResolved) ? prev : nextResolved));
  }, [consoleInstanceId, instances, execBaseRequest, showExec]);

  useEffect(() => {
    setExecContext(prev => ({ ...prev, username: storageUser }));
  }, [storageUser]);

  useEffect(() => {
    setExecContext(prev => ({ ...prev, instanceId: activeInstanceId || '' }));
  }, [activeInstanceId]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const resolveInstanceId = useCallback((preferredId) => {
    const valid = (id) => !!id && instances.some(inst => inst.id === id);
    if (preferredId !== undefined) {
      if (preferredId === '' || valid(preferredId)) {
        return preferredId;
      }
    }
    if (consoleInstanceId === '' || valid(consoleInstanceId)) return consoleInstanceId;
    if (valid(activeInstanceId)) return activeInstanceId;
    return instances[0]?.id || '';
  }, [instances, consoleInstanceId, activeInstanceId]);

  const openConsoleWithRequest = useCallback((rawRequest, preferredInstanceId, meta = {}) => {
    const base = normalizeRequestPrefill(rawRequest || {});
    const desiredInstanceId = meta.instanceId !== undefined ? meta.instanceId : preferredInstanceId;
    const instanceId = resolveInstanceId(desiredInstanceId);
    const instance = instances.find(inst => inst.id === instanceId) || null;
    setExecBaseRequest(base);
    setConsoleInstanceId(instanceId);
    setExecContext({
      actionTitle: meta.actionTitle || (rawRequest && rawRequest.actionTitle) || 'Manual Request',
      source: meta.source || (rawRequest && rawRequest.source) || 'console',
      username: meta.username || storageUser,
      instanceId
    });
    const resolved = applyInstanceToRequest(base, instance);
    setExecResolvedRequest(resolved);
    setShowExec(true);
  }, [instances, resolveInstanceId, storageUser]);

  const rerunExecution = useCallback((record) => {
    if (!record) return;
    const headers = Array.isArray(record.requestHeaders)
      ? record.requestHeaders.map(({ key, value }) => ({ key: key || '', value: value || '' }))
      : [];
    const params = Array.isArray(record.requestParams)
      ? record.requestParams.map(({ key, value }) => ({ key: key || '', value: value || '' }))
      : [];
    const prefill = {
      url: record.url || '',
      method: record.method || '',
      headers,
      params,
      body: record.requestBody || ''
    };
    const actionTitle = buildActionTitleFromRecord(record);
    const meta = {
      actionTitle,
      source: record.source || 'history',
      username: record.username || storageUser,
      instanceId: record.instanceId ?? ''
    };
    openConsoleWithRequest(prefill, meta.instanceId, meta);
  }, [openConsoleWithRequest, storageUser]);

  useImperativeHandle(ref, () => ({
    openExecutionConsoleFromHistory: (record) => rerunExecution(record),
    openExecutionConsoleWithRequest: (request, preferredInstanceId, meta = {}) => openConsoleWithRequest(request, preferredInstanceId, meta)
  }), [rerunExecution, openConsoleWithRequest]);

  const closeExecutionConsole = useCallback(() => {
    setShowExec(false);
    setExecBaseRequest(EMPTY_REQUEST_PREFILL);
    setExecResolvedRequest(EMPTY_REQUEST_PREFILL);
    setExecContext({
      actionTitle: 'Manual Request',
      source: 'console',
      username: storageUser,
      instanceId: activeInstanceId || ''
    });
  }, [activeInstanceId, storageUser]);

  const handleConsoleInstanceChange = useCallback((nextId) => {
    const instance = instances.find(inst => inst.id === nextId) || null;
    setConsoleInstanceId(nextId);
    setExecResolvedRequest(prev => {
      const updated = applyInstanceToRequest(execBaseRequest, instance);
      return requestsEqual(prev, updated) ? prev : updated;
    });
    // Intentionally do NOT propagate to parent here to avoid console closing
    // Changing the global active instance can cause parent to remount and close the modal
  }, [instances, execBaseRequest]);

  useEffect(() => {
    // Persist current session messages per-user
    try {
      localStorage.setItem(storageKey, JSON.stringify(messages));
    } catch (e) { /* ignore quota errors */ }
    scrollToBottom();
  }, [messages, storageKey]);

  const sendMessage = async () => {
    if (showWelcome) {
      setShowWelcome(false);
    }
    
    if (!inputMessage.trim() || isLoading) return;

    const userMessage = {
      id: uuidv4(),
      type: 'user',
      content: inputMessage.trim(),
      timestamp: new Date(),
      sourceFiles: []
    };

    setMessages(prev => [...prev, userMessage]);
    setInputMessage('');
    setIsLoading(true);
    setError(null);
    cancelStreamRef.current = false;
    abortControllerRef.current = new AbortController();

    const assistantMessageId = uuidv4();
    const loadingMessage = {
      id: assistantMessageId,
      type: 'assistant',
      isLoading: true,
      timestamp: new Date(),
      fullContent: true // anticipate full content mode
    };
    setMessages(prev => [...prev, loadingMessage]);

    // Intercept greetings to always reply in the same Watsonx-like format
    const isGreeting = /^(hi|hello|hey|howdy|hi there|good (morning|afternoon|evening))\b/i.test(
      userMessage.content.trim()
    );

    try {
      let sourceFiles = [];

      if (isGreeting) {
        // Handle greetings with immediate streaming
        const greetingText = `# Hello\n\n---\n\nI'm here to help with any questions or topics you'd like to discuss. How can I assist you today?`;
        
        // Start streaming immediately
        setIsLoading(false);
        setMessages(prev => prev.map(msg => 
          msg.id === assistantMessageId 
            ? { 
                ...msg, 
                isLoading: false,
                content: '',
                sourceFiles: []
              }
            : msg
        ));
        setIsStreaming(true);
        setStreamingMessageId(assistantMessageId);
        await streamResponse(greetingText, assistantMessageId, []);
      } else {
        // Make the API call
        const response = await fetch('/api/chat/message', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            message: userMessage.content,
            sessionId: 'web-session-' + Date.now(),
            includeContext: true,
            fastMode: true,
            fullContent: true
          }),
          signal: abortControllerRef.current.signal,
        });

        if (response.ok) {
          // Start streaming immediately when response starts
          setIsLoading(false);
          setMessages(prev => prev.map(msg => 
            msg.id === assistantMessageId 
              ? { 
                  ...msg, 
                  isLoading: false,
                  content: '',
                  sourceFiles: []
                }
              : msg
          ));
          setIsStreaming(true);
          setStreamingMessageId(assistantMessageId);

          // Process response in real-time chunks; hidden/prereq set inside
          await streamResponseFromFetch(response, assistantMessageId);
        } else {
          // Handle error with streaming
          const errorText = 'Backend service unavailable. Please try again later.';
          setIsLoading(false);
          setMessages(prev => prev.map(msg => 
            msg.id === assistantMessageId 
              ? { 
                  ...msg, 
                  isLoading: false,
                  content: '',
                  sourceFiles: []
                }
              : msg
          ));
          setIsStreaming(true);
          setStreamingMessageId(assistantMessageId);
          await streamResponse(errorText, assistantMessageId, []);
          setPrereqById(prev => ({ ...prev, [assistantMessageId]: '' }));
        }
      }

    } catch (error) {
      if (error.name === 'AbortError') {
        console.log('Fetch aborted by user.');
        // When fetch is aborted, remove the loading placeholder message
        setMessages(prev => prev.filter(msg => msg.id !== assistantMessageId));
      } else {
        console.log('Backend not available or other error:', error);
        const responseText = 'Unable to connect to backend service. Please check your connection and try again.';
        
        // Use streaming for error messages too to ensure consistency
        setMessages(prev => prev.map(msg => 
          msg.id === assistantMessageId 
            ? { 
                ...msg, 
                isLoading: false,
                content: '',
                isError: true,
                sourceFiles: []
              }
            : msg
        ));
        setIsStreaming(true);
        setStreamingMessageId(assistantMessageId);
        await streamResponse(responseText, assistantMessageId, []);
      }
    } finally {
      setIsStreaming(false);
      setStreamingMessageId(null);
      setIsLoading(false);
      abortControllerRef.current = null;
      inputRef.current?.focus();
      // Don't auto-save here - let user decide when to save
    }
  };

  const stopStreaming = () => {
    cancelStreamRef.current = true;
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    setIsStreaming(false);
    setStreamingMessageId(null);
    setIsLoading(false);
    
    // Remove any loading messages that might be stuck
    setMessages(prev => prev.filter(msg => !msg.isLoading));
  };

  // Open GitHub file modal and fetch content via backend
  const openGitFileModal = useCallback(async (fileUrl) => {
    setGitModal({ open: true, loading: true, error: null, title: 'Loading…', content: '', meta: null });
    try {
      const resp = await fetch(`/api/github/file?url=${encodeURIComponent(fileUrl)}`);
      const data = await resp.json().catch(() => ({}));
      if (!resp.ok) {
        throw new Error(data && data.error ? data.error : `Failed to load file (status ${resp.status})`);
      }
      const title = data.name || data.path || 'GitHub File';
      setGitModal({
        open: true,
        loading: false,
        error: null,
        title,
        content: data.content || '',
        meta: { path: data.path, repository: data.repository, branch: data.branch },
      });
    } catch (e) {
      setGitModal({ open: true, loading: false, error: e.message || 'Failed to fetch file', title: 'Error', content: '', meta: null });
    }
  }, []);

  // Helper: extract request details from assistant message content
  const extractRequestFromContent = (text) => {
    // Wrapper that prefers a "Create" section; used for top-level play button
    let searchText = text || '';
    let createSectionText = '';
    let prereqText = '';
    try {
      // Find all headings and mark sections
      const headingRe = /(^|\n)\s*(?:#{1,6}\s*|\d+[\.\)]\s+|\(\d+\)\s+|\d+\s*[-–—:]\s+)([^\n]+)/gi;
      let createStart = -1, createEnd = -1; let match;
      const headings = [];
      while ((match = headingRe.exec(text)) !== null) {
        const cleaned = sanitizeTitleText(match[2] || '');
        const title = cleaned.toLowerCase();
        headings.push({ start: match.index + (match[1] ? match[1].length : 0), title });
      }
      
      // Look for a "Create..." section first
      for (let i = 0; i < headings.length; i++) {
        const t = headings[i].title;
        if (t.includes('create')) {
          createStart = headings[i].start;
          createEnd = (i + 1 < headings.length) ? headings[i + 1].start : text.length;
          createSectionText = text.slice(createStart, createEnd);
          break;
        }
      }

  // Extract prerequisites section if present (plural/singular and common variants)
  const preIdx = headings.findIndex(h => /\b(prerequisite|prerequisites|requirements|before you begin|before starting|prereq)\b/i.test(h.title));
      if (preIdx !== -1) {
        const start = headings[preIdx].start;
        const end = preIdx + 1 < headings.length ? headings[preIdx + 1].start : text.length;
        prereqText = text.slice(start, end).trim();
      }
    } catch {}

    // Use createSectionText first if available, otherwise use full text for all searches
    const primaryText = createSectionText || text;
    const fullText = text; // Always keep full text as backup

    return extractRequestFromPrimaryAndFull(primaryText, fullText, prereqText);
  };

  // Parse API details given a primary text slice (e.g., a section) and the full message text
  const extractRequestFromPrimaryAndFull = (primaryText, fullText, prereqText = '') => {
    const sanitizeForLabels = (input = '') => (
      (input || '')
        .replace(/\*\*/g, '')
        .replace(/__+/g, '')
        .replace(/(^|\n)\s*[-*•]+\s*/g, '$1')
        .replace(/(^|\n)\s*>+\s*/g, '$1')
    );

    const primaryLabelText = sanitizeForLabels(primaryText);
    const fullLabelText = sanitizeForLabels(fullText);

  // First try to find method in the create section, then in the full text if needed
  let method = '';
    const findMethodFromBlock = (text) => {
      if (!text) return null;
      // This regex looks for "Method", then lazily matches any characters (including newlines)
      // until it finds a valid HTTP verb. This is robust against missing colons, extra newlines, etc.
      const methodMatch = text.match(
        /(?:Method|HTTP\s*Method)\s*[:=\-–—]?\s*[\s\S]*?\b(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\b/i
      );
      return methodMatch ? methodMatch[1].toUpperCase() : null;
    };
    
    const detectedMethod =
      findMethodFromBlock(primaryText) ||
      findMethodFromBlock(fullText) ||
      findMethodFromBlock(primaryLabelText) ||
      findMethodFromBlock(fullLabelText);
  const hasLabeledMethod = !!detectedMethod;
    if (detectedMethod) method = detectedMethod;

    // First identify and exclude prerequisites sections from URL, header, param, and body extraction
    let nonPrereqText = fullText;
    try {
      const prereqRe = /(^|\n)\s*(?:#{1,6}\s*|\d+[\.\)]\s+|\(\d+\)\s+|\d+\s*[-–—:]\s+)?(?:prerequisite|prerequisites|requirements|before you begin|before starting|prereq)\b[^\n]*\n([\s\S]*?)(?=(\n\s*(?:#{1,6}\s*|\d+[\.\)]\s+|\(\d+\)\s+|\d+\s*[-–—:]\s+)[^\n]+\n)|$)/gi;
      nonPrereqText = fullText.replace(prereqRe, '');
    } catch (e) {
      console.error("Error filtering prerequisites:", e);
    }
    
    // Also filter prerequisites from primary text if it exists
    let nonPrereqPrimaryText = primaryText;
    try {
      const prereqRe = /(^|\n)\s*(?:#{1,6}\s*|\d+[\.\)]\s+|\(\d+\)\s+|\d+\s*[-–—:]\s+)?(?:prerequisite|prerequisites|requirements|before you begin|before starting|prereq)\b[^\n]*\n([\s\S]*?)(?=(\n\s*(?:#{1,6}\s*|\d+[\.\)]\s+|\(\d+\)\s+|\d+\s*[-–—:]\s+)[^\n]+\n)|$)/gi;
      nonPrereqPrimaryText = primaryText.replace(prereqRe, '');
    } catch (e) {
      console.error("Error filtering prerequisites from primary:", e);
    }
    
    // For URL extraction, prioritize: 1. Non-prereq primary section, 2. Non-prereq full text
    let url = '';
    // First look for labeled URL in the non-prereq primary (section) text
    const matchLabeledUrl = (text) => text ? (
      text.match(/\s*(?:URL|Endpoint|Request\s*URL|API\s*URL)\s*[:=\-–—]\s*(?:\r?\n\s*)?((?:https?:\/\/\S+)|\/[^\s]+)/im)
    ) : null;
    const labeledUrlInCreate = matchLabeledUrl(nonPrereqPrimaryText) || matchLabeledUrl(sanitizeForLabels(nonPrereqPrimaryText));
    if (labeledUrlInCreate) url = labeledUrlInCreate[1];
    
    // If not found, try non-prereq full text
    if (!url) {
      const labeledUrlNonPrereq = matchLabeledUrl(nonPrereqText) || matchLabeledUrl(sanitizeForLabels(nonPrereqText));
      if (labeledUrlNonPrereq) url = labeledUrlNonPrereq[1];
    }
    
    // If still not found, look for method+URL pattern in the non-prereq primary section
    if (!url && nonPrereqPrimaryText) {
      const methodAndUrlCreate = nonPrereqPrimaryText.match(/\b(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\b\s+((?:https?:\/\/[^\s"'()`<>]+)|\/[^\s"'()`<>]+)/i);
      if (methodAndUrlCreate) {
        if (!hasLabeledMethod) method = methodAndUrlCreate[1].toUpperCase();
        url = methodAndUrlCreate[2];
      }
    }
    
    // If still not found, try method+URL in non-prereq full text
    if (!url) {
      const methodAndUrlNonPrereq = nonPrereqText.match(/\b(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\b\s+((?:https?:\/\/[^\s"'()`<>]+)|\/[^\s"'()`<>]+)/i);
      if (methodAndUrlNonPrereq) {
        if (!hasLabeledMethod) method = methodAndUrlNonPrereq[1].toUpperCase();
        url = methodAndUrlNonPrereq[2];
      }
    }
    
    // Last resort: look for any URL in non-prereq primary section or non-prereq full text
    if (!url && nonPrereqPrimaryText) {
      const urlOnlyCreate = nonPrereqPrimaryText.match(/https?:\/\/[^\s)"']+/);
      if (urlOnlyCreate) url = urlOnlyCreate[0];
    }
    
    if (!url) {
      const urlOnlyNonPrereq = nonPrereqText.match(/https?:\/\/[^\s)"']+/);
      url = urlOnlyNonPrereq ? urlOnlyNonPrereq[0] : '';
    }

    // Extract params from non-prereq sections only
    const params = [];
    // Accept labels like "Query Params:", "Query Parameters", "Parameters" and allow bullets or same-line values
    const findParamSection = (text) => text ? text.match(/(?:Query\s*Params?|Query\s*Parameters?|Parameters?)\s*[:\-–—]?\s*(?:\r?\n|$)\s*([\s\S]{0,800})/i) : null;
    const paramSectionMatch =
      findParamSection(nonPrereqPrimaryText) ||
      findParamSection(nonPrereqText) ||
      findParamSection(sanitizeForLabels(nonPrereqPrimaryText)) ||
      findParamSection(sanitizeForLabels(nonPrereqText));
    if (paramSectionMatch) {
      let block = paramSectionMatch[1];
      // Stop at the next labeled section header or a double newline gap
      const cut = block.search(/\n(?:Headers?|Request\s*Headers?|Body|Request|Response|Example|Sample)\b|\n\n/);
      if (cut !== -1) block = block.slice(0, cut);
      // Support comma or newline separated inline params
      const items = block.split(/\r?\n|,\s*/);
      items.forEach(line => {
        const trimmed = line.trim(); if (!trimmed) return;
        let m = trimmed.match(/^([A-Za-z0-9_.-]+)\s*=\s*(.+)$/);
        if (!m) m = trimmed.match(/^([A-Za-z0-9_.-]+)\s*[:]\s*(.+)$/);
        if (m) params.push({ id: Date.now()+Math.random(), key: m[1].trim(), value: m[2].trim(), enabled: true });
      });
    }
    try {
      if (url && url.includes('?')) {
        const qStr = url.split('?')[1].split('#')[0];
        new URLSearchParams(qStr).forEach((v,k) => {
          if (!params.some(p => p.key === k)) params.push({ id: Date.now()+Math.random(), key: k, value: v, enabled: true });
        });
      }
    } catch {}
    if (!params.length) params.push({ id: 1, key: '', value: '', enabled: true });

    // Extract headers from non-prereq sections only
    const headers = [];
    const findHeaderSection = (text) => text ? text.match(/(?:Headers?|Request\s*Headers?)\s*[:\-–—]?\s*(?:\r?\n|$)\s*([\s\S]{0,800})/i) : null;
    const headerSectionMatch =
      findHeaderSection(nonPrereqPrimaryText) ||
      findHeaderSection(nonPrereqText) ||
      findHeaderSection(sanitizeForLabels(nonPrereqPrimaryText)) ||
      findHeaderSection(sanitizeForLabels(nonPrereqText));
    if (headerSectionMatch) {
      let block = headerSectionMatch[1];
      const cut = block.search(/\n(?:Query\s*Params?|Parameters?|Body|Request|Response)\b|\n\n/);
      if (cut !== -1) block = block.slice(0, cut);
      // Support comma or newline separated inline headers
      const items = block.split(/\r?\n|,\s*/);
      items.forEach(line => {
        const trimmed = line.trim(); if (!trimmed) return;
        const m = trimmed.match(/^(?:-\s*)?([A-Za-z0-9-]+)\s*[:=]\s*(.+)$/);
        if (m) {
          const key = m[1].trim();
          headers.push({ id: Date.now()+Math.random(), key, value: m[2].trim(), enabled: true });
        }
      });
    }
    // Fallback: scan for explicit apikey header if not parsed from header section
    if (!headers.length) {
      const apikeyInline = (nonPrereqPrimaryText || nonPrereqText).match(/\bapikey\s*[:=]\s*([^\s,;\n]+)/i);
      if (apikeyInline) {
        headers.push({ id: Date.now()+Math.random(), key: 'apikey', value: apikeyInline[1].trim(), enabled: true });
      }
    }
    if (!headers.length) headers.push({ id: 1, key: '', value: '', enabled: true });

    // Extract request body from non-prereq sections only
    let body = '';
    // First get all content before any response sections from non-prereq text
    const nonPrereqPrimaryPreResponse = nonPrereqPrimaryText.split(/\n(?:Response|Sample\s*Response|\w+\s*Response)\b/i)[0] || nonPrereqPrimaryText;
    const nonPrereqFullPreResponse = nonPrereqText.split(/\n(?:Response|Sample\s*Response|\w+\s*Response)\b/i)[0] || nonPrereqText;
    
    // Try to find in non-prereq primary section first
    const fenceRe = /```(?:json)?\n([\s\S]*?)```/gi;
    let fenceMatch;
    while ((fenceMatch = fenceRe.exec(nonPrereqPrimaryPreResponse)) !== null) {
      const idx = fenceMatch.index; 
      const context = nonPrereqPrimaryPreResponse.slice(Math.max(0, idx - 220), idx);
      if (/(Body|Request\s*Body|Payload|Request\s*Payload|Data|JSON\s*Body)\b/i.test(context)) { 
        body = fenceMatch[1].trim(); 
        break; 
      }
    }
    
    // Try labeled body section in non-prereq primary section
    if (!body) {
      const labeled = nonPrereqPrimaryPreResponse.match(/(?:Body|Request\s*Body|Payload|Request\s*Payload|Data|JSON\s*Body)[:\s]*\n([\s\S]{0,4000})/i);
      if (labeled) {
        const section = labeled[1];
        const inFence = section.match(/```(?:json)?\n([\s\S]*?)```/i);
        if (inFence) body = inFence[1].trim();
        else { const inline = section.match(/\{[\s\S]*\}/); if (inline && inline[0].length < 10000) body = inline[0]; }
      }
    }

    // Inline label with JSON immediately after (same line) in non-prereq primary
    if (!body) {
      const inlineLabeled = nonPrereqPrimaryPreResponse.match(/(?:Body|Request\s*Body|Payload|Request\s*Payload|Data|JSON\s*Body)\s*[:=]?\s*(\{[\s\S]*?\})/i);
      if (inlineLabeled && inlineLabeled[1] && inlineLabeled[1].length < 15000) {
        body = inlineLabeled[1].trim();
      }
    }
    
    // If nothing found in non-prereq primary section, try the non-prereq full text
    if (!body) {
      // Try non-prereq full text fence blocks with body keyword context
      let fullTextMatch;
      const fullFenceRe = /```(?:json)?\n([\s\S]*?)```/gi;
      while ((fullTextMatch = fullFenceRe.exec(nonPrereqFullPreResponse)) !== null) {
        const idx = fullTextMatch.index; 
        const context = nonPrereqFullPreResponse.slice(Math.max(0, idx - 220), idx);
        if (/(Body|Request\s*Body|Payload|Request\s*Payload|Data|JSON\s*Body)\b/i.test(context)) { 
          body = fullTextMatch[1].trim(); 
          break; 
        }
      }
      
      // Try labeled body section in non-prereq full text
      if (!body) {
        const labeled = nonPrereqFullPreResponse.match(/(?:Body|Request\s*Body|Payload|Request\s*Payload|Data|JSON\s*Body)[:\s]*\n([\s\S]{0,4000})/i);
        if (labeled) {
          const section = labeled[1];
          const inFence = section.match(/```(?:json)?\n([\s\S]*?)```/i);
          if (inFence) body = inFence[1].trim();
          else { const inline = section.match(/\{[\s\S]*\}/); if (inline && inline[0].length < 10000) body = inline[0]; }
        }
      }
    }

    // Inline label with JSON in non-prereq full text
    if (!body) {
      const inlineLabeledFull = nonPrereqFullPreResponse.match(/(?:Body|Request\s*Body|Payload|Request\s*Payload|Data|JSON\s*Body)\s*[:=]?\s*(\{[\s\S]*?\})/i);
      if (inlineLabeledFull && inlineLabeledFull[1] && inlineLabeledFull[1].length < 15000) {
        body = inlineLabeledFull[1].trim();
      }
    }

    // Fallback: any JSON object after method/URL for write methods from non-prereq text
    if (!body && /^(POST|PUT|PATCH|DELETE)$/i.test(method)) {
      const searchSpace = (nonPrereqPrimaryPreResponse + '\n' + nonPrereqFullPreResponse).slice(0, 40000);
      const jsonCandidate = (() => {
        const idx = searchSpace.indexOf('{');
        if (idx === -1) return null;
        let depth = 0; let inString = false; let esc = false; let end = -1;
        for (let i = idx; i < searchSpace.length; i++) {
          const ch = searchSpace[i];
          if (inString) {
            if (esc) { esc = false; continue; }
            if (ch === '\\') { esc = true; continue; }
            if (ch === '"') inString = false;
          } else {
            if (ch === '"') inString = true;
            else if (ch === '{') depth++;
            else if (ch === '}') { depth--; if (depth === 0) { end = i; break; } }
          }
        }
        if (end !== -1) {
          const snippet = searchSpace.slice(idx, end + 1);
          if (/"[A-Za-z0-9_.-]+"\s*:\s*/.test(snippet)) return snippet; // looks like JSON object
        }
        return null;
      })();
      if (jsonCandidate && jsonCandidate.length < 15000) body = jsonCandidate.trim();
    }
    
    // Last resort for write methods - any fenced block from non-prereq text
    if (!body && /^(POST|PUT|PATCH|DELETE)$/i.test(method)) {
      const createFence = nonPrereqPrimaryPreResponse.match(/```(?:json)?\n([\s\S]*?)```/i);
      const fullFence = nonPrereqFullPreResponse.match(/```(?:json)?\n([\s\S]*?)```/i);
      if (createFence) body = createFence[1].trim();
      else if (fullFence) body = fullFence[1].trim();
    }

    const decodeHtml = (str) => {
      if (!str) return '';
      const textarea = document.createElement('textarea');
      textarea.innerHTML = str;
      return textarea.value.replace(/\\u003c/g, '<').replace(/\\u003e/g, '>');
    };
    url = decodeHtml(url);
    headers.forEach(h => { h.key = decodeHtml(h.key); h.value = decodeHtml(h.value); });
    params.forEach(p => { p.key = decodeHtml(p.key); p.value = decodeHtml(p.value); });
    body = decodeHtml(body);

    // Enforce empty body for non-body methods
    if (/^(GET|HEAD|OPTIONS)$/i.test(method)) {
      body = '';
    }

    return { method, url, headers, params, body, prerequisites: prereqText };
  };

  // Decide when to show the Manual/Automatic execute CTA and Play button.
  // Rule: if we can reliably detect METHOD + URL and at least one
  // of params/body (depending on method), we ALWAYS offer Manual/Automatic
  // regardless of whether prerequisites are present.
  const isExecutableApiResponse = (text) => {
    try {
      if (!text || typeof text !== 'string') return false;
      const { method = '', url = '', params = [], body = '' } = extractRequestFromContent(text);
      const hasMethod = /^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)$/i.test(method || '');
      // Accept either a full URL or a path starting with '/' (Execution Console
      // will prepend the instance base URL).
      const hasUrl = /^https?:\/\//i.test(url || '') || /^\//.test(url || '');
      if (!hasMethod || !hasUrl) return false;

      const upper = method.toUpperCase();
      if (upper === 'GET' || upper === 'HEAD' || upper === 'OPTIONS') {
        // For read-only methods, require at least one query param either in
        // parsed params or directly present in the URL. This is enough to
        // construct a meaningful request; no body is expected.
        const hasParamsList = Array.isArray(params) && params.some(p => (p?.key || '').trim());
        const hasParamsInUrl = (() => { try { return new URL(url).search.length > 1; } catch { return /\?/.test(url); }})();
        return hasParamsList || hasParamsInUrl;
      }
      // For write methods (POST/PUT/PATCH/DELETE), prefer a non-empty body,
      // but if a body cannot be parsed and we still have a valid METHOD + URL
      // and at least one param, we still consider it executable so the user
      // can edit the body manually in the console.
      const hasBody = typeof body === 'string' && body.trim().length > 0;
      if (hasBody) return true;
      const hasParamsList = Array.isArray(params) && params.some(p => (p?.key || '').trim());
      return hasParamsList;
    } catch {
      return false;
    }
  };

  // Handle play click: open manual Execution Console directly
  const handlePlayClick = useCallback((assistantMessage) => {
    const content = assistantMessage?.content || '';
    const prefill = extractRequestFromContent(content);
    const actionTitle = deriveActionTitle(content);
    openConsoleWithRequest(prefill, undefined, {
      actionTitle,
      source: 'chat',
      username: storageUser
    });
  }, [openConsoleWithRequest, storageUser]);

  // Real-time streaming from fetch response
  const streamResponseFromFetch = async (response, messageId) => {
    try {
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let accumulatedData = '';
      let responseText = '';
      let sourceFiles = [];
      let hasStartedStreaming = false;

      while (true) {
        if (cancelStreamRef.current) break;

        const { done, value } = await reader.read();
        if (done) break;

        // Decode the chunk
        const chunk = decoder.decode(value, { stream: true });
        accumulatedData += chunk;

        // Try to parse JSON if we have complete data
        try {
          const data = JSON.parse(accumulatedData);
          responseText = data.rawData || data.response || data.message || 'No data available from backend.';
          sourceFiles = data.sourceFiles || [];
          
          // Apply formatting
          responseText = reformatShellIfNeeded(responseText);
          responseText = reformatYamlIfNeeded(responseText);
          responseText = reformatJavaIfNeeded(responseText);

          // Decide whether to show Manual/Automatic choice and prerequisite gate
          const shouldShowExecChoice = isExecutableApiResponse(responseText);
          
          setHiddenMessageIds(prev => {
            const next = { ...prev };
            // Hide content behind prerequisites gate ONLY when we can execute
            // Always show Manual/Automatic choice if executable, even without prerequisites
            if (shouldShowExecChoice) next[messageId] = true; else delete next[messageId];
            return next;
          });
          // Cache prerequisites if applicable - extract ONLY the prerequisites section
          const parsed = extractRequestFromContent(responseText);
          let prereqBlock = parsed.prerequisites || '';
          if (!prereqBlock || !prereqBlock.trim()) {
            try {
              // Match prerequisites heading and capture content until the next numbered/heading section
              const prereqMatch = responseText.match(/(^|\n)\s*(?:#{1,6}\s*)?(?:prerequisite|prerequisites|requirements|before you begin|before starting|prereq)\b[^\n]*/i);
              if (prereqMatch) {
                const startIdx = prereqMatch.index;
                const afterPrereqHeading = responseText.slice(startIdx);
                // Find where prerequisites section ends - look for next numbered item (1., 2., etc.) or heading
                const endMatch = afterPrereqHeading.slice(prereqMatch[0].length).match(/\n\s*(?:#{1,6}\s+|\d+[\.\)]\s+|\(\d+\)\s+)/);
                if (endMatch) {
                  // Extract from prereq heading to just before the next section
                  const endIdx = startIdx + prereqMatch[0].length + endMatch.index;
                  prereqBlock = responseText.slice(startIdx, endIdx).trim();
                } else {
                  // No next section found, take until a double newline or end
                  const doubleNewline = afterPrereqHeading.indexOf('\n\n');
                  if (doubleNewline > 0) {
                    prereqBlock = responseText.slice(startIdx, startIdx + doubleNewline).trim();
                  } else {
                    prereqBlock = prereqMatch[0].trim();
                  }
                }
                // Ensure it has a proper heading
                const hasHeading = /^\s*#/.test(prereqBlock);
                if (!hasHeading) {
                  prereqBlock = `## Prerequisites\n\n${prereqBlock.replace(/^(prerequisite|prerequisites|requirements|before you begin|before starting|prereq)\b[:\-–—]?\s*/i, '')}`;
                }
              }
            } catch {}
          }
          setPrereqById(prev => ({ ...prev, [messageId]: prereqBlock || '' }));

          // Start streaming the complete response
          await streamResponse(responseText, messageId, sourceFiles);
          break;
        } catch (e) {
          // JSON is incomplete, continue reading
          // But start streaming any readable text we have
          if (!hasStartedStreaming && accumulatedData.length > 50) {
            hasStartedStreaming = true;
            // Extract any readable text and start streaming partial content
            const partialText = extractReadableText(accumulatedData);
            if (partialText) {
              await streamResponse(partialText + '...', messageId, []);
            }
          }
        }
      }
    } catch (error) {
      console.error('Streaming error:', error);
      // Fall back to error streaming
      await streamResponse('Error occurred while streaming response.', messageId, []);
    }
  };

  // Helper function to extract readable text from partial JSON
  const extractReadableText = (data) => {
    try {
      // Try to find response text in partial JSON
      const responseMatch = data.match(/"(?:rawData|response|message)"\s*:\s*"([^"]*(?:\\.[^"]*)*)"/);
      if (responseMatch && responseMatch[1]) {
        return responseMatch[1].replace(/\\n/g, '\n').replace(/\\"/g, '"');
      }
    } catch (e) {
      // Ignore parsing errors
    }
    return '';
  };

  // Enhanced streaming function with natural timing
  const streamResponse = async (responseText, messageId, sourceFiles) => {
    const words = responseText.split(' ');
    let currentContent = '';
    
    for (let i = 0; i < words.length; i++) {
      if (cancelStreamRef.current) break;
      currentContent += (i > 0 ? ' ' : '') + words[i];
      
      setMessages(prev => prev.map(msg => 
        msg.id === messageId 
          ? { 
              ...msg, 
              content: currentContent, 
              sourceFiles: i === words.length - 1 ? sourceFiles : [] // Add source files at the end
            }
          : msg
      ));
      
      // Watson-like streaming timing - faster and more natural
      let delay = 15; // Very fast base delay
      
      if (words[i].endsWith('.') || words[i].endsWith('!') || words[i].endsWith('?')) {
        delay = 100; // Brief pause at sentence endings
      } else if (words[i].endsWith(',') || words[i].endsWith(':') || words[i].endsWith(';')) {
        delay = 50; // Quick pause at punctuation
      } else if (words[i].includes('\n')) {
        delay = 80; // Pause at line breaks
      } else if (words[i].startsWith('```') || words[i].includes('```')) {
        delay = 120; // Pause at code blocks
      } else if (words[i].startsWith('**') || words[i].includes('**')) {
        delay = 30; // Slight pause for formatting
      } else if (words[i].length > 12) {
        delay = 25; // Slightly longer for very long words
      }
      
      // Add some randomness for natural feel (±5ms)
      delay += Math.random() * 10 - 5;
      delay = Math.max(5, delay); // Minimum 5ms delay
      
  await new Promise(resolve => setTimeout(resolve, delay));
    }
  };

  // Function to provide simple fallback responses
  const getMockResponse = (userInput) => {
    return `Backend service is currently unavailable. Your query "${userInput}" has been noted. Please try again later when the service is restored.`;
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey && !isLoading && inputMessage.trim()) {
      e.preventDefault();
      sendMessage();
    }
  };

  const [showHistory, setShowHistory] = useState(false);
  const hasSavedRef = useRef(false);
  const [historyList, setHistoryList] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);

  // Load history from MongoDB when component mounts
  useEffect(() => {
    const loadHistory = async () => {
      if (!storageUser) return;
      setHistoryLoading(true);
      try {
        const history = await historyAPI.getUserHistory(storageUser);
        setHistoryList(history || []);
      } catch (error) {
        console.error('Failed to load history:', error);
      } finally {
        setHistoryLoading(false);
      }
    };
    loadHistory();
  }, [storageUser]);

  const saveSessionToHistory = useCallback(async () => {
    console.log('saveSessionToHistory called, messages.length:', messages.length);
    // Only skip if we have no messages to save
    if (!messages || messages.length === 0) {
      console.log('No messages to save, returning early');
      return;
    }
    // Prefer the most recent persisted messages from localStorage to avoid stale closures
    let latestMessages = messages;
    try {
      const raw = localStorage.getItem(storageKey);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed) && parsed.length) {
          latestMessages = parsed.map(m => ({ ...m, timestamp: new Date(m.timestamp) }));
        }
      }
    } catch { /* ignore */ }

    console.log('Latest messages length:', latestMessages.length);
    if (!latestMessages || latestMessages.length === 0) {
      console.log('No messages to save, returning');
      return;
    }

    const titleCandidate = latestMessages.find(m => m.type === 'user')?.content || 'Session';
    const title = titleCandidate.length > 60 ? titleCandidate.slice(0, 57) + '...' : titleCandidate;
    const session = {
      id: uuidv4(),
      username: storageUser,
      title,
  // Use ISO-8601 with 'Z' (UTC). Backend stores Instant in UTC.
  createdAt: new Date().toISOString(),
      messages: latestMessages.map(m => ({
        id: m.id,
        type: m.type,
        content: m.content,
        fullContent: m.fullContent ?? undefined,
        sourceFiles: m.sourceFiles ?? undefined,
        timestamp: (m.timestamp instanceof Date ? m.timestamp.toISOString() : (m.timestamp || new Date().toISOString()))
      }))
    };

    console.log('Attempting to save session:', session);
    try {
      const saved = await historyAPI.saveHistory(session);
      console.log('✅ Successfully saved session:', saved.id);
      setHistoryList(prev => [saved, ...prev].slice(0, 50)); // cap to 50 sessions
      
      // Show success feedback
      setError(null);
      console.log('💾 Chat saved to history!');
      
    } catch (error) {
      console.error('❌ Failed to save history:', error);
      setError(`Failed to save chat: ${error.message}`);
    }
  }, [messages, storageKey, storageUser]);

  // Auto-save to history on page unload (placed after saveSessionToHistory definition to avoid TDZ)
  useEffect(() => {
    const handleBeforeUnload = () => {
      if (messages.length > 0) {
        saveSessionToHistory();
      }
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [messages, saveSessionToHistory]);

  // Auto-save after a complete conversation (user + assistant exchange)
  useEffect(() => {
    if (messages.length < 2) return; // Need at least user + assistant message
    
    const lastMessage = messages[messages.length - 1];
    const secondLastMessage = messages[messages.length - 2];
    
    // Save when we have a complete exchange: user -> assistant
    if (lastMessage.type === 'assistant' && secondLastMessage.type === 'user') {
      // Debounce to avoid multiple saves during streaming
      const timer = setTimeout(() => {
        console.log('Auto-saving after complete conversation exchange');
        saveSessionToHistory();
      }, 2000);
      
      return () => clearTimeout(timer);
    }
  }, [messages, saveSessionToHistory]);

  const restoreSession = (session) => {
    const restored = (session.messages || []).map(m => ({ ...m, timestamp: new Date(m.timestamp) }));
    // Exit welcome screen and reset UI state so messages are visible
    setShowWelcome(false);
    setHiddenMessageIds({});
    setCollapsedMessageIds({});
    setPrereqById({});
    setMessages(restored);
    setShowHistory(false);
    hasSavedRef.current = false;
  };

  const deleteSession = async (id) => {
    try {
      await historyAPI.deleteHistory(id);
      const next = historyList.filter(s => s.id !== id);
      setHistoryList(next);
    } catch (error) {
      console.error('Failed to delete history:', error);
    }
  };

  const clearChat = () => {
    // stop any ongoing stream
    cancelStreamRef.current = true;
    setIsStreaming(false);
    setStreamingMessageId(null);
    setIsLoading(false);
    // Save current conversation to history before clearing
    saveSessionToHistory();
    setShowWelcome(true);
    setMessages([]);
  hasSavedRef.current = false;
    setInputMessage('');
    setError(null);
    try { localStorage.removeItem(storageKey); } catch (e) { /* ignore */ }
    inputRef.current?.focus();
  };

  const onEraseClick = () => {
    if (skipClearConfirm) {
      clearChat();
    } else {
      setShowClearConfirm(true);
    }
  };

  const handleConfirmClear = () => {
    // Preference is already persisted on checkbox change
    clearChat();
    setShowClearConfirm(false);
  };

  const handleCancelClear = () => {
    setShowClearConfirm(false);
  };

  // Memoized callbacks to prevent MessageComponent re-renders
  const handleToggleCollapse = useCallback((messageId) => {
    setCollapsedMessageIds(prev => ({ ...prev, [messageId]: !prev[messageId] }));
  }, []);

  const handleRevealManual = useCallback((messageId) => {
    setHiddenMessageIds(prev => { const n = { ...prev }; delete n[messageId]; return n; });
    setCollapsedMessageIds(prev => ({ ...prev, [messageId]: false }));
  }, []);

  const openExecutionConsole = useCallback((assistantMessage) => {
    const content = assistantMessage?.content || '';
    const prefill = extractRequestFromContent(content);
    const actionTitle = deriveActionTitle(content);
    openConsoleWithRequest(prefill, undefined, {
      actionTitle,
      source: 'chat',
      username: storageUser
    });
  }, [openConsoleWithRequest, storageUser]);

  // Open Execution Console for a specific section title within a message
  const openExecutionConsoleForSection = useCallback((assistantMessage, sectionTitle) => {
    try {
      const full = assistantMessage.content || '';
      // Build sections using the same heading detection as elsewhere
      const headingRe = /(^|\n)\s*(?:#{1,6}\s*|\d+[\.\)]\s+|\(\d+\)\s+|\d+\s*[-–—]\s+)([^\n]+)/gi;
      const indices = [];
      let match;
      while ((match = headingRe.exec(full)) !== null) {
        const title = sanitizeTitleText(match[2] || '');
        const start = match.index + (match[1] ? match[1].length : 0);
        indices.push({ title, start });
      }
      if (!indices.length) {
        // Fallback: use entire content
        const parsed = extractRequestFromPrimaryAndFull(full, full, '');
        const fallbackTitle = sanitizeTitleText(sectionTitle || '');
        const actionTitle = deriveActionTitle(full, fallbackTitle);
        openConsoleWithRequest(parsed, undefined, {
          actionTitle,
          source: 'chat',
          username: storageUser
        });
        return;
      }
      // Find the first heading whose title includes the sectionTitle text (case-insensitive)
      const normalizedSection = sanitizeTitleText(sectionTitle || '');
      const idx = normalizedSection
        ? indices.findIndex(h => h.title.toLowerCase().includes(normalizedSection.toLowerCase()))
        : -1;
      const useIdx = idx !== -1 ? idx : 0;
      const start = indices[useIdx].start;
      const end = useIdx + 1 < indices.length ? indices[useIdx + 1].start : full.length;
      const sectionText = full.slice(start, end);
  const parsed = extractRequestFromPrimaryAndFull(sectionText, full, '');
  const actionTitle = deriveActionTitle(sectionText, normalizedSection || indices[useIdx].title || '');
      openConsoleWithRequest(parsed, undefined, {
        actionTitle,
        source: 'chat',
        username: storageUser
      });
    } catch (e) {
      // On any error, fallback to whole message extraction
      openExecutionConsole(assistantMessage);
    }
  }, [openConsoleWithRequest, openExecutionConsole, storageUser]);

  // Open Execution Console for a section derived from raw text (used by GitHub modal)
  const openExecutionConsoleForSectionFromRaw = useCallback((rawText, sectionTitle) => {
    try {
      const full = rawText || '';
      const headingRe = /(^|\n)\s*(?:#{1,6}\s*|\d+[\.\)]\s+|\(\d+\)\s+|\d+\s*[-–—]\s+)([^\n]+)/gi;
      const indices = [];
      let match;
      while ((match = headingRe.exec(full)) !== null) {
        const title = sanitizeTitleText(match[2] || '');
        const start = match.index + (match[1] ? match[1].length : 0);
        indices.push({ title, start });
      }
      if (!indices.length) {
        const parsed = extractRequestFromPrimaryAndFull(full, full, '');
        const fallbackTitle = sanitizeTitleText(sectionTitle || '');
        const actionTitle = deriveActionTitle(full, fallbackTitle);
        const meta = {
          actionTitle,
          source: 'github-section',
          username: storageUser
        };
        // Close GitHub modal first to avoid stacking issues
        setGitModal(prev => ({ ...prev, open: false }));
        setTimeout(() => { openConsoleWithRequest(parsed, undefined, meta); }, 0);
        return;
      }
      const normalizedSection = sanitizeTitleText(sectionTitle || '');
      const idx = normalizedSection
        ? indices.findIndex(h => h.title.toLowerCase().includes(normalizedSection.toLowerCase()))
        : -1;
      const useIdx = idx !== -1 ? idx : 0;
      const start = indices[useIdx].start;
      const end = useIdx + 1 < indices.length ? indices[useIdx + 1].start : full.length;
      const sectionText = full.slice(start, end);
  const parsed = extractRequestFromPrimaryAndFull(sectionText, full, '');
  const actionTitle = deriveActionTitle(sectionText, normalizedSection || indices[useIdx].title || '');
      const meta = {
        actionTitle,
        source: 'github-section',
        username: storageUser
      };
      // Close GitHub modal first to avoid stacking issues
      setGitModal(prev => ({ ...prev, open: false }));
      setTimeout(() => { openConsoleWithRequest(parsed, undefined, meta); }, 0);
    } catch (e) {
  const parsed = extractRequestFromPrimaryAndFull(rawText || '', rawText || '', '');
      setGitModal(prev => ({ ...prev, open: false }));
  const fallbackTitle = sanitizeTitleText(sectionTitle || '');
  const actionTitle = deriveActionTitle(rawText || '', fallbackTitle);
      const meta = {
        actionTitle,
        source: 'github-section',
        username: storageUser
      };
      setTimeout(() => { openConsoleWithRequest(parsed, undefined, meta); }, 0);
    }
  }, [openConsoleWithRequest, storageUser]);

  // No automatic execution flow

  const handleSampleQuestion = (question) => {
    setInputMessage(question);
    setShowWelcome(false);
    // Trigger sending the message after a brief delay to ensure state updates
    setTimeout(() => {
      sendMessageWithText(question);
    }, 100);
  };

  const sendMessageWithText = async (messageText = inputMessage) => {
    if (showWelcome) {
      setShowWelcome(false);
    }
    
    if (!messageText.trim()) return;

    const userMessage = {
      id: uuidv4(),
      type: 'user',
      content: messageText.trim(),
      timestamp: new Date(),
    };

    setMessages(prev => [...prev, userMessage]);
    setInputMessage('');
    setError(null);

    // Create loading placeholder message for assistant
    const assistantMessageId = uuidv4();
    const assistantMessage = {
      id: assistantMessageId,
      type: 'assistant',
      content: '',
      timestamp: new Date(),
      isLoading: true,
    };

    setMessages(prev => [...prev, assistantMessage]);
    setIsLoading(true);

    // Create abort controller for this request
    abortControllerRef.current = new AbortController();
    cancelStreamRef.current = false;

    try {
      const response = await fetch('/api/chat/message', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: userMessage.content,
          sessionId: 'web-session-' + Date.now(),
          includeContext: true,
          fastMode: true,
          fullContent: true
        }),
        signal: abortControllerRef.current.signal,
      });

      if (response.ok) {
        // Start real-time streaming immediately
        setIsLoading(false);
        setMessages(prev => prev.map(msg => 
          msg.id === assistantMessageId 
            ? { 
                ...msg, 
                isLoading: false,
                content: '',
                sourceFiles: []
              }
            : msg
        ));
  setIsStreaming(true);
  setStreamingMessageId(assistantMessageId);
        
  // Use real-time streaming from fetch; hidden/prereq set inside
  await streamResponseFromFetch(response, assistantMessageId);
      } else {
        const responseText = 'Backend service unavailable. Please try again later.';
        // Use streaming for error messages too
        setMessages(prev => prev.map(msg => 
          msg.id === assistantMessageId
            ? {
                ...msg,
                isLoading: false,
                content: '',
                isError: true,
                sourceFiles: []
              }
            : msg
        ));
        setIsStreaming(true);
        setStreamingMessageId(assistantMessageId);
  await streamResponse(responseText, assistantMessageId, []);
  setPrereqById(prev => ({ ...prev, [assistantMessageId]: '' }));
      }
    } catch (error) {
      if (error.name !== 'AbortError') {
        const responseText = 'Unable to connect to backend service. Please check your connection and try again.';
        // Use streaming for error messages in catch block too
        setMessages(prev => prev.map(msg => 
          msg.id === assistantMessageId
            ? {
                ...msg,
                isLoading: false,
                content: '',
                isError: true,
                sourceFiles: []
              }
            : msg
        ));
        setIsStreaming(true);
        setStreamingMessageId(assistantMessageId);
  await streamResponse(responseText, assistantMessageId, []);
  setPrereqById(prev => ({ ...prev, [assistantMessageId]: '' }));
      } else {
        // If aborted, remove the loading message
        setMessages(prev => prev.filter(msg => msg.id !== assistantMessageId));
      }
    } finally {
      setIsLoading(false);
      setIsStreaming(false);
      setStreamingMessageId(null);
      abortControllerRef.current = null;
      inputRef.current?.focus();
      // Don't auto-save here - save manually or on significant events
    }
  };

  return (
    <div className="carbon-chat-interface">
      {/* Top bar with actions on the right */}
      <div className="chat-topbar" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
        <div />
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <Button size="sm" kind="ghost" onClick={onEraseClick} title="Start a new chat (saves current to history)">New Chat</Button>
          <Button size="sm" kind="tertiary" onClick={() => setShowHistory(true)} title="View chat history">History</Button>
        </div>
      </div>
      {error && (
        <InlineNotification
          kind="error"
          title="Connection Error"
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
        />
      )}

      {showWelcome ? (
        <WelcomeScreen onSampleClick={handleSampleQuestion} product={product} />
      ) : (
        <div className="messages-container">
          {messages.map((message) => (
            <MessageComponent
              key={message.id}
              message={message}
              isStreaming={isStreaming && message.id === streamingMessageId}
              onOpenExec={handlePlayClick}
              onOpenExecSection={openExecutionConsoleForSection}
              onOpenGitFile={openGitFileModal}
              hidden={!!hiddenMessageIds[message.id]}
              collapsed={!!collapsedMessageIds[message.id]}
              onToggleCollapse={() => handleToggleCollapse(message.id)}
              onRevealManual={() => handleRevealManual(message.id)}
              canExecute={isExecutableApiResponse(message.content || '')}
              prereqText={prereqById[message.id] || extractRequestFromContent(message.content || '').prerequisites}
              assistantName={assistantName}
            />
          ))}

          <div ref={messagesEndRef} className="scroll-anchor" />
        </div>
      )}

      <div className="input-container">
        <div className="typing-box">
          <div className={`watson-input ${isLoading ? 'disabled' : ''}`}>
            <input
              ref={inputRef}
              id="message-input"
              type="text"
              placeholder="Type something..."
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              onKeyDown={handleKeyPress}
              disabled={isLoading}
              className={isLoading || isStreaming ? 'input-active' : ''}
              autoComplete="off"
            />
            {/* Manual save button removed - auto-save handles saving now */}
            <button
              type="button"
              className="icon-btn erase"
              onClick={onEraseClick}
              aria-label="Clear chat"
              title="Clear chat"
            >
              <Erase size={20} />
            </button>
            <button
              type="button"
              className={`icon-btn send ${isStreaming || isLoading ? 'as-stop' : ''}`}
              onClick={isStreaming || isLoading ? stopStreaming : sendMessage}
              disabled={!inputMessage.trim() && !(isStreaming || isLoading)}
              aria-label={isStreaming || isLoading ? 'Stop generation' : 'Send message'}
              title={isStreaming || isLoading ? 'Stop' : 'Send message'}
            >
              {isStreaming || isLoading ? (
                <StopFilledAlt size={20} />
              ) : (
                <Send size={20} />
              )}
            </button>
          </div>
          <div className="input-glow" aria-hidden="true"></div>
        </div>
        <div className="footer-gradient" aria-hidden="true"></div>
      </div>
      {/* Clear chat confirmation modal */}
      <Modal
        open={showClearConfirm}
        modalHeading="Clear chat conversation?"
        primaryButtonText="Clear"
        secondaryButtonText="Cancel"
        onRequestClose={handleCancelClear}
        onRequestSubmit={handleConfirmClear}
        preventCloseOnClickOutside
        className="clear-chat-modal"
      >
        <div className="clear-modal-body">
          <p>
            If you clear this chat, you save it in the session history. You will begin a new chat.
          </p>
          <div className="clear-modal-checkbox">
            <Checkbox
              id="skip-clear-confirm"
              labelText="Don't show this again"
              checked={skipClearConfirm}
              onChange={(arg1, arg2) => {
                // Support both signatures: (checked, id) and (event, { checked })
                const next = typeof arg1 === 'boolean'
                  ? arg1
                  : (arg2 && typeof arg2.checked === 'boolean')
                    ? arg2.checked
                    : !!(arg1 && arg1.target && arg1.target.checked);
                setSkipClearConfirm(next);
                try { localStorage.setItem('clearConfirmSkip', String(next)); } catch {}
              }}
            />
          </div>
        </div>
      </Modal>
      {/* Chat history modal */}
      <Modal
        open={showHistory}
        modalHeading="Chat history"
        primaryButtonText="Close"
        onRequestClose={() => setShowHistory(false)}
        onRequestSubmit={() => setShowHistory(false)}
        size="lg"
      >
        <div style={{ maxHeight: '60vh', overflow: 'auto' }}>
          {historyList.length === 0 ? (
            <p style={{ color: 'var(--cds-text-secondary, #6f6f6f)' }}>No saved conversations yet.</p>
          ) : (
            <div className="history-list">
              {historyList.map(item => (
                <div key={item.id} className="history-item" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid var(--cds-border-subtle, #e0e0e0)' }}>
                  <div>
                    <div style={{ fontWeight: 600 }}>{item.title}</div>
                    <div style={{ fontSize: '0.85rem', color: 'var(--cds-text-secondary, #6f6f6f)' }}>{new Date(item.createdAt).toLocaleString()}</div>
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <Button size="sm" kind="secondary" onClick={() => restoreSession(item)}>Open</Button>
                    <Button size="sm" kind="danger--tertiary" onClick={() => deleteSession(item.id)}>Delete</Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </Modal>
      {/* Automatic/Manual flow removed: play button opens manual console directly */}
      {/* Inline choice in message; modal removed */}

      {/* GitHub file preview modal */}
      <Modal
        open={gitModal.open}
        passiveModal
        modalHeading={gitModal.title || 'GitHub File'}
        onRequestClose={() => setGitModal(prev => ({ ...prev, open: false }))}
        className="github-preview-modal"
        size="lg"
      >
        <div className="github-modal-body">
          {gitModal.meta && (
            <div className="github-meta" style={{ marginBottom: '0.5rem', fontSize: '0.85rem', color: 'var(--cds-text-secondary, #6f6f6f)' }}>
              {gitModal.meta.repository}
              {gitModal.meta.branch ? `@${gitModal.meta.branch}` : ''}
              {gitModal.meta.path ? ` — ${gitModal.meta.path}` : ''}
            </div>
          )}
          {gitModal.loading && (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '1rem 0' }}>
              <Loading withOverlay={false} description="Loading file…" small={false} />
            </div>
          )}
          {gitModal.error && (
            <InlineNotification
              kind="error"
              title="Failed to load file"
              subtitle={gitModal.error}
              lowContrast
            />
          )}
          {!gitModal.loading && !gitModal.error && gitModal.content && (
            <div className="markdown-content" style={{ maxHeight: '60vh', overflow: 'auto' }}>
              <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                components={{
                  h1: ({ children }) => {
                    const title = getNodeText(children).trim();
                    const showPlay = /^(\d+\.|create|query|update|delete)/i.test(title);
                    return (
                      <div className="section-heading-row">
                        <h3 style={{marginTop: '1.5rem', marginBottom: '0.5rem', fontWeight: 600}}>{children}</h3>
                        {showPlay && (
                          <button
                            className="section-play-btn tools-icon-btn"
                            title={`Execute: ${title}`}
                            aria-label={`Execute section: ${title}`}
                            onClick={() => openExecutionConsoleForSectionFromRaw(gitModal.content, title)}
                          >
                            <PlayFilledAlt size={16} />
                          </button>
                        )}
                      </div>
                    );
                  },
                  h2: ({ children }) => {
                    const title = getNodeText(children).trim();
                    const showPlay = /^(\d+\.|create|query|update|delete)/i.test(title);
                    return (
                      <div className="section-heading-row">
                        <h4 style={{marginTop: '1rem', marginBottom: '0.5rem', fontWeight: 600}}>{children}</h4>
                        {showPlay && (
                          <button
                            className="section-play-btn tools-icon-btn"
                            title={`Execute: ${title}`}
                            aria-label={`Execute section: ${title}`}
                            onClick={() => openExecutionConsoleForSectionFromRaw(gitModal.content, title)}
                          >
                            <PlayFilledAlt size={16} />
                          </button>
                        )}
                      </div>
                    );
                  },
                  h3: ({ children }) => {
                    const title = getNodeText(children).trim();
                    const normalizedTitle = sanitizeTitleText(title);
                    const showPlay = /^(create|query|update|delete)/i.test(normalizedTitle) || /^\d/.test(title.trim());
                    return (
                      <div className="section-heading-row">
                        <h5 style={{marginTop: '0.75rem', marginBottom: '0.25rem', fontWeight: 600}}>{children}</h5>
                        {showPlay && (
                          <button
                            className="section-play-btn tools-icon-btn"
                            title={`Execute: ${title}`}
                            aria-label={`Execute section: ${title}`}
                            onClick={() => openExecutionConsoleForSectionFromRaw(gitModal.content, title)}
                          >
                            <PlayFilledAlt size={16} />
                          </button>
                        )}
                      </div>
                    );
                  },
                  p({children, node, ...props}) {
                    const text = getNodeText(children).trim();
                    const looksLikeSection = /^\d+[\.\)]\s+\S+/.test(text) || /^\(\d+\)\s+\S+/.test(text) || /^\d+\s*[-–—:]\s+\S+/.test(text);
                    const normalized = sanitizeTitleText(text);
                    const startsWithVerb = /^(create|query|update|delete)/i.test(normalized);
                    const hasInlineCode = node && node.children && node.children.some(ch => ch.tagName === 'code');
                    if ((looksLikeSection || startsWithVerb) && !hasInlineCode) {
                      return (
                        <div className="section-heading-row" {...props}>
                          <p style={{margin: 0}}>{children}</p>
                          <button
                            className="section-play-btn tools-icon-btn"
                            title={`Execute: ${text}`}
                            aria-label={`Execute section: ${text}`}
                            onClick={() => openExecutionConsoleForSectionFromRaw(gitModal.content, text)}
                          >
                            <PlayFilledAlt size={16} />
                          </button>
                        </div>
                      );
                    }
                    return <p {...props}>{children}</p>;
                  },
                  code({ node, inline, className, children, ...props }) {
                    const language = className ? className.replace('language-', '') : '';
                    if (!inline) {
                      return (
                        <pre className="streaming-code-block" data-lang={language} {...props}>
                          <code>{String(children).replace(/\n$/, '')}</code>
                        </pre>
                      );
                    }
                    return (
                      <code className={`inline-code ${className || ''}`.trim()} {...props}>
                        {children}
                      </code>
                    );
                  },
                  pre({ children }) { return <>{children}</>; },
                  a: ({ href, children }) => (
                    <Link href={href || '#'} target="_blank" rel="noopener noreferrer">{children}</Link>
                  )
                }}
              >
                {gitModal.content}
              </ReactMarkdown>
            </div>
          )}
        </div>
      </Modal>
      {showExec && (
        <ExecutionConsole
          open={showExec}
          initialUrl={execResolvedRequest.url}
          initialMethod={execResolvedRequest.method}
          initialHeaders={execResolvedRequest.headers}
          initialParams={execResolvedRequest.params}
          initialBody={execResolvedRequest.body}
          instances={instances}
          selectedInstanceId={consoleInstanceId}
          defaultInstanceId={activeInstanceId}
          onInstanceChange={handleConsoleInstanceChange}
          onClose={closeExecutionConsole}
          source={execContext.source}
          actionTitle={execContext.actionTitle}
          username={execContext.username}
        />
      )}
    </div>
  );
});

export default CarbonChatInterface;
