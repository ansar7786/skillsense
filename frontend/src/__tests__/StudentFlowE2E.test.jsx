import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import App from '../App';
import client from '../api/client';

describe('Student Full E2E Flow', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  const mockStudentUser = {
    userId: 6,
    collegeId: '26TEST06',
    name: 'Student 6',
    role: 'STUDENT',
    branch: 'IT',
    mustChangePassword: false,
  };

  const mockSurveyDetail = {
    surveyId: 25,
    title: 'Placement Readiness Survey',
    description: 'Assess technical readiness for campus placements.',
    category: 'PLACEMENT',
    isAnonymous: false,
    closesAt: '2026-12-31T23:59:59',
    questionSchema: {
      schemaVersion: 1,
      questions: [
        {
          key: 'java_skill',
          type: 'RATING',
          section: 'Technical Skills',
          prompt: 'Rate your confidence in Java',
          required: true,
          rating: { min: 1, max: 5, minLabel: 'Beginner', maxLabel: 'Expert' },
        },
        {
          key: 'target_role',
          type: 'SINGLE_CHOICE',
          section: 'Career Preferences',
          prompt: 'Which role are you targeting?',
          required: true,
          options: [
            { value: 'swe', label: 'Software Engineer' },
            { value: 'data', label: 'Data Analyst' },
          ],
        },
        {
          key: 'knows_syllabus',
          type: 'YES_NO',
          prompt: 'Do you understand the placement syllabus?',
          required: true,
        },
        {
          key: 'problems_solved',
          type: 'NUMBER',
          prompt: 'How many problems have you solved?',
          required: false,
          min: 0,
          max: 500,
        },
        {
          key: 'blockers',
          type: 'TEXT',
          prompt: 'What obstacles do you face?',
          required: false,
          maxLength: 300,
        },
      ],
    },
  };

  const mockResult = {
    readiness: 78.5,
    band: 'DEVELOPING',
    strongest_skill: 'java',
    weakest_skill: 'system_design',
    skills: [
      { skill: 'java', score: 80.0, target: 70.0, gap: 0.0, cohort_mean: 65.0 },
      { skill: 'system_design', score: 55.0, target: 65.0, gap: 10.0, cohort_mean: 50.0 },
    ],
    tips: [
      'Focus on system design fundamentals like caching and sharding.',
      'Maintain strong practice in Java core libraries.',
      'Participate in weekly mock interview peer sessions.',
    ],
    disclaimer: 'Self-assessed indicator, not an exam score',
  };

  it('executes full student user journey with autosave, submission, 409 duplicate handling, and result view', async () => {
    // Mock Axios client endpoints
    vi.spyOn(client, 'post').mockImplementation((url, body) => {
      if (url === '/auth/login') {
        return Promise.resolve({
          data: { token: 'mock-student-token-xyz', user: mockStudentUser },
        });
      }
      if (url === '/my/surveys/25/submit') {
        return Promise.resolve({ data: { submitted: true, responseId: 'resp-uuid-123' } });
      }
      return Promise.reject(new Error(`Unhandled POST: ${url}`));
    });

    vi.spyOn(client, 'get').mockImplementation((url) => {
      if (url === '/auth/me') {
        return Promise.resolve({ data: mockStudentUser });
      }
      if (url === '/my/surveys') {
        return Promise.resolve({
          data: [
            {
              surveyId: 25,
              title: 'Placement Readiness Survey',
              category: 'PLACEMENT',
              closesAt: '2026-12-31T23:59:59',
              isAnonymous: false,
              status: 'PENDING',
            },
          ],
        });
      }
      if (url === '/my/surveys/25') {
        return Promise.resolve({ data: mockSurveyDetail });
      }
      if (url === '/my/surveys/25/result') {
        return Promise.resolve({ data: mockResult });
      }
      return Promise.reject(new Error(`Unhandled GET: ${url}`));
    });

    // 1. Initial Render on /login
    render(
      <MemoryRouter initialEntries={['/login']}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getByText('Sign in with your institutional credentials')).toBeInTheDocument();

    // 2. Perform Login
    fireEvent.change(screen.getByLabelText(/College ID/i), { target: { value: '26TEST06' } });
    fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'Test@1234' } });
    fireEvent.click(screen.getByRole('button', { name: /Sign In/i }));

    // 3. Verify redirected to /student and surveys loaded
    await waitFor(() => {
      expect(screen.getByText('My Surveys')).toBeInTheDocument();
      expect(screen.getByText('Placement Readiness Survey')).toBeInTheDocument();
    });

    // Verify chips on survey card
    expect(screen.getByText('PLACEMENT')).toBeInTheDocument();
    expect(screen.getByText('Non-Anonymous')).toBeInTheDocument();
    expect(screen.getByText('PENDING')).toBeInTheDocument();

    // 4. Click Take Survey
    const takeSurveyBtn = screen.getByRole('link', { name: /Take Survey/i });
    fireEvent.click(takeSurveyBtn);

    // 5. Verify Take Survey page loaded with AnonymityBanner
    await waitFor(() => {
      expect(
        screen.getByText('Not anonymous: your name is recorded with your answers')
      ).toBeInTheDocument();
      expect(screen.getByText('Technical Skills')).toBeInTheDocument();
      expect(screen.getByText('Career Preferences')).toBeInTheDocument();
    });

    // 6. Answer partial questions to test autosave
    const rating4Btn = screen.getByRole('radio', { name: '4' });
    fireEvent.click(rating4Btn);

    const sweOption = screen.getByLabelText('Software Engineer');
    fireEvent.click(sweOption);

    // Verify localStorage contains draft answers
    const savedDraft = JSON.parse(localStorage.getItem('skillsense_draft_survey_25'));
    expect(savedDraft).toEqual({
      java_skill: 4,
      target_role: 'swe',
    });

    // 7. Answer remaining required question (YES_NO)
    const yesBtn = screen.getByRole('radio', { name: 'Yes' });
    fireEvent.click(yesBtn);

    // Optional NUMBER and TEXT
    const numInput = screen.getByLabelText(/How many problems have you solved/);
    fireEvent.change(numInput, { target: { value: '150' } });

    const textInput = screen.getByLabelText(/What obstacles do you face/);
    fireEvent.change(textInput, { target: { value: 'Time management' } });

    // 8. Submit Survey
    const submitBtn = screen.getByRole('button', { name: /Submit Survey/i });
    fireEvent.click(submitBtn);

    // 9. Verify Thank You screen and localStorage draft cleared
    await waitFor(() => {
      expect(screen.getByText('Thank You!')).toBeInTheDocument();
      expect(screen.getByText('Your response has been successfully submitted.')).toBeInTheDocument();
    });
    expect(localStorage.getItem('skillsense_draft_survey_25')).toBeNull();

    // 10. Click View Personal Result
    const viewResultBtn = screen.getByRole('link', { name: /View Your Personal Result/i });
    fireEvent.click(viewResultBtn);

    // 11. Verify Result Page
    await waitFor(() => {
      expect(screen.getByText('Personal Readiness Result')).toBeInTheDocument();
      expect(screen.getByText('79')).toBeInTheDocument(); // Math.round(78.5)
      expect(screen.getByText('DEVELOPING')).toBeInTheDocument();
      expect(screen.getByText('Self-assessed indicator, not an exam score')).toBeInTheDocument();
    });

    expect(screen.getAllByText('java').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('system_design').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Recommended Next Steps')).toBeInTheDocument();
    expect(screen.getByText(/Focus on system design fundamentals/)).toBeInTheDocument();
  });

  it('shows clear 409 error when student attempts duplicate submission', async () => {
    vi.spyOn(client, 'get').mockImplementation((url) => {
      if (url === '/auth/me') return Promise.resolve({ data: mockStudentUser });
      if (url === '/my/surveys/25') return Promise.resolve({ data: mockSurveyDetail });
      return Promise.reject(new Error(`Unhandled GET: ${url}`));
    });

    // Mock 409 ALREADY_SUBMITTED on submit
    vi.spyOn(client, 'post').mockImplementation((url) => {
      if (url === '/my/surveys/25/submit') {
        const err = new Error('Request failed with status code 409');
        err.response = {
          status: 409,
          data: {
            status: 409,
            code: 'ALREADY_SUBMITTED',
            message: 'You have already submitted this survey',
          },
        };
        return Promise.reject(err);
      }
      return Promise.reject(new Error(`Unhandled POST: ${url}`));
    });

    localStorage.setItem('skillsense_token', 'mock-token');
    localStorage.setItem('skillsense_user', JSON.stringify(mockStudentUser));

    render(
      <MemoryRouter initialEntries={['/student/surveys/25']}>
        <App />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Placement Readiness Survey')).toBeInTheDocument();
    });

    // Answer required questions
    fireEvent.click(screen.getByRole('radio', { name: '5' }));
    fireEvent.click(screen.getByLabelText('Software Engineer'));
    fireEvent.click(screen.getByRole('radio', { name: 'Yes' }));

    // Click submit
    fireEvent.click(screen.getByRole('button', { name: /Submit Survey/i }));

    // Verify 409 ALREADY_SUBMITTED is clearly shown
    await waitFor(() => {
      expect(screen.getByText('You have already submitted this survey.')).toBeInTheDocument();
    });
  });

  it('blocks student from accessing /officials and redirects to /403', async () => {
    localStorage.setItem('skillsense_token', 'mock-token');
    localStorage.setItem('skillsense_user', JSON.stringify(mockStudentUser));

    vi.spyOn(client, 'get').mockImplementation((url) => {
      if (url === '/auth/me') return Promise.resolve({ data: mockStudentUser });
      return Promise.reject(new Error(`Unhandled GET: ${url}`));
    });

    render(
      <MemoryRouter initialEntries={['/officials']}>
        <App />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('403')).toBeInTheDocument();
      expect(screen.getByText('Access Forbidden')).toBeInTheDocument();
      expect(
        screen.getByText('You do not have the required permissions to view this page.')
      ).toBeInTheDocument();
    });
  });
});
