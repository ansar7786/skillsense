import React from 'react';
import { useAuth } from '../auth/AuthContext';
import Navbar from '../components/Navbar';

export default function OfficialsPlaceholderPage() {
  const { user } = useAuth();

  return (
    <>
      <Navbar />
      <main className="container page-content">
        <div className="card text-center" style={{ maxWidth: '600px', margin: '40px auto' }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }} aria-hidden="true">🏛️</div>
          <h2>Officials Portal</h2>
          <p className="card-subtitle">
            Welcome, <strong>{user?.name || user?.collegeId}</strong> ({user?.role})
          </p>
          <div className="alert alert-info mt-3" style={{ textAlign: 'left' }}>
            <p>
              <strong>Phase 7 Notice:</strong> The official Survey Builder and Analytics Dashboard are scheduled for Phase 8.
            </p>
            <p className="mt-1" style={{ fontSize: '0.875rem' }}>
              Student survey forms, anonymity protections, local autosave, and personal readiness scoring are fully active under Phase 7.
            </p>
          </div>
        </div>
      </main>
    </>
  );
}
