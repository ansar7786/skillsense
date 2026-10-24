import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import QuestionEditor from '../components/QuestionEditor';
import SkillTargetsPanel from '../components/SkillTargetsPanel';
import TargetingPanel from '../components/TargetingPanel';
import SurveyPreviewModal from '../components/SurveyPreviewModal';
import ActivationConfirmDialog from '../components/ActivationConfirmDialog';
import { useAuth } from '../auth/AuthContext';
import { validateSurvey } from '../utils/surveyValidator';
import {
  fetchSurvey,
  createSurvey,
  updateSurvey,
  activateSurvey,
  fetchPreset,
  fetchTerms
} from '../api/surveys';

const CATEGORIES = [
  'PLACEMENT',
  'ACADEMIC',
  'FACILITY',
  'EVENT',
  'WELLBEING',
  'CUSTOM'
];

export default function SurveyBuilderPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const isEditing = Boolean(id);

  // Survey State
  const [surveyId, setSurveyId] = useState(id ? Number(id) : null);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('PLACEMENT');
  const [termId, setTermId] = useState('');
  const [isAnonymous, setIsAnonymous] = useState(false);
  const [status, setStatus] = useState('DRAFT');
  const [version, setVersion] = useState(1);
  const [opensAt, setOpensAt] = useState('');
  const [closesAt, setClosesAt] = useState('');
  const [questions, setQuestions] = useState([]);
  const [skillTargets, setSkillTargets] = useState({});
  const [targetFilter, setTargetFilter] = useState({
    scope: user?.role === 'HOD' || user?.role === 'FACULTY' ? 'FILTER' : 'ALL',
    branches: user?.branch ? [user.branch] : [],
    years: [],
    sections: []
  });

  // UI state
  const [selectedQuestionIndex, setSelectedQuestionIndex] = useState(0);
  const [terms, setTerms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState(null);
  const [validationErrors, setValidationErrors] = useState({});
  const [isDirty, setIsDirty] = useState(false);

  // Autosave state: 'idle' | 'saving' | 'saved' | 'error'
  const [autosaveState, setAutosaveState] = useState('saved');
  const isSavingRef = useRef(false);

  // Modals state
  const [showPreview, setShowPreview] = useState(false);
  const [showActivationDialog, setShowActivationDialog] = useState(false);
  const [isActivating, setIsActivating] = useState(false);
  const [activationSuccessData, setActivationSuccessData] = useState(null);

  // Preset confirmation modal
  const [presetConfirmCategory, setPresetConfirmCategory] = useState(null);

  // Load terms and survey data on mount
  useEffect(() => {
    let mounted = true;

    async function init() {
      setLoading(true);
      setPageError(null);
      try {
        const termsRes = await fetchTerms();
        if (mounted) {
          setTerms(termsRes || []);
          const currentTerm = termsRes?.find((t) => t.isCurrent) || termsRes?.[0];
          if (currentTerm && !termId) {
            setTermId(currentTerm.termId);
          }
        }

        if (id) {
          const s = await fetchSurvey(id);
          if (mounted) {
            setSurveyId(s.surveyId);
            setTitle(s.title || '');
            setDescription(s.description || '');
            setCategory(s.category || 'PLACEMENT');
            if (s.termId) setTermId(s.termId);
            setIsAnonymous(s.category === 'WELLBEING' ? true : Boolean(s.isAnonymous));
            setStatus(s.status || 'DRAFT');
            setVersion(s.version || 1);
            setOpensAt(s.opensAt ? s.opensAt.slice(0, 16) : '');
            setClosesAt(s.closesAt ? s.closesAt.slice(0, 16) : '');

            const schema = s.questionSchema;
            if (schema && schema.questions) {
              setQuestions(schema.questions);
            }
            if (s.skillTargets) {
              setSkillTargets(s.skillTargets);
            }
            if (s.targetFilter) {
              setTargetFilter(s.targetFilter);
            }
          }
        } else {
          // Initialize new survey with PLACEMENT default questions
          try {
            const preset = await fetchPreset('PLACEMENT');
            if (mounted && preset?.questions) {
              setQuestions(preset.questions);
              setTitle('Campus Placement Readiness Survey');
            }
          } catch {
            // Fallback default question if preset call fails
            if (mounted) {
              setQuestions([
                {
                  key: 'primary_rating',
                  type: 'RATING',
                  prompt: 'How satisfied are you with overall course delivery?',
                  required: true,
                  rating: { min: 1, max: 5, minLabel: 'Poor', maxLabel: 'Excellent' }
                }
              ]);
            }
          }
        }
      } catch (err) {
        if (mounted) {
          setPageError(err.response?.data?.message || 'Failed to load survey data.');
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    }

    init();

    return () => {
      mounted = false;
    };
  }, [id]);

  // WELLBEING Category Enforcer Rule
  useEffect(() => {
    if (category === 'WELLBEING') {
      setIsAnonymous(true);
    }
  }, [category]);

  // Construct current survey payload
  const currentSurveyData = useMemo(() => {
    return {
      surveyId,
      title,
      description,
      category,
      termId: termId ? Number(termId) : null,
      isAnonymous: category === 'WELLBEING' ? true : isAnonymous,
      opensAt: opensAt ? `${opensAt}:00` : null,
      closesAt: closesAt ? `${closesAt}:00` : null,
      questionSchema: {
        schemaVersion: 1,
        questions
      },
      skillTargets,
      targetFilter
    };
  }, [
    surveyId,
    title,
    description,
    category,
    termId,
    isAnonymous,
    opensAt,
    closesAt,
    questions,
    skillTargets,
    targetFilter
  ]);

  // Save function (can be called manually or by autosave)
  const saveSurvey = async (isAutosave = false) => {
    if (status !== 'DRAFT') return;
    if (isSavingRef.current) return;

    // For autosave, do not try if basic required fields are empty
    if (isAutosave && (!title.trim() || !termId)) return;

    isSavingRef.current = true;
    setAutosaveState('saving');

    try {
      const payload = {
        title: title.trim() || 'Untitled Draft',
        description,
        category,
        termId: Number(termId),
        questionSchema: {
          schemaVersion: 1,
          questions
        },
        skillTargets,
        isAnonymous: category === 'WELLBEING' ? true : isAnonymous,
        targetFilter,
        opensAt: opensAt ? `${opensAt}:00` : null,
        closesAt: closesAt ? `${closesAt}:00` : null
      };

      let result;
      if (surveyId) {
        result = await updateSurvey(surveyId, payload);
      } else {
        result = await createSurvey(payload);
        setSurveyId(result.surveyId);
        // Replace URL quietly without reloading
        window.history.replaceState(null, '', `/officials/surveys/${result.surveyId}/edit`);
      }

      setAutosaveState('saved');
      setIsDirty(false);
      return result;
    } catch (err) {
      setAutosaveState('error');
      if (!isAutosave) {
        alert(err.response?.data?.message || 'Failed to save survey');
      }
    } finally {
      isSavingRef.current = false;
    }
  };

  // Autosave interval every 10 seconds while in DRAFT
  useEffect(() => {
    if (status !== 'DRAFT') return;

    const interval = setInterval(() => {
      if (isDirty && !isSavingRef.current && surveyId) {
        saveSurvey(true);
      }
    }, 10000);

    return () => clearInterval(interval);
  }, [isDirty, status, surveyId, currentSurveyData]);

  // Mark dirty on changes
  const markDirty = () => {
    setIsDirty(true);
    setAutosaveState('saving');
  };

  // Category change & Preset Loading
  const handleCategorySelect = async (newCat) => {
    if (newCat === category) return;

    if (questions.length > 0) {
      setPresetConfirmCategory(newCat);
    } else {
      setCategory(newCat);
      markDirty();
      loadPresetQuestions(newCat);
    }
  };

  const loadPresetQuestions = async (cat) => {
    try {
      const preset = await fetchPreset(cat);
      if (preset && preset.questions) {
        setQuestions(preset.questions);
        setSelectedQuestionIndex(0);
        markDirty();
      }
    } catch (err) {
      console.warn(`Could not load preset for ${cat}`, err);
    }
  };

  const confirmPresetLoad = (cat) => {
    setCategory(cat);
    setPresetConfirmCategory(null);
    markDirty();
    loadPresetQuestions(cat);
  };

  // Question manipulation handlers
  const handleAddQuestion = () => {
    const newIdx = questions.length + 1;
    const newKey = `q_${newIdx}_${Date.now().toString(36).slice(-4)}`;
    const newQuestion = {
      key: newKey,
      type: 'SINGLE_CHOICE',
      prompt: `New Question ${newIdx}`,
      required: true,
      options: [
        { value: 'opt_1', label: 'Option 1' },
        { value: 'opt_2', label: 'Option 2' }
      ]
    };
    setQuestions([...questions, newQuestion]);
    setSelectedQuestionIndex(questions.length);
    markDirty();
  };

  const handleDuplicateQuestion = (idx, e) => {
    e?.stopPropagation();
    const target = questions[idx];
    const dupKey = `${target.key}_copy_${Date.now().toString(36).slice(-3)}`;
    const duplicated = {
      ...JSON.parse(JSON.stringify(target)),
      key: dupKey.slice(0, 40),
      prompt: `${target.prompt} (Copy)`
    };
    const nextQuestions = [...questions];
    nextQuestions.splice(idx + 1, 0, duplicated);
    setQuestions(nextQuestions);
    setSelectedQuestionIndex(idx + 1);
    markDirty();
  };

  const handleDeleteQuestion = (idx, e) => {
    e?.stopPropagation();
    if (questions.length <= 1) {
      alert('A survey must contain at least 1 question.');
      return;
    }
    const nextQuestions = questions.filter((_, i) => i !== idx);
    setQuestions(nextQuestions);
    setSelectedQuestionIndex(Math.max(0, idx - 1));
    markDirty();
  };

  const handleMoveQuestion = (idx, direction, e) => {
    e?.stopPropagation();
    const targetIdx = direction === 'UP' ? idx - 1 : idx + 1;
    if (targetIdx < 0 || targetIdx >= questions.length) return;

    const nextQuestions = [...questions];
    const [moved] = nextQuestions.splice(idx, 1);
    nextQuestions.splice(targetIdx, 0, moved);
    setQuestions(nextQuestions);
    setSelectedQuestionIndex(targetIdx);
    markDirty();
  };

  const handleUpdateQuestion = (updatedQ) => {
    const next = [...questions];
    next[selectedQuestionIndex] = updatedQ;
    setQuestions(next);
    markDirty();
  };

  // Pre-activation validation & trigger
  const handleInitiateActivation = () => {
    const validation = validateSurvey(currentSurveyData);
    if (!validation.isValid) {
      setValidationErrors(validation.errors);

      // Focus / navigate to the first error
      if (validation.firstErrorField) {
        const match = validation.firstErrorField.match(/question_(\d+)/);
        if (match) {
          const errIdx = parseInt(match[1], 10);
          setSelectedQuestionIndex(errIdx);
          setTimeout(() => {
            const el = document.getElementById(`question-editor-${errIdx}`);
            el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
          }, 100);
        } else {
          window.scrollTo({ top: 0, behavior: 'smooth' });
        }
      }
      return;
    }

    setValidationErrors({});
    setShowActivationDialog(true);
  };

  // Confirm activation
  const handleConfirmActivation = async () => {
    setIsActivating(true);
    try {
      // First ensure draft is saved
      let activeSurveyId = surveyId;
      if (isDirty || !activeSurveyId) {
        const saved = await saveSurvey(false);
        if (saved) activeSurveyId = saved.surveyId;
      }

      if (!activeSurveyId) {
        throw new Error('Unable to save survey draft before activation');
      }

      const activatedRes = await activateSurvey(activeSurveyId);
      setStatus('ACTIVE');
      setShowActivationDialog(false);
      setActivationSuccessData(activatedRes);
    } catch (err) {
      alert(err.response?.data?.message || err.message || 'Activation failed');
    } finally {
      setIsActivating(false);
    }
  };

  if (loading) {
    return (
      <>
        <Navbar />
        <div className="container loading-container py-12" role="status">
          <div className="spinner"></div>
          <p>Loading Survey Builder...</p>
        </div>
      </>
    );
  }

  if (pageError) {
    return (
      <>
        <Navbar />
        <div className="container py-8">
          <div className="card text-center py-6">
            <h3 className="text-danger mb-2">Error Loading Survey</h3>
            <p className="text-muted mb-4">{pageError}</p>
            <Link to="/officials" className="btn btn-primary">
              Return to Surveys
            </Link>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <Navbar />

      {/* Builder Top Bar */}
      <header className="builder-top-bar">
        <div className="builder-top-bar-inner">
          {/* Left Title / Breadcrumb */}
          <div className="builder-title-group">
            <Link to="/officials" className="back-link" title="Return to Survey List">
              ← Surveys
            </Link>
            <span className="divider-slash" aria-hidden="true">/</span>
            <input
              type="text"
              className={`builder-title-input ${validationErrors.title ? 'input-error' : ''}`}
              value={title}
              disabled={status !== 'DRAFT'}
              onChange={(e) => {
                setTitle(e.target.value);
                markDirty();
              }}
              placeholder="Survey Title..."
              aria-label="Survey Title"
            />
            <span className={`status-badge status-${status.toLowerCase()}`}>
              {status}
            </span>
          </div>

          {/* Right Action Controls */}
          <div className="builder-actions-group">
            {/* Autosave Status */}
            {status === 'DRAFT' && (
              <span className="autosave-indicator" aria-live="polite">
                {autosaveState === 'saving' && 'Saving...'}
                {autosaveState === 'saved' && 'Saved'}
                {autosaveState === 'error' && <span className="text-danger">Unable to save</span>}
              </span>
            )}

            {/* Preview Button */}
            <button
              type="button"
              className="btn btn-secondary tap-target"
              onClick={() => setShowPreview(true)}
            >
              Preview
            </button>

            {/* Save Draft Button */}
            {status === 'DRAFT' && (
              <button
                type="button"
                className="btn btn-secondary tap-target"
                onClick={() => saveSurvey(false)}
                disabled={isSavingRef.current}
              >
                Save Draft
              </button>
            )}

            {/* Activate Button */}
            {status === 'DRAFT' && (
              <button
                type="button"
                className="btn btn-primary tap-target btn-activate"
                onClick={handleInitiateActivation}
              >
                Activate Survey
              </button>
            )}
          </div>
        </div>
      </header>

      {/* Main Workspace Layout */}
      <main className="builder-main-workspace container">
        {/* Activation Success Banner */}
        {activationSuccessData && (
          <div className="alert alert-success mb-6 p-4">
            <div className="d-flex justify-between align-center">
              <div>
                <h3 className="font-semibold text-lg">🎉 Survey Activated Successfully!</h3>
                <p className="mt-1 text-sm">
                  This survey is now <strong>ACTIVE</strong> and instantly accessible to{' '}
                  <strong>{activationSuccessData.eligibleCount || 0} eligible students</strong>.
                </p>
              </div>
              <div className="d-flex gap-2">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setShowPreview(true)}
                >
                  View Preview
                </button>
                <Link to="/officials" className="btn btn-primary btn-sm">
                  Back to Surveys List
                </Link>
              </div>
            </div>
          </div>
        )}

        {/* Global Validation Error Banner */}
        {Object.keys(validationErrors).length > 0 && (
          <div className="alert alert-danger mb-4 p-3" role="alert">
            <h4 className="font-medium text-sm mb-1">Please correct the following errors before activation:</h4>
            <ul className="text-xs list-disc pl-4">
              {Object.entries(validationErrors).slice(0, 5).map(([key, msg]) => (
                <li key={key}>{msg}</li>
              ))}
              {Object.keys(validationErrors).length > 5 && (
                <li>...and {Object.keys(validationErrors).length - 5} more issues</li>
              )}
            </ul>
          </div>
        )}

        <div className="builder-grid">
          {/* Left Column: Questions List & Question Editor */}
          <section className="builder-left-col" aria-label="Survey Questions and Editor">
            {/* Survey Overview Metadata Card */}
            <div className="card builder-meta-card mb-4">
              <div className="form-group mb-3">
                <label className="field-label">Survey Title <span className="text-danger">*</span></label>
                <input
                  type="text"
                  className={`form-input ${validationErrors.title ? 'input-error' : ''}`}
                  value={title}
                  disabled={status !== 'DRAFT'}
                  onChange={(e) => {
                    setTitle(e.target.value);
                    markDirty();
                  }}
                  placeholder="e.g. Placement Readiness Assessment"
                  maxLength={200}
                />
                {validationErrors.title && (
                  <span className="field-error">{validationErrors.title}</span>
                )}
              </div>

              <div className="form-group mb-0">
                <label className="field-label">Description (Optional guidance for students)</label>
                <textarea
                  rows={2}
                  className="form-textarea"
                  value={description}
                  disabled={status !== 'DRAFT'}
                  onChange={(e) => {
                    setDescription(e.target.value);
                    markDirty();
                  }}
                  placeholder="Briefly explain the purpose of this survey..."
                />
              </div>
            </div>

            {/* Questions Toolbar & Navigation */}
            <div className="d-flex justify-between align-center mb-3">
              <div className="d-flex align-center gap-2">
                <h3 className="section-title">Questions ({questions.length})</h3>
                {validationErrors.questions && (
                  <span className="field-error text-xs">{validationErrors.questions}</span>
                )}
              </div>
              {status === 'DRAFT' && (
                <button
                  type="button"
                  className="btn btn-primary btn-sm tap-target"
                  onClick={handleAddQuestion}
                  disabled={questions.length >= 40}
                >
                  + Add Question
                </button>
              )}
            </div>

            {/* Questions Compact Scroller/List */}
            <div className="questions-nav-list mb-4">
              {questions.map((q, idx) => {
                const isSelected = idx === selectedQuestionIndex;
                const hasSkill = Boolean(q.scoring?.skill);
                const isInvalid = Boolean(
                  validationErrors[`question_${idx}_key`] ||
                  validationErrors[`question_${idx}_prompt`] ||
                  validationErrors[`question_${idx}_options`] ||
                  validationErrors[`question_${idx}_rating`]
                );

                return (
                  <div
                    key={q.key || idx}
                    className={`question-nav-item ${isSelected ? 'item-selected' : ''} ${isInvalid ? 'item-has-error' : ''}`}
                    onClick={() => setSelectedQuestionIndex(idx)}
                    role="button"
                    tabIndex={0}
                    aria-label={`Question ${idx + 1}: ${q.prompt || 'Untitled'}`}
                  >
                    <div className="d-flex align-center gap-2">
                      <span className="nav-q-num">Q{idx + 1}</span>
                      <span className="nav-q-type-badge">{q.type}</span>
                      {hasSkill && <span className="nav-skill-badge">{q.scoring.skill}</span>}
                      {q.required !== false && <span className="nav-req-dot" title="Required">*</span>}
                    </div>

                    <div className="nav-q-prompt truncate-text text-sm">
                      {q.prompt || <span className="text-muted italic">Empty prompt...</span>}
                    </div>

                    {status === 'DRAFT' && (
                      <div className="nav-q-actions">
                        <button
                          type="button"
                          className="btn-icon"
                          title="Move Up"
                          disabled={idx === 0}
                          onClick={(e) => handleMoveQuestion(idx, 'UP', e)}
                          aria-label={`Move question ${idx + 1} up`}
                        >
                          ↑
                        </button>
                        <button
                          type="button"
                          className="btn-icon"
                          title="Move Down"
                          disabled={idx === questions.length - 1}
                          onClick={(e) => handleMoveQuestion(idx, 'DOWN', e)}
                          aria-label={`Move question ${idx + 1} down`}
                        >
                          ↓
                        </button>
                        <button
                          type="button"
                          className="btn-icon"
                          title="Duplicate"
                          onClick={(e) => handleDuplicateQuestion(idx, e)}
                          aria-label={`Duplicate question ${idx + 1}`}
                        >
                          ⧉
                        </button>
                        <button
                          type="button"
                          className="btn-icon-danger"
                          title="Delete"
                          onClick={(e) => handleDeleteQuestion(idx, e)}
                          aria-label={`Delete question ${idx + 1}`}
                        >
                          ✕
                        </button>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            {/* Selected Question Editor */}
            <QuestionEditor
              question={questions[selectedQuestionIndex]}
              index={selectedQuestionIndex}
              onChange={handleUpdateQuestion}
              errors={validationErrors}
            />
          </section>

          {/* Right Column: Settings, Targeting, Scoring & Anonymity */}
          <aside className="builder-right-col" aria-label="Survey Configuration and Settings">
            {/* Category & Term Panel */}
            <div className="card sub-panel mb-4">
              <h4 className="sub-panel-title mb-3">Classification & Academic Term</h4>

              {/* Category */}
              <div className="form-group mb-3">
                <label htmlFor="builder-category" className="field-label">
                  Category <span className="text-danger">*</span>
                </label>
                <select
                  id="builder-category"
                  className="form-select"
                  value={category}
                  disabled={status !== 'DRAFT'}
                  onChange={(e) => handleCategorySelect(e.target.value)}
                >
                  {CATEGORIES.map((cat) => (
                    <option key={cat} value={cat}>
                      {cat}
                    </option>
                  ))}
                </select>
                <small className="field-hint">
                  Choosing a category can populate standard starter questions.
                </small>
              </div>

              {/* Academic Term */}
              <div className="form-group mb-0">
                <label htmlFor="builder-term" className="field-label">
                  Academic Term <span className="text-danger">*</span>
                </label>
                <select
                  id="builder-term"
                  className={`form-select ${validationErrors.termId ? 'input-error' : ''}`}
                  value={termId}
                  disabled={status !== 'DRAFT'}
                  onChange={(e) => {
                    setTermId(e.target.value);
                    markDirty();
                  }}
                >
                  <option value="">Select an Academic Term...</option>
                  {terms.map((t) => (
                    <option key={t.termId} value={t.termId}>
                      {t.label} {t.isCurrent ? '(Current)' : ''}
                    </option>
                  ))}
                </select>
                {validationErrors.termId && (
                  <span className="field-error">{validationErrors.termId}</span>
                )}
              </div>
            </div>

            {/* Anonymity Settings Panel */}
            <div className="card sub-panel mb-4">
              <div className="d-flex justify-between align-center mb-2">
                <h4 className="sub-panel-title">Anonymity & Privacy</h4>
                <span className="badge badge-subtle">
                  {isAnonymous ? 'Anonymous' : 'Identified'}
                </span>
              </div>

              <div className="anonymity-switch-wrapper mb-2">
                <label className="checkbox-label bold-label">
                  <input
                    type="checkbox"
                    checked={isAnonymous}
                    disabled={category === 'WELLBEING' || status !== 'DRAFT'}
                    onChange={(e) => {
                      if (category === 'WELLBEING') return;
                      setIsAnonymous(e.target.checked);
                      markDirty();
                    }}
                  />
                  <span>Strictly Anonymous Survey</span>
                </label>
              </div>

              {category === 'WELLBEING' ? (
                <div className="alert alert-warning py-2 px-3 text-xs mt-2">
                  <strong>Locked:</strong> Wellbeing surveys are strictly anonymous to protect student psychological safety and privacy. This setting cannot be altered.
                </div>
              ) : (
                <p className="field-hint text-xs mt-1">
                  {isAnonymous
                    ? 'Anonymous: student names and IDs are NEVER stored with their answers.'
                    : 'Not anonymous: student names and college IDs are recorded with answers.'}
                </p>
              )}
            </div>

            {/* Targeting Panel */}
            <div className="card mb-4">
              <TargetingPanel
                targetFilter={targetFilter}
                user={user}
                onChange={(nextFilter) => {
                  setTargetFilter(nextFilter);
                  markDirty();
                }}
              />
            </div>

            {/* Schedule & Closing Date */}
            <div className="card sub-panel mb-4">
              <h4 className="sub-panel-title mb-3">Schedule & Availability</h4>
              <div className="form-group mb-3">
                <label htmlFor="builder-opens" className="field-label">Opening Time (Optional)</label>
                <input
                  id="builder-opens"
                  type="datetime-local"
                  className="form-input"
                  value={opensAt}
                  disabled={status !== 'DRAFT'}
                  onChange={(e) => {
                    setOpensAt(e.target.value);
                    markDirty();
                  }}
                />
              </div>

              <div className="form-group mb-0">
                <label htmlFor="builder-closes" className="field-label">Closing Time (Optional)</label>
                <input
                  id="builder-closes"
                  type="datetime-local"
                  className={`form-input ${validationErrors.closesAt ? 'input-error' : ''}`}
                  value={closesAt}
                  onChange={(e) => {
                    setClosesAt(e.target.value);
                    markDirty();
                  }}
                />
                {validationErrors.closesAt && (
                  <span className="field-error">{validationErrors.closesAt}</span>
                )}
              </div>
            </div>

            {/* Competency Skill Targets Panel */}
            <div className="card mb-4">
              <SkillTargetsPanel
                questions={questions}
                skillTargets={skillTargets}
                onChange={(nextTargets) => {
                  setSkillTargets(nextTargets);
                  markDirty();
                }}
              />
            </div>
          </aside>
        </div>
      </main>

      {/* Student Safe Preview Modal */}
      <SurveyPreviewModal
        survey={currentSurveyData}
        isOpen={showPreview}
        onClose={() => setShowPreview(false)}
      />

      {/* Activation Confirmation Dialog */}
      <ActivationConfirmDialog
        isOpen={showActivationDialog}
        survey={currentSurveyData}
        isActivating={isActivating}
        onConfirm={handleConfirmActivation}
        onCancel={() => setShowActivationDialog(false)}
      />

      {/* Preset Loading Confirmation Modal */}
      {presetConfirmCategory && (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal-container">
            <div className="modal-header">
              <h3 className="modal-title">Load {presetConfirmCategory} Starter Preset?</h3>
            </div>
            <div className="modal-body">
              <p>
                You have existing questions in this survey. Loading the <strong>{presetConfirmCategory}</strong> starter preset will replace your current questions with the official preset questions.
              </p>
              <p className="text-muted text-sm mt-2">
                Do you want to proceed and replace existing questions?
              </p>
            </div>
            <div className="modal-footer">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setPresetConfirmCategory(null)}
              >
                Keep Current Questions
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => confirmPresetLoad(presetConfirmCategory)}
              >
                Load Starter Preset
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
