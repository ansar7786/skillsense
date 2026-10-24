import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import Navbar from '../components/Navbar';

export default function ChangePasswordPage() {
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const { changePassword, user } = useAuth();
  const navigate = useNavigate();

  const validatePassword = (pwd) => {
    if (pwd.length < 8) {
      return 'New password must be at least 8 characters long.';
    }
    if (!/[A-Za-z]/.test(pwd)) {
      return 'New password must contain at least one letter.';
    }
    if (!/[0-9]/.test(pwd)) {
      return 'New password must contain at least one digit.';
    }
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!oldPassword || !newPassword || !confirmPassword) {
      setError('Please fill in all fields.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('New password and confirmation do not match.');
      return;
    }

    const valErr = validatePassword(newPassword);
    if (valErr) {
      setError(valErr);
      return;
    }

    setSubmitting(true);
    try {
      await changePassword(oldPassword, newPassword);
      setSuccess('Password updated successfully! Redirecting...');
      setTimeout(() => {
        if (user?.role === 'STUDENT') {
          navigate('/student', { replace: true });
        } else {
          navigate('/officials', { replace: true });
        }
      }, 1000);
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        (err.response?.status === 400
          ? 'Invalid password. Ensure the current password is correct and the new password meets requirements.'
          : 'Failed to update password. Please try again.');
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <Navbar />
      <main className="container page-content">
        <div className="card form-card">
          <h2>Change Password</h2>
          <p className="card-subtitle">
            {user?.mustChangePassword
              ? 'You must change your password on first login before proceeding.'
              : 'Update your account password.'}
          </p>

          {error && (
            <div className="alert alert-error" role="alert">
              {error}
            </div>
          )}

          {success && (
            <div className="alert alert-success" role="status">
              {success}
            </div>
          )}

          <form onSubmit={handleSubmit} className="auth-form" noValidate>
            <div className="form-group">
              <label htmlFor="oldPassword">Current Password</label>
              <input
                type="password"
                id="oldPassword"
                className="text-input tap-target"
                value={oldPassword}
                onChange={(e) => setOldPassword(e.target.value)}
                autoComplete="current-password"
                required
                disabled={submitting}
              />
            </div>

            <div className="form-group">
              <label htmlFor="newPassword">New Password</label>
              <input
                type="password"
                id="newPassword"
                className="text-input tap-target"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                autoComplete="new-password"
                required
                disabled={submitting}
              />
              <small className="help-text">
                Minimum 8 characters, at least one letter and at least one digit.
              </small>
            </div>

            <div className="form-group">
              <label htmlFor="confirmPassword">Confirm New Password</label>
              <input
                type="password"
                id="confirmPassword"
                className="text-input tap-target"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                autoComplete="new-password"
                required
                disabled={submitting}
              />
            </div>

            <button
              type="submit"
              className="btn btn-primary btn-block tap-target"
              disabled={submitting}
            >
              {submitting ? 'Updating...' : 'Update Password'}
            </button>
          </form>
        </div>
      </main>
    </>
  );
}
