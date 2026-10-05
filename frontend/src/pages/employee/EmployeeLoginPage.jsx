import React from 'react';
import AuthCard from '../../components/AuthCard';

/**
 * Employee Portal Authentication Page (/employee/login):
 * Renders Sign In and Register tabs for employees.
 */
export default function EmployeeLoginPage() {
  return (
    <div className="portal-auth-page portal-theme-employee">
      <AuthCard portal="EMPLOYEE" />
    </div>
  );
}
