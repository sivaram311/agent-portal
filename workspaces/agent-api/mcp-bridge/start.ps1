# Start Agent Portal MCP bridge for Gemini Spark (PROD :5430)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if (-not (Test-Path .env)) {
  throw "Missing .env — copy .env.example and set CSS_PASSWORD / MCP_BEARER_TOKEN"
}
if (-not (Test-Path node_modules)) {
  npm install
}
Write-Host "Starting MCP bridge on :5430 ..."
node --env-file=.env server.js
