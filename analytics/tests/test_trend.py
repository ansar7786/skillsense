import pytest
from app.trend import compute_trend

def test_trend_direction_and_confidence():
    """
    Section 7, Step 6:
    Delta < 3.0 is FLAT.
    Delta >= 3.0 is IMPROVED.
    Delta <= -3.0 is DECLINED.
    Flagged LOW_CONFIDENCE when either response count is under 30.
    """
    current_payload = {
        "response_count": 85,
        "index": 65.0,
        "questions": [
            {"key": "q_improved", "norm_mean": 70.0, "n": 85},
            {"key": "q_declined", "norm_mean": 45.0, "n": 85},
            {"key": "q_flat", "norm_mean": 61.5, "n": 85},
            {"key": "q_insufficient", "norm_mean": 80.0, "n": 3}, # n < 5
        ],
        "skills": [
            {"skill": "dsa", "mean": 55.0},
            {"skill": "comm", "mean": 50.0},
            {"skill": "java", "mean": 64.0},
        ],
    }

    previous_payload = {
        "response_count": 80,
        "index": 60.0,
        "questions": [
            {"key": "q_improved", "norm_mean": 65.0, "n": 80}, # +5.0 -> IMPROVED
            {"key": "q_declined", "norm_mean": 50.0, "n": 80}, # -5.0 -> DECLINED
            {"key": "q_flat", "norm_mean": 60.0, "n": 80},     # +1.5 -> FLAT
            {"key": "q_insufficient", "norm_mean": 80.0, "n": 10},
        ],
        "skills": [
            {"skill": "dsa", "mean": 45.0},  # +10.0 -> IMPROVED
            {"skill": "comm", "mean": 55.0}, # -5.0 -> DECLINED
            {"skill": "java", "mean": 62.0}, # +2.0 -> FLAT
        ],
    }

    trend_res = compute_trend(current_payload, previous_payload)

    # 1. Overall
    assert trend_res.overall.delta == 5.0
    assert trend_res.overall.direction == "IMPROVED"
    assert trend_res.overall.low_confidence is False

    # 2. Questions
    q_map = {q.key: q for q in trend_res.questions}
    assert "q_improved" in q_map
    assert q_map["q_improved"].delta == 5.0
    assert q_map["q_improved"].direction == "IMPROVED"
    assert q_map["q_improved"].low_confidence is False

    assert "q_declined" in q_map
    assert q_map["q_declined"].delta == -5.0
    assert q_map["q_declined"].direction == "DECLINED"

    assert "q_flat" in q_map
    assert q_map["q_flat"].delta == 1.5
    assert q_map["q_flat"].direction == "FLAT"

    # q_insufficient has n=3 in current (< 5), must be excluded
    assert "q_insufficient" not in q_map

    # 3. Skills
    s_map = {s.skill: s for s in trend_res.skills}
    assert s_map["dsa"].direction == "IMPROVED"
    assert s_map["comm"].direction == "DECLINED"
    assert s_map["java"].direction == "FLAT"

def test_trend_low_confidence_when_under_30():
    """Verify LOW_CONFIDENCE is flagged when response count < 30 in either survey."""
    current_payload = {
        "response_count": 25, # < 30
        "index": 70.0,
        "questions": [{"key": "q1", "norm_mean": 70.0, "n": 25}],
        "skills": [],
    }
    previous_payload = {
        "response_count": 50,
        "index": 65.0,
        "questions": [{"key": "q1", "norm_mean": 65.0, "n": 50}],
        "skills": [],
    }

    trend_res = compute_trend(current_payload, previous_payload)
    assert trend_res.overall.low_confidence is True
    assert trend_res.questions[0].low_confidence is True
