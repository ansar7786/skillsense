import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import SurveyBuilderPage from '../pages/SurveyBuilderPage';
import AuthContext from '../auth/AuthContext';
import * as surveyApi from '../api/surveys';

vi.mock('../api/surveys', () => ({
  fetchTerms: vi.fn().mockResolvedValue([
    { termId: 1, label: '2026-27 Odd', isCurrent: true }
  ]),
  fetchSurvey: vi.fn(),
  createSurvey: vi.fn(),
  updateSurvey: vi.fn(),
  activateSurvey: vi.fn(),
  fetchPreset: vi.fn().mockImplementation((category) => {
    if (category === 'PLACEMENT') {
      return Promise.resolve({
        schemaVersion: 1,
        questions: [
          {
            key: 'java_skill',
            type: 'RATING',
            prompt: 'How confident are you in core Java and OOP?',
            required: true,
            rating: { min: 1, max: 5 },
            scoring: { skill: 'java', weight: 1.0 }
          },
          {
            key: 'dsa_skill',
            type: 'RATING',
            prompt: 'Rate proficiency in Data Structures and Algorithms',
            required: true,
            rating: { min: 1, max: 5 },
            scoring: { skill: 'dsa', weight: 2.0 }
          }
        ]
      });
    }
    return Promise.resolve({
      schemaVersion: 1,
      questions: [
        {
          key: 'academic_satisfaction',
          type: 'RATING',
          prompt: 'Rate syllabus depth and laboratory coverage',
          required: true,
          rating: { min: 1, max: 5 }
        }
      ]
    });
  }),
  fetchEligibleCount: vi.fn().mockResolvedValue({ count: 118 })
}));

const mockAuthUser = {
  userId: 2,
  collegeId: 'test_admin_survey',
  role: 'ADMIN',
  name: 'Test Administrator'
};

describe('SurveyBuilder — Preset Loading', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads PLACEMENT starter preset on initialization and populates questions', async () => {
    render(
      <AuthContext.Provider
        value={{
          user: mockAuthUser,
          isAuthenticated: true,
          loading: false
        }}
      >
        <MemoryRouter initialEntries={['/officials/surveys/new']}>
          <SurveyBuilderPage />
        </MemoryRouter>
      </AuthContext.Provider>
    );

    // Initial load should fetch PLACEMENT preset
    await waitFor(() => {
      expect(surveyApi.fetchPreset).toHaveBeenCalledWith('PLACEMENT');
      expect(screen.getAllByText(/How confident are you in core Java/i).length).toBeGreaterThan(0);
      expect(screen.getAllByText(/Rate proficiency in Data Structures and Algorithms/i).length).toBeGreaterThan(0);
    });

    // Verify 2 questions were populated
    expect(screen.getByText(/Questions \(2\)/i)).toBeInTheDocument();
  });
});
