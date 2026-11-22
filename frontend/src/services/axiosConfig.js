// frontend/src/services/axiosConfig.js
import axios from 'axios';
import { toast } from 'react-toastify';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const axiosInstance = axios.create({
  baseURL: API_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(process.env.REACT_APP_TOKEN_KEY);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    const { response } = error;

    if (response) {
      // Handle specific status codes
      switch (response.status) {
        case 401: {
          // A wrong password on the login/register form is also a 401, but it is not an expired
          // session: show the server's message and stay on the page instead of reloading it.
          const url = error.config?.url || '';
          if (url.includes('/auth/login') || url.includes('/auth/register')) {
            toast.error(response.data?.message || 'Invalid email or password');
            break;
          }
          localStorage.removeItem(process.env.REACT_APP_TOKEN_KEY);
          toast.error('Session expired. Please login again.');
          if (window.location.pathname !== '/login') {
            window.location.href = '/login';
          }
          break;
        }
        case 403:
          toast.error('You do not have permission to perform this action');
          break;
        case 429: {
          // Rate limited (login/register). Not an auth failure: keep the session, show the wait.
          const retryAfter = response.headers?.['retry-after'];
          toast.error(
            response.data?.message ||
              `Too many attempts. Try again in ${retryAfter || 'a few'} seconds.`
          );
          break;
        }
        case 404:
          toast.error('Resource not found');
          break;
        case 500:
          toast.error('Internal server error. Please try again later.');
          break;
        default:
          // Show specific error message if available
          if (response.data?.message) {
            toast.error(response.data.message);
          }
      }
    } else if (error.request) {
      toast.error('Network error. Please check your connection.');
    } else {
      toast.error('An unexpected error occurred.');
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;