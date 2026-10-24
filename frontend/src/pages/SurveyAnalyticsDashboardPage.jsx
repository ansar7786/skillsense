import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import WaitingBanner from '../components/WaitingBanner';
import ChartCard from '../components/ChartCard';
import SkillGapChart from '../components/SkillGapChart';
import BandDonut from '../components/BandDonut';
import TrendChart from '../components/TrendChart';
import InsightPanel from '../components/InsightPanel';
import {
  fetchSurvey,
  fetchSurveyAnalytics,
  fetchSurveyTrend,
  fetchSurveyInsights,
  fetchSurveyTexts,
  closeSurvey,
  downloadSurveyExport,
  triggerFileDownload,
} from '../api/surveys';
import { Bar, Doughnut } from 'react-chartjs-2';
import { ensureChartRegistered, CHART_COLORS } from '../components/ChartSetup';

ensureChartRegistered();

const TABS = [
  { id: 'overview', label: 'Overview' },
  { id: 'results', label: 'Results' },
  { id: 'skills', label: 'Skills' },
  { id: 'breakdown', label: 'Breakdown' },
  { id: 'trend', label: 'Trend' },
  { id: 'insights', label: 'Insights' },
  { id: 'comments', label: 'Comments' },
];

export default function SurveyAnalyticsDashboardPage() {
  const { id } = useParams();

  const [activeTab, setActiveTab] = useState('overview');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [errorCode, setErrorCode] = useState(null);

  const [survey, setSurvey] = useState(null);
  const [analytics, setAnalytics] = useState(null);
  const [trendData, setTrendData] = useState(null);
  const [trendError, setTrendError] = useState(null);
  const [insights, setInsights] = useState(null);
  const [refreshingInsights, setRefreshingInsights] = useState(false);

  // Free-text responses map (questionKey -> array of text answers)
  const [textResponses, setTextResponses] = useState({});
  const [loadingTexts, setLoadingTexts] = useState(false);

  // Close survey action state
  const [closing, setClosing] = useState(false);
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);

  // Report export states
  const [exportingPdf, setExportingPdf] = useState(false);
  const [exportingXlsx, setExportingXlsx] = useState(false);
  const [exportError, setExportError] = useState(null);

  const handleExport = async (format) => {
    if (exportingPdf || exportingXlsx) return;
    setExportError(null);
    if (format === 'pdf') {
      setExportingPdf(true);
    } else {
      setExportingXlsx(true);
    }

    try {
      const blob = await downloadSurveyExport(id, format);
      const filename = `survey_${id}_report.${format}`;
      triggerFileDownload(blob, filename);
    } catch (err) {
      console.error(`Export ${format.toUpperCase()} failed:`, err);
      const msg = err.response?.data?.message || `Failed to export ${format.toUpperCase()} report. Please try again.`;
      setExportError(msg);
    } finally {
      if (format === 'pdf') {
        setExportingPdf(false);
      } else {
        setExportingXlsx(false);
      }
    }
  };

  // Load Survey and Analytics data
  const loadDashboardData = useCallback(async () => {
    setLoading(true);
    setError(null);
    setErrorCode(null);

    try {
      // 1. Fetch survey metadata
      const surveyRes = await fetchSurvey(id);
      setSurvey(surveyRes);

      // 2. Fetch survey analytics
      const analyticsRes = await fetchSurveyAnalytics(id);
      setAnalytics(analyticsRes);

      // 3. Fetch trend data in background (may 404 if version 1, which is valid)
      try {
        const trendRes = await fetchSurveyTrend(id);
        setTrendData(trendRes);
        setTrendError(null);
      } catch (trendErr) {
        const msg = trendErr.response?.data?.message || 'No historical comparison available.';
        setTrendError(msg);
        setTrendData(null);
      }

      // 4. If analytics available, fetch initial insights
      if (analyticsRes.available) {
        try {
          const insightRes = await fetchSurveyInsights(id, false);
          setInsights(insightRes);
        } catch (insightErr) {
          console.warn('Failed to load initial insights:', insightErr);
        }
      }
    } catch (err) {
      const status = err.response?.status;
      const code = err.response?.data?.code;
      const msg = err.response?.data?.message;

      setErrorCode(status || code);
      if (status === 403) {
        setError('Access Denied: You do not have permission to view analytics for this survey.');
      } else if (status === 404) {
        setError('Survey not found.');
      } else if (status === 503 || code === 'ANALYTICS_UNAVAILABLE') {
        setError('Analytics are temporarily unavailable. Please try again later.');
      } else {
        setError(msg || 'An error occurred while loading survey analytics.');
      }
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadDashboardData();
  }, [loadDashboardData]);

  // Load free-text comments when comments tab is active
  useEffect(() => {
    if (activeTab === 'comments' && analytics?.available && survey?.questionSchema?.questions) {
      const textQuestions = survey.questionSchema.questions.filter((q) => q.type === 'TEXT');
      if (textQuestions.length > 0) {
        setLoadingTexts(true);
        Promise.all(
          textQuestions.map(async (q) => {
            try {
              const texts = await fetchSurveyTexts(id, q.key);
              return { key: q.key, texts };
            } catch (e) {
              return { key: q.key, texts: [] };
            }
          })
        )
          .then((results) => {
            const map = {};
            results.forEach((r) => {
              map[r.key] = r.texts;
            });
            setTextResponses(map);
          })
          .finally(() => setLoadingTexts(false));
      }
    }
  }, [activeTab, analytics?.available, id, survey]);

  // Handle insight regeneration
  const handleRefreshInsights = async () => {
    setRefreshingInsights(true);
    try {
      const fresh = await fetchSurveyInsights(id, true);
      setInsights(fresh);
    } catch (err) {
      console.error('Failed to regenerate insights:', err);
    } finally {
      setRefreshingInsights(false);
    }
  };

  // Handle closing survey
  const handleCloseSurvey = async () => {
    setClosing(true);
    try {
      await closeSurvey(id);
      setShowCloseConfirm(false);
      await loadDashboardData();
    } catch (err) {
      console.error('Failed to close survey:', err);
    } finally {
      setClosing(false);
    }
  };

  const isAnonymous = Boolean(survey?.isAnonymous);
  const isAvailable = Boolean(analytics?.available);
  const analyticsData = analytics?.data || {};

  const responseCount = analytics?.have ?? analyticsData.response_count ?? 0;
  const eligibleCount = survey?.eligibleCount || 0;
  const completionRate =
    eligibleCount > 0
      ? Math.round((responseCount / eligibleCount) * 100)
      : Math.round((analyticsData.completion_rate || 0) * 100);

  const satisfactionIndex = analyticsData.index !== null && analyticsData.index !== undefined
    ? Number(analyticsData.index).toFixed(1)
    : '—';

  // Questions schema lookup
  const questionsMap = useMemo(() => {
    const map = {};
    if (survey?.questionSchema?.questions) {
      survey.questionSchema.questions.forEach((q) => {
        map[q.key] = q;
      });
    }
    return map;
  }, [survey]);

  // Check if survey has scored skills
  const hasScoredSkills = Boolean(analyticsData.skills && analyticsData.skills.length > 0);

  return (
    <>
      <Navbar />
      <main className="container page-content official-dashboard-container">
        {/* Navigation Breadcrumb */}
        <div className="breadcrumb-nav mb-3">
          <Link to="/officials" className="back-link">
            ← Back to Surveys
          </Link>
        </div>

        {/* Loading State */}
        {loading && (
          <div className="card loading-skeleton py-8 text-center" role="status">
            <div className="spinner"></div>
            <p className="mt-3 text-muted">Loading survey analytics dashboard...</p>
          </div>
        )}

        {/* Error State */}
        {!loading && error && (
          <div className="card error-card py-6 text-center" role="alert">
            <div className="error-icon mb-2" aria-hidden="true">⚠️</div>
            <h3 className="error-title mb-2">Unable to load analytics</h3>
            <p className="text-muted mb-4">{error}</p>
            {errorCode !== 403 && errorCode !== 404 && (
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={loadDashboardData}
              >
                Retry Loading
              </button>
            )}
            <Link to="/officials" className="btn btn-primary btn-sm ml-2">
              Return to Survey List
            </Link>
          </div>
        )}

        {/* Main Dashboard Content */}
        {!loading && !error && survey && (
          <>
            {/* Header Section */}
            <div className="card dashboard-header-card mb-4">
              <div className="d-flex justify-between align-start flex-wrap gap-3">
                <div className="dashboard-title-area">
                  <div className="meta-badges-row mb-2">
                    <span className={`category-pill category-${survey.category?.toLowerCase()}`}>
                      {survey.category}
                    </span>
                    <span className={`status-badge status-${survey.status?.toLowerCase()}`}>
                      {survey.status}
                    </span>
                    <span className="anon-badge">
                      {isAnonymous ? '🔒 Anonymous (K=5)' : '👤 Non-Anonymous'}
                    </span>
                    {survey.version && (
                      <span className="version-pill code-font">v{survey.version}</span>
                    )}
                    {survey.termLabel && (
                      <span className="term-pill text-xs text-muted">
                        📅 {survey.termLabel}
                      </span>
                    )}
                  </div>
                  <h1 className="dashboard-survey-title">{survey.title}</h1>
                  {survey.description && (
                    <p className="dashboard-survey-desc text-muted text-sm mt-1">
                      {survey.description}
                    </p>
                  )}
                </div>

                <div className="dashboard-header-actions d-flex align-center gap-2 flex-wrap">
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm export-btn"
                    disabled={exportingPdf || exportingXlsx}
                    onClick={() => handleExport('pdf')}
                    title="Export survey analytics report as PDF"
                  >
                    {exportingPdf ? (
                      <>
                        <span className="spinner-border spinner-border-sm mr-1" aria-hidden="true" />
                        Exporting PDF...
                      </>
                    ) : (
                      '📄 Export PDF'
                    )}
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm export-btn"
                    disabled={exportingPdf || exportingXlsx}
                    onClick={() => handleExport('xlsx')}
                    title="Export survey analytics report as Excel (.xlsx)"
                  >
                    {exportingXlsx ? (
                      <>
                        <span className="spinner-border spinner-border-sm mr-1" aria-hidden="true" />
                        Exporting Excel...
                      </>
                    ) : (
                      '📊 Export Excel'
                    )}
                  </button>
                  {survey.status === 'ACTIVE' && (
                    <button
                      type="button"
                      className="btn btn-warning btn-sm"
                      onClick={() => setShowCloseConfirm(true)}
                    >
                      Close Survey
                    </button>
                  )}
                </div>
              </div>

              {exportError && (
                <div className="alert alert-danger mt-3 d-flex justify-between align-center" role="alert">
                  <span>⚠️ {exportError}</span>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline text-xs"
                    onClick={() => setExportError(null)}
                  >
                    Dismiss
                  </button>
                </div>
              )}

              {/* Progress Summary Line */}
              <div className="dashboard-kpi-summary mt-4 pt-3 border-top">
                <div className="kpi-mini-grid">
                  <div className="kpi-mini-item">
                    <span className="kpi-label">Responses</span>
                    <span className="kpi-value font-mono">{responseCount}</span>
                  </div>
                  <div className="kpi-mini-item">
                    <span className="kpi-label">Eligible Students</span>
                    <span className="kpi-value font-mono">{eligibleCount || '—'}</span>
                  </div>
                  <div className="kpi-mini-item">
                    <span className="kpi-label">Completion Rate</span>
                    <span className="kpi-value font-mono">{completionRate}%</span>
                  </div>
                  <div className="kpi-mini-item">
                    <span className="kpi-label">Satisfaction Index</span>
                    <span className="kpi-value font-mono">{satisfactionIndex}</span>
                  </div>
                  <div className="kpi-mini-item">
                    <span className="kpi-label">Deadline</span>
                    <span className="kpi-value text-sm">
                      {survey.closesAt
                        ? new Date(survey.closesAt).toLocaleDateString(undefined, {
                            month: 'short',
                            day: 'numeric',
                            year: 'numeric',
                          })
                        : 'No deadline'}
                    </span>
                  </div>
                </div>

                {/* Response Progress Track */}
                <div className="response-progress-track mt-3" aria-hidden="true">
                  <div
                    className="response-progress-fill"
                    style={{ width: `${Math.min(completionRate, 100)}%` }}
                  ></div>
                </div>
              </div>
            </div>

            {/* Privacy Gate: When available is false, show WaitingBanner */}
            {!isAvailable ? (
              <WaitingBanner have={analytics?.have || 0} need={analytics?.need || 5} />
            ) : (
              <>
                {/* Dashboard Tabs Bar */}
                <div className="dashboard-tabs-bar mb-4" role="tablist" aria-label="Dashboard views">
                  {TABS.map((tab) => {
                    // Hide skills tab if not scored
                    if (tab.id === 'skills' && !hasScoredSkills) return null;

                    return (
                      <button
                        key={tab.id}
                        type="button"
                        role="tab"
                        aria-selected={activeTab === tab.id}
                        className={`dashboard-tab-btn ${activeTab === tab.id ? 'active' : ''}`}
                        onClick={() => setActiveTab(tab.id)}
                      >
                        {tab.label}
                      </button>
                    );
                  })}
                </div>

                {/* TAB 1: OVERVIEW */}
                {activeTab === 'overview' && (
                  <div className="tab-pane overview-pane" role="tabpanel">
                    <div className="dashboard-cards-grid">
                      {/* Metric Card 1 */}
                      <div className="card metric-tile">
                        <span className="metric-tile-label">Responses Collected</span>
                        <div className="metric-tile-num font-mono">{responseCount}</div>
                        <span className="metric-tile-sub">
                          {eligibleCount > 0 ? `Out of ${eligibleCount} eligible students` : 'All college target'}
                        </span>
                      </div>

                      {/* Metric Card 2 */}
                      <div className="card metric-tile">
                        <span className="metric-tile-label">Completion Rate</span>
                        <div className="metric-tile-num font-mono">{completionRate}%</div>
                        <span className="metric-tile-sub">
                          {completionRate >= 70 ? 'High cohort participation' : 'Active collection'}
                        </span>
                      </div>

                      {/* Metric Card 3 */}
                      <div className="card metric-tile">
                        <span className="metric-tile-label">Cohort Satisfaction Index</span>
                        <div className="metric-tile-num font-mono">{satisfactionIndex}</div>
                        <span className="metric-tile-sub">0–100 normalized rating mean</span>
                      </div>
                    </div>

                    {/* Readiness Band Distribution (if scored) */}
                    {hasScoredSkills && analyticsData.bands && (
                      <div className="overview-scored-section mb-4">
                        <BandDonut bands={analyticsData.bands} totalResponses={responseCount} />
                      </div>
                    )}

                    {/* Skill Gap Summary Preview (if scored) */}
                    {hasScoredSkills && analyticsData.skills && (
                      <div className="overview-skills-preview mb-4">
                        <SkillGapChart skills={analyticsData.skills} />
                      </div>
                    )}

                    {/* Executive Insights Preview */}
                    {insights && (
                      <div className="overview-insights-preview">
                        <InsightPanel
                          insights={insights}
                          loading={refreshingInsights}
                          onRefresh={handleRefreshInsights}
                          refreshing={refreshingInsights}
                        />
                      </div>
                    )}
                  </div>
                )}

                {/* TAB 2: RESULTS (PER QUESTION) */}
                {activeTab === 'results' && (
                  <div className="tab-pane results-pane" role="tabpanel">
                    {(!analyticsData.questions || analyticsData.questions.length === 0) ? (
                      <div className="card empty-state py-6 text-center">
                        <p className="text-muted">No question breakdown results available.</p>
                      </div>
                    ) : (
                      analyticsData.questions.map((qStat) => {
                        const qSchema = questionsMap[qStat.key] || {};
                        const qType = qStat.type;

                        // RATING questions
                        if (qType === 'RATING') {
                          const dist = qStat.distribution || {};
                          const labels = Object.keys(dist).sort((a, b) => Number(a) - Number(b));
                          const counts = labels.map((k) => dist[k] || 0);

                          const barData = {
                            labels,
                            datasets: [
                              {
                                label: 'Responses',
                                data: counts,
                                backgroundColor: '#4f46e5',
                                borderRadius: 4,
                              },
                            ],
                          };

                          const barOpts = {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: { legend: { display: false } },
                            scales: {
                              y: { beginAtZero: true, ticks: { precision: 0 } },
                            },
                          };

                          const tableData = labels.map((k) => ({
                            label: `Rating ${k}`,
                            count: dist[k] || 0,
                            percentage: qStat.n > 0 ? (dist[k] || 0) / qStat.n : 0,
                          }));

                          return (
                            <ChartCard
                              key={qStat.key}
                              prompt={qSchema.prompt || qStat.key}
                              type="Rating"
                              n={qStat.n}
                              mean={qStat.mean}
                              median={qStat.median}
                              tableData={tableData}
                            >
                              <div style={{ height: '200px' }}>
                                <Bar data={barData} options={barOpts} />
                              </div>
                            </ChartCard>
                          );
                        }

                        // SINGLE_CHOICE / MULTI_CHOICE questions
                        if (qType === 'SINGLE_CHOICE' || qType === 'MULTI_CHOICE') {
                          const dist = qStat.distribution || {};
                          const labels = Object.keys(dist);
                          const counts = labels.map((k) => dist[k] || 0);

                          const barData = {
                            labels,
                            datasets: [
                              {
                                label: 'Selections',
                                data: counts,
                                backgroundColor: CHART_COLORS.slice(0, labels.length),
                                borderRadius: 4,
                              },
                            ],
                          };

                          const barOpts = {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: { legend: { display: false } },
                            scales: {
                              y: { beginAtZero: true, ticks: { precision: 0 } },
                            },
                          };

                          const tableData = labels.map((k) => ({
                            label: k,
                            count: dist[k] || 0,
                            percentage: qStat.n > 0 ? (dist[k] || 0) / qStat.n : 0,
                          }));

                          return (
                            <ChartCard
                              key={qStat.key}
                              prompt={qSchema.prompt || qStat.key}
                              type={qType === 'SINGLE_CHOICE' ? 'Single Choice' : 'Multi Choice'}
                              n={qStat.n}
                              tableData={tableData}
                            >
                              <div style={{ height: '220px' }}>
                                <Bar data={barData} options={barOpts} />
                              </div>
                            </ChartCard>
                          );
                        }

                        // YES_NO questions
                        if (qType === 'YES_NO') {
                          const shareYes = qStat.share_yes ?? 0;
                          const yesCount = Math.round(shareYes * qStat.n);
                          const noCount = qStat.n - yesCount;

                          const donutData = {
                            labels: ['Yes', 'No'],
                            datasets: [
                              {
                                data: [yesCount, noCount],
                                backgroundColor: ['#16a34a', '#dc2626'],
                              },
                            ],
                          };

                          const tableData = [
                            { label: 'Yes', count: yesCount, percentage: shareYes },
                            { label: 'No', count: noCount, percentage: 1 - shareYes },
                          ];

                          return (
                            <ChartCard
                              key={qStat.key}
                              prompt={qSchema.prompt || qStat.key}
                              type="Yes / No"
                              n={qStat.n}
                              tableData={tableData}
                            >
                              <div style={{ height: '200px' }}>
                                <Doughnut
                                  data={donutData}
                                  options={{ responsive: true, maintainAspectRatio: false }}
                                />
                              </div>
                            </ChartCard>
                          );
                        }

                        // NUMBER questions
                        if (qType === 'NUMBER') {
                          const hist = qStat.histogram || {};
                          const labels = Object.keys(hist);
                          const counts = labels.map((k) => hist[k] || 0);

                          const barData = {
                            labels,
                            datasets: [
                              {
                                label: 'Frequency',
                                data: counts,
                                backgroundColor: '#0284c7',
                                borderRadius: 4,
                              },
                            ],
                          };

                          const tableData = labels.map((k) => ({
                            label: k,
                            count: hist[k] || 0,
                            percentage: qStat.n > 0 ? (hist[k] || 0) / qStat.n : 0,
                          }));

                          return (
                            <ChartCard
                              key={qStat.key}
                              prompt={qSchema.prompt || qStat.key}
                              type="Number Histogram"
                              n={qStat.n}
                              mean={qStat.mean}
                              median={qStat.median}
                              tableData={tableData}
                            >
                              <div style={{ height: '200px' }}>
                                <Bar data={barData} options={{ responsive: true, maintainAspectRatio: false }} />
                              </div>
                            </ChartCard>
                          );
                        }

                        // TEXT questions
                        if (qType === 'TEXT') {
                          const themes = analyticsData.text_themes?.[qStat.key] || [];
                          return (
                            <div key={qStat.key} className="card chart-card text-themes-card">
                              <div className="chart-card-header">
                                <span className="chart-type-pill">Text Feedback</span>
                                <h3 className="chart-card-title">{qSchema.prompt || qStat.key}</h3>
                              </div>
                              <p className="text-sm text-muted mb-3">n = {qStat.n} responses recorded</p>
                              {themes.length === 0 ? (
                                <p className="text-sm text-muted">No recurring word themes detected.</p>
                              ) : (
                                <div className="text-themes-tags d-flex flex-wrap gap-2">
                                  {themes.map((t, idx) => (
                                    <span key={idx} className="theme-tag">
                                      {t.term} <strong className="font-mono">({t.count})</strong>
                                    </span>
                                  ))}
                                </div>
                              )}
                            </div>
                          );
                        }

                        return null;
                      })
                    )}
                  </div>
                )}

                {/* TAB 3: SKILLS (SCORED SURVEYS ONLY) */}
                {activeTab === 'skills' && (
                  <div className="tab-pane skills-pane" role="tabpanel">
                    <SkillGapChart skills={analyticsData.skills || []} />
                    <div className="mt-4">
                      <BandDonut bands={analyticsData.bands || {}} totalResponses={responseCount} />
                    </div>
                  </div>
                )}

                {/* TAB 4: BREAKDOWN (BRANCH & YEAR) */}
                {activeTab === 'breakdown' && (
                  <div className="tab-pane breakdown-pane" role="tabpanel">
                    {/* Branch Breakdown */}
                    <div className="card chart-card mb-4">
                      <h3 className="chart-card-title mb-1">Branch Comparison</h3>
                      <p className="text-sm text-muted mb-4">Average index score across participating branches</p>

                      <div className="table-responsive">
                        <table className="survey-table">
                          <thead>
                            <tr>
                              <th scope="col">Branch</th>
                              <th scope="col" className="text-right">Responses (n)</th>
                              <th scope="col" className="text-right">Cohort Index</th>
                              <th scope="col">Status</th>
                            </tr>
                          </thead>
                          <tbody>
                            {analyticsData.groups?.branch &&
                              Object.entries(analyticsData.groups.branch).map(([branchName, item]) => (
                                <tr key={branchName}>
                                  <td className="font-medium">{branchName}</td>
                                  <td className="text-right font-mono">{item.n}</td>
                                  <td className="text-right font-mono">
                                    {item.index !== null ? item.index.toFixed(1) : '—'}
                                  </td>
                                  <td>
                                    <span className="status-badge status-active">Active</span>
                                  </td>
                                </tr>
                              ))}

                            {/* Suppressed Groups (Section 5 K=5 privacy rule) */}
                            {analyticsData.groups?.suppressed &&
                              analyticsData.groups.suppressed.map((sup, idx) => (
                                <tr key={idx} className="suppressed-row text-muted">
                                  <td className="font-medium">{sup}</td>
                                  <td className="text-right font-mono">&lt; 5</td>
                                  <td className="text-right">—</td>
                                  <td>
                                    <span className="privacy-pill">Hidden: fewer than 5 responses</span>
                                  </td>
                                </tr>
                              ))}
                          </tbody>
                        </table>
                      </div>
                    </div>

                    {/* Year Breakdown */}
                    <div className="card chart-card">
                      <h3 className="chart-card-title mb-1">Academic Year Comparison</h3>
                      <p className="text-sm text-muted mb-4">Readiness by student class level</p>

                      <div className="table-responsive">
                        <table className="survey-table">
                          <thead>
                            <tr>
                              <th scope="col">Year</th>
                              <th scope="col" className="text-right">Responses (n)</th>
                              <th scope="col" className="text-right">Cohort Index</th>
                            </tr>
                          </thead>
                          <tbody>
                            {analyticsData.groups?.year &&
                              Object.entries(analyticsData.groups.year).map(([yearName, item]) => (
                                <tr key={yearName}>
                                  <td className="font-medium">Year {yearName}</td>
                                  <td className="text-right font-mono">{item.n}</td>
                                  <td className="text-right font-mono">
                                    {item.index !== null ? item.index.toFixed(1) : '—'}
                                  </td>
                                </tr>
                              ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  </div>
                )}

                {/* TAB 5: TREND (VERSION OVER VERSION) */}
                {activeTab === 'trend' && (
                  <div className="tab-pane trend-pane" role="tabpanel">
                    <TrendChart trendData={trendData} error={trendError} />
                  </div>
                )}

                {/* TAB 6: INSIGHTS (AI / RULES) */}
                {activeTab === 'insights' && (
                  <div className="tab-pane insights-pane" role="tabpanel">
                    <InsightPanel
                      insights={insights}
                      loading={refreshingInsights}
                      onRefresh={handleRefreshInsights}
                      refreshing={refreshingInsights}
                    />
                  </div>
                )}

                {/* TAB 7: COMMENTS (TEXT QUESTIONS & THEMES) */}
                {activeTab === 'comments' && (
                  <div className="tab-pane comments-pane" role="tabpanel">
                    <div className="card comments-card">
                      <div className="comments-header mb-4">
                        <h3 className="chart-card-title">Qualitative Feedback</h3>
                        <p className="text-sm text-muted">
                          Aggregated word-frequency themes and submitted text responses.
                        </p>
                      </div>

                      {/* Top Word Themes */}
                      {analyticsData.text_themes && Object.keys(analyticsData.text_themes).length > 0 && (
                        <div className="themes-overview mb-5">
                          <h4 className="text-sm text-uppercase text-muted font-bold mb-3">
                            Recurring Themes & Keywords
                          </h4>
                          {Object.entries(analyticsData.text_themes).map(([key, termList]) => {
                            const qPrompt = questionsMap[key]?.prompt || key;
                            return (
                              <div key={key} className="theme-group mb-3">
                                <span className="theme-group-title text-sm font-medium d-block mb-1">
                                  {qPrompt}:
                                </span>
                                <div className="d-flex flex-wrap gap-2">
                                  {termList.map((t, idx) => (
                                    <span key={idx} className="theme-tag">
                                      {t.term} <strong className="font-mono">({t.count})</strong>
                                    </span>
                                  ))}
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      )}

                      {/* Text Answers Feed */}
                      <div className="text-responses-feed">
                        <h4 className="text-sm text-uppercase text-muted font-bold mb-3">
                          Submitted Responses
                        </h4>

                        {loadingTexts ? (
                          <div className="loading-state py-4 text-center">
                            <div className="spinner"></div>
                            <p className="text-sm text-muted mt-2">Loading submitted text feedback...</p>
                          </div>
                        ) : Object.keys(textResponses).length === 0 ? (
                          <p className="text-sm text-muted">No text responses collected for this survey.</p>
                        ) : (
                          Object.entries(textResponses).map(([key, list]) => {
                            const qPrompt = questionsMap[key]?.prompt || key;
                            return (
                              <div key={key} className="text-question-block mb-4">
                                <h5 className="font-medium text-sm mb-2">{qPrompt}</h5>
                                {list.length === 0 ? (
                                  <p className="text-xs text-muted">No answers provided.</p>
                                ) : (
                                  <div className="text-quotes-list d-flex flex-column gap-2">
                                    {list.map((ans, idx) => (
                                      <div key={idx} className="quote-card">
                                        <p className="quote-text text-sm">“{ans}”</p>
                                      </div>
                                    ))}
                                  </div>
                                )}
                              </div>
                            );
                          })
                        )}
                      </div>
                    </div>
                  </div>
                )}
              </>
            )}
          </>
        )}

        {/* Close Survey Confirmation Modal */}
        {showCloseConfirm && (
          <div className="modal-backdrop" role="dialog" aria-modal="true">
            <div className="modal-card card">
              <h3 className="modal-title mb-2">Close Survey?</h3>
              <p className="text-sm text-muted mb-4">
                Closing this survey will stop accepting student responses immediately. You will
                still be able to view and export all analytics and results.
              </p>
              <div className="d-flex justify-end gap-2">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setShowCloseConfirm(false)}
                  disabled={closing}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  className="btn btn-warning btn-sm"
                  onClick={handleCloseSurvey}
                  disabled={closing}
                >
                  {closing ? 'Closing...' : 'Confirm Close'}
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </>
  );
}
