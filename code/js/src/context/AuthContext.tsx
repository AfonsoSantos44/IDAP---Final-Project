import React, { createContext, useContext, useState, useEffect } from "react";
import { userService } from "../services/userService";

interface AuthContextType {
  token: string | null;
  userId: string | null;
  login: (token: string, userId: string) => void;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [userId, setUserId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Check if user is already authenticated on mount (via cookies)
  useEffect(() => {
    const checkAuth = async () => {
      try {
        // Try to get user info - if cookie exists, this will succeed
        const userInfo = await userService.getMe();
        setToken("authenticated"); // Just a flag, real token is in cookie
        setUserId(String(userInfo.userId ?? userInfo.id));
      } catch (error) {
        // No valid cookie, user is not authenticated
        setToken(null);
        setUserId(null);
      } finally {
        setIsLoading(false);
      }
    };

    checkAuth();
  }, []);

  const login = (newToken: string, newUserId: string) => {
    // Token is stored in cookies by the backend, no need for localStorage
    setToken(newToken);
    setUserId(newUserId);
  };

  const logout = async () => {
    try {
      await userService.logout();
    } catch (error) {
      console.error("Logout error:", error);
    } finally {
      // Cookie is cleared by backend on logout
      setToken(null);
      setUserId(null);
    }
  };

  const isAuthenticated = !!token;

  return (
    <AuthContext.Provider value={{ token, userId, login, logout, isAuthenticated, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
