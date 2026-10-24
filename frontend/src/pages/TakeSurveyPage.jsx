import React, { useState, useEffect, useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import client from '../api/client';
import Navbar from '../components/Navbar';
import AnonymityBanner from '../components/AnonymityBanner';
import QuestionRenderer from '../components/QuestionRenderer';

export default function TakeSurveyPage() {
  const { id } = useParams();

  const [survey, setSurvey] = useState(null);
  const [answers, setAnswers] = useState({});
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [formErrors, setFormErrors] = useState({});
  const [isSubmitted, setIsSubmitted] = useState(false);

  const storageKey = `skillsense_draft_survey_${id}`;

  // 1. Fetch survey details and restore draft answers
  useEffect(() => {
    let isMounted = true;

    const fetchDetail = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await client.get(`/my/surveys/${id}`);
        if (!isMounted) return;
        setSurvey(res.data);

        // Restore draft from local storage if available
        try {
          const savedDraft = localStorage.getItem(storageKey);
          if (savedDraft) {
            setAnswers(JSON.parse(savedDraft));
          }
        } catch {
          // ignore parsing error
        }
      } catch (err) {
        if (!isMounted) return;
        handleApiError(err);
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchDetail();
    return () => {
      isMounted = false;
    };
  }, [id, storageKey]);

  // 2. Autosave in-progress answers to localStorage
  const handleAnswerChange = (questionKey, value) => {
    setAnswers((prev) => {
      const updated = { ...prev, [questionKey]: value };
      try {
        localStorage.setItem(storageKey, JSON.stringify(updated));
      } catch {
        // storage quota or private browsing
      }
      return updated;
    });

    // Clear individual field error if answered
    if (formErrors[questionKey]) {
      setFormErrors((prev) => {
        const next = { ...prev };
        delete next[questionKey];
        return next;
      });
    }
  };

  const handleApiError = (err) => {
    const status = err.response?.status;
    const code = err.response?.data?.code;
    const msg = err.response?.data?.message;

    if (status === 409 && code === 'ALREADY_SUBMITTED') {
      setError('You have already submitted this survey.');
      setIsSubmitted(true);
    } else if (status === 409 && code === 'SURVEY_NOT_ACTIVE') {
      setError('This survey is no longer accepting responses.');
    } else if (status === 403) {
      setError('You are not eligible for this survey.');
    } else if (status === 404) {
      setError('Survey not found.');
    } else {
      setError(msg || 'An error occurred while loading or submitting the survey.');
    }
  };

  // Questions and section grouping
  const questions = useMemo(() => {
    return survey?.questionSchema?.questions || [];
  }, [survey]);

  const sections = useMemo(() => {
    const map = new Map();
    questions.forEach((q) => {
      const secName = q.section || '__default__';
      if (!map.has(secName)) {
        map.set(secName, []);
      }
      map.get(secName).push(q);
    });
    return Array.from(map.entries());
  }, [questions]);

  // Progress computation
  const progressPercent = useMemo(() => {
    const requiredQuestions = questions.filter((q) => q.required);
    if (requiredQuestions.length === 0) {
      if (questions.length === 0) return 0;
      const answeredCount = questions.filter((q) => {
        const a = answers[q.key];
        return a !== undefined && a !== null && a !== '';
      }).length;
      return Math.round((answeredCount / questions.length) * 100);
    }

    const answeredRequired = requiredQuestions.filter((q) => {
      const val = answers[q.key];
      if (val === undefined || val === null || val === '') return false;
      if (Array.isArray(val) && val.length === 0) return false;
      return true;
    }).length;

    return Math.round((answeredRequired / requiredQuestions.length) * 100);
  }, [questions, answers]);

  // Client-side validation before submission
  const validateForm = () => {
    const errors = {};
    for (const q of questions) {
      if (q.required) {
        const val = answers[q.key];
        if (val === undefined || val === null || val === '') {
          errors[q.key] = 'This question is required.';
        } else if (Array.isArray(val) && val.length === 0) {
          errors[q.key] = 'Please select at least one option.';
        }
      }
    }
    return errors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    const validationErrors = validateForm();
    if (Object.keys(validationErrors).length > 0) {
      setFormErrors(validationErrors);
      setError('Please answer all required questions before submitting.');
      // Scroll first error into view
      const firstKey = Object.keys(validationErrors)[0];
      const el = document.getElementById(`q-card-${firstKey}`);
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }

    setSubmitting(true);
    try {
      await client.post(`/my/surveys/${id}/submit`, { answers });
      // Clear saved draft on successful submission
      localStorage.removeItem(storageKey);
      setIsSubmitted(true);
    } catch (err) {
      handleApiError(err);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <Navbar />
      <main className="container page-content">
        {loading && (
          <div className="loading-state" role="status">
            <div className="spinner"></div>
            <p>Loading survey questions...</p>
          </div>
        )}

        {/* Thank You / Submitted Screen */}
        {isSubmitted && (
          <div className="card thank-you-card" role="status" aria-live="polite">
            <div className="thank-you-icon" aria-hidden="true">🎉</div>
            <h2>Thank You!</h2>
            <p className="thank-you-msg">Your response has been successfully submitted.</p>
            {error && <div className="alert alert-info mt-2">{error}</div>}

            <div className="thank-you-actions">
              {!survey?.isAnonymous && (
                <Link
                  to={`/student/surveys/${id}/result`}
                  className="btn btn-primary tap-target"
                >
                  View Your Personal Result
                </Link>
              )}
              <Link to="/student" className="btn btn-secondary tap-target">
                Back to My Surveys
              </Link>
            </div>
          </div>
        )}

        {/* Survey Form */}
        {!loading && !isSubmitted && survey && (
          <div className="survey-form-container">
            <div className="survey-header-card card">
              <div className="chips-row mb-2">
                <span className={`chip chip-category category-${survey.category?.toLowerCase()}`}>
                  {survey.category}
                </span>
                <span className={`chip chip-anonymity ${survey.isAnonymous ? 'chip-anon' : 'chip-named'}`}>
                  {survey.isAnonymous ? 'Anonymous' : 'Non-Anonymous'}
                </span>
              </div>

              <h1>{survey.title}</h1>
              {survey.description && <p className="survey-description">{survey.description}</p>}

              <AnonymityBanner isAnonymous={survey.isAnonymous} />

              {/* Progress Bar */}
              <div className="progress-container" aria-label="Survey completion progress">
                <div className="progress-bar-track">
                  <div
                    className="progress-bar-fill"
                    style={{ width: `${progressPercent}%` }}
                    role="progressbar"
                    aria-valuenow={progressPercent}
                    aria-valuemin="0"
                    aria-valuemax="100"
                  ></div>
                </div>
                <div className="progress-label">
                  <span>Progress</span>
                  <span>{progressPercent}% completed</span>
                </div>
              </div>
            </div>

            {error && (
              <div className="alert alert-error" role="alert">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} noValidate>
              {sections.map(([sectionName, sectionQuestions]) => (
                <div key={sectionName} className="survey-section">
                  {sectionName !== '__default__' && (
                    <h2 className="section-title">{sectionName}</h2>
                  )}

                  <div className="questions-list">
                    {sectionQuestions.map((q) => (
                      <QuestionRenderer
                        key={q.key}
                        question={q}
                        value={answers[q.key]}
                        onChange={handleAnswerChange}
                        error={formErrors[q.key]}
                      />
                    ))}
                  </div>
                </div>
              ))}

              <div className="submit-bar card">
                <p className="submit-disclaimer">
                  Please review your answers before submitting. You cannot edit your answers once submitted.
                </p>
                <button
                  type="submit"
                  className="btn btn-primary btn-block tap-target"
                  disabled={submitting}
                >
                  {submitting ? 'Submitting...' : 'Submit Survey'}
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Error state if survey failed to load and not submitted */}
        {!loading && !survey && error && !isSubmitted && (
          <div className="card text-center">
            <h3>Unable to open survey</h3>
            <p className="text-muted">{error}</p>
            <Link to="/student" className="btn btn-primary mt-2 tap-target">
              Back to My Surveys
            </Link>
          </div>
        )}
      </main>
    </>
  );
}
