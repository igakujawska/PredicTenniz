# Verification record

This file records what was actually exercised for the first release. A green workflow run is authoritative for the exact pushed commit; local checks below describe the development session on 2026-11-01.

## Automated checks

- Java: unit checks for equal-player probabilities, symmetry, surface sensitivity, malformed draws, exact enumeration agreement, payout conservation and probability mass across 2–64 slots.
- API/database: real SQL migration, insert and reload, input validation, missing runs, preview without persistence and disconnected-provider status.
- TypeScript production build and draw-import tests (order, duplicate/unknown players, invalid sizes, payout schedule and currency).
- Python: future data cannot change past features, overlapping-event embargo, frozen equal-date batches, logistic symmetry and proper-score formulas.
- GitHub Actions runs the Java suite separately with H2 and PostgreSQL, plus frontend and Python checks.

## Browser checks

The local React frontend was tested against the running Spring Boot backend, not mocked responses:

- Initial 8-player preview, with no extra history item.
- Run and persist a prediction.
- Change hard court to clay; a different title favorite and probabilities appear.
- Compare Sinner and Alcaraz on clay (24.0% / 76.0% with the illustrative ratings).
- Reject duplicate-player import, then import a valid four-player grass draw.

## Research

Downloaded ATP 2018–2024 from the revision recorded in `EXPERIMENT_001.md`; checked provenance, trained logistic regression on 12,446 matches and evaluated later years. Full local outputs are ignored by Git. The tracked experiment report contains aggregate results and attribution.

## Limits

No API Tennis key was provided, so an authenticated provider response was not exercised. The disconnected state was checked. No production deployment, authentication or DL serving path is claimed. Future improvements need their own tests and evidence.
