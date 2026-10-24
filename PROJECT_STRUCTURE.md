# SkillSense — Complete Project Structure & Implementation Status

> **Current Approved Milestone**: **Phase 6 Complete (Phases 0–6 Approved & Frozen)**  
> **Source of Truth**: [`docs/SKILLSENSE_SPEC.md`](file:///c:/Users/akans/csp-Project/SkillSense/docs/SKILLSENSE_SPEC.md)  
> **Backend Regression Tests**: **101/101 PASS**  
> **Analytics Engine Tests**: **30/30 PASS**  
> **System Health**: Backend (`http://localhost:9000/api/health`) — `{"status":"ok"}` | Analytics (`http://localhost:8001/health`) — `{"status":"ok"}`

---

## 1. High-Level Architecture & Communication Boundaries

```
Browser / Client
      │
      ▼  (HTTP / JWT Auth on Port 5173 -> 9000)
┌────────────────────────────────────────────────────────┐
│               Spring Boot API (Port 9000)              │
│  - Security, JWT Authentication & Role Authorization   │
│  - Survey Lifecycle, Targeting, & Question Validation │
│  - Response Submission & Anonymity Enforcement         │
│  - Analytics Cache & 5-Response Privacy Gate           │
│  - AI Daily Quota Ownership (ai_usage table)           │
│  - Student Personal Results Calculation                │
└──────────────┬──────────────────────────┬──────────────┘
               │                          │
   (JDBC via Hikari)            (HTTP RestClient, 10s Timeout)
               │                          │
               ▼                          ▼
┌────────────────────────┐      ┌─────────────────────────────────────────┐
│     MySQL 8.4 DB       │      │       FastAPI Analytics (Port 8001)     │
│       (Port 3306)      │      │  - Completely Stateless (NO DB ACCESS)  │
│ - V1: Relational Schema│      │  - Aggregates, Normalization, Skill Gaps│
│ - V2: Privacy Triggers │      │  - K=5 Group Suppression Algorithm      │
│ - Anonymity Guarantees │      │  - Cohort Trends across Terms           │
└────────────────────────┘      │  - Gemini Prompt & Output Sanitization  │
                                │  - Retry-Once & RULES Fallback Engine   │
                                └────────────────────┬────────────────────┘
                                                     │
                                            (google-genai SDK)
                                                     │
                                                     ▼
                                        ┌─────────────────────────┐
                                        │       Gemini API        │
                                        │  - ONLY Aggregate Data  │
                                        │  - ZERO Student PII     │
                                        │  - Strict JSON Output   │
                                        └─────────────────────────┘
```

### Strict Architectural Boundaries
1. **The browser NEVER directly communicates with FastAPI (8001) or Gemini.**
2. **FastAPI NEVER connects to MySQL.** It receives JSON payloads from Spring Boot and returns mathematical/AI results.
3. **Gemini NEVER receives student identities, names, roll numbers, individual responses, or suppressed groups.**
4. **Spring Boot is the single source of truth for database persistence, security, and quota ownership.**

---

## 2. Milestone Progress Tracker (Phases 0–12)

| Phase | Description | Status | Test Coverage |
|---|---|---|---|
| **Phase 0** | Repository, Environment, Docker Compose, Port configuration | **APPROVED** | Verified (Ports 9000, 8001, 5173, 3306) |
| **Phase 1** | DB Schema (`V1__init.sql`), Anonymity Triggers (`V2__triggers.sql`) | **APPROVED** | 6 DB Trigger Tests (`DatabaseAnonymityTriggersTest`) |
| **Phase 2** | Authentication, JWT, BCrypt, Login Lockout, Users & Terms CRUD | **APPROVED** | 22 Auth Tests (`AuthenticationAndUsersTest`) |
| **Phase 3** | Survey Engine, Lifecycle (Draft/Active/Closed), Schema Validator | **APPROVED** | 17 Survey Tests + 22 Schema Tests (`SurveyEngineIntegrationTest`, `QuestionSchemaValidatorTest`) |
| **Phase 4** | Response Submission, Anonymity Engine, Participation Log, Single-Submit Gate | **APPROVED** | 13 Submission Tests + 1 Answer Validator Test (`ResponseSubmissionIntegrationTest`, `AnswerValidatorTest`) |
| **Phase 5** | Stateless Python Analytics Engine (Aggregates, Readiness, Trends, Text Themes) | **APPROVED** | 24 Pytest Tests (`tests/test_*.py`) |
| **Phase 6** | Backend ↔ Analytics ↔ Gemini, AnalyticsClient, Caching, 5-Response Gate, AI Quota | **APPROVED** | 20 New Backend Tests (`AnalyticsIntegrationTest`, `AnalyticsClientTest`) + 6 New Pytest Tests (`test_gemini.py`) |
| **Phase 7** | Frontend Foundation & Student Flow | *Pending* | Not started (frozen until Phase 6 sign-off) |
| **Phase 8** | Frontend Survey Builder for Officials | *Pending* | Not started |
| **Phase 9** | Frontend Analytics Dashboard & Charts | *Pending* | Not started |
| **Phase 10** | PDF / Excel Report Exporting & Admin Screens | *Pending* | Not started |
| **Phase 11** | Demo Data Seeding, Dockerization, & Benchmarking | *Pending* | Not started |
| **Phase 12** | Hardening, Security Review, & Final Handover | *Pending* | Not started |

---

## 3. Complete File & Directory Map

```text
skillsense/
├── .env                              # Active environment configuration (Ports, Secrets, DB, Keys)
├── .env.example                      # Reference environment template
├── .gitignore                        # Git exclusion rules
├── docker-compose.yml                # Docker Compose orchestration (MySQL 8)
├── README.md                         # Project setup & run documentation
├── PROJECT_STRUCTURE.md              # Current project file & architecture inventory (This File)
│
├── docs/
│   └── SKILLSENSE_SPEC.md            # Authoritative design document & specification (Frozen)
│
├── analytics/                        # Stateless Python/FastAPI analytics service (Port 8001)
│   ├── pyproject.toml                # Pytest & package configuration
│   ├── requirements.txt              # Python dependencies (fastapi, uvicorn, pydantic, pandas, google-genai)
│   ├── app/
│   │   ├── __init__.py               # Package marker
│   │   ├── config.py                 # Configuration loader (GEMINI_API_KEY, GEMINI_MODEL, timeouts)
│   │   ├── schemas.py                # Pydantic input/output request & response models
│   │   ├── aggregate.py              # Per-question stats, satisfaction index, K=5 group suppression, text themes
│   │   ├── readiness.py              # Formula-based readiness score, skill gaps, bands, student results
│   │   ├── trend.py                  # Question-key matching, delta calculation, low-confidence flagging
│   │   ├── insights.py               # Gemini prompt builder, retry-once, validation, deterministic RULES fallback
│   │   └── main.py                   # FastAPI routing endpoints (/health, /analyze, /student-result, /trend, /insights)
│   └── tests/
│       ├── test_aggregate.py         # Tests for distributions, satisfaction index, K=5 group suppression
│       ├── test_api.py               # Tests for FastAPI endpoints and JSON contracts
│       ├── test_gemini.py            # Tests for Gemini integration, privacy boundary, retry-once, rules fallback
│       ├── test_health.py            # Analytics /health endpoint test
│       ├── test_insights.py          # Deterministic insights & template tests
│       ├── test_readiness.py         # Section 11 worked example verification, gap calculations, bands
│       └── test_trend.py             # Trend delta and confidence tests
│
├── backend/                          # Spring Boot API service (Port 9000)
│   ├── pom.xml                       # Maven build file (Java 21, Spring Boot 3.3.4, Spring Security, JPA, Flyway)
│   ├── mvnw / mvnw.cmd               # Maven wrappers
│   ├── src/
│   │   ├── main/
│   │   │   ├── resources/
│   │   │   │   ├── application.yml   # Spring Boot configuration (Port 9000, DB, JWT, Analytics URL, AI Quota)
│   │   │   │   ├── presets/          # Preset starter schemas by category (Academic, Facility, Placement, etc.)
│   │   │   │   └── db/migration/
│   │   │   │       ├── V1__init.sql      # Core MySQL schema (Users, Terms, Surveys, Responses, Cache, Quota)
│   │   │   │       └── V2__triggers.sql  # Database triggers enforcing anonymity & schema immutability
│   │   │   └── java/com/skillsense/
│   │   │       ├── SkillSenseApplication.java     # Main Spring Boot application entrypoint
│   │   │       │
│   │   │       ├── auth/                          # Authentication & Security
│   │   │       │   ├── AuthController.java        # POST /api/auth/login, GET /api/auth/me, POST /change-password
│   │   │       │   ├── AuthService.java           # Authentication logic & password hashing
│   │   │       │   ├── JwtUtil.java               # JWT token generation, signing, validation (HS256)
│   │   │       │   ├── JwtAuthenticationFilter.java# Bearer token validation filter
│   │   │       │   ├── LoginLockoutService.java   # 5-failed-attempts account lockout mechanism
│   │   │       │   └── dto/                       # Login & Auth request/response DTOs
│   │   │       │
│   │   │       ├── user/                          # User Management & Administration
│   │   │       │   ├── User.java                  # JPA entity for users (Admin, Faculty, HOD, Placement, Student)
│   │   │       │   ├── Role.java                  # Role enum definition
│   │   │       │   ├── UserRepository.java        # User repository with branch, role, and text search
│   │   │       │   ├── UserService.java           # User CRUD and bulk operations
│   │   │       │   ├── UserController.java        # Admin user management endpoints
│   │   │       │   ├── UserCsvImportService.java  # Bulk student CSV importer
│   │   │       │   ├── AdminUserInitializer.java  # Auto-seeding of default admin account
│   │   │       │   └── dto/                       # User request/response DTOs
│   │   │       │
│   │   │       ├── term/                          # Academic Terms Management
│   │   │       │   ├── AcademicTerm.java          # JPA entity for academic semesters
│   │   │       │   ├── AcademicTermRepository.java# Term repository
│   │   │       │   ├── TermService.java           # Term management (setting active term)
│   │   │       │   └── TermController.java        # /api/terms endpoints
│   │   │       │
│   │   │       ├── survey/                        # Survey Engine & Lifecycle
│   │   │       │   ├── entity/
│   │   │       │   │   ├── Survey.java            # JPA entity for surveys
│   │   │       │   │   └── SurveyTemplate.java    # JPA entity for recurring survey templates
│   │   │       │   ├── model/
│   │   │       │   │   ├── SurveyCategory.java    # Enum: PLACEMENT, ACADEMIC, FACILITY, EVENT, WELLBEING, CUSTOM
│   │   │       │   │   ├── SurveyStatus.java      # Enum: DRAFT, ACTIVE, CLOSED
│   │   │       │   │   ├── TargetFilter.java      # Branch, Year, and Section targeting model
│   │   │       │   │   └── schema/                # QuestionSchema, Question, Options, Scoring models
│   │   │       │   ├── repository/
│   │   │       │   │   ├── SurveyRepository.java  # Survey JPA repository
│   │   │       │   │   ├── SurveyTemplateRepository.java
│   │   │       │   │   └── ParticipationLogRepository.java # Tracking survey participation
│   │   │       │   ├── service/
│   │   │       │   │   ├── SurveyService.java     # Survey CRUD, lifecycle transitions, authorization
│   │   │       │   │   ├── TargetingService.java  # Student eligibility resolution
│   │   │       │   │   ├── PresetService.java     # Starter survey schemas
│   │   │       │   │   └── SurveySchedulerService.java # Auto-closing expired surveys
│   │   │       │   ├── validation/
│   │   │       │   │   ├── QuestionSchemaValidator.java # JSON schema validation
│   │   │       │   │   └── AnswerValidator.java         # Student submission answer validator
│   │   │       │   ├── SurveyController.java      # Official survey management endpoints (/api/surveys)
│   │   │       │   ├── PresetController.java      # Preset retrieval endpoint
│   │   │       │   └── dto/                       # Survey create, update, and progress DTOs
│   │   │       │
│   │   │       ├── response/                      # Response Submission & Anonymity Handler
│   │   │       │   ├── entity/
│   │   │       │   │   ├── Response.java          # Response entity (UUID PK; NULL student_id on anonymous)
│   │   │       │   │   ├── ParticipationLog.java  # Participation record (has_responded flag, NO timestamp)
│   │   │       │   │   └── ParticipationLogId.java# Composite PK (survey_id, student_id)
│   │   │       │   ├── repository/
│   │   │       │   │   └── ResponseRepository.java# Response repository
│   │   │       │   ├── service/
│   │   │       │   │   └── ResponseService.java   # Transactional 7-step submission algorithm
│   │   │       │   ├── StudentSurveyController.java# Student portal endpoints (/api/my/surveys)
│   │   │       │   └── dto/                       # Submission and student survey DTOs
│   │   │       │
│   │   │       ├── analytics/                     # Phase 6: Analytics & AI Integration
│   │   │       │   ├── client/
│   │   │       │   │   ├── AnalyticsClient.java   # Interface for communication with FastAPI
│   │   │       │   │   └── DefaultAnalyticsClient.java # RestClient with 10s timeout & 503 mapping
│   │   │       │   ├── entity/
│   │   │       │   │   ├── AnalyticsCache.java    # Entity mapped to analytics_cache table
│   │   │       │   │   ├── AnalyticsCacheId.java  # Composite PK (survey_id, kind)
│   │   │       │   │   ├── AnalyticsCacheKind.java# Enum: AGGREGATE, READINESS, TREND, INSIGHT
│   │   │       │   │   ├── StudentResult.java     # Entity mapped to student_results table
│   │   │       │   │   ├── StudentResultId.java   # Composite PK (survey_id, student_id)
│   │   │       │   │   └── AiUsage.java           # Entity mapped to ai_usage table
│   │   │       │   ├── repository/
│   │   │       │   │   ├── AnalyticsCacheRepository.java
│   │   │       │   │   ├── StudentResultRepository.java
│   │   │       │   │   └── AiUsageRepository.java # Atomic quota incrementing
│   │   │       │   ├── service/
│   │   │       │   │   ├── AnalyticsService.java  # Cache management, 5-response gate, AI quota, throttling
│   │   │       │   │   └── StudentResultService.java # Individual student readiness & deterministic tips
│   │   │       │   ├── controller/
│   │   │       │   │   └── AnalyticsController.java# Official analytics endpoints & dashboard summary
│   │   │       │   └── dto/                       # Analytics, Trend, Insight, Dashboard, & FastAPI DTOs
│   │   │       │
│   │   │       └── common/                        # Cross-Cutting Infrastructure
│   │   │           ├── SecurityConfig.java        # Spring Security filter chain & method security
│   │   │           ├── error/                     # GlobalExceptionHandler, ApiException, ErrorCode
│   │   │           └── audit/                     # Audit log service
│   │   │
│   │   └── test/java/com/skillsense/              # Backend Automated Test Suite (101 Tests)
│   │       ├── HealthControllerTest.java          # Health check endpoint test (1 test)
│   │       ├── DatabaseAnonymityTriggersTest.java # Anonymity & DB triggers tests (6 tests)
│   │       ├── AuthenticationAndUsersTest.java    # Authentication, JWT, and lockout tests (22 tests)
│   │       ├── QuestionSchemaValidatorTest.java   # Question schema rules validation tests (22 tests)
│   │       ├── AnswerValidatorTest.java           # Answer submission validation tests (1 test)
│   │       ├── SurveyEngineIntegrationTest.java   # Survey lifecycle, targeting, versioning tests (17 tests)
│   │       ├── ResponseSubmissionIntegrationTest.java # Response submission & anonymity tests (13 tests)
│   │       ├── AnalyticsIntegrationTest.java      # Phase 6 official endpoints, gate, cache, scope (17 tests)
│   │       └── analytics/
│   │           └── AnalyticsClientTest.java       # Phase 6 RestClient timeout & 503 tests (2 tests)
│   │
└── frontend/                         # React 18 + Vite Frontend (Port 5173 - Foundation Ready for Phase 7)
    ├── package.json                  # Dependencies (react, react-router-dom, axios, chart.js)
    ├── vite.config.js                # Port 5173 configuration
    ├── index.html                    # Root HTML file
    └── src/
        ├── App.jsx                   # Base app layout
        ├── main.jsx                  # React DOM root
        └── index.css                 # Base CSS styling
```

---

## 4. Key Implementation Details & Invariants

### 1. Spring Boot AnalyticsClient
- Located in: [`com.skillsense.analytics.client`](file:///c:/Users/akans/csp-Project/SkillSense/backend/src/main/java/com/skillsense/analytics/client)
- Configured using Spring `RestClient` with a **10-second connect and read timeout**.
- Target: `http://localhost:8001` (configured via `app.analytics.url`).
- Automatically translates any connection failure, timeout, or FastAPI 5xx response into HTTP `503 ANALYTICS_UNAVAILABLE`.

### 2. Analytics Cache
- Table: `analytics_cache` (`survey_id`, `kind`, `response_count`, `payload`, `computed_at`).
- Live response count is queried first. If `cached.response_count == live_count`, the cached JSON payload is returned immediately without calling Python.
- If the count differs, it triggers a recalculation, updates the cache row, and returns the fresh calculation.

### 3. Five-Response Privacy Gate
- For anonymous surveys: if `response_count < 5`, Spring Boot blocks analytics from being generated or exposed. It returns:
  ```json
  { "available": false, "have": N, "need": 5, "data": {} }
  ```
- For free-text comments (`/api/surveys/{id}/texts?key=`): hidden completely if total responses $< 5$ or if fewer than 5 respondents answered that specific question. Comments are shuffled randomly to prevent order-correlation attacks.

### 4. Role Scope & Authorization
- **Student**: Cannot access official analytics endpoints (`403 FORBIDDEN`).
- **HOD**: Permitted to view surveys targeting their own branch. Attempting to view another branch's survey returns `403 FORBIDDEN`.
- **Faculty**: Permitted to view only surveys they personally created (`403 FORBIDDEN` for other surveys).
- **Placement Officer**: Permitted to view `PLACEMENT` category surveys and their own surveys.
- **Admin**: Unrestricted access.

### 5. AI Daily Quota & Insight Caching
- Quota is owned by Spring Boot in the `ai_usage` table.
- Daily limit: `50` (configurable via `app.ai.daily-limit`).
- Only real successful Gemini generations (`source: "AI"`) increment the usage counter. Deterministic `RULES` fallbacks do not consume quota.
- AI insights are cached and reused until the response count shifts by $\ge 10\%$ or the survey closes.
- Manual regeneration (`refresh=true`) is throttled to **1 call per survey per 10 minutes**.

### 6. Student Results
- For non-anonymous scored surveys: student result is calculated deterministically upon submission and stored in `student_results`.
- Accessible via `GET /api/my/surveys/{id}/result`.
- Returns personal readiness, band, skill gap breakdown, deterministic tips, and the label: `"Self-assessed indicator, not an exam score"`.
- Anonymous or unscored surveys return `404 NOT_FOUND`.
- Student tips are deterministic templates to preserve Gemini quota.

### 7. Gemini Sanitization & Fallback
- Gemini prompt receives **only** aggregate metrics wrapped inside `<survey_data>` with explicit prompt-injection defenses.
- Output is validated with Pydantic against `InsightModel`.
- Retries once on failure; if failure persists or key is missing/dummy, seamlessly falls back to deterministic `source: "RULES"`.

---

## 5. Verification Commands

To verify the current state of the project at any time:

### Run All Backend Tests (101 tests):
```powershell
cd backend
.\mvnw.cmd test
```

### Run All Analytics Tests (30 tests):
```powershell
cd analytics
.\venv\Scripts\pytest.exe -v
```

### Check Service Health:
```powershell
curl.exe -s http://localhost:9000/api/health
curl.exe -s http://localhost:8001/health
```
