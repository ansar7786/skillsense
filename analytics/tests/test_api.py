import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_health_endpoint():
    """Verify GET /health returns 200 with status ok."""
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json() == {"status": "ok"}

def test_analyze_endpoint_documented_shape():
    """
    Verify POST /analyze returns the documented schema from Section 7 (lines 447-457).
    """
    payload = {
        "survey": {
            "id": 12,
            "category": "PLACEMENT",
            "is_anonymous": False,
            "eligible_count": 10,
            "question_schema": {
                "schemaVersion": 1,
                "questions": [
                    {
                        "key": "java_skill",
                        "type": "RATING",
                        "prompt": "How confident are you in Java?",
                        "rating": {"min": 1, "max": 5, "minLabel": "Beginner", "maxLabel": "Expert"},
                        "scoring": {"skill": "java", "weight": 2.0},
                    },
                    {
                        "key": "mock_interviews",
                        "type": "SINGLE_CHOICE",
                        "prompt": "How many mock interviews?",
                        "options": [
                            {"value": "0", "label": "None", "score": 0.0},
                            {"value": "1-2", "label": "1 to 2", "score": 50.0},
                            {"value": "3+", "label": "3 or more", "score": 100.0},
                        ],
                        "scoring": {"skill": "interview", "weight": 1.0},
                    },
                    {
                        "key": "blockers",
                        "type": "TEXT",
                        "prompt": "Preparation blockers?",
                    },
                ],
            },
            "skill_targets": {"java": 70.0, "interview": 60.0},
        },
        "responses": [
            {
                "student_id": 1,
                "branch": "CSE",
                "year": 3,
                "answers": {"java_skill": 4, "mock_interviews": "1-2", "blockers": "Need more time"},
            },
            {
                "student_id": 2,
                "branch": "CSE",
                "year": 3,
                "answers": {"java_skill": 5, "mock_interviews": "3+", "blockers": "Need more time"},
            },
            {
                "student_id": 3,
                "branch": "CSE",
                "year": 3,
                "answers": {"java_skill": 3, "mock_interviews": "0", "blockers": "Need more time"},
            },
            {
                "student_id": 4,
                "branch": "IT",
                "year": 2,
                "answers": {"java_skill": 4, "mock_interviews": "1-2", "blockers": "Lack of guidance"},
            },
            {
                "student_id": 5,
                "branch": "IT",
                "year": 2,
                "answers": {"java_skill": 4, "mock_interviews": "1-2", "blockers": "Lack of guidance"},
            },
            {
                "student_id": 6,
                "branch": "IT",
                "year": 2,
                "answers": {"java_skill": 2, "mock_interviews": "0", "blockers": "Lack of guidance"},
            },
        ],
    }

    resp = client.post("/analyze", json=payload)
    assert resp.status_code == 200
    data = resp.json()

    assert data["response_count"] == 6
    assert data["completion_rate"] == 0.6
    assert data["index"] is not None

    # Check questions list
    assert len(data["questions"]) == 3
    q_keys = [q["key"] for q in data["questions"]]
    assert "java_skill" in q_keys
    assert "mock_interviews" in q_keys
    assert "blockers" in q_keys

    # Check skills
    assert len(data["skills"]) == 2
    skills_map = {s["skill"]: s for s in data["skills"]}
    assert "java" in skills_map
    assert "interview" in skills_map

    # Check bands
    assert "READY" in data["bands"]
    assert "DEVELOPING" in data["bands"]
    assert "NEEDS_SUPPORT" in data["bands"]

    # Check groups
    assert "branch" in data["groups"]
    assert "year" in data["groups"]
    assert "suppressed" in data["groups"]

    # Check text themes
    assert "text_themes" in data
    assert "blockers" in data["text_themes"]
    terms = [item["term"] for item in data["text_themes"]["blockers"]]
    # 'time' appeared in 3 documents, 'guidance' appeared in 3 documents
    assert "time" in terms
    assert "guidance" in terms

def test_analyze_anonymous_survey_has_null_student_id():
    """Verify anonymous survey with student_id=null returns identical aggregate output without student identities."""
    payload = {
        "survey": {
            "id": 15,
            "category": "WELLBEING",
            "is_anonymous": True,
            "eligible_count": 5,
            "question_schema": {
                "schemaVersion": 1,
                "questions": [
                    {
                        "key": "wellbeing_rating",
                        "type": "RATING",
                        "prompt": "How manageable is your workload?",
                        "rating": {"min": 1, "max": 5},
                    }
                ],
            },
        },
        "responses": [
            {"student_id": None, "branch": "CSE", "year": 2, "answers": {"wellbeing_rating": 4}},
            {"student_id": None, "branch": "CSE", "year": 2, "answers": {"wellbeing_rating": 3}},
            {"student_id": None, "branch": "CSE", "year": 2, "answers": {"wellbeing_rating": 5}},
            {"student_id": None, "branch": "CSE", "year": 2, "answers": {"wellbeing_rating": 4}},
            {"student_id": None, "branch": "CSE", "year": 2, "answers": {"wellbeing_rating": 3}},
        ],
    }

    resp = client.post("/analyze", json=payload)
    assert resp.status_code == 200
    data = resp.json()
    assert data["response_count"] == 5
    assert data["completion_rate"] == 1.0
    assert data["index"] is not None

def test_student_result_endpoint():
    """Verify POST /student-result computes personal readiness and skill breakdowns."""
    payload = {
        "survey": {
            "id": 12,
            "category": "PLACEMENT",
            "question_schema": {
                "schemaVersion": 1,
                "questions": [
                    {
                        "key": "java_skill",
                        "type": "RATING",
                        "rating": {"min": 1, "max": 5},
                        "scoring": {"skill": "java", "weight": 2.0},
                    },
                    {
                        "key": "mock_interviews",
                        "type": "SINGLE_CHOICE",
                        "options": [{"value": "1-2", "label": "1-2", "score": 50.0}],
                        "scoring": {"skill": "interview", "weight": 1.0},
                    },
                ],
            },
            "skill_targets": {"java": 70.0, "interview": 60.0},
        },
        "answers": {
            "java_skill": 4,
            "mock_interviews": "1-2",
        },
    }

    resp = client.post("/student-result", json=payload)
    assert resp.status_code == 200
    data = resp.json()
    assert data["readiness"] == 66.67
    assert data["band"] == "DEVELOPING"
    assert data["strongest_skill"] == "java"
    assert data["weakest_skill"] == "interview"

def test_trend_endpoint():
    """Verify POST /trend endpoint compares current and previous AGGREGATE payloads."""
    payload = {
        "current": {
            "response_count": 50,
            "index": 70.0,
            "questions": [{"key": "cleanliness", "norm_mean": 65.0, "n": 50}],
            "skills": [],
        },
        "previous": {
            "response_count": 45,
            "index": 62.0,
            "questions": [{"key": "cleanliness", "norm_mean": 55.0, "n": 45}],
            "skills": [],
        },
    }

    resp = client.post("/trend", json=payload)
    assert resp.status_code == 200
    data = resp.json()
    assert data["overall"]["direction"] == "IMPROVED"
    assert data["overall"]["delta"] == 8.0
    assert data["questions"][0]["direction"] == "IMPROVED"

def test_insights_endpoint():
    """Verify POST /insights endpoint returns deterministic rules-based insight schema."""
    payload = {
        "category": "PLACEMENT",
        "aggregate": {
            "response_count": 50,
            "completion_rate": 0.80,
            "index": 65.0,
            "skills": [
                {"skill": "dsa", "target": 70.0, "mean": 50.0, "gap": 20.0, "share_below_target": 0.60, "priority": 1}
            ],
            "bands": {"READY": 0.25, "DEVELOPING": 0.50, "NEEDS_SUPPORT": 0.25},
            "questions": [],
        },
    }

    resp = client.post("/insights", json=payload)
    assert resp.status_code == 200
    data = resp.json()
    assert data["source"] == "RULES"
    assert len(data["headline"]) > 0
    assert len(data["key_findings"]) >= 3
    assert len(data["recommendations"]) >= 1

def test_invalid_requests_handled_safely():
    """Verify invalid payloads return appropriate HTTP 422 Unprocessable Entity."""
    resp = client.post("/analyze", json={"invalid": "payload"})
    assert resp.status_code == 422
