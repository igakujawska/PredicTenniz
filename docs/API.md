# API examples

Start the backend before running these examples. All monetary values are total gross payouts in the requested currency.

```http
POST /api/simulations
Content-Type: application/json

{
  "title": "My quarter-finals",
  "surface": "CLAY",
  "playerIds": ["sinner", "hurkacz", "medvedev", "fritz", "alcaraz", "ruud", "djokovic", "zverev"],
  "prizes": [25000, 50000, 100000, 200000],
  "currency": "EUR"
}
```

Returns `201` with an ID, model version, `ILLUSTRATIVE` data mode, conditional favorite bracket and standings. An 8-player draw needs four prize values: quarter-final exit, semi-final exit, runner-up, champion. Each `advancement[r]` is the probability of winning round `r+1`.

```http
POST /api/predictions
Content-Type: application/json

{"firstId":"sinner","secondId":"alcaraz","surface":"HARD"}
```

Errors use Problem Details JSON. Invalid draws return `400`; an unknown saved run returns `404`; an unconfigured provider returns `503`; a provider failure returns `502`. Provider secrets and upstream error URLs are not exposed.

Draw upload is JSON only in v1. A screenshot/PDF draw would require OCR plus a user confirmation step to verify names and bracket positions. That feature is deliberately deferred until a structured draw import is reliable.
