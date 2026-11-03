import json
from pathlib import Path
import unittest
from fastapi.testclient import TestClient
from ml.service.app import app
from ml.service.engine import ARTIFACTS


class ServiceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.context = TestClient(app)
        cls.client = cls.context.__enter__()

    @classmethod
    def tearDownClass(cls):
        cls.context.__exit__(None, None, None)

    def batch(self, **changes):
        body = {"modelVersion": "ranking-baseline-v1", "asOf": "2026-06-08", "surface": "HARD", "bestOf": 3,
                "pairs": [{"playerAId": 1, "playerBId": 2, "rankA": 1, "rankB": 16}]}
        body.update(changes)
        return body

    def test_readiness_and_model_metadata(self):
        self.assertEqual(self.client.get("/health/ready").status_code, 200)
        models = self.client.get("/v1/models").json()
        self.assertEqual(models[1]["version"], "rank-logistic-v1")
        self.assertEqual(models[1]["testMatches"], 2483)

    def test_real_python_artifact_matches_its_exported_examples(self):
        artifact = json.loads((ARTIFACTS / "rank-logistic-v1.json").read_text(encoding="utf-8"))
        for fixture in artifact["parityCases"]:
            body = self.batch(modelVersion="rank-logistic-v1", surface=fixture["surface"], bestOf=fixture["bestOf"],
                pairs=[{"playerAId": 1, "playerBId": 2, "rankA": fixture["rankA"], "rankB": fixture["rankB"]}])
            response = self.client.post("/v1/predict", json=body)
            self.assertEqual(response.status_code, 200, response.text)
            predicted = response.json()["predictions"][0]
            self.assertAlmostEqual(predicted["probabilityA"], fixture["probability"], places=12)
            self.assertAlmostEqual(predicted["probabilityA"] + predicted["probabilityB"], 1, places=12)

    def test_duplicate_and_self_pairs_are_rejected(self):
        pair = self.batch()["pairs"][0]
        self.assertEqual(self.client.post("/v1/predict", json=self.batch(pairs=[pair, pair])).status_code, 422)
        self.assertEqual(self.client.post("/v1/predict", json=self.batch(pairs=[dict(pair, playerBId=1)])).status_code, 422)

    def test_unknown_model_future_trained_model_and_bad_format_are_rejected(self):
        self.assertEqual(self.client.post("/v1/predict", json=self.batch(modelVersion="missing")).status_code, 422)
        response = self.client.post("/v1/predict", json=self.batch(modelVersion="rank-logistic-v1", asOf="2024-06-08"))
        self.assertEqual(response.status_code, 422)
        self.assertIn("trained through", response.json()["detail"])
        self.assertEqual(self.client.post("/v1/predict", json=self.batch(bestOf=4)).status_code, 422)

    def test_generated_openapi_is_current(self):
        path = Path(__file__).resolve().parents[2] / "contracts/ml-openapi.json"
        self.assertEqual(json.loads(path.read_text(encoding="utf-8")), app.openapi())


if __name__ == "__main__":
    unittest.main()
