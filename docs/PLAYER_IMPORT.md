# Reproducible player import

Run commands from the repository root, with Docker Desktop running and JAVA_HOME pointing to JDK 25 or newer.

## Source

Player profiles: Jeff Sackmann, via [Aneeshers/tennis-sackmann-archive](https://github.com/Aneeshers/tennis-sackmann-archive).
Pinned archive revision: `83733587353df8a41f2fd4f516147d5aa83f5a8d`.
External data license: [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/).
Preserve the source attribution and applicable data terms when redistributing a downloaded or filtered dataset.
This note describes external data terms; it does not select a license for the application code.

The preparation script selects profiles for up to 100 ranked players from the **last available date in the archive**. The observed snapshot date is **2026-06-08**; it is historical data, not today's live ranking. Ranking positions are used to select profiles and are not imported into `player_rankings` by this workflow.

```powershell
./scripts/Prepare-PlayerData.ps1
```

Downloaded CSVs, the selected sample and a provenance manifest remain in ignored `target/import/`. The manifest records original URLs, archive revision, snapshot date and SHA-256 checksums. No source data is bundled into Git.

## Import

```powershell
$manifest = Get-Content target/import/players-top100.manifest.json | ConvertFrom-Json
$importArguments = '-Dspring-boot.run.arguments=--spring.profiles.active=player-import --tennis.import.players.path=target/import/players-top100.csv --tennis.import.players.sha256={0} --server.port=0' -f $manifest.selectedCsvSha256
./mvnw.cmd spring-boot:run '-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false' $importArguments
```

On macOS/Linux use `./mvnw` with equivalent arguments and a prepared CSV. The runner imports once, logs a `CSV_IMPORT` summary, and closes the application. A normal startup does not import data. The JVM flag disables DevTools restart for this one-shot task so startup failures produce a failing process exit code.

The reader validates headers, names, dates, country codes and source IDs; it reports rejected records. Broken CSV structure or a checksum mismatch aborts before writing profiles. All accepted records are written in one transaction; a database failure rolls that transaction back.

Identity is `(source, external_id)`, not a name. Repeating an import skips already known IDs. Different IDs with the same name remain separate. Existing profiles are not refreshed by this first importer; update and reconciliation policy will be a separate step.

The importer currently uses a simple per-record lookup and is intended for the small prepared sample. Large imports need batching and concurrency handling before being exposed as a service.

## View and verify

Run `PredictTenizApplication` normally in IntelliJ. Open `/api/players?page=0&size=20` on port 8080.

```powershell
./mvnw.cmd test
```

Database tests use the `player_entity_test` schema and roll back their records. The application uses the `public` schema.
