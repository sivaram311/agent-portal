#Requires -Version 5.1
<#
.SYNOPSIS
  Host stack for Agent Portal on Windows Server + Docker CE (Windows containers).

.DESCRIPTION
  Docker CE here is OSType=windows - Linux images (postgres/nginx/temurin) cannot run.
  This script runs CSS + portal backend + Angular on the host. Uses H2 by default.
  Optional: -Postgres if you have local Postgres and set POSTGRES_PASSWORD in .env.
#>
param(
  [string]$PublicHost = "",
  [switch]$Postgres,
  [switch]$SkipFrontend,
  [switch]$SkipCss
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Workspace = Split-Path -Parent $Root
$CssRoot = Join-Path $Workspace "centralized-security-system"
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

$envFile = Join-Path $Root ".env"
if (-not (Test-Path $envFile) -and (Test-Path (Join-Path $Root ".env.docker.example"))) {
  Copy-Item (Join-Path $Root ".env.docker.example") $envFile
}
$dotenv = Read-DotEnv $envFile
if (-not $PublicHost) {
  $PublicHost = if ($dotenv["PUBLIC_HOST"]) { $dotenv["PUBLIC_HOST"] } else { "127.0.0.1" }
}

Write-Host "Host stack PUBLIC_HOST=$PublicHost (Docker CE Windows containers cannot run Linux images)"

if (-not $SkipCss) {
  Stop-Port 9000
  Start-Sleep 1
  Write-Host "Building/starting CSS on :9000..."
  $cssJar = Join-Path $CssRoot "target\centralized-security-system-0.1.0-SNAPSHOT.jar"
  Push-Location $CssRoot
  if (-not (Test-Path $cssJar)) {
    mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { Pop-Location; throw "CSS package failed" }
  }
  Pop-Location
  $cssOut = Join-Path $Root "logs\css.out.log"
  $cssErr = Join-Path $Root "logs\css.err.log"
  New-Item -ItemType Directory -Force -Path (Join-Path $Root "logs") | Out-Null
  Start-Process -FilePath "java" -ArgumentList @("-jar", $cssJar) `
    -WorkingDirectory $CssRoot -RedirectStandardOutput $cssOut -RedirectStandardError $cssErr -WindowStyle Hidden
  $deadline = (Get-Date).AddMinutes(2)
  do {
    try {
      $h = Invoke-RestMethod "http://127.0.0.1:9000/actuator/health" -TimeoutSec 2
      if ($h.status -eq "UP") { Write-Host "CSS UP"; break }
    } catch { Start-Sleep 2 }
    if ((Get-Date) -gt $deadline) { throw "CSS health timeout - see logs/css.err.log" }
  } while ($true)
}

Stop-Port 8080
Start-Sleep 1
$starter = Join-Path $CssRoot "clients\spring-boot-starter"
if (Test-Path $starter) {
  Push-Location $starter
  mvn -q -DskipTests install
  Pop-Location
}
Push-Location (Join-Path $Root "backend")
.\mvnw.cmd -q -DskipTests package
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "backend package failed" }
Pop-Location

$env:CSS_ENABLED = "true"
$env:CSS_AUTH_URL = if ($dotenv["CSS_AUTH_URL"]) { $dotenv["CSS_AUTH_URL"] } else { "http://${PublicHost}:9000" }
$env:CSS_JWKS_URI = if ($dotenv["CSS_JWKS_URI"]) { $dotenv["CSS_JWKS_URI"] } else { "http://localhost:9000/.well-known/jwks.json" }
$env:CSS_ISSUER = if ($dotenv["CSS_ISSUER"]) { $dotenv["CSS_ISSUER"] } else { "http://localhost:9000" }
$env:APP_CORS_ORIGINS = if ($dotenv["APP_CORS_ORIGINS"]) {
  $dotenv["APP_CORS_ORIGINS"]
} else {
  "http://${PublicHost}:4200,http://localhost:4200,http://127.0.0.1:4200"
}
$env:AGENT_WORKSPACE_ROOT = if ($dotenv["AGENT_WORKSPACE_ROOT"]) {
  $dotenv["AGENT_WORKSPACE_ROOT"]
} else {
  # Phase 1: sandbox under MyWorkspace (junctions to legacy workspaces/*)
  Join-Path $Workspace "sandbox"
}

# Cursor agent: absolute .cmd + API key + ensure user PATH for child tools
$cursorAgentDir = Join-Path $env:LOCALAPPDATA "cursor-agent"
$defaultAgentCmd = Join-Path $cursorAgentDir "agent.cmd"
$env:CURSOR_AGENT_CMD = if ($dotenv["CURSOR_AGENT_CMD"]) { $dotenv["CURSOR_AGENT_CMD"] } else { $defaultAgentCmd }
if ($dotenv["CURSOR_API_KEY"]) { $env:CURSOR_API_KEY = $dotenv["CURSOR_API_KEY"] }
if ($dotenv["AGENT_DEFAULT_AUTO_APPROVE"]) {
  $env:AGENT_DEFAULT_AUTO_APPROVE = $dotenv["AGENT_DEFAULT_AUTO_APPROVE"]
} else {
  $env:AGENT_DEFAULT_AUTO_APPROVE = "true"
}
if (Test-Path $cursorAgentDir) {
  $env:Path = "$cursorAgentDir;" + $env:Path
}

if ($Postgres) {
  $env:SPRING_PROFILES_ACTIVE = "postgres"
  $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/agentportal"
  $env:SPRING_DATASOURCE_USERNAME = if ($dotenv["POSTGRES_USER"]) { $dotenv["POSTGRES_USER"] } else { "agent" }
  $env:SPRING_DATASOURCE_PASSWORD = if ($dotenv["POSTGRES_PASSWORD"]) { $dotenv["POSTGRES_PASSWORD"] } else { "agent" }
} else {
  Remove-Item Env:SPRING_PROFILES_ACTIVE -EA SilentlyContinue
}

$beOut = Join-Path $Root "logs\backend.out.log"
$beErr = Join-Path $Root "logs\backend.err.log"
$beJar = Join-Path $Root "backend\target\backend-0.0.1-SNAPSHOT.jar"
Write-Host "Backend CURSOR_AGENT_CMD=$($env:CURSOR_AGENT_CMD) apiKeyConfigured=$([bool]$env:CURSOR_API_KEY)"
Start-Process -FilePath "java" -ArgumentList @("-jar", $beJar) `
  -WorkingDirectory (Join-Path $Root "backend") `
  -RedirectStandardOutput $beOut -RedirectStandardError $beErr -WindowStyle Hidden

$deadline = (Get-Date).AddMinutes(2)
do {
  try {
    $h = Invoke-RestMethod "http://127.0.0.1:8080/api/health" -TimeoutSec 2
    if ($h.status -eq "ok") { Write-Host "Backend UP"; break }
  } catch { Start-Sleep 2 }
  if ((Get-Date) -gt $deadline) { throw "Backend health timeout - see logs/backend.err.log" }
} while ($true)

if (-not $SkipFrontend) {
  Stop-Port 4200
  Start-Sleep 1
  $feOut = Join-Path $Root "logs\frontend.out.log"
  $feErr = Join-Path $Root "logs\frontend.err.log"
  $feDir = Join-Path $Root "frontend"
  if (-not (Test-Path (Join-Path $feDir "node_modules"))) {
    Push-Location $feDir
    npm ci
    Pop-Location
  }
  # npm.cmd cannot use Start-Process stdout/stderr redirects reliably; use cmd redirection
  $feCmd = "npm start > `"$feOut`" 2> `"$feErr`""
  Start-Process -FilePath "cmd.exe" -ArgumentList @("/c", $feCmd) `
    -WorkingDirectory $feDir -WindowStyle Hidden
  $deadline = (Get-Date).AddMinutes(3)
  do {
    try {
      $r = Invoke-WebRequest "http://127.0.0.1:4200/" -UseBasicParsing -TimeoutSec 2
      if ($r.StatusCode -eq 200) { Write-Host "Frontend UP"; break }
    } catch { Start-Sleep 3 }
    if ((Get-Date) -gt $deadline) { throw "Frontend timeout - see logs/frontend.err.log" }
  } while ($true)
}

Write-Host ""
Write-Host "UI  http://${PublicHost}:4200"
Write-Host "API http://${PublicHost}:8080"
Write-Host "CSS http://${PublicHost}:9000"
Write-Host "Login admin / admin123"
$cfg = Invoke-RestMethod "http://127.0.0.1:8080/api/auth/config"
Write-Host ("authUrl=" + $cfg.authUrl)
