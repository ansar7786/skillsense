import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import SurveyPreviewModal from '../components/SurveyPreviewModal';

describe('SurveyPreviewModal — Student Safe Rendering', () => {
  const mockSurveyWithScoring = {
    surveyId: 10,
    title: 'Final Term Placement Readiness Assessment',
    description: 'Help us benchmark institutional competency levels.',
    category: 'PLACEMENT',
    termLabel: '2026-27 Odd',
    isAnonymous: true,
    skillTargets: {
      java: 85,
      dsa: 75
    },
    questionSchema: {
      schemaVersion: 1,
      questions: [
        {
          key: 'java_eval',
          type: 'RATING',
          prompt: 'Rate your Java competency and OOP principles',
          required: true,
          rating: { min: 1, max: 5, minLabel: 'Novice', maxLabel: 'Master' },
          scoring: {
            skill: 'java',
            weight: 3.5
          }
        },
        {
          key: 'mock_choice',
          type: 'SINGLE_CHOICE',
          prompt: 'How many mock interviews completed?',
          required: true,
          options: [
            { value: 'zero', label: 'Zero completed', score: 0 },
            { value: 'two', label: 'Two completed', score: 100 }
          ],
          scoring: {
            skill: 'interview',
            weight: 2.0
          }
        }
      ]
    }
  };

  it('renders student-facing questions and anonymity banner safely', () => {
    const handleClose = vi.fn();
    render(
      <SurveyPreviewModal
        survey={mockSurveyWithScoring}
        isOpen={true}
        onClose={handleClose}
      />
    );

    // Banner and titles render
    expect(screen.getByText('Student Preview')).toBeInTheDocument();
    expect(screen.getByText('Final Term Placement Readiness Assessment')).toBeInTheDocument();
    expect(screen.getByText(/your name is not stored with your answers/i)).toBeInTheDocument();
    expect(screen.getByText('Rate your Java competency and OOP principles')).toBeInTheDocument();
    expect(screen.getByText('How many mock interviews completed?')).toBeInTheDocument();
    expect(screen.getByText('Zero completed')).toBeInTheDocument();
    expect(screen.getByText('Two completed')).toBeInTheDocument();

    // CRITICAL: Internal scoring, weights, and skill targets MUST NOT be rendered
    expect(screen.queryByText(/weight: 3.5/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/weight: 2.0/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/85%/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/75%/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/score: 100/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Scored/i)).not.toBeInTheDocument();
  });
});
