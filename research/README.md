# Historical research lab

A deliberately small, dependency-free Python workflow makes the assumptions inspectable. It compares a constant 50% forecast, global Elo, and a fitted one-feature logistic regression. **This workflow does not replace the app's illustrative surface ratings.**

## Download a versioned source

From the repository root:

```powershell
python research/download.py --start-year 2018 --end-year 2024
python research/evaluate.py --train-through 2022 --validation-year 2023 --test-year 2024
python -m unittest discover -s research/tests
```

The downloader resolves the archive's current Git commit, downloads each file at that exact revision, and writes source URLs and SHA-256 checksums to `data/raw/provenance.json`. It aborts rather than substituting another dataset on an error. Reuse `--revision <recorded-sha>` to reproduce a download.

Data: Jeff Sackmann via [Aneeshers/tennis-sackmann-archive](https://github.com/Aneeshers/tennis-sackmann-archive), **CC BY-NC-SA 4.0**. No bundled dataset or commercial data entitlement. The script's output remains under ignored `data/`.

## Method

- Validate required columns; count invalid records, duplicates, excluded surfaces and retirements/walkovers.
- Work only with supported hard, clay and grass singles matches.
- Source dates are tournament start dates, **not match timestamps**. Forecast each date batch using only tournaments whose start was at least **21 days earlier**. Apply Elo updates conservatively after that embargo, keeping equal-date batches frozen. This avoids using later rounds of overlapping events as if they had already happened.
- Treat 21 days as an explicit source-availability assumption, not verified publication latency. A production feed needs true result timestamps.
- Also embargo training labels: include only events starting at least 21 days before the validation year. Do not fit on a late-December event that could finish in January.
- Use global Elo differences divided by 400 as the only regression feature. No match aces, score, duration or other post-match feature enters a forecast.
- Mirror training observations `(x,1)` and `(-x,0)` to neutralize the winner/loser source layout. Do not count the mirrored rows as extra independent matches.
- Fit the scalar logistic coefficient with fixed L2 regularization and no intercept. This enforces pairwise symmetry. This is a pedagogical implementation of batch gradient descent, not a substitute for a general-purpose statistical library.
- Keep coefficients fixed through validation and test. Ratings update only when embargoed results become available. The evaluation is prequential, not a January-1 forecast for an entire season.
- Report log loss, Brier score and accuracy on symmetric observations. At exactly 50%, accuracy receives 0.5 tie credit.

The final test is not a tuning set. This workflow prints validation and test metrics together because it has **no model-selection loop**. If you begin tuning, hide the final test until the end and document your choice using validation alone.

## Outputs

`data/research/report.json`: data-quality counts, chronological split sizes, embargo settings, file checksums, fitted coefficient and metrics. `data/research/model.json`: an offline experiment artifact with its feature definition and training cutoff. The app does not load it.

## Exercises

1. Change the Elo K-factor using validation only. Explain what changes.
2. Add surface-specific Elo without leaking match outcomes.
3. Add ranking difference with train-only missing-value handling and scaling.
4. Plot calibration bins and check subgroup sample sizes.
5. Reimplement the logistic model with scikit-learn and compare probabilities.
6. Try a small PyTorch MLP under identical data and split rules. Deep learning is a future experiment, not part of the serving path yet.
