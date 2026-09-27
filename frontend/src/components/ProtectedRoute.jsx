import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

/**
 * Renders children only if the user is logged in and (optionally) has one of
 * the allowed roles. Otherwise redirects to login or a "not authorized" spot.
 */
export default function ProtectedRoute({ children, allowedRoles }) {
  const { user, loading } = useAuth();

  if (loading) return <div className="loading-state">Loading...</div>;
  if (!user) return <Navigate to="/login" replace />;

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    const fallback = user.role === "CUSTOMER" ? "/dashboard" : "/support";
    return <Navigate to={fallback} replace />;
  }

  return children;
}
