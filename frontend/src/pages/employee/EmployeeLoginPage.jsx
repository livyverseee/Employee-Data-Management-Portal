import React from 'react';
import AuthCard from '../../components/AuthCard';

/**
 * Employee Portal Authentication Page (/employee/login):
 * Renders Sign In and Register tabs with authentic wave header styling.
 */
export default function EmployeeLoginPage() {
  return <AuthCard portal="EMPLOYEE" />;
}
