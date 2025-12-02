import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import {
  Button,
  TextInput,
  TextArea,
  InlineNotification,
  Select,
  SelectItem,
  CodeSnippet,
  Tag
} from '@carbon/react';
import { Close, Play, Add, Subtract } from '@carbon/icons-react';
import './ExecutionConsole.css';
import { executionHistoryAPI } from '../utils/api';

/*
 ExecutionConsole - lightweight Postman-like runner (not a full API testing tool).
 Allows user to compose and execute HTTP requests (GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS)
 with query params, headers, and raw JSON body. Uses fetch() directly from the browser.
 Dark theme styling aligned with existing black header (#161616).
*/

const VALID_HTTP_METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'];

function ExecutionConsole({
  open,
  onClose,
  initialUrl = '',
  initialMethod = '',
  initialHeaders = null,
  initialParams = null,
  initialBody = '',
  instances = [],
  selectedInstanceId = '',
  defaultInstanceId = '',
  onInstanceChange,
  username = '',
  source = 'console',
  actionTitle = 'Manual Request'
}) {
  const tryPrettyJson = (raw) => {
    if (!raw || typeof raw !== 'string') return raw || '';
    try {
      const parsed = JSON.parse(raw);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return raw;
    }
  };
  const normalizeMethod = (value) => {
    const upper = (value || '').toUpperCase();
    return VALID_HTTP_METHODS.includes(upper) ? upper : '';
  };

  const [method, setMethod] = useState(normalizeMethod(initialMethod));
  const [url, setUrl] = useState(initialUrl);
  const norm = (items) => (items || []).map(it => ({ id: it.id || Date.now()+Math.random(), key: it.key||'', value: it.value||'', enabled: it.enabled!==false }));
  const [params, setParams] = useState(initialParams && initialParams.length ? norm(initialParams) : [{ id: 1, key: '', value: '', enabled: true }]);
  const [headers, setHeaders] = useState(initialHeaders && initialHeaders.length ? norm(initialHeaders) : [{ id: 1, key: '', value: '', enabled: true }]);
  const [body, setBody] = useState(initialBody || '');
  // bodyMode: currently only 'raw-json' per requirement, extensible for future (e.g., 'raw-text','form-data')
  const [bodyMode, setBodyMode] = useState('raw-json');
  const [showBody, setShowBody] = useState(true);
  const [responseMeta, setResponseMeta] = useState(null); // { status, timeMs, size, ok }
  const [responseHeaders, setResponseHeaders] = useState([]); // [{key,value}]
  const [responseBody, setResponseBody] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState(null);
  const [autoFormatJson, setAutoFormatJson] = useState(true);
  const [instanceId, setInstanceId] = useState(selectedInstanceId || '');
  const safeActionTitle = (actionTitle && actionTitle.trim()) ? actionTitle.trim() : 'Manual Request';

  useEffect(() => {
    if (open) {
      // Refresh from initial props each time it opens
  setMethod(normalizeMethod(initialMethod));
      setUrl(initialUrl);
  setParams(initialParams && initialParams.length ? norm(initialParams) : [{ id: 1, key: '', value: '', enabled: true }]);
  setHeaders(initialHeaders && initialHeaders.length ? norm(initialHeaders) : [{ id: 1, key: '', value: '', enabled: true }]);
      // If method doesn't support body, ensure empty body
      const initMethod = normalizeMethod(initialMethod);
      if (['GET','HEAD','OPTIONS'].includes(initMethod)) {
        setBody('');
        setShowBody(false);
      } else {
        // Pretty print JSON body if applicable for better readability
        setBody(tryPrettyJson(initialBody || ''));
        setShowBody(true);
      }
      setInstanceId(selectedInstanceId || '');
    } else {
      // reset ephemeral response state when closing
      setResponseMeta(null);
      setResponseHeaders([]);
      setResponseBody('');
      setError(null);
      setIsSending(false);
    }
  }, [open, initialUrl, initialMethod, initialHeaders, initialParams, initialBody, selectedInstanceId]);

  useEffect(() => {
    if (!open) return;
    setInstanceId(selectedInstanceId || '');
  }, [selectedInstanceId, open]);

  // Whenever method changes, clear and hide body for GET/HEAD/OPTIONS
  useEffect(() => {
    if (!method) return;
    if (['GET','HEAD','OPTIONS'].includes(method)) {
      if (body) setBody('');
      if (showBody) setShowBody(false);
    }
  }, [method]);

  const selectedInstance = instances.find(inst => inst.id === instanceId) || null;
  const isDefault = (id) => !!defaultInstanceId && id === defaultInstanceId;

  const handleInstanceSelect = (event) => {
    const nextId = event.target.value;
    setInstanceId(nextId);
    if (onInstanceChange) {
      onInstanceChange(nextId);
    }
  };

  if (!open) return null;

  const updateRow = (type, id, field, value) => {
    const setter = type === 'param' ? setParams : setHeaders;
    const list = type === 'param' ? params : headers;
    setter(list.map(r => r.id === id ? { ...r, [field]: value } : r));
  };

  const addRow = (type) => {
    const setter = type === 'param' ? setParams : setHeaders;
    const list = type === 'param' ? params : headers;
    setter([...list, { id: Date.now() + Math.random(), key: '', value: '', enabled: true }]);
  };

  const removeRow = (type, id) => {
    const setter = type === 'param' ? setParams : setHeaders;
    const list = type === 'param' ? params : headers;
    if (list.length === 1) {
      // just clear
      setter([{ id: 1, key: '', value: '', enabled: true }]);
    } else {
      setter(list.filter(r => r.id !== id));
    }
  };

  const buildFinalUrl = () => {
    const activeParams = params.filter(p => p.enabled && p.key.trim());
    if (!activeParams.length) return url.trim();
    const q = new URLSearchParams();
    activeParams.forEach(p => q.append(p.key.trim(), p.value));
    const base = url.trim().replace(/[?&]$/, '');
    return base + (base.includes('?') ? '&' : '?') + q.toString();
  };

  // Method display component - read-only, extracted from chat
  const MethodDisplay = () => {
    const methodColor = method ? 'blue' : 'gray';
    const methodText = method || 'NO METHOD';
    return (
      <Tag type={methodColor} size="md" className="method-tag-display">
        {methodText}
      </Tag>
    );
  };

  const sendRequest = async () => {
    setError(null);
    setIsSending(true);
    setResponseMeta(null);
    setResponseBody('');
    setResponseHeaders([]);

    const requestUrl = buildFinalUrl();
    if (!requestUrl) {
      setError('URL required');
      setIsSending(false);
      return;
    }

    if (!method) {
      setError('HTTP Method not detected from chat. Please check the chat response format or contact support.');
      setIsSending(false);
      return;
    }

    // Validate URL format
  const canSendBody = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method);

    // compute active headers and params (avoid Temporal Dead Zone issues)
    const activeHeaders = headers.filter(h => h.enabled && h.key && h.key.trim());
    const requestParamsList = params.filter(p => p.enabled && p.key && p.key.trim()).map(p => ({ key: p.key.trim(), value: p.value }));

    // Pre-build execution record skeleton
    let executionData = {
      username: username || (typeof localStorage !== 'undefined' ? (JSON.parse(localStorage.getItem('authUser')||'{}').username || '') : ''),
      timestamp: new Date().toISOString(),
      source: source || 'console',
      instanceId: instanceId || '',
  actionTitle: safeActionTitle,
      method: method,
      url: requestUrl,
      requestHeaders: activeHeaders.map(h => ({ key: h.key.trim(), value: h.value })),
      requestParams: requestParamsList,
      requestBody: '',
      status: 'pending'
    };

    try {
      new URL(requestUrl);
    } catch (urlError) {
      setError(`Invalid URL format: ${requestUrl}`);
      setIsSending(false);
      return;
    }

  const requestHeaders = {};
  activeHeaders.forEach(h => { requestHeaders[h.key.trim()] = h.value; });

  let requestBody = '';
  if (canSendBody && body) {
      if (bodyMode === 'raw-json') {
        try {
          // Pre-validate JSON before sending to proxy
          const parsed = JSON.parse(body);
          if (!Object.keys(requestHeaders).some(k => k.toLowerCase() === 'content-type')) {
            requestHeaders['Content-Type'] = 'application/json';
          }
          requestBody = JSON.stringify(parsed);
        } catch (e) {
          requestBody = body; // Send as-is if not valid JSON
        }
      } else {
        requestBody = body;
      }
    }

    const start = performance.now();
    try {
      console.log('ExecutionConsole: Sending request to proxy:', {
        method: method,
        url: requestUrl,
        headers: requestHeaders,
        bodyLength: requestBody ? requestBody.length : 0
      });

      const proxyResponse = await fetch('/api/proxy', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          method: method,
          url: requestUrl,
          headers: requestHeaders,
          body: requestBody,
        }),
      });

      const timeMs = performance.now() - start;
      
      console.log('ExecutionConsole: Proxy response status:', proxyResponse.status);
      
      let proxyData;
      const rawResponseText = await proxyResponse.text();
      console.log('ExecutionConsole: Raw response (first 200 chars):', rawResponseText.substring(0, 200));
      if (!rawResponseText.trim()) {
        // Empty body (e.g. 204 No Content). Treat as successful envelope with no body.
        proxyData = {
          status: proxyResponse.status,
          statusText: proxyResponse.statusText,
          headers: Object.fromEntries(proxyResponse.headers.entries()),
          body: ''
        };
      } else {
        try {
          proxyData = JSON.parse(rawResponseText);
        } catch (parseError) {
          // If parse fails, assume rawResponseText is the target server body (plain text / HTML / etc.)
          console.warn('ExecutionConsole: Non-JSON proxy payload, using raw text body. Parse error:', parseError);
          proxyData = {
            status: proxyResponse.status,
            statusText: proxyResponse.statusText,
            headers: Object.fromEntries(proxyResponse.headers.entries()),
            body: rawResponseText
          };
        }
      }

      // Check if this is a proxy error (our proxy failed) vs target server error
      if (!proxyResponse.ok && !proxyData.status) {
        // This is a proxy error - our proxy failed
        const errorMsg = proxyData.error || proxyData.details || `Proxy error ${proxyResponse.status}`;
        throw new Error(errorMsg);
      }

      // For target server responses (including error responses), show the response
      // This is normal behavior - like Postman shows 400, 500 responses
      // Display the body exactly as returned. For 204/205 or any empty body, show nothing (no placeholder text).
      let formattedBody = (proxyData.body !== undefined && proxyData.body !== null) ? proxyData.body : '';
  if (autoFormatJson && proxyData.body) {
        try {
          const parsed = JSON.parse(proxyData.body);
          formattedBody = JSON.stringify(parsed, null, 2);
        } catch (e) { 
          // Not JSON, keep as is
          formattedBody = proxyData.body;
        }
      }
      
      const responseHeadersList = [];
      if (proxyData.headers && typeof proxyData.headers === 'object') {
        Object.entries(proxyData.headers).forEach(([k,v]) => {
          responseHeadersList.push({key: k, value: String(v)});
        });
      }

      const responseMetaObj = {
        status: `${proxyData.status || 'Unknown'} ${proxyData.statusText || ''}`.trim(),
        timeMs: Math.round(timeMs),
        size: proxyData.body ? proxyData.body.length : 0,
        ok: proxyData.status >= 200 && proxyData.status < 300,
      };
      setResponseMeta(responseMetaObj);
      setResponseHeaders(responseHeadersList);
      setResponseBody(formattedBody);

      // Persist execution history (success or target-server error included)
      const responseHeadersObj = Object.fromEntries(responseHeadersList.map(h => [h.key, h.value]));
      executionData = {
        ...executionData,
        status: responseMetaObj.ok ? 'success' : 'error',
        statusCode: proxyData.status,
        durationMs: Math.round(timeMs),
        requestBody: requestBody,
        responseHeaders: Object.entries(responseHeadersObj).map(([k,v]) => ({ key: k, value: String(v) })),
        responseBody: proxyData.body || ''
      };
      try { await executionHistoryAPI.saveExecution(executionData); } catch (e) { console.warn('Failed to save execution history:', e); }

    } catch (e) {
      console.error('ExecutionConsole request failed:', e);
      setError(e.message || 'Request failed');
      // Save error history
      const timeMs = performance.now() - start;
      executionData = {
        ...executionData,
        status: 'error',
        durationMs: Math.round(timeMs),
        requestBody: requestBody,
        errorMessage: e.message || 'Request failed',
        stackTrace: e.stack || ''
      };
      try { await executionHistoryAPI.saveExecution(executionData); } catch (err) { console.warn('Failed to save execution history on error:', err); }
    } finally {
      setIsSending(false);
    }
  };

  const canSendBody = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method);

  const overlay = (
    <div className="execution-console-overlay" role="dialog" aria-label="Execution Console">
      <div className="execution-console">
        <div className="exec-header">
          <div className="exec-title">
            <Play size={20} style={{ marginRight: '0.5rem' }} /> API Execution Console
            {responseMeta && (
              <span className="meta-badges">
                <Tag size="sm" type={responseMeta.ok ? 'green' : 'red'}>{responseMeta.status}</Tag>
                <Tag size="sm" type="cool-gray">{responseMeta.timeMs} ms</Tag>
                <Tag size="sm" type="teal">{responseMeta.size} chars</Tag>
              </span>
            )}
          </div>
          <button className="exec-close-btn" onClick={onClose} aria-label="Close execution console"><Close size={20} /></button>
        </div>

        <div className="exec-section request-builder">
          {instances.length > 0 && (
            <div className="instance-select-row">
              <Select
                id="exec-instance-select"
                className="instance-select"
                labelText="Instance"
                value={instanceId}
                onChange={handleInstanceSelect}
              >
                <SelectItem value="" text="Custom / no instance" />
                {instances.map(inst => {
                  const base = (inst.name && inst.name.trim()) ? inst.name : inst.url;
                  const label = isDefault(inst.id) ? `${base} (default)` : base;
                  return (
                    <SelectItem key={inst.id} value={inst.id} text={label} />
                  );
                })}
              </Select>
              {instanceId && selectedInstance && (
                <div className="instance-summary" title={selectedInstance.url}>
                  <Tag size="sm" type="blue">Using {selectedInstance.url}{isDefault(instanceId) ? ' (default)' : ''}</Tag>
                </div>
              )}
            </div>
          )}
          <div className="request-top-row">
            <div className="method-display" aria-label="HTTP method">
              <span className="method-label">Method</span>
              <MethodDisplay />
            </div>
            <TextInput id="request-url" labelText="URL" hideLabel placeholder="https://api.example.com/resource" value={url} onChange={e => setUrl(e.target.value)} className="url-input cds-dark-input" />
            <Button kind="primary" size="md" onClick={sendRequest} disabled={isSending || !method}> {isSending ? 'Sending...' : 'Send'} </Button>
          </div>
          {(/hostname/i.test(url)) && (
            <div className="hostname-warning">
              <InlineNotification
                kind="warning"
                lowContrast
                hideCloseButton
                title="Replace placeholder hostname"
                subtitle="Change 'hostname' in the URL to your actual server host before sending the request."
              />
            </div>
          )}
          {(/<your-apikey-value>/i.test(url) || 
            headers.some(h => h.enabled && (/<your-apikey-value>/i.test(h.key) || /<your-apikey-value>/i.test(h.value))) ||
            /<your-apikey-value>/i.test(body)) && (
            <div className="apikey-warning">
              <InlineNotification
                kind="warning"
                lowContrast
                hideCloseButton
                title="Replace API key placeholder"
                subtitle="Replace '<your-apikey-value>' with your actual API key before sending the request."
              />
            </div>
          )}

          <div className="row-groups">
            <SectionTable
              title="Query Params"
              rows={params}
              onAdd={() => addRow('param')}
              onRemove={(id) => removeRow('param', id)}
              onChange={(id, field, value) => updateRow('param', id, field, value)}
            />
            <SectionTable
              title="Headers"
              rows={headers}
              onAdd={() => addRow('header')}
              onRemove={(id) => removeRow('header', id)}
              onChange={(id, field, value) => updateRow('header', id, field, value)}
            />
          </div>

          {(() => { const nonBodyMethod = ['GET','HEAD','OPTIONS'].includes(method); return (
          <div className="body-editor" aria-disabled={nonBodyMethod}>
            <div className="body-header">
              <span>Body</span>
              <div className="body-controls">
                <select
                  aria-label="Body mode"
                  className="body-mode-select"
                  value={bodyMode}
                  onChange={e => setBodyMode(e.target.value)}
                  disabled={nonBodyMethod}
                >
                  <option value="raw-json">raw-json</option>
                </select>
                {bodyMode === 'raw-json' && (
                  <label className="inline-toggle">
                    <input type="checkbox" checked={autoFormatJson} onChange={() => setAutoFormatJson(v => !v)} disabled={nonBodyMethod} /> auto-format JSON
                  </label>
                )}
                <button
                  type="button"
                  className="body-toggle-btn"
                  onClick={() => setShowBody(s => !s)}
                  disabled={nonBodyMethod}
                  aria-label={showBody ? 'Hide body editor' : 'Show body editor'}
                >
                  {showBody ? 'Hide' : 'Show'}
                </button>
              </div>
            </div>
            {nonBodyMethod && (
              <div className="body-hint">Body is not applicable for {method} requests.</div>
            )}
            {showBody && !nonBodyMethod && (
              <>
                <TextArea
                  id="request-body"
                  labelText="Request Body"
                  hideLabel
                  value={body}
                  onChange={e => setBody(e.target.value)}
                  placeholder={bodyMode === 'raw-json' ? '{"name":"value"}' : 'Request body'}
                  rows={10}
                  className="raw-body-textarea"
                />
                <div className="body-hint">Body is only sent with POST, PUT, PATCH, DELETE. Current method: {method}{!canSendBody ? ' (will be ignored)' : ''}</div>
              </>
            )}
          </div>
          ); })()}
        </div>

        <div className="exec-section response-viewer">
          <h4>Response</h4>
          {error && (
            <InlineNotification kind="error" title="Error" subtitle={error} lowContrast onCloseButtonClick={() => setError(null)} />
          )}
          {!error && !responseMeta && (
            <div className="placeholder">Send a request to see the response.</div>
          )}
          {responseMeta && !error && (
            <div className="response-panels">
              <div className="response-headers">
                <h5>Headers</h5>
                {responseHeaders.length === 0 && <div className="placeholder small">No headers</div>}
                {responseHeaders.length > 0 && (
                  <ul className="headers-list">
                    {responseHeaders.map(h => (
                      <li key={h.key}><strong>{h.key}:</strong> {h.value}</li>
                    ))}
                  </ul>
                )}
              </div>
              <div className="response-body">
                <div className="response-body-header">
                  <h5>Body</h5>
                  {responseMeta && (
                    <div className="response-body-meta" aria-label="Response summary">
                      <span className={`status-chip ${responseMeta.ok ? 'ok' : 'error'}`}>{responseMeta.status}</span>
                      <span>{responseMeta.timeMs} ms</span>
                      <span>{responseMeta.size} chars</span>
                    </div>
                  )}
                </div>
                <div className="code-wrapper">
                  <CodeSnippet
                    type="multi"
                    feedback="Copied"
                    hideCopyButton={false}
                    style={{ maxHeight: '260px', overflow: 'auto' }}
                  >
                    {responseBody}
                  </CodeSnippet>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
      <div className="execution-console-backdrop" onClick={onClose} />
    </div>
  );

  // Render above any other modals
  return createPortal(overlay, document.body);
}

function SectionTable({ title, rows, onAdd, onRemove, onChange }) {
  return (
    <div className="table-section">
      <div className="section-header">
        <h5>{title}</h5>
        <Button kind="ghost" size="sm" onClick={onAdd} renderIcon={Add}>Add</Button>
      </div>
      <div className="kv-rows">
        {rows.map(r => (
          <div key={r.id} className="kv-row">
            <input
              className="kv-enable"
              type="checkbox"
              checked={r.enabled}
              onChange={e => onChange(r.id, 'enabled', e.target.checked)}
              aria-label="Enable row"
            />
            <TextInput
              id={title + '-key-' + r.id}
              labelText="Key"
              hideLabel
              placeholder="key"
              value={r.key}
              onChange={e => onChange(r.id, 'key', e.target.value)}
              className="kv-input key cds-dark-input"
            />
            <TextInput
              id={title + '-value-' + r.id}
              labelText="Value"
              hideLabel
              placeholder="value"
              value={r.value}
              onChange={e => onChange(r.id, 'value', e.target.value)}
              className="kv-input value cds-dark-input"
            />
            <button className="row-remove-btn" onClick={() => onRemove(r.id)} aria-label="Remove row">
              <Subtract size={16} />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

export default ExecutionConsole;
