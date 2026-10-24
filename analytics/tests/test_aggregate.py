import pytest
from app.schemas import (
    QuestionModel,
    RatingConfig,
    OptionConfig,
    ResponseRowModel,
)
from app.aggregate import (
    compute_question_stat,
    compute_satisfaction_index,
    apply_k_suppression,
    compute_group_breakdowns,
    compute_text_themes,
)

def test_question_types_aggregation():
    """Verify statistical aggregation across RATING, SINGLE_CHOICE, MULTI_CHOICE, YES_NO, NUMBER, and TEXT."""
    q_rating = QuestionModel(key="q_rate", type="RATING", rating=RatingConfig(min=1, max=5))
    q_choice = QuestionModel(
        key="q_choice",
        type="SINGLE_CHOICE",
        options=[
            OptionConfig(value="opt_a", label="A", score=20.0),
            OptionConfig(value="opt_b", label="B", score=80.0),
        ],
    )
    q_multi = QuestionModel(
        key="q_multi",
        type="MULTI_CHOICE",
        options=[
            OptionConfig(value="c1", label="Choice 1"),
            OptionConfig(value="c2", label="Choice 2"),
        ],
    )
    q_yesno = QuestionModel(key="q_yn", type="YES_NO")
    q_number = QuestionModel(key="q_num", type="NUMBER")
    q_text = QuestionModel(key="q_txt", type="TEXT")

    responses = [
        ResponseRowModel(answers={"q_rate": 2, "q_choice": "opt_a", "q_multi": ["c1"], "q_yn": True, "q_num": 10, "q_txt": "fast response"}),
        ResponseRowModel(answers={"q_rate": 4, "q_choice": "opt_b", "q_multi": ["c1", "c2"], "q_yn": False, "q_num": 20, "q_txt": "slow response"}),
        ResponseRowModel(answers={"q_rate": 4, "q_choice": "opt_b", "q_multi": ["c2"], "q_yn": True, "q_num": 30, "q_txt": "good service"}),
        ResponseRowModel(answers={"q_rate": 5, "q_choice": "opt_b", "q_multi": [], "q_yn": True, "q_num": 40, "q_txt": "quick turnaround"}),
    ]

    # RATING: answers [2, 4, 4, 5] -> mean = 3.75, median = 4.0
    # norm_mean = (3.75 - 1) / 4 * 100 = 68.8%
    stat_rating = compute_question_stat(q_rating, responses)
    assert stat_rating.n == 4
    assert stat_rating.mean == 3.75
    assert stat_rating.median == 4.0
    assert stat_rating.norm_mean == 68.8
    assert stat_rating.distribution["4"] == 2
    assert stat_rating.distribution["1"] == 0

    # SINGLE_CHOICE: 1 opt_a (20), 3 opt_b (80) -> mean = (20 + 240) / 4 = 65.0
    stat_choice = compute_question_stat(q_choice, responses)
    assert stat_choice.n == 4
    assert stat_choice.distribution["opt_a"] == 1
    assert stat_choice.distribution["opt_b"] == 3
    assert stat_choice.percentages["opt_a"] == 0.25
    assert stat_choice.percentages["opt_b"] == 0.75
    assert stat_choice.norm_mean == 65.0

    # MULTI_CHOICE: c1 appears twice, c2 appears twice
    stat_multi = compute_question_stat(q_multi, responses)
    assert stat_multi.n == 4
    assert stat_multi.distribution["c1"] == 2
    assert stat_multi.distribution["c2"] == 2

    # YES_NO: 3 true, 1 false -> share_yes = 0.75
    stat_yn = compute_question_stat(q_yesno, responses)
    assert stat_yn.n == 4
    assert stat_yn.share_yes == 0.75
    assert stat_yn.norm_mean == 75.0

    # NUMBER: [10, 20, 30, 40] -> mean = 25.0, median = 25.0
    stat_num = compute_question_stat(q_number, responses)
    assert stat_num.n == 4
    assert stat_num.mean == 25.0
    assert stat_num.median == 25.0
    assert len(stat_num.histogram) == 5

    # TEXT: n = 4
    stat_txt = compute_question_stat(q_text, responses)
    assert stat_txt.n == 4

def test_satisfaction_index_calculation():
    """Verify satisfaction index is the mean of normalized rating questions."""
    q1 = QuestionModel(key="q1", type="RATING", rating=RatingConfig(min=1, max=5))
    q2 = QuestionModel(key="q2", type="RATING", rating=RatingConfig(min=1, max=5))

    # All answers 3 (50%) for q1, all answers 5 (100%) for q2
    responses = [ResponseRowModel(answers={"q1": 3, "q2": 5}) for _ in range(5)]
    stats = [compute_question_stat(q1, responses), compute_question_stat(q2, responses)]

    idx = compute_satisfaction_index(stats)
    # Mean of 50.0 and 100.0 is 75.0
    assert idx == 75.0

def test_k_suppression_exactly_one_group_hides_next_smallest():
    """
    Section 5 & 7 anti-subtraction rule:
    If exactly ONE group has < 5 responses, the next smallest visible group MUST ALSO be hidden.
    """
    raw_data = {
        "CSE": {"n": 40, "index": 70.0},
        "IT": {"n": 20, "index": 65.0},
        "ECE": {"n": 8, "index": 60.0},
        "MECH": {"n": 3, "index": 50.0},  # < 5 (only 1 group below threshold)
    }

    vis, supp = apply_k_suppression(raw_data, k=5)

    # MECH is hidden because n=3 < 5
    # Since only MECH is < 5, ECE (n=8, the smallest of [40, 20, 8]) is ALSO hidden!
    assert "MECH" in supp
    assert "ECE" in supp
    assert "MECH" not in vis
    assert "ECE" not in vis

    # Only CSE and IT remain visible
    assert set(vis.keys()) == {"CSE", "IT"}

def test_k_suppression_two_groups_below_threshold():
    """When two or more groups are < 5, only those groups are suppressed."""
    raw_data = {
        "CSE": {"n": 40, "index": 70.0},
        "IT": {"n": 20, "index": 65.0},
        "ECE": {"n": 4, "index": 60.0},   # < 5
        "MECH": {"n": 3, "index": 50.0},  # < 5
    }

    vis, supp = apply_k_suppression(raw_data, k=5)

    # Exactly ECE and MECH suppressed, CSE and IT visible
    assert set(supp) == {"ECE", "MECH"}
    assert set(vis.keys()) == {"CSE", "IT"}

def test_k_suppression_all_groups_sufficient():
    """When all groups >= 5, none are suppressed."""
    raw_data = {
        "CSE": {"n": 40, "index": 70.0},
        "IT": {"n": 20, "index": 65.0},
        "ECE": {"n": 10, "index": 60.0},
    }

    vis, supp = apply_k_suppression(raw_data, k=5)
    assert supp == []
    assert set(vis.keys()) == {"CSE", "IT", "ECE"}

def test_text_themes_threshold_and_stop_words():
    """
    Section 7 Step 4:
    Text themes extract words with document frequency >= 3, stop words removed.
    Full answers are never returned.
    """
    q_text = QuestionModel(key="blockers", type="TEXT")

    # 4 respondents mention 'workload', 2 mention 'wifi', 4 mention 'time'
    responses = [
        ResponseRowModel(answers={"blockers": "The heavy workload takes all my time."}),
        ResponseRowModel(answers={"blockers": "I have excessive workload and no free time."}),
        ResponseRowModel(answers={"blockers": "Need better time management for high workload."}),
        ResponseRowModel(answers={"blockers": "Unbalanced workload and insufficient time."}),
        ResponseRowModel(answers={"blockers": "Weak wifi in the hostel."}),
        ResponseRowModel(answers={"blockers": "Slow wifi connection."}),
    ]

    themes = compute_text_themes([q_text], responses)
    assert "blockers" in themes
    terms = {item.term: item.count for item in themes["blockers"]}

    # 'workload' and 'time' appear in 4 documents (>= 3 threshold)
    assert "workload" in terms
    assert "time" in terms
    assert terms["workload"] == 4
    assert terms["time"] == 4

    # 'wifi' only appears in 2 documents (< 3 threshold) -> MUST BE EXCLUDED
    assert "wifi" not in terms

    # Stop words like 'the', 'and', 'my', 'for' MUST BE EXCLUDED
    for sw in ["the", "and", "my", "for", "all", "all"]:
        assert sw not in terms
