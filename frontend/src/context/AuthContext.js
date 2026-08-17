import React, { createContext, useState, useEffect } from 'react';
import { authService } from '../services/auth';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadUser = async () => {
      const token = authService.getAuthToken();
      if (token) {
        try {
          // Decode token or fetch user info
          const userData = JSON.parse(atob(token.split('.')[1]));
          setUser({
            userId: userData.sub,
            email: userData.email,
            fullName: userData.fullName,
            role: userData.role
          });
        } catch (error) {
          console.error('Error loading user:', error);
          authService.logout();
        }
      }
      setLoading(false);
    };

    loadUser();
  }, []);

  const login = async (email, password) => {
    const response = await authService.login(email, password);
    authService.setAuthToken(response.token);
    setUser({
      userId: response.userId,
      email: response.email,
      fullName: response.fullName,
      role: response.role
    });
    return response;
  };

  const register = async (userData) => {
    const response = await authService.register(userData);
    authService.setAuthToken(response.token);
    setUser({
      userId: response.userId,
      email: response.email,
      fullName: response.fullName,
      role: response.role
    });
    return response;
  };

  const logout = async () => {
    await authService.logout();
    setUser(null);
  };

  const value = {
    user,
    login,
    register,
    logout,
    isAuthenticated: !!user,
    loading
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};