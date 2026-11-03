param([ValidateRange(1, 1000)][int]$Top = 500)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$importDirectory = Join-Path $projectRoot 'target/import'
[System.IO.Directory]::CreateDirectory($importDirectory) | Out-Null
$revision = '83733587353df8a41f2fd4f516147d5aa83f5a8d'
$sourceBase = "https://raw.githubusercontent.com/Aneeshers/tennis-sackmann-archive/$revision/atp"
$playersPath = Join-Path $importDirectory 'atp_players.csv'
$rankingsPath = Join-Path $importDirectory 'atp_rankings_current.csv'
Invoke-WebRequest -Uri "$sourceBase/atp_players.csv" -OutFile $playersPath -TimeoutSec 60
Invoke-WebRequest -Uri "$sourceBase/atp_rankings_current.csv" -OutFile $rankingsPath -TimeoutSec 60
$rankings = Import-Csv -LiteralPath $rankingsPath
$rankingDate = ($rankings | Sort-Object ranking_date -Descending | Select-Object -First 1).ranking_date
$selectedIds = [System.Collections.Generic.HashSet[string]]::new()
foreach ($ranking in $rankings) {
    if ($ranking.ranking_date -eq $rankingDate -and [int]$ranking.rank -le $Top) {
        [void]$selectedIds.Add($ranking.player)
    }
}
if ($selectedIds.Count -lt $Top) { throw "The pinned snapshot contains fewer than $Top ranked profiles." }
$players = @(Import-Csv -LiteralPath $playersPath | Where-Object { $selectedIds.Contains($_.player_id) })
if ($players.Count -eq 0 -or $players.Count -ne $selectedIds.Count) {
    throw 'Ranking IDs could not be matched unambiguously to player profiles.'
}
$outputPath = Join-Path $importDirectory "players-top$Top.csv"
$players | Export-Csv -LiteralPath $outputPath -NoTypeInformation -Encoding UTF8
$manifestPath = Join-Path $importDirectory "players-top$Top.manifest.json"
$manifest = [ordered]@{
    compiler = 'Jeff Sackmann'
    mirror = 'Aneeshers/tennis-sackmann-archive'
    revision = $revision
    license = 'CC BY-NC-SA 4.0'
    rankingDate = $rankingDate
    maximumRank = $Top
    selectedPlayers = $players.Count
    playersSource = "$sourceBase/atp_players.csv"
    playersSha256 = (Get-FileHash -LiteralPath $playersPath -Algorithm SHA256).Hash.ToLowerInvariant()
    rankingsSource = "$sourceBase/atp_rankings_current.csv"
    rankingsSha256 = (Get-FileHash -LiteralPath $rankingsPath -Algorithm SHA256).Hash.ToLowerInvariant()
    selectedCsvSha256 = (Get-FileHash -LiteralPath $outputPath -Algorithm SHA256).Hash.ToLowerInvariant()
}
$manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $manifestPath -Encoding UTF8
[pscustomobject]@{ RankingDate = $rankingDate; Players = $players.Count; Csv = $outputPath; Manifest = $manifestPath }
