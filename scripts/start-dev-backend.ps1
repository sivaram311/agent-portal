#Requires -Version 5.1
<#
.SYNOPSIS
  Start (or restart) only the Agent Portal DEV backend, as a detached Windows
  process that survives the launching shell/session ending.

.DESCRIPTION
  Unlike run-host-stack.ps1, this does NOT touch CSS or the frontend -- use
  it when css-next (:5910) and the Angular dev server (:4200) are already
  running and only the Spring Boot API (:8080) needs to (re)start.

  Reads .env for CSS_*, APP_CORS_ORIGINS, AGENT_WORKSPACE_ROOT the same way
  run-host-stack.ps1 does. Because css.resource-server.* and css.* in
  application.properties both resolve from the *same* env var names
  (CSS_ENABLED, CSS_JWKS_URI, CSS_ISSUER), setting real Windows process env
  vars before Start-Process covers both property groups in one pass --
  unlike passing them as `-D` JVM args one group at a time, which is easy to
  under-cover (see ACTIVITY-LOG 2026-07-26 agent-portal-dev-backend-restart).
#>
param(
  [switch]$Postgres
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

function Stop-Port([int]$Port) {
  Get-NetTCPConnection -LocalPort $Port -State Listen -EA SilentlyContinue |
    ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -EA SilentlyContinue }
}

$dotenv = Read-DotEnv (Join-Path $Root ".env")

Stop-Port 8080
Start-Sleep 1

Push-Location (Join-Path $Root "backend")
.\mvnw.cmd -q -DskipTests package
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "backend package failed" }
Pop-Location

$env:CSS_ENABLED = if ($dotenv["CSS_ENABLED"]) { $dotenv["CSS_ENABLED"] } else { "true" }
$env:CSS_AUTH_URL = if ($dotenv["CSS_AUTH_URL"]) { $dotenv["CSS_AUTH_URL"] } else { "https://delena.buzz" }
$env:CSS_JWKS_URI = if ($dotenv["CSS_JWKS_URI"]) { $dotenv["CSS_JWKS_URI"] } else { "http://127.0.0.1:5910/.well-known/jwks.json" }
$env:CSS_ISSUER = if ($dotenv["CSS_ISSUER"]) { $dotenv["CSS_ISSUER"] } else { "https://css-next.delena.buzz" }
$env:CSS_AUTH_MODE = if ($dotenv["CSS_AUTH_MODE"]) { $dotenv["CSS_AUTH_MODE"] } else { "hybrid" }
$env:CSS_OAUTH_REDIRECT_PATH = if ($dotenv["CSS_OAUTH_REDIRECT_PATH"]) { $dotenv["CSS_OAUTH_REDIRECT_PATH"] } else { "/oauth/callback" }
$env:APP_CORS_ORIGINS = if ($dotenv["APP_CORS_ORIGINS"]) { $dotenv["APP_CORS_ORIGINS"] } else { "*" }
$env:AGENT_WORKSPACE_ROOT = if ($dotenv["AGENT_WORKSPACE_ROOT"]) { $dotenv["AGENT_WORKSPACE_ROOT"] } else { Join-Path $Root "workspaces" }
if ($dotenv["AGENT_WORKSPACE_ALLOWED_ROOTS"]) { $env:AGENT_WORKSPACE_ALLOWED_ROOTS = $dotenv["AGENT_WORKSPACE_ALLOWED_ROOTS"] }

$cursorAgentDir = Join-Path $env:LOCALAPPDATA "cursor-agent"
$defaultAgentCmd = Join-Path $cursorAgentDir "agent.cmd"
$env:CURSOR_AGENT_CMD = if ($dotenv["CURSOR_AGENT_CMD"]) { $dotenv["CURSOR_AGENT_CMD"] } else { $defaultAgentCmd }
if ($dotenv["CURSOR_API_KEY"]) { $env:CURSOR_API_KEY = $dotenv["CURSOR_API_KEY"] }
$env:AGENT_DEFAULT_AUTO_APPROVE = if ($dotenv["AGENT_DEFAULT_AUTO_APPROVE"]) { $dotenv["AGENT_DEFAULT_AUTO_APPROVE"] } else { "true" }
if (Test-Path $cursorAgentDir) { $env:Path = "$cursorAgentDir;" + $env:Path }

if ($Postgres) {
  $env:SPRING_PROFILES_ACTIVE = "postgres"
  $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/agentportal"
  $env:SPRING_DATASOURCE_USERNAME = if ($dotenv["POSTGRES_USER"]) { $dotenv["POSTGRES_USER"] } else { "agent" }
  $env:SPRING_DATASOURCE_PASSWORD = if ($dotenv["POSTGRES_PASSWORD"]) { $dotenv["POSTGRES_PASSWORD"] } else { "agent" }
} else {
  Remove-Item Env:SPRING_PROFILES_ACTIVE -EA SilentlyContinue
}

New-Item -ItemType Directory -Force -Path (Join-Path $Root "logs") | Out-Null
$beOut = Join-Path $Root "logs\backend.out.log"
$beErr = Join-Path $Root "logs\backend.err.log"
$beJar = Join-Path $Root "backend\target\backend-0.0.1-SNAPSHOT.jar"

Start-Process -FilePath "java" -ArgumentList @("-jar", $beJar) `
  -WorkingDirectory (Join-Path $Root "backend") `
  -RedirectStandardOutput $beOut -RedirectStandardError $beErr -WindowStyle Hidden

$deadline = (Get-Date).AddMinutes(2)
do {
  try {
    $h = Invoke-RestMethod "http://127.0.0.1:8080/api/health" -TimeoutSec 2
    if ($h.status -eq "ok") { Write-Host "Backend UP"; break }
  } catch { Start-Sleep 2 }
  if ((Get-Date) -gt $deadline) { throw "Backend health timeout - see logs\backend.err.log" }
} while ($true)

$cfg = Invoke-RestMethod "http://127.0.0.1:8080/api/auth/config"
Write-Host ("cssEnabled=" + $cfg.cssEnabled + " authUrl=" + $cfg.authUrl + " issuer=" + $cfg.issuer)
