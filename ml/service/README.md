# Niezależna aplikacja ML

Z głównego folderu repozytorium:

```powershell
./.venv/Scripts/python.exe -m pip install -r ml/requirements-api.txt
./.venv/Scripts/python.exe -m uvicorn ml.service.app:app --host 127.0.0.1 --port 8001 --reload
```

Swagger: http://localhost:8001/docs. GET `/health/live` potwierdza proces, GET `/health/ready` załadowanie modeli. GET `/v1/models` podaje wersje i metryki. POST `/v1/predict` przyjmuje od 1 do 120 par, warunki i datę analizy; zwraca komplet prawdopodobieństw.

Usługa nie łączy się z PostgreSQL. Dostaje rankingi i identyfikatory w żądaniu. Artefakty w `ml/artifacts/` są tylko do odczytu podczas pracy. Trening nie jest uruchamiany na żądanie użytkownika.

Kontrakt w repo: `contracts/ml-openapi.json`; aktualizacja: `python -m ml.service.export_contract`. Zmiana protokołu wymaga sprawdzenia testów FastAPI i klienta Javy. Błędy wejścia zwracają 422 z komunikatem i listą pól. Nieznany model nie jest zastępowany innym.

Testy API: `python -m unittest ml.service.test_service` po instalacji `ml/requirements-test.txt`. Kontener buduje się z głównego kontekstu: `docker build -f ml/Dockerfile .`.
