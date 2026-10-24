import React from 'react';

export default function ActivationConfirmDialog({
  isOpen,
  survey,
  isActivating,
  onConfirm,
  onCancel
}) {
  if (!isOpen || !survey) return null;

  const isAnonymous = Boolean(survey.isAnonymous);

  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="activation-modal-title">
      <div className="modal-container activation-modal-container">
        <div className="modal-header">
          <div className="modal-icon-badge" aria-hidden="true">🚀</div>
          <h3 id="activation-modal-title" className="modal-title">Activate Survey</h3>
        </div>

        <div className="modal-body">
          <p className="mb-3">
            You are about to activate <strong>{survey.title}</strong>. Once activated, the survey will immediately become available to all eligible students.
          </p>

          <div className={`anonymity-confirmation-card ${isAnonymous ? 'anon-card' : 'non-anon-card'}`}>
            <div className="anonymity-confirmation-header">
              <span className="anonymity-icon" aria-hidden="true">
                {isAnonymous ? '🔒' : '👤'}
              </span>
              <strong>Anonymity Setting:</strong>{' '}
              {isAnonymous ? 'Strictly Anonymous' : 'Non-Anonymous (Identified)'}
            </div>
            <p className="anonymity-confirmation-body">
              {isAnonymous ? (
                <>
                  Student names and IDs <strong>will NOT</strong> be stored with their answers. Responses cannot be linked to any individual student.
                </>
              ) : (
                <>
                  Student names and IDs <strong>will be recorded</strong> alongside their responses.
                </>
              )}
            </p>
          </div>

          <div className="alert alert-warning mt-3 py-2 px-3 text-xs">
            <strong>Irreversible Action:</strong> Once activated, questions, targeting rules, and anonymity settings cannot be edited.
          </div>
        </div>

        <div className="modal-footer">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={onCancel}
            disabled={isActivating}
          >
            Cancel
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={onConfirm}
            disabled={isActivating}
          >
            {isActivating ? 'Activating...' : 'Activate Survey'}
          </button>
        </div>
      </div>
    </div>
  );
}
