# SkillSense — Security Review & Hardening Report
**Phase 12 Final Security Audit**

---

## 1. Executive Summary

SkillSense has undergone a comprehensive, multi-layered security audit covering authentication, role-based access control (RBAC), data anonymity and K=5 suppression, Gemini AI privacy boundaries, API input validation, database integrity, export safety, secrets management, Docker container isolation, and dependency health.

**Overall Security Status: PASS**

The system enforces strict zero-trust boundaries between client roles, insulates student identity from anonymous survey responses at both the application and database levels, and provides hardened isolation between the browser, internal analytics, and external LLM services. All automated test suites (107 backend tests, 31 analytics tests, 77 frontend tests) pass with 0 failures, and production builds compile cleanly.

---

## 2. Authentication

### Findings and Status: PASS
* **Password Hashing**: Passwords are cryptographically hashed using Spring Security `BCryptPasswordEncoder` with a strength work-factor of 10 (`SecurityConfig.java`). Plaintext passwords are never stored in the database or serialized in any API response.
* **Token Standard & Lifetime**: Authentication uses JWT tokens signed with HMAC-SHA256 (`HS256`) with a minimum 256-bit key (`JwtUtil.java`). Token lifetime is strictly enforced at 8 hours (28,800,000 ms).
* **Brute-Force Protection**: `LoginLockoutService.java` tracks failed login attempts per account. If 5 consecutive failed attempts occur, the account is locked for 5 minutes (`ACCOUNT_LOCKED`, HTTP 401). Successful login clears the attempt count.
* **Account Status**: Disabled/deactivated accounts (`isActive == false`) are rejected at login (`FORBIDDEN`, HTTP 403).
* **Password Change & First-Login Flow**: The system flags newly seeded or created accounts with `mustChangePassword = true`. The user is forced to `/change-password` before accessing application functionality. Changing password requires verifying the current password and forbids reusing the identical password.
* **Token Validation**: Expired, malformed, or tampered JWTs trigger HTTP 401 (`UNAUTHENTICATED`) through the custom `AuthenticationEntryPoint`.

---

## 3. Authorization (RBAC)

### Findings and Status: PASS
* **Method & Route Protection**: Spring Security method security (`@EnableMethodSecurity`) enforces authorization at the controller layer via `@PreAuthorize`.
* **Role Partitioning**:
  * `ADMIN`: Can manage users (`/api/users`), academic terms (`/api/terms`), view any survey, and trigger demo seeding.
  * `PLACEMENT_OFFICER`: Can create/manage surveys and view analytics strictly for `PLACEMENT` category surveys (or surveys they created).
  * `HOD`: Can only access and view analytics for surveys targeting their specific assigned branch or college-wide (`ALL`) surveys.
  * `FACULTY`: Can only manage surveys and view analytics for surveys they personally created or where scope permits.
  * `STUDENT`: Restricted exclusively to `/api/my/surveys/*`. Students cannot access survey builder endpoints (`/api/surveys`), official analytics (`/api/surveys/{id}/analytics`), dashboard summaries, or administrative APIs.
* **IDOR Mitigation**: Direct object reference attacks (e.g., student attempting to query or submit another student's survey, or an official attempting to view another branch's analytics) are intercepted at both controller and service levels (`assertCanViewSurvey`, `assertCanViewAnalytics`), returning HTTP 403 `FORBIDDEN` or HTTP 404 `NOT_FOUND`.
* **Survey Schema Sanitization**: When students fetch an active survey for answering, `ResponseService.sanitizeQuestionSchema` strips all scoring definitions, question weights, skill metadata, and option scores to prevent client-side answer inspection.

---

## 4. Privacy & Anonymity

### Findings and Status: PASS
* **Section 5 Anonymity Guarantees**:
  * For anonymous surveys (`is_anonymous = true`), `ResponseService` sets `student_id = NULL`, `submitted_at = NULL`, and `group_section = NULL` upon submission.
  * The response record contains only `group_branch`, `group_year`, and the answers payload (`answer_data`).
* **Database Triggers**:
  * `trg_responses_bi` BEFORE INSERT trigger immediately rejects with SQLSTATE `45000` (`'Anonymous survey: identity, time and section must be NULL'`) if any non-null student ID, timestamp, or section is supplied.
  * `trg_responses_bu` BEFORE UPDATE trigger rejects with SQLSTATE `45000` (`'Responses are immutable'`), preventing retroactive modification.
  * `trg_surveys_bi` & `trg_surveys_bu` ensure any survey categorized as `WELLBEING` must be anonymous (`is_anonymous = true`), and anonymity cannot be changed after activation.
* **Separation of Participation Logs**:
  * The `participation_log` table tracks `(survey_id, student_id, has_responded)` to prevent duplicate voting.
  * It maintains no foreign key, relation, or pointer to the anonymous response record in `responses`, rendering deanonymization via join impossible.
* **K=5 Suppression**:
  * Group breakdown analytics (by branch and study year) hide any group with fewer than 5 responses (`n < 5`).
* **Anti-Subtraction Protection**:
  * In `analytics/app/aggregate.py` (`apply_k_suppression`), if exactly ONE group is hidden due to `n < 5`, the next smallest visible group is also suppressed to prevent determining the hidden count by subtracting visible counts from the known total.
* **Free-Text Privacy**:
  * Free-text responses (`GET /api/surveys/{id}/texts`) are suppressed if fewer than 5 students submitted text answers for that question.
  * If returned, text responses are shuffled randomly (`Collections.shuffle`) to detach sequence order from any chronological log.

---

## 5. Gemini / AI Privacy Boundary

### Findings and Status: PASS
* **Aggregated Data Only**: In `analytics/app/insights.py` (`build_user_message`), Gemini receives ONLY a sanitized JSON block containing aggregated percentages, means, rating counts, and pre-suppressed group figures.
* **No PII Transmission**:
  * Student names, roll numbers, student IDs, emails, sections, and individual submission rows are NEVER sent to Gemini.
  * Free-text student answers are strictly omitted from the Gemini prompt.
  * Groups suppressed under K=5 are stripped prior to prompt serialization.
* **Key Isolation**: The Gemini API key (`GEMINI_API_KEY`) is stored exclusively in backend/analytics server environments. It is never exposed in client bundles, Vite configs, or network responses.
* **Fallback Guarantee**: If the Gemini API is unreachable, times out (20s limit), encounters rate limits, or exceeds daily quotas (50 calls/day), the service automatically falls back to deterministic, rule-based insight generation (`source: "RULES"`).

---

## 6. API Security & Error Handling

### Findings and Status: PASS
* **Input Validation**: Request bodies are validated using Bean Validation (`@Valid`, `@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`).
* **Question Schema Validation**: `QuestionSchemaValidator` validates question structure, rating bounds, choice option keys, duplicate keys, and scoring parameters.
* **Answer Validation**: `AnswerValidator` validates incoming student submissions against question types, allowed choices, and required constraints.
* **Information Disclosure Prevention**: `GlobalExceptionHandler.java` catches all exceptions (`ApiException`, `MethodArgumentNotValidException`, `AccessDeniedException`, `AuthenticationException`, `Exception`) and maps them into standard `ErrorResponse` payloads containing status, error code, and friendly message. Stack traces, SQL statements, and internal server paths are never returned to clients.

---

## 7. Export Security

### Findings and Status: PASS
* **Scope Enforcement**: Both PDF and Excel export endpoints (`/api/surveys/{id}/export`) require full analytics authorization (`assertCanViewAnalytics`) before generation.
* **Anonymous Export Privacy**: Exported documents for anonymous surveys present only aggregate stats, skill gap analyses, and trend summaries; no individual student rows or PII are written.
* **Formula Injection (CSV/XLSX Injection)**: In `ReportExportService.java`, all user-supplied text strings written to Excel cells pass through `escapeFormula`. Any text starting with `=`, `+`, `-`, or `@` is prepended with a single quote (`'`), neutralising spreadsheet formula execution in Excel/Calc.

---

## 8. Database Security

### Findings and Status: PASS
* **Parameterized Queries**: All queries are executed through Spring Data JPA repositories with parameterized query bindings (`@Query`), eliminating SQL injection risks.
* **Transactions & Atomicity**: Critical state updates (response submission and duplicate marking in `markResponded`) run in `@Transactional` boundaries.
* **Flyway Migration**: Database schemas and security triggers are managed via Flyway versioned migrations (`V1__init.sql`, `V2__triggers.sql`), preventing unmonitored schema drift.

---

## 9. Docker & Container Security

### Findings and Status: PASS
* **Non-Root Execution**: The backend Spring Boot Dockerfile defines an unprivileged system user (`skillsense`) and runs via `USER skillsense`.
* **Healthchecks**: MySQL, FastAPI analytics, and Spring Boot backend containers implement automated container health checks in `docker-compose.yml`.
* **Internal Network Communication**: Backend communicates with MySQL (`mysql:3306`) and Analytics (`analytics:8001`) via internal Docker container names.
* **JVM Container Optimization**: Backend Dockerfile specifies `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` to prevent OOM errors inside cgroups.

---

## 10. Secrets Audit

### Findings and Status: PASS
* **Repository State**: No live API keys, JWT production secrets, or private certificates are committed to Git.
* **Git Ignore**: `.env`, `.env.local`, `target/`, `node_modules/`, `dist/`, and Python virtual environments are explicitly ignored in `.gitignore`.
* **Configuration Defaults**: `.env.example` provides non-sensitive placeholders (`your_gemini_api_key_here`, dummy JWT secret strings).

---

## 11. Dependency Audit

### Findings and Status: PASS
* **Backend (`pom.xml`)**: Spring Boot 3.3.4, Java 21, JJWT 0.12.6, Apache POI 5.3.0, OpenPDF 2.0.3, Flyway 10.x. No deprecated or unmaintained libraries.
* **Analytics (`requirements.txt`)**: Python 3.11/3.14 compatible, FastAPI 0.115+, Pydantic 2.8+, google-genai 0.1+, Pandas 2.2+. Clean dependency tree.
* **Frontend (`package.json`)**: React 18.3.1, Vite 5.4.8, React Router 6.26.2, Chart.js 4.4.4, Axios 1.7.7. Modern, production-ready frontend bundle.

---

## 12. Testing Evidence

All automated suites pass with 0 errors and 0 failures:

| Suite | Component | Tests Run | Passed | Failed | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Backend** | Spring Boot / JUnit 5 | 107 | 107 | 0 | **PASS** |
| **Analytics** | FastAPI / Pytest | 31 | 31 | 0 | **PASS** |
| **Frontend** | Vitest / Testing Library | 77 | 77 | 0 | **PASS** |
| **Production Build** | Vite Build | 126 modules | 126 modules | 0 | **PASS** |

---

## 13. Remaining Risks & Mitigations

1. **Local Host Port Exposure for Testing**:
   * *Risk*: In `docker-compose.yml`, port `8001:8001` (FastAPI) and `3306:3306` (MySQL) are exposed to localhost for developer accessibility and debugging.
   * *Mitigation*: In production deployments, remove host port bindings for `analytics` and `mysql`, restricting ingress solely to Nginx / Frontend (`80/443`) and Backend API (`9000`).
2. **Reverse Proxy Rate Limiting**:
   * *Risk*: While login brute-force protection (5 attempts / 5 minutes) is enforced in application code, general API volumetric rate limiting is not implemented at the application tier.
   * *Mitigation*: Deploy Nginx or an API Gateway with `limit_req` zones in front of Spring Boot in public production environments.

---

## Conclusion
The SkillSense system meets and verifies all security, privacy, and architectural requirements set forth in `docs/SKILLSENSE_SPEC.md`.

**FINAL STATUS: PHASE 12 — APPROVED**
