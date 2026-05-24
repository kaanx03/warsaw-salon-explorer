# Load .env from project root and start the app in local profile
$envFile = Join-Path $PSScriptRoot "..\.env"
Get-Content $envFile | Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } | ForEach-Object {
    $parts = $_ -split '=', 2
    Set-Item -Path "env:$($parts[0].Trim())" -Value $parts[1].Trim()
}
$env:SPRING_PROFILES_ACTIVE = "local"
mvn spring-boot:run
