import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ProtectedRoute from '../components/ProtectedRoute';
import AdminUsersPage from '../pages/AdminUsersPage';
import AdminTermsPage from '../pages/AdminTermsPage';
import SurveyAnalyticsDashboardPage from '../pages/SurveyAnalyticsDashboardPage';
import * as AuthContextModule from '../auth/AuthContext';
import * as adminApi from '../api/admin';
import * as surveyApi from '../api/surveys';

// Mock react-chartjs-2
vi.mock('react-chartjs-2', () => ({
  Bar: () => <div data-testid="bar-chart">Bar Chart</div>,
  Doughnut: () => <div data-testid="doughnut-chart">Doughnut Chart</div>,
}));

// Mock Navbar
vi.mock('../components/Navbar', () => ({
  default: () => <nav data-testid="navbar">Navbar</nav>,
}));

describe('Phase 10: Reports, Admin Users & Academic Terms', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.restoreAllMocks();
  });

  describe('Survey Report Exports on Analytics Dashboard', () => {
    const mockSurvey = {
      surveyId: 15,
      title: 'Senior Exit Survey',
      category: 'ACADEMIC',
      status: 'CLOSED',
      isAnonymous: false,
      version: 1,
      eligibleCount: 100,
      termLabel: '2026-27 Even',
      questionSchema: {
        questions: [{ key: 'q1', type: 'RATING', prompt: 'Curriculum rating', required: true }]
      }
    };

    const mockAnalytics = {
      available: true,
      have: 50,
      need: 5,
      data: {
        response_count: 50,
        completion_rate: 0.5,
        index: 80.0,
        questions: [{ key: 'q1', type: 'RATING', n: 50, mean: 4.0, distribution: { '4': 50 } }],
        readiness_distribution: { Ready: 40, Developing: 10, 'Needs Support': 0 },
        breakdowns: {}
      }
    };

    it('renders Export PDF and Export Excel buttons and triggers download on click', async () => {
      vi.spyOn(surveyApi, 'fetchSurvey').mockResolvedValue(mockSurvey);
      vi.spyOn(surveyApi, 'fetchSurveyAnalytics').mockResolvedValue(mockAnalytics);
      vi.spyOn(surveyApi, 'fetchSurveyTrend').mockResolvedValue([]);
      vi.spyOn(surveyApi, 'fetchSurveyInsights').mockResolvedValue({
        summary: 'Strong performance',
        recommendations: [],
        provider: 'RULES'
      });

      const mockDownload = vi.spyOn(surveyApi, 'downloadSurveyExport').mockResolvedValue(new Blob(['fake-pdf']));
      const mockTrigger = vi.spyOn(surveyApi, 'triggerFileDownload').mockImplementation(() => {});

      render(
        <MemoryRouter initialEntries={['/officials/surveys/15/analytics']}>
          <Routes>
            <Route path="/officials/surveys/:id/analytics" element={<SurveyAnalyticsDashboardPage />} />
          </Routes>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Senior Exit Survey')).toBeInTheDocument();
      });

      const pdfBtn = screen.getByRole('button', { name: /Export PDF/i });
      const excelBtn = screen.getByRole('button', { name: /Export Excel/i });
      expect(pdfBtn).toBeInTheDocument();
      expect(excelBtn).toBeInTheDocument();

      // Trigger PDF export
      fireEvent.click(pdfBtn);

      await waitFor(() => {
        expect(mockDownload).toHaveBeenCalledWith('15', 'pdf');
        expect(mockTrigger).toHaveBeenCalled();
      });

      // Trigger Excel export
      fireEvent.click(excelBtn);

      await waitFor(() => {
        expect(mockDownload).toHaveBeenCalledWith('15', 'xlsx');
        expect(mockTrigger).toHaveBeenCalled();
      });
    });

    it('displays error alert if report export fails', async () => {
      vi.spyOn(surveyApi, 'fetchSurvey').mockResolvedValue(mockSurvey);
      vi.spyOn(surveyApi, 'fetchSurveyAnalytics').mockResolvedValue(mockAnalytics);
      vi.spyOn(surveyApi, 'fetchSurveyTrend').mockResolvedValue([]);
      vi.spyOn(surveyApi, 'fetchSurveyInsights').mockResolvedValue({ summary: 'ok', provider: 'RULES' });

      vi.spyOn(surveyApi, 'downloadSurveyExport').mockRejectedValue({
        response: { data: { message: 'Export forbidden for anonymous survey with n < 5' } }
      });

      render(
        <MemoryRouter initialEntries={['/officials/surveys/15/analytics']}>
          <Routes>
            <Route path="/officials/surveys/:id/analytics" element={<SurveyAnalyticsDashboardPage />} />
          </Routes>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Senior Exit Survey')).toBeInTheDocument();
      });

      const pdfBtn = screen.getByRole('button', { name: /Export PDF/i });
      fireEvent.click(pdfBtn);

      await waitFor(() => {
        expect(screen.getByText(/Export forbidden for anonymous survey with n < 5/i)).toBeInTheDocument();
      });
    });
  });

  describe('Admin Route Access Control', () => {
    it('redirects non-admin officials to /403', () => {
      vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
        user: { role: 'FACULTY', collegeId: 'FAC01' },
        isAuthenticated: true,
        loading: false,
        mustChangePassword: false,
      });

      render(
        <MemoryRouter initialEntries={['/admin/users']}>
          <Routes>
            <Route path="/403" element={<div data-testid="forbidden-page">Access Forbidden</div>} />
            <Route
              path="/admin/users"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <div>Admin Users Page</div>
                </ProtectedRoute>
              }
            />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.getByTestId('forbidden-page')).toBeInTheDocument();
      expect(screen.queryByText('Admin Users Page')).not.toBeInTheDocument();
    });

    it('allows ADMIN users to access protected admin pages', () => {
      vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
        user: { role: 'ADMIN', collegeId: 'ADMIN01' },
        isAuthenticated: true,
        loading: false,
        mustChangePassword: false,
      });

      render(
        <MemoryRouter initialEntries={['/admin/users']}>
          <Routes>
            <Route
              path="/admin/users"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <div>Admin Users Page</div>
                </ProtectedRoute>
              }
            />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.getByText('Admin Users Page')).toBeInTheDocument();
    });
  });

  describe('AdminUsersPage Operations', () => {
    const mockUsers = [
      {
        userId: 1,
        collegeId: '26CS01',
        name: 'Alice Student',
        email: 'alice@college.edu',
        role: 'STUDENT',
        branch: 'CS',
        studyYear: 3,
        section: 'A',
        isActive: true,
        createdAt: '2026-09-01T10:00:00'
      },
      {
        userId: 2,
        collegeId: 'FAC_CS01',
        name: 'Dr. Bob Professor',
        email: 'bob@college.edu',
        role: 'FACULTY',
        branch: 'CS',
        studyYear: null,
        section: null,
        isActive: true,
        createdAt: '2026-09-01T10:00:00'
      }
    ];

    it('loads and lists users with details and active status', async () => {
      vi.spyOn(adminApi, 'fetchUsers').mockResolvedValue({
        content: mockUsers,
        totalPages: 1,
        totalElements: 2
      });

      render(
        <MemoryRouter>
          <AdminUsersPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('26CS01')).toBeInTheDocument();
        expect(screen.getByText('Alice Student')).toBeInTheDocument();
        expect(screen.getByText('FAC_CS01')).toBeInTheDocument();
        expect(screen.getByText('Dr. Bob Professor')).toBeInTheDocument();
      });
    });

    it('allows creating a new user and displays generated credentials', async () => {
      vi.spyOn(adminApi, 'fetchUsers').mockResolvedValue({
        content: mockUsers,
        totalPages: 1,
        totalElements: 2
      });

      const mockCreate = vi.spyOn(adminApi, 'createUser').mockResolvedValue({
        user: {
          userId: 3,
          collegeId: '26CS99',
          name: 'Charlie New',
          role: 'STUDENT',
          branch: 'CS',
          isActive: true
        },
        temporaryPassword: 'Temp@Password123'
      });

      render(
        <MemoryRouter>
          <AdminUsersPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('+ Add User')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('+ Add User'));

      expect(screen.getByText('Add New User')).toBeInTheDocument();

      fireEvent.change(screen.getByPlaceholderText('e.g. 26CS01 or FAC_CS01'), {
        target: { value: '26CS99' }
      });
      fireEvent.change(screen.getByPlaceholderText('Full Name'), {
        target: { value: 'Charlie New' }
      });

      fireEvent.click(screen.getByRole('button', { name: 'Create User' }));

      await waitFor(() => {
        expect(mockCreate).toHaveBeenCalled();
        expect(screen.getByText(/Credentials Generated/i)).toBeInTheDocument();
        expect(screen.getByText('Temp@Password123')).toBeInTheDocument();
      });
    });

    it('allows deactivating and reactivating a user', async () => {
      vi.spyOn(adminApi, 'fetchUsers').mockResolvedValue({
        content: mockUsers,
        totalPages: 1,
        totalElements: 2
      });

      const mockUpdate = vi.spyOn(adminApi, 'updateUser').mockResolvedValue({
        ...mockUsers[0],
        isActive: false
      });

      render(
        <MemoryRouter>
          <AdminUsersPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('26CS01')).toBeInTheDocument();
      });

      const deactivateButtons = screen.getAllByRole('button', { name: /Deactivate/i });
      fireEvent.click(deactivateButtons[0]);

      expect(screen.getByText('Deactivate User Account?')).toBeInTheDocument();

      fireEvent.click(screen.getByRole('button', { name: 'Yes, Deactivate' }));

      await waitFor(() => {
        expect(mockUpdate).toHaveBeenCalledWith(1, expect.objectContaining({ isActive: false }));
      });
    });

    it('handles CSV import, shows result summary, and allows credentials download', async () => {
      vi.spyOn(adminApi, 'fetchUsers').mockResolvedValue({
        content: mockUsers,
        totalPages: 1,
        totalElements: 2
      });

      const mockImport = vi.spyOn(adminApi, 'importUsersCsv').mockResolvedValue({
        created: 5,
        skipped: 1,
        errors: [{ row: 3, message: 'Invalid role' }],
        credentials: 'college_id,temporary_password\n26CS05,Temp123\n'
      });

      const mockTrigger = vi.spyOn(surveyApi, 'triggerFileDownload').mockImplementation(() => {});

      render(
        <MemoryRouter>
          <AdminUsersPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('📄 Import CSV')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('📄 Import CSV'));

      expect(screen.getByText('Batch Import Users via CSV')).toBeInTheDocument();

      const fileInput = screen.getByLabelText(/SELECT CSV FILE/i);
      const testFile = new File(['college_id,name,role\n26CS05,Test,STUDENT'], 'users.csv', { type: 'text/csv' });
      fireEvent.change(fileInput, { target: { files: [testFile] } });

      fireEvent.click(screen.getByRole('button', { name: 'Start Import' }));

      await waitFor(() => {
        expect(mockImport).toHaveBeenCalled();
        expect(screen.getByText('Import Summary')).toBeInTheDocument();
        expect(screen.getByText('Row 3')).toBeInTheDocument();
        expect(screen.getByText('Invalid role')).toBeInTheDocument();
      });

      // Test credentials download
      const credsDownloadBtn = screen.getByRole('button', { name: /Download Credentials/i });
      expect(credsDownloadBtn).toBeInTheDocument();
      fireEvent.click(credsDownloadBtn);

      expect(mockTrigger).toHaveBeenCalled();
    });
  });

  describe('AdminTermsPage Operations', () => {
    const mockTerms = [
      {
        termId: 1,
        label: '2025-26 Even',
        startDate: '2026-01-01',
        endDate: '2026-06-30',
        isCurrent: false
      },
      {
        termId: 2,
        label: '2026-27 Odd',
        startDate: '2026-08-01',
        endDate: '2026-12-31',
        isCurrent: true
      }
    ];

    it('lists terms and displays CURRENT TERM badge', async () => {
      vi.spyOn(adminApi, 'fetchAcademicTerms').mockResolvedValue(mockTerms);

      render(
        <MemoryRouter>
          <AdminTermsPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('2025-26 Even')).toBeInTheDocument();
        expect(screen.getByText('2026-27 Odd')).toBeInTheDocument();
        expect(screen.getByText('⭐ CURRENT TERM')).toBeInTheDocument();
      });
    });

    it('allows setting a term as current', async () => {
      vi.spyOn(adminApi, 'fetchAcademicTerms').mockResolvedValue(mockTerms);
      const mockSetCurrent = vi.spyOn(adminApi, 'setCurrentAcademicTerm').mockResolvedValue({
        ...mockTerms[0],
        isCurrent: true
      });

      render(
        <MemoryRouter>
          <AdminTermsPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('2025-26 Even')).toBeInTheDocument();
      });

      const setCurrentBtn = screen.getByRole('button', { name: 'Set as Current' });
      fireEvent.click(setCurrentBtn);

      await waitFor(() => {
        expect(mockSetCurrent).toHaveBeenCalledWith(1);
      });
    });

    it('allows creating a new term via modal', async () => {
      vi.spyOn(adminApi, 'fetchAcademicTerms').mockResolvedValue(mockTerms);
      const mockCreateTerm = vi.spyOn(adminApi, 'createAcademicTerm').mockResolvedValue({
        termId: 3,
        label: '2027-28 Even',
        startDate: '2028-01-01',
        endDate: '2028-06-30',
        isCurrent: false
      });

      render(
        <MemoryRouter>
          <AdminTermsPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('+ Add Academic Term')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('+ Add Academic Term'));

      expect(screen.getByText('Add Academic Term')).toBeInTheDocument();

      fireEvent.change(screen.getByPlaceholderText('e.g. 2026-27 Odd'), {
        target: { value: '2027-28 Even' }
      });

      const dateInputs = screen.getAllByDisplayValue('');
      fireEvent.change(dateInputs[0], { target: { value: '2028-01-01' } });
      fireEvent.change(dateInputs[1], { target: { value: '2028-06-30' } });

      fireEvent.click(screen.getByRole('button', { name: 'Create Term' }));

      await waitFor(() => {
        expect(mockCreateTerm).toHaveBeenCalledWith(
          expect.objectContaining({
            label: '2027-28 Even',
            startDate: '2028-01-01',
            endDate: '2028-06-30'
          })
        );
      });
    });
  });
});
