import React from 'react';
import { useNavigate } from 'react-router-dom';
import { LogOut, User } from 'lucide-react';

/**
 * Top navigation header for dashboards.
 * Styled with brand header colors, portal badge, user info, and a matching logout button.
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
    <header className="dashboard-header-bar">
      <div className="header-left-cluster">
        <h2 className="header-app-title">Employee Data Management Portal</h2>
        <span className={`dashboard-portal-pill ${isDean ? 'pill-dean' : 'pill-employee'}`}>
          {isDean ? 'DEAN PORTAL' : 'EMPLOYEE PORTAL'}
        </span>
      </div>

      <div className="header-right-cluster">
        <div className="header-user-badge">
          <User size={15} className="header-user-icon" aria-hidden="true" />
          <span>
            Logged in as: <strong>{fullName}</strong>
            {username && fullName !== username && (
              <span className="header-username-sub"> (@{username})</span>
            )}
          </span>
        </div>
        <button
          onClick={handleLogout}
          type="button"
          className="btn-header-logout"
          title="Sign out of current portal"
        >
          <LogOut size={15} />
          <span>Logout</span>
        </button>
      </div>
    </header>
  );
}
