import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import Dashboard from './pages/Dashboard';
import RoleProtectedRoute from './components/RoleProtectedRoute';

/**
 * Root redirect handler for path "/":
 * - If user is logged in as DEAN -> /dean/dashboard
 * - If user is logged in as EMPLOYEE -> /employee/dashboard
 * - Otherwise -> /login
 */
function RootRedirect() {
  const token = localStorage.getItem('token');
  const role = localStorage.getItem('role');

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  if (role === 'DEAN') {
    return <Navigate to="/dean/dashboard" replace />;
  } else if (role === 'EMPLOYEE') {
    return <Navigate to="/employee/dashboard" replace />;
  }

  return <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public Login Route */}
        <Route path="/login" element={<LoginPage />} />

        {/* Protected Dean Dashboard (DEAN only) */}
        <Route
          path="/dean/dashboard"
          element={
            <RoleProtectedRoute requiredRole="DEAN">
              <Dashboard role="DEAN" />
            </RoleProtectedRoute>
          }
        />

        {/* Protected Employee Dashboard (EMPLOYEE only) */}
        <Route
          path="/employee/dashboard"
          element={
            <RoleProtectedRoute requiredRole="EMPLOYEE">
              <Dashboard role="EMPLOYEE" />
            </RoleProtectedRoute>
          }
        />

        {/* Root Route: redirects to role dashboard or login */}
        <Route path="/" element={<RootRedirect />} />

        {/* Fallback unknown paths */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
