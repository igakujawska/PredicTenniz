"""Download pinned ATP match files and record their provenance. No third-party dependencies."""
import hashlib
import json
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
REVISION = "83733587353df8a41f2fd4f516147d5aa83f5a8d"
BASE = f"https://raw.githubusercontent.com/Aneeshers/tennis-sackmann-archive/{REVISION}/atp"


def main():
    directory = ROOT / "target" / "ml" / "raw"
    directory.mkdir(parents=True, exist_ok=True)
    files = []
    for year in range(2021, 2026):
        filename = f"atp_matches_{year}.csv"
        url = f"{BASE}/{filename}"
        with urlopen(Request(url, headers={"User-Agent": "PredictTeniz-research/1.0"}), timeout=30) as response:
            payload = response.read()
        if not payload.startswith(b"tourney_id,"):
            raise ValueError(f"Unexpected CSV header in {filename}")
        (directory / filename).write_bytes(payload)
        files.append({"file": filename, "url": url, "sha256": hashlib.sha256(payload).hexdigest()})
        print(f"Downloaded {filename}: {len(payload):,} bytes")
    manifest = {"compiler": "Jeff Sackmann", "mirror": "Aneeshers/tennis-sackmann-archive",
                "revision": REVISION, "license": "CC BY-NC-SA 4.0", "files": files}
    (directory / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
