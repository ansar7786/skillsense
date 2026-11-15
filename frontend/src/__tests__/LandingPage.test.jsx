import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import LandingPage from '../pages/LandingPage';
import * as AuthContextModule from '../auth/AuthContext';

describe('LandingPage Component & Routing', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  const renderLandingPage = (authOverrides = {}) => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      isAuthenticated: false,
      user: null,
      loading: false,
      mustChangePassword: false,
      login: vi.fn(),
      logout: vi.fn(),
      ...authOverrides,
    });

    return render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<div>Login Page Target</div>} />
          <Route path="/student" element={<div>Student Dashboard Target</div>} />
          <Route path="/officials" element={<div>Officials Dashboard Target</div>} />
          <Route path="/change-password" element={<div>Change Password Target</div>} />
        </Routes>
      </MemoryRouter>
    );
  };

  it('renders all static placeholder sections for unauthenticated visitor', () => {
    renderLandingPage();

    // 1. Hero
    expect(screen.getByRole('heading', { level: 1, name: /SkillSense/i })).toBeInTheDocument();
    expect(screen.getByText(/Unified survey, placement readiness, and student analytics platform/i)).toBeInTheDocument();

    // 2. What this is
    expect(screen.getByRole('heading', { level: 2, name: /What is SkillSense\?/i })).toBeInTheDocument();
    expect(screen.getByText(/SkillSense is an institutional intelligence platform/i)).toBeInTheDocument();

    // 3. How it works
    expect(screen.getByRole('heading', { level: 2, name: /How It Works/i })).toBeInTheDocument();
    expect(screen.getByText(/Survey created:/i)).toBeInTheDocument();
    expect(screen.getByText(/Students respond anonymously:/i)).toBeInTheDocument();
    expect(screen.getByText(/Analytics engine computes readiness\/skill-gaps:/i)).toBeInTheDocument();
    expect(screen.getByText(/AI insights \+ dashboards:/i)).toBeInTheDocument();

    // 4. Key features
    expect(screen.getByRole('heading', { level: 2, name: /Key Features/i })).toBeInTheDocument();
    expect(screen.getByText(/K=5 Anonymity and Differential Privacy Guarantees/i)).toBeInTheDocument();

    // 5. Two Login buttons (Hero and Footer)
    const loginButtons = screen.getAllByRole('button', { name: /Login/i });
    expect(loginButtons).toHaveLength(2);
  });

  it('navigates to /login when clicking the Hero Login button', () => {
    renderLandingPage();

    const loginButtons = screen.getAllByRole('button', { name: /Login/i });
    fireEvent.click(loginButtons[0]);

    expect(screen.getByText('Login Page Target')).toBeInTheDocument();
  });

  it('navigates to /login when clicking the Footer Login button', () => {
    renderLandingPage();

    const loginButtons = screen.getAllByRole('button', { name: /Login/i });
    fireEvent.click(loginButtons[1]);

    expect(screen.getByText('Login Page Target')).toBeInTheDocument();
  });

  it('redirects authenticated STUDENT visitor directly to /student dashboard', () => {
    renderLandingPage({
      isAuthenticated: true,
      user: { role: 'STUDENT', collegeId: '21CSE001' },
    });

    expect(screen.getByText('Student Dashboard Target')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { level: 1, name: /SkillSense/i })).not.toBeInTheDocument();
  });

  it('redirects authenticated FACULTY/ADMIN visitor directly to /officials dashboard', () => {
    renderLandingPage({
      isAuthenticated: true,
      user: { role: 'FACULTY', collegeId: 'fac01' },
    });

    expect(screen.getByText('Officials Dashboard Target')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { level: 1, name: /SkillSense/i })).not.toBeInTheDocument();
  });

  it('redirects authenticated visitor with mustChangePassword to /change-password', () => {
    renderLandingPage({
      isAuthenticated: true,
      mustChangePassword: true,
      user: { role: 'STUDENT', collegeId: '21CSE001' },
    });

    expect(screen.getByText('Change Password Target')).toBeInTheDocument();
  });
});

