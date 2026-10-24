import pytest
from app.insights import generate_rule_based_insights

def test_deterministic_insights_placement():
    """Verify rules-based insights for PLACEMENT survey with skills."""
    aggregate_data = {
        "response_count": 50,
        "completion_rate": 0.85,
        "index": 62.0,
        "skills": [
            {"skill": "dsa", "target": 70.0, "mean": 45.0, "gap": 25.0, "share_below_target": 0.70, "priority": 1},
            {"skill": "java", "target": 70.0, "mean": 60.0, "gap": 10.0, "share_below_target": 0.40, "priority": 2},
        ],
        "bands": {"READY": 0.20, "DEVELOPING": 0.50, "NEEDS_SUPPORT": 0.30},
        "questions": [],
    }

    insights = generate_rule_based_insights(aggregate_data, category="PLACEMENT")

    assert insights.source == "RULES"
    assert "dsa" in insights.headline.lower()
    assert len(insights.key_findings) >= 3
    assert len(insights.recommendations) >= 3

    # Placement cell owner
    owners = [r.suggested_owner for r in insights.recommendations]
    assert "Placement cell" in owners

def test_deterministic_insights_wellbeing_and_low_responses():
    """Verify WELLBEING survey includes indicative watch-out and counselling recommendations."""
    aggregate_data = {
        "response_count": 15, # < 30
        "completion_rate": 0.30, # < 50%
        "index": 72.0,
        "questions": [
            {"key": "belonging", "norm_mean": 65.0},
            {"key": "workload", "norm_mean": 55.0},
        ],
        "skills": [],
    }

    insights = generate_rule_based_insights(aggregate_data, category="WELLBEING")

    assert insights.source == "RULES"
    assert any("indicative" in wo.lower() for wo in insights.watch_outs)
    assert any("counselling" in r.action.lower() for r in insights.recommendations)
    assert any("under 30" in wo.lower() for wo in insights.watch_outs)

def test_no_student_pii_in_insights():
    """Verify that insights output contains only aggregate statistics, zero PII."""
    aggregate_data = {
        "response_count": 45,
        "completion_rate": 0.90,
        "index": 70.0,
        "questions": [{"key": "cleanliness", "norm_mean": 40.0}],
        "skills": [],
    }

    insights = generate_rule_based_insights(aggregate_data, category="FACILITY")
    serialized = insights.model_dump_json()

    # Check for PII terms
    forbidden_terms = ["student_id", "email", "roll_number", "college_id", "password"]
    for term in forbidden_terms:
        assert term not in serialized.lower()
