import React, { useState } from 'react';
import { Bar } from 'react-chartjs-2';
import { ensureChartRegistered } from './ChartSetup';

ensureChartRegistered();

/**
 * TrendChart
 * Displays version-over-version comparison between the previous and current survey versions.
 * Displays IMPROVED, DECLINED, or FLAT direction tags, and a low-confidence warning when responses < 30.
 */
export default function TrendChart({ trendData = null, error = null }) {
  const [viewTable, setViewTable] = useState(false);

  if (error) {
    return (
      <div className="card empty-chart-card text-center py-6">
        <div className="empty-icon text-muted mb-2" aria-hidden="true">📈</div>
        <h4 className="font-medium mb-1">No Historical Comparison</h4>
        <p className="text-muted text-sm">
          {error.includes('No previous version') || error.includes('not found')
            ? 'This is version 1 of this survey template. Historical trend analysis will be available once a subsequent version is created.'
            : error}
        </p>
      </div>
    );
  }

  if (!trendData) {
    return (
      <div className="card empty-chart-card text-center py-6">
        <p className="text-muted text-sm">Loading historical trend comparison...</p>
      </div>
    );
  }

  const { overall, skills = [], questions = [] } = trendData;
  const items = skills.length > 0 ? skills : questions;

  if (!items || items.length === 0) {
    return (
      <div className="card empty-chart-card text-center py-6">
        <p className="text-muted text-sm">Insufficient data points to plot version trends.</p>
      </div>
    );
  }

  const labels = items.map((item) => (item.skill || item.key || 'Metric').replace(/_/g, ' ').toUpperCase());
  const prevScores = items.map((item) => Number(item.previous.toFixed(1)));
  const nowScores = items.map((item) => Number(item.now.toFixed(1)));

  const chartData = {
    labels,
    datasets: [
      {
        label: 'Previous Version',
        data: prevScores,
        backgroundColor: '#94a3b8', // Slate light
        borderRadius: 4,
        barThickness: 16,
      },
      {
        label: 'Current Version',
        data: nowScores,
        backgroundColor: '#4f46e5', // Indigo
        borderRadius: 4,
        barThickness: 16,
      },
    ],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
        labels: { font: { family: '-apple-system, sans-serif', size: 12 }, color: '#334155' },
      },
      tooltip: {
        callbacks: {
          afterBody: (context) => {
            const idx = context[0].dataIndex;
            const item = items[idx];
            return `Delta: ${item.delta > 0 ? `+${item.delta.toFixed(1)}` : item.delta.toFixed(1)} (${item.direction})`;
          },
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { color: '#334155', font: { weight: '600' } },
      },
      y: {
        min: 0,
        max: 100,
        grid: { color: '#f1f5f9' },
        ticks: { color: '#64748b' },
      },
    },
  };

  const hasLowConfidence = Boolean(
    overall?.low_confidence || items.some((item) => item.low_confidence)
  );

  return (
    <div className="card chart-card trend-chart-card" role="region" aria-label="Version Trend Comparison">
      <div className="chart-card-header">
        <div>
          <div className="d-flex align-center gap-2">
            <h3 className="chart-card-title">Version-over-Version Comparison</h3>
            {overall?.direction && (
              <span
                className={`direction-pill direction-${overall.direction.toLowerCase()}`}
              >
                Overall: {overall.direction} ({overall.delta > 0 ? `+${overall.delta.toFixed(1)}` : overall.delta.toFixed(1)})
              </span>
            )}
          </div>
          <p className="chart-card-subtitle text-sm text-muted">
            Tracking metric shifts across consecutive survey releases.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={() => setViewTable(!viewTable)}
          aria-label={viewTable ? 'Switch to chart' : 'Switch to table'}
        >
          {viewTable ? '📊 View Chart' : '🔢 View Table'}
        </button>
      </div>

      {hasLowConfidence && (
        <div className="alert alert-warning text-sm py-2 px-3 mb-3" role="alert">
          ⚠️ <strong>Low Confidence:</strong> One or both comparison periods have fewer than 30 responses. Interpret deltas with caution.
        </div>
      )}

      <div className="chart-card-body">
        {viewTable ? (
          <div className="table-responsive">
            <table className="survey-table">
              <thead>
                <tr>
                  <th scope="col">Metric / Skill</th>
                  <th scope="col" className="text-right">Previous</th>
                  <th scope="col" className="text-right">Current</th>
                  <th scope="col" className="text-right">Delta</th>
                  <th scope="col" className="text-center">Trend</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item, idx) => (
                  <tr key={idx}>
                    <td className="font-medium">{(item.skill || item.key).replace(/_/g, ' ')}</td>
                    <td className="text-right">{item.previous.toFixed(1)}</td>
                    <td className="text-right">{item.now.toFixed(1)}</td>
                    <td
                      className={`text-right font-medium ${
                        item.direction === 'IMPROVED'
                          ? 'text-success'
                          : item.direction === 'DECLINED'
                          ? 'text-danger'
                          : 'text-muted'
                      }`}
                    >
                      {item.delta > 0 ? `+${item.delta.toFixed(1)}` : item.delta.toFixed(1)}
                    </td>
                    <td className="text-center">
                      <span className={`direction-badge badge-${item.direction.toLowerCase()}`}>
                        {item.direction}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="chart-canvas-container" style={{ minHeight: '260px' }}>
            <Bar data={chartData} options={chartOptions} />
          </div>
        )}
      </div>
    </div>
  );
}
