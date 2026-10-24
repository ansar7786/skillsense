import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import LoginPage from '../pages/LoginPage';
import * as AuthContextModule from '../auth/AuthContext';

describe('Login Role-Based Redirects', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  const setupLoginTest = (mockUser) => {
    const mockLogin = vi.fn().mockResolvedValue({
      token: 'mock-jwt-token',
      user: mockUser,
    });

    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      login: mockLogin,
      isAuthenticated: false,
      user: null,
      mustChangePassword: false,
    });

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/student" element={<div>Student Home Page</div>} />
          <Route path="/officials" element={<div>Officials Home Page</div>} />
          <Route path="/change-password" element={<div>Change Password Page</div>} />
        </Routes>
      </MemoryRouter>
    );

    return mockLogin;
  };

  const performLogin = () => {
    fireEvent.change(screen.getByLabelText(/College ID/i), { target: { value: 'test_user' } });
    fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'Pass@1234' } });
    fireEvent.click(screen.getByRole('button', { name: /Sign In/i }));
  };

  it('redirects STUDENT to /student on successful login', async () => {
    const mockUser = { role: 'STUDENT', mustChangePassword: false };
    setupLoginTest(mockUser);

    performLogin();

    await waitFor(() => {
      expect(screen.getByText('Student Home Page')).toBeInTheDocument();
    });
  });

  it('redirects FACULTY to /officials on successful login', async () => {
    const mockUser = { role: 'FACULTY', mustChangePassword: false };
    setupLoginTest(mockUser);

    performLogin();

    await waitFor(() => {
      expect(screen.getByText('Officials Home Page')).toBeInTheDocument();
    });
  });

  it('redirects HOD to /officials on successful login', async () => {
    const mockUser = { role: 'HOD', mustChangePassword: false };
    setupLoginTest(mockUser);

    performLogin();

    await waitFor(() => {
      expect(screen.getByText('Officials Home Page')).toBeInTheDocument();
    });
  });

  it('redirects PLACEMENT_OFFICER to /officials on successful login', async () => {
    const mockUser = { role: 'PLACEMENT_OFFICER', mustChangePassword: false };
    setupLoginTest(mockUser);

    performLogin();

    await waitFor(() => {
      expect(screen.getByText('Officials Home Page')).toBeInTheDocument();
    });
  });

  it('redirects ADMIN to /officials on successful login', async () => {
    const mockUser = { role: 'ADMIN', mustChangePassword: false };
    setupLoginTest(mockUser);

    performLogin();

    await waitFor(() => {
      expect(screen.getByText('Officials Home Page')).toBeInTheDocument();
    });
  });

  it('redirects to /change-password if user.mustChangePassword is true regardless of role', async () => {
    const mockUser = { role: 'STUDENT', mustChangePassword: true };
    setupLoginTest(mockUser);

    performLogin();

    await waitFor(() => {
      expect(screen.getByText('Change Password Page')).toBeInTheDocument();
    });
  });
});
