import React from 'react';
import AuthCard from '../../components/AuthCard';

/**
 * Dean Portal Authentication Page (/dean/login):
 * Renders Sign In and Register tabs with authentic wave header styling.
 */
export default function DeanLoginPage() {
  return <AuthCard portal="DEAN" />;
}
