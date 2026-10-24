import React, { useState, useEffect, useRef } from 'react';
import { fetchEligibleCount } from '../api/surveys';

const AVAILABLE_BRANCHES = ['CSE', 'ECE', 'MECH', 'CIVIL', 'IT', 'EEE', 'AIML'];
const AVAILABLE_YEARS = [1, 2, 3, 4];
const AVAILABLE_SECTIONS = ['A', 'B', 'C'];

export default function TargetingPanel({
  targetFilter = { scope: 'ALL', branches: [], years: [], sections: [] },
  user,
  onChange
}) {
  const [eligibleCount, setEligibleCount] = useState(null);
  const [loadingCount, setLoadingCount] = useState(false);
  const [countError, setCountError] = useState(null);

  const debounceTimerRef = useRef(null);

  const isHod = user?.role === 'HOD';
  const isFaculty = user?.role === 'FACULTY';
  const userBranch = user?.branch || 'CSE';

  // Enforce role restrictions
  useEffect(() => {
    if (isHod || isFaculty) {
      if (targetFilter.scope !== 'FILTER' || !targetFilter.branches?.includes(userBranch)) {
        onChange({
          ...targetFilter,
          scope: 'FILTER',
          branches: [userBranch]
        });
      }
    }
  }, [isHod, isFaculty, userBranch]);

  // Debounced eligible count fetch
  useEffect(() => {
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }

    setLoadingCount(true);
    setCountError(null);

    debounceTimerRef.current = setTimeout(async () => {
      try {
        const payload = {
          scope: targetFilter.scope || 'ALL',
          branches: targetFilter.branches || [],
          years: targetFilter.years || [],
          sections: targetFilter.sections || []
        };
        const res = await fetchEligibleCount(payload);
        setEligibleCount(res.count ?? res);
      } catch (err) {
        setCountError('Could not calculate reach');
      } finally {
        setLoadingCount(false);
      }
    }, 350);

    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, [targetFilter.scope, JSON.stringify(targetFilter.branches), JSON.stringify(targetFilter.years), JSON.stringify(targetFilter.sections)]);

  const handleScopeChange = (newScope) => {
    if (isHod || isFaculty) return; // Disallowed
    onChange({
      ...targetFilter,
      scope: newScope
    });
  };

  const toggleBranch = (branch) => {
    if (isHod) return; // HOD locked to own branch
    const current = targetFilter.branches || [];
    const next = current.includes(branch)
      ? current.filter((b) => b !== branch)
      : [...current, branch];
    onChange({ ...targetFilter, branches: next });
  };

  const toggleYear = (yr) => {
    const current = targetFilter.years || [];
    const next = current.includes(yr)
      ? current.filter((y) => y !== yr)
      : [...current, yr];
    onChange({ ...targetFilter, years: next });
  };

  const toggleSection = (sec) => {
    const current = targetFilter.sections || [];
    const next = current.includes(sec)
      ? current.filter((s) => s !== sec)
      : [...current, sec];
    onChange({ ...targetFilter, sections: next });
  };

  return (
    <div className="sub-panel targeting-panel">
      <div className="d-flex justify-between align-center mb-2">
        <h4 className="sub-panel-title">Audience Targeting</h4>
      </div>

      {/* Scope radio buttons */}
      <div className="scope-selection-grid mb-3">
        <label className={`scope-card ${targetFilter.scope === 'ALL' ? 'scope-selected' : ''} ${isHod || isFaculty ? 'scope-disabled' : ''}`}>
          <input
            type="radio"
            name="targetScope"
            value="ALL"
            checked={targetFilter.scope === 'ALL'}
            disabled={isHod || isFaculty}
            onChange={() => handleScopeChange('ALL')}
          />
          <div className="scope-card-content">
            <span className="scope-card-title">All College</span>
            <span className="scope-card-desc">All active enrolled students across all departments</span>
          </div>
        </label>

        <label className={`scope-card ${targetFilter.scope === 'FILTER' ? 'scope-selected' : ''}`}>
          <input
            type="radio"
            name="targetScope"
            value="FILTER"
            checked={targetFilter.scope === 'FILTER'}
            onChange={() => handleScopeChange('FILTER')}
          />
          <div className="scope-card-content">
            <span className="scope-card-title">Specific Cohort</span>
            <span className="scope-card-desc">Filter by department branch, academic year, and section</span>
          </div>
        </label>
      </div>

      {(isHod || isFaculty) && (
        <div className="alert alert-info py-2 px-3 mb-3 text-xs">
          <strong>Notice:</strong> Your official role ({user?.role}) scopes surveys to your department: <strong>{userBranch}</strong>.
        </div>
      )}

      {/* Detailed filters when FILTER is selected */}
      {targetFilter.scope === 'FILTER' && (
        <div className="filter-selectors-grid">
          {/* Branches */}
          <div className="form-group mb-3">
            <label className="field-label">Branches / Departments</label>
            <div className="chip-group">
              {AVAILABLE_BRANCHES.map((b) => {
                const isSelected = targetFilter.branches?.includes(b);
                const isLocked = isHod && b === userBranch;
                return (
                  <button
                    key={b}
                    type="button"
                    disabled={isLocked || (isFaculty && b !== userBranch)}
                    className={`filter-chip ${isSelected ? 'chip-active' : ''}`}
                    onClick={() => toggleBranch(b)}
                  >
                    {b} {isLocked ? '🔒' : ''}
                  </button>
                );
              })}
            </div>
            <small className="field-hint">Leave empty to target all branches</small>
          </div>

          {/* Academic Years */}
          <div className="form-group mb-3">
            <label className="field-label">Academic Year</label>
            <div className="chip-group">
              {AVAILABLE_YEARS.map((yr) => {
                const isSelected = targetFilter.years?.includes(yr);
                return (
                  <button
                    key={yr}
                    type="button"
                    className={`filter-chip ${isSelected ? 'chip-active' : ''}`}
                    onClick={() => toggleYear(yr)}
                  >
                    Year {yr}
                  </button>
                );
              })}
            </div>
            <small className="field-hint">Leave empty to target all years</small>
          </div>

          {/* Sections */}
          <div className="form-group mb-2">
            <label className="field-label">Sections</label>
            <div className="chip-group">
              {AVAILABLE_SECTIONS.map((sec) => {
                const isSelected = targetFilter.sections?.includes(sec);
                return (
                  <button
                    key={sec}
                    type="button"
                    className={`filter-chip ${isSelected ? 'chip-active' : ''}`}
                    onClick={() => toggleSection(sec)}
                  >
                    Section {sec}
                  </button>
                );
              })}
            </div>
            <small className="field-hint">Leave empty to target all sections</small>
          </div>
        </div>
      )}

      {/* Live Eligible Count Badge */}
      <div className="eligible-count-indicator mt-3" aria-live="polite">
        <span className="indicator-icon">👥</span>
        {loadingCount ? (
          <span className="text-muted">Calculating student reach...</span>
        ) : countError ? (
          <span className="text-muted">{countError}</span>
        ) : (
          <span className="eligible-count-text">
            This will reach <strong className="tabular-nums">{eligibleCount !== null ? eligibleCount : '...'}</strong> students
          </span>
        )}
      </div>
    </div>
  );
}
