# SkillSense

SkillSense is a unified survey and student analytics platform for engineering institutions. It collects student feedback across academic, placement readiness, facility, event, and wellbeing categories, computes objective readiness scores and skill gap analyses, provides K=5 anonymity guarantees, and synthesizes executive recommendations via Gemini AI (with a zero-dependency rules engine fallback).

---

## Architecture & Monorepo Structure

- **Database**: MySQL 8.4 with BEFORE INSERT and BEFORE UPDATE anonymity & immutability triggers.
- **Backend (`backend/`)**: Spring Boot 3.3.4 (Java 21, Maven) REST API, JWT auth, RBAC, JPA/Flyway, Apache POI / OpenPDF reporting.
- **Analytics (`analytics/`)**: FastAPI (Python 3.11+, Pandas, Uvicorn) microservice for stats aggregation, satisfaction index, and Gemini LLM insights.
- **Frontend (`frontend/`)**: React 18, Vite, React Router 6, Chart.js, mobile-responsive accessible UI.
- **Documentation (`docs/`)**: Complete specification, deployment & handover manual, security review, performance benchmarks, deviations log, and Postman collection.

---

## Quick Start (Docker Compose — Recommended)

Start the complete application stack (MySQL, Spring Boot Backend, FastAPI Analytics, and Nginx Frontend) with one command:

```powershell
# 1. Copy environment template
Copy-Item .env.example .env

# 2. Build and start all 4 services in the background
docker compose up --build -d
```

### Accessing the Platform

| Service | URL / Port | Credentials / Purpose |
| :--- | :--- | :--- |
| **Frontend Application** | [http://localhost:5173](http://localhost:5173) | Single-page student & official app |
| **Spring Boot REST API** | [http://localhost:9000/api](http://localhost:9000/api) | Health: `GET http://localhost:9000/api/health` |
| **FastAPI Analytics** | [http://localhost:8001](http://localhost:8001) | Health: `GET http://localhost:8001/health` |
| **MySQL Database** | `localhost:3306` | User: `skillsense`, Database: `skillsense` |

To stop all services:
```powershell
docker compose down
```

---

## Local Development (Without Docker for App Services)

### Prerequisites

- **Java**: JDK 21+
- **Maven**: 3.9+ (or use `backend\mvnw.cmd`)
- **Python**: 3.11+
- **Node.js**: v18+ and `npm`
- **Docker Desktop**: For running the MySQL 8 container

### 1. Environment Setup

```powershell
Copy-Item .env.example .env
```

### 2. Start MySQL

```powershell
docker compose up -d mysql
```

### 3. Start Spring Boot Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
Server runs on `http://localhost:9000`. Health check: `GET http://localhost:9000/api/health`.

### 4. Start FastAPI Analytics Service

```powershell
cd analytics
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```
Server runs on `http://localhost:8001`. Health check: `GET http://localhost:8001/health`.

### 5. Start React Frontend

```powershell
cd frontend
npm install
npm run dev
```
Development server runs on `http://localhost:5173`.

---

## Running Test Suites

### Backend Tests (JUnit 5, Spring Boot Test)
```powershell
cd backend
.\mvnw.cmd test
```

### Analytics Tests (FastAPI / pytest)
```powershell
cd analytics
.\venv\Scripts\pytest
```

### Frontend Tests (Vitest & React Testing Library)
```powershell
cd frontend
npm test -- --run
```

### Production Build Verification
```powershell
cd frontend
npm run build
```

---

## Demo Accounts & Seed Profiles

The database can be seeded automatically via admin endpoints:

### Seeder Endpoints (Admin role required)
- **Demo Profile**: `POST http://localhost:9000/api/admin/seed/demo`  
  Seeds 125 users, 2 academic terms, and 6 surveys (Placement v1 [85 responses], Placement v2 [70 responses], Course Feedback DBMS [60 responses, IT n=3 suppressed], Hostel & Facility [90 responses], Wellbeing Pulse [4 responses], and Tech Fest [Draft]).
- **Performance Benchmark**: `POST http://localhost:9000/api/admin/seed/perf`  
  Injects 2,000 anonymous responses and executes live cold/warm benchmark latency measurements.

### Default Demo Accounts (Password: `Demo@1234`)

| Role | College ID | Context / Department |
| :--- | :--- | :--- |
| **ADMIN** | `admin` | Institutional Administrator |
| **PLACEMENT_OFFICER** | `po01` | Placement & Career Cell |
| **HOD** | `hod_cse` | Head of Computer Science & Engineering |
| **FACULTY** | `fac01`, `fac02` | Teaching Faculty |
| **STUDENT** | `21CSE001` – `21CSE060` | CSE Students (Years 2–4, Sections A & B) |
| **STUDENT** | `21IT001` – `21IT030` | IT Students |
| **STUDENT** | `21ECE001` – `21ECE030` | ECE Students |

---

## Documentation Index

- [`docs/SKILLSENSE_SPEC.md`](docs/SKILLSENSE_SPEC.md): Complete product design and system specification.
- [`docs/HANDOVER.md`](docs/HANDOVER.md): Phase 12 deployment, operations, and system handover guide.
- [`docs/SECURITY_REVIEW.md`](docs/SECURITY_REVIEW.md): Exhaustive security checklist and audit findings.
- [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md): 2,000-cohort empirical benchmark measurements.
- [`docs/DEVIATIONS.md`](docs/DEVIATIONS.md): Architectural deviations and rationale log.
- [`postman/skillsense.postman_collection.json`](postman/skillsense.postman_collection.json): Complete Postman API collection.

