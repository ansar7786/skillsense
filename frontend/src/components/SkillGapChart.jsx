import React, { useState } from 'react';
import { Bar } from 'react-chartjs-2';
import { ensureChartRegistered } from './ChartSetup';

ensureChartRegistered();

/**
 * SkillGapChart
 * Visualizes cohort mean scores vs official skill targets using horizontal bars.
 * Includes a "View as Table" toggle for accessibility.
 */
export default function SkillGapChart({ skills = [] }) {
  const [viewTable, setViewTable] = useState(false);

  if (!skills || skills.length === 0) {
    return (
      <div className="card empty-chart-card text-center py-6">
        <p className="text-muted">No skill scoring data available for this survey.</p>
      </div>
    );
  }

  // Sort by priority ascending (1 = highest priority)
  const sortedSkills = [...skills].sort((a, b) => (a.priority || 99) - (b.priority || 99));

  const labels = sortedSkills.map((s) => s.skill.replace(/_/g, ' ').toUpperCase());
  const means = sortedSkills.map((s) => Number(s.mean.toFixed(1)));
  const targets = sortedSkills.map((s) => Number(s.target.toFixed(1)));

  const chartData = {
    labels,
    datasets: [
      {
        label: 'Cohort Mean',
        data: means,
        backgroundColor: '#4f46e5', // Deep indigo
        borderRadius: 4,
        barThickness: 16,
      },
      {
        label: 'Target',
        data: targets,
        backgroundColor: '#cbd5e1', // Light slate
        borderRadius: 4,
        barThickness: 16,
      },
    ],
  };

  const chartOptions = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
        labels: {
          font: { family: '-apple-system, sans-serif', size: 12 },
          color: '#334155',
        },
      },
      tooltip: {
        callbacks: {
          afterBody: (context) => {
            const idx = context[0].dataIndex;
            const item = sortedSkills[idx];
            return `Gap: ${item.gap > 0 ? `+${item.gap.toFixed(1)}` : item.gap.toFixed(1)}\nBelow Target: ${(item.share_below_target * 100).toFixed(0)}%`;
          },
        },
      },
    },
    scales: {
      x: {
        min: 0,
        max: 100,
        grid: { color: '#f1f5f9' },
        ticks: { color: '#64748b' },
      },
      y: {
        grid: { display: false },
        ticks: { color: '#334155', font: { weight: '600' } },
      },
    },
  };

  return (
    <div className="card chart-card" role="region" aria-label="Skill Gap Analysis">
      <div className="chart-card-header">
        <div>
          <h3 className="chart-card-title">Skill Gap Analysis (Cohort Mean vs Target)</h3>
          <p className="chart-card-subtitle text-sm text-muted">
            Ordered by priority. Target readiness is 0–100 scale.
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
                  <th scope="col">Skill</th>
                  <th scope="col" className="text-right">Cohort Mean</th>
                  <th scope="col" className="text-right">Target</th>
                  <th scope="col" className="text-right">Gap</th>
                  <th scope="col" className="text-right">Below Target</th>
                  <th scope="col" className="text-center">Priority</th>
                </tr>
              </thead>
              <tbody>
                {sortedSkills.map((s, idx) => (
                  <tr key={idx}>
                    <td className="font-medium">{s.skill.replace(/_/g, ' ').toUpperCase()}</td>
                    <td className="text-right">{s.mean.toFixed(1)}</td>
                    <td className="text-right">{s.target.toFixed(1)}</td>
                    <td className={`text-right font-medium ${s.gap < 0 ? 'text-danger' : 'text-success'}`}>
                      {s.gap > 0 ? `+${s.gap.toFixed(1)}` : s.gap.toFixed(1)}
                    </td>
                    <td className="text-right">{(s.share_below_target * 100).toFixed(1)}%</td>
                    <td className="text-center">
                      <span className="priority-pill">P{s.priority || idx + 1}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="chart-canvas-container" style={{ minHeight: `${Math.max(220, sortedSkills.length * 50)}px` }}>
            <Bar data={chartData} options={chartOptions} />
          </div>
        )}
      </div>
    </div>
  );
}
