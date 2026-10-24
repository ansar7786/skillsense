import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import client from '../api/client';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => localStorage.getItem('skillsense_token'));
  const [user, setUser] = useState(() => {
    try {
      const saved = localStorage.getItem('skillsense_user');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });
  const [loading, setLoading] = useState(true);

  // Validate or restore session on mount
  useEffect(() => {
    const initAuth = async () => {
      const storedToken = localStorage.getItem('skillsense_token');
      if (storedToken) {
        try {
          const res = await client.get('/auth/me');
          setUser(res.data);
          localStorage.setItem('skillsense_user', JSON.stringify(res.data));
        } catch (err) {
          // Token invalid or expired
          localStorage.removeItem('skillsense_token');
          localStorage.removeItem('skillsense_user');
          setToken(null);
          setUser(null);
        }
      } else {
        setToken(null);
        setUser(null);
      }
      setLoading(false);
    };

    initAuth();
  }, []);

  const login = useCallback(async (collegeId, password) => {
    const response = await client.post('/auth/login', { collegeId, password });
    const { token: receivedToken, user: receivedUser } = response.data;

    localStorage.setItem('skillsense_token', receivedToken);
    localStorage.setItem('skillsense_user', JSON.stringify(receivedUser));

    setToken(receivedToken);
    setUser(receivedUser);

    return response.data;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('skillsense_token');
    localStorage.removeItem('skillsense_user');
    setToken(null);
    setUser(null);
  }, []);

  const changePassword = useCallback(async (oldPassword, newPassword) => {
    await client.post('/auth/change-password', { oldPassword, newPassword });
    // Update local user state mustChangePassword to false
    setUser((prev) => {
      if (!prev) return null;
      const updated = { ...prev, mustChangePassword: false };
      localStorage.setItem('skillsense_user', JSON.stringify(updated));
      return updated;
    });
  }, []);

  const value = {
    token,
    user,
    role: user?.role,
    mustChangePassword: !!user?.mustChangePassword,
    isAuthenticated: !!token && !!user,
    loading,
    login,
    logout,
    changePassword,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
