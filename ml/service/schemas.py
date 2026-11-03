"""The generated OpenAPI contract is built from these strict request/response schemas."""
from datetime import date
from typing import Literal
from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator
from pydantic.alias_generators import to_camel


class Contract(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True, extra="forbid", allow_inf_nan=False)


class PredictionPair(Contract):
    player_a_id: int = Field(gt=0)
    player_b_id: int = Field(gt=0)
    rank_a: int = Field(gt=0, le=2147483647)
    rank_b: int = Field(gt=0, le=2147483647)
    source_a_id: str | None = Field(default=None, pattern=r"^[1-9][0-9]{0,15}$")
    source_b_id: str | None = Field(default=None, pattern=r"^[1-9][0-9]{0,15}$")

    @model_validator(mode="after")
    def different_players(self):
        if self.player_a_id == self.player_b_id:
            raise ValueError("A player cannot play themselves")
        return self


class PredictionBatch(Contract):
    model_version: str = Field(min_length=1, max_length=64)
    as_of: date
    surface: Literal["HARD", "CLAY", "GRASS"]
    best_of: Literal[3, 5]
    category: Literal["TOUR", "ATP_250", "ATP_500", "MASTERS_1000", "GRAND_SLAM"] = "TOUR"
    pairs: list[PredictionPair] = Field(min_length=1, max_length=8128)

    @field_validator("as_of")
    @classmethod
    def no_future_analysis(cls, value):
        if value > date.today():
            raise ValueError("asOf cannot be in the future")
        return value

    @model_validator(mode="after")
    def unique_and_consistent_pairs(self):
        seen, players = set(), {}
        for pair in self.pairs:
            key = tuple(sorted((pair.player_a_id, pair.player_b_id)))
            if key in seen:
                raise ValueError("Duplicate unordered pair")
            seen.add(key)
            for player_id, rank, source in ((pair.player_a_id, pair.rank_a, pair.source_a_id),
                                           (pair.player_b_id, pair.rank_b, pair.source_b_id)):
                identity = rank, source
                if player_id in players and players[player_id] != identity:
                    raise ValueError("A player has inconsistent rank or source identity in this batch")
                players[player_id] = identity
        if len(players) > 128:
            raise ValueError("A batch supports at most 128 different players")
        return self


class PredictionOutcome(Contract):
    player_a_id: int
    player_b_id: int
    probability_a: float = Field(ge=0, le=1)
    probability_b: float = Field(ge=0, le=1)


class BatchResult(Contract):
    category: Literal["TOUR", "ATP_250", "ATP_500", "MASTERS_1000", "GRAND_SLAM"] = "TOUR"
    model_version: str
    as_of: date
    surface: Literal["HARD", "CLAY", "GRASS"]
    best_of: Literal[3, 5]
    predictions: list[PredictionOutcome]


class Scores(Contract):
    log_loss: float = Field(ge=0)
    brier_score: float = Field(ge=0, le=1)
    accuracy: float = Field(ge=0, le=1)


class Benchmarks(Contract):
    ml: Scores
    baseline: Scores
    coin_flip: Scores
    logistic: Scores | None = None
    elo: Scores | None = None


class FeatureGain(Contract):
    feature: str
    gain_share: float


class CategoryScore(Contract):
    matches: int
    model: Scores
    ranking: Scores


class ModelDescription(Contract):
    version: str
    label: str
    trained: bool
    trained_through: date | None = None
    test_year: int | None = None
    test_matches: int | None = None
    metrics: Benchmarks | None = None
    data_through: date | None = None
    train_matches: int | None = None
    history_matches: int | None = None
    feature_names: list[str] = Field(default_factory=list)
    feature_importance: list[FeatureGain] = Field(default_factory=list)
    category_scores: dict[str, CategoryScore] = Field(default_factory=dict)
    notes: list[str] = Field(default_factory=list)


class ValidationIssue(Contract):
    location: list[str | int]
    message: str


class ApiError(Contract):
    detail: str
    errors: list[ValidationIssue] = Field(default_factory=list)
