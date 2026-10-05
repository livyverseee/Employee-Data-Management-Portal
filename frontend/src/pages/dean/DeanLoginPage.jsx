import React from 'react';
import AuthCard from '../../components/AuthCard';

/**
 * Dean Portal Authentication Page (/dean/login):
 * Renders Sign In and Register tabs with Dean access code requirement.
 */
export default function DeanLoginPage() {
  return (
    <div className="portal-auth-page portal-theme-dean">
      <AuthCard portal="DEAN" />
    </div>
  );
}
