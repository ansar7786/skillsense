import React from 'react';

const QUESTION_TYPES = [
  { value: 'RATING', label: 'Rating Scale (1–5 / 1–10)' },
  { value: 'SINGLE_CHOICE', label: 'Single Choice (Radio)' },
  { value: 'MULTI_CHOICE', label: 'Multiple Choice (Checkboxes)' },
  { value: 'YES_NO', label: 'Yes / No' },
  { value: 'NUMBER', label: 'Number Input' },
  { value: 'TEXT', label: 'Free Text' }
];

export default function QuestionEditor({
  question,
  index,
  onChange,
  errors = {}
}) {
  if (!question) {
    return (
      <div className="card question-editor-empty">
        <p className="text-muted">Select a question from the list to edit its properties, or click <strong>+ Add Question</strong>.</p>
      </div>
    );
  }

  const qPrefix = `question_${index}`;

  const updateField = (field, value) => {
    onChange({ ...question, [field]: value });
  };

  const handleTypeChange = (e) => {
    const newType = e.target.value;
    const updated = { ...question, type: newType };

    // Set default structures for new type
    if (newType === 'RATING' && !updated.rating) {
      updated.rating = { min: 1, max: 5, minLabel: 'Beginner', maxLabel: 'Expert' };
    }
    if ((newType === 'SINGLE_CHOICE' || newType === 'MULTI_CHOICE') && (!updated.options || updated.options.length < 2)) {
      updated.options = [
        { value: 'opt_1', label: 'Option 1', score: 0 },
        { value: 'opt_2', label: 'Option 2', score: 100 }
      ];
    }
    if (newType === 'TEXT' && !updated.maxLength) {
      updated.maxLength = 500;
    }
    // Clear scoring if new type does not support it
    if (newType !== 'RATING' && newType !== 'SINGLE_CHOICE' && newType !== 'YES_NO') {
      delete updated.scoring;
    }

    onChange(updated);
  };

  // Option handlers for choice questions
  const handleOptionChange = (optIdx, field, val) => {
    const nextOptions = [...(question.options || [])];
    nextOptions[optIdx] = { ...nextOptions[optIdx], [field]: val };
    onChange({ ...question, options: nextOptions });
  };

  const handleAddOption = () => {
    const nextOptions = [...(question.options || [])];
    const newIdx = nextOptions.length + 1;
    nextOptions.push({
      value: `opt_${newIdx}`,
      label: `Option ${newIdx}`,
      score: 0
    });
    onChange({ ...question, options: nextOptions });
  };

  const handleRemoveOption = (optIdx) => {
    const nextOptions = question.options.filter((_, idx) => idx !== optIdx);
    onChange({ ...question, options: nextOptions });
  };

  // Rating handlers
  const handleRatingChange = (field, val) => {
    const rating = { ...(question.rating || { min: 1, max: 5 }), [field]: val };
    onChange({ ...question, rating });
  };

  // Scoring handlers
  const isScoringSupported = question.type === 'RATING' || question.type === 'SINGLE_CHOICE' || question.type === 'YES_NO';
  const hasScoring = Boolean(question.scoring && (question.scoring.skill || question.scoring.weight !== undefined));

  const handleToggleScoring = (e) => {
    if (e.target.checked) {
      onChange({
        ...question,
        scoring: {
          skill: question.key || 'technical_skill',
          weight: 1.0
        }
      });
    } else {
      const updated = { ...question };
      delete updated.scoring;
      onChange(updated);
    }
  };

  const handleScoringChange = (field, val) => {
    const scoring = { ...(question.scoring || {}), [field]: val };
    onChange({ ...question, scoring });
  };

  return (
    <div className="card question-editor-card" id={`question-editor-${index}`}>
      <div className="question-editor-header">
        <div className="d-flex align-center gap-2">
          <span className="question-num-badge">Q{index + 1}</span>
          <span className="editor-title">Question Properties</span>
        </div>
        <div className="d-flex align-center gap-2">
          {hasScoring ? (
            <span className="badge badge-scored">Scored</span>
          ) : (
            <span className="badge badge-unscored">Unscored</span>
          )}
        </div>
      </div>

      <div className="editor-form-grid">
        {/* Question Key */}
        <div className="form-group">
          <label htmlFor={`q-key-${index}`} className="field-label">
            Question Key <span className="text-danger">*</span>
          </label>
          <input
            id={`q-key-${index}`}
            type="text"
            className={`form-input code-font ${errors[`${qPrefix}_key`] ? 'input-error' : ''}`}
            value={question.key || ''}
            onChange={(e) => updateField('key', e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, ''))}
            placeholder="e.g. java_skill"
            maxLength={40}
          />
          <small className="field-hint">Lowercase, numbers, underscores (2–40 chars)</small>
          {errors[`${qPrefix}_key`] && (
            <span className="field-error" role="alert">{errors[`${qPrefix}_key`]}</span>
          )}
        </div>

        {/* Question Type */}
        <div className="form-group">
          <label htmlFor={`q-type-${index}`} className="field-label">
            Question Type <span className="text-danger">*</span>
          </label>
          <select
            id={`q-type-${index}`}
            className="form-select"
            value={question.type || 'RATING'}
            onChange={handleTypeChange}
          >
            {QUESTION_TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>
        </div>

        {/* Section (Optional Grouping) */}
        <div className="form-group full-width">
          <label htmlFor={`q-section-${index}`} className="field-label">
            Section / Category Group (Optional)
          </label>
          <input
            id={`q-section-${index}`}
            type="text"
            className="form-input"
            value={question.section || ''}
            onChange={(e) => updateField('section', e.target.value)}
            placeholder="e.g. Technical Skills, General Preparedness"
          />
        </div>

        {/* Prompt */}
        <div className="form-group full-width">
          <label htmlFor={`q-prompt-${index}`} className="field-label">
            Question Prompt <span className="text-danger">*</span>
          </label>
          <textarea
            id={`q-prompt-${index}`}
            rows={3}
            className={`form-textarea ${errors[`${qPrefix}_prompt`] ? 'input-error' : ''}`}
            value={question.prompt || ''}
            onChange={(e) => updateField('prompt', e.target.value)}
            placeholder="e.g. How confident are you in core Java and object-oriented programming?"
            maxLength={300}
          />
          <div className="d-flex justify-between">
            <small className="field-hint">What the student will read (3–300 chars)</small>
            <small className="text-muted tabular-nums">{(question.prompt || '').length}/300</small>
          </div>
          {errors[`${qPrefix}_prompt`] && (
            <span className="field-error" role="alert">{errors[`${qPrefix}_prompt`]}</span>
          )}
        </div>

        {/* Required Toggle */}
        <div className="form-group full-width">
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={question.required !== false}
              onChange={(e) => updateField('required', e.target.checked)}
            />
            <span>Required answer (students must respond to complete survey)</span>
          </label>
        </div>
      </div>

      {/* Type-Specific Options Area */}
      <div className="type-options-area mt-4">
        {/* RATING Configuration */}
        {question.type === 'RATING' && (
          <div className="sub-panel">
            <h4 className="sub-panel-title">Rating Scale Configuration</h4>
            <div className="editor-form-grid">
              <div className="form-group">
                <label className="field-label">Minimum Value</label>
                <input
                  type="number"
                  className="form-input tabular-nums"
                  value={question.rating?.min ?? 1}
                  min={1}
                  max={5}
                  onChange={(e) => handleRatingChange('min', parseInt(e.target.value, 10) || 1)}
                />
              </div>
              <div className="form-group">
                <label className="field-label">Maximum Value (up to 10)</label>
                <input
                  type="number"
                  className="form-input tabular-nums"
                  value={question.rating?.max ?? 5}
                  min={2}
                  max={10}
                  onChange={(e) => handleRatingChange('max', parseInt(e.target.value, 10) || 5)}
                />
              </div>
              <div className="form-group">
                <label className="field-label">Min Label</label>
                <input
                  type="text"
                  className="form-input"
                  value={question.rating?.minLabel || ''}
                  placeholder="e.g. Beginner / Strongly Disagree"
                  onChange={(e) => handleRatingChange('minLabel', e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="field-label">Max Label</label>
                <input
                  type="text"
                  className="form-input"
                  value={question.rating?.maxLabel || ''}
                  placeholder="e.g. Expert / Strongly Agree"
                  onChange={(e) => handleRatingChange('maxLabel', e.target.value)}
                />
              </div>
            </div>
            {errors[`${qPrefix}_rating`] && (
              <span className="field-error" role="alert">{errors[`${qPrefix}_rating`]}</span>
            )}
          </div>
        )}

        {/* Choice Question Options */}
        {(question.type === 'SINGLE_CHOICE' || question.type === 'MULTI_CHOICE') && (
          <div className="sub-panel">
            <div className="d-flex justify-between align-center mb-2">
              <h4 className="sub-panel-title">Options List ({question.options?.length || 0}/12)</h4>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={handleAddOption}
                disabled={(question.options?.length || 0) >= 12}
              >
                + Add Option
              </button>
            </div>

            {errors[`${qPrefix}_options`] && (
              <div className="field-error mb-2" role="alert">{errors[`${qPrefix}_options`]}</div>
            )}

            <div className="options-table">
              {(question.options || []).map((opt, optIdx) => (
                <div key={optIdx} className="option-row">
                  <div className="option-col-val">
                    <label className="field-label micro-label">Value</label>
                    <input
                      type="text"
                      className="form-input form-input-sm code-font"
                      value={opt.value || ''}
                      onChange={(e) => handleOptionChange(optIdx, 'value', e.target.value)}
                      placeholder="e.g. 0 or swe"
                    />
                  </div>
                  <div className="option-col-label">
                    <label className="field-label micro-label">Display Label</label>
                    <input
                      type="text"
                      className="form-input form-input-sm"
                      value={opt.label || ''}
                      onChange={(e) => handleOptionChange(optIdx, 'label', e.target.value)}
                      placeholder="e.g. None or SDE"
                    />
                  </div>
                  {hasScoring && question.type === 'SINGLE_CHOICE' && (
                    <div className="option-col-score">
                      <label className="field-label micro-label">Score (0–100)</label>
                      <input
                        type="number"
                        className="form-input form-input-sm tabular-nums"
                        value={opt.score ?? ''}
                        min={0}
                        max={100}
                        onChange={(e) => handleOptionChange(optIdx, 'score', e.target.value === '' ? '' : Number(e.target.value))}
                        placeholder="0–100"
                      />
                    </div>
                  )}
                  <div className="option-col-action">
                    <button
                      type="button"
                      className="btn-icon-danger"
                      title="Remove Option"
                      aria-label={`Remove option ${optIdx + 1}`}
                      disabled={(question.options?.length || 0) <= 2}
                      onClick={() => handleRemoveOption(optIdx)}
                    >
                      ✕
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* YES_NO Notice */}
        {question.type === 'YES_NO' && (
          <div className="sub-panel notice-box">
            <span className="notice-icon">ℹ️</span>
            <span className="notice-text">
              Renders as a clean <strong>Yes / No</strong> binary selector for students.
            </span>
          </div>
        )}

        {/* NUMBER Configuration */}
        {question.type === 'NUMBER' && (
          <div className="sub-panel">
            <h4 className="sub-panel-title">Numeric Limits (Optional)</h4>
            <div className="editor-form-grid">
              <div className="form-group">
                <label className="field-label">Min Allowed Value</label>
                <input
                  type="number"
                  className="form-input tabular-nums"
                  value={question.min ?? ''}
                  onChange={(e) => updateField('min', e.target.value === '' ? undefined : Number(e.target.value))}
                  placeholder="No minimum"
                />
              </div>
              <div className="form-group">
                <label className="field-label">Max Allowed Value</label>
                <input
                  type="number"
                  className="form-input tabular-nums"
                  value={question.max ?? ''}
                  onChange={(e) => updateField('max', e.target.value === '' ? undefined : Number(e.target.value))}
                  placeholder="No maximum"
                />
              </div>
            </div>
            {errors[`${qPrefix}_number`] && (
              <span className="field-error" role="alert">{errors[`${qPrefix}_number`]}</span>
            )}
          </div>
        )}

        {/* TEXT Configuration */}
        {question.type === 'TEXT' && (
          <div className="sub-panel">
            <h4 className="sub-panel-title">Text Constraints</h4>
            <div className="form-group">
              <label className="field-label">Maximum Character Length</label>
              <input
                type="number"
                className="form-input tabular-nums"
                value={question.maxLength ?? 500}
                min={10}
                max={2000}
                onChange={(e) => updateField('maxLength', parseInt(e.target.value, 10) || 500)}
              />
            </div>
            {errors[`${qPrefix}_maxLength`] && (
              <span className="field-error" role="alert">{errors[`${qPrefix}_maxLength`]}</span>
            )}
          </div>
        )}
      </div>

      {/* Scoring Box */}
      {isScoringSupported && (
        <div className={`scoring-box mt-4 ${hasScoring ? 'scoring-active' : ''}`}>
          <div className="d-flex align-center justify-between">
            <label className="checkbox-label bold-label">
              <input
                type="checkbox"
                checked={hasScoring}
                onChange={handleToggleScoring}
              />
              <span>Enable Competency Scoring</span>
            </label>
            <small className="scoring-disclaimer">
              Internal benchmark only (hidden from students)
            </small>
          </div>

          {hasScoring && (
            <div className="scoring-inputs-grid mt-3">
              <div className="form-group">
                <label htmlFor={`scoring-skill-${index}`} className="field-label">
                  Skill / Competency Key <span className="text-danger">*</span>
                </label>
                <input
                  id={`scoring-skill-${index}`}
                  type="text"
                  className={`form-input code-font ${errors[`${qPrefix}_scoring_skill`] ? 'input-error' : ''}`}
                  value={question.scoring?.skill || ''}
                  onChange={(e) => handleScoringChange('skill', e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, ''))}
                  placeholder="e.g. java, python, dsa, communication"
                />
                <small className="field-hint">Used to calculate institutional skill gaps</small>
                {errors[`${qPrefix}_scoring_skill`] && (
                  <span className="field-error" role="alert">{errors[`${qPrefix}_scoring_skill`]}</span>
                )}
              </div>

              <div className="form-group">
                <label htmlFor={`scoring-weight-${index}`} className="field-label">
                  Skill Weight (0.5 – 5.0)
                </label>
                <input
                  id={`scoring-weight-${index}`}
                  type="number"
                  step="0.5"
                  min="0.5"
                  max="5.0"
                  className={`form-input tabular-nums ${errors[`${qPrefix}_scoring_weight`] ? 'input-error' : ''}`}
                  value={question.scoring?.weight ?? 1.0}
                  onChange={(e) => handleScoringChange('weight', parseFloat(e.target.value) || 1.0)}
                />
                <small className="field-hint">Relative importance (default 1.0)</small>
                {errors[`${qPrefix}_scoring_weight`] && (
                  <span className="field-error" role="alert">{errors[`${qPrefix}_scoring_weight`]}</span>
                )}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
