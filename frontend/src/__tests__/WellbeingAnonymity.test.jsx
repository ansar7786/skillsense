import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import SurveyBuilderPage from '../pages/SurveyBuilderPage';
import AuthContext from '../auth/AuthContext';
import * as surveyApi from '../api/surveys';

// Mock surveys API
vi.mock('../api/surveys', () => ({
  fetchTerms: vi.fn().mockResolvedValue([
    { termId: 1, label: '2026-27 Odd', isCurrent: true }
  ]),
  fetchSurvey: vi.fn(),
  createSurvey: vi.fn(),
  updateSurvey: vi.fn(),
  activateSurvey: vi.fn(),
  fetchPreset: vi.fn().mockResolvedValue({
    schemaVersion: 1,
    questions: [
      { key: 'q1', type: 'RATING', prompt: 'Sample question', rating: { min: 1, max: 5 } }
    ]
  }),
  fetchEligibleCount: vi.fn().mockResolvedValue({ count: 118 })
}));

const mockAuthUser = {
  userId: 2,
  collegeId: 'test_admin_survey',
  role: 'ADMIN',
  name: 'Test Administrator'
};

const renderWithContext = (ui) => {
  return render(
    <AuthContext.Provider
      value={{
        user: mockAuthUser,
        isAuthenticated: true,
        loading: false
      }}
    >
      <MemoryRouter initialEntries={['/officials/surveys/new']}>
        {ui}
      </MemoryRouter>
    </AuthContext.Provider>
  );
};

describe('WELLBEING Category Anonymity Enforcer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('locks isAnonymous to true and disables checkbox when category is WELLBEING', async () => {
    const { container } = renderWithContext(<SurveyBuilderPage />);

    // Wait for builder category select to load
    await waitFor(() => {
      expect(container.querySelector('#builder-category')).toBeInTheDocument();
    });

    const categorySelect = container.querySelector('#builder-category');
    const anonCheckbox = screen.getByRole('checkbox', { name: /Strictly Anonymous Survey/i });

    // Initially PLACEMENT: anonymity checkbox should NOT be disabled
    expect(anonCheckbox).not.toBeDisabled();

    // Select WELLBEING category
    fireEvent.change(categorySelect, { target: { value: 'WELLBEING' } });

    // Confirm category switch if confirmation dialog pops up
    const loadPresetBtn = screen.queryByRole('button', { name: /Load Starter Preset/i });
    if (loadPresetBtn) {
      fireEvent.click(loadPresetBtn);
    }

    // After switching to WELLBEING:
    await waitFor(() => {
      expect(anonCheckbox).toBeChecked();
      expect(anonCheckbox).toBeDisabled();
    });

    // Verify warning text is displayed
    expect(screen.getByText(/Wellbeing surveys are strictly anonymous to protect student psychological safety/i)).toBeInTheDocument();
  });

  it('unlocks anonymity checkbox when changing category away from WELLBEING', async () => {
    const { container } = renderWithContext(<SurveyBuilderPage />);

    await waitFor(() => {
      expect(container.querySelector('#builder-category')).toBeInTheDocument();
    });

    const categorySelect = container.querySelector('#builder-category');
    const anonCheckbox = screen.getByRole('checkbox', { name: /Strictly Anonymous Survey/i });

    // Set to WELLBEING
    fireEvent.change(categorySelect, { target: { value: 'WELLBEING' } });
    const loadBtn = screen.queryByRole('button', { name: /Load Starter Preset/i });
    if (loadBtn) fireEvent.click(loadBtn);

    await waitFor(() => {
      expect(anonCheckbox).toBeDisabled();
    });

    // Switch to ACADEMIC
    fireEvent.change(categorySelect, { target: { value: 'ACADEMIC' } });
    const loadBtn2 = screen.queryByRole('button', { name: /Load Starter Preset/i });
    if (loadBtn2) fireEvent.click(loadBtn2);

    // After switching back to ACADEMIC, switch should be unlocked
    await waitFor(() => {
      expect(anonCheckbox).not.toBeDisabled();
    });

    // Can freely toggle
    fireEvent.click(anonCheckbox);
    expect(anonCheckbox).not.toBeChecked();
  });
});
