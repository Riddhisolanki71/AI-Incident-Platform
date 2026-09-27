import React, { createContext, useContext, useEffect, useState } from "react";
import * as api from "../api/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = localStorage.getItem("user");
    const token = localStorage.getItem("token");
    if (stored && token) {
      setUser(JSON.parse(stored));
    }
    setLoading(false);
  }, []);

  // If any API call comes back 401 (expired/invalid token), api.js clears
  // localStorage and fires this event. Clearing in-memory state too means
  // ProtectedRoute redirects to /login on the very next render instead of the
  // UI silently staying "logged in" while every request fails.
  useEffect(() => {
    function handleSessionExpired() {
      setUser(null);
    }
    window.addEventListener("auth:session-expired", handleSessionExpired);
    return () => window.removeEventListener("auth:session-expired", handleSessionExpired);
  }, []);

  function persistSession(authResponse) {
    const currentUser = {
      id: authResponse.userId,
      name: authResponse.name,
      email: authResponse.email,
      role: authResponse.role,
    };
    localStorage.setItem("token", authResponse.token);
    localStorage.setItem("user", JSON.stringify(currentUser));
    setUser(currentUser);
    return currentUser;
  }

  async function loginUser(credentials) {
    const response = await api.login(credentials);
    return persistSession(response);
  }

  async function registerUser(payload) {
    const response = await api.register(payload);
    return persistSession(response);
  }

  async function logoutUser() {
    try {
      await api.logout();
    } catch (e) {
      // Ignore network errors on logout; we clear local state regardless.
    }
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, loginUser, registerUser, logoutUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
