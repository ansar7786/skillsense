import React from 'react';

/**
 * WaitingBanner
 * Displayed when an anonymous survey has fewer than 5 responses (K=5 anonymity gate).
 * Informs officials that analytics are unavailable until enough responses are recorded.
 */
export default function WaitingBanner({ have = 0, need = 5 }) {
  const percentage = Math.min(Math.round((have / need) * 100), 100);

  return (
    <div className="card waiting-banner-card" role="region" aria-label="Privacy Waiting Banner">
      <div className="waiting-banner-icon" aria-hidden="true">🔒</div>
      <div className="waiting-banner-content">
        <h2 className="waiting-banner-title">Analytics are not available yet</h2>
        <p className="waiting-banner-desc">
          To protect student anonymity, aggregate results for this survey will only be displayed
          after at least <strong>{need} responses</strong> have been collected.
        </p>
        <div className="waiting-banner-metric">
          <span className="metric-text">
            <strong>{have}</strong> of <strong>{need}</strong> responses collected
          </span>
          <span className="metric-percent">{percentage}% towards privacy threshold</span>
        </div>
        <div className="waiting-progress-track" aria-hidden="true">
          <div
            className="waiting-progress-fill"
            style={{ width: `${percentage}%` }}
          ></div>
        </div>
      </div>
    </div>
  );
}
