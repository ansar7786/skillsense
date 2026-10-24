import React, { useState } from 'react';

/**
 * ChartCard
 * Wraps a Chart.js visualization with question prompt, type tag, response count (n),
 * optional statistics (mean, median), loading/empty state, and an accessible "View as Table" toggle.
 */
export default function ChartCard({
  title,
  prompt,
  type,
  n,
  mean,
  median,
  tableData = [],
  children,
}) {
  const [viewAsTable, setViewAsTable] = useState(false);

  return (
    <div className="card chart-card" role="region" aria-label={title || prompt}>
      <div className="chart-card-header">
        <div className="chart-card-titles">
          {type && <span className="chart-type-pill">{type}</span>}
          <h3 className="chart-card-title">{prompt || title}</h3>
        </div>
        <div className="chart-card-actions">
          {tableData.length > 0 && (
            <button
              type="button"
              className="btn btn-secondary btn-sm toggle-table-btn"
              onClick={() => setViewAsTable(!viewAsTable)}
              aria-label={viewAsTable ? 'View as chart' : 'View as table'}
            >
              {viewAsTable ? '📊 View Chart' : '🔢 View Table'}
            </button>
          )}
        </div>
      </div>

      <div className="chart-card-meta">
        {n !== undefined && (
          <span className="meta-item">
            <strong>n:</strong> {n} responses
          </span>
        )}
        {mean !== undefined && mean !== null && (
          <span className="meta-item">
            <strong>Mean:</strong> {Number(mean).toFixed(1)}
          </span>
        )}
        {median !== undefined && median !== null && (
          <span className="meta-item">
            <strong>Median:</strong> {Number(median).toFixed(1)}
          </span>
        )}
      </div>

      <div className="chart-card-body">
        {viewAsTable ? (
          <div className="table-responsive chart-table-view">
            <table className="survey-table accessible-chart-table">
              <thead>
                <tr>
                  <th scope="col">Option / Value</th>
                  <th scope="col" className="text-right">Count</th>
                  <th scope="col" className="text-right">Share</th>
                </tr>
              </thead>
              <tbody>
                {tableData.map((row, idx) => (
                  <tr key={idx}>
                    <td>{row.label || row.key || row.term}</td>
                    <td className="text-right">{row.count ?? row.value ?? '—'}</td>
                    <td className="text-right">
                      {row.percentage !== undefined
                        ? `${(row.percentage * 100).toFixed(1)}%`
                        : row.share !== undefined
                        ? `${(row.share * 100).toFixed(1)}%`
                        : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="chart-wrapper">
            {children}
          </div>
        )}
      </div>
    </div>
  );
}
