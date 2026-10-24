const KEY_PATTERN = /^[a-z0-9_]+$/;

/**
 * Validates a survey draft before activation or saving.
 * Mirrors backend Section 6 and QuestionSchemaValidator rules.
 *
 * @param {Object} survey Survey object to validate
 * @returns {Object} { isValid: boolean, errors: Object, firstErrorField: string }
 */
export function validateSurvey(survey) {
  const errors = {};
  let firstErrorField = null;

  function addError(field, message) {
    if (!errors[field]) {
      errors[field] = message;
      if (!firstErrorField) {
        firstErrorField = field;
      }
    }
  }

  // Survey metadata validation
  if (!survey.title || !survey.title.trim()) {
    addError('title', 'Survey title is required');
  } else if (survey.title.trim().length > 200) {
    addError('title', 'Title cannot exceed 200 characters');
  }

  if (!survey.category) {
    addError('category', 'Category is required');
  }

  if (!survey.termId) {
    addError('termId', 'Academic term is required');
  }

  // Date validation
  if (survey.opensAt && survey.closesAt) {
    const opens = new Date(survey.opensAt);
    const closes = new Date(survey.closesAt);
    if (closes <= opens) {
      addError('closesAt', 'Closing date must be after the opening date');
    }
  }

  // Questions validation
  const questions = survey.questionSchema?.questions || [];
  if (questions.length === 0) {
    addError('questions', 'Survey must contain at least 1 question');
  } else if (questions.length > 40) {
    addError('questions', 'Survey cannot contain more than 40 questions');
  }

  const seenKeys = new Set();

  questions.forEach((q, idx) => {
    const qPrefix = `question_${idx}`;
    const key = q.key ? q.key.trim() : '';

    // Key validation
    if (!key) {
      addError(`${qPrefix}_key`, `Question ${idx + 1}: key is required`);
    } else {
      if (key.length < 2) {
        addError(`${qPrefix}_key`, `Question ${idx + 1}: key '${key}' must be at least 2 characters`);
      } else if (key.length > 40) {
        addError(`${qPrefix}_key`, `Question ${idx + 1}: key '${key}' must be at most 40 characters`);
      } else if (!KEY_PATTERN.test(key)) {
        addError(`${qPrefix}_key`, `Question ${idx + 1}: key '${key}' must contain only lowercase letters, digits, and underscores`);
      }

      if (seenKeys.has(key)) {
        addError(`${qPrefix}_key`, `Question ${idx + 1}: duplicate question key '${key}'`);
      } else {
        seenKeys.add(key);
      }
    }

    // Type validation
    if (!q.type) {
      addError(`${qPrefix}_type`, `Question ${idx + 1}: type is required`);
    }

    // Prompt validation
    const prompt = q.prompt ? q.prompt.trim() : '';
    if (!prompt || prompt.length < 3) {
      addError(`${qPrefix}_prompt`, `Question ${idx + 1}: prompt must be at least 3 characters`);
    } else if (prompt.length > 300) {
      addError(`${qPrefix}_prompt`, `Question ${idx + 1}: prompt cannot exceed 300 characters`);
    }

    // Choice question options
    if (q.type === 'SINGLE_CHOICE' || q.type === 'MULTI_CHOICE') {
      const options = q.options || [];
      if (options.length < 2) {
        addError(`${qPrefix}_options`, `Question ${idx + 1}: must have at least 2 options`);
      } else if (options.length > 12) {
        addError(`${qPrefix}_options`, `Question ${idx + 1}: cannot have more than 12 options`);
      }

      const seenOptValues = new Set();
      options.forEach((opt, optIdx) => {
        const val = opt.value ? String(opt.value).trim() : '';
        const label = opt.label ? String(opt.label).trim() : '';
        if (!val) {
          addError(`${qPrefix}_opt_${optIdx}_value`, `Question ${idx + 1}: option ${optIdx + 1} value cannot be empty`);
        } else if (seenOptValues.has(val)) {
          addError(`${qPrefix}_opt_${optIdx}_value`, `Question ${idx + 1}: duplicate option value '${val}'`);
        } else {
          seenOptValues.add(val);
        }

        if (!label) {
          addError(`${qPrefix}_opt_${optIdx}_label`, `Question ${idx + 1}: option ${optIdx + 1} label cannot be empty`);
        }

        if (opt.score !== undefined && opt.score !== null && opt.score !== '') {
          const scoreNum = Number(opt.score);
          if (isNaN(scoreNum) || scoreNum < 0 || scoreNum > 100) {
            addError(`${qPrefix}_opt_${optIdx}_score`, `Question ${idx + 1}: option score must be between 0 and 100`);
          }
        }
      });
    }

    // Rating validation
    if (q.type === 'RATING') {
      const rating = q.rating || {};
      const min = rating.min !== undefined && rating.min !== null ? Number(rating.min) : 1;
      const max = rating.max !== undefined && rating.max !== null ? Number(rating.max) : 5;
      if (isNaN(min) || isNaN(max)) {
        addError(`${qPrefix}_rating`, `Question ${idx + 1}: rating min and max must be valid numbers`);
      } else if (min >= max) {
        addError(`${qPrefix}_rating`, `Question ${idx + 1}: rating min (${min}) must be less than max (${max})`);
      } else if (max > 10) {
        addError(`${qPrefix}_rating`, `Question ${idx + 1}: rating max cannot exceed 10`);
      }
    }

    // Number validation
    if (q.type === 'NUMBER') {
      if (q.min !== undefined && q.min !== null && q.min !== '' &&
          q.max !== undefined && q.max !== null && q.max !== '') {
        const minNum = Number(q.min);
        const maxNum = Number(q.max);
        if (minNum >= maxNum) {
          addError(`${qPrefix}_number`, `Question ${idx + 1}: number min must be less than max`);
        }
      }
    }

    // Text validation
    if (q.type === 'TEXT') {
      if (q.maxLength !== undefined && q.maxLength !== null && q.maxLength !== '') {
        const ml = Number(q.maxLength);
        if (isNaN(ml) || ml <= 0) {
          addError(`${qPrefix}_maxLength`, `Question ${idx + 1}: maxLength must be greater than 0`);
        }
      }
    }

    // Scoring validation
    if (q.scoring && (q.scoring.skill || q.scoring.weight !== undefined)) {
      if (q.type !== 'RATING' && q.type !== 'SINGLE_CHOICE' && q.type !== 'YES_NO') {
        addError(`${qPrefix}_scoring`, `Question ${idx + 1}: scoring is only allowed for RATING, SINGLE_CHOICE, and YES_NO`);
      } else {
        const skill = q.scoring.skill ? q.scoring.skill.trim() : '';
        const weight = q.scoring.weight !== undefined && q.scoring.weight !== null ? Number(q.scoring.weight) : 1.0;

        if (!skill) {
          addError(`${qPrefix}_scoring_skill`, `Question ${idx + 1}: skill name is required when scoring is enabled`);
        } else if (!KEY_PATTERN.test(skill)) {
          addError(`${qPrefix}_scoring_skill`, `Question ${idx + 1}: skill key '${skill}' must contain only lowercase letters, digits, and underscores`);
        }

        if (isNaN(weight) || weight < 0.5 || weight > 5.0) {
          addError(`${qPrefix}_scoring_weight`, `Question ${idx + 1}: scoring weight must be between 0.5 and 5.0`);
        }
      }
    }
  });

  // Skill targets validation
  if (survey.skillTargets && typeof survey.skillTargets === 'object') {
    Object.entries(survey.skillTargets).forEach(([skill, target]) => {
      const num = Number(target);
      if (isNaN(num) || num < 0 || num > 100) {
        addError(`skillTarget_${skill}`, `Skill target for '${skill}' must be between 0 and 100`);
      }
    });
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors,
    firstErrorField
  };
}
