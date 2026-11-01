"""Chronological Elo and logistic baselines with a conservative result-availability embargo."""
import argparse
import csv
import hashlib
import heapq
import itertools
import json
import math
from collections import Counter, defaultdict
from datetime import date, datetime, timedelta
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EMBARGO_DAYS = 21


def sigmoid(value):
    if value >= 0:
        return 1 / (1 + math.exp(-value))
    exp = math.exp(value)
    return exp / (1 + exp)


def elo_probability(difference):
    return sigmoid(math.log(10) * difference / 400)


def load_matches(paths):
    required = {"tourney_id", "tourney_date", "match_num", "winner_id", "loser_id", "surface", "score"}
    counts = Counter()
    seen = set()
    matches = []
    for path in paths:
        with path.open(encoding="utf-8-sig", newline="") as handle:
            reader = csv.DictReader(handle)
            if not required.issubset(reader.fieldnames or []):
                raise ValueError(f"Missing required columns in {path.name}")
            for row in reader:
                counts["read"] += 1
                try:
                    played = datetime.strptime(row["tourney_date"], "%Y%m%d").date()
                    a, b = row["winner_id"], row["loser_id"]
                    if not a or not b or a == b:
                        raise ValueError("Invalid player IDs")
                    if row["surface"] not in {"Hard", "Clay", "Grass"}:
                        counts["excluded_surface"] += 1
                        continue
                    if not row["score"] or any(token in row["score"].upper() for token in ("W/O", "RET", "DEF", "ABN")):
                        counts["excluded_incomplete_match"] += 1
                        continue
                    identity = (row["tourney_id"], row["match_num"], a, b)
                    if identity in seen:
                        counts["duplicates"] += 1
                        continue
                    seen.add(identity)
                    matches.append({"date": played, "a": a, "b": b, "surface": row["surface"]})
                    counts["accepted"] += 1
                except (ValueError, TypeError):
                    counts["invalid"] += 1
    return sorted(matches, key=lambda row: row["date"]), dict(counts)


def build_features(matches, embargo_days=EMBARGO_DAYS, k=24.0):
    """No result affects a feature before start date + embargo, including overlapping events."""
    ratings = defaultdict(lambda: 1500.0)
    pending = []
    sequence = 0
    features = []
    for played, rows in itertools.groupby(matches, key=lambda row: row["date"]):
        while pending and pending[0][0] <= played:
            _, _, updates = heapq.heappop(pending)
            for player, delta in updates.items():
                ratings[player] += delta
        updates = defaultdict(float)
        for row in rows:
            a, b = row["a"], row["b"]
            difference = ratings[a] - ratings[b]
            p = elo_probability(difference)
            features.append({**row, "x": difference / 400, "elo": p})
            updates[a] += k * (1-p)
            updates[b] -= k * (1-p)
        sequence += 1
        heapq.heappush(pending, (played + timedelta(days=embargo_days), sequence, dict(updates)))
    return features


def fit_logistic(features, epochs=1200, learning_rate=0.15, l2=0.01):
    """Mirrored observations enforce label balance; no intercept preserves pair symmetry."""
    if not features:
        raise ValueError("No training observations.")
    observations = [(row["x"], 1.0) for row in features] + [(-row["x"], 0.0) for row in features]
    weight = 0.0
    for _ in range(epochs):
        gradient = sum((sigmoid(weight*x)-y)*x for x, y in observations) / len(observations) + l2*weight
        weight -= learning_rate * gradient
    return weight


def score(probabilities):
    if not probabilities:
        raise ValueError("An evaluation split is empty.")
    clipped = [min(1-1e-12, max(1e-12, p)) for p in probabilities]
    return {"matches": len(clipped), "log_loss": -sum(math.log(p) for p in clipped)/len(clipped),
            "brier": sum((1-p)**2 for p in clipped)/len(clipped),
            "accuracy": sum(1.0 if p > .5 else .5 if p == .5 else 0.0 for p in clipped)/len(clipped)}


def evaluate(rows, weight):
    return {"constant_50": score([.5 for _ in rows]), "global_elo": score([r["elo"] for r in rows]),
            "logistic_elo": score([sigmoid(weight*r["x"]) for r in rows])}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--data-dir", type=Path, default=ROOT / "data" / "raw")
    parser.add_argument("--train-through", type=int, default=2022)
    parser.add_argument("--validation-year", type=int, default=2023)
    parser.add_argument("--test-year", type=int, default=2024)
    args = parser.parse_args()
    if not args.train_through < args.validation_year < args.test_year:
        parser.error("Use strictly increasing training, validation and test years.")
    paths = sorted(args.data_dir.glob("atp_matches_*.csv"))
    if not paths:
        parser.error("No source CSV files. Run research/download.py first.")
    matches, quality = load_matches(paths)
    features = build_features(matches)
    validation_start = date(args.validation_year, 1, 1)
    train = [r for r in features if r["date"].year <= args.train_through and r["date"] + timedelta(days=EMBARGO_DAYS) < validation_start]
    validation = [r for r in features if r["date"].year == args.validation_year]
    test = [r for r in features if r["date"].year == args.test_year]
    weight = fit_logistic(train)
    provenance_path = args.data_dir / "provenance.json"
    provenance = json.loads(provenance_path.read_text(encoding="utf-8")) if provenance_path.exists() else None
    checksums = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
    if provenance:
        for entry in provenance["files"]:
            if checksums.get(entry["file"]) != entry["sha256"]:
                raise ValueError(f"Source checksum mismatch or missing file: {entry['file']}")
    report = {"schema_version": 1, "data_quality": quality, "embargo_days": EMBARGO_DAYS,
              "feature": "global Elo difference / 400; initial 1500; K=24; no intercept",
              "training_matches": len(train), "mirrored_training_rows": len(train)*2,
              "train_through": args.train_through, "validation_year": args.validation_year, "test_year": args.test_year,
              "coefficient": weight, "validation": evaluate(validation, weight), "test": evaluate(test, weight),
              "source_checksums": checksums, "provenance": provenance,
              "limitations": ["Tournament start dates, not match timestamps; 21-day availability assumption.",
                              "No hyperparameter tuning; no calibration claim.", "Offline research; not deployed in the dashboard."]}
    output = ROOT / "data" / "research"
    output.mkdir(parents=True, exist_ok=True)
    (output / "report.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
    (output / "model.json").write_text(json.dumps({"model": "logistic-global-elo-v1", "coefficient": weight,
        "intercept": 0, "feature": report["feature"], "train_through": args.train_through, "embargo_days": EMBARGO_DAYS}, indent=2)+"\n", encoding="utf-8")
    print(json.dumps({"data_quality": quality, "training_matches": len(train), "validation": report["validation"], "test": report["test"]}, indent=2))
    print(f"Saved report: {output / 'report.json'}")


if __name__ == "__main__":
    main()
