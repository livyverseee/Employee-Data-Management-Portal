import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { loginApi } from '../services/api';

/**
 * LoginPage:
 * Renders centered login form, validates credentials against backend,
 * stores auth token/role/username in localStorage, and redirects to appropriate dashboard.
 */
export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError('Please provide both username and password');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await loginApi(username.trim(), password);
      const { token, role, username: returnedUsername } = response.data;

      // Store auth credentials in localStorage
      localStorage.setItem('token', token);
      localStorage.setItem('role', role);
      localStorage.setItem('username', returnedUsername);

      // Redirect directly to the dashboard for that role
      if (role === 'DEAN') {
        navigate('/dean/dashboard', { replace: true });
      } else if (role === 'EMPLOYEE') {
        navigate('/employee/dashboard', { replace: true });
      } else {
        navigate('/', { replace: true });
      }
    } catch (err) {
      const errorMsg = err.response?.data?.message || 'Invalid username or password';
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <h1>Employee Portal</h1>
          <p>Sign in with your assigned credentials</p>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={handleLogin}>
          <div className="form-group">
            <label htmlFor="login-username">Username</label>
            <input
              id="login-username"
              type="text"
              placeholder="e.g. dean or employee"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={loading}
              autoComplete="username"
              autoFocus
            />
          </div>

          <div className="form-group">
            <label htmlFor="login-password">Password</label>
            <input
              id="login-password"
              type="password"
              placeholder="Enter password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={loading}
              autoComplete="current-password"
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '8px' }}
            disabled={loading}
          >
            {loading ? 'Authenticating...' : 'Sign In'}
          </button>
        </form>

        <div className="login-footer-info">
          <div><strong>Demo Credentials:</strong></div>
          <div style={{ marginTop: '4px' }}>Dean: <code>dean</code> / <code>dean123</code></div>
          <div>Employee: <code>employee</code> / <code>emp123</code></div>
        </div>
      </div>
    </div>
  );
}
