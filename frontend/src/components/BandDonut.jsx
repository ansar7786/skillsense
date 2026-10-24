import React, { useState } from 'react';
import { Doughnut } from 'react-chartjs-2';
import { ensureChartRegistered } from './ChartSetup';

ensureChartRegistered();

/**
 * BandDonut
 * Displays the distribution of students across the three readiness bands:
 * READY (Green), DEVELOPING (Amber), and NEEDS_SUPPORT (Red).
 */
export default function BandDonut({ bands = {}, totalResponses = 0 }) {
  const [viewTable, setViewTable] = useState(false);

  const readyShare = bands.READY || 0;
  const devShare = bands.DEVELOPING || 0;
  const supportShare = bands.NEEDS_SUPPORT || 0;

  const readyPct = (readyShare * 100).toFixed(1);
  const devPct = (devShare * 100).toFixed(1);
  const supportPct = (supportShare * 100).toFixed(1);

  const chartData = {
    labels: ['Ready', 'Developing', 'Needs Support'],
    datasets: [
      {
        data: [Number(readyPct), Number(devPct), Number(supportPct)],
        backgroundColor: ['#16a34a', '#d97706', '#dc2626'],
        borderColor: '#ffffff',
        borderWidth: 2,
        hoverOffset: 6,
      },
    ],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '68%',
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          padding: 16,
          font: { family: '-apple-system, sans-serif', size: 12 },
          color: '#334155',
        },
      },
      tooltip: {
        callbacks: {
          label: (context) => ` ${context.label}: ${context.raw}%`,
        },
      },
    },
  };

  return (
    <div className="card chart-card band-donut-card" role="region" aria-label="Readiness Band Distribution">
      <div className="chart-card-header">
        <div>
          <h3 className="chart-card-title">Readiness Band Distribution</h3>
          <p className="chart-card-subtitle text-sm text-muted">
            Proportion of cohort falling into each performance category.
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

      <div className="chart-card-body">
        {viewTable ? (
          <div className="table-responsive">
            <table className="survey-table">
              <thead>
                <tr>
                  <th scope="col">Band</th>
                  <th scope="col" className="text-right">Share (%)</th>
                  <th scope="col" className="text-right">Est. Count</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>
                    <span className="status-badge status-active">READY</span>
                  </td>
                  <td className="text-right font-medium">{readyPct}%</td>
                  <td className="text-right">{Math.round(readyShare * totalResponses)}</td>
                </tr>
                <tr>
                  <td>
                    <span className="status-badge status-draft">DEVELOPING</span>
                  </td>
                  <td className="text-right font-medium">{devPct}%</td>
                  <td className="text-right">{Math.round(devShare * totalResponses)}</td>
                </tr>
                <tr>
                  <td>
                    <span className="status-badge status-closed" style={{ color: '#dc2626', borderColor: '#fca5a5', background: '#fee2e2' }}>
                      NEEDS SUPPORT
                    </span>
                  </td>
                  <td className="text-right font-medium">{supportPct}%</td>
                  <td className="text-right">{Math.round(supportShare * totalResponses)}</td>
                </tr>
              </tbody>
            </table>
          </div>
        ) : (
          <div className="donut-container" style={{ position: 'relative', height: '240px' }}>
            <Doughnut data={chartData} options={chartOptions} />
          </div>
        )}
      </div>
    </div>
  );
}
