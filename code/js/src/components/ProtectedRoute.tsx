import React from "react";
import { Navigate } from "react-router";
import { useAuth } from "../context/AuthContext";

export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = useAuth();

  if (!token) {
    return <Navigate to="/home" replace />;
  }

  return <>{children}</>;
}