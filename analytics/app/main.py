from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.schemas import (
    AnalyzeRequest,
    AnalyzeResponse,
    StudentResultRequest,
    StudentResultResponse,
    TrendRequest,
    TrendResponse,
    InsightsRequest,
    InsightModel,
)
from app.readiness import compute_cohort_readiness, compute_student_result
from app.aggregate import (
    compute_question_stat,
    compute_satisfaction_index,
    compute_group_breakdowns,
    compute_text_themes,
)
from app.trend import compute_trend
from app.insights import generate_insights

app = FastAPI(
    title="SkillSense Analytics Service",
    description="Stateless analytics service for survey computation and insights",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(req: AnalyzeRequest):
    """
    POST /analyze:
    Calculates aggregate statistics for questions, groups (with K=5 suppression),
    cohort skills, readiness bands, and text themes.
    """
    survey = req.survey
    responses = req.responses or []
    parsed_schema = survey.get_parsed_schema()
    questions = parsed_schema.questions or []
    targets = survey.skill_targets or {}

    resp_count = len(responses)
    eligible_count = survey.eligible_count
    if eligible_count > 0:
        completion_rate = round(resp_count / eligible_count, 3)
    else:
        completion_rate = 1.0 if resp_count > 0 else 0.0

    # 1. Per-question stats
    q_stats = [compute_question_stat(q, responses) for q in questions]

    # 2. Overall satisfaction index
    index = compute_satisfaction_index(q_stats)

    # 3. Cohort skills and readiness bands
    skills, bands = compute_cohort_readiness(questions, responses, targets)

    # 4. Group breakdowns with K=5 suppression
    groups = compute_group_breakdowns(responses, questions)

    # 5. Text themes
    text_themes = compute_text_themes(questions, responses)

    return AnalyzeResponse(
        response_count=resp_count,
        completion_rate=completion_rate,
        index=index,
        questions=q_stats,
        skills=skills,
        bands=bands,
        groups=groups,
        text_themes=text_themes,
    )

@app.post("/student-result", response_model=StudentResultResponse)
def student_result(req: StudentResultRequest):
    """
    POST /student-result:
    Computes personal skill scores, readiness R(i), band, gaps,
    and strongest / weakest skill for a single student.
    """
    survey = req.survey
    parsed_schema = survey.get_parsed_schema()
    questions = parsed_schema.questions or []
    targets = survey.skill_targets or {}

    return compute_student_result(
        questions=questions,
        answers=req.answers,
        skill_targets=targets,
        cohort_means=req.cohort_means,
    )

@app.post("/trend", response_model=TrendResponse)
def trend(req: TrendRequest):
    """
    POST /trend:
    Compares current and previous AGGREGATE payloads to compute per-question,
    per-skill, and overall trend deltas and directions.
    """
    if not req.current or not req.previous:
        raise HTTPException(status_code=400, detail="Both current and previous payloads are required")
    return compute_trend(req.current, req.previous)

@app.post("/insights", response_model=InsightModel)
def insights(req: InsightsRequest):
    """
    POST /insights:
    Generates AI or deterministic rules-based aggregate insights and recommendations.
    """
    if not req.aggregate:
        raise HTTPException(status_code=400, detail="Aggregate payload is required")
    return generate_insights(
        aggregate=req.aggregate,
        trend=req.trend,
        category=req.category,
        allow_ai=req.allow_ai,
    )
