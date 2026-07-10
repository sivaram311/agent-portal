#Requires -Version 5.1
<#
.SYNOPSIS
  Bring up Docker deps (Postgres, CSS, frontend) and run the portal backend on the host.

.DESCRIPTION
  Hybrid “as-is” deploy: agent/agy CLIs stay on Windows; only CSS/DB/UI run in Docker.
  Copy .env.docker.example to .env first (or pass -PublicHost).
#>
param(
  [string]$PublicHost = "",
  [switch]$SkipBuild,
  [switch]$NoBackend
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

function Read-DotEnv([string]$Path) {
  $map = @{}
  if (-not (Test-Path $Path)) { return $map }
  Get-Content $Path | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith("#")) { return }
    $i = $line.IndexOf("=")
    if ($i -lt 1) { return }
    $map[$line.Substring(0, $i).Trim()] = $line.Substring($i + 1).Trim()
  }
  return $map
}

$envFile = Join-Path $Root ".env"
if (-not (Test-Path $envFile) -and (Test-Path (Join-Path $Root ".env.docker.example"))) {
  Copy-Item (Join-Path $Root ".env.docker.example") $envFile
  Write-Host "Created .env from .env.docker.example — edit PUBLIC_HOST if needed."
}

$dotenv = Read-DotEnv $envFile
if (-not $PublicHost) {
  $PublicHost = if ($dotenv["PUBLIC_HOST"]) { $dotenv["PUBLIC_HOST"] } else { "127.0.0.1" }
}

# Free ports used by Docker services / host backend if stale listeners remain
foreach ($port in 4200, 9000, 5432) {
  Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
    ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
}

$composeArgs = @("compose", "--env-file", $envFile, "up", "-d")
if (-not $SkipBuild) { $composeArgs += "--build" }
$composeArgs += @("postgres", "css", "frontend")

Write-Host "Starting Docker services: postgres css frontend (PUBLIC_HOST=$PublicHost)"
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  throw "docker not found on PATH. Install Docker Desktop for Windows, then re-run this script."
}
& docker @composeArgs
if ($LASTEXITCODE -ne 0) { throw "docker compose failed with exit $LASTEXITCODE" }

Write-Host "Waiting for CSS health..."
$deadline = (Get-Date).AddMinutes(3)
do {
  try {
    $h = Invoke-RestMethod "http://127.0.0.1:9000/actuator/health" -TimeoutSec 3
    if ($h.status -eq "UP") { break }
  } catch {
    Start-Sleep -Seconds 3
  }
  if ((Get-Date) -gt $deadline) { throw "CSS did not become healthy in time" }
} while ($true)
Write-Host "CSS is UP"

if ($NoBackend) {
  Write-Host "Skipping host backend (-NoBackend). UI: http://${PublicHost}:4200"
  exit 0
}

# Stop anything already on 8080
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
  ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
Start-Sleep -Seconds 1

$backendDir = Join-Path $Root "backend"
Set-Location $backendDir

# Ensure starter is installed for local package
$starter = Join-Path (Split-Path $Root -Parent) "centralized-security-system\clients\spring-boot-starter"
if (Test-Path $starter) {
  Push-Location $starter
  mvn -q -DskipTests install
  Pop-Location
}

Write-Host "Building backend jar..."
& .\mvnw.cmd -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "backend package failed" }

$env:CSS_ENABLED = "true"
$env:CSS_AUTH_URL = if ($dotenv["CSS_AUTH_URL"]) { $dotenv["CSS_AUTH_URL"] } else { "http://${PublicHost}:9000" }
$env:CSS_JWKS_URI = if ($dotenv["CSS_JWKS_URI"]) { $dotenv["CSS_JWKS_URI"] } else { "http://localhost:9000/.well-known/jwks.json" }
$env:CSS_ISSUER = if ($dotenv["CSS_ISSUER"]) { $dotenv["CSS_ISSUER"] } else { "http://localhost:9000" }
$env:APP_CORS_ORIGINS = if ($dotenv["APP_CORS_ORIGINS"]) {
  $dotenv["APP_CORS_ORIGINS"]
} else {
  "http://${PublicHost}:4200,http://localhost:4200,http://127.0.0.1:4200"
}
$env:SPRING_PROFILES_ACTIVE = "postgres"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/agentportal"
$env:SPRING_DATASOURCE_USERNAME = "agent"
$env:SPRING_DATASOURCE_PASSWORD = if ($dotenv["POSTGRES_PASSWORD"]) { $dotenv["POSTGRES_PASSWORD"] } else { "agent" }
$env:AGENT_WORKSPACE_ROOT = Join-Path $Root "workspaces"

Write-Host "Starting host backend on :8080 (agent/agy on PATH)..."
Write-Host "UI http://${PublicHost}:4200  API http://${PublicHost}:8080  CSS http://${PublicHost}:9000"
java -jar (Join-Path $backendDir "target\backend-0.0.1-SNAPSHOT.jar")
