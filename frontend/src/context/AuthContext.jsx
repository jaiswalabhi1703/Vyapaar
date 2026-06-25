import { createContext, useContext, useState } from 'react';
import api from '../api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [username, setUsername] = useState(localStorage.getItem('username'));
  const [role, setRole] = useState(localStorage.getItem('role'));

  async function login(user, password) {
    const { data } = await api.post('/auth/login', { username: user, password });
    persist(data);
  }

  async function register(user, password) {
    const { data } = await api.post('/auth/register', { username: user, password });
    persist(data);
  }

  function persist(data) {
    localStorage.setItem('token', data.token);
    localStorage.setItem('username', data.username);
    localStorage.setItem('role', data.role || 'USER');
    setUsername(data.username);
    setRole(data.role || 'USER');
  }

  function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    setUsername(null);
    setRole(null);
  }

  const isAdmin = role === 'ADMIN';

  return (
    <AuthContext.Provider value={{ username, role, isAdmin, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
