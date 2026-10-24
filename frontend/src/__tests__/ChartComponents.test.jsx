import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import ChartCard from '../components/ChartCard';
import SkillGapChart from '../components/SkillGapChart';
import BandDonut from '../components/BandDonut';
import TrendChart from '../components/TrendChart';
import InsightPanel from '../components/InsightPanel';
import WaitingBanner from '../components/WaitingBanner';

// Mock react-chartjs-2
vi.mock('react-chartjs-2', () => ({
  Bar: (props) => <div data-testid="bar-chart" data-options={JSON.stringify(props.options || {})}>Bar Chart</div>,
  Doughnut: (props) => <div data-testid="doughnut-chart" data-options={JSON.stringify(props.options || {})}>Doughnut Chart</div>,
}));

describe('Phase 9 Chart & Analytics Components', () => {
  // Test 4: K<5 privacy state / WaitingBanner
  describe('WaitingBanner (K=5 Privacy State)', () => {
    it('renders privacy explanation, threshold, and current responses count', () => {
      render(<WaitingBanner have={2} need={5} />);

      expect(screen.getByText('Analytics are not available yet')).toBeInTheDocument();
      expect(screen.getByText(/at least/)).toBeInTheDocument();
      expect(screen.getByText(/5 responses/)).toBeInTheDocument();
      expect(screen.getByText(/40% towards privacy threshold/)).toBeInTheDocument();
    });
  });

  // Test 5: Skill Gap Chart
  describe('SkillGapChart', () => {
    const mockSkills = [
      { skill: 'dsa', target: 80.0, mean: 65.5, gap: -14.5, share_below_target: 0.62, priority: 1 },
      { skill: 'java', target: 70.0, mean: 74.0, gap: 4.0, share_below_target: 0.25, priority: 2 },
    ];

    it('renders horizontal bars and supports toggle to accessible table', () => {
      render(<SkillGapChart skills={mockSkills} />);

      expect(screen.getByText(/Skill Gap Analysis/)).toBeInTheDocument();
      expect(screen.getByTestId('bar-chart')).toBeInTheDocument();

      // Toggle to table
      const toggleBtn = screen.getByRole('button', { name: /switch to table/i });
      fireEvent.click(toggleBtn);

      expect(screen.getByText('DSA')).toBeInTheDocument();
      expect(screen.getByText('JAVA')).toBeInTheDocument();
      expect(screen.getByText('65.5')).toBeInTheDocument();
      expect(screen.getByText('-14.5')).toBeInTheDocument();
      expect(screen.getByText('+4.0')).toBeInTheDocument();
    });

    it('renders empty message when no skills provided', () => {
      render(<SkillGapChart skills={[]} />);
      expect(screen.getByText('No skill scoring data available for this survey.')).toBeInTheDocument();
    });
  });

  // Test 6: Band Distribution
  describe('BandDonut', () => {
    const mockBands = {
      READY: 0.45,
      DEVELOPING: 0.35,
      NEEDS_SUPPORT: 0.20,
    };

    it('renders doughnut chart and allows switching to table view', () => {
      render(<BandDonut bands={mockBands} totalResponses={100} />);

      expect(screen.getByText('Readiness Band Distribution')).toBeInTheDocument();
      expect(screen.getByTestId('doughnut-chart')).toBeInTheDocument();

      // Switch to table
      const toggleBtn = screen.getByRole('button', { name: /switch to table/i });
      fireEvent.click(toggleBtn);

      expect(screen.getByText('READY')).toBeInTheDocument();
      expect(screen.getByText('DEVELOPING')).toBeInTheDocument();
      expect(screen.getByText('NEEDS SUPPORT')).toBeInTheDocument();
      expect(screen.getByText('45.0%')).toBeInTheDocument();
      expect(screen.getByText('35.0%')).toBeInTheDocument();
      expect(screen.getByText('20.0%')).toBeInTheDocument();
    });
  });

  // Test 7: Trend Chart
  describe('TrendChart', () => {
    const mockTrendData = {
      overall: { now: 72.5, previous: 68.0, delta: 4.5, direction: 'IMPROVED', low_confidence: false },
      skills: [
        { skill: 'dsa', now: 65.0, previous: 58.0, delta: 7.0, direction: 'IMPROVED', low_confidence: false },
        { skill: 'system_design', now: 70.0, previous: 70.5, delta: -0.5, direction: 'FLAT', low_confidence: false },
      ],
    };

    it('renders trend comparison with IMPROVED direction badge', () => {
      render(<TrendChart trendData={mockTrendData} />);

      expect(screen.getByText('Version-over-Version Comparison')).toBeInTheDocument();
      expect(screen.getByText(/Overall: IMPROVED/)).toBeInTheDocument();
      expect(screen.getByTestId('bar-chart')).toBeInTheDocument();
    });

    it('renders friendly version 1 error state when no previous version exists', () => {
      render(<TrendChart error="No previous version exists for this survey template" />);
      expect(screen.getByText('No Historical Comparison')).toBeInTheDocument();
      expect(screen.getByText(/version 1 of this survey template/i)).toBeInTheDocument();
    });

    it('renders low confidence alert when flag is set', () => {
      const lowConfData = {
        overall: { now: 70, previous: 60, delta: 10, direction: 'IMPROVED', low_confidence: true },
        skills: [{ skill: 'python', now: 70, previous: 60, delta: 10, direction: 'IMPROVED', low_confidence: true }],
      };
      render(<TrendChart trendData={lowConfData} />);
      expect(screen.getByText(/Low Confidence:/)).toBeInTheDocument();
    });
  });

  // Test 8 & 9: AI Insights & Rules Fallback
  describe('InsightPanel', () => {
    const mockAiInsights = {
      headline: 'DSA and System Design require immediate curriculum attention',
      key_findings: [
        'Over 60% of students scored below the threshold on dynamic programming.',
        'Core communication ratings improved significantly compared to last semester.',
      ],
      recommendations: [
        {
          priority: 1,
          title: 'Organize DSA Coding Bootcamp',
          action: 'Schedule weekend lab sessions focused on graph traversal.',
          rationale: 'Addresses primary technical gap in placement readiness.',
          suggested_owner: 'Placement Faculty Lead',
        },
      ],
      watch_outs: [
        'Low attendance in afternoon elective sessions.',
      ],
      source: 'AI',
      generatedAt: '2026-10-24T10:00:00Z',
    };

    it('renders AI insights with AI source badge and structured recommendations', () => {
      render(<InsightPanel insights={mockAiInsights} onRefresh={() => {}} />);

      expect(screen.getByText(/AI Generated \(Gemini\)/)).toBeInTheDocument();
      expect(screen.getByText('DSA and System Design require immediate curriculum attention')).toBeInTheDocument();
      expect(screen.getByText(/Over 60% of students/)).toBeInTheDocument();
      expect(screen.getByText('Organize DSA Coding Bootcamp')).toBeInTheDocument();
      expect(screen.getByText('P1')).toBeInTheDocument();
      expect(screen.getByText(/Placement Faculty Lead/)).toBeInTheDocument();
      expect(screen.getByText('Low attendance in afternoon elective sessions.')).toBeInTheDocument();
    });

    it('clearly distinguishes RULES fallback when source is RULES', () => {
      const rulesInsights = {
        ...mockAiInsights,
        source: 'RULES',
      };
      render(<InsightPanel insights={rulesInsights} onRefresh={() => {}} />);

      expect(screen.getByText(/Generated without AI \(Rules-based\)/)).toBeInTheDocument();
    });

    it('invokes onRefresh when clicking Refresh Insights', () => {
      const handleRefresh = vi.fn();
      render(<InsightPanel insights={mockAiInsights} onRefresh={handleRefresh} />);

      const btn = screen.getByRole('button', { name: /refresh insights/i });
      fireEvent.click(btn);
      expect(handleRefresh).toHaveBeenCalledTimes(1);
    });
  });

  // ChartCard accessibility toggle test
  describe('ChartCard', () => {
    const tableData = [
      { label: 'Option A', count: 40, percentage: 0.4 },
      { label: 'Option B', count: 60, percentage: 0.6 },
    ];

    it('renders question prompt, n, mean and toggles table view', () => {
      render(
        <ChartCard
          prompt="How confident are you in Java?"
          type="RATING"
          n={100}
          mean={3.8}
          tableData={tableData}
        >
          <div data-testid="child-chart">Chart Content</div>
        </ChartCard>
      );

      expect(screen.getByText('How confident are you in Java?')).toBeInTheDocument();
      expect(screen.getByText('RATING')).toBeInTheDocument();
      expect(screen.getByText(/100 responses/)).toBeInTheDocument();
      expect(screen.getByText(/3.8/)).toBeInTheDocument();
      expect(screen.getByTestId('child-chart')).toBeInTheDocument();

      // Toggle table view
      const toggle = screen.getByRole('button', { name: /view as table/i });
      fireEvent.click(toggle);

      expect(screen.queryByTestId('child-chart')).not.toBeInTheDocument();
      expect(screen.getByText('Option A')).toBeInTheDocument();
      expect(screen.getByText('40.0%')).toBeInTheDocument();
    });
  });
});
