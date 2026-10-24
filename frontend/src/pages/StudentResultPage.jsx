import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import client from '../api/client';
import Navbar from '../components/Navbar';

export default function StudentResultPage() {
  const { id } = useParams();

  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let isMounted = true;

    const fetchResult = async () => {
      setLoading(true);
      setError(null);
      setNotFound(false);
      try {
        const res = await client.get(`/my/surveys/${id}/result`);
        if (!isMounted) return;
        setResult(res.data);
      } catch (err) {
        if (!isMounted) return;
        if (err.response?.status === 404) {
          setNotFound(true);
        } else {
          setError(err.response?.data?.message || 'Failed to load your survey results.');
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchResult();
    return () => {
      isMounted = false;
    };
  }, [id]);

  const getBandClass = (band) => {
    switch (band?.toUpperCase()) {
      case 'READY':
        return 'band-ready';
      case 'DEVELOPING':
        return 'band-developing';
      case 'NEEDS_SUPPORT':
        return 'band-needs-support';
      default:
        return 'band-default';
    }
  };

  return (
    <>
      <Navbar />
      <main className="container page-content">
        <div className="page-header">
          <div>
            <h1>Personal Readiness Result</h1>
            <p className="page-subtitle">Detailed breakdown of your self-assessment</p>
          </div>
          <Link to="/student" className="btn btn-secondary btn-sm tap-target">
            Back to Surveys
          </Link>
        </div>

        {loading && (
          <div className="loading-state" role="status">
            <div className="spinner"></div>
            <p>Loading personal results...</p>
          </div>
        )}

        {notFound && (
          <div className="card text-center empty-state">
            <div className="empty-icon" aria-hidden="true">🔒</div>
            <h3>No individual score available</h3>
            <p className="text-muted">
              Individual results are not available for this survey because it is anonymous or does not have scored skills.
            </p>
            <Link to="/student" className="btn btn-primary mt-3 tap-target">
              Return to My Surveys
            </Link>
          </div>
        )}

        {error && !notFound && (
          <div className="alert alert-error" role="alert">
            <p>{error}</p>
            <Link to="/student" className="btn btn-secondary btn-sm mt-2 tap-target">
              Return to My Surveys
            </Link>
          </div>
        )}

        {!loading && !notFound && result && (
          <div className="result-container">
            {/* Top Score Banner Card */}
            <div className="card readiness-card text-center">
              <span className="readiness-label">Overall Readiness Score</span>
              <div className="readiness-score-display">
                <span className="readiness-number">{Math.round(result.readiness)}</span>
                <span className="readiness-scale">/ 100</span>
              </div>
              <div className="band-badge-container">
                <span className={`band-badge ${getBandClass(result.band)}`}>
                  {result.band?.replace('_', ' ')}
                </span>
              </div>
              <p className="disclaimer-text">
                {result.disclaimer || 'Self-assessed indicator, not an exam score'}
              </p>
            </div>

            {/* Highlights: Strongest & Weakest Skill */}
            {(result.strongest_skill || result.weakest_skill) && (
              <div className="grid-two-cards">
                {result.strongest_skill && (
                  <div className="card highlight-card highlight-strong">
                    <span className="highlight-tag">🌟 Strongest Skill</span>
                    <h3 className="highlight-skill-name">{result.strongest_skill}</h3>
                  </div>
                )}
                {result.weakest_skill && (
                  <div className="card highlight-card highlight-weak">
                    <span className="highlight-tag">🎯 Key Growth Area</span>
                    <h3 className="highlight-skill-name">{result.weakest_skill}</h3>
                  </div>
                )}
              </div>
            )}

            {/* Skills Breakdown */}
            {result.skills && result.skills.length > 0 && (
              <div className="card skills-breakdown-card">
                <h2>Skill Performance vs Target</h2>
                <div className="skills-list">
                  {result.skills.map((s) => {
                    const score = Math.round(s.score);
                    const target = Math.round(s.target);
                    const gap = Math.round(s.gap);
                    const meetsTarget = score >= target;

                    return (
                      <div key={s.skill} className="skill-item">
                        <div className="skill-header">
                          <span className="skill-title">{s.skill}</span>
                          <span className="skill-score-summary">
                            <strong>{score}</strong> / target {target}
                            <span className={`gap-tag ${meetsTarget ? 'gap-met' : 'gap-unmet'}`}>
                              {meetsTarget ? 'Target Met' : `${gap} pt gap`}
                            </span>
                          </span>
                        </div>

                        {/* Bar comparison */}
                        <div className="bar-track">
                          <div
                            className={`bar-fill ${meetsTarget ? 'bar-fill-met' : 'bar-fill-unmet'}`}
                            style={{ width: `${Math.min(100, Math.max(0, score))}%` }}
                            aria-label={`${s.skill} score: ${score}`}
                          ></div>
                          <div
                            className="target-marker"
                            style={{ left: `${Math.min(100, Math.max(0, target))}%` }}
                            title={`Target: ${target}`}
                          ></div>
                        </div>

                        {s.cohort_mean !== null && s.cohort_mean !== undefined && (
                          <div className="cohort-mean-label">
                            Cohort average: {Math.round(s.cohort_mean)}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Improvement Tips */}
            {result.tips && result.tips.length > 0 && (
              <div className="card tips-card">
                <h2>Recommended Next Steps</h2>
                <ul className="tips-list">
                  {result.tips.map((tip, idx) => (
                    <li key={idx} className="tip-item">
                      <span className="tip-bullet" aria-hidden="true">💡</span>
                      <span className="tip-text">{tip}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </main>
    </>
  );
}
