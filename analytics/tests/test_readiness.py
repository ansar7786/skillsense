import pytest
from app.schemas import (
    QuestionModel,
    RatingConfig,
    OptionConfig,
    ScoringConfig,
    ResponseRowModel,
)
from app.readiness import (
    normalize_answer,
    compute_student_result,
    compute_cohort_readiness,
    determine_readiness_band,
)

def test_section_11_worked_example():
    """
    Direct verification of Section 11 Acceptance Test:
    Two scored questions.
    1. java_skill is a 1 to 5 rating with skill java and weight 2, and student answers 4.
    2. mock_interviews has scoring skill interview, weight 1, and option score 50.
    Targets are java 70 and interview 60.

    Must give exactly:
    - Normalised java answer: 75
    - Normalised interview answer: 50
    - Skill scores: java 75, interview 50
    - Readiness R: 66.67, band DEVELOPING
    - Gaps: java 0 and interview 10
    """
    q_java = QuestionModel(
        key="java_skill",
        type="RATING",
        prompt="Rate Java skills",
        rating=RatingConfig(min=1, max=5),
        scoring=ScoringConfig(skill="java", weight=2.0),
    )
    q_interview = QuestionModel(
        key="mock_interviews",
        type="SINGLE_CHOICE",
        prompt="Mock interviews",
        options=[
            OptionConfig(value="0", label="None", score=0.0),
            OptionConfig(value="1-2", label="1 to 2", score=50.0),
            OptionConfig(value="3+", label="3 or more", score=100.0),
        ],
        scoring=ScoringConfig(skill="interview", weight=1.0),
    )

    questions = [q_java, q_interview]
    answers = {
        "java_skill": 4,
        "mock_interviews": "1-2",
    }
    targets = {
        "java": 70.0,
        "interview": 60.0,
    }

    # Step 1: Normalization
    norm_java = normalize_answer(q_java, 4)
    assert norm_java == 75.0

    norm_interview = normalize_answer(q_interview, "1-2")
    assert norm_interview == 50.0

    # Step 2: Student result
    res = compute_student_result(questions, answers, targets)

    assert res.readiness == 66.67
    assert res.band == "DEVELOPING"

    skill_map = {s.skill: s for s in res.skills}
    assert "java" in skill_map
    assert "interview" in skill_map

    assert skill_map["java"].score == 75.0
    assert skill_map["java"].gap == 0.0

    assert skill_map["interview"].score == 50.0
    assert skill_map["interview"].gap == 10.0

    assert res.strongest_skill == "java"
    assert res.weakest_skill == "interview"

def test_unanswered_optional_questions_are_excluded():
    """Unanswered optional questions must be excluded from weight sums, not counted as zero."""
    q1 = QuestionModel(
        key="q1",
        type="RATING",
        rating=RatingConfig(min=1, max=5),
        scoring=ScoringConfig(skill="tech", weight=2.0),
    )
    q2 = QuestionModel(
        key="q2",
        type="RATING",
        rating=RatingConfig(min=1, max=5),
        scoring=ScoringConfig(skill="tech", weight=3.0),
    )

    questions = [q1, q2]
    # Student answers only q1 (score 5 -> 100%), leaves q2 unanswered
    answers = {"q1": 5}
    res = compute_student_result(questions, answers, {"tech": 70.0})

    # Since q2 was omitted, score must be purely from q1 (100.0), NOT (2*100 + 3*0)/5 = 40.0
    assert res.readiness == 100.0
    assert res.skills[0].score == 100.0
    assert res.skills[0].gap == 0.0

def test_boundary_readiness_bands():
    """Test boundary values for readiness bands: READY (>=75), DEVELOPING (50..74.9), NEEDS_SUPPORT (<50)."""
    assert determine_readiness_band(100.0) == "READY"
    assert determine_readiness_band(75.0) == "READY"
    assert determine_readiness_band(74.9) == "DEVELOPING"
    assert determine_readiness_band(50.0) == "DEVELOPING"
    assert determine_readiness_band(49.9) == "NEEDS_SUPPORT"
    assert determine_readiness_band(0.0) == "NEEDS_SUPPORT"

def test_survey_without_scoring_returns_no_skills():
    """Surveys without scoring blocks must return empty skills and default bands."""
    q_unscored = QuestionModel(
        key="feedback",
        type="RATING",
        rating=RatingConfig(min=1, max=5),
    )
    responses = [
        ResponseRowModel(answers={"feedback": 4}),
        ResponseRowModel(answers={"feedback": 5}),
    ]
    skills, bands = compute_cohort_readiness([q_unscored], responses, {})
    assert skills == []
    assert bands.READY == 0.0
    assert bands.DEVELOPING == 0.0
    assert bands.NEEDS_SUPPORT == 0.0

def test_cohort_skill_priority_calculation():
    """
    Verify skill priority is sorted by (cohort_gap * share_below_target) descending.
    """
    q_java = QuestionModel(
        key="java",
        type="RATING",
        rating=RatingConfig(min=1, max=5),
        scoring=ScoringConfig(skill="java", weight=1.0),
    )
    q_dsa = QuestionModel(
        key="dsa",
        type="RATING",
        rating=RatingConfig(min=1, max=5),
        scoring=ScoringConfig(skill="dsa", weight=1.0),
    )
    questions = [q_java, q_dsa]
    targets = {"java": 70.0, "dsa": 70.0}

    # 4 respondents:
    # Java: all answer 5 (100.0) -> gap = 0, share_below = 0.0 -> priority metric = 0.0
    # DSA: all answer 1 (0.0) -> gap = 70.0, share_below = 1.0 -> priority metric = 70.0
    responses = [
        ResponseRowModel(answers={"java": 5, "dsa": 1}),
        ResponseRowModel(answers={"java": 5, "dsa": 1}),
        ResponseRowModel(answers={"java": 5, "dsa": 1}),
        ResponseRowModel(answers={"java": 5, "dsa": 1}),
    ]

    skills, bands = compute_cohort_readiness(questions, responses, targets)
    assert len(skills) == 2
    # DSA must have priority 1 because of larger gap and share below target
    assert skills[0].skill == "dsa"
    assert skills[0].priority == 1
    assert skills[0].gap == 70.0
    assert skills[0].share_below_target == 1.0

    assert skills[1].skill == "java"
    assert skills[1].priority == 2
    assert skills[1].gap == 0.0
    assert skills[1].share_below_target == 0.0
