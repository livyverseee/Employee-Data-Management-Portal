import React from 'react';
import { Navigate } from 'react-router-dom';

/**
 * Route guard component:
 * - If no token exists: redirects to /login.
 * - If user has the wrong role: redirects to the user's own valid dashboard.
 */
export default function RoleProtectedRoute({ requiredRole, children }) {
  const token = localStorage.getItem('token');
  const role = localStorage.getItem('role');

  // No active session token -> go to login
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  // If a specific role is required and user has different role -> redirect to their own dashboard
  if (requiredRole && role !== requiredRole) {
    if (role === 'DEAN') {
      return <Navigate to="/dean/dashboard" replace />;
    } else if (role === 'EMPLOYEE') {
      return <Navigate to="/employee/dashboard" replace />;
    } else {
      return <Navigate to="/login" replace />;
    }
  }

  return children;
}
