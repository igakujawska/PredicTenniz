"""Historical, availability-aware tennis features shared by training and export.

Event dates in this archive are tournament starts. Results become available after
an explicit 21-day embargo; no same-event result enters its own prediction.
"""
import argparse
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from datetime import date, datetime, timedelta
import csv
import hashlib
import heapq
from itertools import count, groupby
import json
import math
from pathlib import Path
from urllib.request import Request, urlopen

import numpy as np
try:
    from .download_matches import BASE, REVISION, ROOT
except ImportError:  # Allow python ml/history_features.py as well as package imports.
    from download_matches import BASE, REVISION, ROOT

SURFACES = ("HARD", "CLAY", "GRASS")
EMBARGO_DAYS = 21
FEATURE_NAMES = ("log_rank_ratio", "elo_difference", "surface_elo_difference", "form_90_difference",
                 "surface_form_180_difference", "win_rate_365_difference", "experience_365_difference",
                 "head_to_head_advantage", "best_of_five", "clay", "grass")
DIRECTED_FEATURES = 8


@dataclass(frozen=True)
class Event:
    event_date: date
    winner: int
    loser: int
    winner_rank: int | None
    loser_rank: int | None
    surface: str = "HARD"
    best_of: int = 3


@dataclass
class PlayerHistory:
    elo: float = 1500.0
    surface_elo: dict = field(default_factory=lambda: dict.fromkeys(SURFACES, 1500.0))
    results: list = field(default_factory=list)


class History:
    def __init__(self):
        self.players = defaultdict(PlayerHistory)
        self.head_to_head = defaultdict(lambda: [0, 0])
        self.pending = []
        self.sequence = count()
        self.current_day = None
        self.profile_cache = {}

    def schedule(self, event):
        available = event.event_date + timedelta(days=EMBARGO_DAYS)
        heapq.heappush(self.pending, (available, next(self.sequence), event))

    def advance(self, day):
        if self.current_day and day < self.current_day:
            raise ValueError("History cannot move backwards in time")
        if self.current_day != day:
            self.profile_cache.clear()
        self.current_day = day
        while self.pending and self.pending[0][0] <= day:
            available = self.pending[0][0]
            batch = []
            while self.pending and self.pending[0][0] == available:
                batch.append(heapq.heappop(self.pending)[2])
            self._apply_batch(available, batch)
            self.profile_cache.clear()

    def _apply_batch(self, available, batch):
        # Freeze ratings for each release-day batch: source row order cannot reveal rounds.
        elo_changes, surface_changes = defaultdict(float), defaultdict(float)
        for event in batch:
            winner, loser = self.players[event.winner], self.players[event.loser]
            expected = 1 / (1 + 10 ** ((loser.elo - winner.elo) / 400))
            change = 32 * (1 - expected)
            elo_changes[event.winner] += change
            elo_changes[event.loser] -= change
            expected_surface = 1 / (1 + 10 ** ((loser.surface_elo[event.surface] - winner.surface_elo[event.surface]) / 400))
            change_surface = 24 * (1 - expected_surface)
            surface_changes[event.winner, event.surface] += change_surface
            surface_changes[event.loser, event.surface] -= change_surface
            winner.results.append((available, True, event.surface))
            loser.results.append((available, False, event.surface))
            low, high = sorted((event.winner, event.loser))
            self.head_to_head[low, high][0 if event.winner == low else 1] += 1
        for player, change in elo_changes.items():
            self.players[player].elo += change
        for (player, surface), change in surface_changes.items():
            self.players[player].surface_elo[surface] += change

    def profile(self, player_id, day):
        if self.current_day != day:
            self.advance(day)
        if player_id in self.profile_cache:
            return self.profile_cache[player_id]
        player = self.players[player_id]
        recent = [result for result in player.results if day - timedelta(days=90) < result[0] <= day]
        year = [result for result in player.results if day - timedelta(days=365) < result[0] <= day]
        surface_results = {surface: [result for result in player.results
                           if result[2] == surface and day - timedelta(days=180) < result[0] <= day] for surface in SURFACES}
        wins90 = sum(result[1] for result in recent)
        wins_year = sum(result[1] for result in year)
        wins_surface = {surface: sum(result[1] for result in results) for surface, results in surface_results.items()}
        profile = {
            "elo": player.elo, "surfaceElo": dict(player.surface_elo),
            "wins90": wins90, "matches90": len(recent), "form90": (wins90 + 1) / (len(recent) + 2),
            "matches365": len(year), "winRate365": (wins_year + 1) / (len(year) + 2),
            "matchesAll": len(player.results), "surfaceWins180": wins_surface,
            "surfaceMatches180": {surface: len(results) for surface, results in surface_results.items()},
            "surfaceForm180": {surface: (wins_surface[surface] + 1) / (len(results) + 2)
                               for surface, results in surface_results.items()}
        }
        self.profile_cache[player_id] = profile
        return profile

    def features(self, player_a, player_b, rank_a, rank_b, surface, best_of, day):
        if min(rank_a, rank_b) < 1 or surface not in SURFACES or best_of not in (3, 5):
            raise ValueError("Invalid match inputs")
        a, b = self.profile(player_a, day), self.profile(player_b, day)
        low, high = sorted((player_a, player_b))
        wins = self.head_to_head.get((low, high), [0, 0])
        wins_a, wins_b = wins if player_a == low else wins[::-1]
        return np.asarray([
            math.log(rank_b) - math.log(rank_a), (a["elo"] - b["elo"]) / 400,
            (a["surfaceElo"][surface] - b["surfaceElo"][surface]) / 400,
            a["form90"] - b["form90"], a["surfaceForm180"][surface] - b["surfaceForm180"][surface],
            a["winRate365"] - b["winRate365"], math.log1p(a["matches365"]) - math.log1p(b["matches365"]),
            (wins_a - wins_b) / (sum(wins) + 2), float(best_of == 5), float(surface == "CLAY"), float(surface == "GRASS")
        ], dtype=np.float32)


def flip_features(features):
    swapped = np.asarray(features, dtype=np.float32).copy()
    swapped[..., :DIRECTED_FEATURES] *= -1
    return swapped


def download():
    directory = ROOT / "target/ml/history"
    directory.mkdir(parents=True, exist_ok=True)
    files = []
    for year in range(2018, 2027):
        filename = f"atp_matches_{year}.csv"
        with urlopen(Request(f"{BASE}/{filename}", headers={"User-Agent": "PredictTeniz-history/2.0"}), timeout=30) as response:
            payload = response.read()
        if not payload.startswith(b"tourney_id,"):
            raise ValueError(f"Unexpected CSV: {filename}")
        (directory / filename).write_bytes(payload)
        files.append({"file": filename, "url": f"{BASE}/{filename}", "sha256": hashlib.sha256(payload).hexdigest()})
        print(f"Downloaded {filename}: {len(payload):,} bytes", flush=True)
    manifest = {"compiler": "Jeff Sackmann", "mirror": "Aneeshers/tennis-sackmann-archive", "revision": REVISION,
                "license": "CC BY-NC-SA 4.0", "files": files}
    (directory / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")


def load_events():
    directory = ROOT / "target/ml/history"
    manifest = json.loads((directory / "manifest.json").read_text(encoding="utf-8"))
    if {item["file"] for item in manifest["files"]} != {f"atp_matches_{year}.csv" for year in range(2018, 2027)}:
        raise ValueError("Expected complete pinned history manifest for 2018-2026")
    events, seen, quality = [], {}, Counter()
    required = {"tourney_id", "match_num", "tourney_date", "tourney_level", "score", "winner_id", "loser_id",
                "winner_rank", "loser_rank", "surface", "best_of"}
    for item in manifest["files"]:
        path = directory / item["file"]
        if hashlib.sha256(path.read_bytes()).hexdigest() != item["sha256"]:
            raise ValueError(f"Checksum mismatch: {path.name}")
        with path.open(encoding="utf-8-sig", newline="") as source:
            rows = csv.DictReader(source)
            if not required.issubset(rows.fieldnames or []):
                raise ValueError(f"Unexpected columns: {path.name}")
            for row in rows:
                quality["read"] += 1
                key = row["tourney_id"], row["match_num"]
                fingerprint = tuple(row[name] for name in sorted(required))
                if key in seen:
                    if seen[key] != fingerprint:
                        raise ValueError(f"Conflicting match: {key}")
                    quality["duplicates"] += 1
                    continue
                seen[key] = fingerprint
                if row["tourney_level"] not in ("A", "M", "G", "F"):
                    quality["excluded_level"] += 1
                    continue
                score = row["score"].upper()
                if not score or any(marker in score for marker in ("RET", "W/O", "DEF", "ABD", "ABN", "WALKOVER")):
                    quality["unfinished"] += 1
                    continue
                try:
                    event_date = datetime.strptime(row["tourney_date"], "%Y%m%d").date()
                    winner, loser = int(row["winner_id"]), int(row["loser_id"])
                    surface, best_of = row["surface"].upper(), int(row["best_of"])
                    if winner == loser or min(winner, loser) < 1 or surface not in SURFACES or best_of not in (3, 5):
                        raise ValueError("Invalid event")
                except (ValueError, TypeError):
                    quality["invalid_event"] += 1
                    continue
                def rank(value):
                    try:
                        result = int(value)
                        return result if result > 0 else None
                    except (ValueError, TypeError):
                        return None
                events.append(Event(event_date, winner, loser, rank(row["winner_rank"]), rank(row["loser_rank"]), surface, best_of))
                quality["accepted_for_history"] += 1
    return sorted(events, key=lambda event: event.event_date), dict(quality), manifest


def build_examples(events):
    state, examples = History(), []
    for day, group in groupby(sorted(events, key=lambda event: event.event_date), key=lambda event: event.event_date):
        batch = list(group)
        state.advance(day)
        for event in batch:
            if event.winner_rank is not None and event.loser_rank is not None and day.year >= 2021:
                x = state.features(event.winner, event.loser, event.winner_rank, event.loser_rank, event.surface, event.best_of, day)
                examples.append((event, x))
        for event in batch:
            state.schedule(event)
    return examples


def serving_snapshot(events, day):
    state = History()
    for event_day, group in groupby(sorted(events, key=lambda event: event.event_date), key=lambda event: event.event_date):
        if event_day > day:
            break
        state.advance(event_day)
        for event in group:
            state.schedule(event)
    state.advance(day)
    return {str(player): state.profile(player, day) for player in list(state.players)}, {
        f"{low}:{high}": wins for (low, high), wins in state.head_to_head.items()}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--download", action="store_true")
    if parser.parse_args().download:
        download()
