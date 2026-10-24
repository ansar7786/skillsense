from typing import List, Dict, Any, Optional, Union
from pydantic import BaseModel, Field

class RatingConfig(BaseModel):
    min: int = 1
    max: int = 5
    minLabel: Optional[str] = None
    maxLabel: Optional[str] = None

class OptionConfig(BaseModel):
    value: str
    label: str
    score: Optional[float] = None

class ScoringConfig(BaseModel):
    skill: str
    weight: float = 1.0

class QuestionModel(BaseModel):
    key: str
    type: str
    prompt: Optional[str] = ""
    required: bool = False
    section: Optional[str] = None
    rating: Optional[RatingConfig] = None
    options: Optional[List[OptionConfig]] = None
    scoring: Optional[ScoringConfig] = None
    maxLength: Optional[int] = None

class QuestionSchemaModel(BaseModel):
    schemaVersion: int = 1
    questions: List[QuestionModel] = []

class SurveyMetaModel(BaseModel):
    id: Optional[int] = None
    category: str = "CUSTOM"
    is_anonymous: bool = False
    eligible_count: int = 0
    question_schema: Union[QuestionSchemaModel, Dict[str, Any]]
    skill_targets: Optional[Dict[str, float]] = None

    def get_parsed_schema(self) -> QuestionSchemaModel:
        if isinstance(self.question_schema, QuestionSchemaModel):
            return self.question_schema
        return QuestionSchemaModel.model_validate(self.question_schema)

class ResponseRowModel(BaseModel):
    student_id: Optional[Union[int, str]] = None
    branch: Optional[str] = None
    year: Optional[Union[int, str]] = None
    section: Optional[str] = None
    answers: Dict[str, Any] = Field(default_factory=dict)

class AnalyzeRequest(BaseModel):
    survey: SurveyMetaModel
    responses: List[ResponseRowModel] = Field(default_factory=list)

class QuestionStatModel(BaseModel):
    key: str
    type: str
    n: int
    mean: Optional[float] = None
    median: Optional[float] = None
    norm_mean: Optional[float] = None
    distribution: Optional[Dict[str, int]] = None
    percentages: Optional[Dict[str, float]] = None
    share_yes: Optional[float] = None
    histogram: Optional[Dict[str, int]] = None

class SkillStatModel(BaseModel):
    skill: str
    target: float
    mean: float
    gap: float
    share_below_target: float
    priority: int

class BandDistributionModel(BaseModel):
    READY: float = 0.0
    DEVELOPING: float = 0.0
    NEEDS_SUPPORT: float = 0.0

class GroupItemModel(BaseModel):
    n: int
    index: Optional[float] = None

class GroupsModel(BaseModel):
    branch: Dict[str, GroupItemModel] = Field(default_factory=dict)
    year: Dict[str, GroupItemModel] = Field(default_factory=dict)
    suppressed: List[str] = Field(default_factory=list)

class TextThemeTermModel(BaseModel):
    term: str
    count: int

class AnalyzeResponse(BaseModel):
    response_count: int
    completion_rate: float
    index: Optional[float] = None
    questions: List[QuestionStatModel] = Field(default_factory=list)
    skills: List[SkillStatModel] = Field(default_factory=list)
    bands: BandDistributionModel = Field(default_factory=BandDistributionModel)
    groups: GroupsModel = Field(default_factory=GroupsModel)
    text_themes: Dict[str, List[TextThemeTermModel]] = Field(default_factory=dict)

# Student Result Models
class StudentResultRequest(BaseModel):
    survey: SurveyMetaModel
    answers: Dict[str, Any] = Field(default_factory=dict)
    cohort_means: Optional[Dict[str, float]] = None

class StudentSkillResultModel(BaseModel):
    skill: str
    score: float
    target: float
    gap: float
    cohort_mean: Optional[float] = None

class StudentResultResponse(BaseModel):
    readiness: float
    band: str
    skills: List[StudentSkillResultModel] = Field(default_factory=list)
    strongest_skill: Optional[str] = None
    weakest_skill: Optional[str] = None

# Trend Models
class TrendRequest(BaseModel):
    current: Dict[str, Any]
    previous: Dict[str, Any]

class TrendItemModel(BaseModel):
    key: Optional[str] = None
    skill: Optional[str] = None
    now: float
    previous: float
    delta: float
    direction: str
    n_now: Optional[int] = None
    n_previous: Optional[int] = None
    low_confidence: bool = False

class TrendResponse(BaseModel):
    overall: TrendItemModel
    questions: List[TrendItemModel] = Field(default_factory=list)
    skills: List[TrendItemModel] = Field(default_factory=list)

# Insights Models
class RecommendationModel(BaseModel):
    priority: int
    title: str
    action: str
    rationale: str
    suggested_owner: str

class InsightModel(BaseModel):
    headline: str
    key_findings: List[str]
    recommendations: List[RecommendationModel]
    watch_outs: List[str] = Field(default_factory=list)
    source: str = "RULES"

class InsightsRequest(BaseModel):
    aggregate: Dict[str, Any]
    trend: Optional[Dict[str, Any]] = None
    category: str = "CUSTOM"
    allow_ai: bool = False
