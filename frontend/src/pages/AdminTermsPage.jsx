import React, { useState, useEffect, useCallback } from 'react';
import Navbar from '../components/Navbar';
import {
  fetchAcademicTerms,
  createAcademicTerm,
  setCurrentAcademicTerm
} from '../api/admin';

export default function AdminTermsPage() {
  const [terms, setTerms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  // Modal State
  const [showAddModal, setShowAddModal] = useState(false);
  const [formError, setFormError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [formData, setFormData] = useState({
    label: '',
    startDate: '',
    endDate: '',
    isCurrent: false
  });

  const loadTerms = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchAcademicTerms();
      setTerms(data || []);
    } catch (err) {
      console.error('Failed to load academic terms:', err);
      setError(err.response?.data?.message || 'Failed to load academic terms.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadTerms();
  }, [loadTerms]);

  const openAddModal = () => {
    setFormData({
      label: '',
      startDate: '',
      endDate: '',
      isCurrent: false
    });
    setFormError(null);
    setShowAddModal(true);
  };

  const handleCreateTerm = async (e) => {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    try {
      const payload = {
        label: formData.label.trim(),
        startDate: formData.startDate,
        endDate: formData.endDate,
        isCurrent: Boolean(formData.isCurrent)
      };
      await createAcademicTerm(payload);
      setShowAddModal(false);
      setActionSuccess(`Academic term "${payload.label}" created successfully.`);
      loadTerms();
    } catch (err) {
      console.error('Failed to create term:', err);
      setFormError(err.response?.data?.message || 'Failed to create term. Check date formats.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleSetCurrent = async (term) => {
    if (term.isCurrent) return;
    try {
      await setCurrentAcademicTerm(term.termId);
      setActionSuccess(`Term "${term.label}" is now the active current academic term.`);
      loadTerms();
    } catch (err) {
      console.error('Failed to set current term:', err);
      setError(err.response?.data?.message || 'Failed to update current term.');
    }
  };

  return (
    <>
      <Navbar />
      <main className="container page-content admin-terms-container">
        {/* Header Bar */}
        <div className="card mb-4">
          <div className="d-flex justify-between align-center flex-wrap gap-3">
            <div>
              <div className="meta-badges-row mb-1">
                <span className="category-pill category-academic">INSTITUTIONAL SETTINGS</span>
                <span className="role-badge">ADMIN ONLY</span>
              </div>
              <h1 className="dashboard-survey-title" style={{ fontSize: '1.5rem', marginBottom: '4px' }}>
                Academic Terms
              </h1>
              <p className="text-muted text-sm" style={{ margin: 0 }}>
                Configure semester terms and designate the institution-wide active term.
              </p>
            </div>
            <div>
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={openAddModal}
              >
                + Add Academic Term
              </button>
            </div>
          </div>
        </div>

        {/* Global Feedback Notifications */}
        {actionSuccess && (
          <div className="alert alert-success mb-3 d-flex justify-between align-center" role="status">
            <span>✅ {actionSuccess}</span>
            <button
              type="button"
              className="btn btn-sm btn-outline text-xs"
              onClick={() => setActionSuccess(null)}
            >
              Dismiss
            </button>
          </div>
        )}

        {error && (
          <div className="alert alert-danger mb-3 d-flex justify-between align-center" role="alert">
            <span>⚠️ {error}</span>
            <button
              type="button"
              className="btn btn-sm btn-outline text-xs"
              onClick={() => setError(null)}
            >
              Dismiss
            </button>
          </div>
        )}

        {/* Terms Table */}
        <div className="card mb-4" style={{ padding: 0, overflow: 'hidden' }}>
          {loading ? (
            <div className="py-8 text-center" role="status">
              <div className="spinner"></div>
              <p className="mt-3 text-muted text-sm">Loading academic terms...</p>
            </div>
          ) : terms.length === 0 ? (
            <div className="py-8 text-center">
              <p className="text-muted">No academic terms configured yet.</p>
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table className="survey-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ background: 'var(--bg-subtle)', borderBottom: '1px solid var(--border-color)', textAlign: 'left' }}>
                    <th style={{ padding: '12px 16px', fontSize: '0.8rem', fontWeight: 600 }}>TERM LABEL</th>
                    <th style={{ padding: '12px 16px', fontSize: '0.8rem', fontWeight: 600 }}>START DATE</th>
                    <th style={{ padding: '12px 16px', fontSize: '0.8rem', fontWeight: 600 }}>END DATE</th>
                    <th style={{ padding: '12px 16px', fontSize: '0.8rem', fontWeight: 600 }}>STATUS</th>
                    <th style={{ padding: '12px 16px', fontSize: '0.8rem', fontWeight: 600, textAlign: 'right' }}>ACTION</th>
                  </tr>
                </thead>
                <tbody>
                  {terms.map((t) => (
                    <tr
                      key={t.termId}
                      style={{
                        borderBottom: '1px solid var(--border-color)',
                        background: t.isCurrent ? 'var(--primary-light)' : 'transparent'
                      }}
                    >
                      <td style={{ padding: '14px 16px', fontWeight: 600 }}>
                        {t.label}
                      </td>
                      <td style={{ padding: '14px 16px', fontFamily: 'monospace', fontSize: '0.85rem' }}>
                        {t.startDate}
                      </td>
                      <td style={{ padding: '14px 16px', fontFamily: 'monospace', fontSize: '0.85rem' }}>
                        {t.endDate}
                      </td>
                      <td style={{ padding: '14px 16px' }}>
                        {t.isCurrent ? (
                          <span
                            className="status-badge status-active"
                            style={{ fontSize: '0.75rem', fontWeight: 700, padding: '3px 10px' }}
                          >
                            ⭐ CURRENT TERM
                          </span>
                        ) : (
                          <span className="text-xs text-muted">Past / Upcoming</span>
                        )}
                      </td>
                      <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                        {t.isCurrent ? (
                          <span className="text-xs font-semibold" style={{ color: 'var(--status-ready-text)' }}>
                            Active
                          </span>
                        ) : (
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm text-xs"
                            onClick={() => handleSetCurrent(t)}
                          >
                            Set as Current
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* ADD TERM MODAL */}
        {showAddModal && (
          <div className="modal-backdrop d-flex align-center justify-center" role="dialog" aria-modal="true">
            <div className="card modal-content" style={{ width: '100%', maxWidth: '480px', padding: 'var(--space-5)' }}>
              <div className="d-flex justify-between align-center mb-3">
                <h3 style={{ margin: 0, fontSize: '1.25rem' }}>Add Academic Term</h3>
                <button
                  type="button"
                  className="btn btn-outline btn-sm text-xs"
                  onClick={() => setShowAddModal(false)}
                >
                  ✕
                </button>
              </div>

              {formError && (
                <div className="alert alert-danger mb-3 text-sm">
                  ⚠️ {formError}
                </div>
              )}

              <form onSubmit={handleCreateTerm}>
                <div className="form-group mb-3">
                  <label className="form-label text-xs font-semibold">TERM LABEL *</label>
                  <input
                    type="text"
                    required
                    maxLength={40}
                    className="form-input"
                    placeholder="e.g. 2026-27 Odd"
                    value={formData.label}
                    onChange={(e) => setFormData({ ...formData, label: e.target.value })}
                  />
                </div>

                <div className="d-flex gap-3 mb-3">
                  <div className="form-group flex-1" style={{ margin: 0 }}>
                    <label className="form-label text-xs font-semibold">START DATE *</label>
                    <input
                      type="date"
                      required
                      className="form-input"
                      value={formData.startDate}
                      onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                    />
                  </div>
                  <div className="form-group flex-1" style={{ margin: 0 }}>
                    <label className="form-label text-xs font-semibold">END DATE *</label>
                    <input
                      type="date"
                      required
                      className="form-input"
                      value={formData.endDate}
                      onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                    />
                  </div>
                </div>

                <div className="form-group mb-4">
                  <label className="d-flex align-center gap-2" style={{ cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={formData.isCurrent}
                      onChange={(e) => setFormData({ ...formData, isCurrent: e.target.checked })}
                    />
                    <span className="text-sm font-semibold">Set immediately as current term</span>
                  </label>
                </div>

                <div className="d-flex justify-end gap-2 pt-2 border-top">
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => setShowAddModal(false)}
                    disabled={submitting}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary btn-sm"
                    disabled={submitting}
                  >
                    {submitting ? 'Creating...' : 'Create Term'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </main>
    </>
  );
}
