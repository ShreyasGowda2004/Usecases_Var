import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  Header,
  HeaderContainer,
  HeaderName,
  HeaderGlobalBar,
  HeaderGlobalAction,
  SkipToContent,
  Content,
  Theme,
  Modal,
  TextInput,
  Button,
  ToastNotification
} from '@carbon/react';
import {
  Light,
  Asleep,
  Settings,
  Add,
  Time,
  LogoGithub
} from '@carbon/icons-react';
import CarbonChatInterface from './CarbonChatInterface';
import Login from './Login';
import './CarbonApp.css';
import ExecutionHistory from './ExecutionHistory';
import { v4 as uuidv4 } from 'uuid';
import { userAPI } from '../utils/api';

const ensureHttpUrl = (value) => {
  const trimmed = (value || '').trim();
  if (!trimmed) return '';
  const withProtocol = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
  try {
    const parsed = new URL(withProtocol);
    return parsed.origin;
  } catch {
    return withProtocol;
  }
};

const cloneInstances = (list = []) => list.map(inst => ({
  id: inst.id || uuidv4(),
  name: inst.name || '',
  url: inst.url || '',
  apiKey: inst.apiKey || ''
}));

const normalizeInstances = (list = []) => cloneInstances(list)
  .map(inst => ({
    ...inst,
    name: inst.name.trim(),
    url: ensureHttpUrl(inst.url),
    apiKey: inst.apiKey.trim()
  }))
  .filter(inst => inst.url || inst.apiKey);

const createEmptyInstance = () => ({
  id: uuidv4(),
  name: '',
  url: '',
  apiKey: ''
});

// Memoized row for editing a single instance to prevent input remounts/focus loss
const InstanceEditorRow = React.memo(function InstanceEditorRow({
  inst,
  idx,
  context, // 'setup' | 'settings'
  isDefault,
  onSetDefault,
  onRemove,
  onChange
}) {
  const handleNameChange = useCallback((e) => onChange(inst.id, 'name', e.target?.value || '', context), [inst.id, onChange, context]);
  const handleUrlChange = useCallback((e) => onChange(inst.id, 'url', e.target?.value || '', context), [inst.id, onChange, context]);
  const handleKeyChange = useCallback((e) => onChange(inst.id, 'apiKey', e.target?.value || '', context), [inst.id, onChange, context]);

  return (
    <div key={inst.id} style={{ border: '1px solid var(--cds-border-subtle, #393939)', borderRadius: '0.5rem', padding: '1rem', display: 'grid', gap: '0.75rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.75rem' }}>
        <span style={{ fontWeight: 500 }}>Instance {idx + 1}</span>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <Button
            size="sm"
            kind={isDefault ? 'primary' : 'ghost'}
            onClick={() => onSetDefault(inst.id)}
          >
            {isDefault ? 'Default' : 'Set default'}
          </Button>
          {onRemove && (
            <Button size="sm" kind="danger--ghost" onClick={() => onRemove(inst.id)}>
              Remove
            </Button>
          )}
        </div>
      </div>
      <TextInput
        id={`instance-name-${context}-${inst.id}`}
        labelText="Display name (optional)"
        placeholder="e.g. Production"
        value={inst.name || ''}
        onChange={handleNameChange}
      />
      <TextInput
        id={`instance-url-${context}-${inst.id}`}
        labelText="Instance URL"
        placeholder="https://your-instance.example.com"
        value={inst.url || ''}
        onChange={handleUrlChange}
      />
      <TextInput
        id={`instance-key-${context}-${inst.id}`}
        type="password"
        labelText="API Key"
        placeholder="Enter API key"
        value={inst.apiKey || ''}
        onChange={handleKeyChange}
      />
    </div>
  );
});

function CarbonApp() {
  const [currentTheme, setCurrentTheme] = useState(() => {
    return localStorage.getItem('carbonTheme') || 'white';
  });
  const [authUser, setAuthUser] = useState(() => {
    try { const raw = localStorage.getItem('authUser'); return raw ? JSON.parse(raw) : null; } catch { return null; }
  });
  const [product, setProduct] = useState(() => {
    try { return localStorage.getItem('selectedProduct') || 'Maximo'; } catch { return 'Maximo'; }
  });
  const [instanceModalOpen, setInstanceModalOpen] = useState(false); // settings modal (dashboard)
  const [setupModalOpen, setSetupModalOpen] = useState(false); // first-time prompt
  const [instances, setInstances] = useState([]);
  const [draftInstances, setDraftInstances] = useState([createEmptyInstance()]);
  const [activeInstanceId, setActiveInstanceId] = useState('');
  const [draftActiveInstanceId, setDraftActiveInstanceId] = useState('');
  const [notice, setNotice] = useState(null);
  const [historyModalOpen, setHistoryModalOpen] = useState(false);
  const [githubModalOpen, setGithubModalOpen] = useState(false);
  const [githubForm, setGithubForm] = useState(() => {
    try { return JSON.parse(localStorage.getItem('githubConfig')) || { baseurl: '', owner: '', repo: '', branch: '', token: '' }; } catch { return { baseurl: '', owner: '', repo: '', branch: '', token: '' }; }
  });
  const lastEditedFieldRef = useRef(null);
  const chatRef = useRef(null);

  const getInputDomId = useCallback((context, field, id) => {
    const suffix = `${context}-${id}`;
    switch (field) {
      case 'name':
        return `instance-name-${suffix}`;
      case 'url':
        return `instance-url-${suffix}`;
      case 'apiKey':
        return `instance-key-${suffix}`;
      default:
        return `instance-${field}-${suffix}`;
    }
  }, []);

  const showNotice = (message, duration = 3000) => {
    setNotice(message);
    if (message) {
      window.setTimeout(() => setNotice(null), duration);
    }
  };

  // Load user config from MongoDB when auth user changes
  useEffect(() => {
    // Sync selected product when user logs in
    try { setProduct(localStorage.getItem('selectedProduct') || 'Maximo'); } catch {}

    if (!authUser) return;
    
    const loadUserConfig = async () => {
      try {
        const user = await userAPI.getUser(authUser.username);
        let stored = [];
        if (Array.isArray(user.instances)) {
          stored = normalizeInstances(user.instances);
        }
        const defaultId = user.defaultInstanceId && stored.some(inst => inst.id === user.defaultInstanceId)
          ? user.defaultInstanceId
          : (stored[0]?.id || '');
        const draftList = stored.length ? cloneInstances(stored) : [createEmptyInstance()];
        const draftDefaultId = draftList.some(inst => inst.id === defaultId) ? defaultId : (draftList[0]?.id || '');
        setInstances(stored);
        setDraftInstances(draftList);
        setActiveInstanceId(defaultId);
        setDraftActiveInstanceId(draftDefaultId);
        setSetupModalOpen(stored.length === 0);
      } catch (error) {
        console.error('Failed to load user config:', error);
        // Fallback to empty state
        const blank = createEmptyInstance();
        setInstances([]);
        setDraftInstances([blank]);
        setActiveInstanceId('');
        setDraftActiveInstanceId(blank.id);
        setSetupModalOpen(true);
      }
    };
    
    loadUserConfig();
  }, [authUser]);

  useEffect(() => {
    setDraftActiveInstanceId(prev => {
      if (prev && draftInstances.some(inst => inst.id === prev)) return prev;
      return draftInstances[0]?.id || '';
    });
  }, [draftInstances]);

  const persistConfigList = async (list = [], defaultId = '') => {
    if (!authUser) return;
    try {
      await userAPI.updateConfig(authUser.username, {
        instances: list,
        defaultInstanceId: defaultId
      });
    } catch (error) {
      console.error('Failed to persist config:', error);
      showNotice('Failed to save settings', 4000);
    }
  };

  const applyInstances = (normalizedList, preferredId) => {
    if (!normalizedList.length) {
      const blank = createEmptyInstance();
      setInstances([]);
      setDraftInstances([blank]);
      setActiveInstanceId('');
      setDraftActiveInstanceId(blank.id);
      persistConfigList([], '');
      return { savedCount: 0, defaultId: '' };
    }
    const fallbackId = normalizedList.some(inst => inst.id === preferredId) ? preferredId : normalizedList[0].id;
    const draftList = cloneInstances(normalizedList);
    setInstances(normalizedList);
    setDraftInstances(draftList);
    setActiveInstanceId(fallbackId);
    setDraftActiveInstanceId(fallbackId);
    persistConfigList(normalizedList, fallbackId);
    return { savedCount: normalizedList.length, defaultId: fallbackId };
  };

  const handleSaveFromSetup = () => {
    const normalized = normalizeInstances(draftInstances);
    const { savedCount } = applyInstances(normalized, draftActiveInstanceId);
    setSetupModalOpen(false);
    if (savedCount) {
      showNotice(savedCount > 1 ? 'Instances saved.' : 'Instance saved.');
    } else {
      showNotice('No instances saved. You can add them later from settings.');
    }
  };

  const handleSkipSetup = () => {
    setSetupModalOpen(false);
  };

  const handleSaveInSettings = () => {
    const normalized = normalizeInstances(draftInstances);
    applyInstances(normalized, draftActiveInstanceId);
    setInstanceModalOpen(false);
    showNotice('Settings updated.');
  };

  const handleClearInstances = async () => {
    if (!authUser) return;
    try {
      await userAPI.updateConfig(authUser.username, {
        instances: [],
        defaultInstanceId: ''
      });
      const blank = createEmptyInstance();
      setInstances([]);
      setDraftInstances([blank]);
      setActiveInstanceId('');
      setDraftActiveInstanceId(blank.id);
      showNotice('All instances removed.');
    } catch (error) {
      console.error('Failed to clear instances:', error);
      showNotice('Failed to clear instances', 4000);
    }
  };

  const handleOpenSettings = () => {
    const draftList = instances.length ? cloneInstances(instances) : [createEmptyInstance()];
    setDraftInstances(draftList);
    setDraftActiveInstanceId(prev => {
      if (!prev) {
        return activeInstanceId || draftList[0]?.id || '';
      }
      const hasPrev = draftList.some(inst => inst.id === prev);
      if (hasPrev) return prev;
      return activeInstanceId && draftList.some(inst => inst.id === activeInstanceId)
        ? activeInstanceId
        : (draftList[0]?.id || '');
    });
    setInstanceModalOpen(true);
  };

  const handleDraftChange = useCallback((id, field, value, context) => {
    const nextValue = typeof value === 'string' ? value : '';
    lastEditedFieldRef.current = { id, field, context };
    setDraftInstances(prev => prev.map(inst => inst.id === id ? { ...inst, [field]: nextValue } : inst));
  }, []);

  useEffect(() => {
    if (!lastEditedFieldRef.current) return;
    const { id, field, context } = lastEditedFieldRef.current;
    const targetId = getInputDomId(context || 'settings', field, id);
    window.requestAnimationFrame(() => {
      const el = document.getElementById(targetId);
      if (el) {
        const pos = el.value.length;
        el.focus();
        el.setSelectionRange?.(pos, pos);
      }
    });
    lastEditedFieldRef.current = null;
  }, [draftInstances, getInputDomId]);

  const handleAddDraftInstance = useCallback(() => {
    setDraftInstances(prev => [...prev, createEmptyInstance()]);
  }, []);

  const handleRemoveDraftInstance = useCallback((id) => {
    setDraftInstances(prev => {
      const next = prev.filter(inst => inst.id !== id);
      if (!next.length) {
        const blank = createEmptyInstance();
        setDraftActiveInstanceId(blank.id);
        return [blank];
      }
      setDraftActiveInstanceId(current => {
        if (current && next.some(inst => inst.id === current)) return current;
        return next[0].id;
      });
      return next;
    });
  }, []);

  const handleSetDraftDefault = useCallback((id) => {
    setDraftActiveInstanceId(id);
  }, []);

  const handleSelectActiveInstance = (id) => {
    if (!id) {
      setActiveInstanceId('');
      setDraftActiveInstanceId('');
      persistConfigList(instances, '');
      return;
    }
    const has = instances.some(inst => inst.id === id);
    const nextId = has ? id : (instances[0]?.id || '');
    setActiveInstanceId(nextId);
    setDraftActiveInstanceId(nextId);
    if (nextId) {
      persistConfigList(instances, nextId);
    } else {
      persistConfigList(instances, '');
    }
  };

  const themes = [
  { value: 'white', label: 'White' },
  { value: 'g90', label: 'Gray 90' }
  ];

  useEffect(() => {
    localStorage.setItem('carbonTheme', currentTheme);
  }, [currentTheme]);

  const toggleTheme = () => {
    const currentIndex = themes.findIndex(theme => theme.value === currentTheme);
    const nextIndex = (currentIndex + 1) % themes.length;
    setCurrentTheme(themes[nextIndex].value);
  };

  const handleOpenGithub = () => {
    // Load latest from localStorage
    try {
      const saved = JSON.parse(localStorage.getItem('githubConfig'));
      if (saved && typeof saved === 'object') {
        setGithubForm({ baseurl: saved.baseurl || '', owner: saved.owner || '', repo: saved.repo || '', branch: saved.branch || '', token: saved.token || '' });
      }
    } catch {}
    setGithubModalOpen(true);
  };

  const handleSaveGithub = () => {
    try {
      const payload = {
        baseurl: (githubForm.baseurl || '').trim(),
        owner: (githubForm.owner || '').trim(),
        repo: (githubForm.repo || '').trim(),
        branch: (githubForm.branch || '').trim(),
        token: (githubForm.token || '').trim(),
      };
      localStorage.setItem('githubConfig', JSON.stringify(payload));
      setGithubModalOpen(false);
      showNotice('GitHub settings saved locally.');
    } catch (e) {
      showNotice('Failed to save GitHub settings');
    }
  };

  const productTitle = `${product || 'Maximo'} AI Assistant`;

  return (
    <Theme theme={currentTheme}>
      <div className="carbon-app" data-carbon-theme={currentTheme}>
        {/* Basic login gate: require a username in localStorage */}
        {!authUser ? (
          <Login onLogin={(user) => setAuthUser(user)} />
        ) : (
          <HeaderContainer
            render={() => (
              <>
                <Theme theme="g100">
                  <Header aria-label="AI Chatbot">
                    <SkipToContent />
                    <HeaderName href="#" prefix="IBM">
                      {productTitle}&nbsp;&nbsp;&nbsp;&nbsp;<span style={{ color: '#8d8d8d' }}>|</span>&nbsp;&nbsp;<span style={{ color: '#4589ff' }}>TESTING</span>
                    </HeaderName>
                    <HeaderGlobalBar>
                      {/* Instance details (dashboard) icon */}
                      <HeaderGlobalAction
                        aria-label="Instance details"
                        tooltipAlignment="end"
                        onClick={handleOpenSettings}
                        title="Instance details"
                      >
                        <Settings size={20} />
                      </HeaderGlobalAction>
                      {/* GitHub settings for Tririga/OMS only */}
                      {product && product !== 'Maximo' && (
                        <HeaderGlobalAction
                          aria-label="GitHub settings"
                          tooltipAlignment="end"
                          onClick={handleOpenGithub}
                          title="GitHub settings"
                        >
                          <LogoGithub size={20} />
                        </HeaderGlobalAction>
                      )}
                      <HeaderGlobalAction
                        aria-label="Execution history"
                        tooltipAlignment="end"
                        onClick={() => setHistoryModalOpen(true)}
                        title="Execution history"
                      >
                        <Time size={20} />
                      </HeaderGlobalAction>
                      <HeaderGlobalAction
                        aria-label="Toggle theme"
                        onClick={toggleTheme}
                        tooltipAlignment="end"
                      >
                        {currentTheme === 'white' ? (
                          <Asleep size={20} />
                        ) : (
                          <Light size={20} />
                        )}
                      </HeaderGlobalAction>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginLeft: '0.5rem', color: '#c6c6c6', fontSize: '0.875rem', height: '3rem' }}>
                        <span title="Signed in user">{authUser?.username}</span>
                        <button
                          onClick={() => { try { localStorage.removeItem('authUser'); } catch {}; setAuthUser(null); }}
                          className="cds--btn cds--btn--ghost cds--btn--sm"
                          style={{ padding: '0.5rem 0.75rem', height: 'auto' }}
                        >
                          Logout
                        </button>
                      </div>
                    </HeaderGlobalBar>
                  </Header>
                </Theme>
                <Content className="carbon-content">
                  <CarbonChatInterface
                    ref={chatRef}
                    authUser={authUser}
                    instances={instances}
                    activeInstanceId={activeInstanceId}
                    onActiveInstanceChange={handleSelectActiveInstance}
                    product={product || 'Maximo'}
                  />
                </Content>
                {/* Toast notification */}
                {notice && (
                  <div style={{ position: 'fixed', right: 16, bottom: 16, zIndex: 1000 }}>
                    <ToastNotification
                      kind="success"
                      title="Success"
                      subtitle={notice}
                      timeout={3000}
                      onCloseButtonClick={() => setNotice(null)}
                    />
                  </div>
                )}
                {/* Execution History Modal */}
                <ExecutionHistory
                  open={historyModalOpen}
                  onClose={() => setHistoryModalOpen(false)}
                  username={authUser?.username}
                  onRerun={(execution) => {
                    if (execution) {
                      // Close modal first, then open console on next tick to avoid z-index/focus issues
                      setHistoryModalOpen(false);
                      setTimeout(() => {
                        chatRef.current?.openExecutionConsoleFromHistory?.(execution);
                      }, 0);
                    }
                  }}
                />
                {/* First-time setup modal */}
                <Modal
                  open={setupModalOpen}
                  modalHeading="Connect your instance (optional)"
                  primaryButtonText="Save & Continue"
                  secondaryButtonText="Skip for now"
                  onRequestClose={handleSkipSetup}
                  onRequestSubmit={handleSaveFromSetup}
                  preventCloseOnClickOutside
                >
                  <div style={{ display: 'grid', gap: '1rem' }}>
                    <p style={{ margin: 0, color: 'var(--cds-text-secondary)' }}>
                      Enter one or more instances to enable authenticated actions. You can manage these later from the dashboard settings icon.
                    </p>
                    {draftInstances.map((inst, idx) => (
                      <InstanceEditorRow
                        key={inst.id}
                        inst={inst}
                        idx={idx}
                        context="setup"
                        isDefault={draftActiveInstanceId === inst.id}
                        onSetDefault={handleSetDraftDefault}
                        onRemove={draftInstances.length > 1 ? handleRemoveDraftInstance : undefined}
                        onChange={handleDraftChange}
                      />
                    ))}
                    <Button kind="ghost" size="sm" onClick={handleAddDraftInstance} renderIcon={Add}>
                      Add another instance
                    </Button>
                  </div>
                </Modal>
                {/* Dashboard settings modal */}
                <Modal
                  open={instanceModalOpen}
                  modalHeading="Instance details"
                  primaryButtonText="Save"
                  secondaryButtonText="Close"
                  onRequestClose={() => setInstanceModalOpen(false)}
                  onRequestSubmit={handleSaveInSettings}
                  size="lg"
                >
                  <div style={{ display: 'grid', gap: '1rem' }}>
                    <p style={{ margin: 0, color: 'var(--cds-text-secondary)' }}>
                      Manage the instances available to this user. The selected default is used for Execution Console pre-fill.
                    </p>
                    {draftInstances.map((inst, idx) => (
                      <InstanceEditorRow
                        key={inst.id}
                        inst={inst}
                        idx={idx}
                        context="settings"
                        isDefault={draftActiveInstanceId === inst.id}
                        onSetDefault={handleSetDraftDefault}
                        onRemove={draftInstances.length > 1 ? handleRemoveDraftInstance : undefined}
                        onChange={handleDraftChange}
                      />
                    ))}
                    <Button kind="ghost" size="sm" onClick={handleAddDraftInstance} renderIcon={Add}>
                      Add another instance
                    </Button>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div style={{ color: 'var(--cds-text-secondary)', fontSize: '0.85rem' }}>
                        Saved per user: {authUser?.username}
                      </div>
                      <Button kind="danger--tertiary" size="sm" onClick={handleClearInstances}>
                        Remove all
                      </Button>
                    </div>
                  </div>
                </Modal>

                {/* GitHub settings modal (local only) */}
                <Modal
                  open={githubModalOpen}
                  modalHeading="GitHub Settings"
                  primaryButtonText="Save"
                  secondaryButtonText="Cancel"
                  onRequestClose={() => setGithubModalOpen(false)}
                  onRequestSubmit={handleSaveGithub}
                  size="lg"
                >
                  <div style={{ display: 'grid', gap: '1rem' }}>
                    <p style={{ margin: 0, color: 'var(--cds-text-secondary)' }}>
                      Provide GitHub details for this product. These are stored locally in your browser.
                    </p>
                    <TextInput
                      id="gh-baseurl"
                      labelText="GitHub Base URL"
                      placeholder="https://github.ibm.com/api/v3"
                      value={githubForm.baseurl}
                      onChange={(e) => setGithubForm(f => ({ ...f, baseurl: e.target.value }))}
                    />
                    <TextInput
                      id="gh-owner"
                      labelText="Owner"
                      placeholder="e.g. my-org"
                      value={githubForm.owner}
                      onChange={(e) => setGithubForm(f => ({ ...f, owner: e.target.value }))}
                    />
                    <TextInput
                      id="gh-repo"
                      labelText="Repository"
                      placeholder="e.g. knowledge-center"
                      value={githubForm.repo}
                      onChange={(e) => setGithubForm(f => ({ ...f, repo: e.target.value }))}
                    />
                    <TextInput
                      id="gh-branch"
                      labelText="Branch (optional)"
                      placeholder="e.g. main"
                      value={githubForm.branch}
                      onChange={(e) => setGithubForm(f => ({ ...f, branch: e.target.value }))}
                    />
                    <TextInput
                      id="gh-token"
                      type="password"
                      labelText="GitHub Token"
                      placeholder="Enter token"
                      value={githubForm.token}
                      onChange={(e) => setGithubForm(f => ({ ...f, token: e.target.value }))}
                    />
                  </div>
                </Modal>
              </>
            )}
          />
        )}
      </div>
    </Theme>
  );
}

export default CarbonApp;
