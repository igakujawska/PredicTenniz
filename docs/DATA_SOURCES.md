# Data sources and provenance

## Included demonstration data

Names identify real tennis players, but all ratings and the payout schedule are **illustrative project inputs**. They are not official rankings, current form estimates, a current tournament draw or official prize amounts. The demo is not a backtest.

## Optional historical research

- Original compiler: **Jeff Sackmann**.
- Original repositories: https://github.com/JeffSackmann/tennis_atp and https://github.com/JeffSackmann/tennis_wta.
- Original ATP URL returned 404 when checked on 2026-11-01.
- Available archival mirror: https://github.com/Aneeshers/tennis-sackmann-archive.
- Mirror snapshot: ATP/WTA upstream sources through June 2026; this is an archive, not a live feed.
- License: **Creative Commons Attribution-NonCommercial-ShareAlike 4.0**. https://creativecommons.org/licenses/by-nc-sa/4.0/

The download helper resolves the mirror's Git commit and records that commit, each source URL and SHA-256 checksum. Raw data and research outputs stay in ignored `data/`. Keep attribution and applicable terms with any redistribution. External data terms apply independently of any future code license selected for this project.

## Optional fixtures API

API Tennis: https://api-tennis.com/documentation. `get_fixtures` requires a provider key and subscription/trial access. A key is supplied only through `TENNIS_API_KEY` on the backend. This first adapter does not train models or automatically map provider players to local snapshots.

Fixture data stays on the provider request/response path and is not committed. Without a configured key, the UI clearly states that the integration is disconnected. The project has no assumed entitlement to historical, earnings, odds or live datasets.
