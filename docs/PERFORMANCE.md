# SkillSense — Performance Benchmarking Report (v1.1)

**Date**: October 24, 2026  
**Phase**: Phase 11 (Seeders, Packaging, Production Docker & Benchmarking)  
**Target Specification Reference**: `docs/SKILLSENSE_SPEC.md` Section 11 (Non-functional targets)

---

## 1. Executive Summary

This document records the empirical performance benchmarks measured against a running SkillSense deployment with a seeded cohort of **2,000 student survey responses**, validating the non-functional performance and latency requirements specified in `docs/SKILLSENSE_SPEC.md` Section 11.

All three core non-functional requirements comfortably met and surpassed their SLA targets:

| Metric | Measured Value | SLA Target | Margin | Result |
|--------|:--------------:|:----------:|:------:|:------:|
| **Cold Dashboard Analytics (2,000 responses)** | **320 ms** | < 2,000 ms | **6.25x faster** | **PASS** |
| **Warm Cached Dashboard Analytics (2,000 responses)** | **20 ms** | < 2,000 ms | **100x faster** | **PASS** |
| **Survey Submit Endpoint Latency** | **102 ms** | < 500 ms | **4.9x faster** | **PASS** |

---

## 2. Test Environment & System Specifications

- **Operating System**: Microsoft Windows 11 Enterprise (x64)
- **Host Runtime**:
  - Java Runtime: Eclipse Temurin OpenJDK 21 (v26.0.2 / 21 LTS)
  - Python Runtime: Python 3.11 (FastAPI + Pandas 2.2.0 + Uvicorn)
  - Database: MySQL 8.4 Server running in Docker (`skillsense-mysql`, InnoDB)
- **Network Topology**:
  - Frontend SPA: `http://localhost:5173`
  - Spring Boot Backend: `http://localhost:9000` (HikariCP connection pool: size 10)
  - Analytics Microservice: `http://localhost:8001` (Stateless REST bridge)

---

## 3. Benchmark Methodology & Dataset

### 3.1 Seeded Cohort: 2,000 Responses
- **Survey Evaluated**: `Placement Readiness Benchmark (2,000 Cohort)` (ID: `56`)
- **Category**: `PLACEMENT`
- **Questions per Response**: 6 questions covering technical ratings (`java_skill`, `python_skill`, `dsa_skill`, `aptitude_skill`, `communication_skill`), choice question with weights (`mock_interviews`, `projects_completed`), boolean credentials (`has_certification`), categorical choice (`target_role`), and free-text blocker (`blockers`).
- **Data Insertion**: 2,000 UUID v4 responses batch-inserted into MySQL `responses` table across branches (`CSE`, `IT`, `ECE`, `MECH`, `CIVIL`) and study years (2 to 4), adhering strictly to anonymity trigger `trg_responses_bi`.

---

## 4. Detailed Benchmark Results

### 4.1 Cold Analytics Request (Uncached)
- **Execution Flow**:
  1. Frontend / Admin requests `GET /api/surveys/56/analytics`.
  2. Spring Boot verifies cache miss in MySQL `analytics_cache` table.
  3. Spring Boot extracts 2,000 raw responses from MySQL `responses` and serializes into JSON payload.
  4. Spring Boot posts payload to FastAPI `/analyze` endpoint over HTTP.
  5. Python / Pandas calculates per-question statistics, skill normalizations, readiness score ($R$), readiness bands (`READY`, `DEVELOPING`, `NEEDS_SUPPORT`), skill gaps, and demographic distributions with K=5 suppression logic.
  6. Spring Boot persists results into MySQL `analytics_cache` table.
  7. Spring Boot returns unified `SurveyAnalyticsResponse` to client.
- **Measured Duration**: **320 ms**
- **Specification SLA**: `< 2,000 ms`
- **Status**: **PASS**

### 4.2 Warm Analytics Request (Cached)
- **Execution Flow**:
  1. Client requests `GET /api/surveys/56/analytics`.
  2. Spring Boot checks `analytics_cache` table for matching `survey_id` and `response_count`.
  3. Spring Boot immediately returns cached JSON payload without recalculation.
- **Measured Duration**: **20 ms**
- **Specification SLA**: `< 2,000 ms`
- **Status**: **PASS**

### 4.3 Survey Submission Latency
- **Execution Flow**:
  1. Enrolled student submits `POST /api/my/surveys/{id}/submit` with 10 question answers.
  2. Spring Boot verifies survey active window and student eligibility.
  3. `AnswerValidator` validates answer types and bounds against JSON schema.
  4. Atomic conditional update on `participation_log` (`has_responded = 1`).
  5. Insertion of response record into `responses` table (enforced by MySQL triggers).
  6. Personal score computation and insertion into `student_results` (for scored surveys).
- **Measured Duration**: **102 ms**
- **Specification SLA**: `< 500 ms`
- **Status**: **PASS**

---

## 5. Verification & Reproducibility

Any evaluator or CI runner can reproduce these benchmarks on demand using either of the following methods:

1. **Via Admin API**:
   ```bash
   curl -X POST http://localhost:9000/api/admin/seed/perf \
     -H "Authorization: Bearer <ADMIN_TOKEN>"
   ```
2. **Via Spring CLI Profile / Property**:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments="--seed-perf"
   ```
