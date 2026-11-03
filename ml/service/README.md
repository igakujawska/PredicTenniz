# Niezależna aplikacja ML

Z głównego folderu repozytorium:

```powershell
./.venv/Scripts/python.exe -m pip install -r ml/requirements-api.txt
./.venv/Scripts/python.exe -m uvicorn ml.service.app:app --host 127.0.0.1 --port 8001 --reload
```

Swagger: http://localhost:8001/docs. GET `/health/live` potwierdza proces, GET `/health/ready` załadowanie modeli. GET `/v1/models` podaje wersje i metryki. POST `/v1/predict` przyjmuje od 1 do 8128 par (do 128 różnych zawodników), warunki, kategorię turnieju i datę analizy; zwraca komplet prawdopodobieństw.

Usługa nie łączy się z PostgreSQL. Dostaje rankingi i identyfikatory w żądaniu. Artefakty w `ml/artifacts/` są tylko do odczytu podczas pracy. Trening nie jest uruchamiany na żądanie użytkownika.

Kontrakt w repo: `contracts/ml-openapi.json`; aktualizacja: `python -m ml.service.export_contract`. Zmiana protokołu wymaga sprawdzenia testów FastAPI i klienta Javy. Błędy wejścia zwracają 422 z komunikatem i listą pól. Nieznany model nie jest zastępowany innym.

Testy API: `python -m unittest ml.service.test_service` po instalacji `ml/requirements-test.txt`. Kontener buduje się z głównego kontekstu: `docker build -f ml/Dockerfile .`.

`form-xgboost-v2` używa źródłowych identyfikatorów do odtwarzania Elo i formy. Udostępniona historia jest datowana, a odpowiedzi poza jej oknem są odrzucane. Nie uruchamiamy treningu przy żądaniu. `/v1/models` zwraca też metryki, udziały cech oraz wyniki dla poszczególnych kategorii.

W kontenerze serwującym używamy natywnego `xgboost.Booster`, dzięki czemu scikit-learn pozostaje zależnością treningową. Test porównuje wynik natywnego serwowania z klasyfikatorem użytym przy treningu.
