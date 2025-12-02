import React, { useState, useEffect } from 'react';
import { Theme, TextInput, Button, InlineNotification, Select, SelectItem } from '@carbon/react';
import { ArrowRight } from '@carbon/icons-react';
import '@carbon/styles/css/styles.css';
import './Login.css';
import { userAPI } from '../utils/api';
import masLoginBackground from '../assets/mas-login-background.png';

function Login({ onLogin }) {
  const [username, setUsername] = useState('');
  const [product, setProduct] = useState(() => {
    try { return localStorage.getItem('selectedProduct') || 'Maximo'; } catch { return 'Maximo'; }
  });
  const [theme] = useState('g100'); // Force dark theme
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Prefill username if it was saved previously
    try {
      const savedUser = localStorage.getItem('rememberedUsername') || 'maxadmin';
      if (savedUser) {
        setUsername(savedUser);
      }
    } catch {}
    const el = document.getElementById('login-username');
    el && el.focus();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const name = username.trim();
    if (!name) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const user = await userAPI.login(name);
      try {
        localStorage.setItem('authUser', JSON.stringify({ username: user.username, id: user.id }));
        // For simplicity, we'll always "remember" the last username
        localStorage.setItem('rememberedUsername', user.username);
        localStorage.setItem('selectedProduct', product || 'Maximo');
      } catch {}
      onLogin && onLogin({ ...user, product: product || 'Maximo' });
    } catch (err) {
      setError(err.message || 'Login failed. Please try again.');
      setLoading(false);
    }
  };

  return (
    <Theme theme={theme}>
      <div className="mas-login-page" data-carbon-theme={theme}>
        {/* Left illustration pane */}
        <div 
          className="mas-login-left"
          style={{ backgroundImage: `url(${masLoginBackground})` }}
        ></div>

        {/* Right login form pane */}
        <div className="mas-login-right">
          <div className="mas-login-form-card">
            <h2 className="mas-login-title">Log in to IBM Maximo AI Assistant</h2>
            <p className="mas-login-subtitle">Continue with local account</p>

            {error && (
              <InlineNotification
                kind="error"
                title="Login Error"
                subtitle={error}
                lowContrast
                hideCloseButton={false}
                onCloseButtonClick={() => setError(null)}
                style={{ marginBottom: '1.5rem' }}
              />
            )}

            <form onSubmit={handleSubmit} autoComplete="off">
              <div style={{ marginBottom: '1.5rem' }}>
                <TextInput
                  id="login-username"
                  labelText="Username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  autoComplete="off"
                  disabled={loading}
                  className="mas-username-input"
                />
              </div>
              <div style={{ marginBottom: '1.5rem' }}>
                <Select
                  id="product-select"
                  labelText="Select Product"
                  value={product}
                  onChange={(e) => setProduct(e.target.value)}
                  disabled={loading}
                >
                  <SelectItem text="Maximo" value="Maximo" />
                  <SelectItem text="Tririga" value="Tririga" />
                  <SelectItem text="OMS" value="OMS" />
                </Select>
              </div>
              
              <Button
                type="submit"
                className="mas-login-button"
                renderIcon={ArrowRight}
                disabled={loading || !username}
              >
                Log in
              </Button>
            </form>
          </div>
        </div>
      </div>
    </Theme>
  );
}

export default Login;
