import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  User,
  Lock,
  Mail,
  Key,
  Eye,
  EyeOff,
  ArrowRight,
  ArrowLeft,
  Loader2,
} from 'lucide-react';
import {
  loginDeanApi,
  loginEmployeeApi,
  registerDeanApi,
  registerEmployeeApi,
} from '../services/api';

/**
 * Shared AuthCard component supporting Sign In and Register tabs.
 * Styled with an authentic top wave header band and clean, accessible card layout.
 * Parameterized by portal ('DEAN' | 'EMPLOYEE').
 */
export default function AuthCard({ portal = 'DEAN' }) {
  const isDean = portal === 'DEAN';
  const navigate = useNavigate();

  // Active tab: 'signin' | 'register'
  const [activeTab, setActiveTab] = useState('signin');

  // Password visibility toggles
  const [showSignInPassword, setShowSignInPassword] = useState(false);
  const [showRegPassword, setShowRegPassword] = useState(false);
  const [showRegConfirmPassword, setShowRegConfirmPassword] = useState(false);

  // Sign In form state
  const [signInIdentifier, setSignInIdentifier] = useState('');
  const [signInPassword, setSignInPassword] = useState('');
  const [signInLoading, setSignInLoading] = useState(false);
  const [signInError, setSignInError] = useState(null);

  // Register form state
  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [accessCode, setAccessCode] = useState('');
  const [registerLoading, setRegisterLoading] = useState(false);
  const [registerErrors, setRegisterErrors] = useState({});
  const [serverError, setServerError] = useState(null);

  // Success message after registration to display on Sign In tab
  const [successMessage, setSuccessMessage] = useState(null);

  // Handle Sign In submission
  const handleSignIn = async (e) => {
    e.preventDefault();
    setSignInError(null);
    setSuccessMessage(null);

    const identifier = signInIdentifier.trim();
    if (!identifier || !signInPassword) {
      setSignInError('Please provide both username/email and password');
      return;
    }

    setSignInLoading(true);

    try {
      const loginFn = isDean ? loginDeanApi : loginEmployeeApi;
      const response = await loginFn(identifier, signInPassword);
      const { token, role, username: retUser, fullName: retFullName } = response.data;

      localStorage.setItem('token', token);
      localStorage.setItem('role', role);
      localStorage.setItem('username', retUser);
      if (retFullName) {
        localStorage.setItem('fullName', retFullName);
      }

      if (role === 'DEAN') {
        navigate('/dean/dashboard', { replace: true });
      } else {
        navigate('/employee/dashboard', { replace: true });
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Authentication failed';
      setSignInError(msg);
    } finally {
      setSignInLoading(false);
    }
  };

  // Client-side validation for Register form
  const validateRegister = () => {
    const errors = {};
    if (!fullName.trim()) {
      errors.fullName = 'Full name is required';
    }
    if (!username.trim()) {
      errors.username = 'Username is required';
    } else if (username.trim().length < 3) {
      errors.username = 'Username must be at least 3 characters';
    }
    if (!email.trim()) {
      errors.email = 'Email address is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      errors.email = 'Please provide a valid email address';
    }
    if (!password) {
      errors.password = 'Password is required';
    } else if (password.length < 6) {
      errors.password = 'Password must be at least 6 characters';
    }
    if (password !== confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
    }
    if (isDean && !accessCode.trim()) {
      errors.accessCode = 'Dean access code is required';
    }
    return errors;
  };

  // Handle Register submission
  const handleRegister = async (e) => {
    e.preventDefault();
    setServerError(null);

    const validationErrors = validateRegister();
    if (Object.keys(validationErrors).length > 0) {
      setRegisterErrors(validationErrors);
      return;
    }
    setRegisterErrors({});
    setRegisterLoading(true);

    try {
      if (isDean) {
        await registerDeanApi({
          fullName: fullName.trim(),
          username: username.trim(),
          email: email.trim(),
          password,
          accessCode: accessCode.trim(),
        });
      } else {
        await registerEmployeeApi({
          fullName: fullName.trim(),
          username: username.trim(),
          email: email.trim(),
          password,
        });
      }

      // Switch to sign in tab with success notification
      setActiveTab('signin');
      setSuccessMessage('Registration successful! Please sign in with your credentials.');
      // Prefill sign in identifier
      setSignInIdentifier(username.trim());
      setSignInPassword('');
      // Reset registration form
      setFullName('');
      setUsername('');
      setEmail('');
      setPassword('');
      setConfirmPassword('');
      setAccessCode('');
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Registration failed';
      setServerError(msg);
    } finally {
      setRegisterLoading(false);
    }
  };

  // Keyboard navigation for segmented tabs
  const handleTabKeyDown = (e, tab) => {
    if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
      e.preventDefault();
      const nextTab = tab === 'signin' ? 'register' : 'signin';
      setActiveTab(nextTab);
      setServerError(null);
      setSignInError(null);
      setSuccessMessage(null);
    }
  };

  return (
    <div className={`auth-page-wrapper portal-theme-${portal.toLowerCase()}`}>
      {/* ================= TOP WAVE HEADER BAND (~260px) ================= */}
      <div className="auth-header-band">
        <div className="auth-header-topbar">
          <Link to="/" className="auth-top-return-link">
            <ArrowLeft size={16} />
            <span>Return to Portal Selection</span>
          </Link>
        </div>

        {/* 3 Layered SVG Waves in brand tints */}
        <div className="auth-waves-container" aria-hidden="true">
          <svg
            className="auth-wave-svg"
            viewBox="0 0 1440 180"
            preserveAspectRatio="none"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M0,50 C320,120 540,20 880,75 C1160,120 1340,40 1440,65 L1440,180 L0,180 Z"
              fill="var(--brand-700)"
              fillOpacity="0.4"
            />
            <path
              d="M0,85 C380,25 640,135 980,70 C1220,30 1360,105 1440,90 L1440,180 L0,180 Z"
              fill="var(--brand-300)"
              fillOpacity="0.6"
            />
            <path
              d="M0,120 C320,80 600,160 940,125 C1220,95 1350,145 1440,135 L1440,180 L0,180 Z"
              fill="var(--bg-page)"
            />
          </svg>
        </div>
      </div>

      {/* ================= OVERLAPPING AUTH CARD ================= */}
      <main className="auth-card-stage">
        <div className="auth-card">
          {/* Centered Role Pill */}
          <div className="auth-role-pill-wrapper">
            <span className="auth-role-pill">
              {isDean ? 'DEAN PORTAL' : 'EMPLOYEE PORTAL'}
            </span>
          </div>

          {/* Title & Truthful Subtitle */}
          <h1 className="auth-card-title">
            {isDean ? 'Dean Administration' : 'Employee Portal'}
          </h1>
          <p className="auth-card-subtitle">
            {isDean
              ? 'Upload datasets and manage employee records.'
              : 'View, search and export employee records.'}
          </p>

          {/* Segmented Control / Tabs */}
          <div className="auth-tabs" role="tablist" aria-label="Authentication Options">
            <button
              id="tab-signin"
              type="button"
              role="tab"
              aria-selected={activeTab === 'signin'}
              aria-controls="panel-signin"
              tabIndex={activeTab === 'signin' ? 0 : -1}
              className={`auth-tab-btn ${activeTab === 'signin' ? 'active' : ''}`}
              onClick={() => {
                setActiveTab('signin');
                setServerError(null);
              }}
              onKeyDown={(e) => handleTabKeyDown(e, 'signin')}
            >
              Sign In
            </button>
            <button
              id="tab-register"
              type="button"
              role="tab"
              aria-selected={activeTab === 'register'}
              aria-controls="panel-register"
              tabIndex={activeTab === 'register' ? 0 : -1}
              className={`auth-tab-btn ${activeTab === 'register' ? 'active' : ''}`}
              onClick={() => {
                setActiveTab('register');
                setSignInError(null);
                setSuccessMessage(null);
              }}
              onKeyDown={(e) => handleTabKeyDown(e, 'register')}
            >
              Register
            </button>
          </div>

          {/* ================= SIGN IN PANEL ================= */}
          {activeTab === 'signin' && (
            <div
              id="panel-signin"
              role="tabpanel"
              aria-labelledby="tab-signin"
              className="auth-tab-panel"
            >
              {successMessage && (
                <div className="auth-alert auth-alert-success" role="status">
                  {successMessage}
                </div>
              )}
              {signInError && (
                <div className="auth-alert auth-alert-error" role="alert">
                  {signInError}
                </div>
              )}

              <form onSubmit={handleSignIn} noValidate>
                <div className="form-field-group">
                  <label htmlFor="sign-in-identifier" className="form-field-label">
                    Username or Email
                  </label>
                  <div className="input-group-field">
                    <User className="input-leading-icon" size={18} aria-hidden="true" />
                    <input
                      id="sign-in-identifier"
                      name="identifier"
                      type="text"
                      placeholder="Username or email"
                      value={signInIdentifier}
                      onChange={(e) => setSignInIdentifier(e.target.value)}
                      disabled={signInLoading}
                      autoComplete="username"
                      required
                    />
                  </div>
                </div>

                <div className="form-field-group">
                  <label htmlFor="sign-in-password" className="form-field-label">
                    Password
                  </label>
                  <div className="input-group-field">
                    <Lock className="input-leading-icon" size={18} aria-hidden="true" />
                    <input
                      id="sign-in-password"
                      name="password"
                      type={showSignInPassword ? 'text' : 'password'}
                      placeholder="Password"
                      value={signInPassword}
                      onChange={(e) => setSignInPassword(e.target.value)}
                      disabled={signInLoading}
                      autoComplete="current-password"
                      required
                    />
                    <button
                      type="button"
                      className="input-trailing-action"
                      aria-label={showSignInPassword ? 'Hide password' : 'Show password'}
                      onClick={() => setShowSignInPassword(!showSignInPassword)}
                      tabIndex={0}
                    >
                      {showSignInPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  </div>
                </div>

                <button
                  type="submit"
                  className="btn-auth-primary"
                  disabled={signInLoading}
                >
                  {signInLoading ? (
                    <>
                      <Loader2 size={18} className="spinner-icon" />
                      <span>Signing In...</span>
                    </>
                  ) : (
                    <>
                      <span>Sign In</span>
                      <ArrowRight size={18} />
                    </>
                  )}
                </button>
              </form>
            </div>
          )}

          {/* ================= REGISTER PANEL ================= */}
          {activeTab === 'register' && (
            <div
              id="panel-register"
              role="tabpanel"
              aria-labelledby="tab-register"
              className="auth-tab-panel"
            >
              {serverError && (
                <div className="auth-alert auth-alert-error" role="alert">
                  {serverError}
                </div>
              )}

              <form onSubmit={handleRegister} noValidate>
                {/* Full Name */}
                <div className="form-field-group">
                  <label htmlFor="reg-fullname" className="form-field-label">
                    Full Name
                  </label>
                  <div className={`input-group-field ${registerErrors.fullName ? 'has-error' : ''}`}>
                    <User className="input-leading-icon" size={18} aria-hidden="true" />
                    <input
                      id="reg-fullname"
                      name="fullName"
                      type="text"
                      placeholder="Full name"
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                      disabled={registerLoading}
                      autoComplete="name"
                      required
                    />
                  </div>
                  {registerErrors.fullName && (
                    <span className="field-error-msg">{registerErrors.fullName}</span>
                  )}
                </div>

                {/* Username & Email in responsive row */}
                <div className="form-grid-two">
                  <div className="form-field-group">
                    <label htmlFor="reg-username" className="form-field-label">
                      Username
                    </label>
                    <div className={`input-group-field ${registerErrors.username ? 'has-error' : ''}`}>
                      <User className="input-leading-icon" size={18} aria-hidden="true" />
                      <input
                        id="reg-username"
                        name="username"
                        type="text"
                        placeholder="Username"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        disabled={registerLoading}
                        autoComplete="username"
                        required
                      />
                    </div>
                    {registerErrors.username && (
                      <span className="field-error-msg">{registerErrors.username}</span>
                    )}
                  </div>

                  <div className="form-field-group">
                    <label htmlFor="reg-email" className="form-field-label">
                      Email Address
                    </label>
                    <div className={`input-group-field ${registerErrors.email ? 'has-error' : ''}`}>
                      <Mail className="input-leading-icon" size={18} aria-hidden="true" />
                      <input
                        id="reg-email"
                        name="email"
                        type="email"
                        placeholder="Email address"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        disabled={registerLoading}
                        autoComplete="email"
                        required
                      />
                    </div>
                    {registerErrors.email && (
                      <span className="field-error-msg">{registerErrors.email}</span>
                    )}
                  </div>
                </div>

                {/* Password & Confirm Password in responsive row */}
                <div className="form-grid-two">
                  <div className="form-field-group">
                    <label htmlFor="reg-password" className="form-field-label">
                      Password
                    </label>
                    <div className={`input-group-field ${registerErrors.password ? 'has-error' : ''}`}>
                      <Lock className="input-leading-icon" size={18} aria-hidden="true" />
                      <input
                        id="reg-password"
                        name="newPassword"
                        type={showRegPassword ? 'text' : 'password'}
                        placeholder="Password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        disabled={registerLoading}
                        autoComplete="new-password"
                        required
                      />
                      <button
                        type="button"
                        className="input-trailing-action"
                        aria-label={showRegPassword ? 'Hide password' : 'Show password'}
                        onClick={() => setShowRegPassword(!showRegPassword)}
                      >
                        {showRegPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                      </button>
                    </div>
                    {registerErrors.password && (
                      <span className="field-error-msg">{registerErrors.password}</span>
                    )}
                  </div>

                  <div className="form-field-group">
                    <label htmlFor="reg-confirm-password" className="form-field-label">
                      Confirm Password
                    </label>
                    <div className={`input-group-field ${registerErrors.confirmPassword ? 'has-error' : ''}`}>
                      <Lock className="input-leading-icon" size={18} aria-hidden="true" />
                      <input
                        id="reg-confirm-password"
                        name="confirmPassword"
                        type={showRegConfirmPassword ? 'text' : 'password'}
                        placeholder="Confirm password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        disabled={registerLoading}
                        autoComplete="new-password"
                        required
                      />
                      <button
                        type="button"
                        className="input-trailing-action"
                        aria-label={showRegConfirmPassword ? 'Hide password' : 'Show password'}
                        onClick={() => setShowRegConfirmPassword(!showRegConfirmPassword)}
                      >
                        {showRegConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                      </button>
                    </div>
                    {registerErrors.confirmPassword && (
                      <span className="field-error-msg">{registerErrors.confirmPassword}</span>
                    )}
                  </div>
                </div>

                {/* Dean Access Code (Required for Dean only) */}
                {isDean && (
                  <div className="form-field-group">
                    <label htmlFor="reg-access-code" className="form-field-label">
                      Dean Access Code
                    </label>
                    <div className={`input-group-field ${registerErrors.accessCode ? 'has-error' : ''}`}>
                      <Key className="input-leading-icon" size={18} aria-hidden="true" />
                      <input
                        id="reg-access-code"
                        name="accessCode"
                        type="password"
                        placeholder="Dean access code"
                        value={accessCode}
                        onChange={(e) => setAccessCode(e.target.value)}
                        disabled={registerLoading}
                        autoComplete="off"
                        required
                      />
                    </div>
                    {registerErrors.accessCode && (
                      <span className="field-error-msg">{registerErrors.accessCode}</span>
                    )}
                  </div>
                )}

                <button
                  type="submit"
                  className="btn-auth-primary"
                  disabled={registerLoading}
                >
                  {registerLoading ? (
                    <>
                      <Loader2 size={18} className="spinner-icon" />
                      <span>Creating Account...</span>
                    </>
                  ) : (
                    <>
                      <span>Create account</span>
                      <ArrowRight size={18} />
                    </>
                  )}
                </button>
              </form>
            </div>
          )}

          {/* Under button: Thin divider & Return link */}
          <div className="auth-card-divider" />
          <div className="auth-card-footer">
            <Link to="/" className="auth-bottom-return-link">
              <ArrowLeft size={14} />
              <span>Return to Portal Selection</span>
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}
