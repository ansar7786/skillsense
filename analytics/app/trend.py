from typing import Dict, Any, List
from app.schemas import TrendResponse, TrendItemModel

def compute_trend(current: Dict[str, Any], previous: Dict[str, Any]) -> TrendResponse:
    """
    Section 7, Step 6: Trend across terms.
    Two surveys of the same template are compared by matching question key and skill.
    Questions require at least 5 responses in both surveys.
    A change of less than 3 points is FLAT, otherwise IMPROVED or DECLINED.
    Flagged LOW_CONFIDENCE when either response count is under 30.
    """
    n_now_total = current.get("response_count", 0)
    n_prev_total = previous.get("response_count", 0)
    overall_low_conf = (n_now_total < 30 or n_prev_total < 30)

    # 1. Overall index trend
    idx_now = current.get("index") or 0.0
    idx_prev = previous.get("index") or 0.0
    delta_idx = round(idx_now - idx_prev, 2)
    if abs(delta_idx) < 3.0:
        dir_idx = "FLAT"
    elif delta_idx >= 3.0:
        dir_idx = "IMPROVED"
    else:
        dir_idx = "DECLINED"

    overall_trend = TrendItemModel(
        now=float(idx_now),
        previous=float(idx_prev),
        delta=delta_idx,
        direction=dir_idx,
        n_now=n_now_total,
        n_previous=n_prev_total,
        low_confidence=overall_low_conf,
    )

    # 2. Per-question trends
    curr_q_map = {q.get("key"): q for q in current.get("questions", []) if q.get("key")}
    prev_q_map = {q.get("key"): q for q in previous.get("questions", []) if q.get("key")}

    question_trends: List[TrendItemModel] = []
    for key, q_now in curr_q_map.items():
        if key in prev_q_map:
            q_prev = prev_q_map[key]
            n_now = q_now.get("n", 0)
            n_prev = q_prev.get("n", 0)

            # Minimum 5 responses in both surveys
            if n_now >= 5 and n_prev >= 5:
                # Use norm_mean for comparison
                m_now = q_now.get("norm_mean") if q_now.get("norm_mean") is not None else q_now.get("mean")
                m_prev = q_prev.get("norm_mean") if q_prev.get("norm_mean") is not None else q_prev.get("mean")

                if m_now is not None and m_prev is not None:
                    delta = round(float(m_now) - float(m_prev), 2)
                    if abs(delta) < 3.0:
                        direction = "FLAT"
                    elif delta >= 3.0:
                        direction = "IMPROVED"
                    else:
                        direction = "DECLINED"

                    low_conf = (n_now < 30 or n_prev < 30)

                    question_trends.append(
                        TrendItemModel(
                            key=key,
                            now=round(float(m_now), 2),
                            previous=round(float(m_prev), 2),
                            delta=delta,
                            direction=direction,
                            n_now=n_now,
                            n_previous=n_prev,
                            low_confidence=low_conf,
                        )
                    )

    # 3. Per-skill trends
    curr_s_map = {s.get("skill"): s for s in current.get("skills", []) if s.get("skill")}
    prev_s_map = {s.get("skill"): s for s in previous.get("skills", []) if s.get("skill")}

    skill_trends: List[TrendItemModel] = []
    for skill_name, s_now in curr_s_map.items():
        if skill_name in prev_s_map:
            s_prev = prev_s_map[skill_name]
            m_now = s_now.get("mean", 0.0)
            m_prev = s_prev.get("mean", 0.0)
            delta = round(float(m_now) - float(m_prev), 2)

            if abs(delta) < 3.0:
                direction = "FLAT"
            elif delta >= 3.0:
                direction = "IMPROVED"
            else:
                direction = "DECLINED"

            skill_trends.append(
                TrendItemModel(
                    skill=skill_name,
                    now=round(float(m_now), 2),
                    previous=round(float(m_prev), 2),
                    delta=delta,
                    direction=direction,
                    low_confidence=overall_low_conf,
                )
            )

    return TrendResponse(
        overall=overall_trend,
        questions=question_trends,
        skills=skill_trends,
    )
