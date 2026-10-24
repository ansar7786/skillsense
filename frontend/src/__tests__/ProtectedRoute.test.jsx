import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { describe, it, expect, vi } from 'vitest';
import ProtectedRoute from '../components/ProtectedRoute';
import * as AuthContextModule from '../auth/AuthContext';

describe('ProtectedRoute Component', () => {
  it('shows loading state when auth is initializing', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      user: null,
      isAuthenticated: false,
      loading: true,
      mustChangePassword: false,
    });

    render(
      <MemoryRouter initialEntries={['/student']}>
        <ProtectedRoute allowedRoles={['STUDENT']}>
          <div>Protected Content</div>
        </ProtectedRoute>
      </MemoryRouter>
    );

    expect(screen.getByText('Loading...')).toBeInTheDocument();
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });

  it('redirects unauthenticated users to /login', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      user: null,
      isAuthenticated: false,
      loading: false,
      mustChangePassword: false,
    });

    render(
      <MemoryRouter initialEntries={['/student']}>
        <Routes>
          <Route path="/login" element={<div>Login Page</div>} />
          <Route
            path="/student"
            element={
              <ProtectedRoute allowedRoles={['STUDENT']}>
                <div>Protected Student Content</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Login Page')).toBeInTheDocument();
    expect(screen.queryByText('Protected Student Content')).not.toBeInTheDocument();
  });

  it('redirects to /change-password if mustChangePassword is true', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      user: { role: 'STUDENT', mustChangePassword: true },
      isAuthenticated: true,
      loading: false,
      mustChangePassword: true,
    });

    render(
      <MemoryRouter initialEntries={['/student']}>
        <Routes>
          <Route path="/change-password" element={<div>Change Password Page</div>} />
          <Route
            path="/student"
            element={
              <ProtectedRoute allowedRoles={['STUDENT']}>
                <div>Student Content</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Change Password Page')).toBeInTheDocument();
    expect(screen.queryByText('Student Content')).not.toBeInTheDocument();
  });

  it('blocks users with wrong role and redirects to /403', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      user: { role: 'FACULTY', mustChangePassword: false },
      isAuthenticated: true,
      loading: false,
      mustChangePassword: false,
    });

    render(
      <MemoryRouter initialEntries={['/student']}>
        <Routes>
          <Route path="/403" element={<div>403 Forbidden</div>} />
          <Route
            path="/student"
            element={
              <ProtectedRoute allowedRoles={['STUDENT']}>
                <div>Student Content</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('403 Forbidden')).toBeInTheDocument();
    expect(screen.queryByText('Student Content')).not.toBeInTheDocument();
  });

  it('allows access to users with matching role', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      user: { role: 'STUDENT', mustChangePassword: false },
      isAuthenticated: true,
      loading: false,
      mustChangePassword: false,
    });

    render(
      <MemoryRouter initialEntries={['/student']}>
        <ProtectedRoute allowedRoles={['STUDENT']}>
          <div>Welcome Student</div>
        </ProtectedRoute>
      </MemoryRouter>
    );

    expect(screen.getByText('Welcome Student')).toBeInTheDocument();
  });
});
