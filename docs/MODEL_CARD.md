# Model card: surface Elo v1

**Status:** illustrative baseline, not fitted or calibrated. **Intended use:** education and software demonstration.

## Inputs

Player identity, selected surface and a static rating per surface. All included ratings are illustrative. No current ranking, injury feed, future result or claim of live data is implied.

## Match probability

`p(i,j) = 1 / (1 + 10^((r_j-r_i)/400))`.

Symmetry: `p(i,j) + p(j,i) = 1`. Equal ratings imply 50%. The engine does not round intermediate probabilities.

## Draw probabilities

Let `q(i,0)=1`. At round `r`, player i's opponents are the players in the opposite half of i's bracket block of size `2^r`:

`q(i,r) = q(i,r-1) * sum_j(q(j,r-1) * p(i,j))`.

Thus the final-round probabilities sum to 1. `q(i,r)` is the probability of **winning round r**, not merely appearing in it. Pairwise probabilities remain constant throughout the event. No byes, withdrawals or reseeding are supported. Ratings are not updated during a hypothetical tournament.

## Expected prize money

For R rounds and total prizes `v[0..R]` in ascending finishing order (first-round exit to champion):

`E[prize_i] = sum_{r=0..R-1} ((q(i,r)-q(i,r+1))*v[r]) + q(i,R)*v[R]`.

This is gross tournament prize money in the declared currency. It is not annual earnings, salary, profit or net income.

## Illustrative path

The displayed bracket chooses the favorite in each conditional matchup. It is one possible path. It does not mean that the whole displayed path is the most probable joint tournament outcome. The probability table integrates over **all** possible paths.

## Evaluation and limitations

No validation metrics are reported for synthetic demo ratings. The Python workflow computes metrics from supplied historical matches. It builds pre-tournament Elo features in batches and applies result updates only 21 days after each tournament's start. This explicit embargo prevents leaking results from overlapping tournaments when true match timestamps are unavailable. It assumes all included event results are available after 21 days; production ingestion must verify actual timestamps. Match statistics from the event must not become pre-match features.

Training rows are mirrored to prevent learning "the first player always wins" from winner/loser CSV columns. Validation uses a later year; test uses a still later year. Training labels also obey the 21-day embargo before validation starts. The test is prequential: coefficients stay fixed and ratings incorporate results only after the embargo. This approximates repeated forecasts through a season, not an entire-season forecast made on January 1.

The research model is logistic regression with one antisymmetric Elo-difference feature and no intercept. Feature scaling, architecture and regularization are fixed before holdout evaluation. Do not repeatedly select models on the final test set. Time-based validation is still limited by missing true match timestamps, survivorship, changing player strength and incomplete source coverage.

## Roadmap

1. Validate source provenance and licensing; download versioned snapshots with checksums.
2. Train chronological global and surface Elo; export timestamped player ratings.
3. Add pre-match features and a regularized logistic baseline; compare with fixed Elo and 50%.
4. Evaluate calibration, log loss and Brier score by surface and period.
5. Investigate gradient boosting and optional PyTorch MLP only after the baseline is stable.
6. Deploy a model only after tracking its version, data cutoff and evaluation report in the API.
