import api from './api';

export const authService = {
  login: async (email, password) => {
    const response = await api.post('/auth/login', { email, password });
    return response.data;
  },

  register: async (userData) => {
    const response = await api.post('/auth/register', userData);
    return response.data;
  },

  logout: async () => {
    const token = localStorage.getItem(process.env.REACT_APP_TOKEN_KEY);
    if (token) {
      await api.post('/auth/logout', {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
    }
    localStorage.removeItem(process.env.REACT_APP_TOKEN_KEY);
  },

  setAuthToken: (token) => {
    if (token) {
      localStorage.setItem(process.env.REACT_APP_TOKEN_KEY, token);
    } else {
      localStorage.removeItem(process.env.REACT_APP_TOKEN_KEY);
    }
  },

  getAuthToken: () => {
    return localStorage.getItem(process.env.REACT_APP_TOKEN_KEY);
  }
};

export const walletService = {
  getWalletByUserId: async (userId) => {
    const response = await api.get(`/wallets/user/${userId}`);
    return response.data;
  },

  getWalletById: async (walletId) => {
    const response = await api.get(`/wallets/${walletId}`);
    return response.data;
  }
};

// Money-moving calls accept an optional Idempotency-Key: retrying with the same key (and the
// same payload) returns the original result instead of moving money twice.
const idempotencyConfig = (idempotencyKey) =>
  idempotencyKey ? { headers: { 'Idempotency-Key': idempotencyKey } } : undefined;

export const transactionService = {
  deposit: async (data, idempotencyKey) => {
    const response = await api.post('/transactions/deposit', data, idempotencyConfig(idempotencyKey));
    return response.data;
  },

  withdraw: async (data, idempotencyKey) => {
    const response = await api.post('/transactions/withdraw', data, idempotencyConfig(idempotencyKey));
    return response.data;
  },

  transfer: async (data, idempotencyKey) => {
    const response = await api.post('/transactions/transfer', data, idempotencyConfig(idempotencyKey));
    return response.data;
  },

  getTransactions: async (walletId, page = 0, size = 20) => {
    const response = await api.get(`/transactions/wallet/${walletId}`, {
      params: { page, size }
    });
    return response.data;
  },

  getBalanceSummary: async (walletId) => {
    const response = await api.get(`/transactions/wallet/${walletId}/balance`);
    return response.data;
  },

  reverseTransaction: async (transactionId) => {
    const response = await api.post(`/transactions/${transactionId}/reverse`);
    return response.data;
  }
};