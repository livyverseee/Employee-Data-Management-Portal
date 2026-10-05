import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LandingPage from './pages/LandingPage';
import DeanLoginPage from './pages/dean/DeanLoginPage';
import DeanDashboard from './pages/dean/DeanDashboard';
import EmployeeLoginPage from './pages/employee/EmployeeLoginPage';
import EmployeeDashboard from './pages/employee/EmployeeDashboard';
import RoleProtectedRoute from './components/RoleProtectedRoute';

/**
 * Main Application Router:
 * - "/"                    Landing page with two cards: "Dean Portal" and "Employee Portal"
 * - "/dean/login"          Dean portal: Sign In | Register tabs
 * - "/dean/dashboard"      Dean dashboard (DEAN only)
 * - "/employee/login"      Employee portal: Sign In | Register tabs
 * - "/employee/dashboard"  Employee dashboard (EMPLOYEE only)
 */
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Landing Page */}
        <Route path="/" element={<LandingPage />} />

        {/* Dean Portal Routes */}
        <Route path="/dean/login" element={<DeanLoginPage />} />
        <Route
          path="/dean/dashboard"
          element={
            <RoleProtectedRoute requiredRole="DEAN">
              <DeanDashboard />
            </RoleProtectedRoute>
          }
        />

        {/* Employee Portal Routes */}
        <Route path="/employee/login" element={<EmployeeLoginPage />} />
        <Route
          path="/employee/dashboard"
          element={
            <RoleProtectedRoute requiredRole="EMPLOYEE">
              <EmployeeDashboard />
            </RoleProtectedRoute>
          }
        />

        {/* Legacy redirect for old /login */}
        <Route path="/login" element={<Navigate to="/" replace />} />

        {/* Catch-all fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
