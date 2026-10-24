import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import Navbar from '../components/Navbar';

export default function NotFoundPage() {
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
          <div className="error-code">404</div>
          <h1>Page Not Found</h1>
          <p className="error-description">
            The page you are looking for does not exist or may have been moved.
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
