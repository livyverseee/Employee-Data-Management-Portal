import React from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * Top navigation header showing app title, portal badge, logged-in name, and logout button.
 * Logout returns to the login page of the same portal.
 */
export default function Header({ role }) {
  const navigate = useNavigate();
  const isDean = role === 'DEAN';

  const fullName = localStorage.getItem('fullName') || localStorage.getItem('username') || 'User';
  const username = localStorage.getItem('username');

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('fullName');
    localStorage.removeItem('role');

    // Return to the login page of the same portal
    if (isDean) {
      navigate('/dean/login', { replace: true });
    } else {
      navigate('/employee/login', { replace: true });
    }
  };

  return (
    <header className="dashboard-header">
      <div className="header-title-area">
        <h2>Employee Data Management Portal</h2>
        <span className={`badge ${isDean ? 'badge-dean' : 'badge-emp'}`}>
          {isDean ? 'DEAN PORTAL' : 'EMPLOYEE PORTAL'}
        </span>
      </div>

      <div className="header-user-area">
        <div className="header-user-info">
          Logged in as: <strong>{fullName}</strong> {username && fullName !== username && <span className="text-muted">(@{username})</span>}
        </div>
        <button onClick={handleLogout} className="btn btn-secondary">
          Logout
        </button>
      </div>
    </header>
  );
}
