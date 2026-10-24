import React from 'react';

/**
 * Skill Targets Configuration Panel
 * Displays 0–100 sliders with current numeric values for each distinct skill in the survey.
 */
export default function SkillTargetsPanel({
  questions = [],
  skillTargets = {},
  onChange
}) {
  // Extract all distinct skills across questions
  const skills = Array.from(
    new Set(
      questions
        .filter((q) => q.scoring && q.scoring.skill && q.scoring.skill.trim())
        .map((q) => q.scoring.skill.trim().toLowerCase())
    )
  ).sort();

  const handleSliderChange = (skill, val) => {
    const updated = {
      ...skillTargets,
      [skill]: Number(val)
    };
    onChange(updated);
  };

  if (skills.length === 0) {
    return (
      <div className="sub-panel skill-targets-empty">
        <h4 className="sub-panel-title">Competency Skill Targets</h4>
        <p className="text-muted text-sm mt-1">
          No skills configured yet. Enable <em>Competency Scoring</em> on questions to define benchmark targets (0–100%).
        </p>
      </div>
    );
  }

  return (
    <div className="sub-panel skill-targets-panel">
      <div className="d-flex justify-between align-center mb-2">
        <h4 className="sub-panel-title">Skill Benchmark Targets</h4>
        <span className="badge badge-subtle">{skills.length} {skills.length === 1 ? 'skill' : 'skills'}</span>
      </div>
      <p className="field-hint mb-3">
        Set institutional target scores (0–100%) to measure cohort readiness gaps.
      </p>

      <div className="skill-sliders-list">
        {skills.map((skill) => {
          const targetValue = skillTargets[skill] !== undefined ? skillTargets[skill] : 70;
          return (
            <div key={skill} className="skill-slider-row">
              <div className="d-flex justify-between align-center mb-1">
                <span className="skill-slider-name code-font">{skill}</span>
                <span className="skill-slider-val tabular-nums">{targetValue}%</span>
              </div>
              <div className="slider-wrapper">
                <input
                  type="range"
                  min="0"
                  max="100"
                  step="5"
                  className="skill-range-slider"
                  value={targetValue}
                  onChange={(e) => handleSliderChange(skill, e.target.value)}
                  aria-label={`Target score for skill ${skill}`}
                />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
