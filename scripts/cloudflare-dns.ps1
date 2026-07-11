#Requires -Version 5.1
# Ops-owned; claim port via /api/platform/ports first; prefer delena.buzz subdomains; see docs/platform/CLOUDFLARE-DNS-PROXY.md
<#
.SYNOPSIS
  Cloudflare DNS helper for Agent Portal.

.DESCRIPTION
  Reads Cloudflare settings from .env and lists, upserts, or deletes DNS records.
  The API token is used only in Authorization headers and is never printed.
#>
param(
  [switch]$List,
  [switch]$Upsert,
  [switch]$Delete,
  [string]$Name = "",
  [string]$Content = "",
  [ValidateSet("A", "CNAME")]
  [string]$Type = "A",
  [switch]$Proxied
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $Root ".env"

function Read-DotEnv([string]$Path) {
  $map = @{}
  if (-not (Test-Path $Path)) { return $map }

  Get-Content $Path | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith("#")) { return }

    $i = $line.IndexOf("=")
    if ($i -lt 1) { return }

    $key = $line.Substring(0, $i).Trim()
    $value = $line.Substring($i + 1).Trim()
    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
      $value = $value.Substring(1, $value.Length - 2)
    }
    $map[$key] = $value
  }

  return $map
}

function Require-Setting([hashtable]$Settings, [string]$Key) {
  if (-not $Settings.ContainsKey($Key) -or [string]::IsNullOrWhiteSpace($Settings[$Key])) {
    throw "Missing $Key in $EnvFile"
  }
  return $Settings[$Key]
}

function Resolve-RecordName([string]$RecordName, [string]$ZoneName) {
  if ([string]::IsNullOrWhiteSpace($RecordName)) {
    throw "Name is required."
  }

  $resolved = $RecordName.Trim().TrimEnd(".")
  if ($resolved.IndexOf(".") -lt 0) {
    $resolved = "$resolved.$ZoneName"
  }
  return $resolved
}

function Invoke-Cloudflare([string]$Method, [string]$Url, [string]$Token, [string]$Body = "") {
  # Write JSON to a temp file — PowerShell + curl.exe --data mangles braces/quotes (CF error 9207).
  $args = @(
    "-sS",
    "-X", $Method,
    "-H", "Authorization: Bearer $Token",
    "-H", "Content-Type: application/json"
  )

  $tmp = $null
  try {
    if ($Body) {
      $tmp = Join-Path $env:TEMP ("cf-dns-" + [guid]::NewGuid().ToString() + ".json")
      Set-Content -Path $tmp -Value $Body -NoNewline -Encoding Ascii
      $args += @("--data-binary", "@$tmp")
    }

    $args += $Url
    $response = & curl.exe @args
    if ($LASTEXITCODE -ne 0) {
      throw "curl.exe failed with exit $LASTEXITCODE"
    }

    try {
      return ($response | ConvertFrom-Json)
    } catch {
      throw "Cloudflare returned non-JSON response."
    }
  } finally {
    if ($tmp -and (Test-Path $tmp)) { Remove-Item $tmp -Force -ErrorAction SilentlyContinue }
  }
}

function Assert-CloudflareSuccess($Response, [string]$Action) {
  if (-not $Response.success) {
    $messages = @()
    if ($Response.errors) {
      $messages = $Response.errors | ForEach-Object {
        if ($_.message) { $_.message } else { $_ | ConvertTo-Json -Compress }
      }
    }
    if (-not $messages -or $messages.Count -eq 0) {
      $messages = @("Unknown Cloudflare API error")
    }
    throw "$Action failed: $($messages -join '; ')"
  }
}

function Get-Records([string]$BaseUrl, [string]$Token, [string]$RecordName, [string]$RecordType) {
  $query = ""
  if ($RecordName) {
    $query += "&name=$([uri]::EscapeDataString($RecordName))"
  }
  if ($RecordType) {
    $query += "&type=$([uri]::EscapeDataString($RecordType))"
  }

  $url = "${BaseUrl}?per_page=100$query"
  $response = Invoke-Cloudflare "GET" $url $Token
  Assert-CloudflareSuccess $response "List DNS records"
  return @($response.result)
}

$operationCount = 0
if ($List) { $operationCount++ }
if ($Upsert) { $operationCount++ }
if ($Delete) { $operationCount++ }
if ($operationCount -ne 1) {
  throw "Specify exactly one command: -List, -Upsert, or -Delete."
}

$settings = Read-DotEnv $EnvFile
$token = Require-Setting $settings "CLOUDFLARE_API_TOKEN"
$zoneId = Require-Setting $settings "CLOUDFLARE_ZONE_ID"
$zoneName = Require-Setting $settings "CLOUDFLARE_ZONE_NAME"
$baseUrl = "https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records"

if ($List) {
  $records = Get-Records $baseUrl $token "" ""
  $records | Select-Object name, type, content, id | Format-Table -AutoSize
  exit 0
}

if ($Upsert) {
  $recordName = Resolve-RecordName $Name $zoneName
  if ([string]::IsNullOrWhiteSpace($Content)) {
    if ($settings.ContainsKey("PUBLIC_HOST") -and -not [string]::IsNullOrWhiteSpace($settings["PUBLIC_HOST"])) {
      $Content = $settings["PUBLIC_HOST"]
    } else {
      throw "Content is required when PUBLIC_HOST is missing from $EnvFile"
    }
  }

  $existing = Get-Records $baseUrl $token $recordName $Type
  $bodyObject = @{
    type = $Type
    name = $recordName
    content = $Content
    proxied = [bool]$Proxied
  }
  $body = $bodyObject | ConvertTo-Json -Compress

  if ($existing.Count -gt 0) {
    $recordId = $existing[0].id
    $response = Invoke-Cloudflare "PUT" "${baseUrl}/$recordId" $token $body
    Assert-CloudflareSuccess $response "Update DNS record"
    Write-Host "Updated $($response.result.name) $($response.result.type) -> $($response.result.content) id=$($response.result.id)"
  } else {
    $response = Invoke-Cloudflare "POST" $baseUrl $token $body
    Assert-CloudflareSuccess $response "Create DNS record"
    Write-Host "Created $($response.result.name) $($response.result.type) -> $($response.result.content) id=$($response.result.id)"
  }
  exit 0
}

if ($Delete) {
  $recordName = Resolve-RecordName $Name $zoneName
  $deleteType = if ($PSBoundParameters.ContainsKey("Type")) { $Type } else { "" }
  $records = Get-Records $baseUrl $token $recordName $deleteType

  if ($records.Count -eq 0) {
    Write-Host "No matching DNS records found for $recordName"
    exit 0
  }

  foreach ($record in $records) {
    $response = Invoke-Cloudflare "DELETE" "${baseUrl}/$($record.id)" $token
    Assert-CloudflareSuccess $response "Delete DNS record"
    Write-Host "Deleted $($record.name) $($record.type) id=$($record.id)"
  }
  exit 0
}
