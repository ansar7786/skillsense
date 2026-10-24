import React, { useState, useEffect, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import SurveyPreviewModal from '../components/SurveyPreviewModal';
import {
  fetchSurveys,
  fetchTerms,
  fetchDashboardSummary,
  closeSurvey,
  deleteSurvey,
  createNewVersion
} from '../api/surveys';

export default function OfficialsHomePage() {
  const navigate = useNavigate();

  const [surveys, setSurveys] = useState([]);
  const [terms, setTerms] = useState([]);
  const [summary, setSummary] = useState(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [termFilter, setTermFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Preview modal state
  const [previewSurvey, setPreviewSurvey] = useState(null);

  // Action state (close/delete)
  const [actionSurveyId, setActionSurveyId] = useState(null);
  const [actionType, setActionType] = useState(null); // 'CLOSE' | 'DELETE'
  const [actionLoading, setActionLoading] = useState(false);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [surveysRes, termsRes, summaryRes] = await Promise.all([
        fetchSurveys({
          status: statusFilter !== 'ALL' ? statusFilter : undefined,
          category: categoryFilter !== 'ALL' ? categoryFilter : undefined,
          termId: termFilter !== 'ALL' ? termFilter : undefined
        }),
        fetchTerms().catch(() => []),
        fetchDashboardSummary().catch(() => null)
      ]);

      setSurveys(surveysRes || []);
      setTerms(termsRes || []);
      setSummary(summaryRes);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load surveys. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [statusFilter, categoryFilter, termFilter]);

  // Client-side search filter
  const filteredSurveys = useMemo(() => {
    if (!searchQuery.trim()) return surveys;
    const q = searchQuery.toLowerCase().trim();
    return surveys.filter((s) =>
      (s.title && s.title.toLowerCase().includes(q)) ||
      (s.description && s.description.toLowerCase().includes(q))
    );
  }, [surveys, searchQuery]);

  // Aggregate stats for KPI tiles
  const stats = useMemo(() => {
    const activeCount = summary?.surveysPerStatus?.ACTIVE ??
      surveys.filter((s) => s.status === 'ACTIVE').length;

    const totalResponses = surveys.reduce((acc, s) => acc + (s.responseCount || 0), 0);

    let avgComp = '0.0%';
    if (summary?.averageCompletionRate !== undefined && summary?.averageCompletionRate !== null) {
      avgComp = `${(summary.averageCompletionRate * 100).toFixed(1)}%`;
    } else {
      const activeWithEligible = surveys.filter((s) => s.eligibleCount && s.eligibleCount > 0);
      if (activeWithEligible.length > 0) {
        const rate = activeWithEligible.reduce((acc, s) => acc + ((s.responseCount || 0) / s.eligibleCount), 0) / activeWithEligible.length;
        avgComp = `${(rate * 100).toFixed(1)}%`;
      }
    }

    return {
      activeSurveys: activeCount,
      totalResponses,
      avgCompletion: avgComp
    };
  }, [surveys, summary]);

  const handleCloseSurvey = async (id) => {
    setActionLoading(true);
    try {
      await closeSurvey(id);
      setActionSurveyId(null);
      setActionType(null);
      await loadData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to close survey');
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeleteDraft = async (id) => {
    setActionLoading(true);
    try {
      await deleteSurvey(id);
      setActionSurveyId(null);
      setActionType(null);
      await loadData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to delete draft survey');
    } finally {
      setActionLoading(false);
    }
  };

  const handleNewVersion = async (survey) => {
    try {
      const res = await createNewVersion(survey.surveyId, {
        title: `${survey.title} (v${(survey.version || 1) + 1})`
      });
      navigate(`/officials/surveys/${res.surveyId}/edit`);
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to create new version');
    }
  };

  return (
    <>
      <Navbar />
      <main className="container page-content officials-home-page">
        {/* Page Top Header */}
        <div className="d-flex justify-between align-center mb-6">
          <div>
            <h1 className="page-header-title">Surveys</h1>
            <p className="page-header-subtitle">Manage institutional feedback, assessments, and readiness surveys</p>
          </div>
          <Link to="/officials/surveys/new" className="btn btn-primary tap-target">
            + New Survey
          </Link>
        </div>

        {/* Summary KPI Tiles */}
        <div className="kpi-grid mb-6">
          <div className="kpi-card">
            <span className="kpi-label">Active Surveys</span>
            <div className="kpi-value tabular-nums">{stats.activeSurveys}</div>
            <span className="kpi-meta text-muted">Currently collecting responses</span>
          </div>

          <div className="kpi-card">
            <span className="kpi-label">Responses Collected</span>
            <div className="kpi-value tabular-nums">{stats.totalResponses}</div>
            <span className="kpi-meta text-muted">Across all surveyed cohorts</span>
          </div>

          <div className="kpi-card">
            <span className="kpi-label">Avg Completion Rate</span>
            <div className="kpi-value tabular-nums">{stats.avgCompletion}</div>
            <span className="kpi-meta text-muted">Eligible vs actual participation</span>
          </div>
        </div>

        {/* Filters and Search Bar */}
        <div className="card filters-card mb-4">
          <div className="filters-row">
            {/* Status Filter */}
            <div className="filter-group">
              <label htmlFor="filter-status" className="field-label micro-label">Status</label>
              <select
                id="filter-status"
                className="form-select form-select-sm"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="ALL">All Statuses</option>
                <option value="DRAFT">Draft</option>
                <option value="ACTIVE">Active</option>
                <option value="CLOSED">Closed</option>
              </select>
            </div>

            {/* Category Filter */}
            <div className="filter-group">
              <label htmlFor="filter-category" className="field-label micro-label">Category</label>
              <select
                id="filter-category"
                className="form-select form-select-sm"
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
              >
                <option value="ALL">All Categories</option>
                <option value="PLACEMENT">Placement</option>
                <option value="ACADEMIC">Academic</option>
                <option value="FACILITY">Facility</option>
                <option value="EVENT">Event</option>
                <option value="WELLBEING">Wellbeing</option>
                <option value="CUSTOM">Custom</option>
              </select>
            </div>

            {/* Term Filter */}
            <div className="filter-group">
              <label htmlFor="filter-term" className="field-label micro-label">Academic Term</label>
              <select
                id="filter-term"
                className="form-select form-select-sm"
                value={termFilter}
                onChange={(e) => setTermFilter(e.target.value)}
              >
                <option value="ALL">All Terms</option>
                {terms.map((t) => (
                  <option key={t.termId} value={t.termId}>
                    {t.label} {t.isCurrent ? '(Current)' : ''}
                  </option>
                ))}
              </select>
            </div>

            {/* Search Input */}
            <div className="filter-group flex-grow">
              <label htmlFor="filter-search" className="field-label micro-label">Search</label>
              <input
                id="filter-search"
                type="search"
                className="form-input form-input-sm"
                placeholder="Search surveys by title or description..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
          </div>
        </div>

        {/* Surveys Content Area */}
        {loading ? (
          <div className="card loading-container py-8" role="status">
            <div className="spinner"></div>
            <p>Loading surveys...</p>
          </div>
        ) : error ? (
          <div className="card error-container py-6 text-center">
            <p className="text-danger mb-3">{error}</p>
            <button type="button" className="btn btn-secondary btn-sm" onClick={loadData}>
              Retry Loading
            </button>
          </div>
        ) : filteredSurveys.length === 0 ? (
          <div className="card empty-state py-8">
            <div className="empty-icon" aria-hidden="true">📋</div>
            <h3 className="empty-title">No surveys found</h3>
            <p className="text-muted text-sm mt-1 mb-4">
              {searchQuery || statusFilter !== 'ALL' || categoryFilter !== 'ALL' || termFilter !== 'ALL'
                ? 'Try adjusting your filters or search keywords.'
                : 'Get started by creating your first student assessment survey.'}
            </p>
            <Link to="/officials/surveys/new" className="btn btn-primary">
              + Create First Survey
            </Link>
          </div>
        ) : (
          <div className="card table-container">
            <table className="survey-table" aria-label="Official Surveys Table">
              <thead>
                <tr>
                  <th scope="col">Survey</th>
                  <th scope="col">Category</th>
                  <th scope="col">Status</th>
                  <th scope="col">Responses</th>
                  <th scope="col">Term</th>
                  <th scope="col">Closes</th>
                  <th scope="col" className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredSurveys.map((survey) => (
                  <tr key={survey.surveyId} className="survey-row">
                    {/* Survey Title & Version & Meta */}
                    <td className="survey-col-title">
                      <div className="d-flex align-center gap-2">
                        <span className="survey-name font-medium">{survey.title}</span>
                        {survey.version && (
                          <span className="version-pill code-font">v{survey.version}</span>
                        )}
                        <span
                          className="anon-icon"
                          title={survey.isAnonymous ? 'Strictly Anonymous' : 'Identified'}
                          aria-label={survey.isAnonymous ? 'Strictly Anonymous' : 'Identified'}
                        >
                          {survey.isAnonymous ? '🔒' : '👤'}
                        </span>
                      </div>
                      {survey.description && (
                        <div className="survey-snippet text-muted text-xs truncate-text mt-1">
                          {survey.description}
                        </div>
                      )}
                    </td>

                    {/* Category Pill */}
                    <td>
                      <span className={`category-pill category-${(survey.category || 'CUSTOM').toLowerCase()}`}>
                        {survey.category}
                      </span>
                    </td>

                    {/* Status Badge */}
                    <td>
                      <span className={`status-badge status-${(survey.status || 'DRAFT').toLowerCase()}`}>
                        {survey.status}
                      </span>
                    </td>

                    {/* Responses & Eligible Count */}
                    <td className="tabular-nums">
                      <div className="responses-count font-medium">
                        {survey.responseCount || 0}
                        {survey.eligibleCount ? (
                          <span className="text-muted font-normal text-xs"> / {survey.eligibleCount}</span>
                        ) : null}
                      </div>
                      {survey.eligibleCount && survey.eligibleCount > 0 ? (
                        <div className="mini-progress-bar mt-1">
                          <div
                            className="mini-progress-fill"
                            style={{
                              width: `${Math.min(100, Math.round(((survey.responseCount || 0) / survey.eligibleCount) * 100))}%`
                            }}
                          ></div>
                        </div>
                      ) : null}
                    </td>

                    {/* Term */}
                    <td className="text-sm">
                      {survey.termLabel || '—'}
                    </td>

                    {/* Closing Date */}
                    <td className="text-sm text-muted">
                      {survey.closesAt
                        ? new Date(survey.closesAt).toLocaleDateString(undefined, {
                            month: 'short',
                            day: 'numeric',
                            year: 'numeric'
                          })
                        : 'No close date'}
                    </td>

                    {/* Actions */}
                    <td className="text-right">
                      <div className="d-flex justify-end gap-2">
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          onClick={() => setPreviewSurvey(survey)}
                          title="Preview student view"
                        >
                          Preview
                        </button>

                        {survey.status === 'DRAFT' && (
                          <>
                            <Link
                              to={`/officials/surveys/${survey.surveyId}/edit`}
                              className="btn btn-primary btn-sm"
                            >
                              Edit
                            </Link>
                            <button
                              type="button"
                              className="btn btn-danger-outline btn-sm"
                              onClick={() => {
                                setActionSurveyId(survey.surveyId);
                                setActionType('DELETE');
                              }}
                              title="Delete Draft"
                            >
                              Delete
                            </button>
                          </>
                        )}

                        {survey.status !== 'DRAFT' && (
                          <Link
                            to={`/officials/surveys/${survey.surveyId}/analytics`}
                            className="btn btn-primary btn-sm"
                            title="View survey analytics dashboard"
                          >
                            Analytics
                          </Link>
                        )}

                        {survey.status === 'ACTIVE' && (
                          <button
                            type="button"
                            className="btn btn-warning btn-sm"
                            onClick={() => {
                              setActionSurveyId(survey.surveyId);
                              setActionType('CLOSE');
                            }}
                            title="Close active survey"
                          >
                            Close
                          </button>
                        )}

                        {survey.status === 'CLOSED' && (
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => handleNewVersion(survey)}
                            title="Create new version"
                          >
                            New Version
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>

      {/* Student Safe Preview Modal */}
      {previewSurvey && (
        <SurveyPreviewModal
          survey={previewSurvey}
          isOpen={Boolean(previewSurvey)}
          onClose={() => setPreviewSurvey(null)}
        />
      )}

      {/* Action Confirmation Modal (Close / Delete) */}
      {actionSurveyId && actionType && (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal-container">
            <div className="modal-header">
              <h3 className="modal-title">
                {actionType === 'CLOSE' ? 'Close Active Survey?' : 'Delete Draft Survey?'}
              </h3>
            </div>
            <div className="modal-body">
              {actionType === 'CLOSE' ? (
                <p>
                  Are you sure you want to close this survey? Students will no longer be able to submit responses.
                </p>
              ) : (
                <p>
                  Are you sure you want to permanently delete this draft survey? This action cannot be undone.
                </p>
              )}
            </div>
            <div className="modal-footer">
              <button
                type="button"
                className="btn btn-secondary"
                disabled={actionLoading}
                onClick={() => {
                  setActionSurveyId(null);
                  setActionType(null);
                }}
              >
                Cancel
              </button>
              <button
                type="button"
                className={actionType === 'CLOSE' ? 'btn btn-warning' : 'btn btn-danger'}
                disabled={actionLoading}
                onClick={() => {
                  if (actionType === 'CLOSE') {
                    handleCloseSurvey(actionSurveyId);
                  } else {
                    handleDeleteDraft(actionSurveyId);
                  }
                }}
              >
                {actionLoading ? 'Processing...' : actionType === 'CLOSE' ? 'Close Survey' : 'Delete Draft'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
