# Experiment 001: does fitted logistic regression beat Elo?

Run on **2026-11-01**. This is an offline historical experiment; these models do not supply the dashboard's illustrative player ratings.

## Source and reproducibility

Jeff Sackmann's ATP match CSVs, 2018–2024, via the [archival mirror](https://github.com/Aneeshers/tennis-sackmann-archive/tree/83733587353df8a41f2fd4f516147d5aa83f5a8d/atp). Exact revision: `83733587353df8a41f2fd4f516147d5aa83f5a8d`. Attribution and data terms: **CC BY-NC-SA 4.0**. This data-derived report is shared under [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/). No project-wide code license has been selected.

```powershell
python research/download.py --start-year 2018 --end-year 2024 --revision 83733587353df8a41f2fd4f516147d5aa83f5a8d
python research/evaluate.py --train-through 2022 --validation-year 2023 --test-year 2024
```

The downloader records each file's SHA-256; evaluation verifies the recorded checksums. Full local output: `data/research/report.json`.

## Data quality and split

- 18,877 records read; 18,256 accepted.
- 568 incomplete/retired/walkover records excluded; 53 unsupported-surface records excluded.
- 12,446 training matches through 2022; 2,837 validation matches in 2023; 2,973 test matches in 2024.
- Tournament start dates stand in for match dates. Result updates use a conservative **21-day embargo**, including overlapping events. This is an explicit availability assumption, not verified feed latency.
- Global Elo: initial rating 1500, K=24. Logistic feature: `(Elo_A - Elo_B) / 400`; no intercept; fixed L2=0.01, 1,200 batch gradient steps, learning rate 0.15.
- Mirrored training rows preserve pairwise symmetry. Fitted coefficient: **1.5081238906**. No hyperparameter search or test-set model selection was performed.

## Results

Lower log loss and Brier score are better. Accuracy awards 0.5 for an exact tie and is reported on real matches, not duplicated rows.

| Model | 2023 log loss | 2023 Brier | 2024 log loss | 2024 Brier | 2024 accuracy |
| --- | ---: | ---: | ---: | ---: | ---: |
| Constant 50% | 0.69315 | 0.25000 | 0.69315 | 0.25000 | 50.00% |
| Global Elo | 0.63731 | 0.22299 | **0.62328** | **0.21809** | 64.04% |
| Logistic on Elo | **0.63596** | **0.22276** | 0.62645 | 0.21887 | 64.04% |

The logistic model slightly improves validation scores but is worse than Elo on the final test. The same accuracy is expected: the fitted positive coefficient changes confidence, not the ordering of players. The observed score differences do not establish statistical significance.

## Decision

Keep Elo as the serving baseline. Do not claim that fitting a model improves this application. Before adding model complexity, verify timestamps, evaluate surface-specific features, inspect calibration and define a fresh future evaluation window. The 2024 test has now been observed and must not become a tuning target.

## Limits

This experiment uses one time split and a simplified, embargoed tournament-level rating update. It has no injury features, betting odds, match-format adjustment, calibration guarantee, confidence intervals or prospective trial. It does not validate the app's synthetic ratings, expected winnings or any live performance claim.
