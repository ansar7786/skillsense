import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import client from '../api/client';
import Navbar from '../components/Navbar';

export default function StudentHomePage() {
  const [surveys, setSurveys] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchSurveys = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await client.get('/my/surveys');
      const list = res.data || [];

      // Sort: 1. PENDING first, 2. then by closesAt deadline ascending
      list.sort((a, b) => {
        if (a.status === 'PENDING' && b.status !== 'PENDING') return -1;
        if (a.status !== 'PENDING' && b.status === 'PENDING') return 1;

        const dateA = a.closesAt ? new Date(a.closesAt).getTime() : Infinity;
        const dateB = b.closesAt ? new Date(b.closesAt).getTime() : Infinity;
        return dateA - dateB;
      });

      setSurveys(list);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load assigned surveys. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSurveys();
  }, []);

  const formatDeadline = (dateStr) => {
    if (!dateStr) return 'No deadline';
    const date = new Date(dateStr);
    return date.toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <>
      <Navbar />
      <main className="container page-content">
        <div className="page-header">
          <div>
            <h1>My Surveys</h1>
            <p className="page-subtitle">Complete your assigned surveys and view feedback</p>
          </div>
          <button
            type="button"
            onClick={fetchSurveys}
            className="btn btn-secondary btn-sm tap-target"
            disabled={loading}
          >
            Refresh
          </button>
        </div>

        {loading && (
          <div className="loading-state" role="status">
            <div className="spinner"></div>
            <p>Loading assigned surveys...</p>
          </div>
        )}

        {error && (
          <div className="alert alert-error" role="alert">
            <p>{error}</p>
            <button type="button" onClick={fetchSurveys} className="btn btn-secondary btn-sm mt-2 tap-target">
              Retry
            </button>
          </div>
        )}

        {!loading && !error && surveys.length === 0 && (
          <div className="empty-state card">
            <div className="empty-icon" aria-hidden="true">📋</div>
            <h3>No assigned surveys</h3>
            <p>You have no pending or assigned surveys at this time.</p>
          </div>
        )}

        {!loading && !error && surveys.length > 0 && (
          <div className="survey-list" role="list">
            {surveys.map((survey) => {
              const isPending = survey.status === 'PENDING';
              return (
                <div key={survey.surveyId} className="survey-card card" role="listitem">
                  <div className="survey-card-header">
                    <div className="chips-row">
                      <span className={`chip chip-category category-${survey.category?.toLowerCase()}`}>
                        {survey.category}
                      </span>
                      <span className={`chip chip-anonymity ${survey.isAnonymous ? 'chip-anon' : 'chip-named'}`}>
                        {survey.isAnonymous ? 'Anonymous' : 'Non-Anonymous'}
                      </span>
                      <span className={`chip chip-status ${isPending ? 'status-pending' : 'status-submitted'}`}>
                        {survey.status}
                      </span>
                    </div>
                  </div>

                  <h3 className="survey-title">{survey.title}</h3>

                  <div className="survey-meta">
                    <span className="deadline-label">
                      <strong>Deadline:</strong> {formatDeadline(survey.closesAt)}
                    </span>
                  </div>

                  <div className="survey-card-actions">
                    {isPending ? (
                      <Link
                        to={`/student/surveys/${survey.surveyId}`}
                        className="btn btn-primary tap-target"
                      >
                        Take Survey
                      </Link>
                    ) : (
                      <div className="submitted-actions">
                        <span className="submitted-checkmark" aria-hidden="true">✓ Submitted</span>
                        {!survey.isAnonymous && (
                          <Link
                            to={`/student/surveys/${survey.surveyId}/result`}
                            className="btn btn-secondary tap-target"
                          >
                            View Result
                          </Link>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>
    </>
  );
}
