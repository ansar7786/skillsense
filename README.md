# SkillSense

SkillSense is a unified survey and insights platform for colleges. It collects responses across various survey categories, calculates readiness scores and skill gaps, and produces AI-driven recommendations.

This repository is organized as a monorepo containing the following components:
- **MySQL 8**: Database service managed via Docker Compose.
- **Backend**: Spring Boot 3.3 (Java 21, Maven) REST API.
- **Analytics**: Stateless FastAPI Python service for numerical calculations and AI insights.
- **Frontend**: React 18 single-page application built with Vite.

---

## Prerequisites

- **Java**: JDK 21+
- **Maven**: 3.9+ (or use the included Maven wrapper `backend\mvnw.cmd`)
- **Python**: 3.11+ (Python 3.11–3.14 supported)
- **Node.js**: v18+ and `npm`
- **Docker Desktop**: For running MySQL 8 container

---

## Environment Setup

1. Copy `.env.example` to `.env` in the repository root:
```powershell
Copy-Item .env.example .env
```

2. Also verify frontend environment variables in `frontend\.env`:
```powershell
VITE_API_URL=http://localhost:9000/api
```

---

## Running Services (Windows PowerShell)

### 1. MySQL Database

Start the MySQL container in the background:
```powershell
docker compose up -d mysql
```

Check health status:
```powershell
docker compose ps
```

To stop MySQL:
```powershell
docker compose down
```

---

### 2. Spring Boot Backend

From the repository root or `backend` folder:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
*(Alternatively, if Maven is in your PATH: `mvn spring-boot:run`)*

The backend server starts on port `9000`:
```text
http://localhost:9000
```

Verify backend health:
```powershell
Invoke-RestMethod -Uri "http://localhost:9000/api/health"
```
Expected response:
```json
{"status":"ok"}
```

---

### 3. FastAPI Analytics Service

From the repository root or `analytics` folder:
```powershell
cd analytics
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

The analytics service starts on:
```text
http://localhost:8001
```

Verify analytics health:
```powershell
Invoke-RestMethod -Uri "http://localhost:8001/health"
```
Expected response:
```json
{"status":"ok"}
```

Run tests:
```powershell
cd analytics
.\venv\Scripts\pytest
```

---

### 4. React Frontend

From the repository root or `frontend` folder:
```powershell
cd frontend
npm install
npm run dev
```

The frontend development server starts on:
```text
http://localhost:5173
```

Open `http://localhost:5173` in a browser. When the backend is running, the page will display:
```text
backend: ok
```

---

## Phase 0 Health Checks Summary

| Service | Port | Endpoint | Expected Output |
| :--- | :--- | :--- | :--- |
| **MySQL** | `3306` | Docker healthcheck | healthy |
| **Backend** | `9000` | `GET http://localhost:9000/api/health` | `{"status":"ok"}` |
| **Analytics** | `8001` | `GET http://localhost:8001/health` | `{"status":"ok"}` |
| **Frontend** | `5173` | `http://localhost:5173` | Displays `backend: ok` |
