import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, CheckCircle2, ShieldCheck, Users } from 'lucide-react';

/**
 * Landing Page ("/"):
 * Styled with an authentic top wave header band and two cards (Dean Portal & Employee Portal).
 * Reflects truthful capabilities and exact design tokens.
 */
export default function LandingPage() {
  const navigate = useNavigate();
  const token = localStorage.getItem('token');
  const role = localStorage.getItem('role');

  return (
    <div className="landing-page-wrapper">
      {/* ================= TOP WAVE HEADER BAND ================= */}
      <header className="landing-header-band">
        <div className="landing-header-content">
          <h1 className="landing-title">Employee Data Management Portal</h1>
          <p className="landing-subtitle">
            Enterprise workforce dataset management, governance, and role-based directory.
          </p>

          {token && role && (
            <div className="landing-active-session">
              <span>
                Signed in as <strong>{role}</strong>
              </span>
              <button
                type="button"
                className="btn-session-dash"
                onClick={() => {
                  if (role === 'DEAN') navigate('/dean/dashboard');
                  else navigate('/employee/dashboard');
                }}
              >
                <span>Go to Dashboard</span>
                <ArrowRight size={14} />
              </button>
            </div>
          )}
        </div>

        {/* 3 Layered SVG Waves along bottom edge */}
        <div className="landing-waves-container" aria-hidden="true">
          <svg
            className="landing-wave-svg"
            viewBox="0 0 1440 180"
            preserveAspectRatio="none"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M0,50 C320,120 540,20 880,75 C1160,120 1340,40 1440,65 L1440,180 L0,180 Z"
              fill="#43437f"
              fillOpacity="0.4"
            />
            <path
              d="M0,85 C380,25 640,135 980,70 C1220,30 1360,105 1440,90 L1440,180 L0,180 Z"
              fill="#a9a9d0"
              fillOpacity="0.6"
            />
            <path
              d="M0,120 C320,80 600,160 940,125 C1220,95 1350,145 1440,135 L1440,180 L0,180 Z"
              fill="var(--bg-page)"
            />
          </svg>
        </div>
      </header>

      {/* ================= PORTAL CARDS STAGE ================= */}
      <main className="landing-cards-stage">
        <div className="landing-cards-grid">
          {/* Dean Portal Card */}
          <div className="landing-portal-card landing-card-dean">
            <div className="landing-card-header">
              <span className="landing-role-pill pill-dean">DEAN PORTAL</span>
              <div className="landing-card-icon icon-dean">
                <ShieldCheck size={28} />
              </div>
              <h2 className="landing-card-title">Dean Portal</h2>
              <p className="landing-card-desc">
                Administrative portal for dataset lifecycle, record mutations, and workforce governance.
              </p>
            </div>

            <ul className="landing-features-list">
              <li>
                <CheckCircle2 size={16} className="feature-icon feature-icon-dean" />
                <span>Upload Excel, CSV, JSON or XML datasets</span>
              </li>
              <li>
                <CheckCircle2 size={16} className="feature-icon feature-icon-dean" />
                <span>Add, edit and delete employee records</span>
              </li>
              <li>
                <CheckCircle2 size={16} className="feature-icon feature-icon-dean" />
                <span>Multi-column dynamic filtering &amp; search</span>
              </li>
              <li>
                <CheckCircle2 size={16} className="feature-icon feature-icon-dean" />
                <span>Excel / CSV dataset export</span>
              </li>
            </ul>

            <Link to="/dean/login" className="btn-portal-action btn-portal-dean">
              <span>Enter Dean Portal</span>
              <ArrowRight size={18} />
            </Link>
          </div>

          {/* Employee Portal Card */}
          <div className="landing-portal-card landing-card-employee">
            <div className="landing-card-header">
              <span className="landing-role-pill pill-employee">EMPLOYEE PORTAL</span>
              <div className="landing-card-icon icon-emp">
                <Users size={28} />
              </div>
              <h2 className="landing-card-title">Employee Portal</h2>
              <p className="landing-card-desc">
                View-only workforce directory and search portal for organization employees.
              </p>
            </div>

            <ul className="landing-features-list">
              <li>
                <CheckCircle2 size={16} className="feature-icon feature-icon-emp" />
                <span>View the dean's active dataset</span>
              </li>
              <li>
                <CheckCircle2 size={16} className="feature-icon feature-icon-emp" />
                <span>Dynamic search &amp; column filters</span>
              </li>
              <li>
                <CheckCircle2 size={16} className="feature-icon feature-icon-emp" />
                <span>Inspect detailed record attributes</span>
              </li>
              <li>
                <CheckCircle2 size={16} className="feature-icon feature-icon-emp" />
                <span>Excel / CSV dataset export</span>
              </li>
            </ul>

            <Link to="/employee/login" className="btn-portal-action btn-portal-employee">
              <span>Enter Employee Portal</span>
              <ArrowRight size={18} />
            </Link>
          </div>
        </div>
      </main>

      <footer className="landing-site-footer">
        <p>Employee Data Management Portal &bull; Enterprise Governance Platform</p>
      </footer>
    </div>
  );
}
