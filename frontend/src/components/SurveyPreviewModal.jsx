import React, { useState } from 'react';
import QuestionRenderer from './QuestionRenderer';
import AnonymityBanner from './AnonymityBanner';

export default function SurveyPreviewModal({
  survey,
  isOpen,
  onClose
}) {
  const [answers, setAnswers] = useState({});

  if (!isOpen || !survey) return null;

  const questions = survey.questionSchema?.questions || [];

  const handleAnswerChange = (key, val) => {
    setAnswers((prev) => ({ ...prev, [key]: val }));
  };

  // Strip internal scoring metadata from questions to simulate pure student view
  const sanitizeForStudent = (q) => {
    const copy = { ...q };
    delete copy.scoring;
    // Strip option-level scores if present
    if (copy.options) {
      copy.options = copy.options.map((opt) => {
        const { score, ...rest } = opt;
        return rest;
      });
    }
    return copy;
  };

  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="preview-modal-title">
      <div className="modal-container preview-modal-container">
        {/* Preview Top Banner */}
        <div className="preview-top-banner">
          <div className="d-flex align-center gap-2">
            <span className="preview-badge">Student Preview</span>
            <span className="preview-desc">
              This preview shows exactly what eligible students will see. Scoring rules and benchmark targets are hidden.
            </span>
          </div>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={onClose}
            aria-label="Close Preview"
          >
            Close Preview ✕
          </button>
        </div>

        {/* Survey Content */}
        <div className="preview-content-scroll">
          <div className="survey-container preview-survey-body">
            {/* Anonymity Banner */}
            <AnonymityBanner isAnonymous={Boolean(survey.isAnonymous)} />

            {/* Survey Header */}
            <div className="card survey-header-card mt-3">
              <div className="survey-category-tag mb-2">
                <span className={`category-pill category-${(survey.category || 'CUSTOM').toLowerCase()}`}>
                  {survey.category || 'CUSTOM'}
                </span>
                {survey.termLabel && (
                  <span className="term-pill ml-2">{survey.termLabel}</span>
                )}
              </div>
              <h2 id="preview-modal-title" className="survey-title">
                {survey.title || 'Untitled Survey'}
              </h2>
              {survey.description && (
                <p className="survey-description text-muted mt-2">
                  {survey.description}
                </p>
              )}
            </div>

            {/* Questions List */}
            <div className="questions-list mt-4">
              {questions.length === 0 ? (
                <div className="card text-center py-6 text-muted">
                  No questions in this survey yet.
                </div>
              ) : (
                questions.map((q, idx) => {
                  const sanitizedQ = sanitizeForStudent(q);
                  return (
                    <QuestionRenderer
                      key={q.key || idx}
                      question={sanitizedQ}
                      index={idx}
                      value={answers[q.key] !== undefined ? answers[q.key] : ''}
                      onChange={(val) => handleAnswerChange(q.key, val)}
                    />
                  );
                })
              )}
            </div>

            {/* Simulated Submit Button */}
            {questions.length > 0 && (
              <div className="preview-actions mt-4">
                <button
                  type="button"
                  className="btn btn-primary"
                  disabled
                  title="Submission is disabled in preview mode"
                >
                  Submit Survey (Preview Only)
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
