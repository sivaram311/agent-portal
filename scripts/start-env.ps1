#Requires -Version 5.1
param(
  [ValidateSet('preprod','prod')]
  [string]$EnvName = 'preprod'
)
$ErrorActionPreference = 'Stop'
$HomeDir = if ($EnvName -eq 'prod') { 'G:\apps\agent-portal' } else { 'F:\apps\agent-portal' }
$Port = if ($EnvName -eq 'prod') { 5080 } else { 4080 }
$Java = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\bin\java.exe'
if (-not (Test-Path $Java)) {
  $Java = (Get-Command java -ErrorAction Stop).Source
}
$Jar = Join-Path $HomeDir 'agent-portal.jar'
$EnvFile = Join-Path $HomeDir '.env'
if (-not (Test-Path $Jar)) { throw "Missing $Jar" }
if (-not (Test-Path $EnvFile)) { throw "Missing $EnvFile" }

if (netstat -ano | findstr 'LISTENING' | findstr ":$Port ") {
  Write-Host "Already listening on :$Port"
  exit 0
}

Get-Content $EnvFile | ForEach-Object {
  $line = $_.Trim()
  if (-not $line -or $line.StartsWith('#')) { return }
  $i = $line.IndexOf('=')
  if ($i -lt 1) { return }
  Set-Item -Path "env:$($line.Substring(0,$i).Trim())" -Value $line.Substring($i+1).Trim()
}

$outLog = Join-Path $HomeDir "agent-portal-$EnvName.out.log"
$errLog = Join-Path $HomeDir "agent-portal-$EnvName.err.log"
$ws = $env:AGENT_WORKSPACE_ROOT
if ($ws -and -not (Test-Path $ws)) { New-Item -ItemType Directory -Force -Path $ws | Out-Null }

$argList = @(
  '-jar', $Jar,
  "--spring.profiles.active=$($env:SPRING_PROFILES_ACTIVE)",
  "--server.port=$Port"
)
Write-Host "Starting agent-portal $EnvName on :$Port"
Start-Process -FilePath $Java -ArgumentList $argList -WorkingDirectory $HomeDir -RedirectStandardOutput $outLog -RedirectStandardError $errLog -WindowStyle Hidden

# Wait for listen + health (Spring Boot can exceed 12s after cold start)
$deadline = (Get-Date).AddSeconds(90)
$listening = $false
while ((Get-Date) -lt $deadline) {
  Start-Sleep -Seconds 3
  if (netstat -ano | findstr 'LISTENING' | findstr ":$Port ") {
    $listening = $true
    try {
      $h = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/api/health" -TimeoutSec 5
      if ($h.status -eq 'ok') {
        Write-Host "UP on :$Port (health ok)"
        exit 0
      }
    } catch {
      # still booting
    }
  }
}

if ($listening) {
  Write-Host "Listening on :$Port but /api/health not ready — see $errLog / $outLog"
} else {
  Write-Host "Failed to bind :$Port - see $errLog"
}
Get-Content $errLog -Tail 40 -ErrorAction SilentlyContinue
exit 1
