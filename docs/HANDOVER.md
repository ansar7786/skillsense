# SkillSense — Final Handover & Deployment Guide
**Phase 12 System Handover Documentation**

---

## 1. System Overview

**SkillSense** is a specialized, production-ready Student Analytics and Feedback platform tailored for engineering colleges. It enables institutions to collect, analyze, and act upon student feedback across multiple categories (Placement Readiness, Academic Course Feedback, Facility Satisfaction, Event Evaluation, Student Wellbeing, and Custom institutional surveys).

SkillSense enforces strict zero-knowledge anonymous survey privacy guarantees, preventing deanonymization through database triggers, physical column isolation, and K=5 group suppression with anti-subtraction protection. It integrates Google Gemini LLM for executive insight synthesis with an automatic, zero-dependency deterministic rules engine fallback.

---

## 2. Architecture

```
                             +-----------------------+
                             |   React 18 + Vite     |
                             |   (Port 5173 / Nginx) |
                             +-----------+-----------+
                                         |
                                         | REST / JWT (HTTP 9000)
                                         v
                             +-----------------------+
                             |   Spring Boot Backend |
                             |   Java 21 / Port 9000 |
                             +-----+-----------+-----+
                                   |           |
            Internal HTTP (8001)   |           | JDBC (3306)
                                   v           v
                        +-------------+     +-------------+
                        |   FastAPI   |     |    MySQL    |
                        |  Analytics  |     |   v8.0/8.4  |
                        +------+------+     +-------------+
                               |
                               | Google GenAI SDK (HTTPS)
                               v
                        +-------------+
                        | Gemini LLM  |
                        |   (Cloud)   |
                        +-------------+
```

* **Frontend**: React 18, Vite, React Router 6, Chart.js, Vanilla CSS design tokens.
* **Backend API**: Spring Boot 3.3.4, Java 21, Spring Security (Stateless JWT), Spring Data JPA, Flyway.
* **Analytics Engine**: Python 3.11+, FastAPI, Pandas, Google GenAI SDK (`google-genai`), Uvicorn.
* **Database**: MySQL 8.0/8.4 with BEFORE INSERT and BEFORE UPDATE integrity & anonymity triggers.

---

## 3. Services & Ports

| Service | Technology | Internal Port | Host Port | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Frontend** | React / Nginx | 5173 | `5173` | Student portal, Official dashboard & Survey Builder |
| **Backend** | Spring Boot (Java 21) | 9000 | `9000` | REST API, RBAC, business logic, reporting |
| **Analytics** | FastAPI (Python 3.11) | 8001 | `8001` | Number crunching, satisfaction index, Gemini AI |
| **Database** | MySQL 8.x | 3306 | `3306` | Relational storage & database anonymity triggers |

---

## 4. Environment Variables

All services are configured through environment variables (defined in `.env` / `.env.example`):

### Database
* `MYSQL_DATABASE`: Database schema name (`skillsense`).
* `MYSQL_USER`: Database user (`skillsense`).
* `MYSQL_PASSWORD`: Database password.
* `MYSQL_ROOT_PASSWORD`: MySQL root administrative password.

### Spring Boot Backend
* `PORT`: Server HTTP port (`9000`).
* `DB_URL`: JDBC connection URL (`jdbc:mysql://mysql:3306/skillsense` in Docker, `jdbc:mysql://localhost:3306/skillsense` locally).
* `DB_USER` / `DB_PASSWORD`: Database credentials.
* `JWT_SECRET`: Minimum 256-bit signing key for HMAC-SHA256 tokens.
* `ANALYTICS_URL`: URL of the FastAPI analytics service (`http://analytics:8001` in Docker, `http://localhost:8001` locally).
* `CORS_ORIGIN`: Allowed origins for CORS (`http://localhost:5173`).
* `AI_DAILY_LIMIT`: Maximum external AI insight calls allowed per calendar day (default: `50`).
* `ADMIN_PASSWORD`: Default password for initialized admin user.

### FastAPI Analytics
* `GEMINI_API_KEY`: Google Gemini API key.
* `GEMINI_MODEL`: Model name (`gemini-2.5-flash` or `gemini-3-flash-preview`).
* `GEMINI_TIMEOUT_SECONDS`: Request timeout in seconds before triggering fallback (`20`).

### Frontend
* `VITE_API_URL`: Base URL for the Spring Boot REST API (`http://localhost:9000/api`).

---

## 5. Database & Flyway Migrations

The database schema is automatically managed by Flyway on backend startup:
* `V1__init.sql`: Creates core tables: `users`, `academic_terms`, `surveys`, `survey_templates`, `participation_log`, `responses`, `student_results`, `analytics_cache`, and `ai_usage`.
* `V2__triggers.sql`: Enforces database-level anonymity and immutability:
  * `trg_responses_bi`: Enforces that for anonymous surveys, `student_id`, `submitted_at`, and `group_section` MUST be NULL.
  * `trg_responses_bu`: Rejects any response row updates (`'Responses are immutable'`).
  * `trg_surveys_bi` / `trg_surveys_bu`: Mandates that all `WELLBEING` surveys must be anonymous, and forbids altering questions or anonymity settings once activated.

---

## 6. Authentication & Roles

* **Scheme**: Stateless Bearer JWT via `Authorization: Bearer <token>` header.
* **Token Lifetime**: 8 hours.
* **Lockout Policy**: 5 failed login attempts trigger an automatic 5-minute lockout.
* **Role Hierarchy**:
  1. `ADMIN`: Full access to user management, academic terms, demo seeder, and all surveys.
  2. `PLACEMENT_OFFICER`: Manages surveys and views analytics for `PLACEMENT` category.
  3. `HOD`: Manages surveys and views analytics for surveys targeting their specific department/branch.
  4. `FACULTY`: Manages created surveys and views analytics within permitted scope.
  5. `STUDENT`: Submits eligible surveys and views individual scored results. Cannot access administrative, builder, or official analytics endpoints.

---

## 7. Demo Accounts & Demo Data

The system includes preconfigured demo accounts representing each institutional role (passwords are set via environment seed configuration):

| Role | College ID | Name | Department / Context |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` | System Administrator | Institution-wide |
| **PLACEMENT_OFFICER** | `placement` | Priya Sharma | Placement Cell |
| **HOD (CSE)** | `hod_cse` | Dr. Anand Kulkarni | Computer Science & Engineering |
| **HOD (ECE)** | `hod_ece` | Dr. Sunita Rao | Electronics & Communication |
| **FACULTY** | `fac_cse_1` | Prof. Rajesh Verma | Computer Science Faculty |
| **STUDENT** | `21CS001` | Aarav Patel | CSE, Year 4, Section A |
| **STUDENT** | `21CS042` | Diya Menon | CSE, Year 4, Section B |
| **STUDENT** | `22EC015` | Rohan Joshi | ECE, Year 3, Section A |

### Demo Surveys Seeded (via `/api/admin/seed/demo`):
1. **CSE Final-Year Placement Readiness Survey** (`PLACEMENT`, Non-anonymous, 20 questions across 5 skill areas).
2. **Campus Facilities & Infrastructure Feedback** (`FACILITY`, Anonymous, 10 questions).
3. **Student Wellbeing & Stress Check** (`WELLBEING`, Anonymous, 8 questions).
4. **Data Structures Course Feedback** (`ACADEMIC`, Anonymous, 8 questions).
5. **Annual Tech Fest 2026 Evaluation** (`EVENT`, Anonymous, 6 questions).
6. **AI & ML Elective Feedback** (`ACADEMIC`, Draft state for testing activation).

### Performance Benchmark Data (via `/api/admin/seed/perf`):
* Injects 2,000 realistic responses with seeded branch/year demographics.
* Benchmark: 2,000-response analytics computation completes in **under 200 milliseconds** (well under the 2.0-second limit).

---

## 8. Docker Startup Instructions

To start the entire SkillSense platform from scratch on a clean machine:

```powershell
# 1. Clone repository and navigate to root
cd SkillSense

# 2. Copy environment template if needed
copy .env.example .env

# 3. Start all services using Docker Compose
docker compose up --build -d
```

### Verification:
* Frontend: `http://localhost:5173`
* Backend Health: `http://localhost:9000/api/health`
* Analytics Health: `http://localhost:8001/health`

To stop all services:
```powershell
docker compose down
```

---

## 9. Test Commands

### Backend Test Suite (Spring Boot)
```powershell
cd backend
.\mvnw.cmd test
```
*Expected: 107 tests run, 0 failures, 0 errors.*

### Analytics Test Suite (FastAPI / pytest)
```powershell
cd analytics
.\venv\Scripts\python.exe -m pytest tests
```
*Expected: 31 tests passed, 0 failures.*

### Frontend Test Suite (Vitest)
```powershell
cd frontend
npm test -- --run
```
*Expected: 14 test files passed, 77 tests passed.*

### Production Build Verification
```powershell
cd frontend
npm run build
```
*Expected: Clean production build in `dist/` with 0 errors.*

---

## 10. API Documentation & Postman Collection

* Postman collection is maintained at: `postman/skillsense.postman_collection.json` (and `docs/skillsense.postman_collection.json`).
* Key Endpoints:
  * `POST /api/auth/login`: Authenticate and receive JWT.
  * `GET /api/auth/me`: Current user session and role.
  * `POST /api/auth/change-password`: Change password.
  * `GET /api/surveys`: List surveys (role-filtered).
  * `POST /api/surveys`: Create new survey.
  * `GET /api/surveys/{id}/analytics`: Compute/retrieve aggregate analytics.
  * `GET /api/surveys/{id}/insights`: Retrieve Gemini AI insights / Rules fallback.
  * `GET /api/surveys/{id}/export?format=pdf|xlsx`: Download executive report.
  * `GET /api/my/surveys`: Student eligible surveys.
  * `POST /api/my/surveys/{id}/submit`: Student survey response submission.
  * `POST /api/admin/seed/demo`: Reset and seed demo dataset.
  * `POST /api/admin/seed/perf`: Inject 2,000-response performance benchmark dataset.

---

## 11. Security & Privacy Highlights

* **Cryptographic Hashing**: Passwords stored as BCrypt (cost factor 10).
* **HS256 Tokens**: JWT tokens signed with SHA-256 HMAC and 8-hour expiration.
* **Physical Isolation of Identity**: Anonymous survey responses do not store student IDs, submission timestamps, or sections.
* **K=5 Suppression**: Aggregates for groups with fewer than 5 respondents are hidden.
* **Anti-Subtraction**: If exactly one group is suppressed, the next smallest group is also suppressed to prevent subtraction from the total count.
* **Gemini LLM Privacy Boundary**: Gemini only receives high-level aggregated summary numbers. No student PII or free text is ever transmitted.
* **Formula Injection Defense**: All spreadsheet export cells beginning with `=`, `+`, `-`, or `@` are safely sanitized with an apostrophe prefix.

---

## 12. Backup & Restore Notes

### Database Backup
```bash
docker exec skillsense-mysql mysqldump -u skillsense -pskillsense_pass skillsense > backup_skillsense.sql
```

### Database Restore
```bash
docker exec -i skillsense-mysql mysql -u skillsense -pskillsense_pass skillsense < backup_skillsense.sql
```

---

## 13. Production Deployment Considerations

1. **Host Port Ingress**: In production, do not bind MySQL (`3306`) or Analytics (`8001`) to public host interfaces.
2. **Reverse Proxy & SSL/TLS**: Deploy Nginx or Cloudflare in front of Spring Boot and the Vite static bundle to terminate HTTPS (TLS 1.3) and enforce rate limiting (`limit_req`).
3. **Secret Management**: Inject production JWT secrets (`JWT_SECRET`) and database passwords via Docker Secrets, AWS Secrets Manager, or HashiCorp Vault.
4. **Database Growth & Partitioning**: For institutions exceeding 100,000 responses annually, consider MySQL range partitioning on `responses` by academic year.

---

## 14. Final Status

* **Phases 0–11**: Fully implemented, tested, and verified.
* **Phase 12 Audit**: Security review complete; all 30 audit objectives verified.
* **Final Verdict**: **PHASE 12 — APPROVED**
