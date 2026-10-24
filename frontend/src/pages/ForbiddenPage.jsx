import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import Navbar from '../components/Navbar';

export default function ForbiddenPage() {
  const { user, isAuthenticated } = useAuth();

  const getHomeRoute = () => {
    if (!isAuthenticated || !user) return '/login';
    return user.role === 'STUDENT' ? '/student' : '/officials';
  };

  return (
    <>
      <Navbar />
      <main className="container page-content">
        <div className="card text-center error-page-card">
          <div className="error-code">403</div>
          <h1>Access Forbidden</h1>
          <p className="error-description">
            You do not have the required permissions to view this page.
          </p>
          <div className="error-actions mt-3">
            <Link to={getHomeRoute()} className="btn btn-primary tap-target">
              Return to Home
            </Link>
          </div>
        </div>
      </main>
    </>
  );
}
