import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi, getAuthToken, getStoredUser, setAuthToken, setStoredUser, removeAuth } from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(getStoredUser());
  const [token, setToken] = useState(getAuthToken());
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const handleUnauthorized = () => {
      setUser(null);
      setToken(null);
      removeAuth();
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);

    // Verify token validity on load
    if (token) {
      authApi.getCurrentUser()
        .then(profile => {
          setUser(profile);
          setStoredUser(profile);
        })
        .catch(() => {
          handleUnauthorized();
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }

    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, [token]);

  const login = async (credentials) => {
    const res = await authApi.login(credentials);
    setToken(res.token);
    setAuthToken(res.token);

    const userObj = {
      id: res.id,
      email: res.email,
      firstName: res.firstName,
      lastName: res.lastName,
      roles: res.roles,
      accountStatus: res.accountStatus
    };

    setUser(userObj);
    setStoredUser(userObj);
    return userObj;
  };

  const register = async (userData) => {
    return await authApi.register(userData);
  };

  const logout = async () => {
    try {
      if (token) {
        await authApi.logout();
      }
    } catch (e) {
      // Ignore network errors on logout
    } finally {
      setUser(null);
      setToken(null);
      removeAuth();
    }
  };

  const hasRole = (roleName) => {
    if (!user || !user.roles) return false;
    const formatted = roleName.startsWith('ROLE_') ? roleName : `ROLE_${roleName}`;
    return user.roles.includes(formatted);
  };

  const getPrimaryRole = () => {
    if (!user || !user.roles || user.roles.length === 0) return null;
    if (user.roles.includes('ROLE_ADMIN')) return 'ADMIN';
    if (user.roles.includes('ROLE_UNDERWRITER')) return 'UNDERWRITER';
    return 'BORROWER';
  };

  const getDashboardRoute = () => {
    const role = getPrimaryRole();
    if (role === 'ADMIN') return '/dashboard/admin';
    if (role === 'UNDERWRITER') return '/dashboard/underwriter';
    return '/dashboard/borrower';
  };

  return (
    <AuthContext.Provider value={{
      user,
      token,
      loading,
      login,
      register,
      logout,
      hasRole,
      getPrimaryRole,
      getDashboardRoute,
      isAuthenticated: !!token && !!user
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
