import React from 'react';
import { Navigate } from 'react-router-dom';

/**
 * Route guard component:
 * - If no token exists: redirects to that portal's login page (/dean/login or /employee/login).
 * - If user has the wrong role: redirects to the user's own valid dashboard.
 */
export default function RoleProtectedRoute({ requiredRole, children }) {
  const token = localStorage.getItem('token');
  const role = localStorage.getItem('role');

  // No active session token -> redirect to that portal's login page
  if (!token) {
    const loginPath = requiredRole === 'DEAN' ? '/dean/login' : '/employee/login';
    return <Navigate to={loginPath} replace />;
  }

  // If role doesn't match requiredRole -> redirect to user's own dashboard
  if (requiredRole && role !== requiredRole) {
    if (role === 'DEAN') {
      return <Navigate to="/dean/dashboard" replace />;
    } else if (role === 'EMPLOYEE') {
      return <Navigate to="/employee/dashboard" replace />;
    } else {
      return <Navigate to="/" replace />;
    }
  }

  return children;
}
