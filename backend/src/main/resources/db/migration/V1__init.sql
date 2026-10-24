CREATE TABLE academic_terms (
  term_id INT AUTO_INCREMENT PRIMARY KEY,
  label VARCHAR(40) NOT NULL UNIQUE,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  is_current BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE users (
  user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  college_id VARCHAR(30) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(160),
  password_hash VARCHAR(100) NOT NULL,
  role ENUM('STUDENT','FACULTY','HOD','PLACEMENT_OFFICER','ADMIN') NOT NULL,
  branch VARCHAR(20),
  study_year TINYINT,
  section VARCHAR(5),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_users_target (role, branch, study_year, section)
);

CREATE TABLE survey_templates (
  template_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(160) NOT NULL,
  category ENUM('PLACEMENT','ACADEMIC','FACILITY','EVENT','WELLBEING','CUSTOM') NOT NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (created_by) REFERENCES users(user_id)
);

CREATE TABLE surveys (
  survey_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  template_id BIGINT NOT NULL,
  version INT NOT NULL DEFAULT 1,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  category ENUM('PLACEMENT','ACADEMIC','FACILITY','EVENT','WELLBEING','CUSTOM') NOT NULL,
  term_id INT NOT NULL,
  question_schema JSON NOT NULL,
  skill_targets JSON NULL,
  is_anonymous BOOLEAN NOT NULL,
  target_filter JSON NOT NULL,
  status ENUM('DRAFT','ACTIVE','CLOSED') NOT NULL DEFAULT 'DRAFT',
  opens_at DATETIME NULL,
  closes_at DATETIME NULL,
  eligible_count INT NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  activated_at DATETIME NULL,
  UNIQUE KEY uq_template_version (template_id, version),
  FOREIGN KEY (template_id) REFERENCES survey_templates(template_id),
  FOREIGN KEY (term_id) REFERENCES academic_terms(term_id),
  FOREIGN KEY (created_by) REFERENCES users(user_id),
  INDEX idx_surveys_status (status, category)
);

CREATE TABLE responses (
  response_id CHAR(36) PRIMARY KEY,          -- random UUID v4 made by the app, never time-ordered
  survey_id BIGINT NOT NULL,
  student_id BIGINT NULL,                    -- NULL on anonymous surveys
  group_branch VARCHAR(20) NULL,
  group_year TINYINT NULL,
  group_section VARCHAR(5) NULL,             -- NULL on anonymous surveys
  answer_data JSON NOT NULL,
  submitted_at DATETIME NULL,                -- NULL on anonymous surveys
  UNIQUE KEY uq_one_per_student (survey_id, student_id),   -- NULLs allowed, so anonymous rows never collide
  FOREIGN KEY (survey_id) REFERENCES surveys(survey_id),
  FOREIGN KEY (student_id) REFERENCES users(user_id),
  INDEX idx_resp_survey (survey_id)
);

CREATE TABLE participation_log (
  survey_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  has_responded BOOLEAN NOT NULL DEFAULT FALSE,   -- no timestamp column on purpose
  PRIMARY KEY (survey_id, student_id),
  FOREIGN KEY (survey_id) REFERENCES surveys(survey_id),
  FOREIGN KEY (student_id) REFERENCES users(user_id)
);

CREATE TABLE student_results (
  survey_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  payload JSON NOT NULL,
  computed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (survey_id, student_id),
  FOREIGN KEY (survey_id) REFERENCES surveys(survey_id),
  FOREIGN KEY (student_id) REFERENCES users(user_id)
);

CREATE TABLE analytics_cache (
  survey_id BIGINT NOT NULL,
  kind ENUM('AGGREGATE','READINESS','TREND','INSIGHT') NOT NULL,
  response_count INT NOT NULL,               -- cache is stale when this differs from the live count
  payload JSON NOT NULL,
  computed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (survey_id, kind),
  FOREIGN KEY (survey_id) REFERENCES surveys(survey_id)
);

CREATE TABLE ai_usage (
  usage_date DATE PRIMARY KEY,
  calls INT NOT NULL DEFAULT 0
);

CREATE TABLE audit_log (
  audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NULL,
  action VARCHAR(60) NOT NULL,
  entity VARCHAR(40) NOT NULL,
  entity_id VARCHAR(40) NULL,
  detail JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
