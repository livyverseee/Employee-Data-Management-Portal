import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  loginDeanApi,
  loginEmployeeApi,
  registerDeanApi,
  registerEmployeeApi,
} from '../services/api';

/**
 * Reusable AuthCard component supporting Sign In and Register tabs.
 * Parameterized by portal ('DEAN' or 'EMPLOYEE').
 */
export default function AuthCard({ portal = 'DEAN' }) {
  const isDean = portal === 'DEAN';
  const navigate = useNavigate();

  // Active tab: 'signin' | 'register'
  const [activeTab, setActiveTab] = useState('signin');

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

  return (
    <div className={`auth-card-container portal-theme-${portal.toLowerCase()}`}>
      <div className="auth-card">
        {/* Header with Portal Badge */}
        <div className="auth-card-header">
          <div className="auth-portal-badge">
            <span className={`badge ${isDean ? 'badge-dean' : 'badge-emp'}`}>
              {isDean ? 'DEAN PORTAL' : 'EMPLOYEE PORTAL'}
            </span>
          </div>
          <h2>{isDean ? 'Dean Administration' : 'Employee Access'}</h2>
          <p className="auth-subtitle">
            {isDean
              ? 'Dataset management, employee records, and governance portal'
              : 'View company workforce directory and active dataset'}
          </p>
        </div>

        {/* Tab navigation */}
        <div className="auth-tabs" role="tablist">
          <button
            type="button"
            className={`auth-tab-btn ${activeTab === 'signin' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('signin');
              setServerError(null);
            }}
          >
            Sign In
          </button>
          <button
            type="button"
            className={`auth-tab-btn ${activeTab === 'register' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('register');
              setSignInError(null);
              setSuccessMessage(null);
            }}
          >
            Register
          </button>
        </div>

        {/* ================= SIGN IN TAB ================= */}
        {activeTab === 'signin' && (
          <div className="auth-tab-content">
            {successMessage && <div className="alert alert-success">{successMessage}</div>}
            {signInError && <div className="alert alert-error">{signInError}</div>}

            <form onSubmit={handleSignIn} noValidate>
              <div className="form-group">
                <label htmlFor="sign-in-identifier">Username or Email</label>
                <input
                  id="sign-in-identifier"
                  type="text"
                  placeholder="e.g. dean or dean@portal.com"
                  value={signInIdentifier}
                  onChange={(e) => setSignInIdentifier(e.target.value)}
                  disabled={signInLoading}
                  autoComplete="username"
                  autoFocus
                />
              </div>

              <div className="form-group">
                <label htmlFor="sign-in-password">Password</label>
                <input
                  id="sign-in-password"
                  type="password"
                  placeholder="Enter your password"
                  value={signInPassword}
                  onChange={(e) => setSignInPassword(e.target.value)}
                  disabled={signInLoading}
                  autoComplete="current-password"
                />
              </div>

              <button
                type="submit"
                className="btn btn-primary btn-block"
                disabled={signInLoading}
              >
                {signInLoading ? 'Signing in...' : 'Sign In'}
              </button>
            </form>
          </div>
        )}

        {/* ================= REGISTER TAB ================= */}
        {activeTab === 'register' && (
          <div className="auth-tab-content">
            {serverError && <div className="alert alert-error">{serverError}</div>}

            <form onSubmit={handleRegister} noValidate>
              <div className="form-group">
                <label htmlFor="reg-fullname">Full Name</label>
                <input
                  id="reg-fullname"
                  type="text"
                  placeholder="e.g. John Doe"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  disabled={registerLoading}
                  className={registerErrors.fullName ? 'input-error' : ''}
                />
                {registerErrors.fullName && (
                  <span className="form-error-text">{registerErrors.fullName}</span>
                )}
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="reg-username">Username</label>
                  <input
                    id="reg-username"
                    type="text"
                    placeholder="e.g. jdoe"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    disabled={registerLoading}
                    className={registerErrors.username ? 'input-error' : ''}
                  />
                  {registerErrors.username && (
                    <span className="form-error-text">{registerErrors.username}</span>
                  )}
                </div>

                <div className="form-group">
                  <label htmlFor="reg-email">Email Address</label>
                  <input
                    id="reg-email"
                    type="email"
                    placeholder="e.g. jdoe@portal.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    disabled={registerLoading}
                    className={registerErrors.email ? 'input-error' : ''}
                  />
                  {registerErrors.email && (
                    <span className="form-error-text">{registerErrors.email}</span>
                  )}
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="reg-password">Password</label>
                  <input
                    id="reg-password"
                    type="password"
                    placeholder="Min 6 characters"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    disabled={registerLoading}
                    className={registerErrors.password ? 'input-error' : ''}
                  />
                  {registerErrors.password && (
                    <span className="form-error-text">{registerErrors.password}</span>
                  )}
                </div>

                <div className="form-group">
                  <label htmlFor="reg-confirm-password">Confirm Password</label>
                  <input
                    id="reg-confirm-password"
                    type="password"
                    placeholder="Re-enter password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    disabled={registerLoading}
                    className={registerErrors.confirmPassword ? 'input-error' : ''}
                  />
                  {registerErrors.confirmPassword && (
                    <span className="form-error-text">{registerErrors.confirmPassword}</span>
                  )}
                </div>
              </div>

              {isDean && (
                <div className="form-group">
                  <label htmlFor="reg-access-code">
                    Dean Access Code <span className="field-hint">(Required for Dean role)</span>
                  </label>
                  <input
                    id="reg-access-code"
                    type="password"
                    placeholder="Enter Dean Access Code (e.g. DEAN2026)"
                    value={accessCode}
                    onChange={(e) => setAccessCode(e.target.value)}
                    disabled={registerLoading}
                    className={registerErrors.accessCode ? 'input-error' : ''}
                  />
                  {registerErrors.accessCode && (
                    <span className="form-error-text">{registerErrors.accessCode}</span>
                  )}
                </div>
              )}

              <button
                type="submit"
                className="btn btn-primary btn-block"
                disabled={registerLoading}
              >
                {registerLoading ? 'Creating Account...' : 'Complete Registration'}
              </button>
            </form>
          </div>
        )}

        <div className="auth-card-footer">
          <Link to="/" className="back-link">
            &larr; Return to Portal Selection
          </Link>
        </div>
      </div>
    </div>
  );
}
