// API helper for user and history operations
const API_BASE = '/api';

export const userAPI = {
  login: async (username) => {
    const response = await fetch(`${API_BASE}/users/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username })
    });
    if (!response.ok) throw new Error('Login failed');
    return response.json();
  },

  register: async (username, email = '') => {
    const response = await fetch(`${API_BASE}/users/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, email })
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Registration failed');
    }
    return response.json();
  },

  getUser: async (username) => {
    const response = await fetch(`${API_BASE}/users/${username}`);
    if (!response.ok) throw new Error('Failed to fetch user');
    return response.json();
  },

  updateConfig: async (username, config) => {
    const response = await fetch(`${API_BASE}/users/${username}/config`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(config)
    });
    if (!response.ok) throw new Error('Failed to update config');
    return response.json();
  }
};

export const historyAPI = {
  saveHistory: async (historyData) => {
    console.log('saveHistory called with:', historyData);
    const response = await fetch(`${API_BASE}/history`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(historyData)
    });
    console.log('saveHistory response status:', response.status);
    if (!response.ok) {
      const errorText = await response.text();
      console.error('saveHistory error response:', errorText);
      throw new Error(`Failed to save history: ${response.status} - ${errorText}`);
    }
    const result = await response.json();
    console.log('saveHistory success:', result);
    return result;
  },

  getUserHistory: async (username) => {
    const response = await fetch(`${API_BASE}/history/${username}`);
    if (!response.ok) throw new Error('Failed to fetch history');
    return response.json();
  },

  getHistoryById: async (id) => {
    const response = await fetch(`${API_BASE}/history/session/${id}`);
    if (!response.ok) throw new Error('Failed to fetch history');
    return response.json();
  },

  deleteHistory: async (id) => {
    const response = await fetch(`${API_BASE}/history/${id}`, {
      method: 'DELETE'
    });
    if (!response.ok) throw new Error('Failed to delete history');
    return response.json();
  },

  deleteAllUserHistory: async (username) => {
    const response = await fetch(`${API_BASE}/history/user/${username}`, {
      method: 'DELETE'
    });
    if (!response.ok) throw new Error('Failed to delete history');
    return response.json();
  },

  countUserHistory: async (username) => {
    const response = await fetch(`${API_BASE}/history/count/${username}`);
    if (!response.ok) throw new Error('Failed to count history');
    return response.json();
  }
};

export const executionHistoryAPI = {
  saveExecution: async (executionData) => {
    const response = await fetch(`${API_BASE}/execution-history`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(executionData)
    });
    if (!response.ok) throw new Error('Failed to save execution');
    return response.json();
  },

  getUserExecutions: async (username) => {
    const response = await fetch(`${API_BASE}/execution-history/${username}`);
    if (!response.ok) {
      let msg = `Failed to load execution history (${response.status})`;
      try {
        const text = await response.text();
        msg += `: ${text}`;
      } catch (e) {
        // ignore
      }
      throw new Error(msg);
    }
    return response.json();
  },

  getById: async (id) => {
    const response = await fetch(`${API_BASE}/execution-history/id/${id}`);
    if (!response.ok) throw new Error('Failed to load execution detail');
    return response.json();
  },

  deleteById: async (id) => {
    const response = await fetch(`${API_BASE}/execution-history/${id}`, { method: 'DELETE' });
    if (!response.ok) throw new Error('Failed to delete execution');
    return response.json();
  },

  deleteAllForUser: async (username) => {
    const response = await fetch(`${API_BASE}/execution-history/user/${username}`, { method: 'DELETE' });
    if (!response.ok) throw new Error('Failed to delete all executions');
    return response.json();
  },

  countForUser: async (username) => {
    const response = await fetch(`${API_BASE}/execution-history/count/${username}`);
    if (!response.ok) throw new Error('Failed to count executions');
    return response.json();
  }
};
