import React from 'react';

export default function QuestionRenderer({ question, value, onChange, error }) {
  const { key, type, prompt, required } = question;

  const handleChange = (newVal) => {
    onChange(key, newVal);
  };

  const renderControl = () => {
    switch (type) {
      case 'SINGLE_CHOICE': {
        const options = question.options || [];
        return (
          <div className="options-group" role="radiogroup" aria-labelledby={`prompt-${key}`}>
            {options.map((opt) => {
              const isChecked = value === opt.value;
              const inputId = `q-${key}-${opt.value}`;
              return (
                <label
                  key={opt.value}
                  htmlFor={inputId}
                  className={`choice-item tap-target ${isChecked ? 'selected' : ''}`}
                >
                  <input
                    type="radio"
                    id={inputId}
                    name={`radio-${key}`}
                    value={opt.value}
                    checked={isChecked}
                    onChange={() => handleChange(opt.value)}
                  />
                  <span className="choice-label">{opt.label}</span>
                </label>
              );
            })}
          </div>
        );
      }

      case 'MULTI_CHOICE': {
        const options = question.options || [];
        const currentVals = Array.isArray(value) ? value : [];

        const handleCheckChange = (optVal, isChecked) => {
          let updated;
          if (isChecked) {
            updated = currentVals.includes(optVal) ? currentVals : [...currentVals, optVal];
          } else {
            updated = currentVals.filter((v) => v !== optVal);
          }
          handleChange(updated);
        };

        return (
          <div className="options-group" role="group" aria-labelledby={`prompt-${key}`}>
            {options.map((opt) => {
              const isChecked = currentVals.includes(opt.value);
              const inputId = `q-${key}-${opt.value}`;
              return (
                <label
                  key={opt.value}
                  htmlFor={inputId}
                  className={`choice-item tap-target ${isChecked ? 'selected' : ''}`}
                >
                  <input
                    type="checkbox"
                    id={inputId}
                    name={`checkbox-${key}`}
                    value={opt.value}
                    checked={isChecked}
                    onChange={(e) => handleCheckChange(opt.value, e.target.checked)}
                  />
                  <span className="choice-label">{opt.label}</span>
                </label>
              );
            })}
          </div>
        );
      }

      case 'RATING': {
        const min = question.rating?.min ?? question.min ?? 1;
        const max = question.rating?.max ?? question.max ?? 5;
        const minLabel = question.rating?.minLabel;
        const maxLabel = question.rating?.maxLabel;

        const range = [];
        for (let i = min; i <= max; i++) {
          range.push(i);
        }

        return (
          <div className="rating-control">
            <div className="rating-buttons" role="radiogroup" aria-labelledby={`prompt-${key}`}>
              {range.map((num) => {
                const isSelected = value === num;
                return (
                  <button
                    key={num}
                    type="button"
                    role="radio"
                    aria-checked={isSelected}
                    className={`rating-btn tap-target ${isSelected ? 'active' : ''}`}
                    onClick={() => handleChange(num)}
                  >
                    {num}
                  </button>
                );
              })}
            </div>
            {(minLabel || maxLabel) && (
              <div className="rating-labels">
                <span className="rating-min-label">{minLabel || ''}</span>
                <span className="rating-max-label">{maxLabel || ''}</span>
              </div>
            )}
          </div>
        );
      }

      case 'YES_NO': {
        const isYes = value === true;
        const isNo = value === false;

        return (
          <div className="yes-no-group" role="radiogroup" aria-labelledby={`prompt-${key}`}>
            <button
              type="button"
              role="radio"
              aria-checked={isYes}
              className={`yes-no-btn tap-target ${isYes ? 'selected-yes' : ''}`}
              onClick={() => handleChange(true)}
            >
              Yes
            </button>
            <button
              type="button"
              role="radio"
              aria-checked={isNo}
              className={`yes-no-btn tap-target ${isNo ? 'selected-no' : ''}`}
              onClick={() => handleChange(false)}
            >
              No
            </button>
          </div>
        );
      }

      case 'NUMBER': {
        const min = question.min;
        const max = question.max;
        return (
          <div className="input-group">
            <input
              type="number"
              id={`input-${key}`}
              className="text-input tap-target"
              min={min}
              max={max}
              value={value ?? ''}
              onChange={(e) => {
                const valStr = e.target.value;
                handleChange(valStr === '' ? null : Number(valStr));
              }}
              placeholder={min !== undefined && max !== undefined ? `${min} to ${max}` : 'Enter number'}
            />
          </div>
        );
      }

      case 'TEXT': {
        const maxLength = question.maxLength || 1000;
        const currentLen = typeof value === 'string' ? value.length : 0;
        return (
          <div className="textarea-group">
            <textarea
              id={`input-${key}`}
              className="textarea-input"
              rows={4}
              maxLength={maxLength}
              value={value || ''}
              onChange={(e) => handleChange(e.target.value)}
              placeholder="Type your response..."
            />
            <div className="char-count">
              {currentLen} / {maxLength}
            </div>
          </div>
        );
      }

      default:
        return <p className="unsupported-msg">Unsupported question type: {type}</p>;
    }
  };

  return (
    <div className={`question-card ${error ? 'has-error' : ''}`} id={`q-card-${key}`}>
      <label id={`prompt-${key}`} htmlFor={`input-${key}`} className="question-prompt">
        {prompt}
        {required && <span className="required-star" aria-hidden="true"> *</span>}
        {required && <span className="sr-only"> (required)</span>}
      </label>

      {renderControl()}

      {error && (
        <div className="question-error" role="alert">
          {error}
        </div>
      )}
    </div>
  );
}
