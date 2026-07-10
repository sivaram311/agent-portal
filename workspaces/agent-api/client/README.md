# Agent API PowerShell Client

PowerShell helper functions for Agent Portal's Agent API. The client is a module-style script that stores connection settings in script scope after you dot-source it.

## Quick Start

From this folder:

```powershell
. .\AgentApi.ps1
Connect-AgentApi -Username "<username>" -Password "<password>"
Get-AgentHealth
```

Use local CSS development credentials when testing on your machine. Do not store credentials in scripts or checked-in files.

Defaults:

```text
BaseUrl:  http://127.0.0.1:8080/api
CssUrl:   http://127.0.0.1:9000
ClientId: agent-portal
```

## API Key Mode

Use this when the portal API is configured for `X-API-Key` fallback and CSS login is not needed:

```powershell
. .\AgentApi.ps1
Connect-AgentApi -ApiKey "<api-key>"
Get-AgentHealth
```

## Basic Session Flow

```powershell
. .\AgentApi.ps1
Connect-AgentApi -Username "<username>" -Password "<password>"

$session = New-AgentSession -Title "Agent API bridge"
Send-AgentPrompt -SessionId $session.id -Prompt "Summarize the agent-api workspace."
Get-AgentMessages -SessionId $session.id
Stop-AgentRun -SessionId $session.id
```

`New-AgentSession` defaults `-WorkspacePath` to `agent-api` and `-Provider` to `cursor`.

## Review Changes

```powershell
$changes = Get-AgentChanges -SessionId $session.id
Get-AgentChangeDiff -SessionId $session.id -Path "README.md"
Keep-AgentChange -SessionId $session.id -Path "README.md"
Restore-AgentChange -SessionId $session.id -Path "README.md"
```

## Permissions

```powershell
Get-AgentPermissions -SessionId $session.id
Resolve-AgentPermission -SessionId $session.id -PermissionId "<permission-id>" -Decision allow-once
```

Supported decisions include `accept`, `allow-once`, `allow-always`, `reject`, `reject-once`, `deny`, and `always`.

## Files, Collaborators, And Guidance

```powershell
Get-AgentFiles -SessionId $session.id
Get-AgentFileContent -SessionId $session.id -Path "README.md"

Get-AgentCollaborators -SessionId $session.id
Add-AgentCollaborator -SessionId $session.id -Username "demo"
Remove-AgentCollaborator -SessionId $session.id -Username "demo"

Get-AgentGuidance -SessionId $session.id
Get-AgentGuidancePacks
Get-AgentGuidanceDefaults
Get-AgentGuidanceTemplates
```

## Public URL Example

When using the public HTTPS site, pass both URLs explicitly:

```powershell
Connect-AgentApi `
  -Username "<username>" `
  -Password "<password>" `
  -BaseUrl "https://delena.buzz/api" `
  -CssUrl "https://delena.buzz/auth"
```

## Function List

Core functions include:

```text
Connect-AgentApi
Disconnect-AgentApi
Set-AgentApiConfig
Get-AgentApiConfig
Invoke-AgentApi
Get-AgentHealth
Get-AgentAuthConfig
Get-AgentPresets
Get-AgentSessions
Get-AgentSession
New-AgentSession
Send-AgentPrompt
Get-AgentMessages
Get-AgentTools
Get-AgentPermissions
Resolve-AgentPermission
Stop-AgentRun
Abandon-AgentSubagent
Get-AgentFiles
Get-AgentFileContent
Get-AgentChanges
Get-AgentChangeDiff
Keep-AgentChange
Restore-AgentChange
Get-AgentEvents
Archive-AgentSession
Unarchive-AgentSession
Get-AgentCollaborators
Add-AgentCollaborator
Remove-AgentCollaborator
Get-AgentGuidance
Set-AgentGuidance
Get-AgentGuidancePacks
New-AgentGuidancePack
Set-AgentGuidancePack
Remove-AgentGuidancePack
Get-AgentGuidanceDefaults
Set-AgentGuidanceDefaults
Get-AgentGuidanceTemplates
Install-AgentGuidanceTemplates
Get-AgentAudit
```
