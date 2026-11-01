param([switch]$Test)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

if (-not $env:JAVA_HOME) {
    $javaRoot = Join-Path $env:ProgramFiles 'Java'
    $jdk = Get-ChildItem -LiteralPath $javaRoot -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    throw 'Set JAVA_HOME to your JDK 21 installation, then run this script again.'
}

Push-Location (Join-Path $projectRoot 'backend')
try {
    if ($Test) { & .\mvnw.cmd verify }
    else { & .\mvnw.cmd spring-boot:run }
    if ($LASTEXITCODE -ne 0) { throw "Backend command exited with code $LASTEXITCODE." }
} finally { Pop-Location }
