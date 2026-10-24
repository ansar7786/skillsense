import React from 'react';

/**
 * InsightPanel
 * Displays synthesized recommendations and executive findings from the survey insights API.
 * Clearly differentiates between AI-synthesized outputs (Gemini) and deterministic RULES fallback.
 */
export default function InsightPanel({
  insights = null,
  loading = false,
  onRefresh = () => {},
  refreshing = false,
}) {
  if (loading && !insights) {
    return (
      <div className="card insight-card loading-skeleton py-8 text-center" role="status">
        <div className="spinner"></div>
        <p className="mt-2 text-muted">Analyzing survey feedback & synthesizing recommendations...</p>
      </div>
    );
  }

  if (!insights) {
    return (
      <div className="card insight-card empty-state py-6 text-center">
        <p className="text-muted">No insight summary is currently available for this survey.</p>
        <button
          type="button"
          className="btn btn-primary btn-sm mt-3"
          onClick={onRefresh}
          disabled={refreshing}
        >
          {refreshing ? 'Generating...' : 'Generate Insights'}
        </button>
      </div>
    );
  }

  const { headline, key_findings = [], recommendations = [], watch_outs = [], source, generatedAt } = insights;
  const isAi = source === 'AI';

  return (
    <div className="card insight-panel-card" role="region" aria-label="Survey Insights and Recommendations">
      {/* Header with Source Badge & Refresh Control */}
      <div className="insight-header d-flex justify-between align-center mb-4 flex-wrap gap-2">
        <div className="d-flex align-center gap-2">
          <span className={`source-pill ${isAi ? 'source-ai' : 'source-rules'}`}>
            {isAi ? '✨ AI Generated (Gemini)' : '⚙️ Generated without AI (Rules-based)'}
          </span>
          {generatedAt && (
            <span className="text-xs text-muted">
              Generated: {new Date(generatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </span>
          )}
        </div>
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={onRefresh}
          disabled={refreshing}
          title="Regenerate insights respecting backend rate limits"
        >
          {refreshing ? 'Refreshing...' : '🔄 Refresh Insights'}
        </button>
      </div>

      {/* Executive Headline */}
      {headline && (
        <div className="insight-headline-box mb-4">
          <h3 className="insight-headline">{headline}</h3>
        </div>
      )}

      {/* Key Findings */}
      {key_findings && key_findings.length > 0 && (
        <div className="insight-section mb-5">
          <h4 className="section-heading text-sm text-uppercase text-muted font-bold mb-2">
            Key Findings
          </h4>
          <ul className="findings-list">
            {key_findings.map((finding, idx) => (
              <li key={idx} className="finding-item">
                <span className="bullet-dot" aria-hidden="true">•</span>
                <span>{finding}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Actionable Recommendations */}
      {recommendations && recommendations.length > 0 && (
        <div className="insight-section mb-5">
          <h4 className="section-heading text-sm text-uppercase text-muted font-bold mb-3">
            Prioritized Recommendations
          </h4>
          <div className="recommendations-grid">
            {recommendations.map((rec, idx) => (
              <div key={idx} className="recommendation-card">
                <div className="rec-header d-flex justify-between align-center mb-2">
                  <span className={`rec-priority-badge priority-p${rec.priority || idx + 1}`}>
                    P{rec.priority || idx + 1}
                  </span>
                  {rec.suggested_owner && (
                    <span className="rec-owner-pill">
                      👤 {rec.suggested_owner}
                    </span>
                  )}
                </div>
                <h5 className="rec-title font-bold mb-1">{rec.title}</h5>
                <p className="rec-action text-sm mb-2">{rec.action}</p>
                {rec.rationale && (
                  <p className="rec-rationale text-xs text-muted">
                    <strong>Rationale:</strong> {rec.rationale}
                  </p>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Watch-outs Alert */}
      {watch_outs && watch_outs.length > 0 && (
        <div className="insight-section watchouts-section">
          <h4 className="section-heading text-sm text-uppercase text-danger font-bold mb-2">
            ⚠️ Watch-Outs & Risks
          </h4>
          <div className="watchouts-box">
            <ul className="watchouts-list">
              {watch_outs.map((item, idx) => (
                <li key={idx} className="watchout-item text-sm">
                  {item}
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </div>
  );
}
