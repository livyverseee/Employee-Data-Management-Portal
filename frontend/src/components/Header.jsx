import React from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * Top navigation header showing app title, role badge, logged-in username, and logout button.
 */
export default function Header({ role }) {
  const navigate = useNavigate();
  const username = localStorage.getItem('username') || 'User';

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    navigate('/login');
  };

  const isDean = role === 'DEAN';

  return (
    <header className="dashboard-header">
      <div className="header-title-area">
        <h2>Employee Data Management Portal</h2>
        <span className={`badge ${isDean ? 'badge-dean' : 'badge-emp'}`}>
          {isDean ? 'Dean Portal' : 'Employee Portal'}
        </span>
      </div>
      <div className="header-user-area">
        <div className="header-user-info">
          Logged in as: <strong>{username}</strong>
        </div>
        <button onClick={handleLogout} className="btn btn-secondary">
          Logout
        </button>
      </div>
    </header>
  );
}
