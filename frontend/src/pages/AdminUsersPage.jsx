import React, { useState, useEffect, useCallback } from 'react';
import Navbar from '../components/Navbar';
import {
  fetchUsers,
  createUser,
  updateUser,
  resetPassword,
  importUsersCsv
} from '../api/admin';
import { triggerFileDownload } from '../api/surveys';

const ROLES = ['STUDENT', 'FACULTY', 'HOD', 'PLACEMENT_OFFICER', 'ADMIN'];

export default function AdminUsersPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters & Pagination
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [branchFilter, setBranchFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Modals & Action States
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeactivateModal, setShowDeactivateModal] = useState(false);
  const [showResetModal, setShowResetModal] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [showCredsModal, setShowCredsModal] = useState(false);

  // Target User for Edit / Deactivate / Reset
  const [targetUser, setTargetUser] = useState(null);
  const [tempPassword, setTempPassword] = useState('');
  const [actionSuccess, setActionSuccess] = useState(null);

  // Form states
  const [formData, setFormData] = useState({
    collegeId: '',
    name: '',
    email: '',
    role: 'STUDENT',
    branch: '',
    studyYear: '',
    section: '',
    isActive: true
  });
  const [formError, setFormError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // CSV Import States
  const [csvFile, setCsvFile] = useState(null);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const [importError, setImportError] = useState(null);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = {
        page,
        size: 15
      };
      if (searchQuery.trim()) params.q = searchQuery.trim();
      if (roleFilter) params.role = roleFilter;
      if (branchFilter.trim()) params.branch = branchFilter.trim();

      const res = await fetchUsers(params);
      setUsers(res.content || []);
      setTotalPages(res.totalPages || 0);
      setTotalElements(res.totalElements || 0);
    } catch (err) {
      console.error('Failed to load users:', err);
      setError(err.response?.data?.message || 'Failed to load user list.');
    } finally {
      setLoading(false);
    }
  }, [page, searchQuery, roleFilter, branchFilter]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  // Handle Search Submission
  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    loadUsers();
  };

  // Open Add Modal
  const openAddModal = () => {
    setFormData({
      collegeId: '',
      name: '',
      email: '',
      role: 'STUDENT',
      branch: '',
      studyYear: '',
      section: '',
      isActive: true
    });
    setFormError(null);
    setShowAddModal(true);
  };

  // Submit Add User
  const handleAddUser = async (e) => {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    try {
      const payload = {
        collegeId: formData.collegeId.trim().toUpperCase(),
        name: formData.name.trim(),
        email: formData.email.trim() || null,
        role: formData.role,
        branch: formData.branch.trim() || null,
        studyYear: formData.studyYear ? parseInt(formData.studyYear, 10) : null,
        section: formData.section.trim() || null
      };
      const res = await createUser(payload);
      setShowAddModal(false);
      setTargetUser(res.user);
      setTempPassword(res.temporaryPassword);
      setShowCredsModal(true);
      setActionSuccess(`User ${res.user.collegeId} created successfully.`);
      loadUsers();
    } catch (err) {
      console.error('Failed to create user:', err);
      setFormError(err.response?.data?.message || 'Failed to create user.');
    } finally {
      setSubmitting(false);
    }
  };

  // Open Edit Modal
  const openEditModal = (user) => {
    setTargetUser(user);
    setFormData({
      collegeId: user.collegeId,
      name: user.name,
      email: user.email || '',
      role: user.role,
      branch: user.branch || '',
      studyYear: user.studyYear ? String(user.studyYear) : '',
      section: user.section || '',
      isActive: user.isActive
    });
    setFormError(null);
    setShowEditModal(true);
  };

  // Submit Edit User
  const handleEditUser = async (e) => {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    try {
      const payload = {
        name: formData.name.trim(),
        email: formData.email.trim() || null,
        branch: formData.branch.trim() || null,
        studyYear: formData.studyYear ? parseInt(formData.studyYear, 10) : null,
        section: formData.section.trim() || null,
        isActive: formData.isActive
      };
      await updateUser(targetUser.userId, payload);
      setShowEditModal(false);
      setActionSuccess(`User ${targetUser.collegeId} updated successfully.`);
      loadUsers();
    } catch (err) {
      console.error('Failed to update user:', err);
      setFormError(err.response?.data?.message || 'Failed to update user.');
    } finally {
      setSubmitting(false);
    }
  };

  // Open Deactivate Confirmation
  const openDeactivateModal = (user) => {
    setTargetUser(user);
    setShowDeactivateModal(true);
  };

  // Execute Deactivate
  const handleDeactivate = async () => {
    if (!targetUser) return;
    setSubmitting(true);
    try {
      const payload = {
        name: targetUser.name,
        email: targetUser.email,
        branch: targetUser.branch,
        studyYear: targetUser.studyYear,
        section: targetUser.section,
        isActive: false
      };
      await updateUser(targetUser.userId, payload);
      setShowDeactivateModal(false);
      setActionSuccess(`User ${targetUser.collegeId} deactivated.`);
      loadUsers();
    } catch (err) {
      console.error('Failed to deactivate user:', err);
      setError(err.response?.data?.message || 'Failed to deactivate user.');
    } finally {
      setSubmitting(false);
    }
  };

  // Execute Reactivate
  const handleReactivate = async (user) => {
    try {
      const payload = {
        name: user.name,
        email: user.email,
        branch: user.branch,
        studyYear: user.studyYear,
        section: user.section,
        isActive: true
      };
      await updateUser(user.userId, payload);
      setActionSuccess(`User ${user.collegeId} reactivated.`);
      loadUsers();
    } catch (err) {
      console.error('Failed to reactivate user:', err);
      setError(err.response?.data?.message || 'Failed to reactivate user.');
    }
  };

  // Open Reset Password Confirmation
  const openResetModal = (user) => {
    setTargetUser(user);
    setShowResetModal(true);
  };

  // Execute Password Reset
  const handleResetPassword = async () => {
    if (!targetUser) return;
    setSubmitting(true);
    try {
      const res = await resetPassword(targetUser.userId);
      setShowResetModal(false);
      setTempPassword(res.temporaryPassword);
      setShowCredsModal(true);
      setActionSuccess(`Password reset for ${targetUser.collegeId}.`);
    } catch (err) {
      console.error('Failed to reset password:', err);
      setError(err.response?.data?.message || 'Failed to reset password.');
    } finally {
      setSubmitting(false);
    }
  };

  // Open CSV Import Modal
  const openImportModal = () => {
    setCsvFile(null);
    setImportResult(null);
    setImportError(null);
    setShowImportModal(true);
  };

  // Execute CSV Import
  const handleImportCsv = async (e) => {
    e.preventDefault();
    if (!csvFile) {
      setImportError('Please select a valid .csv file to import.');
      return;
    }
    setImporting(true);
    setImportError(null);
    try {
      const res = await importUsersCsv(csvFile);
      setImportResult(res);
      loadUsers();
    } catch (err) {
      console.error('CSV import failed:', err);
      setImportError(err.response?.data?.message || 'CSV Import failed. Check file format.');
    } finally {
      setImporting(false);
    }
  };

  // Download Generated Credentials CSV
  const handleDownloadCredentials = () => {
    if (!importResult?.credentials) return;
    const blob = new Blob([importResult.credentials], { type: 'text/csv;charset=utf-8;' });
    triggerFileDownload(blob, 'imported_user_credentials.csv');
  };

  return (
    <>
      <Navbar />
      <main className="container page-content admin-users-container">
        {/* Header Bar */}
        <div className="card mb-4">
          <div className="d-flex justify-between align-center flex-wrap gap-3">
            <div>
              <div className="meta-badges-row mb-1">
                <span className="category-pill category-academic">SYSTEM ADMINISTRATION</span>
                <span className="role-badge">ADMIN ONLY</span>
              </div>
              <h1 className="dashboard-survey-title" style={{ fontSize: '1.5rem', marginBottom: '4px' }}>
                User Management
              </h1>
              <p className="text-muted text-sm" style={{ margin: 0 }}>
                Create, update, deactivate users, reset credentials, and batch import via CSV.
              </p>
            </div>
            <div className="d-flex align-center gap-2 flex-wrap">
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={openImportModal}
              >
                📄 Import CSV
              </button>
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={openAddModal}
              >
                + Add User
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

        {/* Filters & Search Bar */}
        <div className="card mb-4" style={{ padding: 'var(--space-3) var(--space-4)' }}>
          <form onSubmit={handleSearchSubmit} className="d-flex align-center gap-3 flex-wrap">
            <div className="form-group flex-1" style={{ margin: 0, minWidth: '220px' }}>
              <input
                type="text"
                className="form-input"
                placeholder="Search by college ID or name..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
            <div className="form-group" style={{ margin: 0, minWidth: '150px' }}>
              <select
                className="form-input"
                value={roleFilter}
                onChange={(e) => {
                  setRoleFilter(e.target.value);
                  setPage(0);
                }}
              >
                <option value="">All Roles</option>
                {ROLES.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group" style={{ margin: 0, width: '130px' }}>
              <input
                type="text"
                className="form-input"
                placeholder="Branch (e.g. CS)"
                value={branchFilter}
                onChange={(e) => setBranchFilter(e.target.value)}
              />
            </div>
            <button type="submit" className="btn btn-secondary btn-sm" style={{ height: '38px' }}>
              Filter
            </button>
            {(searchQuery || roleFilter || branchFilter) && (
              <button
                type="button"
                className="btn btn-outline btn-sm text-xs"
                onClick={() => {
                  setSearchQuery('');
                  setRoleFilter('');
                  setBranchFilter('');
                  setPage(0);
                }}
              >
                Clear
              </button>
            )}
          </form>
        </div>

        {/* Users Table */}
        <div className="card mb-4" style={{ padding: 0, overflow: 'hidden' }}>
          {loading ? (
            <div className="py-8 text-center" role="status">
              <div className="spinner"></div>
              <p className="mt-3 text-muted text-sm">Loading user directory...</p>
            </div>
          ) : users.length === 0 ? (
            <div className="py-8 text-center">
              <p className="text-muted">No users found matching current filters.</p>
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table className="survey-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ background: 'var(--bg-subtle)', borderBottom: '1px solid var(--border-color)', textAlign: 'left' }}>
                    <th style={{ padding: '10px 16px', fontSize: '0.8rem', fontWeight: 600 }}>COLLEGE ID</th>
                    <th style={{ padding: '10px 16px', fontSize: '0.8rem', fontWeight: 600 }}>NAME</th>
                    <th style={{ padding: '10px 16px', fontSize: '0.8rem', fontWeight: 600 }}>ROLE</th>
                    <th style={{ padding: '10px 16px', fontSize: '0.8rem', fontWeight: 600 }}>ACADEMIC INFO</th>
                    <th style={{ padding: '10px 16px', fontSize: '0.8rem', fontWeight: 600 }}>STATUS</th>
                    <th style={{ padding: '10px 16px', fontSize: '0.8rem', fontWeight: 600, textAlign: 'right' }}>ACTIONS</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => (
                    <tr
                      key={u.userId}
                      style={{
                        borderBottom: '1px solid var(--border-color)',
                        opacity: u.isActive ? 1 : 0.65
                      }}
                    >
                      <td style={{ padding: '12px 16px', fontWeight: 600, fontFamily: 'monospace' }}>
                        {u.collegeId}
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        <div>{u.name}</div>
                        {u.email && <div className="text-xs text-muted">{u.email}</div>}
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        <span className="role-badge" style={{ textTransform: 'uppercase' }}>
                          {u.role}
                        </span>
                      </td>
                      <td style={{ padding: '12px 16px', fontSize: '0.85rem' }}>
                        {u.branch || '—'}
                        {u.studyYear ? ` · Y${u.studyYear}` : ''}
                        {u.section ? ` (${u.section})` : ''}
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        <span
                          className={`status-badge ${u.isActive ? 'status-active' : 'status-closed'}`}
                          style={{ fontSize: '0.75rem', padding: '2px 8px' }}
                        >
                          {u.isActive ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                        <div className="d-flex justify-end gap-2 align-center">
                          <button
                            type="button"
                            className="btn btn-outline btn-sm text-xs"
                            onClick={() => openEditModal(u)}
                            title="Edit User Details"
                          >
                            Edit
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline btn-sm text-xs"
                            onClick={() => openResetModal(u)}
                            title="Reset User Password"
                          >
                            Reset Pwd
                          </button>
                          {u.isActive ? (
                            <button
                              type="button"
                              className="btn btn-warning btn-sm text-xs"
                              onClick={() => openDeactivateModal(u)}
                              title="Deactivate User"
                            >
                              Deactivate
                            </button>
                          ) : (
                            <button
                              type="button"
                              className="btn btn-secondary btn-sm text-xs"
                              onClick={() => handleReactivate(u)}
                              title="Reactivate User"
                            >
                              Activate
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="d-flex justify-between align-center p-3 border-top" style={{ background: 'var(--bg-subtle)' }}>
              <span className="text-xs text-muted">
                Showing {users.length} of {totalElements} users (Page {page + 1} of {totalPages})
              </span>
              <div className="d-flex gap-2">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm text-xs"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  ← Previous
                </button>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm text-xs"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next →
                </button>
              </div>
            </div>
          )}
        </div>

        {/* ADD USER MODAL */}
        {showAddModal && (
          <div className="modal-backdrop d-flex align-center justify-center" role="dialog" aria-modal="true">
            <div className="card modal-content" style={{ width: '100%', maxWidth: '520px', padding: 'var(--space-5)' }}>
              <div className="d-flex justify-between align-center mb-3">
                <h3 style={{ margin: 0, fontSize: '1.25rem' }}>Add New User</h3>
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

              <form onSubmit={handleAddUser}>
                <div className="form-group mb-3">
                  <label className="form-label text-xs font-semibold">COLLEGE ID *</label>
                  <input
                    type="text"
                    required
                    maxLength={30}
                    className="form-input font-mono"
                    placeholder="e.g. 26CS01 or FAC_CS01"
                    value={formData.collegeId}
                    onChange={(e) => setFormData({ ...formData, collegeId: e.target.value })}
                  />
                </div>

                <div className="form-group mb-3">
                  <label className="form-label text-xs font-semibold">FULL NAME *</label>
                  <input
                    type="text"
                    required
                    maxLength={120}
                    className="form-input"
                    placeholder="Full Name"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  />
                </div>

                <div className="form-group mb-3">
                  <label className="form-label text-xs font-semibold">EMAIL</label>
                  <input
                    type="email"
                    maxLength={160}
                    className="form-input"
                    placeholder="user@college.edu (optional)"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  />
                </div>

                <div className="form-group mb-3">
                  <label className="form-label text-xs font-semibold">ROLE *</label>
                  <select
                    className="form-input"
                    value={formData.role}
                    onChange={(e) => setFormData({ ...formData, role: e.target.value })}
                  >
                    {ROLES.map((r) => (
                      <option key={r} value={r}>{r}</option>
                    ))}
                  </select>
                </div>

                <div className="d-flex gap-3 mb-4">
                  <div className="form-group flex-1" style={{ margin: 0 }}>
                    <label className="form-label text-xs font-semibold">BRANCH</label>
                    <input
                      type="text"
                      maxLength={20}
                      className="form-input"
                      placeholder="e.g. CS"
                      value={formData.branch}
                      onChange={(e) => setFormData({ ...formData, branch: e.target.value })}
                    />
                  </div>
                  <div className="form-group" style={{ margin: 0, width: '110px' }}>
                    <label className="form-label text-xs font-semibold">YEAR</label>
                    <select
                      className="form-input"
                      value={formData.studyYear}
                      onChange={(e) => setFormData({ ...formData, studyYear: e.target.value })}
                    >
                      <option value="">—</option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                    </select>
                  </div>
                  <div className="form-group" style={{ margin: 0, width: '90px' }}>
                    <label className="form-label text-xs font-semibold">SECTION</label>
                    <input
                      type="text"
                      maxLength={5}
                      className="form-input"
                      placeholder="e.g. A"
                      value={formData.section}
                      onChange={(e) => setFormData({ ...formData, section: e.target.value })}
                    />
                  </div>
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
                    {submitting ? 'Creating...' : 'Create User'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* EDIT USER MODAL */}
        {showEditModal && targetUser && (
          <div className="modal-backdrop d-flex align-center justify-center" role="dialog" aria-modal="true">
            <div className="card modal-content" style={{ width: '100%', maxWidth: '520px', padding: 'var(--space-5)' }}>
              <div className="d-flex justify-between align-center mb-3">
                <h3 style={{ margin: 0, fontSize: '1.25rem' }}>
                  Edit User: <span className="font-mono">{targetUser.collegeId}</span>
                </h3>
                <button
                  type="button"
                  className="btn btn-outline btn-sm text-xs"
                  onClick={() => setShowEditModal(false)}
                >
                  ✕
                </button>
              </div>

              {formError && (
                <div className="alert alert-danger mb-3 text-sm">
                  ⚠️ {formError}
                </div>
              )}

              <form onSubmit={handleEditUser}>
                <div className="form-group mb-3">
                  <label className="form-label text-xs font-semibold">FULL NAME *</label>
                  <input
                    type="text"
                    required
                    maxLength={120}
                    className="form-input"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  />
                </div>

                <div className="form-group mb-3">
                  <label className="form-label text-xs font-semibold">EMAIL</label>
                  <input
                    type="email"
                    maxLength={160}
                    className="form-input"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  />
                </div>

                <div className="d-flex gap-3 mb-3">
                  <div className="form-group flex-1" style={{ margin: 0 }}>
                    <label className="form-label text-xs font-semibold">BRANCH</label>
                    <input
                      type="text"
                      maxLength={20}
                      className="form-input"
                      value={formData.branch}
                      onChange={(e) => setFormData({ ...formData, branch: e.target.value })}
                    />
                  </div>
                  <div className="form-group" style={{ margin: 0, width: '110px' }}>
                    <label className="form-label text-xs font-semibold">YEAR</label>
                    <select
                      className="form-input"
                      value={formData.studyYear}
                      onChange={(e) => setFormData({ ...formData, studyYear: e.target.value })}
                    >
                      <option value="">—</option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                    </select>
                  </div>
                  <div className="form-group" style={{ margin: 0, width: '90px' }}>
                    <label className="form-label text-xs font-semibold">SECTION</label>
                    <input
                      type="text"
                      maxLength={5}
                      className="form-input"
                      value={formData.section}
                      onChange={(e) => setFormData({ ...formData, section: e.target.value })}
                    />
                  </div>
                </div>

                <div className="form-group mb-4">
                  <label className="d-flex align-center gap-2" style={{ cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={formData.isActive}
                      onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                    />
                    <span className="text-sm font-semibold">Active User Account</span>
                  </label>
                  <p className="text-xs text-muted mt-1">
                    Inactive users are immediately blocked from logging in to SkillSense.
                  </p>
                </div>

                <div className="d-flex justify-end gap-2 pt-2 border-top">
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => setShowEditModal(false)}
                    disabled={submitting}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary btn-sm"
                    disabled={submitting}
                  >
                    {submitting ? 'Saving...' : 'Save Changes'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* DEACTIVATE CONFIRMATION MODAL */}
        {showDeactivateModal && targetUser && (
          <div className="modal-backdrop d-flex align-center justify-center" role="dialog" aria-modal="true">
            <div className="card modal-content" style={{ width: '100%', maxWidth: '440px', padding: 'var(--space-5)' }}>
              <h3 style={{ margin: 0, marginBottom: '8px', fontSize: '1.25rem' }}>
                Deactivate User Account?
              </h3>
              <p className="text-sm text-muted mb-4">
                Are you sure you want to deactivate <strong className="font-mono">{targetUser.collegeId}</strong> ({targetUser.name})?
                The user will be immediately blocked from signing in.
              </p>
              <div className="d-flex justify-end gap-2">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setShowDeactivateModal(false)}
                  disabled={submitting}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  className="btn btn-warning btn-sm"
                  onClick={handleDeactivate}
                  disabled={submitting}
                >
                  {submitting ? 'Deactivating...' : 'Yes, Deactivate'}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* RESET PASSWORD CONFIRMATION MODAL */}
        {showResetModal && targetUser && (
          <div className="modal-backdrop d-flex align-center justify-center" role="dialog" aria-modal="true">
            <div className="card modal-content" style={{ width: '100%', maxWidth: '440px', padding: 'var(--space-5)' }}>
              <h3 style={{ margin: 0, marginBottom: '8px', fontSize: '1.25rem' }}>
                Reset User Password?
              </h3>
              <p className="text-sm text-muted mb-4">
                This will generate a new temporary password for <strong className="font-mono">{targetUser.collegeId}</strong>.
                The user will be required to change it on their next login.
              </p>
              <div className="d-flex justify-end gap-2">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setShowResetModal(false)}
                  disabled={submitting}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={handleResetPassword}
                  disabled={submitting}
                >
                  {submitting ? 'Resetting...' : 'Generate New Password'}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* CREDENTIALS DISPLAY MODAL */}
        {showCredsModal && targetUser && (
          <div className="modal-backdrop d-flex align-center justify-center" role="dialog" aria-modal="true">
            <div className="card modal-content" style={{ width: '100%', maxWidth: '480px', padding: 'var(--space-5)' }}>
              <h3 style={{ margin: 0, marginBottom: '8px', fontSize: '1.25rem' }}>
                🔑 Credentials Generated
              </h3>
              <p className="text-sm text-muted mb-3">
                Securely copy the temporary password below. For security, it will not be displayed again.
              </p>
              <div className="card p-3 mb-4" style={{ background: 'var(--bg-subtle)' }}>
                <div className="d-flex justify-between align-center mb-1">
                  <span className="text-xs text-muted">College ID</span>
                  <span className="font-mono font-semibold">{targetUser.collegeId}</span>
                </div>
                <div className="d-flex justify-between align-center">
                  <span className="text-xs text-muted">Temporary Password</span>
                  <span className="font-mono font-semibold" style={{ color: 'var(--primary)', userSelect: 'all' }}>
                    {tempPassword}
                  </span>
                </div>
              </div>
              <div className="d-flex justify-end gap-2">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => {
                    navigator.clipboard.writeText(tempPassword);
                    setActionSuccess('Password copied to clipboard.');
                  }}
                >
                  📋 Copy Password
                </button>
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={() => setShowCredsModal(false)}
                >
                  Done
                </button>
              </div>
            </div>
          </div>
        )}

        {/* CSV IMPORT MODAL */}
        {showImportModal && (
          <div className="modal-backdrop d-flex align-center justify-center" role="dialog" aria-modal="true">
            <div className="card modal-content" style={{ width: '100%', maxWidth: '580px', padding: 'var(--space-5)' }}>
              <div className="d-flex justify-between align-center mb-3">
                <h3 style={{ margin: 0, fontSize: '1.25rem' }}>Batch Import Users via CSV</h3>
                <button
                  type="button"
                  className="btn btn-outline btn-sm text-xs"
                  onClick={() => setShowImportModal(false)}
                >
                  ✕
                </button>
              </div>

              {!importResult ? (
                <form onSubmit={handleImportCsv}>
                  <p className="text-sm text-muted mb-3">
                    Upload a CSV file containing user records. The CSV must include the following column header:
                  </p>
                  <pre
                    className="p-2 mb-3 code-font text-xs"
                    style={{ background: 'var(--bg-subtle)', borderRadius: 'var(--radius-sm)', overflowX: 'auto' }}
                  >
                    college_id,name,email,role,branch,study_year,section
                  </pre>

                  {importError && (
                    <div className="alert alert-danger mb-3 text-sm">
                      ⚠️ {importError}
                    </div>
                  )}

                  <div className="form-group mb-4">
                    <label htmlFor="csv-file-input" className="form-label text-xs font-semibold">SELECT CSV FILE</label>
                    <input
                      id="csv-file-input"
                      type="file"
                      accept=".csv,text/csv"
                      className="form-input"
                      onChange={(e) => setCsvFile(e.target.files[0] || null)}
                    />
                  </div>

                  <div className="d-flex justify-end gap-2 pt-2 border-top">
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => setShowImportModal(false)}
                      disabled={importing}
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="btn btn-primary btn-sm"
                      disabled={importing || !csvFile}
                    >
                      {importing ? 'Processing CSV...' : 'Start Import'}
                    </button>
                  </div>
                </form>
              ) : (
                <div>
                  <div className="card p-3 mb-3" style={{ background: 'var(--bg-subtle)' }}>
                    <h4 style={{ margin: '0 0 8px 0', fontSize: '0.95rem' }}>Import Summary</h4>
                    <div className="d-flex gap-4">
                      <div>
                        <span className="text-xs text-muted d-block">Created</span>
                        <strong className="text-lg" style={{ color: 'var(--status-ready-text)' }}>
                          {importResult.created}
                        </strong>
                      </div>
                      <div>
                        <span className="text-xs text-muted d-block">Skipped</span>
                        <strong className="text-lg text-muted">
                          {importResult.skipped}
                        </strong>
                      </div>
                      <div>
                        <span className="text-xs text-muted d-block">Errors</span>
                        <strong className="text-lg" style={{ color: importResult.errors?.length ? 'var(--status-needs-support-text)' : 'inherit' }}>
                          {importResult.errors?.length || 0}
                        </strong>
                      </div>
                    </div>
                  </div>

                  {/* Row Errors if any */}
                  {importResult.errors && importResult.errors.length > 0 && (
                    <div className="mb-3" style={{ maxHeight: '160px', overflowY: 'auto' }}>
                      <span className="text-xs font-semibold text-muted d-block mb-1">Row Errors:</span>
                      <table style={{ width: '100%', fontSize: '0.8rem', borderCollapse: 'collapse' }}>
                        <tbody>
                          {importResult.errors.map((err, idx) => (
                            <tr key={idx} style={{ borderBottom: '1px solid var(--border-color)' }}>
                              <td style={{ padding: '4px 8px', fontWeight: 600, width: '70px' }}>Row {err.row}</td>
                              <td style={{ padding: '4px 8px', color: 'var(--status-needs-support-text)' }}>{err.message}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}

                  {/* Credentials Download */}
                  {importResult.credentials && (
                    <div className="alert alert-info mb-3 d-flex justify-between align-center">
                      <div>
                        <span className="font-semibold text-xs d-block">Temporary Passwords Generated</span>
                        <span className="text-xs">Download credentials CSV for newly created users.</span>
                      </div>
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={handleDownloadCredentials}
                      >
                        📥 Download Credentials (.csv)
                      </button>
                    </div>
                  )}

                  <div className="d-flex justify-end pt-2 border-top">
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      onClick={() => setShowImportModal(false)}
                    >
                      Close
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </main>
    </>
  );
}
