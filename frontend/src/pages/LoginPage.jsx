import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function LoginPage() {
  const [collegeId, setCollegeId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const { login, isAuthenticated, user, mustChangePassword } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Redirect if already logged in
  useEffect(() => {
    if (isAuthenticated && user) {
      if (mustChangePassword) {
        navigate('/change-password', { replace: true });
      } else if (user.role === 'STUDENT') {
        navigate('/student', { replace: true });
      } else {
        navigate('/officials', { replace: true });
      }
    }
  }, [isAuthenticated, user, mustChangePassword, navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const trimmedId = collegeId.trim();
    if (!trimmedId || !password) {
      setError('Please enter both College ID and password.');
      return;
    }

    setSubmitting(true);
    try {
      const data = await login(trimmedId, password);
      const loggedUser = data.user;

      if (loggedUser.mustChangePassword) {
        navigate('/change-password', { replace: true });
      } else {
        // Redirect to intended from location if valid, otherwise role home
        const fromPath = location.state?.from?.pathname;
        if (fromPath && fromPath !== '/login') {
          navigate(fromPath, { replace: true });
        } else if (loggedUser.role === 'STUDENT') {
          navigate('/student', { replace: true });
        } else {
          navigate('/officials', { replace: true });
        }
      }
    } catch (err) {
      const message =
        err.response?.data?.message ||
        (err.response?.status === 401
          ? 'Invalid College ID or password.'
          : 'Unable to connect to the server. Please try again.');
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <div className="auth-logo" aria-hidden="true">📊</div>
          <h1>SkillSense</h1>
          <p className="auth-subtitle">Sign in with your institutional credentials</p>
        </div>

        {error && (
          <div className="alert alert-error" role="alert">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form" noValidate>
          <div className="form-group">
            <label htmlFor="collegeId">College ID / Roll Number</label>
            <input
              type="text"
              id="collegeId"
              name="collegeId"
              className="text-input tap-target"
              value={collegeId}
              onChange={(e) => setCollegeId(e.target.value)}
              placeholder="e.g. 26TEST01"
              autoComplete="username"
              required
              disabled={submitting}
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
              name="password"
              className="text-input tap-target"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              autoComplete="current-password"
              required
              disabled={submitting}
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-block tap-target"
            disabled={submitting}
          >
            {submitting ? 'Signing in...' : 'Sign In'}
          </button>
        </form>
      </div>
    </div>
  );
}
