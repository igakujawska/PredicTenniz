"""Download explicitly versioned, attributed historical tennis CSVs. Standard library only."""
import argparse
import hashlib
import json
import re
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPOSITORY = "Aneeshers/tennis-sackmann-archive"


def fetch(url):
    request = urllib.request.Request(url, headers={"User-Agent": "CourtVision-educational-research/0.1"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--start-year", type=int, default=2018)
    parser.add_argument("--end-year", type=int, default=2024)
    parser.add_argument("--revision", help="Exact archive commit SHA; otherwise resolve main once")
    args = parser.parse_args()
    if not 1968 <= args.start_year <= args.end_year <= 2025:
        parser.error("Use a range between 1968 and 2025 (complete calendar years).")
    revision = args.revision or json.loads(fetch(f"https://api.github.com/repos/{REPOSITORY}/commits/main"))["sha"]
    if not re.fullmatch(r"[a-fA-F0-9]{40}", revision):
        parser.error("Revision must be a full 40-character Git commit SHA.")
    output = ROOT / "data" / "raw"
    output.mkdir(parents=True, exist_ok=True)
    entries = []
    for year in range(args.start_year, args.end_year + 1):
        filename = f"atp_matches_{year}.csv"
        url = f"https://raw.githubusercontent.com/{REPOSITORY}/{revision}/atp/{filename}"
        payload = fetch(url)
        if not payload.startswith(b"tourney_id,"):
            raise ValueError(f"Unexpected CSV schema: {filename}")
        (output / filename).write_bytes(payload)
        entries.append({"file": filename, "url": url, "sha256": hashlib.sha256(payload).hexdigest(), "bytes": len(payload)})
        print(f"Downloaded {filename}: {len(payload):,} bytes")
    provenance = {"compiler": "Jeff Sackmann", "mirror": REPOSITORY, "revision": revision,
                  "license": "CC BY-NC-SA 4.0", "files": entries}
    (output / "provenance.json").write_text(json.dumps(provenance, indent=2) + "\n", encoding="utf-8")
    print(f"Recorded provenance at {output / 'provenance.json'}")


if __name__ == "__main__":
    main()
