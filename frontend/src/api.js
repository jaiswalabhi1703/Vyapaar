import axios from 'axios';

// Empty/undefined => relative URLs (same-origin); nginx proxies /api and /auth to the gateway
// in the Docker build. The dev server sets VITE_API_BASE in .env to hit the gateway directly.
const baseURL = import.meta.env.VITE_API_BASE || '';

const api = axios.create({ baseURL });

// Attach the JWT to every request if we have one.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// On 401, drop the stale token so the UI bounces back to login.
api.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('username');
    }
    return Promise.reject(err);
  }
);

export default api;
