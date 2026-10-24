import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import TargetingPanel from '../components/TargetingPanel';
import * as surveyApi from '../api/surveys';

vi.mock('../api/surveys', () => ({
  fetchEligibleCount: vi.fn().mockResolvedValue({ count: 118 })
}));

describe('TargetingPanel & Live Eligible Count', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('queries live eligible count for college-wide target filter', async () => {
    const handleChange = vi.fn();
    const adminUser = { role: 'ADMIN', branch: null };

    render(
      <TargetingPanel
        targetFilter={{ scope: 'ALL', branches: [], years: [], sections: [] }}
        user={adminUser}
        onChange={handleChange}
      />
    );

    // Debounced query runs
    await waitFor(() => {
      expect(surveyApi.fetchEligibleCount).toHaveBeenCalledWith({
        scope: 'ALL',
        branches: [],
        years: [],
        sections: []
      });
      expect(screen.getByText(/This will reach/i)).toBeInTheDocument();
      expect(screen.getByText('118')).toBeInTheDocument();
    });
  });

  it('restricts HOD user to their branch and scopes to FILTER', async () => {
    const handleChange = vi.fn();
    const hodUser = { role: 'HOD', branch: 'CSE' };

    render(
      <TargetingPanel
        targetFilter={{ scope: 'ALL', branches: [], years: [], sections: [] }}
        user={hodUser}
        onChange={handleChange}
      />
    );

    // HOD is automatically constrained
    await waitFor(() => {
      expect(handleChange).toHaveBeenCalledWith(
        expect.objectContaining({
          scope: 'FILTER',
          branches: ['CSE']
        })
      );
    });

    // All College radio should be disabled for HOD
    const allCollegeRadio = screen.getByRole('radio', { name: /All College/i });
    expect(allCollegeRadio).toBeDisabled();
  });

  it('toggles cohort filter parameters when clicking chips', async () => {
    const handleChange = vi.fn();
    const adminUser = { role: 'ADMIN', branch: null };

    render(
      <TargetingPanel
        targetFilter={{ scope: 'FILTER', branches: ['CSE'], years: [4], sections: ['A'] }}
        user={adminUser}
        onChange={handleChange}
      />
    );

    // Click on year 3 chip
    const yr3Btn = screen.getByRole('button', { name: /Year 3/i });
    fireEvent.click(yr3Btn);

    expect(handleChange).toHaveBeenCalledWith(
      expect.objectContaining({
        years: [4, 3]
      })
    );
  });
});
