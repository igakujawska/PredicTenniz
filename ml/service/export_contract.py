"""Generate the reviewed Java/Python boundary without starting an HTTP server."""
import json
from pathlib import Path
from .app import app

if __name__ == "__main__":
    output = Path(__file__).resolve().parents[2] / "contracts/ml-openapi.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(app.openapi(), indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(output)
