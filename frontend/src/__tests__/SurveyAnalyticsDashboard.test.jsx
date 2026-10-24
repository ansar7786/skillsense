import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import SurveyAnalyticsDashboardPage from '../pages/SurveyAnalyticsDashboardPage';
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

describe('SurveyAnalyticsDashboardPage', () => {
  const mockSurvey = {
    surveyId: 10,
    title: 'Final Term Placement Readiness Assessment',
    description: 'Comprehensive evaluation of student placement preparedness.',
    category: 'PLACEMENT',
    status: 'ACTIVE',
    isAnonymous: false,
    version: 2,
    eligibleCount: 120,
    termLabel: 'Spring 2026',
    questionSchema: {
      questions: [
        { key: 'q_java', type: 'RATING', prompt: 'Java proficiency level', required: true },
        { key: 'q_role', type: 'SINGLE_CHOICE', prompt: 'Desired job role', required: true },
        { key: 'q_cert', type: 'YES_NO', prompt: 'Do you hold cloud certifications?', required: true },
        { key: 'q_comments', type: 'TEXT', prompt: 'Areas where faculty can provide additional support', required: false },
      ],
    },
  };

  const mockAnalyticsAvailable = {
    available: true,
    have: 45,
    need: 5,
    data: {
      response_count: 45,
      completion_rate: 0.375,
      index: 76.4,
      questions: [
        {
          key: 'q_java',
          type: 'RATING',
          n: 45,
          mean: 3.8,
          median: 4.0,
          distribution: { '1': 2, '2': 5, '3': 12, '4': 18, '5': 8 },
        },
        {
          key: 'q_role',
          type: 'SINGLE_CHOICE',
          n: 45,
          distribution: { 'Full Stack': 25, 'Data Science': 15, 'DevOps': 5 },
        },
        {
          key: 'q_cert',
          type: 'YES_NO',
          n: 45,
          share_yes: 0.60,
        },
        {
          key: 'q_comments',
          type: 'TEXT',
          n: 20,
        },
      ],
      skills: [
        {
          skill: 'cloud_architecture',
          target: 75.0,
          mean: 68.0,
          gap: -7.0,
          share_below_target: 0.40,
          priority: 1,
        },
      ],
      bands: {
        READY: 0.50,
        DEVELOPING: 0.35,
        NEEDS_SUPPORT: 0.15,
      },
      groups: {
        branch: {
          CSE: { n: 30, index: 78.5 },
          ECE: { n: 15, index: 72.0 },
        },
        year: {
          '4': { n: 45, index: 76.4 },
        },
        suppressed: ['CIVIL (< 5 responses)'],
      },
      text_themes: {
        q_comments: [
          { term: 'practice', count: 12 },
          { term: 'mock interviews', count: 8 },
        ],
      },
    },
  };

  const mockTrend = {
    overall: { now: 76.4, previous: 71.0, delta: 5.4, direction: 'IMPROVED', low_confidence: false },
    skills: [
      { skill: 'cloud_architecture', now: 68.0, previous: 62.0, delta: 6.0, direction: 'IMPROVED', low_confidence: false },
    ],
  };

  const mockInsights = {
    headline: 'Strong cloud architecture foundation with targeted improvement needed in mock interviews',
    key_findings: [
      '60% of students have industry certifications.',
      'Core Java ratings show positive upward trend.',
    ],
    recommendations: [
      {
        priority: 1,
        title: 'Conduct Mock Technical Interviews',
        action: 'Schedule 1-on-1 interview simulations.',
        rationale: 'Addresses student feedback for practical interview readiness.',
        suggested_owner: 'Training & Placement Cell',
      },
    ],
    watch_outs: ['Certifications higher in CSE compared to other branches.'],
    source: 'AI',
    generatedAt: '2026-10-24T12:00:00Z',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderDashboard = (surveyId = '10') => {
    return render(
      <MemoryRouter initialEntries={[`/officials/surveys/${surveyId}/analytics`]}>
        <Routes>
          <Route
            path="/officials/surveys/:id/analytics"
            element={<SurveyAnalyticsDashboardPage />}
          />
        </Routes>
      </MemoryRouter>
    );
  };

  it('1. shows loading skeleton on initial mount', () => {
    vi.spyOn(surveyApi, 'fetchSurvey').mockReturnValue(new Promise(() => {}));
    vi.spyOn(surveyApi, 'fetchSurveyAnalytics').mockReturnValue(new Promise(() => {}));

    renderDashboard();

    expect(screen.getByText('Loading survey analytics dashboard...')).toBeInTheDocument();
  });

  it('2. renders survey header, title, badges, and overview tiles when data loads', async () => {
    vi.spyOn(surveyApi, 'fetchSurvey').mockResolvedValue(mockSurvey);
    vi.spyOn(surveyApi, 'fetchSurveyAnalytics').mockResolvedValue(mockAnalyticsAvailable);
    vi.spyOn(surveyApi, 'fetchSurveyTrend').mockResolvedValue(mockTrend);
    vi.spyOn(surveyApi, 'fetchSurveyInsights').mockResolvedValue(mockInsights);

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText('Final Term Placement Readiness Assessment')).toBeInTheDocument();
    });

    expect(screen.getByText('PLACEMENT')).toBeInTheDocument();
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
    expect(screen.getByText('👤 Non-Anonymous')).toBeInTheDocument();
    expect(screen.getByText('v2')).toBeInTheDocument();
    expect(screen.getAllByText('45').length).toBeGreaterThan(0); // Responses in header/KPI
    expect(screen.getAllByText('38%').length).toBeGreaterThan(0); // 45/120 = 37.5% -> 38% in header and overview
    expect(screen.getAllByText('76.4').length).toBeGreaterThan(0); // Satisfaction Index in header and overview
  });

  it('3. handles K<5 privacy state when available is false', async () => {
    const mockAnonSurvey = {
      ...mockSurvey,
      isAnonymous: true,
    };
    const mockUnderThresholdAnalytics = {
      available: false,
      have: 2,
      need: 5,
      data: {},
    };

    vi.spyOn(surveyApi, 'fetchSurvey').mockResolvedValue(mockAnonSurvey);
    vi.spyOn(surveyApi, 'fetchSurveyAnalytics').mockResolvedValue(mockUnderThresholdAnalytics);
    vi.spyOn(surveyApi, 'fetchSurveyTrend').mockRejectedValue({ response: { status: 404 } });

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText('Analytics are not available yet')).toBeInTheDocument();
    });

    const waitingRegion = screen.getByRole('region', { name: /privacy waiting banner/i });
    expect(waitingRegion).toHaveTextContent(/at least 5 responses/i);
    expect(waitingRegion).toHaveTextContent(/2 of 5 responses collected/i);
    expect(waitingRegion).toHaveTextContent(/40% towards privacy threshold/i);
    // Tabs and charts must NOT be shown
    expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
  });

  it('4. navigates through dashboard tabs: Results, Skills, Breakdown, Trend, Insights, Comments', async () => {
    vi.spyOn(surveyApi, 'fetchSurvey').mockResolvedValue(mockSurvey);
    vi.spyOn(surveyApi, 'fetchSurveyAnalytics').mockResolvedValue(mockAnalyticsAvailable);
    vi.spyOn(surveyApi, 'fetchSurveyTrend').mockResolvedValue(mockTrend);
    vi.spyOn(surveyApi, 'fetchSurveyInsights').mockResolvedValue(mockInsights);
    vi.spyOn(surveyApi, 'fetchSurveyTexts').mockResolvedValue(['Need more hands-on labs.', 'Interview prep was great.']);

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText('Final Term Placement Readiness Assessment')).toBeInTheDocument();
    });

    // 1. Click Results Tab
    fireEvent.click(screen.getByRole('tab', { name: 'Results' }));
    expect(screen.getByText('Java proficiency level')).toBeInTheDocument();
    expect(screen.getByText('Desired job role')).toBeInTheDocument();
    expect(screen.getByText('Do you hold cloud certifications?')).toBeInTheDocument();

    // 2. Click Skills Tab
    fireEvent.click(screen.getByRole('tab', { name: 'Skills' }));
    expect(screen.getByText(/Skill Gap Analysis/)).toBeInTheDocument();
    expect(screen.getByText('Readiness Band Distribution')).toBeInTheDocument();

    // 3. Click Breakdown Tab
    fireEvent.click(screen.getByRole('tab', { name: 'Breakdown' }));
    expect(screen.getByText('Branch Comparison')).toBeInTheDocument();
    expect(screen.getByText('CSE')).toBeInTheDocument();
    expect(screen.getByText('ECE')).toBeInTheDocument();
    expect(screen.getByText('Hidden: fewer than 5 responses')).toBeInTheDocument();

    // 4. Click Trend Tab
    fireEvent.click(screen.getByRole('tab', { name: 'Trend' }));
    expect(screen.getByText('Version-over-Version Comparison')).toBeInTheDocument();
    expect(screen.getByText(/IMPROVED/)).toBeInTheDocument();

    // 5. Click Insights Tab
    fireEvent.click(screen.getByRole('tab', { name: 'Insights' }));
    expect(screen.getByText(/AI Generated \(Gemini\)/)).toBeInTheDocument();
    expect(screen.getByText(/Strong cloud architecture foundation/)).toBeInTheDocument();
    expect(screen.getByText('Conduct Mock Technical Interviews')).toBeInTheDocument();

    // 6. Click Comments Tab
    fireEvent.click(screen.getByRole('tab', { name: 'Comments' }));
    expect(screen.getByText('Qualitative Feedback')).toBeInTheDocument();
    expect(screen.getByText(/practice/)).toBeInTheDocument();
  });

  it('5. handles 403 Forbidden error', async () => {
    vi.spyOn(surveyApi, 'fetchSurvey').mockRejectedValue({
      response: { status: 403, data: { message: 'Access Denied' } },
    });

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText(/Access Denied: You do not have permission/)).toBeInTheDocument();
    });
  });

  it('6. handles 404 Not Found error', async () => {
    vi.spyOn(surveyApi, 'fetchSurvey').mockRejectedValue({
      response: { status: 404, data: { message: 'Survey not found' } },
    });

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText('Survey not found.')).toBeInTheDocument();
    });
  });

  it('7. handles 503 ANALYTICS_UNAVAILABLE error with professional retry message', async () => {
    vi.spyOn(surveyApi, 'fetchSurvey').mockResolvedValue(mockSurvey);
    vi.spyOn(surveyApi, 'fetchSurveyAnalytics').mockRejectedValue({
      response: { status: 503, data: { code: 'ANALYTICS_UNAVAILABLE' } },
    });

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText('Analytics are temporarily unavailable. Please try again later.')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: /retry loading/i })).toBeInTheDocument();
  });

  it('8. triggers refresh of AI insights when user clicks Refresh Insights', async () => {
    vi.spyOn(surveyApi, 'fetchSurvey').mockResolvedValue(mockSurvey);
    vi.spyOn(surveyApi, 'fetchSurveyAnalytics').mockResolvedValue(mockAnalyticsAvailable);
    vi.spyOn(surveyApi, 'fetchSurveyTrend').mockResolvedValue(mockTrend);
    const insightSpy = vi.spyOn(surveyApi, 'fetchSurveyInsights')
      .mockResolvedValueOnce(mockInsights)
      .mockResolvedValueOnce({
        ...mockInsights,
        headline: 'Regenerated fresh insights successfully',
      });

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText('Final Term Placement Readiness Assessment')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('tab', { name: 'Insights' }));
    await waitFor(() => {
      expect(screen.getByText(/Strong cloud architecture foundation/)).toBeInTheDocument();
    });

    const refreshBtn = screen.getByRole('button', { name: /refresh insights/i });
    fireEvent.click(refreshBtn);

    await waitFor(() => {
      expect(insightSpy).toHaveBeenCalledWith('10', true);
    });
  });
});
