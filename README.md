# PredictTeniz

Aplikacja do analizy drabinek ATP: ustaw pary, porównaj modele dla jednego meczu, sprawdź wszystkie możliwe drogi do tytułu i zapisz turniej. Projekt rozwijany w IntelliJ, z osobnymi aplikacjami Java, Python i Angular w jednym repozytorium.

## Podział aplikacji

| Część | Katalog | Odpowiedzialność | Adres lokalny |
|---|---|---|---|
| Angular | `frontend/` | Widoki, formularze, drabinka, jasny/ciemny motyw | http://localhost:4200 |
| Java + Spring Boot | `src/`, `pom.xml` | API, dane zawodników, zapis turniejów, obliczenia drabinki | http://localhost:8080/api/health |
| Python + FastAPI | `ml/service/`, `ml/artifacts/` | Wykonywanie i wersjonowanie modeli | http://localhost:8001/docs |
| PostgreSQL | `compose.yaml` | Profile, rankingi i drabinki należące do Javy | port 5434 |

Java jest językiem backendu, a Spring Boot jego frameworkiem. Python nie łączy się z bazą Javy. Dostaje jawne dane wejściowe przez HTTP i oddaje prawdopodobieństwa. [Diagram i przepływ żądania](docs/ARCHITECTURE.md).

## Uruchomienie podczas pracy nad kodem

Wymagane: JDK 25+, Node.js 24.15+ w linii 24, Python 3.12 i Docker Desktop. W głównym folderze skopiuj `.env.example` do `.env` i ustaw lokalne hasło bazy. Nie dodawaj `.env` do Git.

1. Python — pierwszy terminal w głównym folderze:

```powershell
py -3.12 -m venv .venv
./.venv/Scripts/python.exe -m pip install -r ml/requirements-api.txt
./.venv/Scripts/python.exe -m uvicorn ml.service.app:app --host 127.0.0.1 --port 8001 --reload
```

2. Spring Boot — uruchom `PredictTenizApplication` przyciskiem Run w IntelliJ. Backend sam uruchamia PostgreSQL przez Compose. Możesz też użyć `./mvnw.cmd spring-boot:run` po ustawieniu `JAVA_HOME`.

3. Angular — drugi terminal:

```powershell
cd frontend
npm ci
npm start
```

Otwórz **http://localhost:4200**. Angular przekazuje `/api` do Springa, więc przeglądarka korzysta z jednego adresu i zachowuje sesję CSRF. Port 8080 przekierowuje do osobnego frontendu; Java nie dostarcza już starego szablonu HTML. Każdy proces zatrzymujesz osobno przez Stop w IntelliJ lub Ctrl+C w jego terminalu.

## Uruchomienie całego zestawu w Dockerze

Po przygotowaniu `.env`, w głównym folderze:

```powershell
docker compose --profile full up --build -d
```

Po uzyskaniu statusu healthy otwórz **http://localhost:4200**. Polecenie `docker compose --profile full ps` pokazuje stan usług. Ten tryb wymaga wolnych portów 4200, 8080 i 8001 — najpierw zatrzymaj odpowiadające im procesy uruchomione lokalnie. Zatrzymanie bez usuwania danych:

```powershell
docker compose --profile full stop
```

Domyślny `docker compose up -d` uruchamia tylko bazę. Profil `full` dodaje ML, backend i frontend. Kontenery mają healthchecki; backend startuje po bazie i Pythonie, a frontend po backendzie. Dane PostgreSQL pozostają w dotychczasowym wolumenie. Nowa baza jest pusta i wymaga poniższego importu.

### Pierwszy import danych — PowerShell

Uruchom polecenia w głównym folderze projektu:

```powershell
./scripts/Prepare-PlayerData.ps1
$manifest = Get-Content target/import/players-top100.manifest.json | ConvertFrom-Json
$jvm = '-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false'
$profiles = '-Dspring-boot.run.arguments=--spring.profiles.active=player-import --tennis.import.players.path=target/import/players-top100.csv --tennis.import.players.sha256={0} --server.port=0' -f $manifest.selectedCsvSha256
./mvnw.cmd spring-boot:run $jvm $profiles
$rankings = '-Dspring-boot.run.arguments=--spring.profiles.active=ranking-import --tennis.import.rankings.path=target/import/atp_rankings_current.csv --tennis.import.rankings.sha256={0} --server.port=0' -f $manifest.rankingsSha256
./mvnw.cmd spring-boot:run $jvm $rankings
```

Każdy importer kończy pracę po wypisaniu podsumowania. Następnie uruchom aplikację normalnie, bez profilu importu. Na macOS/Linux użyj `./mvnw` i równoważnych argumentów oraz przygotowanego CSV. Dane i manifesty w `target/import` nie są dodawane do Git.

## Co działa

- Drabinki 2/4/8/16 zawodników: zmiana par, losowanie, podświetlanie drogi gracza oraz szanse na tytuł ze wszystkich możliwych ścieżek.
- Zapis, edycja i kopia turnieju; ochrona wersją przed nadpisaniem zmian z drugiego okna; import/eksport JSON.
- Porównanie pojedynczego meczu dla dostępnych modeli, wyszukiwanie zawodników i filtr kraju.
- Oddzielne API ML z kontraktem [OpenAPI](contracts/ml-openapi.json), walidacją wejścia i ograniczonym czasem oczekiwania.
- Podczas awarii Pythona można odczytać definicję zapisanej drabinki; aplikacja pokazuje brak prognozy zamiast zastępczych wyników.

Dostępne modele to heurystyka rankingowa i regresja logistyczna. Próbka rankingu ma datę **2026-06-08**, nie dzisiejszą. ML osiągnął log loss 0.6266 wobec 0.6276 heurystyki na 2483 meczach sezonu 2025, przy tej samej trafności 64.9%. [Metodologia i ograniczenia](ml/README.md). Moduł historycznych cech Elo i formy jest przygotowaniem kolejnego eksperymentu; nie jest jeszcze modelem używanym w aplikacji.

## Sprawdzenie projektu

```powershell
./mvnw.cmd test
./.venv/Scripts/python.exe -m pip install -r ml/requirements-test.txt -r ml/requirements.txt
./.venv/Scripts/python.exe -m unittest discover -s ml -p 'test_*.py'
./.venv/Scripts/python.exe -m unittest ml.service.test_service
npm --prefix frontend test
npm --prefix frontend run build
```

Docker Desktop musi działać. Testy bazy korzystają ze schematu `player_entity_test` i wycofują dane. Testy API Javy zastępują granicę HTTP ML, a testy klienta używają lokalnego serwera testowego. Zestaw Pythona sprawdza prawdziwy artefakt, walidację, kontrakt i dostępność historycznych cech.

Dane: Jeff Sackmann, wersjonowane archiwum CSV, CC BY-NC-SA 4.0. [Pochodzenie i import](docs/PLAYER_IMPORT.md). Aplikacja nie ma jeszcze danych na żywo, OCR drabinek, nagród pieniężnych ani sieci neuronowych.
