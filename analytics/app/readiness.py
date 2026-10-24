from typing import List, Dict, Any, Optional, Tuple
from app.schemas import (
    QuestionModel,
    ResponseRowModel,
    SkillStatModel,
    BandDistributionModel,
    StudentSkillResultModel,
    StudentResultResponse,
)

def normalize_answer(q: QuestionModel, answer: Any) -> Optional[float]:
    """
    Step 1 of Section 7: Normalise answer to a 0 to 100 scale.
    RATING: (v - min) / (max - min) * 100
    SINGLE_CHOICE with score: option score
    YES_NO: 100 if yes, else 0
    Unanswered / non-scored: None
    """
    if answer is None:
        return None

    q_type = q.type.upper() if q.type else ""

    if q_type == "RATING":
        try:
            val = float(answer)
        except (ValueError, TypeError):
            return None
        min_val = float(q.rating.min) if q.rating and q.rating.min is not None else 1.0
        max_val = float(q.rating.max) if q.rating and q.rating.max is not None else 5.0
        if max_val == min_val:
            return 100.0
        norm = (val - min_val) / (max_val - min_val) * 100.0
        return max(0.0, min(100.0, norm))

    if q_type == "SINGLE_CHOICE":
        if q.options:
            str_ans = str(answer)
            for opt in q.options:
                if str(opt.value) == str_ans:
                    if opt.score is not None:
                        return max(0.0, min(100.0, float(opt.score)))
                    break
        return None

    if q_type == "YES_NO":
        if isinstance(answer, bool):
            return 100.0 if answer else 0.0
        str_ans = str(answer).strip().lower()
        if str_ans in ("true", "yes", "1"):
            return 100.0
        return 0.0

    return None

def determine_readiness_band(score: float) -> str:
    """
    Readiness band defined in Section 7:
    READY: >= 75
    DEVELOPING: 50 to 74.9
    NEEDS_SUPPORT: < 50
    """
    if score >= 75.0:
        return "READY"
    elif score >= 50.0:
        return "DEVELOPING"
    else:
        return "NEEDS_SUPPORT"

def compute_student_result(
    questions: List[QuestionModel],
    answers: Dict[str, Any],
    skill_targets: Optional[Dict[str, float]] = None,
    cohort_means: Optional[Dict[str, float]] = None,
) -> StudentResultResponse:
    """
    Computes personal skill scores, readiness R(i), band, gaps,
    and strongest / weakest skill for a single student.
    """
    targets = skill_targets or {}
    c_means = cohort_means or {}

    # Map question keys to QuestionModel
    q_map = {q.key: q for q in questions if q.scoring and q.scoring.skill}

    skill_weighted_sums: Dict[str, float] = {}
    skill_weights: Dict[str, float] = {}
    total_weighted_sum = 0.0
    total_weight = 0.0

    for key, q in q_map.items():
        if key in answers and answers[key] is not None:
            norm = normalize_answer(q, answers[key])
            if norm is not None:
                skill = q.scoring.skill.lower().strip()
                weight = float(q.scoring.weight) if q.scoring.weight else 1.0

                skill_weighted_sums[skill] = skill_weighted_sums.get(skill, 0.0) + (weight * norm)
                skill_weights[skill] = skill_weights.get(skill, 0.0) + weight

                total_weighted_sum += (weight * norm)
                total_weight += weight

    if total_weight > 0:
        readiness = round(total_weighted_sum / total_weight, 2)
    else:
        readiness = 0.0

    band = determine_readiness_band(readiness)

    skills_result: List[StudentSkillResultModel] = []
    for skill, weight_sum in skill_weights.items():
        if weight_sum > 0:
            s_score = round(skill_weighted_sums[skill] / weight_sum, 2)
            t_s = float(targets.get(skill, 70.0))
            gap = round(max(0.0, t_s - s_score), 2)
            c_mean = c_means.get(skill)

            skills_result.append(
                StudentSkillResultModel(
                    skill=skill,
                    score=s_score,
                    target=t_s,
                    gap=gap,
                    cohort_mean=c_mean,
                )
            )

    # Sort skills by skill name for consistency
    skills_result.sort(key=lambda x: x.skill)

    strongest_skill = None
    weakest_skill = None

    if skills_result:
        # Strongest: highest score; Weakest: largest gap (or lowest score)
        strongest = max(skills_result, key=lambda x: x.score)
        weakest = max(skills_result, key=lambda x: (x.gap, -x.score))
        strongest_skill = strongest.skill
        weakest_skill = weakest.skill

    return StudentResultResponse(
        readiness=readiness,
        band=band,
        skills=skills_result,
        strongest_skill=strongest_skill,
        weakest_skill=weakest_skill,
    )

def compute_cohort_readiness(
    questions: List[QuestionModel],
    responses: List[ResponseRowModel],
    skill_targets: Optional[Dict[str, float]] = None,
) -> Tuple[List[SkillStatModel], BandDistributionModel]:
    """
    Step 2 & 3: Calculates cohort skill scores, gaps, share below target,
    skill priority, and readiness band distribution over all respondents.
    """
    targets = skill_targets or {}
    scored_questions = [q for q in questions if q.scoring and q.scoring.skill]

    if not scored_questions or not responses:
        return [], BandDistributionModel()

    # Per-student scores
    all_readiness_scores: List[float] = []
    skill_scores_by_student: Dict[str, List[float]] = {}

    for resp in responses:
        answers = resp.answers or {}
        skill_weighted_sums: Dict[str, float] = {}
        skill_weights: Dict[str, float] = {}
        total_weighted_sum = 0.0
        total_weight = 0.0

        for q in scored_questions:
            if q.key in answers and answers[q.key] is not None:
                norm = normalize_answer(q, answers[q.key])
                if norm is not None:
                    skill = q.scoring.skill.lower().strip()
                    weight = float(q.scoring.weight) if q.scoring.weight else 1.0

                    skill_weighted_sums[skill] = skill_weighted_sums.get(skill, 0.0) + (weight * norm)
                    skill_weights[skill] = skill_weights.get(skill, 0.0) + weight

                    total_weighted_sum += (weight * norm)
                    total_weight += weight

        if total_weight > 0:
            r_val = round(total_weighted_sum / total_weight, 2)
            all_readiness_scores.append(r_val)

        for skill, w_sum in skill_weights.items():
            if w_sum > 0:
                s_score = round(skill_weighted_sums[skill] / w_sum, 2)
                skill_scores_by_student.setdefault(skill, []).append(s_score)

    # Compute skill stats
    skill_stats_list = []
    for skill, scores in skill_scores_by_student.items():
        if scores:
            mean_score = round(sum(scores) / len(scores), 2)
            target = float(targets.get(skill, 70.0))
            gap = round(max(0.0, target - mean_score), 2)
            below_count = sum(1 for s in scores if s < target)
            share_below = round(below_count / len(scores), 2)
            priority_metric = gap * share_below

            skill_stats_list.append({
                "skill": skill,
                "target": target,
                "mean": mean_score,
                "gap": gap,
                "share_below_target": share_below,
                "metric": priority_metric,
            })

    # Sort skills by priority metric descending, then skill name
    skill_stats_list.sort(key=lambda x: (-x["metric"], x["skill"]))

    final_skills: List[SkillStatModel] = []
    for idx, item in enumerate(skill_stats_list, start=1):
        final_skills.append(
            SkillStatModel(
                skill=item["skill"],
                target=item["target"],
                mean=item["mean"],
                gap=item["gap"],
                share_below_target=item["share_below_target"],
                priority=idx,
            )
        )

    # Compute band distribution
    if all_readiness_scores:
        total_scored = len(all_readiness_scores)
        ready_cnt = sum(1 for r in all_readiness_scores if r >= 75.0)
        dev_cnt = sum(1 for r in all_readiness_scores if 50.0 <= r < 75.0)
        needs_cnt = sum(1 for r in all_readiness_scores if r < 50.0)

        bands = BandDistributionModel(
            READY=round(ready_cnt / total_scored, 2),
            DEVELOPING=round(dev_cnt / total_scored, 2),
            NEEDS_SUPPORT=round(needs_cnt / total_scored, 2),
        )
    else:
        bands = BandDistributionModel()

    return final_skills, bands
