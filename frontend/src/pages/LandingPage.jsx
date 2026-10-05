import React from 'react';
import { Link, useNavigate } from 'react-router-dom';

/**
 * Landing page at "/":
 * Displays two prominent cards: "Dean Portal" and "Employee Portal".
 * Directs users to the respective portal sign-in/register experience.
 */
export default function LandingPage() {
  const navigate = useNavigate();
  const token = localStorage.getItem('token');
  const role = localStorage.getItem('role');

  return (
    <div className="landing-container">
      <div className="landing-header">
        <h1 className="landing-title">Employee Data Management Portal</h1>
        <p className="landing-subtitle">
          Secure, role-based workforce data management and analysis
        </p>

        {token && role && (
          <div className="active-session-banner">
            <span>You are currently signed in as <strong>{role}</strong>.</span>
            <button
              type="button"
              className="btn btn-outline btn-sm"
              onClick={() => {
                if (role === 'DEAN') navigate('/dean/dashboard');
                else navigate('/employee/dashboard');
              }}
            >
              Go to your Dashboard &rarr;
            </button>
          </div>
        )}
      </div>

      <div className="landing-portals-grid">
        {/* Dean Portal Card */}
        <div className="portal-card portal-card-dean">
          <div className="portal-card-badge">
            <span className="badge badge-dean">DEAN PORTAL</span>
          </div>
          <div className="portal-icon">🎓</div>
          <h2>Dean Portal</h2>
          <p className="portal-desc">
            Administrative portal for academic leadership. Upload and replace employee XML datasets,
            edit workforce records, inspect details, and manage database lifecycle.
          </p>
          <ul className="portal-features">
            <li>✓ XML Dataset Ingestion & Atomic Replacement</li>
            <li>✓ Edit & Delete Employee Records</li>
            <li>✓ Multi-column Filtering & Search</li>
            <li>✓ CSV Export & Detail Inspection</li>
          </ul>
          <Link to="/dean/login" className="btn btn-primary btn-portal btn-dean">
            Enter Dean Portal &rarr;
          </Link>
        </div>

        {/* Employee Portal Card */}
        <div className="portal-card portal-card-employee">
          <div className="portal-card-badge">
            <span className="badge badge-emp">EMPLOYEE PORTAL</span>
          </div>
          <div className="portal-icon">👥</div>
          <h2>Employee Portal</h2>
          <p className="portal-desc">
            View-only workforce directory for employees. Search and filter through active organization
            records, inspect detailed employee attributes, and download filtered datasets.
          </p>
          <ul className="portal-features">
            <li>✓ View Scoped Active Dataset</li>
            <li>✓ Multi-column Dynamic Filtering</li>
            <li>✓ Instant Detail Modal Inspection</li>
            <li>✓ Filtered CSV Export</li>
          </ul>
          <Link to="/employee/login" className="btn btn-primary btn-portal btn-employee">
            Enter Employee Portal &rarr;
          </Link>
        </div>
      </div>

      <footer className="landing-footer">
        <p>Employee Data Management Portal &bull; Technical Evaluation Project</p>
      </footer>
    </div>
  );
}
