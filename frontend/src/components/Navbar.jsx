import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const getRoleHome = () => {
    if (!user) return '/login';
    return user.role === 'STUDENT' ? '/student' : '/officials';
  };

  return (
    <header className="navbar">
      <div className="navbar-container">
        <div className="d-flex align-center gap-3">
          <Link to={getRoleHome()} className="navbar-brand">
            <span className="brand-logo" aria-hidden="true">📊</span>
            <span className="brand-name">SkillSense</span>
          </Link>

          {isAuthenticated && user?.role === 'ADMIN' && (
            <nav className="navbar-admin-links d-flex align-center gap-2" style={{ marginLeft: 'var(--space-2)' }}>
              <Link to="/officials" className="nav-link" style={{ padding: '4px 8px', borderRadius: 'var(--radius-sm)' }}>
                Surveys
              </Link>
              <Link to="/admin/users" className="nav-link" style={{ padding: '4px 8px', borderRadius: 'var(--radius-sm)' }}>
                Users
              </Link>
              <Link to="/admin/terms" className="nav-link" style={{ padding: '4px 8px', borderRadius: 'var(--radius-sm)' }}>
                Terms
              </Link>
            </nav>
          )}
        </div>

        {isAuthenticated && (
          <div className="navbar-user">
            <div className="user-info">
              <span className="user-name">{user?.name || user?.collegeId}</span>
              <span className="role-badge">{user?.role}</span>
            </div>
            <div className="navbar-actions">
              <Link to="/change-password" className="nav-link" title="Change Password">
                Password
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                className="btn-logout tap-target"
                aria-label="Logout"
              >
                Logout
              </button>
            </div>
          </div>
        )}
      </div>
    </header>
  );
}
