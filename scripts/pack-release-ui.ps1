#Requires -Version 5.1
<#
.SYNOPSIS
  Copy Angular production UI into a flat nginx-ready folder.

.DESCRIPTION
  Angular :application builder emits dist/<project>/browser/.
  Nginx roots (F:/G:/apps/agent-portal/ui) expect index.html at the root of ui/,
  not under ui/browser/. Always use this helper (or copy browser/*) when packing releases.
#>
param(
  [string]$BrowserSrc = "E:\MyWorkspace\agent-portal\frontend\dist\frontend\browser",
  [Parameter(Mandatory = $true)]
  [string]$DestUiDir
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path (Join-Path $BrowserSrc 'index.html'))) {
  throw "Missing $BrowserSrc\index.html - run: npm run build (frontend)"
}

if (Test-Path $DestUiDir) {
  Remove-Item $DestUiDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $DestUiDir | Out-Null
Copy-Item -Recurse -Force (Join-Path $BrowserSrc '*') $DestUiDir

$index = Join-Path $DestUiDir 'index.html'
if (-not (Test-Path $index)) {
  throw "Pack failed: $index not found (did you copy dist/frontend instead of dist/frontend/browser?)"
}

Write-Host "OK flat UI -> $DestUiDir"
