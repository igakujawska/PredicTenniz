"""Train a symmetric tennis model and evaluate once on a later, untouched season.

Run from the repository root: python ml/train.py
Raw CSVs and experiment outputs stay in target/ml; this script never edits Java sources.
"""
import argparse
from collections import Counter
import csv
from dataclasses import dataclass
from datetime import date, timedelta
import hashlib
import json
import math
from pathlib import Path

import numpy as np
import sklearn
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import brier_score_loss, log_loss

ROOT = Path(__file__).resolve().parents[1]
SURFACES = ("HARD", "CLAY", "GRASS")
EMBARGO_DAYS = 21


@dataclass(frozen=True)
class Match:
    event_date: date
    winner_rank: int
    loser_rank: int
    surface: str
    best_of: int


def features(rank_a, rank_b, surface, best_of):
    """Only pre-match inputs. Swapping players negates every feature."""
    if rank_a < 1 or rank_b < 1 or surface not in SURFACES or best_of not in (3, 5):
        raise ValueError("Invalid model inputs")
    difference = math.log(rank_b) - math.log(rank_a)
    return [difference if surface == candidate else 0.0 for candidate in SURFACES] + [difference if best_of == 5 else 0.0]


def load_matches(directory):
    manifest = json.loads((directory / "manifest.json").read_text(encoding="utf-8"))
    expected_files = {f"atp_matches_{year}.csv" for year in range(2021, 2026)}
    if {entry["file"] for entry in manifest["files"]} != expected_files:
        raise ValueError("Expected pinned match files for 2021-2025")
    counters = Counter()
    matches = []
    seen = {}
    required = {"tourney_id", "match_num", "tourney_date", "tourney_level", "surface", "best_of",
                "winner_rank", "loser_rank", "winner_id", "loser_id", "score"}
    for entry in manifest["files"]:
        path = directory / entry["file"]
        if hashlib.sha256(path.read_bytes()).hexdigest() != entry["sha256"]:
            raise ValueError(f"Source checksum mismatch: {path.name}")
        with path.open(encoding="utf-8-sig", newline="") as source:
            reader = csv.DictReader(source)
            if not required.issubset(reader.fieldnames or []):
                raise ValueError(f"Missing source columns in {path.name}")
            for row in reader:
                counters["read"] += 1
                key = (row["tourney_id"], row["match_num"])
                fingerprint = tuple(row[column] for column in sorted(required))
                if key in seen:
                    if seen[key] != fingerprint:
                        raise ValueError(f"Conflicting match identity: {key}")
                    counters["duplicates"] += 1
                    continue
                seen[key] = fingerprint
                if row["tourney_level"] not in ("A", "M", "G", "F"):
                    counters["excluded_level"] += 1
                    continue
                score = row["score"].upper()
                if not score or any(marker in score for marker in ("RET", "W/O", "DEF", "ABD", "ABN", "WALKOVER")):
                    counters["unfinished"] += 1
                    continue
                try:
                    event_date = date(int(row["tourney_date"][:4]), int(row["tourney_date"][4:6]), int(row["tourney_date"][6:8]))
                    winner_rank, loser_rank = int(row["winner_rank"]), int(row["loser_rank"])
                    surface, best_of = row["surface"].upper(), int(row["best_of"])
                    features(winner_rank, loser_rank, surface, best_of)
                except (ValueError, TypeError):
                    counters["invalid_or_missing_features"] += 1
                    continue
                matches.append(Match(event_date, winner_rank, loser_rank, surface, best_of))
                counters["accepted"] += 1
    return sorted(matches, key=lambda match: match.event_date), dict(counters), manifest


def split_matches(matches):
    """CSV dates are event starts, not exact result timestamps: embargo boundary events."""
    validation_start, test_start = date(2024, 1, 1), date(2025, 1, 1)
    train_end = validation_start - timedelta(days=EMBARGO_DAYS)
    final_train_end = test_start - timedelta(days=EMBARGO_DAYS)
    train = [match for match in matches if match.event_date < train_end]
    validation = [match for match in matches if validation_start <= match.event_date < final_train_end]
    final_train = [match for match in matches if match.event_date < final_train_end]
    test = [match for match in matches if test_start <= match.event_date < date(2026, 1, 1)]
    if any(not split for split in (train, validation, final_train, test)):
        raise ValueError("Every chronological partition must contain matches")
    return train, validation, final_train, test


def matrix(matches):
    return np.asarray([features(match.winner_rank, match.loser_rank, match.surface, match.best_of) for match in matches])


def fit_model(matches, regularization):
    winners = matrix(matches)
    x = np.concatenate((winners, -winners))
    y = np.concatenate((np.ones(len(winners)), np.zeros(len(winners))))
    model = LogisticRegression(C=regularization, fit_intercept=False, solver="lbfgs", max_iter=2000, tol=1e-10)
    model.fit(x, y)
    if model.n_iter_.max() >= model.max_iter:
        raise RuntimeError("Model did not converge")
    return model


def evaluate(winner_probabilities):
    """Mirrored rows balance source winner/loser ordering; match counts are NOT doubled."""
    p = np.asarray(winner_probabilities)
    labels = np.concatenate((np.ones(len(p)), np.zeros(len(p))))
    probabilities = np.concatenate((p, 1 - p))
    return {"logLoss": float(log_loss(labels, probabilities)),
            "brierScore": float(brier_score_loss(labels, probabilities)),
            "accuracy": float(np.mean(p > 0.5) + 0.5 * np.mean(p == 0.5))}


def baseline(matches):
    return np.asarray([math.sqrt(match.loser_rank) / (math.sqrt(match.winner_rank) + math.sqrt(match.loser_rank)) for match in matches])


def calibration(probabilities):
    labels = np.concatenate((np.ones(len(probabilities)), np.zeros(len(probabilities))))
    probabilities = np.concatenate((probabilities, 1 - probabilities))
    bins = []
    assignments = np.minimum((probabilities * 10).astype(int), 9)
    for index in range(10):
        mask = assignments == index
        if mask.any():
            bins.append({"predicted": float(probabilities[mask].mean()), "observed": float(labels[mask].mean()),
                         "observations": int(mask.sum())})
    return bins


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT / "target/ml/rank-logistic-v1.json")
    args = parser.parse_args()
    matches, quality, manifest = load_matches(ROOT / "target/ml/raw")
    train, validation, final_train, test = split_matches(matches)
    candidates = []
    for regularization in (0.1, 1.0, 10.0):
        model = fit_model(train, regularization)
        score = evaluate(model.predict_proba(matrix(validation))[:, 1])
        candidates.append({"C": regularization, **score})
    selected = min(candidates, key=lambda candidate: candidate["logLoss"])
    model = fit_model(final_train, selected["C"])
    probabilities = model.predict_proba(matrix(test))[:, 1]
    weights = dict(zip(("hard", "clay", "grass", "bestOf5"), model.coef_[0].tolist()))
    parity_cases = []
    for a, b, surface, best_of in ((1, 16, "HARD", 3), (1, 16, "CLAY", 3), (1, 16, "GRASS", 5),
                                   (27, 13, "HARD", 5), (10, 10, "CLAY", 3)):
        probability = float(model.predict_proba([features(a, b, surface, best_of)])[0, 1])
        parity_cases.append({"rankA": a, "rankB": b, "surface": surface, "bestOf": best_of, "probability": probability})
    artifact = {
        "schemaVersion": 1, "version": "rank-logistic-v1", "label": "Regresja logistyczna ML",
        "trainedThrough": "2024-12-31", "coefficients": weights,
        "featureDefinition": "log(rankB/rankA) by surface plus best-of-five interaction; no intercept",
        "testYear": 2025, "testMatches": len(test),
        "metrics": {"ml": evaluate(probabilities), "baseline": evaluate(baseline(test)),
                    "coinFlip": evaluate(np.full(len(test), 0.5))},
        "calibration": calibration(probabilities), "parityCases": parity_cases,
        "protocol": {"trainMatches": len(train), "validationMatches": len(validation),
                     "finalTrainMatches": len(final_train), "embargoDays": EMBARGO_DAYS,
                     "lastTrainingEvent": max(match.event_date for match in final_train).isoformat(),
                     "validationCandidates": candidates, "selectedC": selected["C"],
                     "scikitLearnVersion": sklearn.__version__, "numpyVersion": np.__version__},
        "dataQuality": quality, "provenance": manifest,
        "limitations": ["Tournament start dates approximate availability; 21-day embargo is an assumption.",
                        "Only completed ATP main-tour hard/clay/grass matches with known ranks are evaluated.",
                        "No recent form, injuries, serve statistics or player-specific features.",
                        "Held-out 2025 results do not establish live 2026 performance."]
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(artifact, indent=2, allow_nan=False) + "\n", encoding="utf-8")
    print(json.dumps({"output": str(args.output), "testMatches": len(test), "finalTrainMatches": len(final_train),
                      "selectedC": selected["C"], "metrics": artifact["metrics"]}, indent=2))


if __name__ == "__main__":
    main()
