"""Model execution belongs exclusively to Python. No connection to the Java database."""
from datetime import date
import json
import math
from pathlib import Path

from .schemas import BatchResult, ModelDescription, PredictionBatch, PredictionOutcome

ARTIFACTS = Path(__file__).resolve().parents[1] / "artifacts"


class PredictionEngine:
    def __init__(self, artifacts=ARTIFACTS):
        self.artifact = json.loads((artifacts / "rank-logistic-v1.json").read_text(encoding="utf-8"))
        if self.artifact["version"] != "rank-logistic-v1" or self.artifact["schemaVersion"] != 1:
            raise ValueError("Unsupported required model artifact")
        self.coefficients = self.artifact["coefficients"]
        if not all(math.isfinite(self.coefficients[name]) for name in ("hard", "clay", "grass", "bestOf5")):
            raise ValueError("Invalid model coefficients")
        self.trained_through = date.fromisoformat(self.artifact["trainedThrough"])

    def descriptions(self):
        return [ModelDescription(version="ranking-baseline-v1", label="Ranking · bazowy", trained=False),
                ModelDescription(version="rank-logistic-v1", label="Regresja logistyczna ML", trained=True,
                    trained_through=self.trained_through, test_year=self.artifact["testYear"],
                    test_matches=self.artifact["testMatches"], metrics=self.artifact["metrics"])]

    def predict(self, request: PredictionBatch):
        if request.model_version not in {model.version for model in self.descriptions()}:
            raise ValueError("Unknown modelVersion")
        if request.model_version == "rank-logistic-v1" and request.as_of < self.trained_through:
            raise ValueError(f"Model was trained through {self.trained_through}; select a later asOf")
        predictions = []
        for pair in request.pairs:
            if request.model_version == "ranking-baseline-v1":
                probability = math.sqrt(pair.rank_b) / (math.sqrt(pair.rank_a) + math.sqrt(pair.rank_b))
            else:
                coefficient = self.coefficients[request.surface.lower()]
                if request.best_of == 5:
                    coefficient += self.coefficients["bestOf5"]
                probability = 1 / (1 + math.exp(-coefficient * (math.log(pair.rank_b) - math.log(pair.rank_a))))
            predictions.append(PredictionOutcome(player_a_id=pair.player_a_id, player_b_id=pair.player_b_id,
                                                  probability_a=probability, probability_b=1 - probability))
        return BatchResult(model_version=request.model_version, as_of=request.as_of,
                           surface=request.surface, best_of=request.best_of, predictions=predictions)
