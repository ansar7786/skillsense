import json
import pytest
from unittest.mock import MagicMock, patch
from app.insights import (
    build_user_message,
    generate_insights,
    generate_rule_based_insights,
)
from app.schemas import InsightModel
from app.config import settings

def test_gemini_payload_privacy_boundary():
    """
    CRITICAL PRIVACY TEST:
    Verify build_user_message strictly contains only aggregate statistics.
    MUST NEVER contain student IDs, names, emails, roll numbers,
    individual free text, or suppressed groups.
    """
    aggregate = {
        "response_count": 50,
        "completion_rate": 0.85,
        "index": 72.0,
        "questions": [
            {
                "key": "q1",
                "type": "RATING",
                "prompt": "How satisfied are you with the faculty mentorship program?",
                "norm_mean": 75.0,
                "n": 50,
            },
            {
                "key": "blockers",
                "type": "TEXT",
                "prompt": "Tell us what obstacles you face.",
                "n": 40,
            },
        ],
        "skills": [{"skill": "python", "target": 70.0, "mean": 65.0, "gap": 5.0, "share_below_target": 0.3}],
        "bands": {"READY": 0.4, "DEVELOPING": 0.5, "NEEDS_SUPPORT": 0.1},
        "groups": {
            "branch": {"CSE": {"n": 40, "index": 75.0}},
            "year": {"3": {"n": 40, "index": 75.0}},
            "suppressed": ["MECH", "CIVIL"],
        },
        # Injected sensitive fields that MUST be stripped:
        "student_id": 12345,
        "student_name": "Alice Smith",
        "roll_number": "21CSE999",
        "email": "alice@skillsense.edu",
        "raw_text_answers": ["Prof X was terrible", "Dean Y is unfair"],
    }

    message = build_user_message(aggregate, category="PLACEMENT")

    # Assert delimiter
    assert message.startswith("<survey_data>")
    assert message.endswith("</survey_data>")

    # Assert PII is NOT present anywhere in the Gemini prompt payload
    forbidden_terms = [
        "12345",
        "alice smith",
        "21cse999",
        "alice@skillsense.edu",
        "prof x was terrible",
        "dean y is unfair",
        "mech",
        "civil",
    ]
    message_lower = message.lower()
    for term in forbidden_terms:
        assert term not in message_lower, f"Forbidden term '{term}' leaked into Gemini prompt!"

def test_gemini_fallback_when_allow_ai_is_false():
    """When allow_ai is False, system immediately uses deterministic rules fallback."""
    aggregate = {"response_count": 20, "index": 60.0, "skills": [], "questions": []}
    res = generate_insights(aggregate, category="ACADEMIC", allow_ai=False)
    assert res.source == "RULES"

def test_gemini_fallback_when_dummy_or_missing_key():
    """When GEMINI_API_KEY is dummy or empty, system returns RULES without crashing."""
    aggregate = {"response_count": 25, "index": 65.0, "skills": [], "questions": []}
    with patch.object(settings, "GEMINI_API_KEY", "dummy_placeholder_key"):
        res = generate_insights(aggregate, category="PLACEMENT", allow_ai=True)
        assert res.source == "RULES"

def test_mocked_gemini_success_returns_source_ai():
    """When Gemini returns valid JSON matching InsightModel, source is 'AI'."""
    aggregate = {
        "response_count": 60,
        "completion_rate": 0.90,
        "index": 75.0,
        "skills": [{"skill": "dsa", "target": 70.0, "mean": 50.0, "gap": 20.0, "share_below_target": 0.5}],
        "bands": {"READY": 0.3, "DEVELOPING": 0.5, "NEEDS_SUPPORT": 0.2},
        "questions": [],
    }

    mock_json = json.dumps({
        "headline": "DSA proficiency lags behind institutional target by 20 points.",
        "key_findings": [
            "Cohort DSA average is 50.0 against target 70.0.",
            "50% of students fall below target in DSA.",
            "Overall readiness index is 75.0."
        ],
        "recommendations": [
            {
                "priority": 1,
                "title": "Bootcamp on Core Algorithms",
                "action": "Schedule 4-week weekend coding camp on graph algorithms.",
                "rationale": "Addresses the 20 point DSA gap.",
                "suggested_owner": "Placement cell"
            }
        ],
        "watch_outs": ["Ensure sufficient lab capacity for coding sessions."]
    })

    mock_resp = MagicMock()
    mock_resp.text = mock_json

    mock_client = MagicMock()
    mock_client.models.generate_content.return_value = mock_resp

    with patch.object(settings, "GEMINI_API_KEY", "AIzaSyValidMockKey12345"), \
         patch("google.genai.Client", return_value=mock_client):
        res = generate_insights(aggregate, category="PLACEMENT", allow_ai=True)
        assert res.source == "AI"
        assert "DSA proficiency" in res.headline
        assert len(res.recommendations) == 1

def test_mocked_gemini_retry_once_on_first_failure():
    """When first attempt fails, Gemini retries once; if second succeeds, returns source 'AI'."""
    aggregate = {
        "response_count": 50,
        "completion_rate": 0.80,
        "index": 68.0,
        "skills": [],
        "questions": [],
    }

    valid_json = json.dumps({
        "headline": "Satisfaction level is stable at 68.0.",
        "key_findings": ["Overall satisfaction is 68.0."],
        "recommendations": [
            {
                "priority": 1,
                "title": "Maintain Current Quality",
                "action": "Continue quarterly reviews.",
                "rationale": "Stable score.",
                "suggested_owner": "Admin"
            }
        ],
        "watch_outs": []
    })

    mock_resp_fail = MagicMock()
    mock_resp_fail.text = "INVALID_NON_JSON"

    mock_resp_success = MagicMock()
    mock_resp_success.text = valid_json

    mock_client = MagicMock()
    # First call returns invalid JSON, second call returns valid JSON
    mock_client.models.generate_content.side_effect = [mock_resp_fail, mock_resp_success]

    with patch.object(settings, "GEMINI_API_KEY", "AIzaSyValidMockKey12345"), \
         patch("google.genai.Client", return_value=mock_client):
        res = generate_insights(aggregate, category="ACADEMIC", allow_ai=True)
        assert res.source == "AI"
        assert res.headline == "Satisfaction level is stable at 68.0."
        assert mock_client.models.generate_content.call_count == 2

def test_mocked_gemini_second_failure_falls_back_to_rules():
    """When both attempts fail, system safely falls back to deterministic RULES."""
    aggregate = {
        "response_count": 50,
        "completion_rate": 0.80,
        "index": 68.0,
        "skills": [],
        "questions": [],
    }

    mock_client = MagicMock()
    mock_client.models.generate_content.side_effect = [Exception("API Error 1"), Exception("API Error 2")]

    with patch.object(settings, "GEMINI_API_KEY", "AIzaSyValidMockKey12345"), \
         patch("google.genai.Client", return_value=mock_client):
        res = generate_insights(aggregate, category="ACADEMIC", allow_ai=True)
        assert res.source == "RULES"
        assert mock_client.models.generate_content.call_count == 2


def test_config_resolves_project_root_env_path(tmp_path):
    """
    Verify Settings resolves the project root .env path based on the analytics
    service directory structure rather than the process current working directory.
    Must not require, assert, or expose real secrets.
    """
    from app.config import PROJECT_ROOT, ROOT_ENV_FILE, Settings
    from pydantic_settings import SettingsConfigDict

    assert ROOT_ENV_FILE.name == ".env"
    assert ROOT_ENV_FILE.parent == PROJECT_ROOT
    assert (PROJECT_ROOT / "analytics").is_dir()

    # Verify Settings can load configuration from an isolated mock env file
    mock_env = tmp_path / "mock.env"
    mock_env.write_text("GEMINI_MODEL=gemini-test-mock\nGEMINI_TIMEOUT_SECONDS=45\n", encoding="utf-8")

    class IsolatedSettings(Settings):
        model_config = SettingsConfigDict(
            env_file=mock_env,
            extra="ignore",
        )

    iso = IsolatedSettings()
    assert iso.GEMINI_MODEL == "gemini-test-mock"
    assert iso.GEMINI_TIMEOUT_SECONDS == 45

