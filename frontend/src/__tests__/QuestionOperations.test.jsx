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
  fetchPreset: vi.fn().mockResolvedValue({
    schemaVersion: 1,
    questions: [
      { key: 'q_first', type: 'RATING', prompt: 'First Question Prompt', rating: { min: 1, max: 5 } },
      { key: 'q_second', type: 'TEXT', prompt: 'Second Question Prompt', maxLength: 500 }
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

const renderBuilder = () => {
  return render(
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
};

describe('SurveyBuilder — Question Operations', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('adds a new question when clicking + Add Question', async () => {
    renderBuilder();

    await waitFor(() => {
      expect(screen.getAllByText(/First Question Prompt/i).length).toBeGreaterThan(0);
    });

    const addBtn = screen.getByRole('button', { name: /\+ Add Question/i });
    fireEvent.click(addBtn);

    // Should now have 3 questions
    expect(screen.getByText(/Questions \(3\)/i)).toBeInTheDocument();
    expect(screen.getAllByText(/New Question 3/i).length).toBeGreaterThan(0);
  });

  it('duplicates an existing question', async () => {
    renderBuilder();

    await waitFor(() => {
      expect(screen.getAllByText(/First Question Prompt/i).length).toBeGreaterThan(0);
    });

    const dupBtn = screen.getByLabelText(/Duplicate question 1/i);
    fireEvent.click(dupBtn);

    // Should now have 3 questions with a duplicate of Q1
    expect(screen.getByText(/Questions \(3\)/i)).toBeInTheDocument();
    expect(screen.getAllByText(/First Question Prompt \(Copy\)/i).length).toBeGreaterThan(0);
  });

  it('deletes a question when clicking delete', async () => {
    renderBuilder();

    await waitFor(() => {
      expect(screen.getAllByText(/First Question Prompt/i).length).toBeGreaterThan(0);
    });

    const delBtn = screen.getByLabelText(/Delete question 1/i);
    fireEvent.click(delBtn);

    // Q1 deleted, now 1 question left
    expect(screen.getByText(/Questions \(1\)/i)).toBeInTheDocument();
    expect(screen.queryByText('First Question Prompt')).not.toBeInTheDocument();
    expect(screen.getAllByText(/Second Question Prompt/i).length).toBeGreaterThan(0);
  });

  it('moves questions up and down in sequence', async () => {
    renderBuilder();

    await waitFor(() => {
      expect(screen.getAllByText(/First Question Prompt/i).length).toBeGreaterThan(0);
    });

    // Move Q1 down
    const moveDownBtn = screen.getByLabelText(/Move question 1 down/i);
    fireEvent.click(moveDownBtn);

    // Now Q1 should be Second Question Prompt and Q2 should be First Question Prompt
    const navItems = screen.getAllByRole('button', { name: /Question \d:/i });
    expect(navItems[0]).toHaveTextContent('Second Question Prompt');
    expect(navItems[1]).toHaveTextContent('First Question Prompt');

    // Move Q2 up
    const moveUpBtn = screen.getByLabelText(/Move question 2 up/i);
    fireEvent.click(moveUpBtn);

    // Restored original order
    const restoredItems = screen.getAllByRole('button', { name: /Question \d:/i });
    expect(restoredItems[0]).toHaveTextContent('First Question Prompt');
    expect(restoredItems[1]).toHaveTextContent('Second Question Prompt');
  });
});
