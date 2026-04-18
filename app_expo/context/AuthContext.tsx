import React, { createContext, useContext, useEffect, useState } from 'react';
import { authService } from '../services/authService';
import { userService } from '../services/userService';
import type { BackendUser } from '../services/types';

interface PendingSignup {
  email: string;
  name: string;
  password: string;
}

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: BackendUser | null;
  pendingSignup: PendingSignup | null;
  login: (email: string, password: string) => Promise<void>;
  requestOtp: (email: string) => Promise<void>;
  verifyOtp: (email: string, code: string, name?: string, password?: string) => Promise<void>;
  setPendingSignup: (payload: PendingSignup | null) => void;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [user, setUser] = useState<BackendUser | null>(null);
  const [pendingSignup, setPendingSignup] = useState<PendingSignup | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const token = await authService.initializeFromStorage();
        if (token) {
          const userData = await userService.getCurrentUser();
          setUser(userData);
          setIsAuthenticated(true);
        }
      } catch {
        await authService.logout();
      } finally {
        setIsLoading(false);
      }
    })();
  }, []);

  const login = async (email: string, password: string) => {
    await authService.login(email, password);
    const userData = await userService.getCurrentUser();
    setUser(userData);
    setPendingSignup(null);
    setIsAuthenticated(true);
  };

  const requestOtp = async (email: string) => {
    await authService.requestOtp(email);
  };

  const verifyOtp = async (email: string, code: string, name?: string, password?: string) => {
    await authService.verifyOtp(email, code, name, password);
    const userData = await userService.getCurrentUser();
    setUser(userData);
    setPendingSignup(null);
    setIsAuthenticated(true);
  };

  const logout = async () => {
    await authService.logout();
    setUser(null);
    setPendingSignup(null);
    setIsAuthenticated(false);
  };

  const refreshUser = async () => {
    try {
      const userData = await userService.getCurrentUser();
      setUser(userData);
    } catch {
      // ignore
    }
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated, isLoading, user, pendingSignup, login, requestOtp, verifyOtp, setPendingSignup, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
