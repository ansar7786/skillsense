import { describe, it, expect } from 'vitest';
import { validateSurvey } from '../utils/surveyValidator';

describe('SurveyValidator — Section 6 & Schema Validation Rules', () => {
  const validBaseSurvey = {
    title: 'Valid Placement Survey',
    category: 'PLACEMENT',
    termId: 1,
    questionSchema: {
      schemaVersion: 1,
      questions: [
        {
          key: 'java_skill',
          type: 'RATING',
          prompt: 'Rate your Java confidence',
          required: true,
          rating: { min: 1, max: 5 },
          scoring: { skill: 'java', weight: 1.0 }
        }
      ]
    },
    skillTargets: { java: 70 }
  };

  it('validates a completely valid survey schema with 0 errors', () => {
    const res = validateSurvey(validBaseSurvey);
    expect(res.isValid).toBe(true);
    expect(Object.keys(res.errors)).toHaveLength(0);
  });

  it('flags missing or blank survey title', () => {
    const survey = { ...validBaseSurvey, title: '   ' };
    const res = validateSurvey(survey);
    expect(res.isValid).toBe(false);
    expect(res.errors.title).toMatch(/title is required/i);
  });

  it('flags missing category or termId', () => {
    const survey = { ...validBaseSurvey, category: null, termId: null };
    const res = validateSurvey(survey);
    expect(res.isValid).toBe(false);
    expect(res.errors.category).toBeDefined();
    expect(res.errors.termId).toBeDefined();
  });

  it('flags survey with zero questions', () => {
    const survey = {
      ...validBaseSurvey,
      questionSchema: { schemaVersion: 1, questions: [] }
    };
    const res = validateSurvey(survey);
    expect(res.isValid).toBe(false);
    expect(res.errors.questions).toMatch(/at least 1 question/i);
  });

  it('flags invalid or duplicate question keys', () => {
    const survey = {
      ...validBaseSurvey,
      questionSchema: {
        schemaVersion: 1,
        questions: [
          {
            key: 'dup_key',
            type: 'TEXT',
            prompt: 'Question one prompt',
            required: true
          },
          {
            key: 'dup_key',
            type: 'TEXT',
            prompt: 'Question two prompt',
            required: true
          },
          {
            key: 'INVALID-KEY!',
            type: 'TEXT',
            prompt: 'Question three prompt',
            required: true
          }
        ]
      }
    };
    const res = validateSurvey(survey);
    expect(res.isValid).toBe(false);
    expect(res.errors.question_1_key).toMatch(/duplicate question key/i);
    expect(res.errors.question_2_key).toMatch(/lowercase letters, digits/i);
  });

  it('flags short or missing prompt', () => {
    const survey = {
      ...validBaseSurvey,
      questionSchema: {
        schemaVersion: 1,
        questions: [
          {
            key: 'short_prompt',
            type: 'RATING',
            prompt: 'ab',
            rating: { min: 1, max: 5 }
          }
        ]
      }
    };
    const res = validateSurvey(survey);
    expect(res.isValid).toBe(false);
    expect(res.errors.question_0_prompt).toMatch(/at least 3 characters/i);
  });

  it('flags single choice question with fewer than 2 options or empty option values', () => {
    const survey = {
      ...validBaseSurvey,
      questionSchema: {
        schemaVersion: 1,
        questions: [
          {
            key: 'choice_q',
            type: 'SINGLE_CHOICE',
            prompt: 'Choose one target career',
            options: [{ value: '', label: 'Option 1' }]
          }
        ]
      }
    };
    const res = validateSurvey(survey);
    expect(res.isValid).toBe(false);
    expect(res.errors.question_0_options).toMatch(/at least 2 options/i);
    expect(res.errors.question_0_opt_0_value).toMatch(/cannot be empty/i);
  });

  it('flags invalid rating range (min >= max or max > 10)', () => {
    const survey = {
      ...validBaseSurvey,
      questionSchema: {
        schemaVersion: 1,
        questions: [
          {
            key: 'bad_rating',
            type: 'RATING',
            prompt: 'Rate this course overall',
            rating: { min: 5, max: 5 }
          }
        ]
      }
    };
    const res = validateSurvey(survey);
    expect(res.isValid).toBe(false);
    expect(res.errors.question_0_rating).toMatch(/must be less than max/i);
  });

  it('flags invalid number range (min >= max)', () => {
    const survey = {
      ...validBaseSurvey,
      questionSchema: {
        schemaVersion: 1,
        questions: [
          {
            key: 'bad_num',
            type: 'NUMBER',
            prompt: 'Enter number of projects deployed',
            min: 10,
            max: 5
          }
        ]
      }
    };
    const res = validateSurvey(survey);
    expect(res.isValid).toBe(false);
    expect(res.errors.question_0_number).toMatch(/min must be less than max/i);
  });

  it('flags invalid text maxLength (<= 0)', () => {
    const survey = {
      ...validBaseSurvey,
      questionSchema: {
        schemaVersion: 1,
        questions: [
          {
            key: 'bad_text',
            type: 'TEXT',
            prompt: 'Enter your overall feedback',
            maxLength: -5
          }
        ]
      }
    };
    const res = validateSurvey(survey);
    expect(res.isValid).toBe(false);
    expect(res.errors.question_0_maxLength).toMatch(/greater than 0/i);
  });

  it('flags invalid scoring configuration (weight < 0.5 or > 5.0, or invalid skill key)', () => {
    const survey = {
      ...validBaseSurvey,
      questionSchema: {
        schemaVersion: 1,
        questions: [
          {
            key: 'scored_q',
            type: 'RATING',
            prompt: 'Rate problem solving skills',
            rating: { min: 1, max: 5 },
            scoring: { skill: 'INVALID SKILL!', weight: 10.0 }
          }
        ]
      }
    };
    const res = validateSurvey(survey);
    expect(res.isValid).toBe(false);
    expect(res.errors.question_0_scoring_skill).toMatch(/lowercase letters, digits/i);
    expect(res.errors.question_0_scoring_weight).toMatch(/between 0.5 and 5.0/i);
  });

  it('flags invalid date combinations (closesAt <= opensAt)', () => {
    const survey = {
      ...validBaseSurvey,
      opensAt: '2026-10-31T12:00:00',
      closesAt: '2026-10-30T12:00:00'
    };
    const res = validateSurvey(survey);
    expect(res.isValid).toBe(false);
    expect(res.errors.closesAt).toMatch(/must be after the opening date/i);
  });

  it('flags out-of-range skill targets (< 0 or > 100)', () => {
    const survey = {
      ...validBaseSurvey,
      skillTargets: { java: 150 }
    };
    const res = validateSurvey(survey);
    expect(res.isValid).toBe(false);
    expect(res.errors.skillTarget_java).toMatch(/between 0 and 100/i);
  });
});
